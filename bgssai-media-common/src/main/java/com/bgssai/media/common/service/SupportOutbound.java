package com.bgssai.media.common.service;

/** 用户端可选的统一客服出站。管理端没有实现时保持为空。 */
public interface SupportOutbound {
    void enqueueSession(String sessionId, String label, String subject, String body, String messageId);

    void enqueueMessage(String sessionId, String messageId, String body);

    void enqueueTicket(String sessionId, String ticketId, String subject);
}
