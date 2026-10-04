package com.bgssai.media.user.supportbridge;

import java.util.Date;

/** 对应表 cs_support_receipt。统一客服回传幂等回执。 */
public class CsSupportReceipt {
    private Long id;
    private String deliveryId;
    private String eventKind;
    private Date appliedAt;
    private Byte delFlag;
    private String creator;
    private String modifier;
    private Date gmtCreate;
    private Date gmtModified;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }
    public String getEventKind() { return eventKind; }
    public void setEventKind(String eventKind) { this.eventKind = eventKind; }
    public Date getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Date appliedAt) { this.appliedAt = appliedAt; }
    public Byte getDelFlag() { return delFlag; }
    public void setDelFlag(Byte delFlag) { this.delFlag = delFlag; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public String getModifier() { return modifier; }
    public void setModifier(String modifier) { this.modifier = modifier; }
    public Date getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(Date gmtCreate) { this.gmtCreate = gmtCreate; }
    public Date getGmtModified() { return gmtModified; }
    public void setGmtModified(Date gmtModified) { this.gmtModified = gmtModified; }
}
