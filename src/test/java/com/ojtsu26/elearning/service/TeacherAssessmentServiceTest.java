package com.ojtsu26.elearning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizPayload;
import com.ojtsu26.elearning.dto.assessment.AssessmentDtos.QuizView;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.QuizAttempt;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.QuizAttemptStatus;
import com.ojtsu26.elearning.model.enums.QuizStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizAnswerRepository;
import com.ojtsu26.elearning.repository.QuizAttemptRepository;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.service.impl.AssessmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherAssessmentServiceTest {

    @Mock private QuizRepository quizRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuizAttemptRepository quizAttemptRepository;
    @Mock private QuizAnswerRepository quizAnswerRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseEnrollmentRepository enrollmentRepository;
    @Mock private CurrentUserService currentUserService;

    private AssessmentServiceImpl service;
    private User teacher;
    private Course course;

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
        teacher = User.builder().id(7).role(Role.TEACHER).build();
        course = Course.builder().id(100).title("Java OOP").instructor(teacher).build();
    }

    @Test
    void teacherCreatesQuizForOwnedQuizLesson() {
        Lesson lesson = Lesson.builder()
                .id(200)
                .title("OOP Quiz")
                .type(LessonType.QUIZ)
                .course(course)
                .build();
        QuizPayload payload = payload(200);
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(lessonRepository.findByIdWithCourseAndAssessment(200))
                .thenReturn(Optional.of(lesson));
        when(quizRepository.existsByLessonId(200)).thenReturn(false);
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> {
            Quiz saved = invocation.getArgument(0);
            saved.setId(300);
            return saved;
        });
        when(questionRepository.findByQuizIdOrderByDisplayOrderAscIdAsc(300))
                .thenReturn(List.of());

        QuizView result = service.createTeacherQuiz(100, payload);

        assertEquals(300, result.getId());
        assertEquals("Java OOP Quiz", result.getTitle());
        assertEquals(QuizStatus.DRAFT, result.getStatus());
        verify(quizRepository).save(any(Quiz.class));
    }

    @Test
    void teacherCannotCreateQuizForVideoLesson() {
        Lesson lesson = Lesson.builder()
                .id(200)
                .type(LessonType.VIDEO)
                .course(course)
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findById(100)).thenReturn(Optional.of(course));
        when(lessonRepository.findByIdWithCourseAndAssessment(200))
                .thenReturn(Optional.of(lesson));

        assertThrows(BusinessException.class,
                () -> service.createTeacherQuiz(100, payload(200)));
        verify(quizRepository, never()).save(any());
    }

    @Test
    void studentQuizAttemptUsesTenRandomQuestionsAndKeepsSelection() {
        User student = User.builder().id(8).role(Role.STUDENT).build();
        Lesson lesson = Lesson.builder()
                .id(200)
                .type(LessonType.QUIZ)
                .course(course)
                .build();
        Quiz quiz = Quiz.builder()
                .id(300)
                .title("Java OOP Quiz")
                .status(QuizStatus.PUBLISHED)
                .lesson(lesson)
                .build();
        List<Question> questions = IntStream.rangeClosed(1, 10)
                .mapToObj(index -> Question.builder()
                        .id(400 + index)
                        .quiz(quiz)
                        .questionText("Question " + index)
                        .optionsJson("[{\"content\":\"A\",\"correct\":true},"
                                + "{\"content\":\"B\",\"correct\":false}]")
                        .points(java.math.BigDecimal.ONE)
                        .build())
                .toList();
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(quizRepository.findByIdWithCourse(300)).thenReturn(Optional.of(quiz));
        when(enrollmentRepository.existsByStudentIdAndCourseId(8, 100))
                .thenReturn(true);
        when(quizAttemptRepository
                .findTopByQuizIdAndStudentIdAndStatusOrderByStartedAtDesc(
                        300, 8, QuizAttemptStatus.DRAFT))
                .thenReturn(Optional.empty());
        when(questionRepository.findRandomByQuizId(eq(300), any()))
                .thenReturn(questions);
        when(questionRepository.findByQuizIdAndIdIn(
                300, questions.stream().map(Question::getId).toList()))
                .thenReturn(questions);
        when(quizAttemptRepository.save(any(QuizAttempt.class)))
                .thenAnswer(invocation -> {
                    QuizAttempt saved = invocation.getArgument(0);
                    saved.setId(500);
                    return saved;
                });
        when(quizAnswerRepository.findByAttemptId(500)).thenReturn(List.of());

        var result = service.startQuizAttempt(300);

        assertEquals(10, result.getQuiz().getQuestions().size());
        assertEquals(new java.math.BigDecimal("10"), result.getTotalPoints());
        verify(questionRepository).findRandomByQuizId(eq(300), any());
    }

    private QuizPayload payload(Integer lessonId) {
        QuizPayload payload = new QuizPayload();
        payload.setTitle("Java OOP Quiz");
        payload.setLessonId(lessonId);
        payload.setStatus(QuizStatus.DRAFT);
        return payload;
    }
}
