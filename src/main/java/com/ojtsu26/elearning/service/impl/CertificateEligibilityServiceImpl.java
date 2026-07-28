package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.CertificateEligibilityResponse;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CertificateRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.QuizAttemptRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.service.CertificateEligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CertificateEligibilityServiceImpl implements CertificateEligibilityService {

    private static final BigDecimal COMPLETE_PROGRESS = new BigDecimal("100.00");

    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CertificateRepository certificateRepository;

    @Override
    @Transactional(readOnly = true)
    public CertificateEligibilityResponse evaluateEligibility(Integer enrollmentId, Integer currentStudentId) {
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));

        User student = enrollment.getStudent();
        if (student == null || student.getId() == null || !student.getId().equals(currentStudentId)) {
            throw new BusinessException(ErrorCode.ENROLLMENT_ACCESS_DENIED);
        }

        Course course = enrollment.getCourse();
        List<String> unmet = new ArrayList<>();
        if (course == null) {
            unmet.add("Course is missing.");
        } else if (course.getStatus() != CourseStatus.APPROVED) {
            unmet.add("Course is not approved for certificate issuance.");
        }

        if (student.getStatus() != UserStatus.ACTIVE) {
            unmet.add("Student account is not active.");
        }

        BigDecimal progressPercent = enrollment.getProgressPercentage() == null
                ? BigDecimal.ZERO
                : enrollment.getProgressPercentage();
        if (!Boolean.TRUE.equals(enrollment.getIsCompleted()) || progressPercent.compareTo(COMPLETE_PROGRESS) < 0) {
            unmet.add("Course progress is not complete.");
        }

        long totalRequiredLessons = 0;
        long completedRequiredLessons = 0;
        long totalRequiredAssessments = 0;
        long passedRequiredAssessments = 0;
        if (course != null && course.getId() != null) {
            totalRequiredLessons = lessonRepository.countRequiredContentLessons(course.getId());
            completedRequiredLessons = lessonRepository.countCompletedRequiredContentLessons(course.getId(), enrollmentId);
            totalRequiredAssessments = lessonRepository.countRequiredAssessmentLessons(course.getId());
            Set<Integer> passedAssessmentLessonIds = new HashSet<>(
                    submissionRepository.findPassedRequiredAssessmentLessonIds(
                            currentStudentId, course.getId()));
            passedAssessmentLessonIds.addAll(quizAttemptRepository.findPassedQuizLessonIds(
                    currentStudentId, course.getId(), new BigDecimal("70.00")));
            passedRequiredAssessments = passedAssessmentLessonIds.size();
        }

        if (completedRequiredLessons < totalRequiredLessons) {
            unmet.add("Required lessons are not fully completed.");
        }
        if (passedRequiredAssessments < totalRequiredAssessments) {
            unmet.add("Required assessments are not fully passed.");
        }
        if (certificateRepository.existsByEnrollmentId(enrollmentId)) {
            unmet.add("A certificate already exists for this enrollment.");
        }

        boolean eligible = unmet.isEmpty();
        return CertificateEligibilityResponse.builder()
                .eligible(eligible)
                .status(eligible ? "ELIGIBLE" : "NOT_ELIGIBLE")
                .progressPercent(progressPercent)
                .completedRequiredLessons(completedRequiredLessons)
                .totalRequiredLessons(totalRequiredLessons)
                .passedRequiredAssessments(passedRequiredAssessments)
                .totalRequiredAssessments(totalRequiredAssessments)
                .unmetRequirements(unmet)
                .build();
    }
}
