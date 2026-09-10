package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.McpToken;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface McpTokenMapper {
    int insertSelective(McpToken row);

    List<McpToken> selectActiveByUserId(@Param("userId") Long userId);

    McpToken selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    McpToken selectActiveByHash(@Param("tokenHash") String tokenHash);

    int revoke(@Param("id") Long id, @Param("userId") Long userId, @Param("now") Date now);

    int touchLastUsed(@Param("id") Long id, @Param("now") Date now);
}
