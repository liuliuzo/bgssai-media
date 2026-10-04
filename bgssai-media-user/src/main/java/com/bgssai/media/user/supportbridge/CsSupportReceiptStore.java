package com.bgssai.media.user.supportbridge;

public interface CsSupportReceiptStore {
    int insert(CsSupportReceipt row);

    CsSupportReceipt selectOneByExample(CsSupportReceiptExample example);
}
