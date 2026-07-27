package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.service.CertificateService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CertificateReconciliationServiceTest {

    @Test
    void retriesOnlyCompletedEnrollmentsWithoutCertificates() {
        CourseEnrollmentRepository enrollmentRepository =
                mock(CourseEnrollmentRepository.class);
        CertificateService certificateService = mock(CertificateService.class);
        when(enrollmentRepository.findCompletedEnrollmentIdsWithoutCertificate())
                .thenReturn(List.of(42));
        CertificateReconciliationService reconciliation =
                new CertificateReconciliationService(
                        enrollmentRepository, certificateService);

        reconciliation.issueMissingEligibleCertificates();

        verify(certificateService).issueAutomaticallyIfEligible(42);
    }
}
