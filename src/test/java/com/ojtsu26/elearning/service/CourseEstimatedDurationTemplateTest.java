package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseEstimatedDurationTemplateTest {

    @Test
    void learnerFacingCourseViewsRenderEstimatedDuration() throws Exception {
        List<String> templates = List.of(
                "src/main/resources/templates/public/home.html",
                "src/main/resources/templates/public/courses.html",
                "src/main/resources/templates/public/course-detail.html",
                "src/main/resources/templates/student/courses.html",
                "src/main/resources/templates/student/course-detail.html",
                "src/main/resources/templates/student/my-courses.html",
                "src/main/resources/templates/student/dashboard.html",
                "src/main/resources/templates/student/learning.html",
                "src/main/resources/templates/teacher/courses.html",
                "src/main/resources/templates/admin/courses.html",
                "src/main/resources/templates/admin/course-approval.html"
        );

        for (String template : templates) {
            String source = Files.readString(Path.of(template));
            assertTrue(source.contains("estimatedDurationDisplay"), template);
            assertTrue(source.contains("Estimated duration:"), template);
            assertFalse(source.contains("Thời lượng dự tính"), template);
        }
    }

    @Test
    void curriculumViewsRenderVideoDurationOnTheRight() throws Exception {
        List<String> templates = List.of(
                "src/main/resources/templates/public/course-detail.html",
                "src/main/resources/templates/student/course-detail.html",
                "src/main/resources/templates/student/learning.html"
        );

        for (String template : templates) {
            String source = Files.readString(Path.of(template));
            assertTrue(source.contains("videoDurationDisplay"), template);
        }
    }

    @Test
    void learningViewRendersAndUpdatesVideoLessonCompletion() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/student/learning.html"));
        String script = Files.readString(
                Path.of("src/main/resources/static/js/student/student.js"));

        assertTrue(template.contains("video-progress-fill"));
        assertTrue(template.contains("data-lesson-state"));
        assertTrue(script.contains("Video completed."));
        assertTrue(script.contains("progress.lessonProgressPercentage"));
        assertTrue(script.contains("playerDurationSeconds"));
        assertTrue(script.contains("updateDurationDisplays"));
        assertTrue(template.contains("data-course-duration-display"));
        assertTrue(template.contains("data-lesson-duration"));
    }

    @Test
    void teacherVideoFormDetectsDurationInsteadOfAskingForSeconds() throws Exception {
        String form = Files.readString(
                Path.of("src/main/resources/templates/teacher/video-form.html"));
        String management = Files.readString(
                Path.of("src/main/resources/templates/teacher/videos.html"));
        String script = Files.readString(
                Path.of("src/main/resources/static/js/teacher/teacher.js"));

        assertTrue(form.contains("type=\"hidden\" th:field=\"*{durationSeconds}\""));
        assertTrue(form.contains("data-video-metadata-status"));
        assertFalse(form.contains("type=\"number\""));
        assertFalse(form.contains("DURATION (SECONDS)"));
        assertFalse(management.contains("video.durationSeconds"));
        assertTrue(script.contains("player.getDuration()"));
        assertTrue(script.contains("Math.ceil(Number(seconds))"));
        assertTrue(script.contains("detectDirectVideoDuration"));
    }
}
