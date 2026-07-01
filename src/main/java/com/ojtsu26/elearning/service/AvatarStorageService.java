package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
public class AvatarStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path avatarDirectory;
    private final long maxSize;

    public AvatarStorageService(
            @Value("${app.upload.avatar-directory:uploads/avatars}") String avatarDirectory,
            @Value("${app.upload.avatar-max-size:5242880}") long maxSize) {
        this.avatarDirectory = Paths.get(avatarDirectory).toAbsolutePath().normalize();
        this.maxSize = maxSize;
    }

    public String store(MultipartFile file, Integer userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_AVATAR, "Avatar image is required");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.AVATAR_TOO_LARGE, "Avatar image must not exceed " + (maxSize / 1024 / 1024) + "MB");
        }

        String contentType = normalizeContentType(file.getContentType());
        validateContentType(contentType);

        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(16);
            validateSignature(contentType, header);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INVALID_AVATAR, "Unable to read avatar image");
        }

        try (InputStream inputStream = file.getInputStream()) {
            return store(inputStream, contentType, userId);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.AVATAR_STORAGE_ERROR, "Unable to store avatar image");
        }
    }

    public String store(byte[] content, String contentType, Integer userId) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.INVALID_AVATAR, "Avatar image is required");
        }
        if (content.length > maxSize) {
            throw new BusinessException(ErrorCode.AVATAR_TOO_LARGE, "Avatar image must not exceed " + (maxSize / 1024 / 1024) + "MB");
        }

        String normalizedContentType = normalizeContentType(contentType);
        validateContentType(normalizedContentType);
        validateSignature(normalizedContentType, content);

        try {
            return store(new java.io.ByteArrayInputStream(content), normalizedContentType, userId);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.AVATAR_STORAGE_ERROR, "Unable to store avatar image");
        }
    }

    public void deleteManagedAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith("/uploads/avatars/")) {
            return;
        }

        Path fileName = Paths.get(avatarUrl).getFileName();
        if (fileName == null) {
            return;
        }

        Path target = avatarDirectory.resolve(fileName).normalize();
        if (!target.startsWith(avatarDirectory)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Keep profile data intact if old file cleanup fails.
        }
    }

    private String store(InputStream inputStream, String contentType, Integer userId) throws IOException {
        Files.createDirectories(avatarDirectory);
        String fileName = "user-" + userId + "-" + UUID.randomUUID() + EXTENSIONS.get(contentType);
        Path destination = avatarDirectory.resolve(fileName).normalize();
        if (!destination.startsWith(avatarDirectory)) {
            throw new BusinessException(ErrorCode.AVATAR_STORAGE_ERROR, "Invalid avatar storage path");
        }

        Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/avatars/" + fileName;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_AVATAR_TYPE, "Only JPEG, PNG, and WEBP images are supported");
        }
        return contentType.toLowerCase(Locale.ROOT);
    }

    private void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_AVATAR_TYPE, "Only JPEG, PNG, and WEBP images are supported");
        }
    }

    private void validateSignature(String contentType, byte[] header) {
        boolean valid = switch (contentType) {
            case "image/jpeg" -> header.length >= 3
                    && (header[0] & 0xff) == 0xff
                    && (header[1] & 0xff) == 0xd8
                    && (header[2] & 0xff) == 0xff;
            case "image/png" -> header.length >= 8
                    && (header[0] & 0xff) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4e
                    && header[3] == 0x47;
            case "image/webp" -> header.length >= 12
                    && header[0] == 0x52
                    && header[1] == 0x49
                    && header[2] == 0x46
                    && header[3] == 0x46
                    && header[8] == 0x57
                    && header[9] == 0x45
                    && header[10] == 0x42
                    && header[11] == 0x50;
            default -> false;
        };

        if (!valid) {
            throw new BusinessException(ErrorCode.INVALID_AVATAR, "Avatar content does not match a supported image format");
        }
    }
}
