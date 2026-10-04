package com.bgssai.media.user.supportbridge;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * 把本产品客服会话、访客消息和工单写入可重试队列，再推到统一客服。
 * 未配置时不入队、不外呼。投递失败留在队列里，重启不丢。
 */
@Service
public class SupportOutboxService {

    private static final Logger log = LoggerFactory.getLogger(SupportOutboxService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final SupportBridgeProperties properties;
    private final CsSupportOutboxStore outboxMapper;
    private final HttpClient httpClient;

    public SupportOutboxService(SupportBridgeProperties properties, CsSupportOutboxStore outboxMapper) {
        this.properties = properties;
        this.outboxMapper = outboxMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public void enqueueSession(String sessionId, String label, String subject, String body, String messageId) {
        if (!properties.isReady()) {
            return;
        }
        if (blank(sessionId)) {
            throw new SupportBridgeException(500, "客服队列写入失败");
        }
        enqueue("SESSION", "session:" + sessionId, "/sessions",
                SupportPayloads.sessionJson(sessionId, label, subject, body, messageId));
    }

    public void enqueueMessage(String sessionId, String messageId, String body) {
        if (!properties.isReady()) {
            return;
        }
        if (blank(sessionId) || blank(messageId) || blank(body)) {
            throw new SupportBridgeException(500, "客服队列写入失败");
        }
        enqueue("MESSAGE", "message:" + messageId, "/sessions/" + sessionId + "/messages",
                SupportPayloads.messageJson(sessionId, messageId, body));
    }

    public void enqueueTicket(String sessionId, String ticketId, String subject) {
        if (!properties.isReady()) {
            return;
        }
        if (blank(sessionId) || blank(ticketId)) {
            throw new SupportBridgeException(500, "客服队列写入失败");
        }
        enqueue("TICKET", "ticket:" + ticketId, "/tickets",
                SupportPayloads.ticketJson(sessionId, ticketId, subject));
    }

    public void flush() {
        if (!properties.isReady()) {
            return;
        }
        CsSupportOutboxExample example = new CsSupportOutboxExample();
        example.setStatus("PENDING");
        example.setDelFlag((byte) 0);
        example.setDueAt(new Date());
        for (CsSupportOutbox row : outboxMapper.selectByExample(example)) {
            deliver(row);
        }
    }

    private void enqueue(String kind, String ref, String path, String payload) {
        CsSupportOutbox row = new CsSupportOutbox();
        row.setEventKind(kind);
        row.setExternalRef(ref);
        row.setRequestPath(path);
        row.setPayloadJson(payload);
        row.setStatus("PENDING");
        row.setAttemptCount(0);
        row.setDelFlag((byte) 0);
        row.setCreator("support-bridge");
        row.setModifier("support-bridge");
        try {
            if (outboxMapper.insert(row) != 1) {
                throw new SupportBridgeException(500, "客服队列写入失败");
            }
        } catch (DuplicateKeyException ex) {
            log.info("统一客服队列已有相同事件 kind={} ref={}", kind, ref);
        } catch (SupportBridgeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("写入统一客服队列失败 kind={} ref={} error={}", kind, ref, ex.getClass().getSimpleName());
            throw new SupportBridgeException(500, "客服队列写入失败，请稍后重试");
        }
    }

    private void deliver(CsSupportOutbox row) {
        int attempts = row.getAttemptCount() == null ? 0 : row.getAttemptCount();
        try {
            int code = post(row.getRequestPath(), row.getPayloadJson());
            if (code == 404) {
                code = bootstrapAndRetry(row);
            }
            if (code >= 200 && code < 300) {
                update(row.getId(), "SENT", attempts + 1, "", null, new Date());
                return;
            }
            String error = "HTTP " + code;
            if (code == 408 || code == 429 || code >= 500 || code == 404) {
                scheduleRetry(row.getId(), attempts + 1, error);
            } else {
                update(row.getId(), "FAILED", attempts + 1, clip(error), null, null);
            }
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            scheduleRetry(row.getId(), attempts + 1, ex.getClass().getSimpleName());
        }
    }

    private int bootstrapAndRetry(CsSupportOutbox row) throws Exception {
        String sessionId = sessionIdFrom(row);
        String visitorId = text(row.getPayloadJson(), "visitorId");
        if (sessionId == null || visitorId == null) {
            return 404;
        }
        int boot = post("/sessions", SupportPayloads.sessionBootstrapJson(sessionId, visitorId));
        if (boot < 200 || boot >= 300) {
            return boot;
        }
        return post(row.getRequestPath(), row.getPayloadJson());
    }

    private int post(String path, String payload) throws Exception {
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        String signature = SupportSignatures.sign(properties.getIngestSecret(), body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.normalizedBaseUrl()
                        + "/api/ingest/" + properties.getProductCode() + path))
                .timeout(Duration.ofSeconds(10))
                .header("content-type", "application/json")
                .header("x-support-signature", "sha256=" + signature)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode();
    }

    private void scheduleRetry(Long id, int attempts, String error) {
        if (attempts >= 30) {
            update(id, "FAILED", attempts, clip(error), null, null);
            return;
        }
        long seconds = Math.min(3600L, 1L << Math.min(attempts, 11));
        update(id, "PENDING", attempts, clip(error), new Date(System.currentTimeMillis() + seconds * 1000L), null);
    }

    private void update(Long id, String status, int attempts, String error, Date next, Date sent) {
        CsSupportOutboxExample example = new CsSupportOutboxExample();
        example.setId(id);
        example.setStatus("PENDING");
        example.setDelFlag((byte) 0);
        CsSupportOutbox patch = new CsSupportOutbox();
        patch.setStatus(status);
        patch.setAttemptCount(attempts);
        patch.setLastError(error);
        patch.setNextAttemptAt(next);
        patch.setSentAt(sent);
        patch.setModifier("support-bridge");
        outboxMapper.updateByExampleSelective(patch, example);
        log.info("统一客服投递 id={} status={} attempt={}", id, status, attempts);
    }

    private static String sessionIdFrom(CsSupportOutbox row) {
        String path = row.getRequestPath();
        if (path != null && path.startsWith("/sessions/") && path.contains("/messages")) {
            String id = path.substring("/sessions/".length(), path.indexOf("/messages"));
            return id.isBlank() ? null : id;
        }
        return text(row.getPayloadJson(), "sessionId");
    }

    private static String text(String payload, String field) {
        try {
            JsonNode node = JSON.readTree(payload).get(field);
            if (node == null || node.isNull()) {
                return null;
            }
            String value = node.asText("");
            return value.isBlank() ? null : value;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String clip(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 500 ? error : error.substring(0, 500);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
