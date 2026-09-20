package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaTranscodeRendition;
import com.bgssai.media.common.domain.MediaTranscodeRenditionExample;

import java.util.List;

public interface MediaTranscodeRenditionMapper {
    long countByExample(MediaTranscodeRenditionExample example);
    List<MediaTranscodeRendition> selectByExample(MediaTranscodeRenditionExample example);
    MediaTranscodeRendition selectByPrimaryKey(Long id);
    int insertSelective(MediaTranscodeRendition row);
    int deleteByExample(MediaTranscodeRenditionExample example);
}
