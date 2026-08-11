package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.common.VideoUtils;
import com.ojtsu26.elearning.service.VideoMetadataDurationResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YouTubeVideoMetadataDurationResolver implements VideoMetadataDurationResolver {

    private static final int MAX_DURATION_SECONDS = 24 * 60 * 60;
    private static final Pattern ISO_DURATION = Pattern.compile(
            "\"duration\"\\s*:\\s*\"PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?\"");

    private final HttpClient httpClient;
    private final ConcurrentMap<String, Integer> durationCache = new ConcurrentHashMap<>();
    private final String apiKey;

    @Autowired
    public YouTubeVideoMetadataDurationResolver(
            @Value("${app.video.youtube-api-key:}") String apiKey) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(),
                apiKey);
    }

    YouTubeVideoMetadataDurationResolver(HttpClient httpClient, String apiKey) {
        this.httpClient = httpClient;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public OptionalInt resolveDurationSeconds(String videoUrl) {
        String videoId = VideoUtils.extractYouTubeVideoId(videoUrl);
        if (videoId == null) {
            return OptionalInt.empty();
        }
        Integer cached = durationCache.get(videoId);
        if (cached != null) {
            return OptionalInt.of(cached);
        }
        if (apiKey.isBlank()) {
            return OptionalInt.empty();
        }

        try {
            String apiUrl = UriComponentsBuilder.fromUriString("https://www.googleapis.com/youtube/v3/videos")
                    .queryParam("part", "contentDetails")
                    .queryParam("id", videoId)
                    .queryParam("key", apiKey)
                    .build()
                    .toUriString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return OptionalInt.empty();
            }
            OptionalInt resolved = parseDurationSeconds(response.body());
            resolved.ifPresent(duration -> durationCache.put(videoId, duration));
            return resolved;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return OptionalInt.empty();
        } catch (Exception ignored) {
            return OptionalInt.empty();
        }
    }

    static OptionalInt parseDurationSeconds(String body) {
        if (body == null || body.isBlank()) {
            return OptionalInt.empty();
        }
        Matcher matcher = ISO_DURATION.matcher(body);
        if (!matcher.find()) {
            return OptionalInt.empty();
        }
        int hours = parsePart(matcher.group(1));
        int minutes = parsePart(matcher.group(2));
        int seconds = parsePart(matcher.group(3));
        int total = hours * 3600 + minutes * 60 + seconds;
        return total > 0 && total <= MAX_DURATION_SECONDS ? OptionalInt.of(total) : OptionalInt.empty();
    }

    private static int parsePart(String value) {
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }
}
