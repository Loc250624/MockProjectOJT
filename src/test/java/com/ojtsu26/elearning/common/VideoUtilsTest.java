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

    @Test
    void classifyVideoSourceIdentifiesSupportedAndUnavailableStates() {
        assertEquals(VideoUtils.VideoSourceType.MISSING, VideoUtils.classifyVideoSource(" ").type());
        assertEquals(VideoUtils.VideoSourceType.YOUTUBE, VideoUtils.classifyVideoSource("https://youtu.be/qz0aGYrrIhU").type());
        assertEquals(VideoUtils.VideoSourceType.DIRECT_MEDIA, VideoUtils.classifyVideoSource("https://cdn.example.com/video.mp4?token=demo").type());
        assertEquals(VideoUtils.VideoSourceType.LOCAL_UPLOAD, VideoUtils.classifyVideoSource("/uploads/lessons/video.webm").type());
        assertEquals(VideoUtils.VideoSourceType.MALFORMED, VideoUtils.classifyVideoSource("ftp://example.com/video.mp4").type());
        assertEquals(VideoUtils.VideoSourceType.MALFORMED, VideoUtils.classifyVideoSource("https://www.youtube.com/watch?v=short").type());
        assertEquals(VideoUtils.VideoSourceType.UNSUPPORTED_PROVIDER, VideoUtils.classifyVideoSource("https://vimeo.com/123456").type());
        assertFalse(VideoUtils.classifyVideoSource("https://vimeo.com/123456").playable());
    }
}
