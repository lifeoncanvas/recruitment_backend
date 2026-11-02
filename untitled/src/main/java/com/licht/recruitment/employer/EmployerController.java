package com.licht.recruitment.employer;

import com.licht.recruitment.common.ApiResponse;
import com.licht.recruitment.common.CvStorageService;
import com.licht.recruitment.common.JwtService;
import com.licht.recruitment.common.PasswordUtil;
import com.licht.recruitment.auth.LoginRequest;
import com.licht.recruitment.jobs.Job;
import com.licht.recruitment.jobs.JobRepository;
import com.licht.recruitment.candidate.Candidate;
import com.licht.recruitment.candidate.CandidateRepository;
import com.licht.recruitment.applications.Application;
import com.licht.recruitment.applications.ApplicationRepository;
import com.licht.recruitment.enquiries.Enquiry;
import com.licht.recruitment.enquiries.EnquiryRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/employer")
public class EmployerController {

    public static final Set<String> JOB_STATUSES = Set.of("Active", "Paused", "Filled");
    public static final Set<String> APPLICATION_STATUSES = Set.of(
            "Application Received", "Under Review", "Shortlisted", "Interview Scheduled",
            "Offer Extended", "Declined", "Placed");

    @Autowired
    private EmployerRepository employerRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private CvStorageService cvStorageService;

    @Autowired
    private EmployerDocumentRepository documentRepository;

    /** Role of the short-lived token issued at registration; it can only upload compliance documents. */
    private static final String REGISTRATION_ROLE = "EMPLOYER_REGISTRATION";
    private static final long REGISTRATION_TOKEN_MS = 60 * 60 * 1000L;
    private static final int MAX_DOCUMENTS = 10;

    /**
     * Resolves the calling employer from the token. Only Verified employers are returned, so a
     * Pending or Rejected account loses access immediately, even with an unexpired token.
     */
    private Employer resolveEmployer(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return null;
        }

        JwtService.TokenClaims claims = jwtService.validateToken(authHeader);
        if (claims == null || !"EMPLOYER".equalsIgnoreCase(claims.getRole()) || claims.getUserId() == null) {
            return null;
        }

