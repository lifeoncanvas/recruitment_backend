package com.licht.recruitment.enquiries;

import com.licht.recruitment.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.licht.recruitment.common.CvStorageService;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private CvStorageService cvStorageService;

    private static final SecureRandom RANDOM = new SecureRandom();

    private String newReferenceCode(String prefix) {
        String code;
        do {
            code = prefix + (10000 + RANDOM.nextInt(90000));
        } while (enquiryRepository.findByReferenceCode(code).isPresent());
        return code;
    }

    /** Clears server-controlled fields so a client cannot overwrite existing rows or forge file paths. */
    private static void resetServerFields(Enquiry enquiry) {
        enquiry.setId(null);
        enquiry.setCvFileName(null);
        enquiry.setCvFileUrl(null);
        enquiry.setCreatedAt(LocalDateTime.now());
    }

    @PostMapping("/contact")
    public ResponseEntity<ApiResponse<Enquiry>> submitContact(@RequestBody Enquiry enquiry) {
        if (enquiry.getFullName() == null || enquiry.getFullName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Please provide your full name."));
        }
        if (enquiry.getEmail() == null || !enquiry.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Please provide a valid email address."));
        }
        if (enquiry.getMessage() == null || enquiry.getMessage().trim().length() < 5) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Please provide a descriptive message (at least 5 characters)."));
        }
        if (!enquiry.isPrivacyConsent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Please accept the UK GDPR privacy consent to submit."));
        }

        resetServerFields(enquiry);
        String refCode = newReferenceCode("LICHT-CON-");
        enquiry.setReferenceCode(refCode);
        enquiry.setType("CONTACT");

        Enquiry saved = enquiryRepository.save(enquiry);
        ApiResponse<Enquiry> response = new ApiResponse<>(
            true, 
            "Your enquiry has been received successfully. A consultant will review and respond shortly.", 
            refCode, 
            saved
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/employer")
    public ResponseEntity<ApiResponse<Enquiry>> submitEmployerEnquiry(@RequestBody Enquiry enquiry) {
        if (enquiry.getOrganisationName() == null || enquiry.getOrganisationName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Company / Organisation name is required."));
        }
        if (enquiry.getFullName() == null || enquiry.getFullName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Primary contact person name is required."));
        }
        if (enquiry.getEmail() == null || !enquiry.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Valid work email address is required."));
        }
        if (enquiry.getTelephone() == null || enquiry.getTelephone().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Contact telephone number is required."));
        }

        resetServerFields(enquiry);
        String refCode = newReferenceCode("LICHT-EMP-");
        enquiry.setReferenceCode(refCode);
        enquiry.setType("EMPLOYER_STAFFING");

        Enquiry saved = enquiryRepository.save(enquiry);
        ApiResponse<Enquiry> response = new ApiResponse<>(
            true, 
            "Staffing enquiry registered. A consultant will contact you shortly.", 
            refCode, 
            saved
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/cv-drop", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Enquiry>> submitCvDrop(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("telephone") String telephone,
            @RequestParam(value = "preferredSector", defaultValue = "Care Homes") String preferredSector,
            @RequestParam(value = "targetRole", required = false) String targetRole,
            @RequestParam(value = "preferredLocation", required = false) String preferredLocation,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "privacyConsent", defaultValue = "false") boolean privacyConsent,
            @RequestParam("file") MultipartFile file) {

        if (fullName == null || fullName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Full name is required."));
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Valid email address is required."));
        }
        if (telephone == null || telephone.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Telephone number is required."));
        }
        if (!privacyConsent) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Please accept the UK GDPR privacy consent to submit."));
        }

        String rawName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "cv.pdf";
        String sanitizedName = rawName.replaceAll("[^a-zA-Z0-9._-]", "_");

        String storageKey;
        try {
            storageKey = cvStorageService.store(file, "cvs");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Could not save CV file. Please try again."));
        }

        String refCode = newReferenceCode("LICHT-CAN-");
        Enquiry enquiry = new Enquiry();
        enquiry.setReferenceCode(refCode);
        enquiry.setType("CV_DROP");
        enquiry.setFullName(fullName);
        enquiry.setEmail(email.trim().toLowerCase());
        enquiry.setTelephone(telephone);
        enquiry.setSector(preferredSector);
        enquiry.setTargetRole(targetRole);
        enquiry.setPreferredLocation(preferredLocation);
        enquiry.setMessage(message);
        enquiry.setPrivacyConsent(privacyConsent);

        enquiry.setCvFileName(sanitizedName);
        enquiry.setCvFileUrl(storageKey);

        Enquiry saved = enquiryRepository.save(enquiry);
        ApiResponse<Enquiry> response = new ApiResponse<>(
            true, 
            "CV received successfully. Our placement team will review your credentials.", 
            refCode, 
            saved
        );
        return ResponseEntity.ok(response);
    }
}
