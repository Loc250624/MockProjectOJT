package com.ojtsu26.elearning.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoUtils {

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "^(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtube\\.com\\/(?:watch\\?(?:.*&)?v=|embed\\/|shorts\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})(?:\\S*)?$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Extracts YouTube Video ID from a given URL.
     * Supports formats:
     * - https://www.youtube.com/watch?v=VIDEO_ID
     * - https://m.youtube.com/watch?v=VIDEO_ID
     * - https://youtu.be/VIDEO_ID
     * - https://www.youtube.com/embed/VIDEO_ID
     * - https://youtube.com/embed/VIDEO_ID
     * - https://www.youtube.com/shorts/VIDEO_ID
     */
    public static String extractYouTubeVideoId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Matcher matcher = YOUTUBE_PATTERN.matcher(url.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Generates a playable embed URL for supported video hosting providers.
     * Returns null if the URL is not from a supported provider (e.g. direct video link).
     */
    public static String generateEmbedUrl(String url) {
        String youtubeId = extractYouTubeVideoId(url);
        if (youtubeId != null) {
            return "https://www.youtube.com/embed/" + youtubeId + "?enablejsapi=1";
        }
        return null;
    }

    /**
     * Validates that the video URL is well-formed.
     * Ensures correct protocol (http/https) and checks that if it's a YouTube link, the ID can be extracted.
     */
    public static void validateVideoUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new RuntimeException("Video URL is required");
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new RuntimeException("Video URL must start with http:// or https://");
        }
        if (trimmed.contains("youtube.com") || trimmed.contains("youtu.be")) {
            String youtubeId = extractYouTubeVideoId(trimmed);
            if (youtubeId == null) {
                throw new RuntimeException("Invalid YouTube URL. Unable to extract Video ID.");
            }
        }
    }
}
