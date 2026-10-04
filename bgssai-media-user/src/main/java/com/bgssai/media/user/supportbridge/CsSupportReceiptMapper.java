package com.bgssai.media.user.supportbridge;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CsSupportReceiptMapper extends CsSupportReceiptStore {

    @Override
    @Insert("INSERT INTO cs_support_receipt (delivery_id, event_kind, applied_at, del_flag, creator, modifier) "
            + "VALUES (#{deliveryId}, #{eventKind}, #{appliedAt}, #{delFlag}, #{creator}, #{modifier})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CsSupportReceipt row);

    @Override
    @Select("SELECT id, delivery_id AS deliveryId, event_kind AS eventKind, applied_at AS appliedAt, "
            + "del_flag AS delFlag, creator, modifier, gmt_create AS gmtCreate, gmt_modified AS gmtModified "
            + "FROM cs_support_receipt WHERE delivery_id = #{deliveryId} AND del_flag = #{delFlag}")
    CsSupportReceipt selectOneByExample(CsSupportReceiptExample example);
}
