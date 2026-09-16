package com.bgssai.media.common.probe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/**
 * Fetches the first bytes of an ingest asset over HTTP and decides whether it is playable
 * (MEDIA-01).
 *
 * <p>The request is a ranged GET rather than a HEAD: many object stores and CDNs answer HEAD
 * from metadata that was never validated, and a HEAD cannot show that the body is an HTML
 * error page. Asking for {@code Range: bytes=0-N} costs a few kilobytes and simultaneously
 * proves the origin honours range requests, which is what lets the player seek and resume.
 *
 * <p>Verdicts are deliberately split between terminal and retryable. A timeout or a 5xx says
 * nothing about the asset, so it stays PENDING and can be retried; a 404 or an HTML body is a
 * fact about the asset and is terminal.
 */
@Component
public class HttpMediaAssetProbe implements MediaAssetProbe {

    private static final Logger log = LoggerFactory.getLogger(HttpMediaAssetProbe.class);

    private final boolean enabled;
    private final int sniffBytes;
    private final Duration requestTimeout;
    private final HttpClient client;

    public HttpMediaAssetProbe(
            @Value("${bgssai.media.probe.enabled:true}") boolean enabled,
            @Value("${bgssai.media.probe.sniff-bytes:8192}") int sniffBytes,
            @Value("${bgssai.media.probe.connect-timeout-ms:4000}") long connectTimeoutMs,
            @Value("${bgssai.media.probe.request-timeout-ms:10000}") long requestTimeoutMs) {
        this.enabled = enabled;
        this.sniffBytes = Math.max(512, sniffBytes);
        this.requestTimeout = Duration.ofMillis(Math.max(1000, requestTimeoutMs));
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(500, connectTimeoutMs)))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Test seam: inject a client pointed at a local server. */
    HttpMediaAssetProbe(HttpClient client, boolean enabled, int sniffBytes, long requestTimeoutMs) {
        this.enabled = enabled;
        this.sniffBytes = Math.max(512, sniffBytes);
        this.requestTimeout = Duration.ofMillis(Math.max(1000, requestTimeoutMs));
        this.client = client;
    }

    @Override
    public MediaProbeResult probe(String mediaUrl, String storageKey) {
        String url = mediaUrl == null ? "" : mediaUrl.trim();
        if (!enabled) {
            return MediaProbeResult.skipped(
                    "asset not verified: bgssai.media.probe.enabled=false, playability unproven");
        }
        if (url.isEmpty()) {
            String key = storageKey == null ? "" : storageKey.trim();
            if (key.isEmpty()) {
                return MediaProbeResult.rejected("asset missing: no media_url and no storage_key", 0, "");
            }
            return MediaProbeResult.skipped(
                    "asset not verified: storage_key only, supply a playable media_url (a signed URL is fine)");
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return MediaProbeResult.rejected("asset invalid: media_url must be http(s)", 0, "");
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(requestTimeout)
                    .header("Range", "bytes=0-" + (sniffBytes - 1))
                    .header("User-Agent", "bgssai-media-ingest-probe/1")
                    .GET()
                    .build();
        } catch (IllegalArgumentException bad) {
            return MediaProbeResult.rejected("asset invalid: malformed media_url", 0, "");
        }

        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException io) {
            // A network-level failure says nothing about the asset itself; stay retryable.
            return MediaProbeResult.retryable("asset unreachable: " + rootMessage(io), 0);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return MediaProbeResult.retryable("asset probe interrupted", 0);
        }

        return classify(response);
    }

    private MediaProbeResult classify(HttpResponse<byte[]> response) {
        int status = response.statusCode();
        String contentType = response.headers().firstValue("content-type").orElse("");

        if (status == 401 || status == 403) {
            return MediaProbeResult.rejected(
                    "asset not accessible: origin returned " + status + " (private or expired URL)",
                    status, contentType);
        }
        if (status == 404 || status == 410) {
            return MediaProbeResult.rejected(
                    "asset dead: origin returned " + status, status, contentType);
        }
        if (status == 408 || status == 429 || status >= 500) {
            return MediaProbeResult.retryable(
                    "asset probe inconclusive: origin returned " + status, status);
        }
        if (status < 200 || status >= 300) {
            return MediaProbeResult.rejected(
                    "asset not playable: unexpected status " + status, status, contentType);
        }

        byte[] body = response.body() == null ? new byte[0] : response.body();
        if (body.length == 0) {
            return MediaProbeResult.rejected("asset empty: origin returned no bytes", status, contentType);
        }

        String container = MediaContainerSniffer.sniff(body);
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (MediaContainerSniffer.HTML.equals(container)
                || (container.isEmpty()
                        && (ct.startsWith("text/html") || ct.startsWith("application/xhtml")))) {
            return MediaProbeResult.rejected(
                    "asset is an HTML page, not media (status " + status + ")", status, contentType);
        }
        if (container.isEmpty()) {
            return MediaProbeResult.rejected(
                    "asset not decodable: no known media container in the first "
                            + Math.min(body.length, sniffBytes) + " bytes"
                            + (contentType.isBlank() ? "" : " (content-type " + contentType + ")"),
                    status, contentType);
        }

        boolean seekable = status == 206
                || response.headers().firstValue("accept-ranges")
                        .map(v -> v.toLowerCase(Locale.ROOT).contains("bytes"))
                        .orElse(false);
        long length = totalLength(response);
        if (log.isDebugEnabled()) {
            log.debug("media probe ok container={} status={} seekable={}", container, status, seekable);
        }
        return MediaProbeResult.playable(status, contentType, length, container, seekable);
    }

    /** Prefers the total size from {@code Content-Range} over the ranged {@code Content-Length}. */
    private static long totalLength(HttpResponse<byte[]> response) {
        String contentRange = response.headers().firstValue("content-range").orElse("");
        int slash = contentRange.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < contentRange.length()) {
            try {
                return Long.parseLong(contentRange.substring(slash + 1).trim());
            } catch (NumberFormatException ignored) {
                // A "*" total means the origin does not know the full size.
            }
        }
        return response.headers().firstValueAsLong("content-length").orElse(-1L);
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg == null || msg.isBlank() ? cur.getClass().getSimpleName() : msg;
    }
}
