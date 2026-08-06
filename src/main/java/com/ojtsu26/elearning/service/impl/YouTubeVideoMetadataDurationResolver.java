package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.common.VideoUtils;
import com.ojtsu26.elearning.service.VideoMetadataDurationResolver;
import org.springframework.stereotype.Component;

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
    private static final Pattern LENGTH_SECONDS = Pattern.compile("\\\"lengthSeconds\\\":\\\"(\\d+)\\\"");
    private static final Pattern APPROX_DURATION_MILLIS = Pattern.compile("\\\"approxDurationMs\\\":\\\"(\\d+)\\\"");

    private final HttpClient httpClient;
    private final ConcurrentMap<String, Integer> durationCache = new ConcurrentHashMap<>();

    public YouTubeVideoMetadataDurationResolver() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    YouTubeVideoMetadataDurationResolver(HttpClient httpClient) {
        this.httpClient = httpClient;
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

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.youtube.com/watch?v=" + videoId))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept-Language", "en-US,en;q=0.9")
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
        OptionalInt exact = firstValidMatch(LENGTH_SECONDS, body, false);
        return exact.isPresent() ? exact : firstValidMatch(APPROX_DURATION_MILLIS, body, true);
    }

    private static OptionalInt firstValidMatch(Pattern pattern, String body, boolean milliseconds) {
        if (body == null || body.isBlank()) {
            return OptionalInt.empty();
        }
        Matcher matcher = pattern.matcher(body);
        while (matcher.find()) {
            try {
                long raw = Long.parseLong(matcher.group(1));
                long seconds = milliseconds ? (raw + 999L) / 1000L : raw;
                if (seconds > 0 && seconds <= MAX_DURATION_SECONDS) {
                    return OptionalInt.of((int) seconds);
                }
            } catch (NumberFormatException ignored) {
                // Continue to the next metadata candidate.
            }
        }
        return OptionalInt.empty();
    }
}
