package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaChannelFollow;
import com.bgssai.media.common.domain.MediaChannelFollowExample;

import java.util.List;

public interface MediaChannelFollowMapper {
    long countByExample(MediaChannelFollowExample example);
    List<MediaChannelFollow> selectByExample(MediaChannelFollowExample example);
    int insertSelective(MediaChannelFollow row);
    int deleteByExample(MediaChannelFollowExample example);
}
