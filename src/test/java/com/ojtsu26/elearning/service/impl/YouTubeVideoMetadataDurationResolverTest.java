package com.ojtsu26.elearning.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YouTubeVideoMetadataDurationResolverTest {

    @Test
    void readsExactDurationFromYouTubeMetadata() {
        assertEquals(8458, YouTubeVideoMetadataDurationResolver
                .parseDurationSeconds("{\"lengthSeconds\":\"8458\"}")
                .getAsInt());
    }

    @Test
    void fallsBackToApproximateMillisecondsAndRoundsUp() {
        assertEquals(8458, YouTubeVideoMetadataDurationResolver
                .parseDurationSeconds("{\"approxDurationMs\":\"8457001\"}")
                .getAsInt());
    }

    @Test
    void ignoresMissingOrUnreasonableMetadata() {
        assertTrue(YouTubeVideoMetadataDurationResolver
                .parseDurationSeconds("{\"lengthSeconds\":\"999999\"}")
                .isEmpty());
        assertTrue(YouTubeVideoMetadataDurationResolver.parseDurationSeconds("").isEmpty());
    }
}
