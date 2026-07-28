package com.ojtsu26.elearning.service.ai.question;

import com.ojtsu26.elearning.model.enums.QuestionDifficulty;

import java.util.List;

public record GeneratedQuestion(String questionText,
                                List<String> options,
                                String correctAnswer,
                                String topicCode,
                                QuestionDifficulty difficulty) {
}
