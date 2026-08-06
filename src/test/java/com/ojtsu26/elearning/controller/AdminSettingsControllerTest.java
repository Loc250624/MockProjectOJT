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

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
    void anonymousCannotAccessSettingsPage() throws Exception {
        for (String path : List.of("/admin/settings", "/admin/system-settings")) {
            mockMvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/auth/login"));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessSettingsPage() throws Exception {
        for (String path : List.of("/admin/settings", "/admin/system-settings")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotAccessSettingsPage() throws Exception {
        for (String path : List.of("/admin/settings", "/admin/system-settings")) {
            mockMvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));
        }
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotAccessSettingsPage() throws Exception {
        for (String path : List.of("/admin/settings", "/admin/system-settings")) {
            mockMvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReadSettingsAndDefaultsAreSeeded() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[*].key", hasItem("site.name")))
                .andExpect(jsonPath("$.data[*].key", hasItem("commerce.currency")))
                .andExpect(jsonPath("$.data[*].key", not(hasItem("payment.gateway.vnpay.secretKey"))))
                .andExpect(jsonPath("$.data[?(@.key == 'commerce.currency')].type").value(hasItem("ENUM")))
                .andExpect(jsonPath("$.data[?(@.key == 'commerce.currency')].options").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.key == 'commerce.teacherCommissionRate')].minValue").value(hasItem("0")))
                .andExpect(jsonPath("$.data[?(@.key == 'commerce.teacherCommissionRate')].maxValue").value(hasItem("1")));
    }

    @Test
    void adminCanUpdateEditableSetting() throws Exception {
        mockMvc.perform(put("/api/admin/settings/site.name")
                        .with(adminPrincipal())
                        .with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
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
                        .with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"sometimes\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("site.maintenanceMode must be true or false"));
    }

    @Test
    void keyOutsideWhitelistIsRejected() throws Exception {
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
                        .with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"2.0.0\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("System setting key is not whitelisted: system.buildVersion"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingWhitelistedKeyFallsBackToDefault() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk());

        SystemSetting currency = systemSettingRepository.findByKey("commerce.currency").orElseThrow();
        systemSettingRepository.delete(currency);

        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.key == 'commerce.currency')].value").value(hasItem("VND")));
    }

    @Test
    void decimalOutsideRangeIsRejected() throws Exception {
        mockMvc.perform(put("/api/admin/settings/commerce.teacherCommissionRate")
                        .with(adminPrincipal())
                        .with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"1.25\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("commerce.teacherCommissionRate must be at most 1"));
    }

    @Test
    void enumOutsideOptionsIsRejected() throws Exception {
        mockMvc.perform(put("/api/admin/settings/commerce.currency")
                        .with(adminPrincipal())
                        .with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"EUR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("commerce.currency must be one of: VND, USD"));
    }

    @Test
    void bulkUpdateOnlyAcceptsWhitelistedKeys() throws Exception {
        mockMvc.perform(post("/api/admin/settings/bulk")
                        .with(adminPrincipal())
                        .with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":{\"site.name\":\"Safe Name\",\"payment.gateway.secret\":\"dont-store\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("System setting key is not whitelisted: payment.gateway.secret"));
    }

    @Test
    void bulkValidUpdateUpdatesAuditMetadata() throws Exception {
        mockMvc.perform(post("/api/admin/settings/bulk")
                        .with(adminPrincipal())
                        .with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":{\"site.supportEmail\":\"HELP@LUMINA.EDU.VN\",\"commerce.currency\":\"USD\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.key == 'site.supportEmail')].value").value(hasItem("help@lumina.edu.vn")))
                .andExpect(jsonPath("$.data[?(@.key == 'site.supportEmail')].updatedById").value(hasItem(admin().getId())))
                .andExpect(jsonPath("$.data[?(@.key == 'commerce.currency')].value").value(hasItem("USD")));
    }

    @Test
    void updateWithoutSettingsRequestVerificationHeaderIsRejected() throws Exception {
        mockMvc.perform(put("/api/admin/settings/site.name")
                        .with(adminPrincipal())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"No Header\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bulkUpdateWithoutSettingsRequestVerificationHeaderIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/settings/bulk")
                        .with(adminPrincipal())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":{\"site.name\":\"No Header\"}}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotModifySettings() throws Exception {
        mockMvc.perform(put("/api/admin/settings/site.name")
                        .with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"Teacher Change\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void settingStringRejectsScriptValueAndOverlongValue() throws Exception {
        mockMvc.perform(put("/api/admin/settings/site.name")
                        .with(adminPrincipal())
                        .with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/admin/settings/site.name")
                        .with(adminPrincipal())
                        .with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"" + "A".repeat(121) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("site.name must be 120 characters or fewer"));
    }

    @Test
    void settingsJsonResponseDoesNotExposeSecretValues() throws Exception {
        systemSettingRepository.save(SystemSetting.builder()
                .key("payment.gateway.momo.secretKey")
                .value("super-secret-value")
                .type(SystemSettingType.STRING)
                .category("Payment")
                .description("Payment secret")
                .editable(true)
                .build());

        mockMvc.perform(get("/api/admin/settings")
                        .with(adminPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].key", not(hasItem("payment.gateway.momo.secretKey"))))
                .andExpect(jsonPath("$.data[*].value", not(hasItem("super-secret-value"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sensitiveDatabaseSettingIsNotExposedBecauseItIsNotWhitelisted() throws Exception {
        systemSettingRepository.save(SystemSetting.builder()
                .key("payment.gateway.vnpay.secretKey")
                .value("plain-secret")
                .type(SystemSettingType.STRING)
                .category("Payment")
                .description("Payment secret")
                .editable(true)
                .build());

        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].key", not(hasItem("payment.gateway.vnpay.secretKey"))))
                .andExpect(jsonPath("$.data[*].value", not(hasItem("plain-secret"))));
    }

    @Test
    void legacySystemSettingsPlaceholderNoLongerReturnsSuccessfulUpdate() throws Exception {
        mockMvc.perform(patch("/api/admin/system-settings")
                        .with(adminPrincipal())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"anything\":\"value\"}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("Use /api/admin/settings for whitelisted system settings."));
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
        return userRepository.findByAuthProviderAndEmailIgnoreCase(
                AuthProvider.LOCAL, "admin.settings@example.com").orElseThrow();
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
