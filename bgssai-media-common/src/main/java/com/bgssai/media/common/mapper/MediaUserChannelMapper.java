package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaUserChannel;
import com.bgssai.media.common.domain.MediaUserChannelExample;

import java.util.List;

public interface MediaUserChannelMapper {
    long countByExample(MediaUserChannelExample example);
    List<MediaUserChannel> selectByExample(MediaUserChannelExample example);
    MediaUserChannel selectByPrimaryKey(Long id);
    int insertSelective(MediaUserChannel row);
    int updateByPrimaryKeySelective(MediaUserChannel row);
}
