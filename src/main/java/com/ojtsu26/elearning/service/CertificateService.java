package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.CertificateResponseDTO;
import com.ojtsu26.elearning.dto.response.CertificateVerificationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CertificateService {
    CertificateResponseDTO issueIfEligible(Integer enrollmentId, Integer currentStudentId);
    CertificateResponseDTO issueAutomaticallyIfEligible(Integer enrollmentId);
    Page<CertificateResponseDTO> getMyCertificates(Integer currentStudentId, Pageable pageable);
    CertificateResponseDTO getMyCertificate(Integer certificateId, Integer currentStudentId);
    byte[] generateCertificatePdf(Integer certificateId, Integer currentStudentId);
    CertificateVerificationDTO verifyByCode(String verificationCode);
}
