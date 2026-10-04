package com.bgssai.media.user.supportbridge;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** 发给统一客服的 camelCase JSON。外部编号稳定，重试不换号，不带会话令牌。 */
public final class SupportPayloads {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int LABEL_MAX = 80;
    private static final int SUBJECT_MAX = 180;

    private SupportPayloads() {
    }

    public static String sessionJson(String sessionId, String label, String subject, String body, String messageId) {
        ObjectNode node = JSON.createObjectNode();
        node.put("externalId", sessionId);
        node.put("visitorId", sessionId);
        node.put("visitorLabel", clip(blank(label) ? "访客" : label, LABEL_MAX));
        node.put("subject", clip(blank(subject) ? "在线客服" : subject, SUBJECT_MAX));
        if (!blank(body)) {
            node.put("body", body.trim());
        }
        if (!blank(messageId)) {
            node.put("messageExternalId", messageId);
        }
        return write(node);
    }

    public static String messageJson(String sessionId, String messageId, String body) {
        ObjectNode node = JSON.createObjectNode();
        node.put("externalId", messageId);
        node.put("visitorId", sessionId);
        node.put("body", body.trim());
        return write(node);
    }

    public static String ticketJson(String sessionId, String ticketId, String subject) {
        String title = blank(subject) ? "客服工单" : subject.trim();
        ObjectNode node = JSON.createObjectNode();
        node.put("externalId", ticketId);
        node.put("visitorId", sessionId);
        node.put("subject", clip(title, SUBJECT_MAX));
        node.put("sessionId", sessionId);
        node.put("description", title);
        node.put("status", "OPEN");
        node.put("priority", "normal");
        return write(node);
    }

    public static String sessionBootstrapJson(String sessionId, String visitorId) {
        ObjectNode node = JSON.createObjectNode();
        node.put("externalId", sessionId);
        node.put("visitorId", visitorId);
        node.put("visitorLabel", "访客");
        node.put("subject", "在线客服");
        return write(node);
    }

    private static String clip(String value, int max) {
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String write(ObjectNode node) {
        try {
            return JSON.writeValueAsString(node);
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成客服报文", ex);
        }
    }
}
