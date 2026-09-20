package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaRecommendItem;
import com.bgssai.media.common.domain.MediaRecommendItemExample;

import java.util.List;

public interface MediaRecommendItemMapper {
    long countByExample(MediaRecommendItemExample example);
    List<MediaRecommendItem> selectByExample(MediaRecommendItemExample example);
    int insertSelective(MediaRecommendItem row);
}
