package com.bgssai.media.user.supportbridge;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SupportCallbackReceiver {

    private static final Logger log = LoggerFactory.getLogger(SupportCallbackReceiver.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final SupportBridgeProperties properties;
    private final SupportCallbackService service;

    public SupportCallbackReceiver(SupportBridgeProperties properties, SupportCallbackService service) {
        this.properties = properties;
        this.service = service;
    }

    public ResponseEntity<Map<String, Object>> receive(byte[] raw, String signature, String idempotencyKey) {
        log.info("接收统一客服回调");
        if (!properties.isReady()) {
            return reply(503, "客服同步未启用");
        }
        if (raw == null || raw.length == 0 || raw.length > 65536) {
            return reply(400, "回调长度无效");
        }
        if (!SupportSignatures.matches(properties.getIngestSecret(), raw, signature)) {
            return reply(401, "签名无效");
        }
        JsonNode root;
        try {
            root = JSON.readTree(raw);
        } catch (Exception ex) {
            return reply(400, "回调格式无效");
        }
        if (root == null || !root.isObject()) {
            return reply(400, "回调格式无效");
        }
        SupportCallbackEvent event = new SupportCallbackEvent();
        event.setProductCode(text(root, "productCode"));
        event.setEvent(text(root, "event"));
        event.setDeliveryId(text(root, "deliveryId"));
        event.setExternalSessionId(text(root, "externalSessionId"));
        event.setExternalTicketId(text(root, "externalTicketId"));
        event.setStatus(text(root, "status"));
        JsonNode message = root.get("message");
        if (message != null && message.isObject()) {
            event.setMessageBody(text(message, "body"));
        }
        if (!properties.getProductCode().equals(event.getProductCode())) {
            return reply(403, "产品不匹配");
        }
        if (event.getDeliveryId() == null || event.getDeliveryId().isBlank()
                || !event.getDeliveryId().equals(idempotencyKey)) {
            return reply(400, "幂等编号不匹配");
        }
        try {
            service.apply(event);
            return reply(200, "success");
        } catch (SupportBridgeException ex) {
            int code = httpCode(ex.getCode());
            log.warn("统一客服回传未应用 code={}", code);
            return reply(code, ex.getMessage());
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    private static int httpCode(Object raw) {
        String text = raw == null ? "" : String.valueOf(raw);
        if (text.matches("[1-5][0-9]{2}")) {
            return Integer.parseInt(text);
        }
        return 500;
    }

    private static ResponseEntity<Map<String, Object>> reply(int http, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", http >= 200 && http < 300 ? 0 : http);
        body.put("message", message);
        if (http >= 200 && http < 300) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.status(http).body(body);
    }
}
