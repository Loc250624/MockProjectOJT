package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentWebhookResponseMigrationTest {

    @Test
    void migrationAllowsAFullPaymentGatewayCallbackToBeStored() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:payment_webhook_migration;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa",
                "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE Transactions (
                            id INT NOT NULL AUTO_INCREMENT,
                            webhook_response VARCHAR(255),
                            PRIMARY KEY (id)
                        )
                        """);
            }

            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("db/migration/V6__expand_payment_webhook_response.sql"));

            String callback = "vnp_callback_field=" + "x".repeat(2_000);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO Transactions (webhook_response) VALUES (?)")) {
                statement.setString(1, callback);
                assertEquals(1, statement.executeUpdate());
            }

            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT CHAR_LENGTH(webhook_response) FROM Transactions")) {
                result.next();
                assertEquals(callback.length(), result.getInt(1));
            }
        }
    }
}
