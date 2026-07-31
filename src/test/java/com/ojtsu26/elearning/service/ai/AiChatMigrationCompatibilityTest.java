package com.ojtsu26.elearning.service.ai;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiChatMigrationCompatibilityTest {

    @Test
    void migrationRunsOnH2InMySqlCompatibilityMode() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:ai_chat_migration;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE Users (
                            id INT NOT NULL AUTO_INCREMENT,
                            PRIMARY KEY (id)
                        )
                        """);
            }
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V3__create_ai_chat_history.sql"));
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V4__expand_ai_chat_message_content.sql"));

            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("""
                         SELECT COUNT(*)
                         FROM INFORMATION_SCHEMA.TABLES
                         WHERE TABLE_NAME IN (
                           'Ai_Chat_Conversations',
                           'Ai_Chat_Messages',
                           'Ai_Chat_Suggestions'
                         )
                         """)) {
                result.next();
                assertEquals(3, result.getInt(1));
            }

            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("""
                         SELECT DATA_TYPE
                         FROM INFORMATION_SCHEMA.COLUMNS
                         WHERE TABLE_NAME = 'Ai_Chat_Messages'
                           AND COLUMN_NAME = 'content'
                         """)) {
                result.next();
                assertEquals("CHARACTER VARYING", result.getString(1));
            }
        }
    }
}
