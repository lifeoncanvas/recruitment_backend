package com.licht.recruitment.employer;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employers")
public class Employer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String contactName;

    private String roleTitle;

    @Column(nullable = false)
    private String organisationName;

    private String facilityType;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String phone;
    private String location;
    private String postcode;
    private String registeredAddress;
    private String website;
    private String companyNumber;
    private String cqcProviderId;
    private String cqcRating;
    private String insurancePolicy;
    private String primarySector;

    private String verificationStatus = "Pending";
    private String rejectionReason;
    private boolean documentsVerified = false;

    private String registrationDate = LocalDate.now().toString();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "employer_saved_candidates", joinColumns = @JoinColumn(name = "employer_id"))
    @Column(name = "candidate_id")
    private List<String> savedCandidates = new ArrayList<>();

    public Employer() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getRoleTitle() { return roleTitle; }
    public void setRoleTitle(String roleTitle) { this.roleTitle = roleTitle; }

    public String getOrganisationName() { return organisationName; }
    public void setOrganisationName(String organisationName) { this.organisationName = organisationName; }

    public String getFacilityType() { return facilityType; }
    public void setFacilityType(String facilityType) { this.facilityType = facilityType; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }

    public String getRegisteredAddress() { return registeredAddress; }
    public void setRegisteredAddress(String registeredAddress) { this.registeredAddress = registeredAddress; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getCompanyNumber() { return companyNumber; }
    public void setCompanyNumber(String companyNumber) { this.companyNumber = companyNumber; }

    public String getCqcProviderId() { return cqcProviderId; }
    public void setCqcProviderId(String cqcProviderId) { this.cqcProviderId = cqcProviderId; }

    public String getCqcRating() { return cqcRating; }
    public void setCqcRating(String cqcRating) { this.cqcRating = cqcRating; }

    public String getInsurancePolicy() { return insurancePolicy; }
    public void setInsurancePolicy(String insurancePolicy) { this.insurancePolicy = insurancePolicy; }

    public String getPrimarySector() { return primarySector; }
    public void setPrimarySector(String primarySector) { this.primarySector = primarySector; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public boolean isDocumentsVerified() { return documentsVerified; }
    public void setDocumentsVerified(boolean documentsVerified) { this.documentsVerified = documentsVerified; }

    public String getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(String registrationDate) { this.registrationDate = registrationDate; }

    public List<String> getSavedCandidates() { return savedCandidates; }
    public void setSavedCandidates(List<String> savedCandidates) { this.savedCandidates = savedCandidates; }
}
