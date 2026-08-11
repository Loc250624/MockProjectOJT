package com.ojtsu26.elearning.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;

@Service
public class MediaDurationService {

    private static final int MAX_DURATION_SECONDS = 24 * 60 * 60;

    private final String ffprobePath;

    public MediaDurationService(@Value("${app.video.ffprobe-path:ffprobe}") String ffprobePath) {
        this.ffprobePath = ffprobePath;
    }

    public OptionalInt resolveLocalDurationSeconds(Path path) {
        OptionalInt ffprobeDuration = resolveWithFfprobe(path);
        return ffprobeDuration.isPresent() ? ffprobeDuration : resolveMp4Duration(path);
    }

    private OptionalInt resolveWithFfprobe(Path path) {
        if (path == null || ffprobePath == null || ffprobePath.isBlank()) {
            return OptionalInt.empty();
        }
        Process process = null;
        try {
            process = new ProcessBuilder(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    path.toString())
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS);
            if (!finished || process.exitValue() != 0) {
                return OptionalInt.empty();
            }
            String output = new String(process.getInputStream().readAllBytes()).trim();
            if (output.isBlank()) {
                return OptionalInt.empty();
            }
            int seconds = (int) Math.ceil(Double.parseDouble(output));
            return validDuration(seconds) ? OptionalInt.of(seconds) : OptionalInt.empty();
        } catch (Exception ignored) {
            return OptionalInt.empty();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private OptionalInt resolveMp4Duration(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return OptionalInt.empty();
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] data = inputStream.readNBytes(2 * 1024 * 1024);
            int mvhd = indexOf(data, new byte[] {'m', 'v', 'h', 'd'});
            if (mvhd < 4 || mvhd + 28 >= data.length) {
                return OptionalInt.empty();
            }
            int atomStart = mvhd - 4;
            int version = data[mvhd + 4] & 0xFF;
            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
            long timescale;
            long duration;
            if (version == 1) {
                if (atomStart + 32 >= data.length) {
                    return OptionalInt.empty();
                }
                timescale = Integer.toUnsignedLong(buffer.getInt(mvhd + 24));
                duration = buffer.getLong(mvhd + 28);
            } else {
                timescale = Integer.toUnsignedLong(buffer.getInt(mvhd + 16));
                duration = Integer.toUnsignedLong(buffer.getInt(mvhd + 20));
            }
            if (timescale <= 0 || duration <= 0) {
                return OptionalInt.empty();
            }
            int seconds = (int) Math.ceil(duration / (double) timescale);
            return validDuration(seconds) ? OptionalInt.of(seconds) : OptionalInt.empty();
        } catch (IOException ex) {
            return OptionalInt.empty();
        }
    }

    private int indexOf(byte[] source, byte[] target) {
        if (source == null || target == null || source.length < target.length) {
            return -1;
        }
        outer:
        for (int i = 0; i <= source.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public OptionalInt clientFallback(Integer durationSeconds) {
        int seconds = durationSeconds == null ? 0 : durationSeconds;
        return validDuration(seconds) ? OptionalInt.of(seconds) : OptionalInt.empty();
    }

    private boolean validDuration(int seconds) {
        return seconds > 0 && seconds <= MAX_DURATION_SECONDS;
    }
}
