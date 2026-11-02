package com.licht.recruitment.enquiries;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enquiries")
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String referenceCode; // e.g. LICHT-EMP-12345 or LICHT-CAN-12345

    @Column(nullable = false)
    private String type; // "CONTACT", "EMPLOYER_STAFFING", "CV_DROP"

    private String fullName;
    private String email;
    private String telephone;
    private String organisationName;
    private String sector;
    private String jobTitle;
    private String numberOfVacancies;
    private String workLocation;
    private String recruitmentType;
    private String requiredExperience;
    private String expectedStartDate;
    private String enquiryType;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String targetRole;
    private String preferredLocation;
    private String cvFileName;
    private String cvFileUrl;

    private boolean privacyConsent;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Enquiry() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getOrganisationName() { return organisationName; }
    public void setOrganisationName(String organisationName) { this.organisationName = organisationName; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getNumberOfVacancies() { return numberOfVacancies; }
    public void setNumberOfVacancies(String numberOfVacancies) { this.numberOfVacancies = numberOfVacancies; }

    public String getWorkLocation() { return workLocation; }
    public void setWorkLocation(String workLocation) { this.workLocation = workLocation; }

    public String getRecruitmentType() { return recruitmentType; }
    public void setRecruitmentType(String recruitmentType) { this.recruitmentType = recruitmentType; }

    public String getRequiredExperience() { return requiredExperience; }
    public void setRequiredExperience(String requiredExperience) { this.requiredExperience = requiredExperience; }

    public String getExpectedStartDate() { return expectedStartDate; }
    public void setExpectedStartDate(String expectedStartDate) { this.expectedStartDate = expectedStartDate; }

    public String getEnquiryType() { return enquiryType; }
    public void setEnquiryType(String enquiryType) { this.enquiryType = enquiryType; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public String getPreferredLocation() { return preferredLocation; }
    public void setPreferredLocation(String preferredLocation) { this.preferredLocation = preferredLocation; }

    public String getCvFileName() { return cvFileName; }
    public void setCvFileName(String cvFileName) { this.cvFileName = cvFileName; }

    public String getCvFileUrl() { return cvFileUrl; }
    public void setCvFileUrl(String cvFileUrl) { this.cvFileUrl = cvFileUrl; }

    public boolean isPrivacyConsent() { return privacyConsent; }
    public void setPrivacyConsent(boolean privacyConsent) { this.privacyConsent = privacyConsent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
