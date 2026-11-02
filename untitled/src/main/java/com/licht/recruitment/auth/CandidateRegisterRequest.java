package com.licht.recruitment.auth;

import java.util.ArrayList;
import java.util.List;

/**
 * Fields a candidate may set when registering. Server-controlled fields
 * (id, compliance flags, CV storage paths) are deliberately absent.
 */
public class CandidateRegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private String location;
    private String postcode;
    private String headline;
    private String experienceLevel;
    private String targetSector;
    private String availability;
    private String dbsStatus;
    private String bio;
    private List<String> skills = new ArrayList<>();
    private List<String> certifications = new ArrayList<>();

    public CandidateRegisterRequest() {}

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

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }
}
