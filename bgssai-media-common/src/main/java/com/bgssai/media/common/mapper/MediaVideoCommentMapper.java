package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaVideoComment;
import com.bgssai.media.common.domain.MediaVideoCommentExample;

import java.util.List;

public interface MediaVideoCommentMapper {
    long countByExample(MediaVideoCommentExample example);
    List<MediaVideoComment> selectByExample(MediaVideoCommentExample example);
    MediaVideoComment selectByPrimaryKey(Long id);
    int insertSelective(MediaVideoComment row);
    int deleteByPrimaryKey(Long id);
}
