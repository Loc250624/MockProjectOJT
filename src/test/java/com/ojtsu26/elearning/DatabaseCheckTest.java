package com.ojtsu26.elearning;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@SpringBootTest
public class DatabaseCheckTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void printColumns() {
        String[] tables = { "blog_posts", "blog_comments", "blog_moderation_logs", "blog_comment_moderation_logs" };
        System.out.println("\n=== COLUMNS OF BLOG TABLES ===");
        for (String table : tables) {
            System.out.println("Columns of " + table + ":");
            try {
                DatabaseMetaData metaData = jdbcTemplate.getDataSource().getConnection().getMetaData();
                ResultSet rs = metaData.getColumns("mock_project", null, table, "%");
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String columnType = rs.getString("TYPE_NAME");
                    System.out.println("  - " + columnName + " (" + columnType + ")");
                }
            } catch (Exception e) {
                System.out.println("  ERROR -> " + e.getMessage());
            }
        }
        System.out.println("==============================\n");
    }
}
