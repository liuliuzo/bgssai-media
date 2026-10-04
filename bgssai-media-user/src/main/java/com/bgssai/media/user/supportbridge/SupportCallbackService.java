package com.bgssai.media.user.supportbridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/** 验签之后把回写进本产品收件箱。回执与业务更新同一事务。 */
@Service
public class SupportCallbackService {

    private static final Logger log = LoggerFactory.getLogger(SupportCallbackService.class);

    private final CsSupportReceiptStore receiptMapper;
    private final SupportInboxWriter writer;

    public SupportCallbackService(CsSupportReceiptStore receiptMapper, SupportInboxWriter writer) {
        this.receiptMapper = receiptMapper;
        this.writer = writer;
    }

    @Transactional
    public void apply(SupportCallbackEvent event) {
        String deliveryId = text(event.getDeliveryId());
        String kind = text(event.getEvent());
        log.info("应用统一客服回调 kind={} delivery_id={}", kind, deliveryId);
        if (deliveryId.isBlank() || deliveryId.length() > 128 || kind.isBlank() || kind.length() > 32) {
            throw new SupportBridgeException(400, "回传缺少事件编号");
        }
        CsSupportReceiptExample example = new CsSupportReceiptExample();
        example.setDeliveryId(deliveryId);
        example.setDelFlag((byte) 0);
        if (receiptMapper.selectOneByExample(example) != null) {
            return;
        }
        CsSupportReceipt receipt = new CsSupportReceipt();
        receipt.setDeliveryId(deliveryId);
        receipt.setEventKind(kind);
        receipt.setAppliedAt(new Date());
        receipt.setDelFlag((byte) 0);
        receipt.setCreator("support");
        receipt.setModifier("support");
        try {
            if (receiptMapper.insert(receipt) != 1) {
                throw new SupportBridgeException(500, "记录客服回传失败");
            }
        } catch (DuplicateKeyException ex) {
            log.info("统一客服回传已处理 delivery_id={}", deliveryId);
            return;
        }
        switch (kind) {
            case "agent.reply" -> writer.appendAgentReply(text(event.getExternalSessionId()), text(event.getMessageBody()));
            case "ticket.reply" -> writer.appendTicketReply(text(event.getExternalTicketId()),
                    text(event.getExternalSessionId()), text(event.getMessageBody()));
            case "session.status" -> writer.updateSessionStatus(text(event.getExternalSessionId()), text(event.getStatus()));
            case "ticket.status" -> writer.updateTicketStatus(text(event.getExternalTicketId()),
                    text(event.getExternalSessionId()), text(event.getStatus()));
            default -> throw new SupportBridgeException(400, "不支持的客服回传事件");
        }
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
