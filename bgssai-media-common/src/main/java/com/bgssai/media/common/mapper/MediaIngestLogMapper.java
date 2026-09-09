package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaIngestLog;
import com.bgssai.media.common.domain.MediaIngestLogExample;

import java.util.List;

public interface MediaIngestLogMapper {
    List<MediaIngestLog> selectByExample(MediaIngestLogExample example);
    MediaIngestLog selectByPrimaryKey(Long id);
    int insertSelective(MediaIngestLog row);
}
