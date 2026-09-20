package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaCreatorVideo;
import com.bgssai.media.common.domain.MediaCreatorVideoExample;

import java.util.List;

public interface MediaCreatorVideoMapper {
    long countByExample(MediaCreatorVideoExample example);
    List<MediaCreatorVideo> selectByExample(MediaCreatorVideoExample example);
    MediaCreatorVideo selectByPrimaryKey(Long id);
    int insertSelective(MediaCreatorVideo row);
    int updateByPrimaryKeySelective(MediaCreatorVideo row);
}