        return employerRepository.findById(claims.getUserId())
                .filter(e -> "Verified".equalsIgnoreCase(e.getVerificationStatus()))
                .orElse(null);
    }

    private boolean isAdmin(String authHeader) {
        if (authHeader == null) return false;
        JwtService.TokenClaims claims = jwtService.validateToken(authHeader);
        return claims != null && "ADMIN".equalsIgnoreCase(claims.getRole());
    }

    private static <T> ResponseEntity<ApiResponse<T>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Unauthorized: Please log in with a verified employer account."));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // 1. Employer Registration with Validation & Password Hashing
    @PostMapping("/register")
    public ResponseEntity<?> registerEmployer(@RequestBody Map<String, Object> body) {
        String organisationName = str(body, "organisationName");
        String contactName = str(body, "contactName");
        String email = str(body, "email");
        String password = str(body, "password");

        if (isBlank(organisationName)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Organisation / Company name is required."));
        }
        if (email == null || !email.trim().toLowerCase().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Valid work email address is required."));
        }
        if (isBlank(contactName)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Primary contact person name is required."));
        }
        String passwordError = passwordUtil.validateStrength(password);
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(passwordError));
        }

        String cleanEmail = email.trim().toLowerCase();
        if (employerRepository.existsByEmail(cleanEmail)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("An employer account with this email is already registered."));
        }

        // Build a fresh entity from whitelisted fields only (no id, no verification fields)
        Employer employer = new Employer();
        employer.setOrganisationName(organisationName.trim());
        employer.setContactName(contactName.trim());
        employer.setEmail(cleanEmail);
        employer.setPassword(passwordUtil.hashPassword(password));
        employer.setRoleTitle(str(body, "roleTitle"));
        employer.setFacilityType(str(body, "facilityType"));
        employer.setPhone(str(body, "phone"));
        employer.setLocation(str(body, "location"));
        employer.setPostcode(str(body, "postcode"));
        employer.setRegisteredAddress(str(body, "registeredAddress"));
        employer.setWebsite(str(body, "website"));
        employer.setCompanyNumber(str(body, "companyNumber"));
        employer.setCqcProviderId(str(body, "cqcProviderId"));
        employer.setCqcRating(str(body, "cqcRating"));
        employer.setInsurancePolicy(str(body, "insurancePolicy"));
        employer.setPrimarySector(str(body, "primarySector"));
        employer.setVerificationStatus("Pending");
        employer.setDocumentsVerified(false);

        Employer saved = employerRepository.save(employer);

        // No portal token is issued: the account cannot be used until an admin verifies it.
        // A one-hour upload token lets the registrant attach compliance documents for review.
        Map<String, Object> data = new HashMap<>();
        data.put("employer", saved);
        data.put("verificationStatus", "Pending");
        data.put("documentUploadToken", jwtService.generateToken(saved.getEmail(), REGISTRATION_ROLE, saved.getId(), REGISTRATION_TOKEN_MS));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Organisation registered. Pending compliance verification.", data));
    }

    // 2. Employer Login with Password & Status Verification
    @PostMapping("/login")
    public ResponseEntity<?> loginEmployer(@Valid @RequestBody LoginRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        Optional<Employer> empOpt = employerRepository.findByEmail(cleanEmail);

        if (empOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password."));
        }

        Employer employer = empOpt.get();
        if (!passwordUtil.verifyPassword(request.getPassword(), employer.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password."));
        }

        // Auto migrate plain text to PBKDF2 hash upon successful login
        if (employer.getPassword() != null && !employer.getPassword().startsWith("pbkdf2$")) {
            employer.setPassword(passwordUtil.hashPassword(request.getPassword()));
            employerRepository.save(employer);
        }

        // Verification status checks
        if ("Pending".equalsIgnoreCase(employer.getVerificationStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Account pending verification. You will be notified once approved by LICHT compliance."));
        }

        if ("Rejected".equalsIgnoreCase(employer.getVerificationStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Account registration was declined: " + (employer.getRejectionReason() != null ? employer.getRejectionReason() : "Compliance requirements not met.")));
        }

        String token = jwtService.generateToken(employer.getEmail(), "EMPLOYER", employer.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("employer", employer);
        data.put("verificationStatus", employer.getVerificationStatus());

        return ResponseEntity.ok(ApiResponse.success("Login successful. Welcome to the Employer Portal.", data));
    }

    // 2b. Upload a compliance document (verified employer, or the registration upload token)
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "docType", defaultValue = "Supporting document") String docType,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long employerId = null;
        Employer verified = resolveEmployer(authHeader);
        if (verified != null) {
            employerId = verified.getId();
        } else {
            JwtService.TokenClaims claims = jwtService.validateToken(authHeader);
            if (claims != null && REGISTRATION_ROLE.equals(claims.getRole()) && claims.getUserId() != null
                    && employerRepository.existsById(claims.getUserId())) {
                employerId = claims.getUserId();
            }
        }
        if (employerId == null) return unauthorized();

        if (documentRepository.countByEmployerId(employerId) >= MAX_DOCUMENTS) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Document limit reached. Please contact the LICHT compliance team."));
        }

        try {
            String key = cvStorageService.store(file, "employer-docs", true);
            EmployerDocument doc = new EmployerDocument();
            doc.setEmployerId(employerId);
            doc.setDocType(docType.length() > 80 ? docType.substring(0, 80) : docType);
            String rawName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
            doc.setFileName(rawName.replaceAll("[^a-zA-Z0-9._-]", "_"));
            doc.setFileSize(Math.max(1, file.getSize() / 1024) + " KB");
            doc.setStorageKey(key);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Document uploaded for compliance review", documentRepository.save(doc)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Could not save document. Please try again."));
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<?> listDocuments(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        if (emp == null) return unauthorized();
        return ResponseEntity.ok(documentRepository.findByEmployerId(emp.getId()));
    }

    @GetMapping("/documents/{docId}/file")
    public ResponseEntity<?> downloadDocument(
            @PathVariable Long docId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        if (emp == null) return unauthorized();
        Optional<EmployerDocument> doc = documentRepository.findByIdAndEmployerId(docId, emp.getId());
        if (doc.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Document not found"));
        return cvStorageService.download(doc.get().getStorageKey(), doc.get().getFileName());
    }

    // 3. Get Profile
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        if (emp == null) return unauthorized();
        return ResponseEntity.ok(emp);
    }

    // 4. Update Profile
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, Object> updates,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        if (emp == null) return unauthorized();

        if (str(updates, "contactName") != null) emp.setContactName(str(updates, "contactName"));
        if (str(updates, "roleTitle") != null) emp.setRoleTitle(str(updates, "roleTitle"));
        if (str(updates, "organisationName") != null) emp.setOrganisationName(str(updates, "organisationName"));
        if (str(updates, "phone") != null) emp.setPhone(str(updates, "phone"));
        if (str(updates, "location") != null) emp.setLocation(str(updates, "location"));
        if (str(updates, "postcode") != null) emp.setPostcode(str(updates, "postcode"));
        if (str(updates, "registeredAddress") != null) emp.setRegisteredAddress(str(updates, "registeredAddress"));
        if (str(updates, "website") != null) emp.setWebsite(str(updates, "website"));
        if (str(updates, "insurancePolicy") != null) emp.setInsurancePolicy(str(updates, "insurancePolicy"));

        Employer saved = employerRepository.save(emp);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", saved));
    }

    // 5. Get Employer's Job Listings
    @GetMapping("/jobs")
    public ResponseEntity<?> getEmployerJobs(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        if (emp == null) return unauthorized();

        List<Job> jobs = jobRepository.findByEmployerId(emp.getId());
        return ResponseEntity.ok(jobs);
    }

    // 6. Post New Job (whitelisted fields only; id, owner and status are server-controlled)
    @PostMapping("/jobs")
    public ResponseEntity<?> postJob(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        if (emp == null) return unauthorized();

        String title = str(body, "title");
        if (isBlank(title)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Job title is required."));
        }
        if (isBlank(str(body, "location"))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Job location is required."));
        }

        Job job = new Job();
        job.setTitle(title.trim());
        job.setSector(str(body, "sector"));
        job.setLocation(str(body, "location"));
        job.setType(str(body, "type"));
        job.setContract(str(body, "contract"));
        job.setShift(str(body, "shift"));
        job.setWorkingPattern(str(body, "workingPattern"));
        job.setPay(str(body, "pay"));
        job.setSummary(str(body, "summary") != null ? str(body, "summary") : title.trim() + " in " + str(body, "location"));
        job.setRoleMeaning(str(body, "roleMeaning"));
        job.setBeforeYouApply(str(body, "beforeYouApply"));
        job.setWhatRoleInvolves(strList(body, "whatRoleInvolves"));
        job.setWhatYouBring(strList(body, "whatYouBring"));
        job.setUrgentCover(Boolean.TRUE.equals(body.get("urgentCover")));
        job.setEmployerId(emp.getId());
        job.setCompanyName(emp.getOrganisationName());
        job.setStatus("Active");

        Job saved = jobRepository.save(job);
        // Give every job a public reference code so candidates can find and apply to it
        if (saved.getCode() == null) {
            saved.setCode("LJ-" + saved.getId());
            saved = jobRepository.save(saved);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job posted successfully", saved));
    }

    // 7. Change Job Status
    @PatchMapping("/jobs/{id}/status")
    public ResponseEntity<?> updateJobStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        boolean admin = isAdmin(authHeader);
        if (emp == null && !admin) return unauthorized();

        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Job not found"));

        Job job = jobOpt.get();
        if (!admin && !Objects.equals(job.getEmployerId(), emp.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access forbidden: You do not own this job listing."));
        }

        String status = payload.get("status");
        if (status == null || !JOB_STATUSES.contains(status)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Status must be one of " + JOB_STATUSES + "."));
        }
        job.setStatus(status);
        jobRepository.save(job);
        return ResponseEntity.ok(ApiResponse.success("Status updated to " + job.getStatus(), job));
    }

    // 8a. All applications across this employer's own jobs
    @GetMapping("/applications")
    public ResponseEntity<?> getAllApplicants(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        if (emp == null) return unauthorized();
        return ResponseEntity.ok(applicationRepository.findByEmployerId(emp.getId()));
    }

    // 8b. View Applicants for a Job (Protected - strictly verifies employer ownership)
    @GetMapping("/jobs/{id}/applicants")
    public ResponseEntity<?> getJobApplicants(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        boolean admin = isAdmin(authHeader);
        if (emp == null && !admin) return unauthorized();

        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Job not found"));

        Job job = jobOpt.get();
        if (!admin && !Objects.equals(job.getEmployerId(), emp.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access forbidden: You cannot view applicants for jobs owned by other employers."));
        }

        List<Application> applicants = applicationRepository.findByJobId(id);
        return ResponseEntity.ok(applicants);
    }

    /**
     * Loads an application only if it belongs to the given job and the caller owns that job.
     */
    private Optional<Application> ownedApplication(Long jobId, Long applicationId, Employer emp, boolean admin) {
        Optional<Job> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) return Optional.empty();
        if (!admin && !Objects.equals(jobOpt.get().getEmployerId(), emp.getId())) return Optional.empty();
        return applicationRepository.findById(applicationId)
                .filter(app -> Objects.equals(app.getJobId(), jobId));
    }

    // 9. Update Applicant Status (Protected - verifies job ownership AND that the application is for that job)
    @PatchMapping("/jobs/{jobId}/applicants/{applicantId}")
    public ResponseEntity<?> updateApplicantStatus(
            @PathVariable Long jobId,
            @PathVariable Long applicantId,
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        boolean admin = isAdmin(authHeader);
        if (emp == null && !admin) return unauthorized();

        Optional<Application> appOpt = ownedApplication(jobId, applicantId, emp, admin);
        if (appOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Application not found"));

        String status = payload.get("status");
        if (status == null || !APPLICATION_STATUSES.contains(status)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid application status."));
        }
        Application app = appOpt.get();
        app.setStatus(status);
        applicationRepository.save(app);
        return ResponseEntity.ok(ApiResponse.success("Applicant status updated", app));
    }

    // 9b. Download an applicant's CV (only for applications to the caller's own jobs)
    @GetMapping("/jobs/{jobId}/applicants/{applicantId}/cv")
    public ResponseEntity<?> downloadApplicantCv(
            @PathVariable Long jobId,
            @PathVariable Long applicantId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        boolean admin = isAdmin(authHeader);
        if (emp == null && !admin) return unauthorized();

        Optional<Application> appOpt = ownedApplication(jobId, applicantId, emp, admin);
        if (appOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Application not found"));

        Optional<Candidate> cand = candidateRepository.findById(appOpt.get().getCandidateId());
        if (cand.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Candidate not found"));
        return cvStorageService.download(cand.get().getSavedCvFileUrl(), cand.get().getSavedCvFileName());
    }

    // 10. Talent Pool (verified employers/admins only; contact details are withheld until an interview is arranged)
    @GetMapping("/talent-pool")
    public ResponseEntity<?> getTalentPool(
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String location,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        boolean admin = isAdmin(authHeader);
        if (emp == null && !admin) return unauthorized();

        List<Map<String, Object>> pool = new ArrayList<>();
        for (Candidate c : candidateRepository.findAll()) {
            if (!isBlank(sector) && !sector.equalsIgnoreCase(c.getTargetSector())) continue;
            if (!isBlank(location) && (c.getLocation() == null || !c.getLocation().toLowerCase().contains(location.toLowerCase()))) continue;

            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", c.getId());
            view.put("fullName", c.getFullName());
            view.put("headline", c.getHeadline());
            view.put("targetSector", c.getTargetSector());
            view.put("location", c.getLocation());
            view.put("experienceLevel", c.getExperienceLevel());
            view.put("availability", c.getAvailability());
            view.put("dbsStatus", c.getDbsStatus());
            view.put("dbsVerified", c.isDbsVerified());
            view.put("rightToWorkVerified", c.isRightToWorkVerified());
            view.put("nmcPinVerified", c.isNmcPinVerified());
            view.put("skills", c.getSkills());
            view.put("certifications", c.getCertifications());
            view.put("bio", c.getBio());
            pool.add(view);
        }
        return ResponseEntity.ok(pool);
    }

    // 11. Request Interview with Talent Candidate (recorded for the LICHT placement desk to action)
    @PostMapping("/talent-pool/{id}/interview")
    public ResponseEntity<?> requestInterview(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        if (emp == null) return unauthorized();

        Optional<Candidate> cand = candidateRepository.findById(id);
        if (cand.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Candidate not found"));

        String message = payload != null && payload.get("message") != null
                ? payload.get("message")
                : "Direct interview request from " + emp.getOrganisationName();

        Enquiry enquiry = new Enquiry();
        enquiry.setReferenceCode("LICHT-INT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        enquiry.setType("INTERVIEW_REQUEST");
        enquiry.setFullName(emp.getContactName());
        enquiry.setEmail(emp.getEmail());
        enquiry.setTelephone(emp.getPhone());
        enquiry.setOrganisationName(emp.getOrganisationName());
        enquiry.setSubject("Interview request for candidate #" + id + " (" + cand.get().getFullName() + ")");
        enquiry.setMessage(message);
        enquiry.setPrivacyConsent(true);
        enquiryRepository.save(enquiry);

        return ResponseEntity.ok(ApiResponse.success("Interview request sent to the LICHT placement desk", enquiry.getReferenceCode()));
    }

    // 12. Save / Unsave Candidate
    @PostMapping("/talent-pool/{id}/save")
    public ResponseEntity<?> saveCandidate(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        if (emp == null) return unauthorized();

        if (!emp.getSavedCandidates().contains(id)) {
            emp.getSavedCandidates().add(id);
            employerRepository.save(emp);
        }
        return ResponseEntity.ok(ApiResponse.success("Candidate saved to shortlist", emp.getSavedCandidates()));
    }

    @DeleteMapping("/talent-pool/{id}/save")
    public ResponseEntity<?> unsaveCandidate(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Employer emp = resolveEmployer(authHeader);
        if (emp == null) return unauthorized();

        emp.getSavedCandidates().remove(id);
        employerRepository.save(emp);
        return ResponseEntity.ok(ApiResponse.success("Candidate removed from shortlist", emp.getSavedCandidates()));
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body != null ? body.get(key) : null;
        return v != null ? v.toString() : null;
    }

    private static List<String> strList(Map<String, Object> body, String key) {
        List<String> out = new ArrayList<>();
        Object v = body != null ? body.get(key) : null;
        if (v instanceof List<?> list) {
            for (Object o : list) if (o != null) out.add(o.toString());
        }
        return out;
    }
}
