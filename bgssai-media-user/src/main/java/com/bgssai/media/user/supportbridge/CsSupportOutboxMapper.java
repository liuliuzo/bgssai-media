package com.bgssai.media.user.supportbridge;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CsSupportOutboxMapper extends CsSupportOutboxStore {

    @Override
    @Insert("INSERT INTO cs_support_outbox (event_kind, external_ref, request_path, payload_json, status, attempt_count, "
            + "last_error, next_attempt_at, sent_at, del_flag, creator, modifier) VALUES (#{eventKind}, #{externalRef}, "
            + "#{requestPath}, #{payloadJson}, #{status}, #{attemptCount}, #{lastError}, #{nextAttemptAt}, #{sentAt}, "
            + "#{delFlag}, #{creator}, #{modifier})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CsSupportOutbox row);

    @Override
    @Select("SELECT id, event_kind AS eventKind, external_ref AS externalRef, request_path AS requestPath, "
            + "payload_json AS payloadJson, status, attempt_count AS attemptCount, last_error AS lastError, "
            + "next_attempt_at AS nextAttemptAt, sent_at AS sentAt, del_flag AS delFlag, creator, modifier, "
            + "gmt_create AS gmtCreate, gmt_modified AS gmtModified FROM cs_support_outbox "
            + "WHERE status = #{status} AND del_flag = #{delFlag} "
            + "AND (next_attempt_at IS NULL OR next_attempt_at <= #{dueAt}) ORDER BY id ASC LIMIT 20")
    List<CsSupportOutbox> selectByExample(CsSupportOutboxExample example);

    @Override
    @Update("<script>UPDATE cs_support_outbox <set>"
            + "<if test='record.status != null'>status = #{record.status},</if>"
            + "<if test='record.sentAt != null'>sent_at = #{record.sentAt},</if>"
            + "<if test='record.attemptCount != null'>attempt_count = #{record.attemptCount},</if>"
            + "<if test='record.lastError != null'>last_error = #{record.lastError},</if>"
            + "<if test='record.nextAttemptAt != null'>next_attempt_at = #{record.nextAttemptAt},</if>"
            + "<if test='record.modifier != null'>modifier = #{record.modifier},</if>"
            + "</set> WHERE id = #{example.id} AND status = #{example.status} AND del_flag = #{example.delFlag}</script>")
    int updateByExampleSelective(@Param("record") CsSupportOutbox record, @Param("example") CsSupportOutboxExample example);
}
