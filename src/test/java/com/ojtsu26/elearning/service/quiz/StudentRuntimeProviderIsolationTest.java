package com.ojtsu26.elearning.service.quiz;

import com.ojtsu26.elearning.service.ai.question.QuestionGenerationProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class StudentRuntimeProviderIsolationTest {
    @Test
    void studentAttemptRuntimeHasNoQuestionGenerationProviderDependency() {
        boolean dependsOnQuestionGenerationProvider =
                Arrays.stream(QuizAttemptApplicationService.class.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(QuestionGenerationProvider.class::equals);

        assertFalse(
                dependsOnQuestionGenerationProvider,
                "Student quiz runtime must not depend on the question-generation provider");
    }
}
