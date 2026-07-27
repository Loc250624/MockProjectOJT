package com.ojtsu26.elearning.feedback.email;

import java.time.LocalDateTime;

public record FeedbackSubmittedEvent(
        Integer feedbackId,
        String studentDisplayName,
        String studentEmail,
        LocalDateTime submittedAt
) {
}
