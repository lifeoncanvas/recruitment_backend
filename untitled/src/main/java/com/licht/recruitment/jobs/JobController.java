package com.licht.recruitment.jobs;

import com.licht.recruitment.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs(
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String search) {

        if ("all".equalsIgnoreCase(sector)) sector = null;
        if ("all".equalsIgnoreCase(type)) type = null;
        if (sector != null && sector.trim().isEmpty()) sector = null;
        if (type != null && type.trim().isEmpty()) type = null;
        if (location != null && location.trim().isEmpty()) location = null;
        if (search != null && search.trim().isEmpty()) search = null;

        // Only live vacancies are public; paused/filled roles stay visible to their employer and admins
        List<Job> all = jobRepository.findByStatus("Active");

        if (sector == null && type == null && location == null && search == null) {
            return ResponseEntity.ok(all);
        }

        final String sSector = sector != null ? sector.toLowerCase() : null;
        final String sType = type != null ? type.toLowerCase() : null;
        final String sLoc = location != null ? location.toLowerCase() : null;
        final String sSearch = search != null ? search.toLowerCase() : null;

        List<Job> filtered = all.stream().filter(j -> {
            if (sSector != null && (j.getSector() == null || !j.getSector().equalsIgnoreCase(sSector))) return false;
            if (sType != null && (j.getType() == null || !j.getType().toLowerCase().contains(sType))) return false;
            if (sLoc != null && (j.getLocation() == null || !j.getLocation().toLowerCase().contains(sLoc))) return false;
            if (sSearch != null) {
                boolean matchTitle = j.getTitle() != null && j.getTitle().toLowerCase().contains(sSearch);
                boolean matchSummary = j.getSummary() != null && j.getSummary().toLowerCase().contains(sSearch);
                if (!matchTitle && !matchSummary) return false;
            }
            return true;
        }).toList();

        return ResponseEntity.ok(filtered);
    }

    // Accepts either the numeric id or the public reference code (e.g. CA-101)
    @GetMapping("/{idOrCode}")
    public ResponseEntity<?> getJobById(@PathVariable String idOrCode) {
        Optional<Job> job = Optional.empty();
        try {
            job = jobRepository.findById(Long.parseLong(idOrCode));
        } catch (NumberFormatException ignored) {}
        if (job.isEmpty()) {
            job = jobRepository.findByCode(idOrCode);
        }
        if (job.isPresent() && "Active".equalsIgnoreCase(job.get().getStatus())) {
            return ResponseEntity.ok(job.get());
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Vacancy not found"));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<Job>> getFeaturedJobs() {
        List<Job> urgentJobs = jobRepository.findByUrgentCoverTrue().stream()
                .filter(j -> "Active".equalsIgnoreCase(j.getStatus())).toList();
        if (urgentJobs.isEmpty()) {
            urgentJobs = jobRepository.findByStatus("Active");
            if (urgentJobs.size() > 4) {
                urgentJobs = urgentJobs.subList(0, 4);
            }
        }
        return ResponseEntity.ok(urgentJobs);
    }
}
