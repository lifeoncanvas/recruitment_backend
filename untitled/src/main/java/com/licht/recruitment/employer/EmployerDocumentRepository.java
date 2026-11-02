package com.licht.recruitment.employer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployerDocumentRepository extends JpaRepository<EmployerDocument, Long> {
    List<EmployerDocument> findByEmployerId(Long employerId);
    Optional<EmployerDocument> findByIdAndEmployerId(Long id, Long employerId);
    long countByEmployerId(Long employerId);
}
