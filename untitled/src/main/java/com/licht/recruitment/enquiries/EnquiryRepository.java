package com.licht.recruitment.enquiries;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {
    Optional<Enquiry> findByReferenceCode(String referenceCode);
    List<Enquiry> findByTypeOrderByCreatedAtDesc(String type);
}
