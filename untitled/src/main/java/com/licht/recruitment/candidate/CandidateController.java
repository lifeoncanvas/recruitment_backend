package com.licht.recruitment.candidate;

import com.licht.recruitment.common.ApiResponse;
import com.licht.recruitment.common.JwtService;
import com.licht.recruitment.jobs.Job;
import com.licht.recruitment.jobs.JobRepository;
import com.licht.recruitment.applications.Application;
import com.licht.recruitment.applications.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.licht.recruitment.common.CvStorageService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/candidate")
public class CandidateController {

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CvStorageService cvStorageService;

    private Candidate resolveCandidate(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return null;
        }

        JwtService.TokenClaims claims = jwtService.validateToken(authHeader);
        if (claims == null) {
            return null;
        }

        if ("CANDIDATE".equalsIgnoreCase(claims.getRole())) {
            if (claims.getUserId() != null) {
                return candidateRepository.findById(claims.getUserId()).orElse(null);
            }
            if (claims.getSubject() != null) {
                return candidateRepository.findByEmail(claims.getSubject().toLowerCase()).orElse(null);
            }
        }
        return null;
    }

    private boolean isAdmin(String authHeader) {
        if (authHeader == null) return false;
        JwtService.TokenClaims claims = jwtService.validateToken(authHeader);
        return claims != null && "ADMIN".equalsIgnoreCase(claims.getRole());
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        if (candidate == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Please log in as a candidate to view profile."));
        }
        return ResponseEntity.ok(candidate);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Candidate updates,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        if (candidate == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized: Please log in as a candidate to update profile."));
        }

        if (updates.getFullName() != null) candidate.setFullName(updates.getFullName());
        if (updates.getPhone() != null) candidate.setPhone(updates.getPhone());
        if (updates.getLocation() != null) candidate.setLocation(updates.getLocation());
        if (updates.getPostcode() != null) candidate.setPostcode(updates.getPostcode());
        if (updates.getHeadline() != null) candidate.setHeadline(updates.getHeadline());
        if (updates.getExperienceLevel() != null) candidate.setExperienceLevel(updates.getExperienceLevel());
        if (updates.getTargetSector() != null) candidate.setTargetSector(updates.getTargetSector());
        if (updates.getAvailability() != null) candidate.setAvailability(updates.getAvailability());
        if (updates.getDbsStatus() != null) candidate.setDbsStatus(updates.getDbsStatus());
        if (updates.getBio() != null) candidate.setBio(updates.getBio());
        if (updates.getSkills() != null) candidate.setSkills(updates.getSkills());
        if (updates.getCertifications() != null) candidate.setCertifications(updates.getCertifications());

        Candidate saved = candidateRepository.save(candidate);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", saved));
    }

    @PostMapping(value = "/upload-cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCV(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        if (candidate == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized: Candidate login required."));
        }

        try {
            // Validates the real file signature and stores outside any public directory
            String storageKey = cvStorageService.store(file, "candidate-cvs");
            String rawName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "cv.pdf";

            candidate.setSavedCvFileName(rawName.replaceAll("[^a-zA-Z0-9._-]", "_"));
            candidate.setSavedCvFileSize(Math.max(1, file.getSize() / 1024) + " KB");
            candidate.setSavedCvUploadedDate(LocalDate.now().toString());
            candidate.setSavedCvFileUrl(storageKey);

            candidateRepository.save(candidate);

            Map<String, String> data = new HashMap<>();
            data.put("fileName", candidate.getSavedCvFileName());
            data.put("fileSize", candidate.getSavedCvFileSize());
            data.put("uploadedDate", candidate.getSavedCvUploadedDate());
            data.put("fileUrl", "/candidate/cv");

            return ResponseEntity.ok(ApiResponse.success("CV uploaded successfully", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Could not save CV file. Please try again."));
        }
    }

    @GetMapping("/cv")
    public ResponseEntity<?> downloadOwnCv(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        if (candidate == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        return cvStorageService.download(candidate.getSavedCvFileUrl(), candidate.getSavedCvFileName());
    }

    @PostMapping("/experience")
    public ResponseEntity<?> addExperience(
            @RequestBody ExperienceItem exp,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        if (candidate == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        // Always insert a new row; a client-supplied id would merge into another user's record
        exp.setId(null);
        candidate.getPreviousExperience().add(exp);
        candidateRepository.save(candidate);
        return ResponseEntity.ok(ApiResponse.success("Experience added", candidate.getPreviousExperience()));
    }

    @DeleteMapping("/experience/{id}")
    public ResponseEntity<?> deleteExperience(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        if (candidate == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        candidate.getPreviousExperience().removeIf(e -> Objects.equals(e.getId(), id));
        candidateRepository.save(candidate);
        return ResponseEntity.ok(ApiResponse.success("Experience removed", candidate.getPreviousExperience()));
    }

    @PostMapping("/education")
    public ResponseEntity<?> addEducation(
            @RequestBody EducationItem edu,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        if (candidate == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        edu.setId(null);
        candidate.getEducation().add(edu);
        candidateRepository.save(candidate);
        return ResponseEntity.ok(ApiResponse.success("Education added", candidate.getEducation()));
    }

    @DeleteMapping("/education/{id}")
    public ResponseEntity<?> deleteEducation(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        if (candidate == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        candidate.getEducation().removeIf(e -> Objects.equals(e.getId(), id));
        candidateRepository.save(candidate);
        return ResponseEntity.ok(ApiResponse.success("Education removed", candidate.getEducation()));
    }

    @PostMapping("/applications")
    public ResponseEntity<?> applyToJob(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        if (candidate == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized: Candidate login required to submit application."));

        Object rawJobId = payload.get("jobId");
        if (rawJobId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Job ID or reference code is required."));
        }
        String strJobId = rawJobId.toString().trim();
        String notes = payload.get("notes") != null ? payload.get("notes").toString() : "";

        Optional<Job> jobOpt = Optional.empty();
        try {
            Long numericId = Long.parseLong(strJobId);
            jobOpt = jobRepository.findById(numericId);
        } catch (NumberFormatException ignored) {}

        if (jobOpt.isEmpty()) {
            jobOpt = jobRepository.findByCode(strJobId);
        }
        if (jobOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("The requested vacancy could not be found."));
        }
        Job job = jobOpt.get();

        if (!"Active".equalsIgnoreCase(job.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("This vacancy is no longer accepting applications."));
        }

        if (applicationRepository.existsByCandidateIdAndJobId(candidate.getId(), job.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("You have already submitted an application for this role."));
        }

        Application app = new Application();
        app.setCandidateId(candidate.getId());
        app.setCandidateName(candidate.getFullName());
        app.setCandidateEmail(candidate.getEmail());
        app.setCandidatePhone(candidate.getPhone());
        app.setCandidateLocation(candidate.getLocation());
        app.setDbsStatus(candidate.getDbsStatus());
        app.setCurrentRole(candidate.getHeadline());
        app.setJobId(job.getId());
        app.setJobTitle(job.getTitle());
        app.setCompanyName(job.getCompanyName() != null ? job.getCompanyName() : "LICHT Partner");
        app.setSector(job.getSector());
        app.setLocation(job.getLocation());
        app.setContractType(job.getContract() != null ? job.getContract() : job.getType());
        app.setPay(job.getPay());
        app.setEmployerId(job.getEmployerId());
        app.setCvUsed(candidate.getSavedCvFileName() != null ? candidate.getSavedCvFileName() : "Uploaded_CV.pdf");
        app.setNotes(notes);

        Application saved = applicationRepository.save(app);
        return ResponseEntity.ok(ApiResponse.success("Application submitted successfully", saved));
    }

    @GetMapping("/applications")
    public ResponseEntity<?> getApplications(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        if (candidate == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        List<Application> apps = applicationRepository.findByCandidateIdOrderByAppliedDateDesc(candidate.getId());
        return ResponseEntity.ok(apps);
    }

    // Secured withdraw endpoint with strict ownership check to prevent BOLA / IDOR deletion
    @DeleteMapping("/applications/{id}")
    public ResponseEntity<?> withdrawApplication(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        boolean admin = isAdmin(authHeader);
        if (candidate == null && !admin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));
        }

        Optional<Application> appOpt = applicationRepository.findById(id);
        if (appOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Application not found"));
        }

        Application app = appOpt.get();
        if (!admin && !Objects.equals(app.getCandidateId(), candidate.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access forbidden: You cannot withdraw another candidate's application."));
        }

        applicationRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Application withdrawn", null));
    }

    @PostMapping("/saved-jobs/{jobId}")
    public ResponseEntity<?> saveJob(
            @PathVariable String jobId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        if (candidate == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        if (!candidate.getSavedJobs().contains(jobId)) {
            candidate.getSavedJobs().add(jobId);
            candidateRepository.save(candidate);
        }
        return ResponseEntity.ok(ApiResponse.success("Job saved", candidate.getSavedJobs()));
    }

    @DeleteMapping("/saved-jobs/{jobId}")
    public ResponseEntity<?> unsaveJob(
            @PathVariable String jobId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Candidate candidate = resolveCandidate(authHeader);
        if (candidate == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthorized"));

        candidate.getSavedJobs().remove(jobId);
        candidateRepository.save(candidate);
        return ResponseEntity.ok(ApiResponse.success("Job unsaved", candidate.getSavedJobs()));
    }
}
