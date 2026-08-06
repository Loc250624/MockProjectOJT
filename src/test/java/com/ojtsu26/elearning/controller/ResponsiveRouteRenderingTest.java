package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:responsive_route_rendering;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ResponsiveRouteRenderingTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;

    private User teacher;
    private User admin;
    private User student;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        teacher = userRepository.save(testUser("Responsive Teacher", "teacher.responsive@example.com", Role.TEACHER));
        admin = userRepository.save(testUser("Responsive Admin", "admin.responsive@example.com", Role.ADMIN));
        student = userRepository.save(testUser("Mạnh Cường Hoàng", "student.responsive@example.com", Role.STUDENT));
    }

    @Test
    void publicHeaderRendersSharedBrandAndMobilePanel() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"public-nav-panel\"")))
                .andExpect(content().string(containsString("aria-controls=\"public-navigation\"")))
                .andExpect(content().string(containsString("/js/public/public.js")))
                .andExpect(result -> assertSingleChatbot(result.getResponse().getContentAsString()));

        mockMvc.perform(get("/").with(user(new CustomUserDetails(student))))
                .andExpect(status().isOk())
                .andExpect(result -> assertAccountChip(result.getResponse().getContentAsString(), student));

        for (User expectedUser : new User[] {student, teacher, admin}) {
            mockMvc.perform(get("/").with(user(expectedUser.getEmail()).roles(expectedUser.getRole().name())))
                    .andExpect(status().isOk())
                    .andExpect(result -> assertAccountChip(result.getResponse().getContentAsString(), expectedUser));
        }
    }

    @Test
    void authenticationAndStudentShellsRenderOneGlobalChatbot() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(result -> assertSingleChatbot(result.getResponse().getContentAsString()));

        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(result -> assertSingleChatbot(result.getResponse().getContentAsString()));

        mockMvc.perform(get("/student/dashboard").with(user(new CustomUserDetails(student))))
                .andExpect(status().isOk())
                .andExpect(result -> assertAccountChip(result.getResponse().getContentAsString(), student))
                .andExpect(result -> assertSingleChatbot(result.getResponse().getContentAsString()));
    }

    @Test
    void teacherResponsiveRoutesRenderCardTablesAndExpandableDetails() throws Exception {
        CustomUserDetails principal = new CustomUserDetails(teacher);

        mockMvc.perform(get("/teacher/dashboard").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("teacher-header-actions")))
                .andExpect(content().string(containsString("help-drawer-panel")))
                .andExpect(result -> assertAccountChip(result.getResponse().getContentAsString(), teacher))
                .andExpect(result -> assertSingleChatbot(result.getResponse().getContentAsString()));

        mockMvc.perform(get("/teacher/courses").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("responsive-data-table")))
                .andExpect(content().string(containsString("No courses found")));

        mockMvc.perform(get("/teacher/roadmap").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("responsive-data-table")))
                .andExpect(content().string(containsString("No roadmaps found")));
    }

    @Test
    void adminCoursesRendersSharedResponsiveDataList() throws Exception {
        mockMvc.perform(get("/admin/courses").with(user(new CustomUserDetails(admin))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("responsive-data-table")))
                .andExpect(content().string(containsString("No courses found in the system")))
                .andExpect(content().string(containsString("system-health-badge")))
                .andExpect(result -> assertAccountChip(result.getResponse().getContentAsString(), admin))
                .andExpect(result -> assertSingleChatbot(result.getResponse().getContentAsString()));
    }

    private void assertAccountChip(String html, User expectedUser) {
        org.junit.jupiter.api.Assertions.assertTrue(html.contains(
                "data-user-chip-name=\"" + expectedUser.getFullName() + "\""));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains(
                "data-user-chip-role=\"" + expectedUser.getRole().name() + "\""));
        if (expectedUser.getAvatarUrl() == null) {
            String initial = expectedUser.getFullName().substring(0, 1).toUpperCase();
            org.junit.jupiter.api.Assertions.assertTrue(html.contains(
                    "data-user-chip-initial=\"true\">" + initial + "</span>"));
            org.junit.jupiter.api.Assertions.assertFalse(html.contains("data-user-chip-image=\"true\""));
        } else {
            org.junit.jupiter.api.Assertions.assertTrue(html.contains(expectedUser.getAvatarUrl()));
            org.junit.jupiter.api.Assertions.assertTrue(html.contains("data-user-chip-image=\"true\""));
        }
        String avatarMarker = "class=\"topbar-avatar user-chip-avatar\"";
        int first = html.indexOf(avatarMarker);
        org.junit.jupiter.api.Assertions.assertTrue(first >= 0, "Account chip avatar should render");
        org.junit.jupiter.api.Assertions.assertEquals(-1, html.indexOf(avatarMarker, first + avatarMarker.length()),
                "Account chip should render exactly one avatar");
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("user-chip-avatar-fallback"));
    }

    private void assertSingleChatbot(String html) {
        String marker = "data-ai-chatbot-root";
        int first = html.indexOf(marker);
        org.junit.jupiter.api.Assertions.assertTrue(first >= 0, "AI Chatbot root should render");
        org.junit.jupiter.api.Assertions.assertEquals(-1, html.indexOf(marker, first + marker.length()),
                "AI Chatbot should render exactly once");
    }

    private User testUser(String fullName, String email, Role role) {
        return User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash("secret")
                .avatarUrl(role == Role.STUDENT ? null : "/uploads/test-" + role.name().toLowerCase() + ".png")
                .role(role)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }
}
