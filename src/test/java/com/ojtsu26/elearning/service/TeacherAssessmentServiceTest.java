package com.ojtsu26.elearning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.AssignmentPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.AssignmentSubmissionPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.GradePayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.OptionPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuestionPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.TestcasePayload;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.AssignmentAssignee;
import com.ojtsu26.elearning.model.entity.CodeJudgeResult;
import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.Testcase;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CodeJudgeStatus;
import com.ojtsu26.elearning.model.enums.AssignmentType;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.NotificationType;
import com.ojtsu26.elearning.model.enums.QuestionType;
import com.ojtsu26.elearning.model.enums.QuestionReviewStatus;
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
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;

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
    @Mock
    private NotificationService notificationService;
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
                notificationService,
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
    void archiveQuizArchivesWithoutDeletingHistory() {
        Quiz quiz = quizFor(teacher);
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(quizRepository.findByIdWithCourse(301)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(100)).thenReturn(Optional.of(quiz.getLesson().getCourse()));

        service.archiveTeacherQuiz(301);

        assertThat(quiz.getStatus()).isEqualTo(QuizStatus.ARCHIVED);
        verify(quizRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteQuizWithAttemptsIsBlockedToPreserveHistory() {
        when(quizAttemptRepository.countByQuizId(301)).thenReturn(2L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.deleteTeacherQuiz(301));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        verify(quizRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(quizRepository, never()).deleteById(org.mockito.ArgumentMatchers.any());
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
    void createQuizRejectsNonQuizLesson() {
        Lesson codingLesson = lessonFor(teacher, LessonType.CODING);
        QuizPayload payload = quizPayload(QuizStatus.DRAFT);
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findById(100)).thenReturn(Optional.of(codingLesson.getCourse()));
        when(lessonRepository.findByIdWithCourseAndAssessment(300)).thenReturn(Optional.of(codingLesson));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createTeacherQuiz(100, payload));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        verify(quizRepository, never()).save(any());
    }

    @Test
    void createQuizRejectsDuplicateForLesson() {
        Lesson quizLesson = lessonFor(teacher, LessonType.QUIZ);
        QuizPayload payload = quizPayload(QuizStatus.DRAFT);
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findById(100)).thenReturn(Optional.of(quizLesson.getCourse()));
        when(lessonRepository.findByIdWithCourseAndAssessment(300)).thenReturn(Optional.of(quizLesson));
        when(quizRepository.existsByLessonId(300)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createTeacherQuiz(100, payload));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ASSESSMENT_CONTENT_ALREADY_EXISTS);
        verify(quizRepository, never()).save(any());
    }

    @Test
    void createCodingAssignmentPersistsForOwnedCodingLesson() {
        Lesson codingLesson = lessonFor(teacher, LessonType.CODING);
        AssignmentPayload payload = assignmentPayload();
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findById(100)).thenReturn(Optional.of(codingLesson.getCourse()));
        when(lessonRepository.findByIdWithCourseAndAssessment(300)).thenReturn(Optional.of(codingLesson));
        when(codingAssignmentRepository.existsByLessonId(300)).thenReturn(false);
        when(codingAssignmentRepository.save(any(CodingAssignment.class))).thenAnswer(invocation -> {
            CodingAssignment saved = invocation.getArgument(0);
            saved.setId(400);
            return saved;
        });

        var response = service.createTeacherAssignment(100, payload);

        assertThat(response.getId()).isEqualTo(400);
        assertThat(response.getLessonId()).isEqualTo(300);
        assertThat(response.getTitle()).isEqualTo("Two Sum");
        assertThat(response.getProblemStatement()).isEqualTo("Return indices.");
        assertThat(response.getAllowedLanguages()).isEqualTo("java");
        verify(codingAssignmentRepository).save(any(CodingAssignment.class));
    }

    @Test
    void createCodingAssignmentRejectsQuizLesson() {
        Lesson quizLesson = lessonFor(teacher, LessonType.QUIZ);
        AssignmentPayload payload = assignmentPayload();
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findById(100)).thenReturn(Optional.of(quizLesson.getCourse()));
        when(lessonRepository.findByIdWithCourseAndAssessment(300)).thenReturn(Optional.of(quizLesson));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createTeacherAssignment(100, payload));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        verify(codingAssignmentRepository, never()).save(any());
    }

    @Test
    void createCodingAssignmentRejectsDuplicateForLesson() {
        Lesson codingLesson = lessonFor(teacher, LessonType.CODING);
        AssignmentPayload payload = assignmentPayload();
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findById(100)).thenReturn(Optional.of(codingLesson.getCourse()));
        when(lessonRepository.findByIdWithCourseAndAssessment(300)).thenReturn(Optional.of(codingLesson));
        when(codingAssignmentRepository.existsByLessonId(300)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createTeacherAssignment(100, payload));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ASSESSMENT_CONTENT_ALREADY_EXISTS);
        verify(codingAssignmentRepository, never()).save(any());
    }

    @Test
    void deleteAssignmentWithSubmissionsArchivesInsteadOfDeletingHistory() {
        CodingAssignment assignment = assignmentFor(teacher, new BigDecimal("100.00"));
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(codingAssignmentRepository.findByIdWithCourse(400)).thenReturn(Optional.of(assignment));
        when(courseRepository.findById(100)).thenReturn(Optional.of(assignment.getLesson().getCourse()));
        when(submissionRepository.countByAssignmentId(400)).thenReturn(1L);

        service.deleteTeacherAssignment(400);

        assertThat(assignment.getStatus()).isEqualTo("ARCHIVED");
        verify(codingAssignmentRepository, never()).delete(any());
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
    void deleteQuestionWithAnswersArchivesItToPreserveAttemptHistory() {
        Quiz quiz = quizFor(teacher);
        Question question = Question.builder().id(401).quiz(quiz).questionText("Existing?").build();
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(questionRepository.findById(401)).thenReturn(Optional.of(question));
        when(quizRepository.findByIdWithCourse(301)).thenReturn(Optional.of(quiz));
        when(courseRepository.findById(100)).thenReturn(Optional.of(quiz.getLesson().getCourse()));
        when(quizAnswerRepository.countByQuestionId(401)).thenReturn(1L);

        service.deleteTeacherQuestion(401);

        assertThat(question.getActive()).isFalse();
        assertThat(question.getReviewStatus()).isEqualTo(QuestionReviewStatus.ARCHIVED);
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

    @Test
    void studentSubmitAssignmentPersistsJudgeResultAndTeacherNotification() {
        User student = User.builder().id(30).role(Role.STUDENT).fullName("Student A").build();
        Submission savedSubmission = Submission.builder().id(910).build();
        CodingAssignment assignment = assignmentFor(teacher, new BigDecimal("100.00"));
        AssignmentSubmissionPayload payload = new AssignmentSubmissionPayload();
        payload.setContentText("work");
        payload.setCodeLanguage("Java");
        payload.setCodeContent("class Solution {}");
        codeJudgeAdapter.setOutcome(new CodeJudgeAdapter.JudgeOutcome(
                CodeJudgeStatus.UNAVAILABLE,
                1,
                0,
                List.of(),
                "Code judge is not configured.",
                0L
        ));
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(codingAssignmentRepository.findByIdWithCourse(400)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndCourseId(30, 100)).thenReturn(true);
        when(submissionRepository.findTopByAssignmentIdAndStudentIdAndStatusOrderByUpdatedAtDesc(400, 30, SubmissionStatus.DRAFT))
                .thenReturn(Optional.empty());
        when(submissionRepository.findMaxAttemptNo(400, 30)).thenReturn(0);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(910);
            savedSubmission.setAssignment(submission.getAssignment());
            savedSubmission.setStudent(submission.getStudent());
            return submission;
        });
        when(testcaseRepository.findByAssignmentIdOrderByIdAsc(400)).thenReturn(List.of(
                Testcase.builder().id(701).assignment(assignment).points(BigDecimal.ONE).build()
        ));
        when(codeJudgeResultRepository.save(any(CodeJudgeResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.submitAssignment(400, payload);

        assertThat(response.getId()).isEqualTo(910);
        assertThat(response.getAssignmentId()).isEqualTo(400);
        assertThat(response.getJudgeStatus()).isEqualTo(CodeJudgeStatus.UNAVAILABLE);
        verify(notificationService).createNotification(eq(teacher), eq(com.ojtsu26.elearning.model.enums.NotificationType.ASSIGNMENT_SUBMITTED),
                eq("New assignment submission"), eq("Student A submitted Assignment."),
                eq("/teacher/grading?courseId=100&assignmentId=400&submissionId=910"),
                eq("TEACHER_ASSIGNMENT_SUBMISSION:submission:910"));
        verify(codeJudgeResultRepository).save(any(CodeJudgeResult.class));
    }

    @Test
    void createAssignmentRejectsAssigneeOutsideCourseEnrollment() {
        Course course = Course.builder().id(100).title("Course").instructor(teacher).build();
        AssignmentPayload payload = assignmentPayload();
        payload.setType(AssignmentType.ESSAY);
        payload.setInstructions("Write a short essay.");
        payload.setProblemStatement(null);
        payload.setAllowedLanguages(null);
        payload.setLessonId(null);
        payload.setAssigneeStudentIds(List.of(30));
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(30, 100)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createTeacherAssignment(100, payload));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        verify(codingAssignmentRepository, never()).save(any());
    }

    @Test
    void studentCannotSubmitAssignmentWhenNotInAssigneeList() {
        User student = User.builder().id(30).role(Role.STUDENT).fullName("Student A").build();
        User assignedStudent = User.builder().id(31).role(Role.STUDENT).fullName("Student B").build();
        CodingAssignment assignment = assignmentFor(teacher, new BigDecimal("100.00"));
        assignment.setAssignees(List.of(AssignmentAssignee.builder().assignment(assignment).student(assignedStudent).build()));
        AssignmentSubmissionPayload payload = new AssignmentSubmissionPayload();
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(codingAssignmentRepository.findByIdWithCourse(400)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndCourseId(30, 100)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submitAssignment(400, payload));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void studentSubmitMcqAssignmentAutoGradesAndCreatesVersionedSubmission() {
        User student = User.builder().id(30).role(Role.STUDENT).fullName("Student A").build();
        CodingAssignment assignment = assignmentFor(teacher, new BigDecimal("100.00"));
        assignment.setType(AssignmentType.MCQ);
        Question question = Question.builder()
                .id(901)
                .assignment(assignment)
                .questionText("Pick A")
                .questionType(QuestionType.SINGLE_CHOICE)
                .points(BigDecimal.ONE)
                .optionsJson("[{\"content\":\"A\",\"correct\":true},{\"content\":\"B\",\"correct\":false}]")
                .correctAnswer("0")
                .build();
        AssignmentSubmissionPayload payload = new AssignmentSubmissionPayload();
        var answer = new com.ojtsu26.elearning.dto.assessment.AssessmentDtos.AnswerPayload();
        answer.setQuestionId(901);
        answer.setSelectedOptionIds(List.of(0));
        payload.setAnswers(List.of(answer));
        List<com.ojtsu26.elearning.model.entity.QuizAnswer> savedAnswers = new java.util.ArrayList<>();
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(codingAssignmentRepository.findByIdWithCourse(400)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByStudentIdAndCourseId(30, 100)).thenReturn(true);
        when(submissionRepository.findTopByAssignmentIdAndStudentIdAndStatusOrderByUpdatedAtDesc(400, 30, SubmissionStatus.DRAFT))
                .thenReturn(Optional.empty());
        when(submissionRepository.findMaxAttemptNo(400, 30)).thenReturn(1);
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(920);
            return submission;
        });
        when(questionRepository.findByAssignmentIdOrderByDisplayOrderAscIdAsc(400)).thenReturn(List.of(question));
        when(questionRepository.findById(901)).thenReturn(Optional.of(question));
        when(quizAnswerRepository.findBySubmissionId(920)).thenAnswer(invocation -> savedAnswers);
        when(quizAnswerRepository.save(any(com.ojtsu26.elearning.model.entity.QuizAnswer.class))).thenAnswer(invocation -> {
            com.ojtsu26.elearning.model.entity.QuizAnswer quizAnswer = invocation.getArgument(0);
            savedAnswers.add(quizAnswer);
            return quizAnswer;
        });

        var response = service.submitAssignment(400, payload);

        assertThat(response.getAttemptNo()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.AUTO_GRADED);
        assertThat(response.getScore()).isEqualByComparingTo("100.00");
        verify(notificationService).createNotification(eq(teacher), eq(NotificationType.ASSIGNMENT_SUBMITTED),
                eq("New assignment submission"), eq("Student A submitted Assignment."),
                eq("/teacher/grading?courseId=100&assignmentId=400&submissionId=920"),
                eq("TEACHER_ASSIGNMENT_SUBMISSION:submission:920"));
        verify(submissionRepository, times(2)).save(any(Submission.class));
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
                .course(course)
                .lesson(lesson)
                .status("PUBLISHED")
                .type(com.ojtsu26.elearning.model.enums.AssignmentType.CODING)
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
        Lesson lesson = lessonFor(instructor, LessonType.QUIZ);
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

    private Lesson lessonFor(User instructor, LessonType type) {
        Course course = Course.builder()
                .id(instructor.getId().equals(teacher.getId()) ? 100 : 200)
                .title("Course")
                .instructor(instructor)
                .build();
        return Lesson.builder()
                .id(300)
                .title(type == LessonType.QUIZ ? "Quiz Lesson" : "Coding Lesson")
                .type(type)
                .course(course)
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

    private AssignmentPayload assignmentPayload() {
        AssignmentPayload payload = new AssignmentPayload();
        payload.setLessonId(300);
        payload.setTitle("Two Sum");
        payload.setProblemStatement("Return indices.");
        payload.setStarterCode("class Solution {}");
        payload.setAllowedLanguages("java");
        payload.setTimeLimitMs(1000);
        payload.setMaxScore(new BigDecimal("100.00"));
        payload.setStatus("DRAFT");
        return payload;
    }
}
