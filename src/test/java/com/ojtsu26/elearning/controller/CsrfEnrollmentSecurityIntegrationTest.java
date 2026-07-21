package com.ojtsu26.elearning.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.NotificationRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.JwtUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:csrf_enrollment_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "app.cors.allowed-origins=http://localhost:5173"
})
class CsrfEnrollmentSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private User googleStudent;
    private User teacher;
    private Course freeCourse;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        googleStudent = userRepository.save(User.builder()
                .fullName("New Google Student")
                .email("new.google.student@example.com")
                .passwordHash("oauth")
                .authProvider(AuthProvider.GOOGLE)
                .providerId("google-sub-csrf")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build());
        teacher = userRepository.save(User.builder()
                .fullName("Teacher")
                .email("teacher.csrf@example.com")
                .passwordHash("oauth")
                .authProvider(AuthProvider.GOOGLE)
                .providerId("google-sub-teacher-csrf")
                .role(Role.TEACHER)
                .status(UserStatus.ACTIVE)
                .build());
        freeCourse = courseRepository.save(Course.builder()
                .title("Free CSRF Course")
                .description("A course used by CSRF enrollment tests")
                .price(BigDecimal.ZERO)
                .status(CourseStatus.APPROVED)
                .instructor(teacher)
                .build());
    }

    @Test
    void csrfEndpointReturnsMaterializedTokenAndCookie() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void enrollWithoutCsrfTokenIsForbiddenBeforeBusinessLogic() throws Exception {
        mockMvc.perform(post("/student/courses/" + freeCourse.getId() + "/enroll")
                        .cookie(jwtCookie(googleStudent)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", anyOf(is("CSRF_TOKEN_MISSING"), is("CSRF_TOKEN_INVALID"))));

        assertThat(enrollmentRepository.findByStudentIdAndCourseId(googleStudent.getId(), freeCourse.getId())).isEmpty();
    }

    @Test
    void newGoogleStudentEnrollsWithJwtCookieAndCsrfHeader() throws Exception {
        CsrfPair csrf = csrfPair();

        mockMvc.perform(post("/student/courses/" + freeCourse.getId() + "/enroll")
                        .cookie(jwtCookie(googleStudent), csrf.cookie())
                        .header(csrf.headerName(), csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Enrollment confirmed"))
                .andExpect(jsonPath("$.data.studentId").value(googleStudent.getId()))
                .andExpect(jsonPath("$.data.courseId").value(freeCourse.getId()));

        assertThat(enrollmentRepository.findByStudentIdAndCourseId(googleStudent.getId(), freeCourse.getId())).isPresent();
    }

    @Test
    void corsPreflightAllowsConfiguredFrontendOriginAndCsrfHeadersWithCredentials() throws Exception {
        mockMvc.perform(options("/student/courses/" + freeCourse.getId() + "/enroll")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-XSRF-TOKEN, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        org.hamcrest.Matchers.containsString("X-XSRF-TOKEN")));
    }

    private CsrfPair csrfPair() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return new CsrfPair(body.get("headerName").asText(), body.get("token").asText(), cookie);
    }

    private Cookie jwtCookie(User user) {
        return new Cookie("jwt_token", jwtUtils.generateTokenFromEmail(user.getEmail()));
    }

    private record CsrfPair(String headerName, String token, Cookie cookie) {
    }
}
