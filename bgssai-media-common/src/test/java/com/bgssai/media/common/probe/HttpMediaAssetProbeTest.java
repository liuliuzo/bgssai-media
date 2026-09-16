package com.bgssai.media.common.probe;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MEDIA-01 acceptance: the four ways an ingest used to reach a fake READY — a dead link, an
 * HTML page served with 200, a private object, and bytes no player can decode — must all be
 * kept out of READY, while a genuine media body gets through.
 */
class HttpMediaAssetProbeTest {

    private HttpServer server;
    private String base;
    private HttpMediaAssetProbe probe;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/good.mp4", ex -> {
            byte[] body = mp4Bytes();
            ex.getResponseHeaders().add("Content-Type", "video/mp4");
            ex.getResponseHeaders().add("Accept-Ranges", "bytes");
            ex.getResponseHeaders().add("Content-Range", "bytes 0-" + (body.length - 1) + "/40960");
            respond(ex, 206, body);
        });
        server.createContext("/playlist.m3u8", ex -> {
            byte[] body = "#EXTM3U\n#EXT-X-VERSION:3\n#EXTINF:6.0,\nseg0.ts\n"
                    .getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/vnd.apple.mpegurl");
            respond(ex, 200, body);
        });
        server.createContext("/missing.mp4", ex ->
                respond(ex, 404, "not found".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/private.mp4", ex ->
                respond(ex, 403, "forbidden".getBytes(StandardCharsets.UTF_8)));
        // The dangerous case: 200 OK, a .mp4 path, and an HTML login wall in the body.
        server.createContext("/login.mp4", ex -> {
            byte[] body = "<!DOCTYPE html><html><body>Please sign in</body></html>"
                    .getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            respond(ex, 200, body);
        });
        // 200 OK, declared as video, but the bytes are not any container.
        server.createContext("/garbage.mp4", ex -> {
            byte[] body = new byte[2048];
            for (int i = 0; i < body.length; i++) {
                body[i] = (byte) (i % 97 + 1);
            }
            ex.getResponseHeaders().add("Content-Type", "video/mp4");
            respond(ex, 200, body);
        });
        server.createContext("/flaky.mp4", ex ->
                respond(ex, 503, "try later".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/empty.mp4", ex -> {
            ex.getResponseHeaders().add("Content-Type", "video/mp4");
            respond(ex, 200, new byte[0]);
        });

        server.setExecutor(null);
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        probe = new HttpMediaAssetProbe(HttpClient.newHttpClient(), true, 8192, 5000);
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void realMp4IsPlayableAndSeekable() {
        MediaProbeResult result = probe.probe(base + "/good.mp4", null);
        assertTrue(result.isPlayable(), result.getMessage());
        assertEquals("mp4", result.getContainer());
        assertTrue(result.isSeekable(), "range support is what lets the player seek and resume");
        assertEquals(40960L, result.getContentLength());
    }

    @Test
    void hlsPlaylistIsPlayable() {
        MediaProbeResult result = probe.probe(base + "/playlist.m3u8", null);
        assertTrue(result.isPlayable(), result.getMessage());
        assertEquals("hls", result.getContainer());
    }

    @Test
    void deadLinkIsRejected() {
        MediaProbeResult result = probe.probe(base + "/missing.mp4", null);
        assertEquals(MediaProbeResult.Verdict.REJECTED, result.getVerdict());
        assertTrue(result.getMessage().contains("404"), result.getMessage());
    }

    @Test
    void privateObjectIsRejected() {
        MediaProbeResult result = probe.probe(base + "/private.mp4", null);
        assertEquals(MediaProbeResult.Verdict.REJECTED, result.getVerdict());
        assertTrue(result.getMessage().contains("403"), result.getMessage());
    }

    @Test
    void htmlMasqueradingAsVideoIsRejected() {
        MediaProbeResult result = probe.probe(base + "/login.mp4", null);
        assertEquals(MediaProbeResult.Verdict.REJECTED, result.getVerdict());
        assertTrue(result.getMessage().contains("HTML"), result.getMessage());
        assertFalse(result.isPlayable());
    }

    @Test
    void undecodableBytesAreRejectedEvenWithVideoContentType() {
        MediaProbeResult result = probe.probe(base + "/garbage.mp4", null);
        assertEquals(MediaProbeResult.Verdict.REJECTED, result.getVerdict());
        assertTrue(result.getMessage().contains("not decodable"), result.getMessage());
    }

    @Test
    void emptyBodyIsRejected() {
        MediaProbeResult result = probe.probe(base + "/empty.mp4", null);
        assertEquals(MediaProbeResult.Verdict.REJECTED, result.getVerdict());
    }

    @Test
    void originErrorStaysRetryableSoTheRetryIsNotBurned() {
        MediaProbeResult result = probe.probe(base + "/flaky.mp4", null);
        assertEquals(MediaProbeResult.Verdict.RETRYABLE, result.getVerdict());
    }

    @Test
    void unreachableHostIsRetryableNotTerminal() {
        // Port 1 on loopback refuses connections: a transport failure, not a fact about the asset.
        MediaProbeResult result = probe.probe("http://127.0.0.1:1/whatever.mp4", null);
        assertEquals(MediaProbeResult.Verdict.RETRYABLE, result.getVerdict());
    }

    @Test
    void storageKeyWithoutUrlIsUnverifiedNotPlayable() {
        MediaProbeResult result = probe.probe("", "short/film/1.mp4");
        assertEquals(MediaProbeResult.Verdict.SKIPPED, result.getVerdict());
        assertFalse(result.isPlayable());
    }

    @Test
    void noAssetAtAllIsRejected() {
        MediaProbeResult result = probe.probe(null, null);
        assertEquals(MediaProbeResult.Verdict.REJECTED, result.getVerdict());
    }

    @Test
    void nonHttpSchemeIsRejected() {
        MediaProbeResult result = probe.probe("file:///etc/passwd", null);
        assertEquals(MediaProbeResult.Verdict.REJECTED, result.getVerdict());
    }

    @Test
    void disabledProbeNeverClaimsPlayable() {
        HttpMediaAssetProbe off = new HttpMediaAssetProbe(HttpClient.newHttpClient(), false, 8192, 5000);
        MediaProbeResult result = off.probe(base + "/good.mp4", null);
        assertEquals(MediaProbeResult.Verdict.SKIPPED, result.getVerdict());
        assertFalse(result.isPlayable());
    }

    /** Minimal ISO base media file: size + "ftyp" + brand. */
    private static byte[] mp4Bytes() {
        byte[] body = new byte[1024];
        body[0] = 0;
        body[1] = 0;
        body[2] = 0;
        body[3] = 0x18;
        byte[] tag = "ftypisom".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(tag, 0, body, 4, tag.length);
        return body;
    }

    private static void respond(HttpExchange ex, int status, byte[] body) throws IOException {
        if (body.length == 0) {
            ex.sendResponseHeaders(status, -1);
            ex.close();
            return;
        }
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }
}
