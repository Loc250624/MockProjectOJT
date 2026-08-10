package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.model.entity.SystemSetting;
import com.ojtsu26.elearning.model.enums.SystemSettingType;
import com.ojtsu26.elearning.repository.SystemSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:maintenance_mode_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class MaintenanceModeInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @BeforeEach
    void setUp() {
        systemSettingRepository.deleteAll();
        saveSetting("site.maintenanceMode", "false", SystemSettingType.BOOLEAN);
        saveSetting("site.supportEmail", "support@lumina.edu.vn", SystemSettingType.EMAIL);
    }

    @Test
    void maintenanceOffAllowsPublicHomepage() throws Exception {
        setMaintenance(false);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Maintenance in progress"))));
    }

    @Test
    void maintenanceOnBlocksPublicContentWithMaintenanceRedirect() throws Exception {
        setMaintenance(true);

        mockMvc.perform(get("/courses"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/maintenance"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void maintenanceOnBlocksAlreadyLoggedInStudentOnNextRequest() throws Exception {
        setMaintenance(true);

        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/maintenance"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void maintenanceOnBlocksAlreadyLoggedInTeacherOnNextRequest() throws Exception {
        setMaintenance(true);

        mockMvc.perform(get("/teacher/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/maintenance"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void maintenanceOnReturnsJson503ForApiRequest() throws Exception {
        setMaintenance(true);

        mockMvc.perform(get("/student/courses/1/enrollment-state")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("MAINTENANCE_MODE"))
                .andExpect(jsonPath("$.message").value("LumiNa is temporarily unavailable due to maintenance."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void maintenanceOnAllowsAdminAndShowsIndicator() throws Exception {
        setMaintenance(true);

        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Maintenance Mode Active")));
    }

    @Test
    void maintenancePageAndStaticAssetsAreWhitelisted() throws Exception {
        setMaintenance(true);

        mockMvc.perform(get("/maintenance"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("Maintenance in progress")))
                .andExpect(content().string(containsString("support@lumina.edu.vn")));

        mockMvc.perform(get("/css/lumina-design-system.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("--lumina-blue")));
    }

    @Test
    void maintenanceOnDoesNotBlockAuthTechnicalRoutes() throws Exception {
        setMaintenance(true);

        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login")));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("MAINTENANCE_MODE"));
    }

    @Test
    void maintenanceOnDoesNotBlockPaymentCallbacksOrReturnRoute() throws Exception {
        setMaintenance(true);

        mockMvc.perform(get("/api/payment/vnpay-ipn"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("MAINTENANCE_MODE"))));

        mockMvc.perform(post("/api/payment/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("MAINTENANCE_MODE"))));

        mockMvc.perform(get("/student/payment-result"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Maintenance in progress"))));
    }

    private void setMaintenance(boolean active) {
        SystemSetting setting = systemSettingRepository.findByKey("site.maintenanceMode").orElseThrow();
        setting.setValue(Boolean.toString(active));
        systemSettingRepository.save(setting);
    }

    private void saveSetting(String key, String value, SystemSettingType type) {
        systemSettingRepository.save(SystemSetting.builder()
                .key(key)
                .value(value)
                .type(type)
                .category("Site")
                .description(key)
                .editable(true)
                .build());
    }
}
