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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentDashboardServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private CertificateRepository certificateRepository;

    private StudentDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentDashboardServiceImpl(
                currentUserService, certificateRepository);
    }

    @Test
    void returnsCertificateStatsWithoutAssignmentDeadlineData() {
        User student = User.builder().id(5).role(Role.STUDENT).build();
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(certificateRepository.countByStudentIdAndStatus(
                5, CertificateStatus.ACTIVE)).thenReturn(2L);
        when(certificateRepository.countByStudentIdAndStatus(
                5, CertificateStatus.REVOKED)).thenReturn(1L);

        StudentDashboardStatsDTO result = service.getCurrentStudentStats();

        assertEquals(2, result.getActiveCertificates());
        assertEquals(1, result.getRevokedCertificates());
        assertEquals("2 active, 1 revoked", result.getCertificateCaption());
    }

    @Test
    void nonStudentCannotReadStudentDashboardData() {
        when(currentUserService.getCurrentUser())
                .thenReturn(User.builder().id(7).role(Role.TEACHER).build());

        assertThrows(BusinessException.class, service::getCurrentStudentStats);
        verifyNoInteractions(certificateRepository);
    }
}
