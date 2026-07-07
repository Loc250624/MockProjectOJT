package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.SystemSetting;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.SystemSettingType;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.SystemSettingRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_settings_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class AdminSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @BeforeEach
    void setUp() {
        systemSettingRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(testUser("Admin Settings", "admin.settings@example.com", Role.ADMIN));
        userRepository.save(testUser("Teacher Settings", "teacher.settings@example.com", Role.TEACHER));
        userRepository.save(testUser("Student Settings", "student.settings@example.com", Role.STUDENT));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReadSettingsAndDefaultsAreSeeded() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[*].key", hasItem("site.name")))
                .andExpect(jsonPath("$.data[*].key", hasItem("commerce.currency")));
    }

    @Test
    void adminCanUpdateEditableSetting() throws Exception {
        mockMvc.perform(put("/api/admin/settings/site.name")
                        .with(adminPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"OJTSU26 Learning Hub\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.key").value("site.name"))
                .andExpect(jsonPath("$.data.value").value("OJTSU26 Learning Hub"))
                .andExpect(jsonPath("$.data.updatedById").value(admin().getId()));
    }

    @Test
    void invalidTypeValueIsRejected() throws Exception {
        mockMvc.perform(put("/api/admin/settings/site.maintenanceMode")
                        .with(adminPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"sometimes\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("site.maintenanceMode must be true or false"));
    }

    @Test
    void nonEditableSettingIsRejected() throws Exception {
        systemSettingRepository.save(SystemSetting.builder()
                .key("system.buildVersion")
                .value("1.0.0")
                .type(SystemSettingType.STRING)
                .category("System")
                .description("Current build version.")
                .editable(false)
                .build());

        mockMvc.perform(put("/api/admin/settings/system.buildVersion")
                        .with(adminPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"2.0.0\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("System setting is not editable: system.buildVersion"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotAccessSettingsApi() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessSettingsApi() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotAccessSettingsApi() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor adminPrincipal() {
        return user(new CustomUserDetails(admin()));
    }

    private User admin() {
        return userRepository.findByEmail("admin.settings@example.com").orElseThrow();
    }

    private User testUser(String fullName, String email, Role role) {
        return User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash("secret")
                .avatarUrl(null)
                .role(role)
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }
}
