package com.bgssai.media.common.publish;

import com.bgssai.media.common.dto.ShortDramaIngestRequest;
import com.bgssai.media.common.web.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP poster short (or a local smoke job) uses to publish a packaged short to media.
 * No secrets are hardcoded; caller supplies the ingest token at runtime.
 */
public final class ShortDramaPublishClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String mediaBaseUrl;

    public ShortDramaPublishClient(String mediaBaseUrl) {
        this(mediaBaseUrl, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), snakeMapper());
    }

    static ObjectMapper snakeMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }

    public ShortDramaPublishClient(String mediaBaseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        if (mediaBaseUrl == null || mediaBaseUrl.isBlank()) {
            throw new BizException(400, "media_base_url required");
        }
        this.mediaBaseUrl = trimSlash(mediaBaseUrl);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public String ingestUrl() {
        return mediaBaseUrl + ShortDramaPublishJob.INGEST_PATH;
    }

    public HttpRequest buildRequest(String ingestToken, ShortDramaIngestRequest payload) {
        if (ingestToken == null || ingestToken.isBlank()) {
            throw new BizException(401, "ingest token required");
        }
        if (payload == null) {
            throw new BizException(400, "payload required");
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            return HttpRequest.newBuilder()
                    .uri(URI.create(ingestUrl()))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header(ShortDramaPublishJob.TOKEN_HEADER, ingestToken)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "publish payload encode failed");
        }
    }

    public Map<String, Object> publish(String ingestToken, ShortDramaIngestRequest payload) {
        try {
            HttpResponse<String> response = httpClient.send(
                    buildRequest(ingestToken, payload), HttpResponse.BodyHandlers.ofString());
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(response.body(), Map.class);
            return body;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(502, "media ingest call failed");
        }
    }

    private static String trimSlash(String url) {
        String v = url.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}
