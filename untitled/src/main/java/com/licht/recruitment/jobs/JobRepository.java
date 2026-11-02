package com.licht.recruitment.jobs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(String status);
    List<Job> findByEmployerId(Long employerId);
    List<Job> findByUrgentCoverTrue();
    java.util.Optional<Job> findByCode(String code);

    @Query("SELECT j FROM Job j WHERE " +
           "(:sector IS NULL OR LOWER(j.sector) = LOWER(:sector)) AND " +
           "(:type IS NULL OR LOWER(j.type) LIKE LOWER(CONCAT('%', :type, '%'))) AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:search IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(j.summary) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Job> searchJobs(
        @Param("sector") String sector,
        @Param("type") String type,
        @Param("location") String location,
        @Param("search") String search
    );
}
