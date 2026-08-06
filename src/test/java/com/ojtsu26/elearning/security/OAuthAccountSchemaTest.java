package com.ojtsu26.elearning.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthAccountSchemaTest {

    @Test
    void entityAndMigrationUseProviderScopedEmailUniquenessWithoutDeletingUsers() throws Exception {
        String entity = Files.readString(Path.of(
                "src/main/java/com/ojtsu26/elearning/model/entity/User.java"));
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V7__separate_oauth_accounts.sql"));

        assertThat(entity).contains(
                "uk_users_auth_provider_email",
                "columnNames = {\"auth_provider\", \"email\"}");
        assertThat(entity).doesNotContain("@Column(unique = true)\n    private String email");
        assertThat(migration).contains(
                "UNIQUE (`auth_provider`, `email`)",
                "DELETE upi",
                "LOWER(upi.provider) <> LOWER(u.auth_provider)");
        assertThat(migration.toLowerCase()).doesNotContain("delete from users", "delete u from users");
    }
}
