package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.CertificateEligibilityResponse;
import com.ojtsu26.elearning.exception.BusinessException;
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
import com.ojtsu26.elearning.service.impl.CertificateEligibilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateEligibilityServiceTest {

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private CertificateRepository certificateRepository;

    private CertificateEligibilityServiceImpl service;
    private CourseEnrollment enrollment;

    @BeforeEach
    void setUp() {
        service = new CertificateEligibilityServiceImpl(
                enrollmentRepository,
                lessonRepository,
                submissionRepository,
                quizAttemptRepository,
                certificateRepository);
        User student = User.builder().id(1).status(UserStatus.ACTIVE).build();
        Course course = Course.builder().id(10).status(CourseStatus.APPROVED).build();
        enrollment = CourseEnrollment.builder()
                .id(20)
                .student(student)
                .course(course)
                .progressPercentage(new BigDecimal("100.00"))
                .isCompleted(true)
                .build();
    }

    @Test
    void missingEnrollmentThrowsBusinessException() {
        when(enrollmentRepository.findById(20)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.evaluateEligibility(20, 1));
    }

    @Test
    void enrollmentOwnedByAnotherStudentIsDenied() {
        when(enrollmentRepository.findById(20)).thenReturn(Optional.of(enrollment));

        assertThrows(BusinessException.class, () -> service.evaluateEligibility(20, 99));
    }

    @Test
    void incompleteRequiredLessonMakesStudentNotEligible() {
        when(enrollmentRepository.findById(20)).thenReturn(Optional.of(enrollment));
        when(lessonRepository.countRequiredContentLessons(10)).thenReturn(2L);
        when(lessonRepository.countCompletedRequiredContentLessons(10, 20)).thenReturn(1L);
        when(lessonRepository.countRequiredAssessmentLessons(10)).thenReturn(1L);
        when(submissionRepository.findPassedRequiredAssessmentLessonIds(1, 10))
                .thenReturn(List.of(101));
        when(quizAttemptRepository.findPassedQuizLessonIds(1, 10, new BigDecimal("70.00")))
                .thenReturn(List.of());

        CertificateEligibilityResponse response = service.evaluateEligibility(20, 1);

        assertFalse(response.isEligible());
        assertTrue(response.getUnmetRequirements().contains("Required lessons are not fully completed."));
    }

    @Test
    void progressCompleteButRequiredAssessmentNotPassedIsNotEligible() {
        when(enrollmentRepository.findById(20)).thenReturn(Optional.of(enrollment));
        when(lessonRepository.countRequiredContentLessons(10)).thenReturn(1L);
        when(lessonRepository.countCompletedRequiredContentLessons(10, 20)).thenReturn(1L);
        when(lessonRepository.countRequiredAssessmentLessons(10)).thenReturn(1L);
        when(submissionRepository.findPassedRequiredAssessmentLessonIds(1, 10))
                .thenReturn(List.of());
        when(quizAttemptRepository.findPassedQuizLessonIds(1, 10, new BigDecimal("70.00")))
                .thenReturn(List.of());

        CertificateEligibilityResponse response = service.evaluateEligibility(20, 1);

        assertFalse(response.isEligible());
        assertTrue(response.getUnmetRequirements().contains("Required assessments are not fully passed."));
    }

    @Test
    void allRequirementsMetIsEligible() {
        when(enrollmentRepository.findById(20)).thenReturn(Optional.of(enrollment));
        when(lessonRepository.countRequiredContentLessons(10)).thenReturn(2L);
        when(lessonRepository.countCompletedRequiredContentLessons(10, 20)).thenReturn(2L);
        when(lessonRepository.countRequiredAssessmentLessons(10)).thenReturn(1L);
        when(submissionRepository.findPassedRequiredAssessmentLessonIds(1, 10))
                .thenReturn(List.of(101));
        when(quizAttemptRepository.findPassedQuizLessonIds(1, 10, new BigDecimal("70.00")))
                .thenReturn(List.of());

        CertificateEligibilityResponse response = service.evaluateEligibility(20, 1);

        assertTrue(response.isEligible());
        assertEquals("ELIGIBLE", response.getStatus());
    }

    @Test
    void courseNotApprovedIsNotEligible() {
        enrollment.getCourse().setStatus(CourseStatus.HIDDEN);
        when(enrollmentRepository.findById(20)).thenReturn(Optional.of(enrollment));

        CertificateEligibilityResponse response = service.evaluateEligibility(20, 1);

        assertFalse(response.isEligible());
        assertTrue(response.getUnmetRequirements().contains("Course is not approved for certificate issuance."));
    }

    @Test
    void canonicalQuizAttemptCountsAsPassedAssessment() {
        when(enrollmentRepository.findById(20)).thenReturn(Optional.of(enrollment));
        when(lessonRepository.countRequiredContentLessons(10)).thenReturn(2L);
        when(lessonRepository.countCompletedRequiredContentLessons(10, 20)).thenReturn(2L);
        when(lessonRepository.countRequiredAssessmentLessons(10)).thenReturn(1L);
        when(submissionRepository.findPassedRequiredAssessmentLessonIds(1, 10))
                .thenReturn(List.of());
        when(quizAttemptRepository.findPassedQuizLessonIds(1, 10, new BigDecimal("70.00")))
                .thenReturn(List.of(101));

        CertificateEligibilityResponse response = service.evaluateEligibility(20, 1);

        assertTrue(response.isEligible());
        assertEquals(1L, response.getPassedRequiredAssessments());
    }

    @Test
    void legacyAndCanonicalRecordsForSameLessonAreNotDoubleCounted() {
        when(enrollmentRepository.findById(20)).thenReturn(Optional.of(enrollment));
        when(lessonRepository.countRequiredContentLessons(10)).thenReturn(2L);
        when(lessonRepository.countCompletedRequiredContentLessons(10, 20)).thenReturn(2L);
        when(lessonRepository.countRequiredAssessmentLessons(10)).thenReturn(1L);
        when(submissionRepository.findPassedRequiredAssessmentLessonIds(1, 10))
                .thenReturn(List.of(101));
        when(quizAttemptRepository.findPassedQuizLessonIds(1, 10, new BigDecimal("70.00")))
                .thenReturn(List.of(101));

        CertificateEligibilityResponse response = service.evaluateEligibility(20, 1);

        assertTrue(response.isEligible());
        assertEquals(1L, response.getPassedRequiredAssessments());
    }
}
