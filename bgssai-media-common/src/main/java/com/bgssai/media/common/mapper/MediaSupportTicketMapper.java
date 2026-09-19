package com.bgssai.media.common.mapper;

import com.bgssai.media.common.domain.MediaSupportTicket;
import com.bgssai.media.common.domain.MediaSupportTicketExample;

import java.util.List;

public interface MediaSupportTicketMapper {
    long countByExample(MediaSupportTicketExample example);

    List<MediaSupportTicket> selectByExample(MediaSupportTicketExample example);

    MediaSupportTicket selectByPrimaryKey(Long id);

    int insertSelective(MediaSupportTicket row);

    int updateByPrimaryKeySelective(MediaSupportTicket row);
}
