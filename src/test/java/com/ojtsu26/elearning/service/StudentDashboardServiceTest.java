package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.StudentDashboardStatsDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CertificateStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CertificateRepository;
import com.ojtsu26.elearning.service.impl.StudentDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentDashboardServiceTest {

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private CertificateRepository certificateRepository;

    private StudentDashboardServiceImpl service;
    private User student;

    @BeforeEach
    void setUp() {
        service = new StudentDashboardServiceImpl(currentUserService, certificateRepository);
        student = User.builder().id(3).role(Role.STUDENT).build();
    }

    @Test
    void certificateCardCountsOnlyActiveCertificatesAndMentionsRevokedSeparately() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(certificateRepository.countByStudentIdAndStatus(3, CertificateStatus.ACTIVE)).thenReturn(2L);
        when(certificateRepository.countByStudentIdAndStatus(3, CertificateStatus.REVOKED)).thenReturn(1L);

        StudentDashboardStatsDTO stats = service.getCurrentStudentStats();

        assertEquals(2, stats.getActiveCertificates());
        assertEquals(1, stats.getRevokedCertificates());
        assertEquals("2 active, 1 revoked", stats.getCertificateCaption());
    }

    @Test
    void zeroCertificatesDoNotFallbackToSampleData() {
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(certificateRepository.countByStudentIdAndStatus(3, CertificateStatus.ACTIVE)).thenReturn(0L);
        when(certificateRepository.countByStudentIdAndStatus(3, CertificateStatus.REVOKED)).thenReturn(0L);

        StudentDashboardStatsDTO stats = service.getCurrentStudentStats();

        assertEquals(0, stats.getActiveCertificates());
        assertEquals("No credentials yet", stats.getCertificateCaption());
        assertEquals("stat-change-neutral", stats.getCertificateCaptionClass());
    }

    @Test
    void wrongRoleIsRejectedBeforeCertificateQueries() {
        when(currentUserService.getCurrentUser()).thenReturn(User.builder().id(4).role(Role.TEACHER).build());

        assertThrows(BusinessException.class, () -> service.getCurrentStudentStats());

        verify(certificateRepository, never()).countByStudentIdAndStatus(any(), any());
    }
}
