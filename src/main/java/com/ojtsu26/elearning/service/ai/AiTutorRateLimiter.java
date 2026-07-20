package com.ojtsu26.elearning.service.ai;

import com.ojtsu26.elearning.config.AiTutorProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class AiTutorRateLimiter {

    private final AiTutorProperties properties;
    private final Clock clock = Clock.systemUTC();
    private final ConcurrentMap<Integer, Deque<Instant>> requestsByStudent = new ConcurrentHashMap<>();

    public void check(Integer studentId) {
        if (studentId == null) {
            throw new AiTutorRateLimitException("Too many AI Tutor requests. Please wait a moment.");
        }
        int maxRequests = Math.max(1, properties.getRateLimitMaxRequests());
        long windowSeconds = Math.max(1, properties.getRateLimitWindowSeconds());
        Instant now = Instant.now(clock);
        Instant cutoff = now.minusSeconds(windowSeconds);
        Deque<Instant> bucket = requestsByStudent.computeIfAbsent(studentId, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(cutoff)) {
                bucket.removeFirst();
            }
            if (bucket.size() >= maxRequests) {
                throw new AiTutorRateLimitException("Too many AI Tutor requests. Please wait a moment.");
            }
            bucket.addLast(now);
        }
    }

    public void clear() {
        requestsByStudent.clear();
    }
}
