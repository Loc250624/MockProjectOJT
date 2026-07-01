package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Service
public class ExternalImageService {

    private final HttpClient httpClient;
    private final int timeoutMillis;
    private final long maxSize;

    public ExternalImageService(
            @Value("${app.avatar.remote-timeout:5000}") int timeoutMillis,
            @Value("${app.avatar.remote-max-size:5242880}") long maxSize) {
        this.timeoutMillis = timeoutMillis;
        this.maxSize = maxSize;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMillis))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public DownloadedImage download(String imageUrl) {
        URI uri = parseAndValidateUri(imageUrl);

        try {
            for (int redirectCount = 0; redirectCount < 3; redirectCount += 1) {
                validateHost(uri);
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofMillis(timeoutMillis))
                        .GET()
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    Optional<String> location = response.headers().firstValue("location");
                    if (location.isEmpty()) {
                        throw new BusinessException(ErrorCode.INVALID_AVATAR, "Remote image redirect is invalid");
                    }
                    uri = uri.resolve(location.get());
                    parseAndValidateUri(uri.toString());
                    continue;
                }

                if (status < 200 || status >= 300) {
                    throw new BusinessException(ErrorCode.INVALID_AVATAR, "Remote image is unavailable");
                }

                String contentType = response.headers().firstValue("content-type")
                        .map(value -> value.split(";")[0].trim().toLowerCase(Locale.ROOT))
                        .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_AVATAR_TYPE, "Remote response is not an image"));

                if (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp")) {
                    throw new BusinessException(ErrorCode.UNSUPPORTED_AVATAR_TYPE, "Only JPEG, PNG, and WEBP image URLs are supported");
                }

                return new DownloadedImage(readBounded(response.body()), contentType);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INVALID_AVATAR, "Unable to access remote image");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INVALID_AVATAR, "Remote image request was interrupted");
        }

        throw new BusinessException(ErrorCode.INVALID_AVATAR, "Remote image has too many redirects");
    }

    private URI parseAndValidateUri(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new BusinessException(ErrorCode.INVALID_AVATAR, "Image URL must start with http:// or https://");
            }
            if (uri.getHost() == null || uri.getUserInfo() != null) {
                throw new BusinessException(ErrorCode.INVALID_AVATAR, "Image URL is invalid");
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_AVATAR, "Image URL is invalid");
        }
    }

    private void validateHost(URI uri) {
        String host = uri.getHost();
        if ("localhost".equalsIgnoreCase(host)) {
            throw new BusinessException(ErrorCode.INVALID_AVATAR, "Localhost image URLs are not allowed");
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.getHostAddress().equals("169.254.169.254")) {
                    throw new BusinessException(ErrorCode.INVALID_AVATAR, "Private image URLs are not allowed");
                }
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INVALID_AVATAR, "Image URL host cannot be resolved");
        }
    }

    private byte[] readBounded(InputStream inputStream) throws IOException {
        try (InputStream source = inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = source.read(buffer)) != -1) {
                total += read;
                if (total > maxSize) {
                    throw new BusinessException(ErrorCode.AVATAR_TOO_LARGE, "Remote image must not exceed " + (maxSize / 1024 / 1024) + "MB");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    public record DownloadedImage(byte[] content, String contentType) {
    }
}
