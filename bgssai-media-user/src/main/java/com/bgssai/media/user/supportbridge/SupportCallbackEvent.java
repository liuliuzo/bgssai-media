package com.bgssai.media.user.supportbridge;

/** 已验签的统一客服回调。多余字段在解析时丢弃。 */
public final class SupportCallbackEvent {
    private String productCode;
    private String event;
    private String deliveryId;
    private String externalSessionId;
    private String externalTicketId;
    private String status;
    private String messageBody;

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }
    public String getExternalSessionId() { return externalSessionId; }
    public void setExternalSessionId(String externalSessionId) { this.externalSessionId = externalSessionId; }
    public String getExternalTicketId() { return externalTicketId; }
    public void setExternalTicketId(String externalTicketId) { this.externalTicketId = externalTicketId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessageBody() { return messageBody; }
    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }
}
