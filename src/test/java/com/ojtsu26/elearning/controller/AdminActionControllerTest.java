package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.entity.BlogPost;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Transaction;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.BlogPostStatus;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.PaymentMethod;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.TransactionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.BlogPostRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.TransactionRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import com.ojtsu26.elearning.security.JwtUtils;
import com.ojtsu26.elearning.security.OAuth2LoginSuccessHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AdminActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @BeforeEach
    void setUp() {
        blogPostRepository.deleteAll();
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

    @Test
    void adminCreatesStudentTeacherAndAdminAccounts() throws Exception {
        createUser("Created Student", "created.student@example.com", "STUDENT")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
        createUser("Created Teacher", "created.teacher@example.com", "TEACHER")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("TEACHER"));
        createUser("Created Admin", "created.admin@example.com", "ADMIN")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        User teacher = findByEmail("created.teacher@example.com");
        assertEquals(AuthProvider.LOCAL, teacher.getAuthProvider());
        assertEquals(UserStatus.ACTIVE, teacher.getStatus());
        assertNotEquals("Secret123!", teacher.getPasswordHash());
    }

    @Test
    void createUserRejectsDuplicateEmailAndPasswordMismatch() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .with(adminPrincipal())
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"fullName":"Duplicate","email":"ALICE.STUDENT@example.com","role":"STUDENT","password":"Secret123!","confirmPassword":"Secret123!"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/admin/users")
                        .with(adminPrincipal())
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"fullName":"Mismatch","email":"mismatch@example.com","role":"TEACHER","password":"Secret123!","confirmPassword":"Different123!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
    }

    @Test
    void exportsFilteredUsersAsDownloadableUtf8Csv() throws Exception {
        userRepository.save(user("=Formula Name", "formula@example.com", Role.STUDENT, UserStatus.ACTIVE, AuthProvider.LOCAL));

        mockMvc.perform(get("/api/admin/users/export")
                        .with(adminPrincipal())
                        .param("role", "STUDENT")
                        .param("sortBy", "email")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment; filename=\"admin-users-")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("ID,Full Name,Email,Role,Status,Auth Provider,Join Date")))
                .andExpect(content().string(containsString("\"alice.student@example.com\"")))
                .andExpect(content().string(containsString("\"'=Formula Name\"")))
                .andExpect(content().string(not(containsString("bob.teacher@example.com"))));
    }

    @Test
    void adminBlocksActiveUser() throws Exception {
        User target = findByEmail("alice.student@example.com");

        mockMvc.perform(patch("/api/admin/users/{id}/block", target.getId())
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(target.getId()))
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));

        assertEquals(UserStatus.BLOCKED, findByEmail("alice.student@example.com").getStatus());
    }

    @Test
    void adminUnblocksBlockedUser() throws Exception {
        User target = findByEmail("david.blocked@example.com");

        mockMvc.perform(patch("/api/admin/users/{id}/unblock", target.getId())
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(target.getId()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertEquals(UserStatus.ACTIVE, findByEmail("david.blocked@example.com").getStatus());
    }

    @Test
    void blockMissingUserReturnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{id}/block", 999_999)
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User does not exist"));
    }

    @Test
    void adminCannotBlockSelf() throws Exception {
        User admin = findByEmail("carla.admin@example.com");

        mockMvc.perform(patch("/api/admin/users/{id}/block", admin.getId())
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Admin cannot modify their own account"));

        assertEquals(UserStatus.ACTIVE, findByEmail("carla.admin@example.com").getStatus());
    }

    @Test
    void blockAlreadyBlockedUserReturnsConflict() throws Exception {
        User target = findByEmail("david.blocked@example.com");

        mockMvc.perform(patch("/api/admin/users/{id}/block", target.getId())
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User is already blocked"));
    }

    @Test
    void unblockActiveUserReturnsConflict() throws Exception {
        User target = findByEmail("alice.student@example.com");

        mockMvc.perform(patch("/api/admin/users/{id}/unblock", target.getId())
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User is not blocked"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotBlockUser() throws Exception {
        User target = findByEmail("alice.student@example.com");

        mockMvc.perform(patch("/api/admin/users/{id}/block", target.getId())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotBlockUser() throws Exception {
        User target = findByEmail("alice.student@example.com");

        mockMvc.perform(patch("/api/admin/users/{id}/block", target.getId())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void blockedUserCannotLoginLocally() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"frank.teacher@example.com\",\"password\":\"secret-hash\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("jwt_token"));
    }

    @Test
    void localLoginWithoutCsrfIsRejectedBeforeAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"frank.teacher@example.com\",\"password\":\"secret-hash\"}"))
                .andExpect(status().isForbidden())
                .andExpect(cookie().doesNotExist("jwt_token"));
    }

    @Test
    void blockedUserCannotUseOldJwt() throws Exception {
        User student = findByEmail("alice.student@example.com");
        String token = jwtUtils.generateTokenFromEmail(student.getEmail());
        student.setStatus(UserStatus.BLOCKED);
        userRepository.save(student);

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blockedUserCannotLoginWithOAuth2() throws Exception {
        User blocked = findByEmail("david.blocked@example.com");
        blocked.setAuthProvider(AuthProvider.LOCAL);
        userRepository.save(blocked);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")),
                Map.of("email", blocked.getEmail(), "name", blocked.getFullName(), "sub", "blocked-google-sub"),
                "email");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google");

        oAuth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("/auth/login?error=blocked", response.getRedirectedUrl());
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
        assertEquals(AuthProvider.LOCAL, findByEmail("david.blocked@example.com").getAuthProvider());
        assertEquals(UserStatus.BLOCKED, findByEmail("david.blocked@example.com").getStatus());
    }

    @Test
    void adminSoftDeletesActiveUser() throws Exception {
        User target = findByEmail("alice.student@example.com");

        mockMvc.perform(delete("/api/admin/users/{id}", target.getId())
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User account has been soft-deleted. Historical data remains preserved."))
                .andExpect(jsonPath("$.data.id").value(target.getId()))
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        assertEquals(UserStatus.DELETED, findByEmail("alice.student@example.com").getStatus());
    }

    @Test
    void adminSoftDeletesBlockedUser() throws Exception {
        User target = findByEmail("david.blocked@example.com");

        mockMvc.perform(delete("/api/admin/users/{id}", target.getId())
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(target.getId()))
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        assertEquals(UserStatus.DELETED, findByEmail("david.blocked@example.com").getStatus());
    }

    @Test
    void adminCannotSoftDeleteSelf() throws Exception {
        User admin = findByEmail("carla.admin@example.com");

        mockMvc.perform(delete("/api/admin/users/{id}", admin.getId())
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Admin cannot soft-delete their own account"));

        assertEquals(UserStatus.ACTIVE, findByEmail("carla.admin@example.com").getStatus());
    }

    @Test
    void softDeleteMissingUserReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/admin/users/{id}", 999_999)
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User does not exist"));
    }

    @Test
    void repeatedSoftDeleteIsIdempotentAndKeepsUserRecord() throws Exception {
        User target = findByEmail("alice.student@example.com");
        long countBefore = userRepository.count();

        mockMvc.perform(delete("/api/admin/users/{id}", target.getId())
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        mockMvc.perform(delete("/api/admin/users/{id}", target.getId())
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        assertEquals(countBefore, userRepository.count());
        assertEquals(UserStatus.DELETED, userRepository.findById(target.getId()).orElseThrow().getStatus());
    }

    @Test
    void softDeleteKeepsRelatedEnrollmentTransactionCourseAndBlogPost() throws Exception {
        User student = findByEmail("alice.student@example.com");
        User teacher = findByEmail("bob.teacher@example.com");
        Course course = courseRepository.save(Course.builder()
                .title("Soft Delete Safety")
                .description("Related data must remain")
                .price(BigDecimal.TEN)
                .status(CourseStatus.APPROVED)
                .instructor(teacher)
                .build());
        CourseEnrollment enrollment = courseEnrollmentRepository.save(CourseEnrollment.builder()
                .student(student)
                .course(course)
                .progressPercentage(BigDecimal.valueOf(42))
                .isCompleted(false)
                .build());
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .student(student)
                .course(course)
                .amount(BigDecimal.TEN)
                .paymentMethod(PaymentMethod.VNPAY)
                .transactionRef("soft-delete-retention-" + student.getId())
                .status(TransactionStatus.SUCCESS)
                .build());
        BlogPost blogPost = blogPostRepository.save(BlogPost.builder()
                .author(student)
                .title("Retained Blog")
                .content("This blog should remain after user soft-delete.")
                .status(BlogPostStatus.PUBLISHED)
                .build());

        mockMvc.perform(delete("/api/admin/users/{id}", student.getId())
                        .with(adminPrincipal())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELETED"));

        assertEquals(UserStatus.DELETED, userRepository.findById(student.getId()).orElseThrow().getStatus());
        assertEquals(true, courseRepository.existsById(course.getId()));
        assertEquals(true, courseEnrollmentRepository.existsById(enrollment.getId()));
        assertEquals(true, transactionRepository.existsById(transaction.getId()));
        assertEquals(true, blogPostRepository.existsById(blogPost.getId()));
    }

    @Test
    void deletedUserCannotLoginLocally() throws Exception {
        User target = findByEmail("alice.student@example.com");
        target.setStatus(UserStatus.DELETED);
        userRepository.save(target);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"alice.student@example.com\",\"password\":\"secret-hash\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("jwt_token"));
    }

    @Test
    void deletedUserCannotUseOldJwt() throws Exception {
        User student = findByEmail("alice.student@example.com");
        String token = jwtUtils.generateTokenFromEmail(student.getEmail());
        student.setStatus(UserStatus.DELETED);
        userRepository.save(student);

        mockMvc.perform(get("/api/student/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedUserCannotLoginWithOAuth2() throws Exception {
        User deleted = findByEmail("bob.teacher@example.com");
        deleted.setStatus(UserStatus.DELETED);
        userRepository.save(deleted);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER")),
                Map.of("email", deleted.getEmail(), "name", deleted.getFullName(), "sub", "deleted-google-sub"),
                "email");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google");

        oAuth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("/auth/login?error=deleted", response.getRedirectedUrl());
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
        assertEquals(UserStatus.DELETED, findByEmail("bob.teacher@example.com").getStatus());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void filtersByDeletedStatus() throws Exception {
        User target = findByEmail("alice.student@example.com");
        target.setStatus(UserStatus.DELETED);
        userRepository.save(target);

        mockMvc.perform(get("/api/admin/users").param("status", "DELETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("DELETED"))
                .andExpect(jsonPath("$.data.content[0].email").value("alice.student@example.com"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotSoftDeleteUser() throws Exception {
        User target = findByEmail("alice.student@example.com");

        mockMvc.perform(delete("/api/admin/users/{id}", target.getId())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotSoftDeleteUser() throws Exception {
        User target = findByEmail("alice.student@example.com");

        mockMvc.perform(delete("/api/admin/users/{id}", target.getId())
                        .with(csrf()))
                .andExpect(status().isForbidden());
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

    private User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    private org.springframework.test.web.servlet.ResultActions createUser(String name, String email, String role) throws Exception {
        return mockMvc.perform(post("/api/admin/users")
                .with(adminPrincipal())
                .with(csrf())
                .contentType("application/json")
                .content("{\"fullName\":\"" + name + "\",\"email\":\"" + email + "\",\"role\":\"" + role
                        + "\",\"password\":\"Secret123!\",\"confirmPassword\":\"Secret123!\"}"));
    }

    private RequestPostProcessor adminPrincipal() {
        User admin = findByEmail("carla.admin@example.com");
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user(new CustomUserDetails(admin));
    }
}
