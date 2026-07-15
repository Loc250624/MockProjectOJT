package com.ojtsu26.elearning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.GradePayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.OptionPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.TestcasePayload;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.CodeJudgeResult;
import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.Testcase;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CodeJudgeStatus;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.QuestionType;
import com.ojtsu26.elearning.model.enums.QuizStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.repository.CodeJudgeResultRepository;
import com.ojtsu26.elearning.repository.CodingAssignmentRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.GradeFeedbackRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizAnswerRepository;
import com.ojtsu26.elearning.repository.QuizAttemptRepository;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.TestcaseRepository;
import com.ojtsu26.elearning.service.impl.AssessmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TeacherAssessmentServiceTest {

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private QuizAnswerRepository quizAnswerRepository;
    @Mock
    private CodingAssignmentRepository codingAssignmentRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private TestcaseRepository testcaseRepository;
    @Mock
    private CodeJudgeResultRepository codeJudgeResultRepository;
    @Mock
    private GradeFeedbackRepository gradeFeedbackRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private CurrentUserService currentUserService;
    private FakeCodeJudgeAdapter codeJudgeAdapter;

    private AssessmentServiceImpl service;
    private User teacher;
    private User otherTeacher;

    @BeforeEach
    void setUp() {
        codeJudgeAdapter = new FakeCodeJudgeAdapter();
        service = new AssessmentServiceImpl(
                quizRepository,
                questionRepository,
                quizAttemptRepository,
                quizAnswerRepository,
                codingAssignmentRepository,
                submissionRepository,
                testcaseRepository,
                codeJudgeResultRepository,
                gradeFeedbackRepository,
                lessonRepository,
                courseRepository,
                enrollmentRepository,
                currentUserService,
                codeJudgeAdapter,
                new ObjectMapper()
        );
        teacher = User.builder().id(10).role(Role.TEACHER).fullName("Teacher A").build();
        otherTeacher = User.builder().id(20).role(Role.TEACHER).fullName("Teacher B").build();
        lenient().when(gradeFeedbackRepository.findTopBySubmissionIdOrderByGradedAtDesc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void gradeSubmissionRejectsSubmissionOwnedByAnotherTeacher() {
        Submission foreignSubmission = submissionFor(otherTeacher, new BigDecimal("100.00"));
        GradePayload payload = gradePayload(new BigDecimal("80.00"));
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(submissionRepository.findByIdWithAssignmentCourse(900)).thenReturn(Optional.of(foreignSubmission));
        when(courseRepository.findById(200)).thenReturn(Optional.of(foreignSubmission.getAssignment().getLesson().getCourse()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.gradeSubmission(900, payload));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(gradeFeedbackRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void gradeSubmissionRejectsScoreAboveAssignmentMaxScore() {
        Submission ownSubmission = submissionFor(teacher, new BigDecimal("50.00"));
        GradePayload payload = gradePayload(new BigDecimal("75.00"));
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(submissionRepository.findByIdWithAssignmentCourse(901)).thenReturn(Optional.of(ownSubmission));
        when(courseRepository.findById(100)).thenReturn(Optional.of(ownSubmission.getAssignment().getLesson().getCourse()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.gradeSubmission(901, payload));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        verify(gradeFeedbackRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void gradeSubmissionPersistsFeedbackForOwnedSubmission() {
        Submission ownSubmission = submissionFor(teacher, new BigDecimal("100.00"));
        GradePayload payload = gradePayload(new BigDecimal("88.50"));
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(submissionRepository.findByIdWithAssignmentCourse(902)).thenReturn(Optional.of(ownSubmission));
        when(courseRepository.findById(100)).thenReturn(Optional.of(ownSubmission.getAssignment().getLesson().getCourse()));

        service.gradeSubmission(902, payload);

        assertThat(ownSubmission.getScore()).isEqualByComparingTo("88.50");
        assertThat(ownSubmission.getTeacherFeedback()).isEqualTo("Good work");
        assertThat(ownSubmission.getStatus()).isEqualTo(SubmissionStatus.GRADED);
        verify(gradeFeedbackRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void gradeSubmissionCanSaveDraftWithoutPublishingToStudent() {
        Submission ownSubmission = submissionFor(teacher, new BigDecimal("100.00"));
        GradePayload payload = gradePayload(new BigDecimal("72.25"));
        payload.setPublish(false);
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(submissionRepository.findByIdWithAssignmentCourse(903)).thenReturn(Optional.of(ownSubmission));
        when(courseRepository.findById(100)).thenReturn(Optional.of(ownSubmission.getAssignment().getLesson().getCourse()));

        var response = service.gradeSubmission(903, payload);

        assertThat(ownSubmission.getScore()).isEqualByComparingTo("72.25");
        assertThat(ownSubmission.getTeacherFeedback()).isEqualTo("Good work");
        assertThat(ownSubmission.getStatus()).isEqualTo(SubmissionStatus.PENDING_REVIEW);
        assertThat(response.getReleased()).isFalse();
        verify(gradeFeedbackRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteQuizWithAttemptsArchivesInsteadOfDeletingHistory() {
        Quiz quiz = quizFor(teacher);
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(quizRepository.findByIdWithCourse(301)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(100)).thenReturn(Optional.of(quiz.getLesson().getCourse()));
        when(quizAttemptRepository.countByQuizId(301)).thenReturn(2L);

        service.deleteTeacherQuiz(301);

        assertThat(quiz.getStatus()).isEqualTo(QuizStatus.ARCHIVED);
        verify(quizRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void publishQuizRejectsEmptyQuestionSet() {
        Quiz quiz = quizFor(teacher);
        QuizPayload payload = quizPayload(QuizStatus.PUBLISHED);
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(quizRepository.findByIdWithCourse(301)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(100)).thenReturn(Optional.of(quiz.getLesson().getCourse()));
        when(questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(301)).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateTeacherQuiz(301, payload));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(quiz.getStatus()).isEqualTo(QuizStatus.DRAFT);
    }

    @Test
    void singleChoiceQuestionRejectsMultipleCorrectOptions() {
        Quiz quiz = quizFor(teacher);
        QuestionPayload payload = questionPayload(true, true);
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(quizRepository.findByIdWithCourse(301)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(100)).thenReturn(Optional.of(quiz.getLesson().getCourse()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createTeacherQuestion(301, payload));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        verify(questionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteQuestionWithAnswersIsBlockedToPreserveAttemptHistory() {
        Quiz quiz = quizFor(teacher);
        Question question = Question.builder().id(401).quiz(quiz).questionText("Existing?").build();
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(questionRepository.findById(401)).thenReturn(Optional.of(question));
        when(quizRepository.findByIdWithCourse(301)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(100)).thenReturn(Optional.of(quiz.getLesson().getCourse()));
        when(quizAnswerRepository.countByQuestionId(401)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.deleteTeacherQuestion(401));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        verify(questionRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createTestcaseRejectsForeignAssignment() {
        CodingAssignment assignment = assignmentFor(otherTeacher, new BigDecimal("100.00"));
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(codingAssignmentRepository.findByIdWithCourse(400)).thenReturn(Optional.of(assignment));
        when(courseRepository.findById(200)).thenReturn(Optional.of(assignment.getLesson().getCourse()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createTeacherTestcase(400, testcasePayload("1", "1", BigDecimal.ONE, false)));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(testcaseRepository, never()).save(any());
    }

    @Test
    void updateTestcaseRejectsNonPositiveWeight() {
        Testcase testcase = Testcase.builder()
                .id(701)
                .assignment(assignmentFor(teacher, new BigDecimal("100.00")))
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(testcaseRepository.findByIdWithAssignmentCourse(701)).thenReturn(Optional.of(testcase));
        when(courseRepository.findById(100)).thenReturn(Optional.of(testcase.getAssignment().getLesson().getCourse()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateTeacherTestcase(701, testcasePayload("1", "1", BigDecimal.ZERO, false)));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        verify(testcaseRepository, never()).save(any());
    }

    @Test
    void judgeSubmissionCalculatesServerScoreFromWeightedTestcases() {
        Submission ownSubmission = submissionFor(teacher, new BigDecimal("100.00"));
        List<Testcase> testcases = List.of(
                Testcase.builder().id(701).assignment(ownSubmission.getAssignment()).points(new BigDecimal("2.00")).isHidden(false).build(),
                Testcase.builder().id(702).assignment(ownSubmission.getAssignment()).points(new BigDecimal("3.00")).isHidden(true).build()
        );
        codeJudgeAdapter.setOutcome(new CodeJudgeAdapter.JudgeOutcome(
                CodeJudgeStatus.FAILED,
                2,
                1,
                List.of(new CodeJudgeAdapter.JudgeCaseOutcome(701, true),
                        new CodeJudgeAdapter.JudgeCaseOutcome(702, false)),
                "One testcase failed.",
                42L
        ));
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(submissionRepository.findByIdWithAssignmentCourse(904)).thenReturn(Optional.of(ownSubmission));
        when(courseRepository.findById(100)).thenReturn(Optional.of(ownSubmission.getAssignment().getLesson().getCourse()));
        when(testcaseRepository.findByAssignmentIdOrderByIdAsc(400)).thenReturn(testcases);
        when(codeJudgeResultRepository.save(any(CodeJudgeResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gradeFeedbackRepository.findTopBySubmissionIdOrderByGradedAtDesc(anyInt())).thenReturn(Optional.empty());

        var response = service.judgeSubmission(904);

        assertThat(ownSubmission.getStatus()).isEqualTo(SubmissionStatus.RETURNED);
        assertThat(ownSubmission.getScore()).isEqualByComparingTo("40.00");
        assertThat(response.getJudgeStatus()).isEqualTo(CodeJudgeStatus.FAILED);
        assertThat(response.getPassedTests()).isEqualTo(1);
        verify(codeJudgeResultRepository).save(any(CodeJudgeResult.class));
    }

    @Test
    void unavailableJudgeDoesNotOverwriteScoreOrSubmissionStatus() {
        Submission ownSubmission = submissionFor(teacher, new BigDecimal("100.00"));
        ownSubmission.setScore(new BigDecimal("75.00"));
        List<Testcase> testcases = List.of(
                Testcase.builder().id(701).assignment(ownSubmission.getAssignment()).points(BigDecimal.ONE).isHidden(false).build()
        );
        codeJudgeAdapter.setOutcome(new CodeJudgeAdapter.JudgeOutcome(
                CodeJudgeStatus.UNAVAILABLE,
                1,
                0,
                List.of(),
                "Code judge is not configured.",
                0L
        ));
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(submissionRepository.findByIdWithAssignmentCourse(905)).thenReturn(Optional.of(ownSubmission));
        when(courseRepository.findById(100)).thenReturn(Optional.of(ownSubmission.getAssignment().getLesson().getCourse()));
        when(testcaseRepository.findByAssignmentIdOrderByIdAsc(400)).thenReturn(testcases);
        when(codeJudgeResultRepository.save(any(CodeJudgeResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gradeFeedbackRepository.findTopBySubmissionIdOrderByGradedAtDesc(anyInt())).thenReturn(Optional.empty());

        var response = service.judgeSubmission(905);

        assertThat(ownSubmission.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(ownSubmission.getScore()).isEqualByComparingTo("75.00");
        assertThat(response.getJudgeStatus()).isEqualTo(CodeJudgeStatus.UNAVAILABLE);
    }

    private Submission submissionFor(User instructor, BigDecimal maxScore) {
        CodingAssignment assignment = assignmentFor(instructor, maxScore);
        return Submission.builder()
                .id(500)
                .assignment(assignment)
                .lesson(assignment.getLesson())
                .status(SubmissionStatus.SUBMITTED)
                .build();
    }

    private CodingAssignment assignmentFor(User instructor, BigDecimal maxScore) {
        Course course = Course.builder()
                .id(instructor.getId().equals(teacher.getId()) ? 100 : 200)
                .title("Course")
                .instructor(instructor)
                .build();
        Lesson lesson = Lesson.builder()
                .id(300)
                .title("Lesson")
                .type(LessonType.CODING)
                .course(course)
                .build();
        CodingAssignment assignment = CodingAssignment.builder()
                .id(400)
                .title("Assignment")
                .lesson(lesson)
                .maxScore(maxScore)
                .build();
        return assignment;
    }

    private GradePayload gradePayload(BigDecimal score) {
        GradePayload payload = new GradePayload();
        payload.setScore(score);
        payload.setFeedback(" Good work ");
        return payload;
    }

    private Quiz quizFor(User instructor) {
        Course course = Course.builder()
                .id(instructor.getId().equals(teacher.getId()) ? 100 : 200)
                .title("Course")
                .instructor(instructor)
                .build();
        Lesson lesson = Lesson.builder()
                .id(300)
                .title("Quiz Lesson")
                .type(LessonType.QUIZ)
                .course(course)
                .build();
        return Quiz.builder()
                .id(301)
                .title("Quiz")
                .lesson(lesson)
                .status(QuizStatus.DRAFT)
                .maxAttempts(1)
                .passingScore(new BigDecimal("70.00"))
                .durationMinutes(30)
                .build();
    }

    private QuizPayload quizPayload(QuizStatus status) {
        QuizPayload payload = new QuizPayload();
        payload.setLessonId(300);
        payload.setTitle("Quiz");
        payload.setDurationMinutes(30);
        payload.setMaxAttempts(1);
        payload.setPassingScore(new BigDecimal("70.00"));
        payload.setStatus(status);
        return payload;
    }

    private QuestionPayload questionPayload(boolean firstCorrect, boolean secondCorrect) {
        QuestionPayload payload = new QuestionPayload();
        payload.setContent("Pick one");
        payload.setQuestionType(QuestionType.SINGLE_CHOICE);
        payload.setPoints(BigDecimal.ONE);
        payload.setOptions(List.of(option("A", firstCorrect), option("B", secondCorrect)));
        return payload;
    }

    private OptionPayload option(String content, boolean correct) {
        OptionPayload payload = new OptionPayload();
        payload.setContent(content);
        payload.setCorrect(correct);
        return payload;
    }

    private TestcasePayload testcasePayload(String input, String expectedOutput, BigDecimal points, boolean hidden) {
        TestcasePayload payload = new TestcasePayload();
        payload.setInput(input);
        payload.setExpectedOutput(expectedOutput);
        payload.setPoints(points);
        payload.setHidden(hidden);
        return payload;
    }
}
