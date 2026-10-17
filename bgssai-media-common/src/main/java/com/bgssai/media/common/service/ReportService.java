package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaCreatorVideo;
import com.bgssai.media.common.domain.MediaVideoComment;
import com.bgssai.media.common.mapper.MediaCreatorVideoMapper;
import com.bgssai.media.common.mapper.MediaVideoCommentMapper;
import com.bgssai.media.common.mapper.OpcExtMapper;
import com.bgssai.media.common.web.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Content report + admin handling with notices (PRODUCT-015).
 */
@Service
public class ReportService {

    private final OpcExtMapper opcExtMapper;
    private final MediaCreatorVideoMapper videoMapper;
    private final MediaVideoCommentMapper commentMapper;
    private final CommunityService communityService;

    public ReportService(OpcExtMapper opcExtMapper,
                         MediaCreatorVideoMapper videoMapper,
                         MediaVideoCommentMapper commentMapper,
                         CommunityService communityService) {
        this.opcExtMapper = opcExtMapper;
        this.videoMapper = videoMapper;
        this.commentMapper = commentMapper;
        this.communityService = communityService;
    }

    @Transactional
    public Map<String, Object> submit(Long reporterUserId, String targetType, Long targetId, String reason) {
        if (reporterUserId == null) {
            throw new BizException(401, "login required");
        }
        String type = targetType == null ? "" : targetType.trim().toUpperCase(Locale.ROOT);
        if (!"VIDEO".equals(type) && !"COMMENT".equals(type)) {
            throw new BizException(400, "target_type must be VIDEO or COMMENT");
        }
        if (targetId == null) {
            throw new BizException(400, "target_id required");
        }
        String text = reason == null ? "" : reason.trim();
        if (text.isEmpty()) {
            throw new BizException(400, "reason required");
        }
        if ("VIDEO".equals(type)) {
            communityService.getReadyVideo(targetId);
        } else {
            MediaVideoComment c = commentMapper.selectByPrimaryKey(targetId);
            if (c == null || c.getDeletedAt() != null) {
                throw new BizException(404, "comment not found");
            }
        }
        String mergeKey = reporterUserId + ":" + type + ":" + targetId;
        opcExtMapper.upsertReport(reporterUserId, type, targetId, text, mergeKey);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accepted", true);
        out.put("merged", true);
        out.put("target_type", type);
        out.put("target_id", targetId);
        return out;
    }

    public List<Map<String, Object>> listOpen(int limit) {
        List<Map<String, Object>> rows = opcExtMapper.listOpenReports(Math.min(Math.max(limit, 1), 200));
        for (Map<String, Object> row : rows) {
            snake(row);
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> resolve(Long reportId, String status, String note) {
        if (reportId == null) {
            throw new BizException(400, "report_id required");
        }
        String st = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (!"resolved".equals(st) && !"rejected".equals(st)) {
            throw new BizException(400, "status must be resolved or rejected");
        }
        Map<String, Object> report = opcExtMapper.getReport(reportId);
        if (report == null) {
            throw new BizException(404, "report not found");
        }
        if (!"open".equals(String.valueOf(report.get("status")))) {
            throw new BizException(409, "report already handled");
        }
        int n = opcExtMapper.resolveReport(reportId, st, note, 1);
        if (n != 1) {
            throw new BizException(409, "report already handled");
        }
        Long reporterId = asLong(report.get("reporterUserId"));
        String targetType = String.valueOf(report.get("targetType"));
        Long targetId = asLong(report.get("targetId"));
        String title = "举报处理结果";
        String body = "您的举报已处理为：" + st + (note == null || note.isBlank() ? "" : ("。" + note.trim()));
        opcExtMapper.insertNotice(reporterId, "report_result", title, body, targetType, targetId);

        Long authorId = resolveAuthor(targetType, targetId);
        if (authorId != null && !authorId.equals(reporterId)) {
            opcExtMapper.insertNotice(authorId, "report_result", "内容举报处理通知",
                    "您的内容相关举报已处理为：" + st, targetType, targetId);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", reportId);
        out.put("status", st);
        out.put("notify_sent", true);
        return out;
    }

    @Transactional
    public Map<String, Object> moderateVideo(Long videoId, String status) {
        if (videoId == null) {
            throw new BizException(400, "video_id required");
        }
        String st = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (!"approved".equals(st) && !"rejected".equals(st) && !"pending".equals(st)) {
            throw new BizException(400, "invalid moderation status");
        }
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
        if (video == null) {
            throw new BizException(404, "video not found");
        }
        opcExtMapper.setModeration(videoId, st);
        if (video.getUserId() != null) {
            opcExtMapper.insertNotice(video.getUserId(), "moderation", "作品审核结果",
                    "作品审核状态：" + st, "VIDEO", videoId);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("video_id", videoId);
        out.put("moderation_status", st);
        return out;
    }

    private Long resolveAuthor(String targetType, Long targetId) {
        if ("VIDEO".equalsIgnoreCase(targetType)) {
            MediaCreatorVideo v = videoMapper.selectByPrimaryKey(targetId);
            return v == null ? null : v.getUserId();
        }
        MediaVideoComment c = commentMapper.selectByPrimaryKey(targetId);
        if (c == null) {
            return null;
        }
        MediaCreatorVideo v = videoMapper.selectByPrimaryKey(c.getVideoId());
        return v == null ? null : v.getUserId();
    }

    private static Long asLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(String.valueOf(v));
    }

    private static void snake(Map<String, Object> row) {
        move(row, "reporterUserId", "reporter_user_id");
        move(row, "targetType", "target_type");
        move(row, "targetId", "target_id");
        move(row, "createdAt", "created_at");
        move(row, "handlerNote", "handler_note");
        move(row, "handledAt", "handled_at");
        move(row, "notifySent", "notify_sent");
    }

    private static void move(Map<String, Object> row, String from, String to) {
        if (row.containsKey(from)) {
            row.put(to, row.remove(from));
        }
    }
}
