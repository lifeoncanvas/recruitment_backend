package com.licht.recruitment.applications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByCandidateIdOrderByAppliedDateDesc(Long candidateId);
    List<Application> findByJobId(Long jobId);
    List<Application> findByEmployerId(Long employerId);
    boolean existsByCandidateIdAndJobId(Long candidateId, Long jobId);
}
