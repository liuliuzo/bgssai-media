package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaTranscodeJob;
import com.bgssai.media.common.domain.MediaTranscodeJobExample;

import java.util.List;

public interface MediaTranscodeJobMapper {
    long countByExample(MediaTranscodeJobExample example);
    List<MediaTranscodeJob> selectByExample(MediaTranscodeJobExample example);
    MediaTranscodeJob selectByPrimaryKey(Long id);
    int insertSelective(MediaTranscodeJob row);
    int updateByPrimaryKeySelective(MediaTranscodeJob row);
}
