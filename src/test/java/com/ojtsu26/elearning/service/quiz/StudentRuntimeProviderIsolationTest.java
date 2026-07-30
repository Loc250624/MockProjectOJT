package com.ojtsu26.elearning.service.quiz;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class StudentRuntimeProviderIsolationTest {
    @Test
    void studentAttemptRuntimeHasNoQuestionGenerationProviderDependency() {
        boolean dependsOnQuestionGenerationCode =
                Arrays.stream(QuizAttemptApplicationService.class.getDeclaredFields())
                .map(Field::getType)
                .anyMatch(type -> type.getPackageName().contains(".service.ai.question"));

        assertFalse(
                dependsOnQuestionGenerationCode,
                "Student quiz runtime must not depend on the question-generation provider");
    }
}
