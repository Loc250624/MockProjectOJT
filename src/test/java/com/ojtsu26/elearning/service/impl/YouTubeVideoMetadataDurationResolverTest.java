package com.ojtsu26.elearning.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YouTubeVideoMetadataDurationResolverTest {

    @Test
    void readsIsoDurationFromYouTubeDataApiMetadata() {
        assertEquals(8458, YouTubeVideoMetadataDurationResolver
                .parseDurationSeconds("{\"items\":[{\"contentDetails\":{\"duration\":\"PT2H20M58S\"}}]}")
                .getAsInt());
    }

    @Test
    void readsMinuteAndSecondOnlyIsoDuration() {
        assertEquals(615, YouTubeVideoMetadataDurationResolver
                .parseDurationSeconds("{\"duration\":\"PT10M15S\"}")
                .getAsInt());
    }

    @Test
    void ignoresMissingOrUnreasonableMetadata() {
        assertTrue(YouTubeVideoMetadataDurationResolver
                .parseDurationSeconds("{\"duration\":\"PT999H\"}")
                .isEmpty());
        assertTrue(YouTubeVideoMetadataDurationResolver.parseDurationSeconds("").isEmpty());
    }
}
