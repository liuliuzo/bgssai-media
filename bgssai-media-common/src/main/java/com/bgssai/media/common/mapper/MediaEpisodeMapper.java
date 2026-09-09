package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaEpisode;
import com.bgssai.media.common.domain.MediaEpisodeExample;

import java.util.List;

public interface MediaEpisodeMapper {
    long countByExample(MediaEpisodeExample example);
    List<MediaEpisode> selectByExample(MediaEpisodeExample example);
    MediaEpisode selectByPrimaryKey(Long id);
    int insertSelective(MediaEpisode row);
    int updateByPrimaryKeySelective(MediaEpisode row);
    int deleteByPrimaryKey(Long id);
    int deleteByDramaId(Long dramaId);
}
