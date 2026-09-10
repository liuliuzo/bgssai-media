package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.PlatformSmsConfig;

public interface PlatformSmsConfigMapper {
    PlatformSmsConfig selectByPrimaryKey(Long id);
}
