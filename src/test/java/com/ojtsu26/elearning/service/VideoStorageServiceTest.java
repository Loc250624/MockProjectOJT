package com.ojtsu26.elearning.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class VideoStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsExecutableRenamedAsMp4() {
        VideoStorageService service = new VideoStorageService(tempDir.toString(), 1024);
        MockMultipartFile file = new MockMultipartFile(
                "videoFile",
                "lesson.mp4",
                "video/mp4",
                "MZ fake executable".getBytes());

        assertThrows(RuntimeException.class, () -> service.store(file));
    }
}
