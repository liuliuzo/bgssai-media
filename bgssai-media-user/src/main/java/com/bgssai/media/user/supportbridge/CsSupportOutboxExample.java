package com.bgssai.media.user.supportbridge;

import java.util.Date;

/** cs_support_outbox 的白名单查询条件。 */
public class CsSupportOutboxExample {
    private Long id;
    private String status;
    private Byte delFlag;
    private Date dueAt;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Byte getDelFlag() { return delFlag; }
    public void setDelFlag(Byte value) { delFlag = value; }
    public Date getDueAt() { return dueAt; }
    public void setDueAt(Date value) { dueAt = value; }
}
