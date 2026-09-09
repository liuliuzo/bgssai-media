package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.domain.MediaDramaExample;

import java.util.List;

public interface MediaDramaMapper {
    long countByExample(MediaDramaExample example);
    List<MediaDrama> selectByExample(MediaDramaExample example);
    MediaDrama selectByPrimaryKey(Long id);
    int insertSelective(MediaDrama row);
    int updateByPrimaryKeySelective(MediaDrama row);
    int deleteByPrimaryKey(Long id);
}
