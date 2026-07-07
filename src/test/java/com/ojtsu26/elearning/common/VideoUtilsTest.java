package com.ojtsu26.elearning.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VideoUtilsTest {

    @Test
    void testExtractYouTubeVideoId_ValidUrls() {
        assertEquals("qz0aGYrrIhU", VideoUtils.extractYouTubeVideoId("https://www.youtube.com/watch?v=qz0aGYrrIhU"));
        assertEquals("qz0aGYrrIhU", VideoUtils.extractYouTubeVideoId("https://m.youtube.com/watch?v=qz0aGYrrIhU"));
        assertEquals("qz0aGYrrIhU", VideoUtils.extractYouTubeVideoId("https://youtu.be/qz0aGYrrIhU"));
        assertEquals("qz0aGYrrIhU", VideoUtils.extractYouTubeVideoId("https://www.youtube.com/embed/qz0aGYrrIhU"));
        assertEquals("qz0aGYrrIhU", VideoUtils.extractYouTubeVideoId("https://youtube.com/embed/qz0aGYrrIhU"));
        assertEquals("qz0aGYrrIhU", VideoUtils.extractYouTubeVideoId("https://www.youtube.com/shorts/qz0aGYrrIhU"));
        assertEquals("qz0aGYrrIhU", VideoUtils.extractYouTubeVideoId("https://www.youtube.com/watch?v=qz0aGYrrIhU&t=10s"));
        assertEquals("qz0aGYrrIhU", VideoUtils.extractYouTubeVideoId("https://youtu.be/qz0aGYrrIhU?t=10"));
    }

    @Test
    void testExtractYouTubeVideoId_InvalidUrls() {
        assertNull(VideoUtils.extractYouTubeVideoId("https://www.google.com"));
        assertNull(VideoUtils.extractYouTubeVideoId("https://www.youtube.com/"));
        assertNull(VideoUtils.extractYouTubeVideoId("https://youtube.com/watch?v=short"));
        assertNull(VideoUtils.extractYouTubeVideoId(null));
        assertNull(VideoUtils.extractYouTubeVideoId("   "));
    }

    @Test
    void testGenerateEmbedUrl() {
        assertEquals("https://www.youtube.com/embed/qz0aGYrrIhU?enablejsapi=1", VideoUtils.generateEmbedUrl("https://www.youtube.com/watch?v=qz0aGYrrIhU"));
        assertNull(VideoUtils.generateEmbedUrl("/uploads/video.mp4"));
    }

    @Test
    void testValidateVideoUrl() {
        // Valid URLs
        assertDoesNotThrow(() -> VideoUtils.validateVideoUrl("https://www.youtube.com/watch?v=qz0aGYrrIhU"));
        assertDoesNotThrow(() -> VideoUtils.validateVideoUrl("https://example.com/video.mp4"));

        // Invalid protocol
        RuntimeException ex1 = assertThrows(RuntimeException.class, () -> VideoUtils.validateVideoUrl("ftp://example.com/video.mp4"));
        assertEquals("Video URL must start with http:// or https://", ex1.getMessage());

        // Malformed YouTube URL
        RuntimeException ex2 = assertThrows(RuntimeException.class, () -> VideoUtils.validateVideoUrl("https://www.youtube.com/watch?v=invalid"));
        assertEquals("Invalid YouTube URL. Unable to extract Video ID.", ex2.getMessage());
    }
}
