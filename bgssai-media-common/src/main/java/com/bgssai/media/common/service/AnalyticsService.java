package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaCreatorVideo;
import com.bgssai.media.common.domain.MediaCreatorVideoExample;
import com.bgssai.media.common.mapper.MediaCreatorVideoMapper;
import com.bgssai.media.common.mapper.OpcExtMapper;
import com.bgssai.media.common.web.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Creator analytics from raw view / subscribe events (PRODUCT-014).
 */
@Service
public class AnalyticsService {

    public static final String METRIC_NOTE =
            "口径：播放量=播放事件数；观看时长=watched_sec 合计；完播率=completed/播放量；"
                    + "订阅增减=当日 follow/unfollow 事件；互动=作品 like_count/comment_count 快照。按事件日聚合。";

    private final OpcExtMapper opcExtMapper;
    private final MediaCreatorVideoMapper videoMapper;

    public AnalyticsService(OpcExtMapper opcExtMapper, MediaCreatorVideoMapper videoMapper) {
        this.opcExtMapper = opcExtMapper;
        this.videoMapper = videoMapper;
    }

    @Transactional
    public Map<String, Object> recordView(Long userId, Long videoId, Integer watchedSec, Boolean completed) {
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
        if (video == null) {
            throw new BizException(404, "video not found");
        }
        int sec = watchedSec == null ? 0 : Math.max(0, watchedSec);
        int done = Boolean.TRUE.equals(completed) ? 1 : 0;
        opcExtMapper.insertViewEvent(userId, videoId, video.getChannelId(), sec, done, Date.valueOf(LocalDate.now()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("recorded", true);
        out.put("video_id", videoId);
        return out;
    }

    public Map<String, Object> videoStats(Long ownerUserId, Long videoId, String from, String to) {
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
        if (video == null) {
            throw new BizException(404, "video not found");
        }
        if (ownerUserId == null || !ownerUserId.equals(video.getUserId())) {
            throw new BizException(403, "forbidden");
        }
        LocalDate fromDay = parseDay(from, LocalDate.now().minusDays(29));
        LocalDate toDay = parseDay(to, LocalDate.now());
        if (toDay.isBefore(fromDay)) {
            throw new BizException(400, "invalid date range");
        }
        List<Map<String, Object>> daily = opcExtMapper.videoDailyStats(
                videoId, Date.valueOf(fromDay), Date.valueOf(toDay));
        List<Map<String, Object>> subs = opcExtMapper.channelSubscribeDaily(
                video.getChannelId(), Date.valueOf(fromDay), Date.valueOf(toDay));

        long plays = 0;
        long watched = 0;
        long completed = 0;
        for (Map<String, Object> d : daily) {
            plays += asLong(d.get("play_count"));
            watched += asLong(d.get("watched_sec_sum"));
            completed += asLong(d.get("completed_count"));
            snakeDay(d);
        }
        for (Map<String, Object> s : subs) {
            snakeDay(s);
        }
        Double completionRate = plays == 0 ? null : (completed * 1.0 / plays);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("video_id", videoId);
        out.put("from", fromDay.toString());
        out.put("to", toDay.toString());
        out.put("metric_note", METRIC_NOTE);
        out.put("updated_at", java.time.Instant.now().toString());
        out.put("play_count", plays);
        out.put("watched_sec_sum", watched);
        out.put("completion_rate", completionRate);
        out.put("like_count", video.getLikeCount() == null ? 0 : video.getLikeCount());
        out.put("comment_count", video.getCommentCount() == null ? 0 : video.getCommentCount());
        out.put("daily", daily);
        out.put("subscribe_daily", subs);
        out.put("has_data", !daily.isEmpty() || plays > 0);
        return out;
    }

    /**
     * Aggregate analytics across all of the creator's videos (and their channel subscribe events).
     */
    public Map<String, Object> creatorAggregateStats(Long ownerUserId, String from, String to) {
        if (ownerUserId == null) {
            throw new BizException(401, "login required");
        }
        LocalDate fromDay = parseDay(from, LocalDate.now().minusDays(29));
        LocalDate toDay = parseDay(to, LocalDate.now());
        if (toDay.isBefore(fromDay)) {
            throw new BizException(400, "invalid date range");
        }

        MediaCreatorVideoExample ex = new MediaCreatorVideoExample();
        ex.createCriteria().andUserIdEqualTo(ownerUserId);
        List<MediaCreatorVideo> videos = videoMapper.selectByExample(ex);
        long likeSum = 0;
        long commentSum = 0;
        Long channelId = null;
        for (MediaCreatorVideo video : videos) {
            likeSum += video.getLikeCount() == null ? 0 : video.getLikeCount();
            commentSum += video.getCommentCount() == null ? 0 : video.getCommentCount();
            if (channelId == null && video.getChannelId() != null) {
                channelId = video.getChannelId();
            }
        }

        List<Map<String, Object>> daily = opcExtMapper.creatorDailyStats(
                ownerUserId, Date.valueOf(fromDay), Date.valueOf(toDay));
        List<Map<String, Object>> subs = channelId == null
                ? List.of()
                : opcExtMapper.channelSubscribeDaily(channelId, Date.valueOf(fromDay), Date.valueOf(toDay));

        long plays = 0;
        long watched = 0;
        long completed = 0;
        for (Map<String, Object> d : daily) {
            plays += asLong(d.get("play_count"));
            watched += asLong(d.get("watched_sec_sum"));
            completed += asLong(d.get("completed_count"));
            snakeDay(d);
        }
        for (Map<String, Object> s : subs) {
            snakeDay(s);
        }
        Double completionRate = plays == 0 ? null : (completed * 1.0 / plays);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", fromDay.toString());
        out.put("to", toDay.toString());
        out.put("metric_note", METRIC_NOTE);
        out.put("updated_at", java.time.Instant.now().toString());
        out.put("video_count", videos.size());
        out.put("play_count", plays);
        out.put("watched_sec_sum", watched);
        out.put("completion_rate", completionRate);
        out.put("like_count", likeSum);
        out.put("comment_count", commentSum);
        out.put("channel_id", channelId);
        out.put("daily", daily);
        out.put("subscribe_daily", subs);
        out.put("has_data", !daily.isEmpty() || plays > 0 || !videos.isEmpty());
        return out;
    }

    private static LocalDate parseDay(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new BizException(400, "invalid date: " + raw);
        }
    }

    private static long asLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }

    private static void snakeDay(Map<String, Object> row) {
        if (row.containsKey("eventDay")) {
            row.put("event_day", String.valueOf(row.remove("eventDay")));
        }
    }
}
