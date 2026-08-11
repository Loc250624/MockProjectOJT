package com.ojtsu26.elearning.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserFacingErrorMessageTest {

    @Test
    void replacesForeignKeySqlWithUserFriendlyQuizMessage() {
        String mysqlMessage = "could not execute statement [Cannot delete or update a parent row: "
                + "a foreign key constraint fails (`mock_project`.`quiz_blueprint_items`, CONSTRAINT "
                + "`FKd32kaljvn4ktv0cjftgoeyr53` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`))] "
                + "[delete from quizzes where id=?]; SQL [delete from quizzes where id=?]; constraint [null]";

        assertEquals(
                "This quiz cannot be deleted because it still has questions, attempts, or related quiz content. Remove those related items first.",
                UserFacingErrorMessage.from(mysqlMessage));
    }

    @Test
    void replacesDuplicateEntrySqlWithUserFriendlyMessage() {
        assertEquals(
                "A record with this information already exists. Please use a different value and try again.",
                UserFacingErrorMessage.from("Duplicate entry 'java' for key 'categories.uk_name'"));
    }

    @Test
    void leavesBusinessMessagesUnchanged() {
        assertEquals(
                "You do not have permission to delete this course.",
                UserFacingErrorMessage.from("You do not have permission to delete this course."));
    }

    @Test
    void leavesValidationConstraintMessagesUnchanged() {
        assertEquals(
                "Validation failed for title: must not be blank",
                UserFacingErrorMessage.from("Validation failed for title: must not be blank"));
    }
}
