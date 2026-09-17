package com.bgssai.media.common.sms;

/**
 * 单次短信发码结果：服务商 RequestId + 写入模板的产品名与序号。
 *
 * <p>序号由本进程递增生成，供客服对照短信正文；发码 API 原样回给调用方。
 */
public final class SmsSendResult {

    private final String product;
    private final long seq;
    private final String requestId;

    public SmsSendResult(String product, long seq, String requestId) {
        this.product = product;
        this.seq = seq;
        this.requestId = requestId;
    }

    public String getProduct() {
        return product;
    }

    public long getSeq() {
        return seq;
    }

    public String getRequestId() {
        return requestId;
    }
}
