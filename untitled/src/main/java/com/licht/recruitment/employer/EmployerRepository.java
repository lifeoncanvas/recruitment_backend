package com.licht.recruitment.employer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployerRepository extends JpaRepository<Employer, Long> {
    Optional<Employer> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Employer> findByVerificationStatus(String verificationStatus);
}
