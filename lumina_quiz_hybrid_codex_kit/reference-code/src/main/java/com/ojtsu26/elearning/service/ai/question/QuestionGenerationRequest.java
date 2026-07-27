package com.ojtsu26.elearning.service.ai.question;

import java.util.List;

public record QuestionGenerationRequest(
        Integer quizId,
        String lessonContent,
        List<String> learningObjectives,
        List<GenerationBucket> buckets) {

    public record GenerationBucket(
            String topicCode,
            String difficulty,
            int count) {
    }
}
