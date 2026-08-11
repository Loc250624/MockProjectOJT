package com.ojtsu26.elearning.common;

import org.springframework.dao.DataAccessException;

import java.sql.SQLException;
import java.util.Locale;

public final class UserFacingErrorMessage {

    private static final String GENERIC_DATA_MESSAGE =
            "We could not complete this action because some related data could not be updated. Please review the item and try again.";
    private static final String RELATED_DATA_MESSAGE =
            "This item cannot be deleted because it is still used by other content. Remove or reassign the related content first.";

    private UserFacingErrorMessage() {
    }

    public static String from(Throwable throwable) {
        if (throwable == null) {
            return GENERIC_DATA_MESSAGE;
        }
        String databaseMessage = findDatabaseMessage(throwable);
        if (databaseMessage != null) {
            return from(databaseMessage);
        }
        return from(throwable.getMessage());
    }

    public static String from(String message) {
        if (message == null || message.isBlank()) {
            return GENERIC_DATA_MESSAGE;
        }
        if (!looksLikeDatabaseMessage(message)) {
            return message;
        }

        String lower = message.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "foreign key constraint", "cannot delete or update a parent row", "referenced from table")) {
            if (containsAny(lower, "quiz", "question", "attempt", "assessment")) {
                return "This quiz cannot be deleted because it still has questions, attempts, or related quiz content. Remove those related items first.";
            }
            if (containsAny(lower, "course", "lesson", "video", "enrollment", "order")) {
                return "This item cannot be deleted because learners or course content still depend on it. Remove or reassign the related content first.";
            }
            return RELATED_DATA_MESSAGE;
        }
        if (containsAny(lower, "duplicate entry", "unique constraint", "unique index")) {
            return "A record with this information already exists. Please use a different value and try again.";
        }
        if (containsAny(lower, "data too long", "value too long", "string or binary data would be truncated")) {
            return "One of the fields is too long. Shorten it and try again.";
        }
        if (containsAny(lower, "cannot be null", "not-null property", "null value in column")) {
            return "Please fill in all required fields before saving.";
        }
        if (containsAny(lower, "deadlock", "lock wait timeout")) {
            return "This action is taking longer than expected because related data is being updated. Please try again.";
        }
        return GENERIC_DATA_MESSAGE;
    }

    private static String findDatabaseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException || current instanceof DataAccessException) {
                return current.getMessage();
            }
            String message = current.getMessage();
            if (looksLikeDatabaseMessage(message)) {
                return message;
            }
            current = current.getCause();
        }
        return null;
    }

    private static boolean looksLikeDatabaseMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return containsAny(lower,
                "could not execute statement",
                "sql [",
                "constraint [",
                "foreign key constraint",
                "duplicate entry",
                "data integrity violation",
                "data too long",
                "cannot delete or update a parent row",
                "jdbc",
                "mysql",
                "sqlstate",
                "referenced from table",
                "not-null property",
                "null value in column");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
