package com.licht.recruitment.auth;

import com.licht.recruitment.common.ApiResponse;
import com.licht.recruitment.common.JwtService;
import com.licht.recruitment.common.PasswordUtil;
import com.licht.recruitment.candidate.Candidate;
import com.licht.recruitment.candidate.CandidateRepository;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordUtil passwordUtil;

    @Value("${app.security.admin.email:admin@lichtrecruitmentservices.com}")
    private String configuredAdminEmail;

    @Value("${app.security.admin.password:}")
    private String configuredAdminPassword;

    @PostConstruct
    void requireAdminPassword() {
        if (configuredAdminPassword == null || configuredAdminPassword.length() < 12) {
            throw new IllegalStateException("ADMIN_PASSWORD environment variable must be set to at least 12 characters.");
        }
    }

    // 1. Candidate Registration with Verification & Validation
    @PostMapping("/candidate/register")
    public ResponseEntity<?> registerCandidate(@RequestBody CandidateRegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email address is required."));
        }
        String cleanEmail = request.getEmail().trim().toLowerCase();
        if (!cleanEmail.matches(EMAIL_PATTERN)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Please provide a valid email format."));
        }
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Full name is required."));
        }
        String passwordError = passwordUtil.validateStrength(request.getPassword());
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(passwordError));
        }
        if (candidateRepository.existsByEmail(cleanEmail)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("An account with this email address is already registered."));
        }

        // Build a fresh entity from whitelisted fields only (no id, no compliance flags)
        Candidate candidate = new Candidate();
        candidate.setFullName(request.getFullName().trim());
        candidate.setEmail(cleanEmail);
        candidate.setPassword(passwordUtil.hashPassword(request.getPassword()));
        candidate.setPhone(request.getPhone());
        candidate.setLocation(request.getLocation());
        candidate.setPostcode(request.getPostcode());
        candidate.setHeadline(request.getHeadline());
        candidate.setExperienceLevel(request.getExperienceLevel());
        candidate.setTargetSector(request.getTargetSector());
        candidate.setAvailability(request.getAvailability());
        candidate.setDbsStatus(request.getDbsStatus());
        candidate.setBio(request.getBio());
        candidate.setSkills(request.getSkills() != null ? new ArrayList<>(request.getSkills()) : new ArrayList<>());
        candidate.setCertifications(request.getCertifications() != null ? new ArrayList<>(request.getCertifications()) : new ArrayList<>());

        Candidate saved = candidateRepository.save(candidate);

        // Generate cryptographically signed token
        String token = jwtService.generateToken(saved.getEmail(), "CANDIDATE", saved.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", saved);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", data));
    }

    // 2. Candidate Login with Credential Verification
    @PostMapping("/candidate/login")
    public ResponseEntity<?> loginCandidate(@Valid @RequestBody LoginRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        Optional<Candidate> candidateOpt = candidateRepository.findByEmail(cleanEmail);

        if (candidateOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password."));
        }

        Candidate candidate = candidateOpt.get();
        if (!passwordUtil.verifyPassword(request.getPassword(), candidate.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password."));
        }

        // If the password was previously unhashed (plain-text), migrate to PBKDF2 on successful login
        if (candidate.getPassword() != null && !candidate.getPassword().startsWith("pbkdf2$")) {
            candidate.setPassword(passwordUtil.hashPassword(request.getPassword()));
            candidateRepository.save(candidate);
        }

        String token = jwtService.generateToken(candidate.getEmail(), "CANDIDATE", candidate.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", candidate);

        return ResponseEntity.ok(ApiResponse.success("Login successful. Welcome back!", data));
    }

    // 3. Candidate Logout
    @PostMapping("/candidate/logout")
    public ResponseEntity<?> logoutCandidate() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully.", null));
    }

    // 4. Admin Login with Configured Secrets & Signed Tokens
    @PostMapping("/admin/login")
    public ResponseEntity<?> loginAdmin(@Valid @RequestBody LoginRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        boolean emailMatch = configuredAdminEmail != null && configuredAdminEmail.trim().equalsIgnoreCase(cleanEmail);
        boolean passwordMatch = MessageDigest.isEqual(
                configuredAdminPassword.getBytes(StandardCharsets.UTF_8),
                request.getPassword().getBytes(StandardCharsets.UTF_8));

        if (emailMatch && passwordMatch) {
            String token = jwtService.generateToken(configuredAdminEmail, "ADMIN", 0L);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("email", configuredAdminEmail);
            data.put("role", "SUPER_ADMIN");

            return ResponseEntity.ok(ApiResponse.success("Admin authorized successfully.", data));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid administrator credentials. Access restricted."));
    }
}
