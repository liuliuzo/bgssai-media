package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaSupportSession;
import com.bgssai.media.common.domain.MediaSupportSessionExample;

import java.util.List;

public interface MediaSupportSessionMapper {
    long countByExample(MediaSupportSessionExample example);

    List<MediaSupportSession> selectByExample(MediaSupportSessionExample example);

    MediaSupportSession selectByPrimaryKey(Long id);

    int insertSelective(MediaSupportSession row);

    int updateByPrimaryKeySelective(MediaSupportSession row);
}
