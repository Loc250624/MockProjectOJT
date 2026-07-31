package com.ojtsu26.elearning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.QuizAttempt;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.QuizAttemptStatus;
import com.ojtsu26.elearning.model.enums.QuizStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizAnswerRepository;
import com.ojtsu26.elearning.repository.QuizAttemptRepository;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.service.impl.AssessmentServiceImpl;
import com.ojtsu26.elearning.service.quiz.QuizAttemptApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentQuizOverviewServiceTest {

    @Mock private QuizRepository quizRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuizAttemptRepository quizAttemptRepository;
    @Mock private QuizAnswerRepository quizAnswerRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseEnrollmentRepository enrollmentRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private QuizAttemptApplicationService quizAttemptApplicationService;
    @Mock private LessonProgressRepository lessonProgressRepository;

    private AssessmentServiceImpl service;
    private User student;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        service = new AssessmentServiceImpl(
                quizRepository,
                questionRepository,
                quizAttemptRepository,
                quizAnswerRepository,
                lessonRepository,
                courseRepository,
                enrollmentRepository,
                currentUserService,
                new ObjectMapper());
        student = User.builder().id(8).build();
        Course course = Course.builder().id(100).title("Java").build();
        Lesson lesson = Lesson.builder()
                .id(200)
                .title("OOP check")
                .type(LessonType.QUIZ)
                .course(course)
                .build();
        quiz = Quiz.builder()
                .id(300)
                .title("Java OOP Quiz")
                .description("Check your understanding before continuing.")
                .durationMinutes(20)
                .passingScore(new BigDecimal("70.00"))
                .status(QuizStatus.PUBLISHED)
                .lesson(lesson)
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(student);
    }

    @Test
    void overviewResumesDraftWithoutCreatingAnotherAttempt() {
        stubOverviewAccess();
        QuizAttempt draft = QuizAttempt.builder()
                .id(500)
                .quiz(quiz)
                .student(student)
                .status(QuizAttemptStatus.DRAFT)
                .build();
        when(quizAttemptRepository
                .findTopByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(
                        300, 8, QuizAttemptStatus.DRAFT))
                .thenReturn(Optional.of(draft));

        var result = service.getStudentQuizOverview(100, 200);

        assertEquals(500, result.getAttemptId());
        assertEquals(QuizAttemptStatus.DRAFT, result.getAttemptStatus());
        assertEquals("Java OOP Quiz", result.getTitle());
        verify(quizAttemptRepository, never()).save(any());
    }

    @Test
    void overviewUsesLatestSubmittedAttemptOnlyWhenNoDraftExists() {
        stubOverviewAccess();
        QuizAttempt graded = QuizAttempt.builder()
                .id(501)
                .quiz(quiz)
                .student(student)
                .status(QuizAttemptStatus.GRADED)
                .score(new BigDecimal("85.00"))
                .build();
        when(quizAttemptRepository
                .findTopByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(
                        300, 8, QuizAttemptStatus.DRAFT))
                .thenReturn(Optional.empty());
        when(quizAttemptRepository
                .findTopByQuizIdAndStudentIdOrderByStartedAtDesc(300, 8))
                .thenReturn(Optional.of(graded));

        var result = service.getStudentQuizOverview(100, 200);

        assertEquals(501, result.getAttemptId());
        assertEquals(QuizAttemptStatus.GRADED, result.getAttemptStatus());
        assertEquals(new BigDecimal("85.00"), result.getScore());
        assertEquals(10, result.getQuestionCount());
        verify(quizAttemptRepository, never()).save(any());
    }

    @Test
    void overviewDisplaysScoreReconciledByCanonicalAttemptService() {
        stubOverviewAccess();
        ReflectionTestUtils.setField(
                service, "quizAttemptApplicationService", quizAttemptApplicationService);
        QuizAttempt staleAttempt = QuizAttempt.builder()
                .id(501)
                .quiz(quiz)
                .student(student)
                .status(QuizAttemptStatus.GRADED)
                .score(new BigDecimal("80.00"))
                .build();
        QuizAttempt reconciledAttempt = QuizAttempt.builder()
                .id(501)
                .quiz(quiz)
                .student(student)
                .status(QuizAttemptStatus.GRADED)
                .score(new BigDecimal("100.00"))
                .build();
        when(quizAttemptRepository
                .findTopByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(
                        300, 8, QuizAttemptStatus.DRAFT))
                .thenReturn(Optional.empty());
        when(quizAttemptRepository
                .findTopByQuizIdAndStudentIdOrderByStartedAtDesc(300, 8))
                .thenReturn(Optional.of(staleAttempt));
        when(quizAttemptApplicationService.getOwnedAttempt(501))
                .thenReturn(new QuizAttemptApplicationService.AttemptSession(
                        reconciledAttempt, List.of(), Map.of()));

        var result = service.getStudentQuizOverview(100, 200);

        assertEquals(new BigDecimal("100.00"), result.getScore());
    }

    @Test
    void passingDedicatedAttemptCompletesQuizLessonProgress() {
        ReflectionTestUtils.setField(
                service, "quizAttemptApplicationService", quizAttemptApplicationService);
        ReflectionTestUtils.setField(
                service, "lessonProgressRepository", lessonProgressRepository);
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .id(20)
                .student(student)
                .course(quiz.getLesson().getCourse())
                .build();
        QuizAttempt graded = QuizAttempt.builder()
                .id(502)
                .quiz(quiz)
                .student(student)
                .status(QuizAttemptStatus.GRADED)
                .score(new BigDecimal("80.00"))
                .build();
        var session = new QuizAttemptApplicationService.AttemptSession(
                graded, List.of(), Map.of());
        when(quizAttemptApplicationService.submitSelectedAnswers(502, Map.of()))
                .thenReturn(session);
        when(enrollmentRepository.findByStudentIdAndCourseIdForUpdate(8, 100))
                .thenReturn(Optional.of(enrollment));
        when(lessonProgressRepository
                .findByEnrollmentIdAndLessonIdForUpdate(20, 200))
                .thenReturn(Optional.empty());
        service.submitQuizAttempt(
                502, new com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizDraftPayload());

        verify(lessonProgressRepository).save(any(LessonProgress.class));
    }

    private void stubOverviewAccess() {
        when(quizRepository.findByLessonId(200)).thenReturn(Optional.of(quiz));
        when(enrollmentRepository.existsByStudentIdAndCourseId(8, 100)).thenReturn(true);
    }
}
