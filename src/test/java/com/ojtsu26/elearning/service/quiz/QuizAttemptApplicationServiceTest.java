package com.ojtsu26.elearning.service.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.*;
import com.ojtsu26.elearning.model.enums.*;
import com.ojtsu26.elearning.repository.*;
import com.ojtsu26.elearning.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAttemptApplicationServiceTest {
    @Mock private QuizRepository quizRepository;
    @Mock private QuizAttemptRepository attemptRepository;
    @Mock private QuizAnswerRepository answerRepository;
    @Mock private CourseEnrollmentRepository enrollmentRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private QuizQuestionAssignmentService assignmentService;
    @Mock private QuizGradingService gradingService;

    private QuizAttemptApplicationService service;
    private User student;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        service = new QuizAttemptApplicationService(
                quizRepository,
                attemptRepository,
                answerRepository,
                enrollmentRepository,
                currentUserService,
                assignmentService,
                gradingService,
                new ObjectMapper());
        student = User.builder().id(1).role(Role.STUDENT).build();
        User teacher = User.builder().id(9).role(Role.TEACHER).build();
        Course course = Course.builder().id(2).instructor(teacher).build();
        Lesson lesson = Lesson.builder().id(3).course(course).type(LessonType.QUIZ).build();
        quiz = Quiz.builder()
                .id(4)
                .lesson(lesson)
                .status(QuizStatus.PUBLISHED)
                .maxAttempts(1)
                .build();
        lenient().when(currentUserService.getCurrentUser()).thenReturn(student);
    }

    @Test
    void resumeReturnsExactlyPersistedQuestionOrderWithoutResampling() {
        QuizAttempt draft = QuizAttempt.builder()
                .id(5).quiz(quiz).student(student).status(QuizAttemptStatus.DRAFT).build();
        List<QuizAttemptQuestion> assigned = List.of(
                assignment(draft, 11, 1),
                assignment(draft, 10, 2));
        when(quizRepository.findByIdForAttemptStart(4)).thenReturn(Optional.of(quiz));
        when(enrollmentRepository.existsByStudentIdAndCourseId(1, 2)).thenReturn(true);
        when(attemptRepository.findTopByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(
                4, 1, QuizAttemptStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(assignmentService.loadAssigned(5)).thenReturn(assigned);
        when(answerRepository.findByAttemptId(5)).thenReturn(List.of());

        QuizAttemptApplicationService.AttemptSession result = service.startOrResume(4);

        assertThat(result.questions()).extracting(item -> item.getQuestion().getId())
                .containsExactly(11, 10);
        verify(assignmentService, never()).assign(any());
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void rejectsAnswerForQuestionNotAssignedToAttempt() {
        QuizAttempt draft = QuizAttempt.builder()
                .id(5).quiz(quiz).student(student).status(QuizAttemptStatus.DRAFT).build();
        when(attemptRepository.findByIdWithQuizCourse(5)).thenReturn(Optional.of(draft));
        when(assignmentService.loadAssigned(5)).thenReturn(List.of(assignment(draft, 10, 1)));

        assertThatThrownBy(() -> service.saveTextAnswers(5, Map.of(99, "A")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not assigned");
        verify(answerRepository, never()).save(any());
    }

    @Test
    void repeatedSubmitReturnsExistingGradeWithoutGradingAgain() {
        QuizAttempt graded = QuizAttempt.builder()
                .id(5).quiz(quiz).student(student).status(QuizAttemptStatus.GRADED).build();
        when(attemptRepository.findByIdWithQuizCourse(5)).thenReturn(Optional.of(graded));
        when(assignmentService.loadAssigned(5)).thenReturn(List.of());
        when(answerRepository.findByAttemptId(5)).thenReturn(List.of());

        QuizAttemptApplicationService.AttemptSession result =
                service.submitTextAnswers(5, Map.of());

        assertThat(result.attempt()).isSameAs(graded);
        verifyNoInteractions(gradingService);
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void submitGradesUsingAllAnswersPersistedByTheCurrentRequest() {
        QuizAttempt draft = QuizAttempt.builder()
                .id(5).quiz(quiz).student(student).status(QuizAttemptStatus.DRAFT).build();
        List<QuizAttemptQuestion> assigned = java.util.stream.IntStream.range(0, 10)
                .mapToObj(index -> assignment(draft, 100 + index, index + 1))
                .toList();
        List<QuizAnswer> eightPreviouslySaved = assigned.stream()
                .limit(8)
                .map(item -> QuizAnswer.builder()
                        .attempt(draft)
                        .question(item.getQuestion())
                        .selectedOptionsJson("[0]")
                        .build())
                .toList();
        Map<Integer, List<Integer>> submitted = new LinkedHashMap<>();
        assigned.forEach(item -> submitted.put(item.getQuestion().getId(), List.of(0)));

        when(attemptRepository.findByIdWithQuizCourse(5)).thenReturn(Optional.of(draft));
        when(assignmentService.loadAssigned(5)).thenReturn(assigned);
        when(answerRepository.findByAttemptId(5)).thenReturn(eightPreviouslySaved);
        when(answerRepository.save(any(QuizAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gradingService.grade(eq(assigned), argThat(answers -> answers.size() == 10)))
                .thenReturn(new QuizGradingService.GradeResult(
                        BigDecimal.TEN, BigDecimal.TEN, new BigDecimal("100.00")));

        QuizAttemptApplicationService.AttemptSession result =
                service.submitSelectedAnswers(5, submitted);

        assertThat(result.attempt().getScore()).isEqualByComparingTo("100.00");
        assertThat(result.answers()).hasSize(10);
        verify(answerRepository, times(1)).findByAttemptId(5);
        verify(gradingService).grade(eq(assigned), argThat(answers -> answers.size() == 10));
    }

    @Test
    void rejectsManualSubmissionWhileRequiredQuestionsAreUnanswered() {
        quiz.setDurationMinutes(25);
        QuizAttempt draft = QuizAttempt.builder()
                .id(5)
                .quiz(quiz)
                .student(student)
                .status(QuizAttemptStatus.DRAFT)
                .startedAt(LocalDateTime.now())
                .build();
        List<QuizAttemptQuestion> assigned = List.of(
                assignment(draft, 10, 1),
                assignment(draft, 11, 2));
        Map<Integer, List<Integer>> submitted = new LinkedHashMap<>();
        submitted.put(10, List.of(0));
        submitted.put(11, List.of());

        when(attemptRepository.findByIdWithQuizCourse(5)).thenReturn(Optional.of(draft));
        when(assignmentService.loadAssigned(5)).thenReturn(assigned);
        when(answerRepository.findByAttemptId(5)).thenReturn(List.of());
        when(answerRepository.save(any(QuizAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.submitSelectedAnswers(5, submitted))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("All questions must be answered");
        verifyNoInteractions(gradingService);
    }

    @Test
    void allowsAutomaticSubmissionWithUnansweredQuestionsAfterTimeExpires() {
        quiz.setDurationMinutes(25);
        QuizAttempt draft = QuizAttempt.builder()
                .id(5)
                .quiz(quiz)
                .student(student)
                .status(QuizAttemptStatus.DRAFT)
                .startedAt(LocalDateTime.now().minusMinutes(26))
                .build();
        List<QuizAttemptQuestion> assigned = List.of(
                assignment(draft, 10, 1),
                assignment(draft, 11, 2));
        Map<Integer, List<Integer>> submitted = new LinkedHashMap<>();
        submitted.put(10, List.of(0));
        submitted.put(11, List.of());

        when(attemptRepository.findByIdWithQuizCourse(5)).thenReturn(Optional.of(draft));
        when(assignmentService.loadAssigned(5)).thenReturn(assigned);
        when(answerRepository.findByAttemptId(5)).thenReturn(List.of());
        when(answerRepository.save(any(QuizAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(gradingService.grade(eq(assigned), anyList()))
                .thenReturn(new QuizGradingService.GradeResult(
                        BigDecimal.ONE, new BigDecimal("2.00"), new BigDecimal("50.00")));

        QuizAttemptApplicationService.AttemptSession result =
                service.submitSelectedAnswers(5, submitted);

        assertThat(result.attempt().getStatus()).isEqualTo(QuizAttemptStatus.GRADED);
        assertThat(result.attempt().getScore()).isEqualByComparingTo("50.00");
        verify(gradingService).grade(eq(assigned), anyList());
    }

    @Test
    void openingGradedAttemptRepairsStaleStoredPercentage() {
        QuizAttempt graded = QuizAttempt.builder()
                .id(5)
                .quiz(quiz)
                .student(student)
                .status(QuizAttemptStatus.GRADED)
                .score(new BigDecimal("80.00"))
                .totalPoints(BigDecimal.TEN)
                .build();
        QuizAttemptQuestion assigned = assignment(graded, 10, 1);
        QuizAnswer answer = QuizAnswer.builder()
                .attempt(graded)
                .question(assigned.getQuestion())
                .selectedOptionsJson("[0]")
                .build();
        when(attemptRepository.findByIdWithQuizCourse(5)).thenReturn(Optional.of(graded));
        when(assignmentService.loadAssigned(5)).thenReturn(List.of(assigned));
        when(answerRepository.findByAttemptId(5)).thenReturn(List.of(answer));
        when(gradingService.grade(List.of(assigned), List.of(answer)))
                .thenReturn(new QuizGradingService.GradeResult(
                        BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("100.00")));

        QuizAttemptApplicationService.AttemptSession result = service.getOwnedAttempt(5);

        assertThat(result.attempt().getScore()).isEqualByComparingTo("100.00");
        verify(attemptRepository).save(graded);
    }

    @Test
    void createsEveryRequestedAttemptEvenWhenConfiguredMaximumIsOne() {
        when(quizRepository.findByIdForAttemptStart(4)).thenReturn(Optional.of(quiz));
        when(enrollmentRepository.existsByStudentIdAndCourseId(1, 2)).thenReturn(true);
        when(attemptRepository.findTopByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(
                4, 1, QuizAttemptStatus.DRAFT)).thenReturn(Optional.empty());
        int[] nextAttemptId = {6};
        when(attemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> {
            QuizAttempt attempt = invocation.getArgument(0);
            attempt.setId(nextAttemptId[0]++);
            return attempt;
        });
        when(assignmentService.assign(any(QuizAttempt.class))).thenReturn(List.of());
        when(answerRepository.findByAttemptId(anyInt())).thenReturn(List.of());

        QuizAttemptApplicationService.AttemptSession first = service.startOrResume(4);
        QuizAttemptApplicationService.AttemptSession second = service.startOrResume(4);

        assertThat(first.attempt().getId()).isEqualTo(6);
        assertThat(second.attempt().getId()).isEqualTo(7);
        assertThat(first.attempt().getStatus()).isEqualTo(QuizAttemptStatus.DRAFT);
        assertThat(second.attempt().getStatus()).isEqualTo(QuizAttemptStatus.DRAFT);
        assertThat(second.attempt().getQuiz()).isSameAs(quiz);
        assertThat(second.attempt().getStudent()).isSameAs(student);
        verify(attemptRepository, times(2)).save(any(QuizAttempt.class));
        verify(assignmentService, times(2)).assign(any(QuizAttempt.class));
    }

    @Test
    void rejectsAttemptOwnedByAnotherStudent() {
        QuizAttempt foreign = QuizAttempt.builder()
                .id(5)
                .quiz(quiz)
                .student(User.builder().id(99).build())
                .status(QuizAttemptStatus.DRAFT)
                .build();
        when(attemptRepository.findByIdWithQuizCourse(5)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.saveTextAnswers(5, Map.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("access denied");
    }

    private QuizAttemptQuestion assignment(
            QuizAttempt attempt, int questionId, int displayOrder) {
        return QuizAttemptQuestion.builder()
                .attempt(attempt)
                .question(Question.builder().id(questionId).build())
                .displayOrder(displayOrder)
                .build();
    }
}
