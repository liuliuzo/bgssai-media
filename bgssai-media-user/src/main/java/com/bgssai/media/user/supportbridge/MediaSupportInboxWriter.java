package com.bgssai.media.user.supportbridge;

import java.util.Date;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bgssai.media.common.domain.MediaSupportMessage;
import com.bgssai.media.common.domain.MediaSupportSession;
import com.bgssai.media.common.domain.MediaSupportTicket;
import com.bgssai.media.common.mapper.MediaSupportMessageMapper;
import com.bgssai.media.common.mapper.MediaSupportSessionMapper;
import com.bgssai.media.common.mapper.MediaSupportTicketMapper;

/** 统一客服回写到 media 站内客服表。 */
@Service
public class MediaSupportInboxWriter implements SupportInboxWriter {

    private static final int BODY_MAX = 4000;
    private static final Map<String, String> TICKET_STATUS = Map.of(
            "OPEN", "open",
            "IN_PROGRESS", "pending",
            "PENDING", "pending",
            "RESOLVED", "resolved",
            "CLOSED", "closed");

    private final MediaSupportSessionMapper sessionMapper;
    private final MediaSupportMessageMapper messageMapper;
    private final MediaSupportTicketMapper ticketMapper;

    public MediaSupportInboxWriter(MediaSupportSessionMapper sessionMapper, MediaSupportMessageMapper messageMapper,
                                    MediaSupportTicketMapper ticketMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.ticketMapper = ticketMapper;
    }

    @Override
    public void appendAgentReply(String externalSessionId, String body) {
        writeReply(sessionOf(externalSessionId), body);
    }

    @Override
    public void appendTicketReply(String externalTicketId, String externalSessionId, String body) {
        writeReply(sessionForTicket(externalTicketId, externalSessionId), body);
    }

    @Override
    public void updateSessionStatus(String externalSessionId, String status) {
        String mapped = switch (status == null ? "" : status.trim()) {
            case "open" -> "open";
            case "pending" -> "pending";
            case "closed" -> "closed";
            default -> throw new SupportBridgeException(400, "无法识别的会话状态");
        };
        MediaSupportSession session = sessionOf(externalSessionId);
        MediaSupportSession patch = new MediaSupportSession();
        patch.setId(session.getId());
        patch.setStatus(mapped);
        if (sessionMapper.updateByPrimaryKeySelective(patch) != 1) {
            throw new SupportBridgeException(500, "更新客服会话失败");
        }
    }

    @Override
    public void updateTicketStatus(String externalTicketId, String externalSessionId, String status) {
        MediaSupportTicket ticket = ticketOf(externalTicketId);
        if (externalSessionId != null && !externalSessionId.isBlank()
                && !externalSessionId.equals(String.valueOf(ticket.getSessionId()))) {
            throw new SupportBridgeException(422, "工单与会话不匹配");
        }
        String key = status == null ? "" : status.trim().toUpperCase();
        String mapped = TICKET_STATUS.get(key);
        if (mapped == null) {
            throw new SupportBridgeException(400, "无法识别的工单状态");
        }
        MediaSupportTicket patch = new MediaSupportTicket();
        patch.setId(ticket.getId());
        patch.setStatus(mapped);
        if (ticketMapper.updateByPrimaryKeySelective(patch) != 1) {
            throw new SupportBridgeException(500, "更新客服工单失败");
        }
    }

    private void writeReply(MediaSupportSession session, String body) {
        if (body == null || body.isBlank()) {
            throw new SupportBridgeException(400, "客服回复为空");
        }
        if (body.length() > BODY_MAX) {
            throw new SupportBridgeException(400, "客服回复超过长度限制");
        }
        Date now = new Date();
        MediaSupportMessage message = new MediaSupportMessage();
        message.setSessionId(session.getId());
        message.setSenderType("admin");
        message.setContent(body);
        if (messageMapper.insertSelective(message) != 1) {
            throw new SupportBridgeException(500, "写入客服回复失败");
        }
        int unread = session.getUserUnread() == null ? 0 : session.getUserUnread();
        MediaSupportSession patch = new MediaSupportSession();
        patch.setId(session.getId());
        patch.setStatus("open");
        patch.setLastMessageAt(now);
        patch.setUserUnread(unread + 1);
        if (sessionMapper.updateByPrimaryKeySelective(patch) != 1) {
            throw new SupportBridgeException(500, "更新客服会话失败");
        }
    }

    private MediaSupportSession sessionForTicket(String externalTicketId, String externalSessionId) {
        if (externalTicketId != null && !externalTicketId.isBlank()) {
            MediaSupportTicket ticket = ticketOf(externalTicketId);
            if (externalSessionId != null && !externalSessionId.isBlank()
                    && !externalSessionId.equals(String.valueOf(ticket.getSessionId()))) {
                throw new SupportBridgeException(422, "工单与会话不匹配");
            }
            return sessionOf(String.valueOf(ticket.getSessionId()));
        }
        return sessionOf(externalSessionId);
    }

    private MediaSupportSession sessionOf(String externalId) {
        Long id = parseId(externalId);
        if (id == null) {
            throw new SupportBridgeException(422, "无法对应客服会话");
        }
        MediaSupportSession row = sessionMapper.selectByPrimaryKey(id);
        if (row == null) {
            throw new SupportBridgeException(422, "无法对应客服会话");
        }
        return row;
    }

    private MediaSupportTicket ticketOf(String externalId) {
        Long id = parseId(externalId);
        if (id == null) {
            throw new SupportBridgeException(422, "无法对应客服工单");
        }
        MediaSupportTicket row = ticketMapper.selectByPrimaryKey(id);
        if (row == null) {
            throw new SupportBridgeException(422, "无法对应客服工单");
        }
        return row;
    }

    private static Long parseId(String raw) {
        if (raw == null || !raw.matches("[0-9]{1,18}")) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
