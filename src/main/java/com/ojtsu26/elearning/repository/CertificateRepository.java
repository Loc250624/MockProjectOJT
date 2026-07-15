package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Certificate;
import com.ojtsu26.elearning.model.enums.CertificateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Integer> {
    long countByStudentId(Integer studentId);
    long countByStudentIdAndStatus(Integer studentId, CertificateStatus status);
    long countByCourseInstructorId(Integer instructorId);
    Optional<Certificate> findByEnrollmentId(Integer enrollmentId);
    Optional<Certificate> findByVerificationCodeIgnoreCase(String verificationCode);
    Optional<Certificate> findByIdAndStudentId(Integer certificateId, Integer studentId);
    Page<Certificate> findAllByStudentId(Integer studentId, Pageable pageable);
    boolean existsByEnrollmentId(Integer enrollmentId);
}
