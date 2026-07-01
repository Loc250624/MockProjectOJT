package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_users_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class AdminActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(user("Alice Student", "alice.student@example.com", Role.STUDENT, UserStatus.ACTIVE, AuthProvider.LOCAL));
        userRepository.save(user("Bob Teacher", "bob.teacher@example.com", Role.TEACHER, UserStatus.ACTIVE, AuthProvider.GOOGLE));
        userRepository.save(user("Carla Admin", "carla.admin@example.com", Role.ADMIN, UserStatus.ACTIVE, AuthProvider.LOCAL));
        userRepository.save(user("David Blocked", "david.blocked@example.com", Role.STUDENT, UserStatus.BLOCKED, AuthProvider.GITHUB));
        userRepository.save(user("Elena Search", "elena.search@example.com", Role.STUDENT, UserStatus.ACTIVE, AuthProvider.GOOGLE));
        userRepository.save(user("Frank Blocked Teacher", "frank.teacher@example.com", Role.TEACHER, UserStatus.BLOCKED, AuthProvider.LOCAL));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsersIsPaginated() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "email")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(6))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchesByFullName() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("keyword", "elena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Elena Search"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchesByEmail() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("keyword", "teacher@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].email", everyItem(containsString("teacher@example.com"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void filtersByRole() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("role", "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].role", everyItem(org.hamcrest.Matchers.is("TEACHER"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void filtersByStatus() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("status", "BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].status", everyItem(org.hamcrest.Matchers.is("BLOCKED"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void filtersByAuthProvider() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("authProvider", "GOOGLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].authProvider", everyItem(org.hamcrest.Matchers.is("GOOGLE"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void combinesKeywordRoleAndStatus() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .param("keyword", "teacher")
                        .param("role", "TEACHER")
                        .param("status", "BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Frank Blocked Teacher"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sortsAscendingAndDescending() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .param("sortBy", "email")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].email").value("alice.student@example.com"));

        mockMvc.perform(get("/api/admin/users")
                        .param("sortBy", "email")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].email").value("frank.teacher@example.com"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void regularUserIsDenied() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void responseDoesNotContainPasswordHash() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidFilterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("role", "OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    private User user(String fullName, String email, Role role, UserStatus status, AuthProvider authProvider) {
        return User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash("secret-hash")
                .avatarUrl(null)
                .role(role)
                .status(status)
                .authProvider(authProvider)
                .build();
    }
}
