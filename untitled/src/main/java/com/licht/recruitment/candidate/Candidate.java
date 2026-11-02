package com.licht.recruitment.candidate;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String phone;
    private String location;
    private String postcode;
    private String headline;
    private String experienceLevel;
    private String targetSector;
    private String availability;
    private String dbsStatus;
    
    // Compliance & Admin fields
    private boolean dbsVerified = false;
    private boolean rightToWorkVerified = false;
    private boolean nmcPinVerified = false;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "candidate_skills", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "candidate_certifications", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "certification")
    private List<String> certifications = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private List<ExperienceItem> previousExperience = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private List<EducationItem> education = new ArrayList<>();

    // CV details
    private String savedCvFileName;
    private String savedCvFileSize;
    private String savedCvUploadedDate;
    private String savedCvFileUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "candidate_saved_jobs", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "job_id")
    private List<String> savedJobs = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    public Candidate() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

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

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }

    public String getTargetSector() { return targetSector; }
    public void setTargetSector(String targetSector) { this.targetSector = targetSector; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getDbsStatus() { return dbsStatus; }
    public void setDbsStatus(String dbsStatus) { this.dbsStatus = dbsStatus; }

    public boolean isDbsVerified() { return dbsVerified; }
    public void setDbsVerified(boolean dbsVerified) { this.dbsVerified = dbsVerified; }

    public boolean isRightToWorkVerified() { return rightToWorkVerified; }
    public void setRightToWorkVerified(boolean rightToWorkVerified) { this.rightToWorkVerified = rightToWorkVerified; }

    public boolean isNmcPinVerified() { return nmcPinVerified; }
    public void setNmcPinVerified(boolean nmcPinVerified) { this.nmcPinVerified = nmcPinVerified; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }

    public List<ExperienceItem> getPreviousExperience() { return previousExperience; }
    public void setPreviousExperience(List<ExperienceItem> previousExperience) { this.previousExperience = previousExperience; }

    public List<EducationItem> getEducation() { return education; }
    public void setEducation(List<EducationItem> education) { this.education = education; }

    public String getSavedCvFileName() { return savedCvFileName; }
    public void setSavedCvFileName(String savedCvFileName) { this.savedCvFileName = savedCvFileName; }

    public String getSavedCvFileSize() { return savedCvFileSize; }
    public void setSavedCvFileSize(String savedCvFileSize) { this.savedCvFileSize = savedCvFileSize; }

    public String getSavedCvUploadedDate() { return savedCvUploadedDate; }
    public void setSavedCvUploadedDate(String savedCvUploadedDate) { this.savedCvUploadedDate = savedCvUploadedDate; }

    public String getSavedCvFileUrl() { return savedCvFileUrl; }
    public void setSavedCvFileUrl(String savedCvFileUrl) { this.savedCvFileUrl = savedCvFileUrl; }

    public List<String> getSavedJobs() { return savedJobs; }
    public void setSavedJobs(List<String> savedJobs) { this.savedJobs = savedJobs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
