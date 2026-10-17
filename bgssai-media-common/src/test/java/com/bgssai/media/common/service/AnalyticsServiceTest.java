package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaCreatorVideo;
import com.bgssai.media.common.domain.MediaCreatorVideoExample;
import com.bgssai.media.common.mapper.MediaCreatorVideoMapper;
import com.bgssai.media.common.mapper.OpcExtMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    OpcExtMapper opc = mock(OpcExtMapper.class);
    MediaCreatorVideoMapper videos = mock(MediaCreatorVideoMapper.class);
    AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(opc, videos);
    }

    @Test
    void creatorAggregateSumsVideosAndDaily() {
        MediaCreatorVideo a = new MediaCreatorVideo();
        a.setId(1L);
        a.setUserId(9L);
        a.setChannelId(4L);
        a.setLikeCount(3);
        a.setCommentCount(2);
        MediaCreatorVideo b = new MediaCreatorVideo();
        b.setId(2L);
        b.setUserId(9L);
        b.setChannelId(4L);
        b.setLikeCount(7);
        b.setCommentCount(1);
        when(videos.selectByExample(any(MediaCreatorVideoExample.class))).thenReturn(List.of(a, b));

        Map<String, Object> day = new HashMap<>();
        day.put("eventDay", Date.valueOf("2026-10-01"));
        day.put("play_count", 10);
        day.put("watched_sec_sum", 100);
        day.put("completed_count", 4);
        when(opc.creatorDailyStats(eq(9L), any(Date.class), any(Date.class))).thenReturn(List.of(day));
        when(opc.channelSubscribeDaily(eq(4L), any(Date.class), any(Date.class))).thenReturn(List.of());

        Map<String, Object> out = service.creatorAggregateStats(9L, "2026-10-01", "2026-10-17");
        assertEquals(2, out.get("video_count"));
        assertEquals(10L, out.get("play_count"));
        assertEquals(100L, out.get("watched_sec_sum"));
        assertEquals(10L, out.get("like_count"));
        assertEquals(3L, out.get("comment_count"));
        assertEquals(4L, out.get("channel_id"));
        assertTrue((Boolean) out.get("has_data"));
        assertEquals(0.4, (Double) out.get("completion_rate"), 1e-9);
    }
}
