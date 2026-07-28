package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.service.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Retries idempotent certificate issuance for enrollments that reached 100%
 * before an eligibility bug was corrected.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class CertificateReconciliationService {
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CertificateService certificateService;

    @EventListener(ApplicationReadyEvent.class)
    public void issueMissingEligibleCertificates() {
        for (Integer enrollmentId : enrollmentRepository.findCompletedEnrollmentIdsWithoutCertificate()) {
            try {
                certificateService.issueAutomaticallyIfEligible(enrollmentId);
            } catch (BusinessException ex) {
                log.debug("Enrollment {} is not eligible for certificate reconciliation.", enrollmentId);
            }
        }
    }
}
