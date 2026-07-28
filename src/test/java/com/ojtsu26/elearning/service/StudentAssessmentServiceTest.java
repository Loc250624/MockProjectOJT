package com.ojtsu26.elearning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.dto.request.StudentQuizSubmissionRequestDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningLessonDTO;
import com.ojtsu26.elearning.dto.response.StudentQuizAttemptDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.service.impl.StudentAssessmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAssessmentServiceTest {

    @Mock private StudentLearningService studentLearningService;
    @Mock private CurrentUserService currentUserService;
    @Mock private LessonRepository lessonRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private CourseEnrollmentRepository enrollmentRepository;

    private StudentAssessmentServiceImpl service;
    private ObjectMapper objectMapper;
    private User student;
    private Lesson lesson;
    private Quiz quiz;
    private List<Question> questions;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new StudentAssessmentServiceImpl(
                studentLearningService,
                currentUserService,
                lessonRepository,
                quizRepository,
                questionRepository,
                submissionRepository,
                lessonProgressRepository,
                enrollmentRepository,
                objectMapper);
        student = User.builder().id(1).role(Role.STUDENT).build();
        Course course = Course.builder().id(10).build();
        lesson = Lesson.builder()
                .id(201)
                .course(course)
                .title("Java OOP Quiz")
                .type(LessonType.QUIZ)
                .build();
        quiz = Quiz.builder().id(301).lesson(lesson).title("Java OOP Quiz").build();
        questions = new ArrayList<>();
        for (int index = 1; index <= 12; index++) {
            questions.add(Question.builder()
                    .id(400 + index)
                    .quiz(quiz)
                    .questionText("Question " + index)
                    .optionsJson("[\"A\",\"B\"]")
                    .correctAnswer("A")
                    .build());
        }
    }

    @Test
    void newAttemptLoadsOnlyTenRandomQuestionsAndPersistsTheirIds() throws Exception {
        stubAccessibleQuiz();
        when(submissionRepository.findDraftsForUpdate(
                1, 201, SubmissionStatus.PENDING_REVIEW)).thenReturn(List.of());
        when(questionRepository.findRandomByQuizId(any(), any(Pageable.class)))
                .thenReturn(questions.subList(0, 10));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission saved = invocation.getArgument(0);
            saved.setId(501);
            return saved;
        });

        StudentQuizAttemptDTO result = service.getOrStartQuiz(10, 201);

        assertEquals(10, result.getQuestions().size());
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(questionRepository).findRandomByQuizId(eq(301), page.capture());
        assertEquals(10, page.getValue().getPageSize());
        ArgumentCaptor<Submission> savedAttempt = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(savedAttempt.capture());
        JsonNode payload = objectMapper.readTree(savedAttempt.getValue().getSubmittedContent());
        assertEquals(10, payload.get("questionIds").size());
    }

    @Test
    void existingDraftKeepsItsOriginalQuestionSet() {
        stubAccessibleQuiz();
        List<Integer> selectedIds = questions.subList(1, 11).stream()
                .map(Question::getId)
                .toList();
        Submission draft = Submission.builder()
                .id(501)
                .student(student)
                .lesson(lesson)
                .status(SubmissionStatus.PENDING_REVIEW)
                .submittedContent("{\"type\":\"QUIZ\",\"state\":\"DRAFT\",\"answers\":{},"
                        + "\"questionIds\":" + selectedIds + "}")
                .build();
        when(submissionRepository.findDraftsForUpdate(
                1, 201, SubmissionStatus.PENDING_REVIEW)).thenReturn(List.of(draft));
        when(questionRepository.findByQuizIdAndIdIn(301, selectedIds))
                .thenReturn(questions.subList(1, 11));

        StudentQuizAttemptDTO result = service.getOrStartQuiz(10, 201);

        assertEquals(selectedIds,
                result.getQuestions().stream().map(item -> item.getId()).toList());
        verify(questionRepository, never()).findRandomByQuizId(any(), any());
    }

    @Test
    void draftRejectsAnswerForQuestionOutsideSelectedSet() {
        stubAccessibleQuiz();
        List<Question> selected = questions.subList(0, 10);
        List<Integer> selectedIds = selected.stream().map(Question::getId).toList();
        Submission draft = Submission.builder()
                .id(501)
                .student(student)
                .lesson(lesson)
                .status(SubmissionStatus.PENDING_REVIEW)
                .submittedContent("{\"type\":\"QUIZ\",\"state\":\"DRAFT\",\"answers\":{},"
                        + "\"questionIds\":" + selectedIds + "}")
                .build();
        StudentQuizSubmissionRequestDTO request = new StudentQuizSubmissionRequestDTO();
        request.setAttemptId(501);
        request.setAnswers(new LinkedHashMap<>(Map.of(questions.get(11).getId(), "A")));
        when(quizRepository.findByLessonId(201)).thenReturn(Optional.of(quiz));
        when(submissionRepository.findOwnedLessonSubmission(501, 1, 201))
                .thenReturn(Optional.of(draft));
        when(questionRepository.findByQuizIdAndIdIn(301, selectedIds)).thenReturn(selected);

        assertThrows(BusinessException.class,
                () -> service.saveQuizDraft(10, 201, request));
        verify(submissionRepository, never()).save(any());
    }

    private void stubAccessibleQuiz() {
        when(studentLearningService.openLesson(10, 201))
                .thenReturn(StudentLearningLessonDTO.builder().id(201).build());
        when(lessonRepository.findById(201)).thenReturn(Optional.of(lesson));
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(quizRepository.findByLessonId(201)).thenReturn(Optional.of(quiz));
    }
}
