package com.ojtsu26.elearning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.dto.request.StudentCodeSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.request.StudentQuizSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.response.StudentCodingAssignmentDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningLessonDTO;
import com.ojtsu26.elearning.dto.response.StudentQuizAttemptDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.Testcase;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CodingAssignmentRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.TestcaseRepository;
import com.ojtsu26.elearning.service.impl.StudentAssessmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class StudentAssessmentServiceTest {

    @Mock
    private StudentLearningService studentLearningService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private CodingAssignmentRepository codingAssignmentRepository;
    @Mock
    private TestcaseRepository testcaseRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    private StudentAssessmentServiceImpl service;
    private ObjectMapper objectMapper;
    private User student;
    private Course course;
    private Lesson quizLesson;
    private Lesson codeLesson;
    private Quiz quiz;
    private Question questionOne;
    private Question questionTwo;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new StudentAssessmentServiceImpl(
                studentLearningService,
                currentUserService,
                lessonRepository,
                quizRepository,
                questionRepository,
                codingAssignmentRepository,
                testcaseRepository,
                submissionRepository,
                lessonProgressRepository,
                enrollmentRepository,
                objectMapper
        );
        student = User.builder().id(1).role(Role.STUDENT).status(UserStatus.ACTIVE).build();
        course = Course.builder().id(10).status(CourseStatus.APPROVED).build();
        quizLesson = Lesson.builder().id(201).course(course).title("Quiz").type(LessonType.QUIZ).orderIndex(1).build();
        codeLesson = Lesson.builder().id(202).course(course).title("Code").type(LessonType.CODING).orderIndex(2).build();
        quiz = Quiz.builder().id(301).lesson(quizLesson).title("Quiz One").passingScore(new BigDecimal("70.00")).build();
        questionOne = Question.builder().id(401).quiz(quiz).questionText("First?").optionsJson("[\"A\",\"B\"]").correctAnswer("A").build();
        questionTwo = Question.builder().id(402).quiz(quiz).questionText("Second?").optionsJson("[\"A\",\"B\"]").correctAnswer("B").build();
    }

    @Test
    void quizStartReusesPendingAttemptWithoutCreatingDuplicate() {
        stubAccessible(quizLesson);
        Submission pending = Submission.builder()
                .id(501)
                .student(student)
                .lesson(quizLesson)
                .status(SubmissionStatus.PENDING_REVIEW)
                .submittedContent("{\"type\":\"QUIZ\",\"state\":\"DRAFT\",\"answers\":{\"401\":\"A\"}}")
                .build();
        when(quizRepository.findByLessonId(201)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuizIdOrderByIdAsc(301)).thenReturn(List.of(questionOne, questionTwo));
        when(submissionRepository.findTopByStudentIdAndLessonIdAndStatusOrderByIdDesc(1, 201, SubmissionStatus.PENDING_REVIEW))
                .thenReturn(Optional.of(pending));

        StudentQuizAttemptDTO response = service.getOrStartQuiz(10, 201);

        assertEquals(501, response.getAttemptId());
        assertEquals(List.of(401, 402), response.getQuestions().stream().map(item -> item.getId()).toList());
        assertEquals("A", response.getAnswers().get(401));
        assertFalse(response.getSubmitted());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void missingQuizReturnsUnavailableState() {
        stubAccessible(quizLesson);
        when(quizRepository.findByLessonId(201)).thenReturn(Optional.empty());

        StudentQuizAttemptDTO response = service.getOrStartQuiz(10, 201);

        assertTrue(response.getUnavailable());
        assertEquals("This quiz is not configured yet.", response.getUnavailableMessage());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void quizDraftRejectsQuestionFromAnotherQuiz() {
        stubAccessible(quizLesson);
        Submission pending = Submission.builder().id(501).student(student).lesson(quizLesson).status(SubmissionStatus.PENDING_REVIEW).build();
        when(quizRepository.findByLessonId(201)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuizIdOrderByIdAsc(301)).thenReturn(List.of(questionOne));
        when(submissionRepository.findOwnedLessonSubmission(501, 1, 201)).thenReturn(Optional.of(pending));

        StudentQuizSubmissionRequestDTO request = quizRequest(501, Map.of(999, "A"));

        assertThrows(BusinessException.class, () -> service.saveQuizDraft(10, 201, request));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void quizSubmitPersistsScoreAndMarksPassedLessonProgressComplete() {
        stubAccessible(quizLesson);
        Submission pending = Submission.builder().id(501).student(student).lesson(quizLesson).status(SubmissionStatus.PENDING_REVIEW).build();
        CourseEnrollment enrollment = CourseEnrollment.builder().id(20).student(student).course(course).build();
        LessonProgress progress = LessonProgress.builder().enrollment(enrollment).lesson(quizLesson).isCompleted(false).build();
        when(quizRepository.findByLessonId(201)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuizIdOrderByIdAsc(301)).thenReturn(List.of(questionOne, questionTwo));
        when(submissionRepository.findOwnedLessonSubmission(501, 1, 201)).thenReturn(Optional.of(pending));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentRepository.findByStudentIdAndCourseIdForUpdate(1, 10)).thenReturn(Optional.of(enrollment));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonIdForUpdate(20, 201)).thenReturn(Optional.of(progress));

        StudentQuizAttemptDTO response = service.submitQuiz(10, 201, quizRequest(501, Map.of(401, "A", 402, "B")));

        assertEquals(new BigDecimal("100.00"), response.getScore());
        assertEquals(SubmissionStatus.PASSED, pending.getStatus());
        assertTrue(response.getPassed());
        assertTrue(progress.getIsCompleted());
        assertNotNull(progress.getCompletedAt());
        verify(lessonProgressRepository).save(progress);
    }

    @Test
    void lockedQuizAccessStopsBeforeAttemptLookup() {
        when(studentLearningService.openLesson(10, 201))
                .thenThrow(new BusinessException(ErrorCode.ACCESS_DENIED, "Complete \"Intro\" to unlock this lesson."));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.getOrStartQuiz(10, 201));

        assertEquals("Complete \"Intro\" to unlock this lesson.", exception.getMessage());
        verify(submissionRepository, never()).findTopByStudentIdAndLessonIdOrderByIdDesc(any(), any());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void codingAssignmentLoadExcludesHiddenExpectedOutputs() {
        stubAccessible(codeLesson);
        CodingAssignment assignment = CodingAssignment.builder()
                .id(601)
                .lesson(codeLesson)
                .title("Loops")
                .problemStatement("Solve it.")
                .starterCode("class Solution {}")
                .allowedLanguages("Java, Python")
                .timeLimitMs(2000)
                .build();
        when(codingAssignmentRepository.findByLessonId(202)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findTopByStudentIdAndLessonIdOrderByIdDesc(1, 202)).thenReturn(Optional.empty());
        when(testcaseRepository.findByAssignmentIdOrderByIdAsc(601)).thenReturn(List.of(
                Testcase.builder().id(701).assignment(assignment).inputData("visible").expectedOutput("secret-visible-output").isHidden(false).build(),
                Testcase.builder().id(702).assignment(assignment).inputData("hidden").expectedOutput("secret-hidden-output").isHidden(true).build()
        ));

        StudentCodingAssignmentDTO response = service.getCodingAssignment(10, 202);

        assertFalse(response.getUnavailable());
        assertEquals(List.of("Java", "Python"), response.getAllowedLanguages());
        assertEquals(1, response.getExamples().size());
        assertEquals("visible", response.getExamples().get(0).getInputData());
    }

    @Test
    void codingSubmissionRejectsUnsupportedLanguage() {
        stubAccessible(codeLesson);
        CodingAssignment assignment = CodingAssignment.builder()
                .id(601)
                .lesson(codeLesson)
                .title("Loops")
                .problemStatement("Solve it.")
                .starterCode("class Solution {}")
                .allowedLanguages("Java, Python")
                .build();
        when(codingAssignmentRepository.findByLessonId(202)).thenReturn(Optional.of(assignment));

        StudentCodeSubmissionRequestDTO request = codeRequest(null, "JavaScript", "console.log(1);");

        assertThrows(BusinessException.class, () -> service.submitCode(10, 202, request));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void codingSubmitUsesAuthenticatedStudentAndLessonWithoutExecution() throws Exception {
        stubAccessible(codeLesson);
        CodingAssignment assignment = CodingAssignment.builder()
                .id(601)
                .lesson(codeLesson)
                .title("Loops")
                .problemStatement("Solve it.")
                .starterCode("class Solution {}")
                .allowedLanguages("Java, Python")
                .build();
        when(codingAssignmentRepository.findByLessonId(202)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findTopByStudentIdAndLessonIdAndStatusOrderByIdDesc(1, 202, SubmissionStatus.PENDING_REVIEW))
                .thenReturn(Optional.empty());
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(801);
            return submission;
        });
        when(testcaseRepository.findByAssignmentIdOrderByIdAsc(601)).thenReturn(List.of());

        StudentCodingAssignmentDTO response = service.submitCode(10, 202, codeRequest(null, "Java", "class Solution {}"));

        assertEquals(801, response.getSubmissionId());
        assertEquals(SubmissionStatus.PENDING_REVIEW, response.getStatus());
        assertTrue(response.getSubmitted());
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(submissionCaptor.capture());
        Submission saved = submissionCaptor.getValue();
        assertEquals(student, saved.getStudent());
        assertEquals(codeLesson, saved.getLesson());
        assertEquals("SUBMITTED", objectMapper.readTree(saved.getSubmittedContent()).get("state").asText());
    }

    private void stubAccessible(Lesson lesson) {
        when(studentLearningService.openLesson(10, lesson.getId()))
                .thenReturn(StudentLearningLessonDTO.builder().id(lesson.getId()).type(lesson.getType()).build());
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        lenient().when(currentUserService.getCurrentUser()).thenReturn(student);
    }

    private StudentQuizSubmissionRequestDTO quizRequest(Integer attemptId, Map<Integer, String> answers) {
        StudentQuizSubmissionRequestDTO request = new StudentQuizSubmissionRequestDTO();
        request.setAttemptId(attemptId);
        request.setAnswers(answers);
        return request;
    }

    private StudentCodeSubmissionRequestDTO codeRequest(Integer submissionId, String language, String code) {
        StudentCodeSubmissionRequestDTO request = new StudentCodeSubmissionRequestDTO();
        request.setSubmissionId(submissionId);
        request.setLanguage(language);
        request.setCode(code);
        return request;
    }
}
