package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.StudentFeedback;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.FeedbackCategory;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.StudentFeedbackRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:student_feedback_security_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "app.feedback-email.enabled=false"
})
class StudentFeedbackSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentFeedbackRepository feedbackRepository;

    private User student;
    private User otherStudent;
    private User teacher;
    private User admin;

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll();
        userRepository.deleteAll();

        student = userRepository.save(testUser("Student One", "student.feedback@example.com", Role.STUDENT));
        otherStudent = userRepository.save(testUser("Student Two", "student.feedback.2@example.com", Role.STUDENT));
        teacher = userRepository.save(testUser("Teacher One", "teacher.feedback@example.com", Role.TEACHER));
        admin = userRepository.save(testUser("Admin One", "admin.feedback@example.com", Role.ADMIN));
    }

    @Test
    void studentCanOpenFormAndSubmitWithAuthenticatedOwnerOnly() throws Exception {
        mockMvc.perform(validStudentPost(student, "  Search feedback  ", "  Search should remember filters.  ")
                        .param("studentId", otherStudent.getId().toString())
                        .param("email", "attacker@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/feedback"));

        StudentFeedback saved = feedbackRepository.findAll().get(0);
        assertEquals(student.getId(), saved.getStudent().getId());
        assertEquals(FeedbackCategory.PLATFORM_UI, saved.getCategory());
        assertEquals("Search feedback", saved.getSubject());
        assertEquals("Search should remember filters.", saved.getContent());
        assertEquals(5, saved.getCourseContentRating());
        assertEquals(4, saved.getInstructorSupportRating());
        assertEquals(3, saved.getLearningExperienceRating());
        assertEquals(2, saved.getPlatformUsabilityRating());
        assertEquals(1, saved.getAssessmentExperienceRating());
        assertEquals(5, saved.getOverallSatisfactionRating());

        mockMvc.perform(get("/student/feedback").with(user(new CustomUserDetails(student))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"lumina-header\"")))
                .andExpect(content().string(containsString("href=\"/student/feedback\"")))
                .andExpect(content().string(not(containsString("sidebar-student"))))
                .andExpect(content().string(not(containsString("portal-topbar"))))
                .andExpect(content().string(containsString("Submit Feedback")))
                .andExpect(content().string(not(containsString("Search feedback"))))
                .andExpect(content().string(not(containsString("/student/feedback/" + saved.getId()))));
    }

    @Test
    void invalidStudentFeedbackFormDoesNotCreateFeedback() throws Exception {
        mockMvc.perform(validStudentPost(student, " ", " "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/feedback"));

        assertTrue(feedbackRepository.findAll().isEmpty());
    }

    @Test
    void overlongStudentFeedbackFormDoesNotCreateFeedback() throws Exception {
        mockMvc.perform(validStudentPost(student, "S".repeat(151), "C".repeat(5001)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/feedback"));

        assertTrue(feedbackRepository.findAll().isEmpty());
    }

    @Test
    void missingCategoryAndRatingsDoNotCreateFeedback() throws Exception {
        mockMvc.perform(post("/student/feedback")
                        .with(user(new CustomUserDetails(student)))
                        .with(csrf())
                        .param("subject", "Missing scores")
                        .param("content", "No category or scores"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/feedback"));

        assertTrue(feedbackRepository.findAll().isEmpty());
    }

    @Test
    void successFlashRendersSuccessOnlyState() throws Exception {
        mockMvc.perform(get("/student/feedback")
                        .with(user(new CustomUserDetails(student)))
                        .flashAttr("successMessage", "Feedback submitted successfully."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Feedback submitted successfully.")))
                .andExpect(content().string(containsString("Back to Dashboard")))
                .andExpect(content().string(not(containsString("Submit Feedback"))))
                .andExpect(content().string(not(containsString("Rating Matrix"))))
                .andExpect(content().string(not(containsString("View submitted"))))
                .andExpect(content().string(not(containsString("Feedback history"))));
    }

    @Test
    void oldStudentReadUrlsAreDisabled() throws Exception {
        StudentFeedback feedback = feedbackRepository.save(feedbackFor(otherStudent, "Private feedback", "Admin only readback."));

        mockMvc.perform(get("/student/feedback/" + feedback.getId()).with(user(new CustomUserDetails(student))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/student/feedback/new").with(user(new CustomUserDetails(student))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/student/feedback/history").with(user(new CustomUserDetails(student))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void adminViewsEscapeStoredUserInput() throws Exception {
        StudentFeedback feedback = feedbackRepository.save(feedbackFor(
                student,
                "<script>alert(1)</script>",
                "<img src=x onerror=alert(1)>"));

        mockMvc.perform(get("/admin/feedback").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));

        mockMvc.perform(get("/admin/feedback/" + feedback.getId()).with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(containsString("&lt;img src=x onerror=alert(1)&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(not(containsString("<img src=x onerror=alert(1)>"))));
    }

    @Test
    void adminCanListAndViewAllFeedback() throws Exception {
        StudentFeedback feedback = feedbackRepository.save(feedbackFor(student, "Admin review", "Visible to admin."));

        mockMvc.perform(get("/admin/feedback").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin review")))
                .andExpect(content().string(containsString("Course Content")))
                .andExpect(content().string(containsString("student.feedback@example.com")));

        mockMvc.perform(get("/admin/feedback/" + feedback.getId()).with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Visible to admin.")))
                .andExpect(content().string(containsString("Course Content")))
                .andExpect(content().string(containsString("Overall Satisfaction")));
    }

    @Test
    void adminFeedbackListRendersPagination() throws Exception {
        for (int index = 1; index <= 11; index++) {
            feedbackRepository.save(feedbackFor(student, "Feedback " + index, "Content " + index));
        }

        mockMvc.perform(get("/admin/feedback").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Page 1 of 2")))
                .andExpect(content().string(containsString("/admin/feedback?page=1")));
    }

    @Test
    void guestAndTeacherCannotOpenStudentFeedbackRoutes() throws Exception {
        mockMvc.perform(get("/student/feedback"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        mockMvc.perform(get("/student/feedback").with(user(new CustomUserDetails(teacher))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/student/feedback").with(user(new CustomUserDetails(admin))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void studentAndTeacherCannotOpenAdminFeedbackRoutes() throws Exception {
        mockMvc.perform(get("/admin/feedback").with(user(new CustomUserDetails(student))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/admin/feedback").with(user(new CustomUserDetails(teacher))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void directFeedbackUrlsAreProtectedForWrongActors() throws Exception {
        StudentFeedback feedback = feedbackRepository.save(feedbackFor(student, "Direct URL", "Protected"));

        mockMvc.perform(get("/student/feedback/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        mockMvc.perform(get("/student/feedback/new").with(user(new CustomUserDetails(admin))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/student/feedback/" + feedback.getId()).with(user(new CustomUserDetails(teacher))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(validStudentPost(teacher, "Teacher post", "Blocked"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/admin/feedback/" + feedback.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

        mockMvc.perform(get("/admin/feedback/" + feedback.getId()).with(user(new CustomUserDetails(student))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/admin/feedback/" + feedback.getId()).with(user(new CustomUserDetails(teacher))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void postRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/student/feedback")
                        .with(user(new CustomUserDetails(student)))
                        .param("category", FeedbackCategory.PLATFORM_UI.name())
                        .param("subject", "No csrf")
                        .param("content", "Rejected")
                        .param("courseContentRating", "5")
                        .param("instructorSupportRating", "4")
                        .param("learningExperienceRating", "3")
                        .param("platformUsabilityRating", "2")
                        .param("assessmentExperienceRating", "1")
                        .param("overallSatisfactionRating", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        assertTrue(feedbackRepository.findAll().isEmpty());
    }

    @Test
    void localLogoutFlowStillRedirectsHomeWithCsrf() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .with(user(new CustomUserDetails(student)))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void publicHeaderShowsFeedbackOnlyForAuthenticatedStudent() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/student/feedback\""))));

        mockMvc.perform(get("/").with(user(new CustomUserDetails(student))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/blogs\"")))
                .andExpect(content().string(containsString("href=\"/student/feedback\"")));

        mockMvc.perform(get("/").with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/blogs\"")))
                .andExpect(content().string(not(containsString("href=\"/student/feedback\""))));

        mockMvc.perform(get("/").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/blogs\"")))
                .andExpect(content().string(not(containsString("href=\"/student/feedback\""))));
    }

    private MockHttpServletRequestBuilder validStudentPost(User actor, String subject, String content) {
        return post("/student/feedback")
                .with(user(new CustomUserDetails(actor)))
                .with(csrf())
                .param("category", FeedbackCategory.PLATFORM_UI.name())
                .param("subject", subject)
                .param("content", content)
                .param("courseContentRating", "5")
                .param("instructorSupportRating", "4")
                .param("learningExperienceRating", "3")
                .param("platformUsabilityRating", "2")
                .param("assessmentExperienceRating", "1")
                .param("overallSatisfactionRating", "5");
    }

    private StudentFeedback feedbackFor(User owner, String subject, String content) {
        return StudentFeedback.builder()
                .student(owner)
                .category(FeedbackCategory.COURSE_CONTENT)
                .subject(subject)
                .content(content)
                .courseContentRating(5)
                .instructorSupportRating(4)
                .learningExperienceRating(3)
                .platformUsabilityRating(2)
                .assessmentExperienceRating(1)
                .overallSatisfactionRating(5)
                .build();
    }

    private User testUser(String fullName, String email, Role role) {
        return User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash("secret")
                .role(role)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }
}
