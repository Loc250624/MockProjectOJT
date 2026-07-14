package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CertificateRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.repository.OrderRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:dashboard_rendering_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class DashboardCardRenderingTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CertificateRepository certificateRepository;
    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;
    @Autowired
    private LessonProgressRepository lessonProgressRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private OrderRepository orderRepository;

    private User teacher;
    private User student;

    @BeforeEach
    void setUp() {
        certificateRepository.deleteAll();
        lessonProgressRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        teacher = userRepository.save(testUser("Teacher", "teacher.dashboard@example.com", Role.TEACHER));
        student = userRepository.save(testUser("Student", "student.dashboard@example.com", Role.STUDENT));
    }

    @Test
    void teacherDashboardRendersRealEmptyStateWithoutKnownHardcodedCardValues() throws Exception {
        mockMvc.perform(get("/teacher/dashboard").with(user(new CustomUserDetails(teacher))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("1,482"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("78.4%"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("$42.8k"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No change from last month")));
    }

    @Test
    void studentDashboardRendersCertificateEmptyStateWithoutSampleCount() throws Exception {
        mockMvc.perform(get("/student/dashboard").with(user(new CustomUserDetails(student))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("Verified credentials"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No credentials yet")));
    }

    @Test
    void wrongRolesCannotOpenDashboardRoutes() throws Exception {
        mockMvc.perform(get("/teacher/dashboard").with(user(new CustomUserDetails(student))))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/student/dashboard").with(user(new CustomUserDetails(teacher))))
                .andExpect(status().is3xxRedirection());
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
