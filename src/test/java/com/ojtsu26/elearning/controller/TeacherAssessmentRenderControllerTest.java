package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":80,\"feedback\":\"done\"}"))
                .andExpect(status().isForbidden());
    }

    private CustomUserDetails teacherPrincipal() {
        return new CustomUserDetails(teacher);
    }

    private CustomUserDetails studentPrincipal() {
        return new CustomUserDetails(User.builder()
                .id(2001)
                .fullName("Student Render")
                .email("student.render@example.com")
                .passwordHash("secret")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build());
    }
}
