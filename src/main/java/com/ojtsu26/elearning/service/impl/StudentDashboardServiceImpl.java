package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.StudentDashboardStatsDTO;
import com.ojtsu26.elearning.dto.response.StudentDeadlineDashboardDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CertificateStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CertificateRepository;
import com.ojtsu26.elearning.repository.CodingAssignmentRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.projection.StudentDeadlineProjection;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentDashboardServiceImpl implements StudentDashboardService {

    private static final int DEFAULT_LIMIT = 3;
    private static final Set<SubmissionStatus> COMPLETED_STATUSES = Set.of(
            SubmissionStatus.AUTO_GRADED,
            SubmissionStatus.GRADED,
            SubmissionStatus.PASSED
    );

    private final CurrentUserService currentUserService;
    private final CodingAssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CertificateRepository certificateRepository;

    @Override
    @Transactional(readOnly = true)
    public StudentDashboardStatsDTO getCurrentStudentStats() {
        User student = currentUserService.getCurrentUser();
        if (student.getRole() != Role.STUDENT) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        long activeCertificates = certificateRepository.countByStudentIdAndStatus(student.getId(), CertificateStatus.ACTIVE);
        long revokedCertificates = certificateRepository.countByStudentIdAndStatus(student.getId(), CertificateStatus.REVOKED);

        return StudentDashboardStatsDTO.builder()
                .activeCertificates(activeCertificates)
                .revokedCertificates(revokedCertificates)
                .certificateCaption(certificateCaption(activeCertificates, revokedCertificates))
                .certificateCaptionClass(activeCertificates > 0 ? "stat-change-up" : "stat-change-neutral")
                .build();
    }

    private String certificateCaption(long activeCertificates, long revokedCertificates) {
        if (activeCertificates == 0 && revokedCertificates == 0) {
            return "No credentials yet";
        }
        if (revokedCertificates > 0) {
            return activeCertificates + " active, " + revokedCertificates + " revoked";
        }
        return activeCertificates == 1 ? "1 active credential" : activeCertificates + " active credentials";
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentDeadlineDashboardDTO> getCurrentStudentDeadlines(int limit) {
        User student = currentUserService.getCurrentUser();
        requireActiveStudent(student);

        Collection<Integer> completedAssignmentIds = submissionRepository
                .findCompletedAssignmentIdsForStudent(student.getId(), COMPLETED_STATUSES);
        if (completedAssignmentIds.isEmpty()) {
            completedAssignmentIds = List.of(-1);
        }

        int pageSize = limit <= 0 ? DEFAULT_LIMIT : limit;
        List<StudentDeadlineProjection> deadlines = assignmentRepository
                .findUpcomingDeadlinesForStudentExcludingCompleted(
                        student.getId(),
                        completedAssignmentIds,
                        PageRequest.of(0, pageSize)
                );
        List<Integer> assignmentIds = deadlines.stream()
                .map(StudentDeadlineProjection::getAssignmentId)
                .toList();
        Map<Integer, Submission> latestSubmissions = assignmentIds.isEmpty()
                ? Map.of()
                : submissionRepository.findLatestByAssignmentIdsAndStudentId(assignmentIds, student.getId())
                .stream()
                .filter(submission -> submission.getAssignment() != null)
                .collect(Collectors.toMap(
                        submission -> submission.getAssignment().getId(),
                        Function.identity(),
                        (left, right) -> left
                ));

        LocalDateTime now = LocalDateTime.now();
        return deadlines.stream()
                .map(deadline -> toDeadlineDto(deadline, latestSubmissions.get(deadline.getAssignmentId()), now))
                .toList();
    }

    private StudentDeadlineDashboardDTO toDeadlineDto(StudentDeadlineProjection deadline,
                                                      Submission submission,
                                                      LocalDateTime now) {
        LocalDateTime dueAt = deadline.getDueAt();
        DeadlineStatus status = resolveStatus(dueAt, submission, now);
        return StudentDeadlineDashboardDTO.builder()
                .id(deadline.getAssignmentId())
                .type("Assignment")
                .title(blankToFallback(deadline.getTitle(), "Untitled assignment"))
                .courseId(deadline.getCourseId())
                .courseTitle(blankToFallback(deadline.getCourseTitle(), "Untitled course"))
                .dueAt(dueAt)
                .day(dueAt == null ? "--" : String.format(Locale.ENGLISH, "%02d", dueAt.getDayOfMonth()))
                .month(dueAt == null ? "---" : dueAt.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ENGLISH))
                .statusLabel(status.label())
                .statusClass(status.badgeClass())
                .cardClass(status.cardClass())
                .destinationUrl("/student/assignments/" + deadline.getAssignmentId() + "/submit")
                .build();
    }

    private DeadlineStatus resolveStatus(LocalDateTime dueAt, Submission submission, LocalDateTime now) {
        if (submission != null && submission.getStatus() != null) {
            SubmissionStatus submissionStatus = submission.getStatus();
            if (submissionStatus == SubmissionStatus.SUBMITTED || submissionStatus == SubmissionStatus.PENDING_REVIEW) {
                return new DeadlineStatus("Submitted", "badge-warning", "muted");
            }
            if (submissionStatus == SubmissionStatus.RETURNED || submissionStatus == SubmissionStatus.FAILED) {
                return new DeadlineStatus("Needs Revision", "badge-warning", "urgent");
            }
            if (COMPLETED_STATUSES.contains(submissionStatus)) {
                return new DeadlineStatus("Completed", "badge-success", "muted");
            }
        }
        if (dueAt != null && dueAt.isBefore(now)) {
            return new DeadlineStatus("Overdue", "badge-danger", "urgent");
        }
        if (dueAt != null && dueAt.isBefore(now.plusDays(7))) {
            return new DeadlineStatus("Due Soon", "badge-warning", "urgent");
        }
        return new DeadlineStatus("Upcoming", "badge-secondary", "");
    }

    private void requireActiveStudent(User user) {
        if (user == null || user.getRole() != Role.STUDENT) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Only active students can access dashboard data.");
        }
    }

    private String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record DeadlineStatus(String label, String badgeClass, String cardClass) {
    }
}
