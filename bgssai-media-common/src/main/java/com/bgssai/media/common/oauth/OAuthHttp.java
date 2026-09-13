package com.bgssai.media.common.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

@Component
public class OAuthHttp {

    private final HttpClient client;
    private final ObjectMapper mapper;

    public OAuthHttp() {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.mapper = new ObjectMapper();
    }

    public JsonNode get(String url) {
        return send(HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(20)).build());
    }

    public JsonNode getBearer(String url, String token) {
        return send(HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .build());
    }

    public JsonNode postForm(String url, Map<String, String> form) {
        return send(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody(form)))
                .timeout(Duration.ofSeconds(20))
                .build());
    }

    public String postFormRaw(String url, Map<String, String> form) {
        try {
            HttpResponse<String> res = client.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(formBody(form)))
                            .timeout(Duration.ofSeconds(20))
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return res.body();
        } catch (Exception e) {
            throw new IllegalStateException("oauth http failed", e);
        }
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.body() == null || res.body().isBlank()) {
                return mapper.createObjectNode();
            }
            return mapper.readTree(res.body());
        } catch (Exception e) {
            throw new IllegalStateException("oauth http failed", e);
        }
    }

    static String formBody(Map<String, String> form) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> e : form.entrySet()) {
            joiner.add(enc(e.getKey()) + "=" + enc(e.getValue() == null ? "" : e.getValue()));
        }
        return joiner.toString();
    }

    static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return "";
        }
        String value = node.get(field).asText();
        return value == null || "null".equals(value) ? "" : value;
    }
}
