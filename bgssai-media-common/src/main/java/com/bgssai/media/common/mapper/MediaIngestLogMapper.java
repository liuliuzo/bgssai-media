package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaIngestLog;
import com.bgssai.media.common.domain.MediaIngestLogExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MediaIngestLogMapper {
    List<MediaIngestLog> selectByExample(MediaIngestLogExample example);
    MediaIngestLog selectByPrimaryKey(Long id);
    int insertSelective(MediaIngestLog row);
    MediaIngestLog selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
    MediaIngestLog selectByMediaId(@Param("mediaId") String mediaId);
    int updateByIdempotencyKey(MediaIngestLog row);
    List<MediaIngestLog> listRecentContract(@Param("limit") int limit, @Param("offset") int offset);
    long countContract();
}

