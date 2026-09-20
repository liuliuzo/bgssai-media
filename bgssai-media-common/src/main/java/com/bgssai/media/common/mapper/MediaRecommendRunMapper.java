package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaRecommendRun;
import com.bgssai.media.common.domain.MediaRecommendRunExample;

import java.util.List;

public interface MediaRecommendRunMapper {
    long countByExample(MediaRecommendRunExample example);
    List<MediaRecommendRun> selectByExample(MediaRecommendRunExample example);
    MediaRecommendRun selectByPrimaryKey(Long id);
    int insertSelective(MediaRecommendRun row);
    int updateByPrimaryKeySelective(MediaRecommendRun row);
}
