package com.ojtsu26.elearning.service;

import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class RemoteVideoUrlValidator {

    private static final int MAX_REDIRECTS = 3;
    private static final List<String> ALLOWED_CONTENT_TYPE_PREFIXES = List.of(
            "video/",
            "application/vnd.apple.mpegurl",
            "application/x-mpegurl"
    );

    private final HttpClient httpClient;

    public RemoteVideoUrlValidator() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    RemoteVideoUrlValidator(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String validateDirectVideoUrl(String rawUrl) {
        URI uri = parseHttpsUri(rawUrl);
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            assertPublicHost(uri);
            try {
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(Duration.ofSeconds(8))
                        .header("User-Agent", "LumiNa-VideoValidator/1.0")
                        .build();
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    Optional<String> location = response.headers().firstValue("Location");
                    if (location.isEmpty()) {
                        throw new RuntimeException("The remote video redirected without a location.");
                    }
                    uri = parseHttpsUri(uri.resolve(location.get()).toString());
                    continue;
                }
                if (status < 200 || status >= 300) {
                    throw new RuntimeException("The remote video URL is not reachable.");
                }
                String contentType = response.headers().firstValue("Content-Type")
                        .map(value -> value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT))
                        .orElse("");
                if (!isAllowedContentType(contentType)) {
                    throw new RuntimeException("The remote URL is not a supported public video.");
                }
                return uri.toString();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Timed out while checking the remote video URL.");
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException("Unable to check the remote video URL.");
            }
        }
        throw new RuntimeException("The remote video URL redirected too many times.");
    }

    private URI parseHttpsUri(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new RuntimeException("Video URL is required.");
        }
        try {
            URI uri = URI.create(rawUrl.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new RuntimeException("Direct video URLs must be HTTPS public media URLs.");
            }
            return uri.normalize();
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("The video source URL is not valid.");
        }
    }

    private void assertPublicHost(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new RuntimeException("The video source URL is not valid.");
        }
        String lowerHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(lowerHost)
                || lowerHost.endsWith(".localhost")
                || "metadata.google.internal".equals(lowerHost)) {
            throw new RuntimeException("Private or internal video URLs are not allowed.");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (!isPublicAddress(address)) {
                    throw new RuntimeException("Private or internal video URLs are not allowed.");
                }
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Unable to resolve the remote video host.");
        }
    }

    private boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address && bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            if (first == 0 || first == 10 || first == 127 || first >= 224) {
                return false;
            }
            if (first == 169 && second == 254) {
                return false;
            }
            if (first == 172 && second >= 16 && second <= 31) {
                return false;
            }
            if (first == 192 && second == 168) {
                return false;
            }
            return !(first == 100 && second >= 64 && second <= 127);
        }
        if (address instanceof Inet6Address && bytes.length == 16) {
            int first = bytes[0] & 0xFF;
            return first < 0xFC;
        }
        return true;
    }

    private boolean isAllowedContentType(String contentType) {
        return ALLOWED_CONTENT_TYPE_PREFIXES.stream()
                .anyMatch(contentType::startsWith);
    }
}
