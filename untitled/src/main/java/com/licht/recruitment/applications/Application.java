package com.licht.recruitment.applications;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "job_applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long candidateId;

    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String candidateLocation;
    private String dbsStatus;
    private int experienceYears = 0;

    @Column(name = "candidate_current_role")
    private String currentRole;

    @Column(nullable = false)
    private Long jobId;

    private String jobTitle;
    private String companyName;
    private String sector;
    private String location;
    private String contractType;
    private String pay;

    private Long employerId;

    private String status = "Application Received";
    private String cvUsed;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private int matchScore = 85;
    private String appliedDate = LocalDate.now().toString();

    public Application() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

    public String getCandidatePhone() { return candidatePhone; }
    public void setCandidatePhone(String candidatePhone) { this.candidatePhone = candidatePhone; }

    public String getCandidateLocation() { return candidateLocation; }
    public void setCandidateLocation(String candidateLocation) { this.candidateLocation = candidateLocation; }

    public String getDbsStatus() { return dbsStatus; }
    public void setDbsStatus(String dbsStatus) { this.dbsStatus = dbsStatus; }

    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

    public String getCurrentRole() { return currentRole; }
    public void setCurrentRole(String currentRole) { this.currentRole = currentRole; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public String getPay() { return pay; }
    public void setPay(String pay) { this.pay = pay; }

    public Long getEmployerId() { return employerId; }
    public void setEmployerId(Long employerId) { this.employerId = employerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCvUsed() { return cvUsed; }
    public void setCvUsed(String cvUsed) { this.cvUsed = cvUsed; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }

    public String getAppliedDate() { return appliedDate; }
    public void setAppliedDate(String appliedDate) { this.appliedDate = appliedDate; }
}
