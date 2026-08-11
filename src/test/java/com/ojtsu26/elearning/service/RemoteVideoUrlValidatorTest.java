package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteVideoUrlValidatorTest {

    private final RemoteVideoUrlValidator validator = new RemoteVideoUrlValidator();

    @Test
    void rejectsLoopbackDirectVideoUrlBeforeFetching() {
        assertThrows(RuntimeException.class,
                () -> validator.validateDirectVideoUrl("https://127.0.0.1/video.mp4"));
    }

    @Test
    void rejectsNonHttpsDirectVideoUrl() {
        assertThrows(RuntimeException.class,
                () -> validator.validateDirectVideoUrl("http://cdn.example.com/video.mp4"));
    }
}
