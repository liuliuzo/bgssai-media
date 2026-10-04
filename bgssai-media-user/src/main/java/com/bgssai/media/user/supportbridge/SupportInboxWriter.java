package com.bgssai.media.user.supportbridge;

/** 把统一客服的回复和状态写回本产品收件箱。 */
public interface SupportInboxWriter {
    void appendAgentReply(String externalSessionId, String body);

    void appendTicketReply(String externalTicketId, String externalSessionId, String body);

    void updateSessionStatus(String externalSessionId, String status);

    void updateTicketStatus(String externalTicketId, String externalSessionId, String status);
}
