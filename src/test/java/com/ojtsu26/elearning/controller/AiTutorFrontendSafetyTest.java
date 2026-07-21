package com.ojtsu26.elearning.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiTutorFrontendSafetyTest {

    @Test
    void aiTutorMessagesUseTextContentInsteadOfRawHtml() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/student/student.js"));
        String styles = Files.readString(Path.of("src/main/resources/static/css/student/student.css"));
        int start = script.indexOf("function initAiTutor()");
        int end = script.indexOf("function applyLearningProgress", start);

        assertTrue(start >= 0, "AI Tutor initializer should exist");
        assertTrue(end > start, "AI Tutor initializer should stay before learning progress helpers");

        String aiTutorCode = script.substring(start, end);
        assertTrue(aiTutorCode.contains("bubble.textContent = text || '';"));
        assertFalse(aiTutorCode.contains("innerHTML"));
        assertFalse(aiTutorCode.contains("insertAdjacentHTML"));
        assertTrue(styles.contains(".ai-tutor-panel[hidden]"));
        assertTrue(styles.contains("display: none;"));
    }
}
