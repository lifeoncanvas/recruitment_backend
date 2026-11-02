package com.licht.recruitment.common;

import com.licht.recruitment.jobs.Job;
import com.licht.recruitment.jobs.JobRepository;
import com.licht.recruitment.candidate.Candidate;
import com.licht.recruitment.candidate.CandidateRepository;
import com.licht.recruitment.employer.Employer;
import com.licht.recruitment.employer.EmployerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private EmployerRepository employerRepository;

    @Autowired
    private PasswordUtil passwordUtil;

    // Demo logins have passwords in source control, so they are only created when explicitly enabled
    @Value("${app.seed.demo-accounts:false}")
    private boolean seedDemoAccounts;

    @Override
    public void run(String... args) throws Exception {
        if (jobRepository.count() == 0) {
            Job j1 = new Job();
            j1.setCode("CA-101");
            j1.setTitle("Care Assistant");
            j1.setSector("Care homes");
            j1.setLocation("London");
            j1.setType("Temporary");
            j1.setContract("Temporary");
            j1.setShift("Day & night shifts");
            j1.setWorkingPattern("Day & night shifts");
            j1.setPay("£12.50 - £14.50 / hour");
            j1.setSummary("Bring a compassionate approach to supporting residents with everyday care and wellbeing.");
            j1.setRoleMeaning("As a Care Assistant, you support residents with day-to-day care, comfort and wellbeing.");
            j1.setWhatRoleInvolves(Arrays.asList(
                "Supporting personal care while respecting privacy, dignity and choice.",
                "Helping residents with meals, mobility and daily routines.",
                "Working with the care team and communicating changes in wellbeing.",
                "Following care plans, recording information and working within agreed procedures."
            ));
            j1.setWhatYouBring(Arrays.asList(
                "A compassionate, dependable and respectful approach.",
                "Clear communication and the ability to work collaboratively.",
                "Relevant care experience or completed Care Certificate."
            ));
            j1.setUrgentCover(true);
            j1.setCompanyName("Orchard View Care Suites");
            jobRepository.save(j1);

            Job j2 = new Job();
            j2.setCode("SW-102");
            j2.setTitle("Support Worker");
            j2.setSector("Supported living");
            j2.setLocation("Birmingham");
            j2.setType("Permanent");
            j2.setContract("Permanent");
            j2.setShift("Rotational shifts");
            j2.setWorkingPattern("Rotational shifts");
            j2.setPay("£24,000 - £27,000 / annum");
            j2.setSummary("Empower individuals in supported living settings to build independence and life skills.");
            j2.setRoleMeaning("Support workers assist individuals with complex needs to lead active, meaningful lives.");
            j2.setWhatRoleInvolves(Arrays.asList(
                "Assisting service users with daily living activities and community outings.",
                "Facilitating social inclusion and skill development.",
                "Maintaining positive behavioral support plans."
            ));
            j2.setWhatYouBring(Arrays.asList(
                "Empathy, patience and strong resilience.",
                "Level 2/3 Health and Social Care qualification preferred."
            ));
            j2.setUrgentCover(false);
            j2.setCompanyName("Midlands Living Pathways");
            jobRepository.save(j2);

            Job j3 = new Job();
            j3.setCode("RN-103");
            j3.setTitle("Registered General Nurse (RGN)");
            j3.setSector("Healthcare");
            j3.setLocation("Manchester");
            j3.setType("Permanent");
            j3.setContract("Permanent");
            j3.setShift("Day or Night shifts");
            j3.setWorkingPattern("Day or Night shifts");
            j3.setPay("£34,000 - £41,000 / annum");
            j3.setSummary("Provide high-standard clinical care, medication administration, and patient assessment.");
            j3.setRoleMeaning("Registered Nurses deliver clinical oversight and acute patient management.");
            j3.setWhatRoleInvolves(Arrays.asList(
                "Clinical assessment, care planning and medication rounds.",
                "Liaising with multidisciplinary teams and family members.",
                "Mentoring junior healthcare support staff."
            ));
            j3.setWhatYouBring(Arrays.asList(
                "Active NMC PIN registration.",
                "Minimum 6 months post-registration clinical experience."
            ));
            j3.setUrgentCover(true);
            j3.setCompanyName("St. Jude Healthcare Trust");
            jobRepository.save(j3);
        }

        if (seedDemoAccounts && employerRepository.count() == 0) {
            Employer emp = new Employer();
            emp.setOrganisationName("St. Jude Healthcare & Care Group");
            emp.setContactName("Sarah Jenkins");
            emp.setRoleTitle("Clinical Operations Lead");
            emp.setFacilityType("Nursing Care & Residential");
            emp.setEmail("sarah.jenkins@stjude-healthcare.co.uk");
            emp.setPassword(passwordUtil.hashPassword("LichtEmp2026!"));
            emp.setPhone("0161 829 4400");
            emp.setLocation("Manchester");
            emp.setPostcode("M3 4LQ");
            emp.setCqcRating("Good");
            emp.setVerificationStatus("Verified");
            emp.setDocumentsVerified(true);
            emp.setPrimarySector("Healthcare");
            employerRepository.save(emp);
        }

        if (seedDemoAccounts && candidateRepository.count() == 0) {
            Candidate cand = new Candidate();
            cand.setFullName("Marcus Campbell");
            cand.setEmail("marcus.campbell@example.co.uk");
            cand.setPassword(passwordUtil.hashPassword("Candidate2026!"));
            cand.setPhone("07700 900842");
            cand.setLocation("Manchester");
            cand.setPostcode("M14 6XX");
            cand.setHeadline("Senior Care Assistant & Shift Lead");
            cand.setExperienceLevel("5+ years of experience");
            cand.setTargetSector("Care homes");
            cand.setAvailability("Immediately available");
            cand.setDbsStatus("Enhanced DBS on Update Service");
            cand.setDbsVerified(true);
            cand.setRightToWorkVerified(true);
            cand.setBio("Dedicated and compassionate Senior Care Assistant with 6+ years of clinical and residential care experience.");
            cand.setSkills(Arrays.asList("Medication Administration", "PEG Feeding", "Moving & Handling (Train the Trainer)", "Dementia Care"));
            cand.setCertifications(Arrays.asList("NVQ Level 3 Health & Social Care", "First Aid at Work", "Enhanced DBS"));
            candidateRepository.save(cand);
        }
    }
}
