package com.ojtsu26.elearning.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class VideoStorageService {

    private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
            "video/mp4", "mp4",
            "video/webm", "webm",
            "video/ogg", "ogv",
            "video/quicktime", "mov",
            "application/vnd.apple.mpegurl", "m3u8",
            "application/x-mpegurl", "m3u8"
    );
    private static final Set<String> SAFE_EXTENSIONS = Set.of("mp4", "webm", "ogv", "mov", "m4v", "m3u8");

    private final Path videoDirectory;
    private final long maxSizeBytes;

    public VideoStorageService(
            @Value("${app.upload.video-directory:uploads/videos}") String videoDirectory,
            @Value("${app.upload.video-max-size:104857600}") long maxSizeBytes) {
        this.videoDirectory = Paths.get(videoDirectory).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeBytes;
    }

    public StoredVideo store(MultipartFile file) {
        validate(file);
        String extension = extensionFor(file);
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path destination = videoDirectory.resolve(storedFilename).normalize();
        if (!destination.startsWith(videoDirectory)) {
            throw new RuntimeException("Invalid video storage path.");
        }
        try {
            Files.createDirectories(videoDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            String publicUrl = "/uploads/videos/" + storedFilename;
            return new StoredVideo(
                    publicUrl,
                    storedFilename,
                    safeOriginalName(file.getOriginalFilename()),
                    normalizedContentType(file),
                    file.getSize(),
                    destination);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to store the uploaded video.", ex);
        }
    }

    public void deleteIfPresent(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith("/uploads/videos/")) {
            return;
        }
        String filename = publicUrl.substring("/uploads/videos/".length());
        Path target = videoDirectory.resolve(filename).normalize();
        if (!target.startsWith(videoDirectory)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // A failed cleanup should not hide the original lesson operation.
        }
    }

    public static boolean isSupportedContentType(String contentType) {
        return contentType != null && MIME_TO_EXTENSION.containsKey(contentType.toLowerCase(Locale.ROOT));
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Select a video file to upload.");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new RuntimeException("The selected video is too large. Maximum size is "
                    + DataSize.ofBytes(maxSizeBytes).toMegabytes() + " MB.");
        }
        if (!isSupportedContentType(file.getContentType())) {
            throw new RuntimeException("Unsupported video file type.");
        }
        if (!hasSupportedMagic(file)) {
            throw new RuntimeException("The selected file does not look like a supported video.");
        }
    }

    private boolean hasSupportedMagic(MultipartFile file) {
        byte[] header = new byte[16];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to inspect the uploaded video.", ex);
        }
        if (read < 4) {
            return false;
        }
        String contentType = normalizedContentType(file);
        if ("video/mp4".equals(contentType) || "video/quicktime".equals(contentType)) {
            return read >= 12
                    && header[4] == 'f'
                    && header[5] == 't'
                    && header[6] == 'y'
                    && header[7] == 'p';
        }
        if ("video/webm".equals(contentType)) {
            return (header[0] & 0xFF) == 0x1A
                    && (header[1] & 0xFF) == 0x45
                    && (header[2] & 0xFF) == 0xDF
                    && (header[3] & 0xFF) == 0xA3;
        }
        if ("video/ogg".equals(contentType)) {
            return header[0] == 'O' && header[1] == 'g' && header[2] == 'g' && header[3] == 'S';
        }
        if ("application/vnd.apple.mpegurl".equals(contentType) || "application/x-mpegurl".equals(contentType)) {
            String prefix = new String(header, 0, Math.max(0, read));
            return prefix.startsWith("#EXTM3U");
        }
        return false;
    }

    private String extensionFor(MultipartFile file) {
        String extension = MIME_TO_EXTENSION.get(normalizedContentType(file));
        if (extension == null || !SAFE_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("Unsupported video file type.");
        }
        return extension;
    }

    private String normalizedContentType(MultipartFile file) {
        return file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    }

    private String safeOriginalName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }
        return Paths.get(originalFilename).getFileName().toString();
    }

    @Getter
    public static class StoredVideo {
        private final String publicUrl;
        private final String storedFilename;
        private final String originalFilename;
        private final String contentType;
        private final long fileSizeBytes;
        private final Path storedPath;

        StoredVideo(String publicUrl,
                    String storedFilename,
                    String originalFilename,
                    String contentType,
                    long fileSizeBytes,
                    Path storedPath) {
            this.publicUrl = publicUrl;
            this.storedFilename = storedFilename;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.fileSizeBytes = fileSizeBytes;
            this.storedPath = storedPath;
        }
    }
}
