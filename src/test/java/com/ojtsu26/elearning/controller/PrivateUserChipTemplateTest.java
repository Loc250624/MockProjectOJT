package com.ojtsu26.elearning.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PrivateUserChipTemplateTest {

    @Test
    void everyPrivateRoleUsesOneProfileAvatarChipWithoutEmailFallback() throws Exception {
        String layout = Files.readString(Path.of("src/main/resources/templates/fragments/layout.html"));
        String css = Files.readString(Path.of("src/main/resources/static/css/lumina-design-system.css"));
        int fragmentStart = layout.indexOf("th:fragment=\"private-user-chip");
        int adminTopbar = layout.indexOf("th:fragment=\"topbar-admin", fragmentStart);
        String chip = layout.substring(fragmentStart, adminTopbar);

        assertThat(chip).contains("#authentication?.principal instanceof", "#authentication.principal.user");
        assertThat(chip).contains("resolvedUser?.fullName");
        assertThat(chip).contains("resolvedUser?.role");
        assertThat(chip).contains("resolvedUser?.avatarUrl");
        assertThat(chip).doesNotContain("#authentication?.name");
        assertThat(chip).contains("portalUser", "data-user-chip-avatar=\"true\"", "data-user-chip-initial=\"true\"");
        assertThat(chip).contains("th:if=\"${!#strings.isEmpty(chipAvatar)}\"");
        assertThat(chip).doesNotContain("avatar-placeholder.png", "user-chip-avatar-fallback");
        assertThat(chip).doesNotContain("email");
        assertThat(css).contains(".user-chip .user-name", "text-overflow: ellipsis");

        assertThat(layout).contains(
                "private-user-chip('/admin/profile', 'Administrator', 'ADMIN')",
                "private-user-chip('/teacher/profile', 'Teacher', 'TEACHER')",
                "private-user-chip('/student/profile', 'Student', 'STUDENT')");
    }

    @Test
    void everyAuthenticatedHeaderUsesTheSharedNameRoleAndAvatarChip() throws Exception {
        String layout = Files.readString(Path.of("src/main/resources/templates/fragments/layout.html"));
        String publicHeader = layout.substring(
                layout.indexOf("th:fragment=\"header(role)\""),
                layout.indexOf("th:fragment=\"sidebar-admin\""));

        assertThat(publicHeader).contains("Authenticated users use the same name, role and profile avatar");
        assertThat(publicHeader).contains(
                "private-user-chip('/admin/profile', 'Administrator', 'ADMIN')",
                "private-user-chip('/teacher/profile', 'Teacher', 'TEACHER')",
                "private-user-chip('/student/profile', 'Student', 'STUDENT')");
    }

    @Test
    void roleTemplatesEitherUseAPortalTopbarOrAHeaderContainingTheSharedChip() throws Exception {
        for (String role : new String[] {"admin", "teacher", "student"}) {
            try (var templates = Files.list(Path.of("src/main/resources/templates", role))) {
                for (Path template : templates.filter(path -> path.toString().endsWith(".html")).toList()) {
                    String html = Files.readString(template);
                    assertThat(html)
                            .as(template.toString())
                            .containsAnyOf("topbar-" + role, "layout :: header(", "private-user-chip");
                }
            }
        }
    }
}
