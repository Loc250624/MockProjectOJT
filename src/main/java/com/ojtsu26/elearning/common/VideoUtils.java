package com.ojtsu26.elearning.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoUtils {

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "^(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtube\\.com\\/(?:watch\\?(?:.*&)?v=|embed\\/|shorts\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})(?:\\S*)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DIRECT_MEDIA_PATTERN = Pattern.compile(
            "(?i).+\\.(mp4|webm|ogg|ogv|mov|m4v|m3u8)(?:[?#].*)?$"
    );

    public enum VideoSourceType {
        MISSING,
        MALFORMED,
        YOUTUBE,
        DIRECT_MEDIA,
        LOCAL_UPLOAD,
        UNSUPPORTED_PROVIDER
    }

    public record VideoSourceInfo(VideoSourceType type, boolean playable, String message) {
    }

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

    public static VideoSourceInfo classifyVideoSource(String url) {
        if (url == null || url.isBlank()) {
            return new VideoSourceInfo(VideoSourceType.MISSING, false, "This lesson video is not configured yet.");
        }

        String trimmed = url.trim();
        if (extractYouTubeVideoId(trimmed) != null) {
            return new VideoSourceInfo(VideoSourceType.YOUTUBE, true, null);
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/uploads/") && DIRECT_MEDIA_PATTERN.matcher(lower).matches()) {
            return new VideoSourceInfo(VideoSourceType.LOCAL_UPLOAD, true, null);
        }
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
            return new VideoSourceInfo(VideoSourceType.MALFORMED, false, "The YouTube video link is not valid.");
        }
        if (lower.contains("vimeo.com")) {
            return new VideoSourceInfo(VideoSourceType.UNSUPPORTED_PROVIDER, false, "This video provider is not supported in the course player yet.");
        }
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return new VideoSourceInfo(VideoSourceType.MALFORMED, false, "The video source must be an HTTPS/HTTP media URL or supported embed URL.");
        }

        try {
            URI uri = new URI(trimmed);
            if (uri.getHost() == null || uri.getScheme() == null) {
                return new VideoSourceInfo(VideoSourceType.MALFORMED, false, "The video source URL is not valid.");
            }
        } catch (URISyntaxException ex) {
            return new VideoSourceInfo(VideoSourceType.MALFORMED, false, "The video source URL is not valid.");
        }

        if (DIRECT_MEDIA_PATTERN.matcher(lower).matches()) {
            return new VideoSourceInfo(VideoSourceType.DIRECT_MEDIA, true, null);
        }

        return new VideoSourceInfo(VideoSourceType.UNSUPPORTED_PROVIDER, false, "This video source is not a supported direct media file or YouTube URL.");
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
