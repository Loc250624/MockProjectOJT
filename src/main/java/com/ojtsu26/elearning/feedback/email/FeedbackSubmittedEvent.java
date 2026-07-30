package com.ojtsu26.elearning.feedback.email;

import java.time.LocalDateTime;

public record FeedbackSubmittedEvent(
        Integer feedbackId,
        String studentDisplayName,
        String studentEmail,
        String subject,
        String content,
        Integer courseContentRating,
        Integer instructorSupportRating,
        Integer learningExperienceRating,
        Integer platformUsabilityRating,
        Integer assessmentExperienceRating,
        Integer overallSatisfactionRating,
        LocalDateTime submittedAt
) {
}
