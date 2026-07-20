package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.model.entity.Question;
import com.ojtsu26.elearning.model.entity.Quiz;
import com.ojtsu26.elearning.model.entity.Testcase;
import com.ojtsu26.elearning.model.enums.CodeJudgeStatus;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.QuizStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CodingAssignmentRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.QuestionRepository;
import com.ojtsu26.elearning.repository.QuizRepository;
import com.ojtsu26.elearning.repository.TestcaseRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.service.CodeJudgeAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:teacher_assessment_render_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class TeacherAssessmentRenderControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private CodingAssignmentRepository codingAssignmentRepository;
    @Autowired
    private TestcaseRepository testcaseRepository;
    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired
    private LessonProgressRepository lessonProgressRepository;
    @MockBean
    private CodeJudgeAdapter codeJudgeAdapter;

    private User teacher;

    @BeforeEach
    void setUp() {
        teacher = userRepository.save(User.builder()
                .fullName("Teacher Render")
                .email("teacher.render." + System.nanoTime() + "@example.com")
                .passwordHash("secret")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build());
    }

    @Test
    void teacherGradingRendersPortalLayoutAndEmptyState() throws Exception {
        mockMvc.perform(get("/teacher/grading").with(user(teacherPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/grading"))
                .andExpect(model().attributeExists("submissions"))
                .andExpect(model().attributeExists("submissionPage"))
                .andExpect(model().attributeExists("gradingSummary"))
                .andExpect(content().string(containsString("lumina-portal")))
                .andExpect(content().string(containsString("Teacher Portal")))
                .andExpect(content().string(containsString("No submissions need attention.")))
                .andExpect(content().string(containsString("/css/lumina-design-system.css")))
                .andExpect(content().string(containsString("/css/teacher/teacher.css")))
                .andExpect(content().string(containsString("/js/teacher/teacher.js")));
    }

    @Test
    void teacherAssignmentsRendersPortalLayoutAndEmptyState() throws Exception {
        mockMvc.perform(get("/teacher/assignments").with(user(teacherPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/assignments"))
                .andExpect(model().attributeExists("assignments"))
                .andExpect(model().attributeExists("assignmentPage"))
                .andExpect(content().string(containsString("lumina-portal")))
                .andExpect(content().string(containsString("Teacher Portal")))
                .andExpect(content().string(containsString("No assignments to show yet.")))
                .andExpect(content().string(containsString("/css/lumina-design-system.css")))
                .andExpect(content().string(containsString("/css/teacher/teacher.css")))
                .andExpect(content().string(containsString("/js/teacher/teacher.js")));
    }

    @Test
    void teacherQuizzesRendersBuilderForOwnedCourse() throws Exception {
        Course course = courseRepository.save(Course.builder()
                .title("Quiz Course")
                .description("Course with quizzes")
                .status(CourseStatus.APPROVED)
                .instructor(teacher)
                .build());
        lessonRepository.save(Lesson.builder()
                .title("Quiz Lesson")
                .type(LessonType.QUIZ)
                .orderIndex(1)
                .course(course)
                .build());

        mockMvc.perform(get("/teacher/quizzes")
                        .param("courseId", course.getId().toString())
                        .with(user(teacherPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/quizzes"))
                .andExpect(model().attributeExists("courses"))
                .andExpect(model().attributeExists("lessons"))
                .andExpect(model().attributeExists("quizzes"))
                .andExpect(content().string(containsString("lumina-portal")))
                .andExpect(content().string(containsString("Quiz Builder")))
                .andExpect(content().string(containsString("Quiz Lesson")))
                .andExpect(content().string(containsString("No quizzes in this course yet.")))
                .andExpect(content().string(containsString("/js/teacher/teacher.js")));
    }

    @Test
    void teacherAssignmentsRendersCodingEditorForSelectedLesson() throws Exception {
        Course course = courseRepository.save(Course.builder()
                .title("Coding Course")
                .description("Course with coding exercises")
                .status(CourseStatus.APPROVED)
                .instructor(teacher)
                .build());
        Lesson lesson = lessonRepository.save(Lesson.builder()
                .title("Coding Lesson")
                .type(LessonType.CODING)
                .orderIndex(1)
                .course(course)
                .build());

        mockMvc.perform(get("/teacher/assignments")
                        .param("courseId", course.getId().toString())
                        .param("lessonId", lesson.getId().toString())
                        .with(user(teacherPrincipal())))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/assignments"))
                .andExpect(model().attribute("selectedLessonId", lesson.getId()))
                .andExpect(model().attributeExists("selectedLesson"))
                .andExpect(content().string(containsString("Create Coding Exercise")))
                .andExpect(content().string(containsString("teacher-assignment-form")))
                .andExpect(content().string(containsString("Coding Lesson")));
    }

    @Test
    void teacherApiCreatesQuizAndRefreshShowsPersistedLessonScopedQuiz() throws Exception {
        Course course = persistCourse("Quiz Persistence", teacher);
        Lesson lesson = persistLesson(course, "Persisted Quiz Lesson", LessonType.QUIZ, 1);

        mockMvc.perform(post("/api/teacher/courses/{courseId}/quizzes", course.getId())
                        .with(user(teacherPrincipal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lessonId": %d,
                                  "title": "Persisted Quiz",
                                  "description": "Created from API",
                                  "durationMinutes": 25,
                                  "maxAttempts": 2,
                                  "passingScore": 70,
                                  "status": "DRAFT"
                                }
                                """.formatted(lesson.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lessonId").value(lesson.getId()))
                .andExpect(jsonPath("$.data.title").value("Persisted Quiz"));

        mockMvc.perform(get("/teacher/quizzes")
                        .param("courseId", course.getId().toString())
                        .param("lessonId", lesson.getId().toString())
                        .with(user(teacherPrincipal())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Persisted Quiz")))
                .andExpect(content().string(containsString("Persisted Quiz Lesson")));
    }

    @Test
    void teacherApiCreatesEditsAndDeletesCodingExerciseWithRefreshPersistence() throws Exception {
        Course course = persistCourse("Coding Persistence", teacher);
        Lesson lesson = persistLesson(course, "Persisted Coding Lesson", LessonType.CODING, 1);

        String createResponse = mockMvc.perform(post("/api/teacher/courses/{courseId}/assignments", course.getId())
                        .with(user(teacherPrincipal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lessonId": %d,
                                  "title": "Initial Exercise",
                                  "problemStatement": "Initial statement",
                                  "starterCode": "class Solution {}",
                                  "allowedLanguages": "java",
                                  "timeLimitMs": 1000,
                                  "maxScore": 100,
                                  "status": "DRAFT"
                                }
                                """.formatted(lesson.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lessonId").value(lesson.getId()))
                .andExpect(jsonPath("$.data.title").value("Initial Exercise"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer assignmentId = Integer.valueOf(createResponse.replaceAll("(?s).*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(put("/api/teacher/assignments/{assignmentId}", assignmentId)
                        .with(user(teacherPrincipal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lessonId": %d,
                                  "title": "Edited Exercise",
                                  "problemStatement": "Edited statement",
                                  "starterCode": "class Solution { }",
                                  "allowedLanguages": "java, python",
                                  "timeLimitMs": 2000,
                                  "maxScore": 90,
                                  "status": "PUBLISHED"
                                }
                                """.formatted(lesson.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Edited Exercise"))
                .andExpect(jsonPath("$.data.problemStatement").value("Edited statement"));

        mockMvc.perform(get("/teacher/assignments")
                        .param("courseId", course.getId().toString())
                        .param("lessonId", lesson.getId().toString())
                        .with(user(teacherPrincipal())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit Coding Exercise")))
                .andExpect(content().string(containsString("Edited Exercise")))
                .andExpect(content().string(containsString("Edited statement")));

        mockMvc.perform(delete("/api/teacher/assignments/{assignmentId}", assignmentId)
                        .with(user(teacherPrincipal()))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void teacherApiRejectsOwnershipByTeacherBAndAnonymousAndStudent() throws Exception {
        Course courseA = persistCourse("Teacher A Course", teacher);
        Lesson lessonA = persistLesson(courseA, "Teacher A Quiz", LessonType.QUIZ, 1);
        User teacherB = userRepository.save(User.builder()
                .fullName("Teacher B")
                .email("teacher.b." + System.nanoTime() + "@example.com")
                .passwordHash("secret")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build());

        String payload = """
                {
                  "lessonId": %d,
                  "title": "Forbidden Quiz",
                  "durationMinutes": 15,
                  "maxAttempts": 1,
                  "passingScore": 70,
                  "status": "DRAFT"
                }
                """.formatted(lessonA.getId());

        mockMvc.perform(post("/api/teacher/courses/{courseId}/quizzes", courseA.getId())
                        .with(user(new CustomUserDetails(teacherB)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/teacher/courses/{courseId}/quizzes", courseA.getId())
                        .with(user(studentPrincipal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/teacher/courses/{courseId}/quizzes", courseA.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void teacherApiRejectsQuizAndCodingTypeMismatchWithoutDirtyRecords() throws Exception {
        Course course = persistCourse("Type Mismatch", teacher);
        Lesson textLesson = persistLesson(course, "Text Lesson", null, 1);
        Lesson videoLesson = persistLesson(course, "Video Lesson", LessonType.VIDEO, 2);
        Lesson quizLesson = persistLesson(course, "Quiz Lesson", LessonType.QUIZ, 3);

        mockMvc.perform(post("/api/teacher/courses/{courseId}/quizzes", course.getId())
                        .with(user(teacherPrincipal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lessonId": %d,
                                  "title": "Invalid Quiz",
                                  "durationMinutes": 15,
                                  "maxAttempts": 1,
                                  "passingScore": 70,
                                  "status": "DRAFT"
                                }
                                """.formatted(textLesson.getId())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/teacher/courses/{courseId}/assignments", course.getId())
                        .with(user(teacherPrincipal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lessonId": %d,
                                  "title": "Invalid Coding",
                                  "problemStatement": "Nope",
                                  "allowedLanguages": "java",
                                  "timeLimitMs": 1000,
                                  "maxScore": 100,
                                  "status": "DRAFT"
                                }
                                """.formatted(videoLesson.getId())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/teacher/courses/{courseId}/assignments", course.getId())
                        .with(user(teacherPrincipal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lessonId": %d,
                                  "title": "Invalid Coding On Quiz",
                                  "problemStatement": "Nope",
                                  "allowedLanguages": "java",
                                  "timeLimitMs": 1000,
                                  "maxScore": 100,
                                  "status": "DRAFT"
                                }
                                """.formatted(quizLesson.getId())))
                .andExpect(status().isBadRequest());

        assertTrue(quizRepository.findByLessonId(textLesson.getId()).isEmpty());
        assertTrue(codingAssignmentRepository.findByLessonId(videoLesson.getId()).isEmpty());
        assertTrue(codingAssignmentRepository.findByLessonId(quizLesson.getId()).isEmpty());
    }

    @Test
    void studentLearningRendersQuizAndCodingWithoutTeacherControlsAndHidesHiddenTests() throws Exception {
        User student = persistStudent();
        Course quizCourse = persistCourse("Student Quiz Rendering", teacher);
        Course codeCourse = persistCourse("Student Coding Rendering", teacher);
        Lesson quizLesson = persistLesson(quizCourse, "Student Quiz Lesson", LessonType.QUIZ, 1);
        Lesson codeLesson = persistLesson(codeCourse, "Student Coding Lesson", LessonType.CODING, 1);
        courseEnrollmentRepository.save(CourseEnrollment.builder()
                .student(student)
                .course(quizCourse)
                .isCompleted(false)
                .progressPercentage(BigDecimal.ZERO)
                .build());
        courseEnrollmentRepository.save(CourseEnrollment.builder()
                .student(student)
                .course(codeCourse)
                .isCompleted(false)
                .progressPercentage(BigDecimal.ZERO)
                .build());
        Quiz quiz = quizRepository.save(Quiz.builder()
                .lesson(quizLesson)
                .title("Student Visible Quiz")
                .status(QuizStatus.PUBLISHED)
                .durationMinutes(10)
                .maxAttempts(1)
                .passingScore(new BigDecimal("70.00"))
                .build());
        questionRepository.save(Question.builder()
                .quiz(quiz)
                .questionText("Visible question")
                .optionsJson("[\"A\",\"B\"]")
                .correctAnswer("A")
                .points(BigDecimal.ONE)
                .displayOrder(1)
                .build());
        CodingAssignment assignment = codingAssignmentRepository.save(CodingAssignment.builder()
                .lesson(codeLesson)
                .title("Student Visible Coding")
                .problemStatement("Solve without seeing hidden data")
                .starterCode("class Solution {}")
                .allowedLanguages("java")
                .timeLimitMs(1000)
                .maxScore(new BigDecimal("100.00"))
                .status("PUBLISHED")
                .build());
        testcaseRepository.save(Testcase.builder()
                .assignment(assignment)
                .inputData("visible input")
                .expectedOutput("visible expected should stay server side")
                .isHidden(false)
                .points(BigDecimal.ONE)
                .build());
        testcaseRepository.save(Testcase.builder()
                .assignment(assignment)
                .inputData("hidden input")
                .expectedOutput("hidden expected")
                .isHidden(true)
                .points(BigDecimal.ONE)
                .build());

        mockMvc.perform(get("/student/learning")
                        .param("courseId", quizCourse.getId().toString())
                        .param("lessonId", quizLesson.getId().toString())
                        .with(user(new CustomUserDetails(student))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-quiz-panel")))
                .andExpect(content().string(not(containsString(">History<"))))
                .andExpect(content().string(not(containsString("teacher-assignment-form"))));

        mockMvc.perform(get("/student/learning")
                        .param("courseId", codeCourse.getId().toString())
                        .param("lessonId", codeLesson.getId().toString())
                        .with(user(new CustomUserDetails(student))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-code-panel")))
                .andExpect(content().string(not(containsString(">History<"))))
                .andExpect(content().string(not(containsString("teacher-assignment-form"))));

        mockMvc.perform(get("/student/courses/{courseId}/lessons/{lessonId}/coding-assignment",
                        codeCourse.getId(), codeLesson.getId())
                        .with(user(new CustomUserDetails(student))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("visible input")))
                .andExpect(content().string(not(containsString("Submission History"))))
                .andExpect(content().string(not(containsString(">History<"))))
                .andExpect(content().string(not(containsString("hidden input"))))
                .andExpect(content().string(not(containsString("hidden expected"))))
                .andExpect(content().string(not(containsString("visible expected should stay server side"))));
    }

    @Test
    void studentCodingPassUnlocksNextLessonAndRefreshResolver() throws Exception {
        User student = persistStudent();
        Course course = persistCourse("Sequential Coding Progression", teacher);
        Lesson codeLesson = persistLesson(course, "Pass Coding First", LessonType.CODING, 1);
        Lesson nextLesson = persistLesson(course, "Unlocked Next Lesson", LessonType.VIDEO, 2);
        CourseEnrollment enrollment = courseEnrollmentRepository.save(CourseEnrollment.builder()
                .student(student)
                .course(course)
                .isCompleted(false)
                .progressPercentage(BigDecimal.ZERO)
                .build());
        CodingAssignment assignment = codingAssignmentRepository.save(CodingAssignment.builder()
                .lesson(codeLesson)
                .title("Passing Exercise")
                .problemStatement("Return the expected value")
                .starterCode("class Solution {}")
                .allowedLanguages("java")
                .timeLimitMs(1000)
                .maxScore(new BigDecimal("100.00"))
                .status("PUBLISHED")
                .build());
        testcaseRepository.save(Testcase.builder()
                .assignment(assignment)
                .inputData("1")
                .expectedOutput("1")
                .isHidden(false)
                .points(BigDecimal.ONE)
                .build());
        when(codeJudgeAdapter.judge(any(), any())).thenReturn(new CodeJudgeAdapter.JudgeOutcome(
                CodeJudgeStatus.PASSED,
                1,
                1,
                List.of(),
                "All tests passed.",
                12L
        ));

        mockMvc.perform(get("/student/courses/{courseId}/lessons/{lessonId}", course.getId(), nextLesson.getId())
                        .with(user(new CustomUserDetails(student))))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/student/courses/{courseId}/lessons/{lessonId}/coding-assignment/submit",
                        course.getId(), codeLesson.getId())
                        .with(user(new CustomUserDetails(student)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "language": "java",
                                  "code": "class Solution { int solve() { return 1; } }"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PASSED"))
                .andExpect(jsonPath("$.data.judgeStatus").value("PASSED"))
                .andExpect(jsonPath("$.data.learningProgress.lessonCompleted").value(true))
                .andExpect(jsonPath("$.data.learningProgress.nextLessonId").value(nextLesson.getId()))
                .andExpect(jsonPath("$.data.learningProgress.nextLessonAccessible").value(true))
                .andExpect(jsonPath("$.data.learningProgress.completedLessons").value(1))
                .andExpect(jsonPath("$.data.learningProgress.totalLessons").value(2));

        Optional<LessonProgress> completedProgress = lessonProgressRepository
                .findByEnrollmentIdAndLessonId(enrollment.getId(), codeLesson.getId());
        assertTrue(completedProgress.isPresent());
        assertTrue(Boolean.TRUE.equals(completedProgress.get().getIsCompleted()));

        mockMvc.perform(get("/student/courses/{courseId}/lessons/{lessonId}", course.getId(), nextLesson.getId())
                        .with(user(new CustomUserDetails(student))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(nextLesson.getId()))
                .andExpect(jsonPath("$.data.accessible").value(true));

        mockMvc.perform(get("/student/courses/{courseId}/learning", course.getId())
                        .with(user(new CustomUserDetails(student))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeLessonId").value(nextLesson.getId()));
    }

    @Test
    void teacherAssessmentStaticAssetsArePubliclyAvailable() throws Exception {
        mockMvc.perform(get("/css/lumina-design-system.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".lumina-portal")));

        mockMvc.perform(get("/css/teacher/teacher.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(".teacher-assessment-header")));

        mockMvc.perform(get("/js/teacher/teacher.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("initTeacherAssessments")));
    }

    @Test
    void studentCannotAccessTeacherAssessmentRoutes() throws Exception {
        mockMvc.perform(get("/teacher/grading").with(user(studentPrincipal())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/teacher/assignments").with(user(studentPrincipal())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(post("/api/teacher/submissions/1/grade")
                        .with(user(studentPrincipal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":80,\"feedback\":\"done\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/teacher/courses/1/assignments")
                        .with(user(studentPrincipal()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lessonId\":1,\"title\":\"No\",\"problemStatement\":\"No\",\"allowedLanguages\":\"java\"}"))
                .andExpect(status().isForbidden());
    }

    private CustomUserDetails teacherPrincipal() {
        return new CustomUserDetails(teacher);
    }

    private CustomUserDetails studentPrincipal() {
        return new CustomUserDetails(persistStudent());
    }

    private User persistStudent() {
        return userRepository.save(User.builder()
                .fullName("Student Render")
                .email("student.render." + System.nanoTime() + "@example.com")
                .passwordHash("secret")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build());
    }

    private Course persistCourse(String title, User instructor) {
        return courseRepository.save(Course.builder()
                .title(title)
                .description(title)
                .status(CourseStatus.APPROVED)
                .instructor(instructor)
                .build());
    }

    private Lesson persistLesson(Course course, String title, LessonType type, int orderIndex) {
        return lessonRepository.save(Lesson.builder()
                .title(title)
                .content("Content for " + title)
                .type(type)
                .orderIndex(orderIndex)
                .course(course)
                .build());
    }
}
