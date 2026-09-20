package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaVideoLike;
import com.bgssai.media.common.domain.MediaVideoLikeExample;

import java.util.List;

public interface MediaVideoLikeMapper {
    long countByExample(MediaVideoLikeExample example);
    List<MediaVideoLike> selectByExample(MediaVideoLikeExample example);
    int insertSelective(MediaVideoLike row);
    int deleteByExample(MediaVideoLikeExample example);
}
