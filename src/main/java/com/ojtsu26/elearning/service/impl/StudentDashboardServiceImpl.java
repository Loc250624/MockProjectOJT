package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.StudentDashboardStatsDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CertificateStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CertificateRepository;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentDashboardServiceImpl implements StudentDashboardService {
    private final CurrentUserService currentUserService;
    private final CertificateRepository certificateRepository;

    @Override
    @Transactional(readOnly = true)
    public StudentDashboardStatsDTO getCurrentStudentStats() {
        User student = currentUserService.getCurrentUser();
        if (student.getRole() != Role.STUDENT) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        long active = certificateRepository.countByStudentIdAndStatus(
                student.getId(), CertificateStatus.ACTIVE);
        long revoked = certificateRepository.countByStudentIdAndStatus(
                student.getId(), CertificateStatus.REVOKED);

        return StudentDashboardStatsDTO.builder()
                .activeCertificates(active)
                .revokedCertificates(revoked)
                .certificateCaption(certificateCaption(active, revoked))
                .certificateCaptionClass(active > 0
                        ? "stat-change-up"
                        : "stat-change-neutral")
                .build();
    }

    private String certificateCaption(long active, long revoked) {
        if (active == 0 && revoked == 0) {
            return "No credentials yet";
        }
        if (revoked > 0) {
            return active + " active, " + revoked + " revoked";
        }
        return active == 1 ? "1 active credential" : active + " active credentials";
    }
}
