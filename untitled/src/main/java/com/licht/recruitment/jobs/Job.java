package com.licht.recruitment.jobs;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code; // e.g. CA-101
    private String title;
    private String sector; // 'Care homes', 'Healthcare', 'Supported living', 'Corporate'
    private String location;
    private String type; // 'Temporary', 'Permanent', 'Full time', 'Part time'
    private String shift;
    private String contract;
    private String workingPattern;
    private String pay;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String roleMeaning;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_role_involves", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "item", columnDefinition = "TEXT")
    private List<String> whatRoleInvolves = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_you_bring", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "item", columnDefinition = "TEXT")
    private List<String> whatYouBring = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String beforeYouApply;

    private String image;
    private String status = "Active"; // 'Active', 'Paused', 'Filled'
    private boolean urgentCover = false;
    private String postedDate = LocalDate.now().toString();

    private Long employerId;
    private String companyName;

    public Job() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public String getContract() { return contract; }
    public void setContract(String contract) { this.contract = contract; }

    public String getWorkingPattern() { return workingPattern; }
    public void setWorkingPattern(String workingPattern) { this.workingPattern = workingPattern; }

    public String getPay() { return pay; }
    public void setPay(String pay) { this.pay = pay; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getRoleMeaning() { return roleMeaning; }
    public void setRoleMeaning(String roleMeaning) { this.roleMeaning = roleMeaning; }

    public List<String> getWhatRoleInvolves() { return whatRoleInvolves; }
    public void setWhatRoleInvolves(List<String> whatRoleInvolves) { this.whatRoleInvolves = whatRoleInvolves; }

    public List<String> getWhatYouBring() { return whatYouBring; }
    public void setWhatYouBring(List<String> whatYouBring) { this.whatYouBring = whatYouBring; }

    public String getBeforeYouApply() { return beforeYouApply; }
    public void setBeforeYouApply(String beforeYouApply) { this.beforeYouApply = beforeYouApply; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isUrgentCover() { return urgentCover; }
    public void setUrgentCover(boolean urgentCover) { this.urgentCover = urgentCover; }

    public String getPostedDate() { return postedDate; }
    public void setPostedDate(String postedDate) { this.postedDate = postedDate; }

    public Long getEmployerId() { return employerId; }
    public void setEmployerId(Long employerId) { this.employerId = employerId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}
