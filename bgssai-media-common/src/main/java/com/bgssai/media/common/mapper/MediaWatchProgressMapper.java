package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaWatchProgress;
import com.bgssai.media.common.domain.MediaWatchProgressExample;

import java.util.List;

public interface MediaWatchProgressMapper {
    List<MediaWatchProgress> selectByExample(MediaWatchProgressExample example);
    int insertSelective(MediaWatchProgress row);
    int updateByPrimaryKeySelective(MediaWatchProgress row);
}
