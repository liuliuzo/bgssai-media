package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaSupportMessage;
import com.bgssai.media.common.domain.MediaSupportMessageExample;

import java.util.List;

public interface MediaSupportMessageMapper {
    List<MediaSupportMessage> selectByExample(MediaSupportMessageExample example);

    MediaSupportMessage selectByPrimaryKey(Long id);

    int insertSelective(MediaSupportMessage row);
}
