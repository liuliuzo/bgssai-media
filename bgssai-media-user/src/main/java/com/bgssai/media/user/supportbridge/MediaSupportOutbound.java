package com.bgssai.media.user.supportbridge;

import org.springframework.stereotype.Component;

import com.bgssai.media.common.service.SupportOutbound;

@Component
public class MediaSupportOutbound implements SupportOutbound {

    private final SupportOutboxService outbox;

    public MediaSupportOutbound(SupportOutboxService outbox) {
        this.outbox = outbox;
    }

    @Override
    public void enqueueSession(String sessionId, String label, String subject, String body, String messageId) {
        outbox.enqueueSession(sessionId, label, subject, body, messageId);
    }

    @Override
    public void enqueueMessage(String sessionId, String messageId, String body) {
        outbox.enqueueMessage(sessionId, messageId, body);
    }

    @Override
    public void enqueueTicket(String sessionId, String ticketId, String subject) {
        outbox.enqueueTicket(sessionId, ticketId, subject);
    }
}
