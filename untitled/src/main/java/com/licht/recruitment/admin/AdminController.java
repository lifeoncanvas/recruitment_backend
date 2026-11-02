package com.licht.recruitment.admin;

import com.licht.recruitment.common.ApiResponse;
import com.licht.recruitment.common.CvStorageService;
import com.licht.recruitment.common.JwtService;
import com.licht.recruitment.employer.Employer;
import com.licht.recruitment.employer.EmployerController;
import com.licht.recruitment.employer.EmployerDocument;
import com.licht.recruitment.employer.EmployerDocumentRepository;
import com.licht.recruitment.employer.EmployerRepository;
import com.licht.recruitment.candidate.Candidate;
import com.licht.recruitment.candidate.CandidateRepository;
import com.licht.recruitment.applications.Application;
import com.licht.recruitment.applications.ApplicationRepository;
import com.licht.recruitment.enquiries.Enquiry;
import com.licht.recruitment.enquiries.EnquiryRepository;
import com.licht.recruitment.jobs.Job;
import com.licht.recruitment.jobs.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private EmployerRepository employerRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CvStorageService cvStorageService;

    @Autowired
    private EmployerDocumentRepository documentRepository;

    private boolean verifyAdminAccess(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return false;
        }
        JwtService.TokenClaims claims = jwtService.validateToken(authHeader);
        return claims != null && "ADMIN".equalsIgnoreCase(claims.getRole());
    }

    private <T> ResponseEntity<ApiResponse<T>> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Unauthorized: Valid administrator authorization token is required."));
    }

    @GetMapping("/employers")
    public ResponseEntity<?> getEmployers(
            @RequestParam(required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();

        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            String formattedStatus = status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
            return ResponseEntity.ok(employerRepository.findByVerificationStatus(formattedStatus));
        }
        return ResponseEntity.ok(employerRepository.findAll());
    }

    @PatchMapping("/employers/{id}/approve")
    public ResponseEntity<?> approveEmployer(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();

        Optional<Employer> empOpt = employerRepository.findById(id);
        if (empOpt.isEmpty()) return ResponseEntity.status(404).body(ApiResponse.error("Employer not found"));

        Employer emp = empOpt.get();
        emp.setVerificationStatus("Verified");
        emp.setDocumentsVerified(true);
        emp.setRejectionReason(null);
        employerRepository.save(emp);

        return ResponseEntity.ok(ApiResponse.success("Employer approved and verified successfully", emp));
    }

    @PatchMapping("/employers/{id}/reject")
    public ResponseEntity<?> rejectEmployer(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();

        Optional<Employer> empOpt = employerRepository.findById(id);
        if (empOpt.isEmpty()) return ResponseEntity.status(404).body(ApiResponse.error("Employer not found"));

        Employer emp = empOpt.get();
        emp.setVerificationStatus("Rejected");
        emp.setDocumentsVerified(false);
        if (payload != null && payload.containsKey("reason")) {
            emp.setRejectionReason(payload.get("reason"));
        }
        employerRepository.save(emp);

        return ResponseEntity.ok(ApiResponse.success("Employer status set to Rejected", emp));
    }

    @DeleteMapping("/employers/{id}")
    public ResponseEntity<?> deleteEmployer(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();

        if (!employerRepository.existsById(id)) {
            return ResponseEntity.status(404).body(ApiResponse.error("Employer not found"));
        }
        // Take the deleted employer's vacancies off the public board instead of leaving orphans live
        for (Job job : jobRepository.findByEmployerId(id)) {
            job.setStatus("Filled");
            jobRepository.save(job);
        }
        documentRepository.deleteAll(documentRepository.findByEmployerId(id));
        employerRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Employer deleted successfully", null));
    }

    @GetMapping("/employers/{id}/documents")
    public ResponseEntity<?> getEmployerDocuments(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();
        return ResponseEntity.ok(documentRepository.findByEmployerId(id));
    }

    @GetMapping("/employers/{id}/documents/{docId}/file")
    public ResponseEntity<?> downloadEmployerDocument(
            @PathVariable Long id,
            @PathVariable Long docId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();

        Optional<EmployerDocument> doc = documentRepository.findByIdAndEmployerId(docId, id);
        if (doc.isEmpty()) return ResponseEntity.status(404).body(ApiResponse.error("Document not found"));
        return cvStorageService.download(doc.get().getStorageKey(), doc.get().getFileName());
    }

    @GetMapping("/jobs")
    public ResponseEntity<?> getAllJobs(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();
        return ResponseEntity.ok(jobRepository.findAll());
    }

    @GetMapping("/candidates")
    public ResponseEntity<?> getCandidates(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();

        return ResponseEntity.ok(candidateRepository.findAll());
    }

    @GetMapping("/candidates/{id}/cv")
    public ResponseEntity<?> downloadCandidateCv(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();

        Optional<Candidate> cand = candidateRepository.findById(id);
        if (cand.isEmpty()) return ResponseEntity.status(404).body(ApiResponse.error("Candidate not found"));
        return cvStorageService.download(cand.get().getSavedCvFileUrl(), cand.get().getSavedCvFileName());
    }

    @PatchMapping("/candidates/{id}/compliance")
    public ResponseEntity<?> updateCompliance(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();

        Optional<Candidate> candOpt = candidateRepository.findById(id);
        if (candOpt.isEmpty()) return ResponseEntity.status(404).body(ApiResponse.error("Candidate not found"));

        Candidate candidate = candOpt.get();
        if (payload.get("dbsVerified") != null) {
            candidate.setDbsVerified(payload.get("dbsVerified"));
        }
        if (payload.get("rightToWorkVerified") != null) {
            candidate.setRightToWorkVerified(payload.get("rightToWorkVerified"));
        }
        if (payload.get("nmcPinVerified") != null) {
            candidate.setNmcPinVerified(payload.get("nmcPinVerified"));
        }

        candidateRepository.save(candidate);
        return ResponseEntity.ok(ApiResponse.success("Candidate compliance checks updated", candidate));
    }

    @GetMapping("/applications")
    public ResponseEntity<?> getAllApplications(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();

        return ResponseEntity.ok(applicationRepository.findAll());
    }

    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();

        Optional<Application> appOpt = applicationRepository.findById(id);
        if (appOpt.isEmpty()) return ResponseEntity.status(404).body(ApiResponse.error("Application not found"));

        String status = payload.get("status");
        if (status == null || !EmployerController.APPLICATION_STATUSES.contains(status)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid application status."));
        }
        Application app = appOpt.get();
        app.setStatus(status);
        applicationRepository.save(app);
        return ResponseEntity.ok(ApiResponse.success("Application status updated", app));
    }

    @GetMapping("/enquiries")
    public ResponseEntity<?> getEnquiries(
            @RequestParam(required = false) String type,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();

        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(enquiryRepository.findByTypeOrderByCreatedAtDesc(type.toUpperCase()));
        }
        List<Enquiry> all = enquiryRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(all);
    }

    @GetMapping("/enquiries/{id}/cv")
    public ResponseEntity<?> downloadEnquiryCv(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!verifyAdminAccess(authHeader)) return unauthorizedResponse();

        Optional<Enquiry> enq = enquiryRepository.findById(id);
        if (enq.isEmpty()) return ResponseEntity.status(404).body(ApiResponse.error("Enquiry not found"));
        return cvStorageService.download(enq.get().getCvFileUrl(), enq.get().getCvFileName());
    }
}
