package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaCreatorVideo;
import com.bgssai.media.common.domain.MediaCreatorVideoExample;
import com.bgssai.media.common.domain.MediaTranscodeJob;
import com.bgssai.media.common.domain.MediaTranscodeJobExample;
import com.bgssai.media.common.domain.MediaTranscodeRendition;
import com.bgssai.media.common.domain.MediaTranscodeRenditionExample;
import com.bgssai.media.common.domain.MediaUserChannel;
import com.bgssai.media.common.mapper.MediaCreatorVideoMapper;
import com.bgssai.media.common.mapper.MediaTranscodeJobMapper;
import com.bgssai.media.common.mapper.MediaTranscodeRenditionMapper;
import com.bgssai.media.common.transcode.FfmpegGateway;
import com.bgssai.media.common.web.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Authenticated creator upload, local store, async ffmpeg transcode (AUD20-020).
 * Job statuses: QUEUED / RUNNING / READY / FAILED. Missing ffmpeg fails the job.
 */
@Service
public class CreatorVideoService {

    public static final String STATUS_QUEUED = "QUEUED";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_FAILED = "FAILED";
    public static final String REASON_FFMPEG_MISSING = "ffmpeg not found";

    static final int[] RENDITION_HEIGHTS = {360, 720, 1080};
    private static final Set<String> PUBLIC_ASSETS = Set.of(
            "360p.mp4", "720p.mp4", "1080p.mp4", "cover.jpg", "cover.png");
    private static final long MAX_BYTES = 512L * 1024 * 1024;
    private static final int MAX_RETRY = 8;
    private static final Logger log = LoggerFactory.getLogger(CreatorVideoService.class);

    private final MediaCreatorVideoMapper videoMapper;
    private final MediaTranscodeJobMapper jobMapper;
    private final MediaTranscodeRenditionMapper renditionMapper;
    private final CommunityService communityService;
    private final FfmpegGateway ffmpegGateway;
    private final Path storageDir;
    private final String publicBase;
    private final TaskExecutor transcodeExecutor;

    public CreatorVideoService(MediaCreatorVideoMapper videoMapper,
                               MediaTranscodeJobMapper jobMapper,
                               MediaTranscodeRenditionMapper renditionMapper,
                               CommunityService communityService,
                               FfmpegGateway ffmpegGateway,
                               @Value("${bgssai.media.creator.storage-dir:uploads/creator}") String storageDir,
                               @Value("${bgssai.media.creator.public-base:/api/creator/media}") String publicBase,
                               @Autowired(required = false) @Qualifier("mediaTranscodeExecutor") TaskExecutor transcodeExecutor) {
        this.videoMapper = videoMapper;
        this.jobMapper = jobMapper;
        this.renditionMapper = renditionMapper;
        this.communityService = communityService;
        this.ffmpegGateway = ffmpegGateway;
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
        this.publicBase = publicBase.endsWith("/") ? publicBase.substring(0, publicBase.length() - 1) : publicBase;
        this.transcodeExecutor = transcodeExecutor;
    }

    @Transactional
    public Map<String, Object> upload(Long userId,
                                      String title,
                                      String description,
                                      String originalFilename,
                                      String contentType,
                                      byte[] fileBytes,
                                      byte[] coverBytes) {
        requireUser(userId);
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BizException(400, "file required");
        }
        if (fileBytes.length > MAX_BYTES) {
            throw new BizException(400, "file too large");
        }
        String filename = safeName(originalFilename);
        String resolvedTitle = title == null || title.isBlank() ? filename : title.trim();
        if (resolvedTitle.length() > 256) {
            throw new BizException(400, "title too long");
        }
        MediaUserChannel channel = communityService.ensureMine(userId);

        Date now = new Date();
        MediaCreatorVideo video = new MediaCreatorVideo();
        video.setUserId(userId);
        video.setChannelId(channel.getId());
        video.setTitle(resolvedTitle);
        video.setDescription(trimToNull(description));
        video.setStatus(STATUS_QUEUED);
        video.setOriginalFilename(filename);
        video.setContentType(contentType);
        video.setSizeBytes((long) fileBytes.length);
        video.setLikeCount(0);
        video.setCommentCount(0);
        video.setCreatedAt(now);
        videoMapper.insertSelective(video);

        try {
            Path dir = videoDir(video.getId());
            Files.createDirectories(dir);
            Path original = dir.resolve("original.mp4");
            Files.write(original, fileBytes);
            video.setOriginalPath(original.toString());
            if (coverBytes != null && coverBytes.length > 0) {
                Path cover = dir.resolve("cover.jpg");
                Files.write(cover, coverBytes);
                video.setCoverPath(cover.toString());
                video.setCoverUrl(playUrl(video.getId(), "cover.jpg"));
            }
            videoMapper.updateByPrimaryKeySelective(video);
        } catch (Exception ex) {
            failVideo(video, "store file failed");
            MediaTranscodeJob failed = newJob(video, userId);
            failed.setStatus(STATUS_FAILED);
            failed.setFailureReason(truncate("store file failed"));
            failed.setFinishedAt(new Date());
            jobMapper.insertSelective(failed);
            throw new BizException(500, "store file failed");
        }

        MediaTranscodeJob job = newJob(video, userId);
        jobMapper.insertSelective(job);
        dispatch(job.getId());
        return detail(userId, video.getId());
    }

    public Map<String, Object> detail(Long userId, Long videoId) {
        requireUser(userId);
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
        if (video == null) {
            throw new BizException(404, "video not found");
        }
        boolean owner = userId.equals(video.getUserId());
        if (!owner && !STATUS_READY.equals(video.getStatus())) {
            throw new BizException(404, "video not found");
        }
        return toView(video, owner);
    }

    public List<Map<String, Object>> listMine(Long userId) {
        requireUser(userId);
        MediaCreatorVideoExample ex = new MediaCreatorVideoExample();
        ex.createCriteria().andUserIdEqualTo(userId);
        ex.setOrderByClause("id desc");
        List<Map<String, Object>> out = new ArrayList<>();
        for (MediaCreatorVideo video : videoMapper.selectByExample(ex)) {
            out.add(toView(video, true));
        }
        return out;
    }

    public Map<String, Object> getJob(Long userId, Long jobId) {
        requireUser(userId);
        MediaTranscodeJob job = jobMapper.selectByPrimaryKey(jobId);
        if (job == null) {
            throw new BizException(404, "job not found");
        }
        if (!userId.equals(job.getUserId())) {
            throw new BizException(403, "forbidden");
        }
        return jobView(job, renditionsOf(job.getVideoId()));
    }

    @Transactional
    public Map<String, Object> retry(Long userId, Long jobId) {
        requireUser(userId);
        MediaTranscodeJob job = jobMapper.selectByPrimaryKey(jobId);
        if (job == null) {
            throw new BizException(404, "job not found");
        }
        if (!userId.equals(job.getUserId())) {
            throw new BizException(403, "forbidden");
        }
        if (!STATUS_FAILED.equals(job.getStatus())) {
            throw new BizException(400, "only FAILED jobs can retry");
        }
        int retries = job.getRetryCount() == null ? 0 : job.getRetryCount();
        if (retries >= MAX_RETRY) {
            throw new BizException(400, "retry limit reached");
        }
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(job.getVideoId());
        if (video == null) {
            throw new BizException(404, "video not found");
        }
        MediaTranscodeRenditionExample rex = new MediaTranscodeRenditionExample();
        rex.createCriteria().andJobIdEqualTo(job.getId());
        renditionMapper.deleteByExample(rex);

        job.setStatus(STATUS_QUEUED);
        job.setFailureReason("");
        job.setRetryCount(retries + 1);
        job.setStartedAt(null);
        job.setFinishedAt(null);
        jobMapper.updateByPrimaryKeySelective(job);

        video.setStatus(STATUS_QUEUED);
        videoMapper.updateByPrimaryKeySelective(video);
        dispatch(job.getId());
        return getJob(userId, jobId);
    }

    public Map<String, Object> playback(Long userId, Long videoId) {
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
        if (video == null) {
            throw new BizException(404, "video not found");
        }
        boolean owner = userId != null && userId.equals(video.getUserId());
        if (!STATUS_READY.equals(video.getStatus()) && !owner) {
            throw new BizException(404, "video not found");
        }
        if (!STATUS_READY.equals(video.getStatus())) {
            throw new BizException(409, "video not ready");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("video_id", video.getId());
        out.put("cover_url", video.getCoverUrl());
        out.put("renditions", renditionMaps(renditionsOf(video.getId())));
        return out;
    }

    public Path publicAsset(Long videoId, String asset) {
        if (videoId == null || asset == null || !PUBLIC_ASSETS.contains(asset)) {
            throw new BizException(404, "asset not found");
        }
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
        if (video == null || !STATUS_READY.equals(video.getStatus())) {
            throw new BizException(404, "asset not found");
        }
        Path dir = videoDir(videoId);
        Path resolved = dir.resolve(asset).normalize();
        if (!resolved.startsWith(dir)) {
            throw new BizException(404, "asset not found");
        }
        if (!Files.isRegularFile(resolved)) {
            throw new BizException(404, "asset not found");
        }
        return resolved;
    }

    @Scheduled(fixedDelayString = "${bgssai.media.transcode.poll-ms:2000}")
    public void drainQueuedJobs() {
        MediaTranscodeJobExample ex = new MediaTranscodeJobExample();
        ex.createCriteria().andStatusEqualTo(STATUS_QUEUED);
        ex.setOrderByClause("id asc");
        for (MediaTranscodeJob job : jobMapper.selectByExample(ex)) {
            processJob(job.getId());
        }
    }

    public void processJob(Long jobId) {
        MediaTranscodeJob job = jobMapper.selectByPrimaryKey(jobId);
        if (job == null || !STATUS_QUEUED.equals(job.getStatus())) {
            return;
        }
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(job.getVideoId());
        if (video == null) {
            job.setStatus(STATUS_FAILED);
            job.setFailureReason("video missing");
            job.setFinishedAt(new Date());
            jobMapper.updateByPrimaryKeySelective(job);
            return;
        }

        job.setStatus(STATUS_RUNNING);
        job.setStartedAt(new Date());
        job.setFailureReason("");
        jobMapper.updateByPrimaryKeySelective(job);
        video.setStatus(STATUS_RUNNING);
        videoMapper.updateByPrimaryKeySelective(video);

        if (!ffmpegGateway.available()) {
            failJob(job, video, REASON_FFMPEG_MISSING);
            return;
        }

        try {
            Path original = Path.of(video.getOriginalPath());
            if (!Files.isRegularFile(original)) {
                failJob(job, video, "original file missing");
                return;
            }
            Path dir = videoDir(video.getId());
            Files.createDirectories(dir);

            MediaTranscodeRenditionExample rex = new MediaTranscodeRenditionExample();
            rex.createCriteria().andVideoIdEqualTo(video.getId());
            renditionMapper.deleteByExample(rex);

            for (int height : RENDITION_HEIGHTS) {
                String label = height + "p";
                String asset = label + ".mp4";
                Path dest = dir.resolve(asset);
                ffmpegGateway.transcode(original, dest, height);
                MediaTranscodeRendition row = new MediaTranscodeRendition();
                row.setJobId(job.getId());
                row.setVideoId(video.getId());
                row.setBitrateLabel(label);
                row.setHeight(height);
                row.setStoragePath(dest.toString());
                row.setPlayUrl(playUrl(video.getId(), asset));
                row.setCreatedAt(new Date());
                renditionMapper.insertSelective(row);
            }

            if (video.getCoverPath() == null || video.getCoverPath().isBlank()
                    || !Files.isRegularFile(Path.of(video.getCoverPath()))) {
                Path cover = dir.resolve("cover.jpg");
                ffmpegGateway.extractCover(original, cover);
                video.setCoverPath(cover.toString());
                video.setCoverUrl(playUrl(video.getId(), "cover.jpg"));
            }

            Date done = new Date();
            video.setStatus(STATUS_READY);
            video.setUpdatedAt(done);
            videoMapper.updateByPrimaryKeySelective(video);
            job.setStatus(STATUS_READY);
            job.setFailureReason("");
            job.setFinishedAt(done);
            jobMapper.updateByPrimaryKeySelective(job);
        } catch (Exception ex) {
            log.warn("transcode job {} failed: {}", jobId, ex.toString());
            failJob(job, video, "transcode failed: " + ex.getMessage());
        }
    }

    private void dispatch(Long jobId) {
        if (transcodeExecutor == null) {
            return;
        }
        transcodeExecutor.execute(() -> {
            try {
                processJob(jobId);
            } catch (RuntimeException ex) {
                log.warn("transcode dispatch {}: {}", jobId, ex.toString());
            }
        });
    }

    private void failJob(MediaTranscodeJob job, MediaCreatorVideo video, String reason) {
        String truncated = truncate(reason);
        Date now = new Date();
        video.setStatus(STATUS_FAILED);
        videoMapper.updateByPrimaryKeySelective(video);
        job.setStatus(STATUS_FAILED);
        job.setFailureReason(truncated);
        job.setFinishedAt(now);
        jobMapper.updateByPrimaryKeySelective(job);
    }

    private void failVideo(MediaCreatorVideo video, String reason) {
        video.setStatus(STATUS_FAILED);
        videoMapper.updateByPrimaryKeySelective(video);
    }

    private MediaTranscodeJob newJob(MediaCreatorVideo video, Long userId) {
        MediaTranscodeJob job = new MediaTranscodeJob();
        job.setVideoId(video.getId());
        job.setUserId(userId);
        job.setStatus(STATUS_QUEUED);
        job.setRetryCount(0);
        job.setCreatedAt(new Date());
        return job;
    }

    private Map<String, Object> toView(MediaCreatorVideo video, boolean owner) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", video.getId());
        out.put("user_id", video.getUserId());
        out.put("channel_id", video.getChannelId());
        out.put("title", video.getTitle());
        out.put("description", video.getDescription());
        out.put("status", video.getStatus());
        out.put("cover_url", video.getCoverUrl());
        out.put("like_count", video.getLikeCount() == null ? 0 : video.getLikeCount());
        out.put("comment_count", video.getCommentCount() == null ? 0 : video.getCommentCount());
        out.put("created_at", video.getCreatedAt());
        if (owner) {
            MediaTranscodeJob job = latestJob(video.getId());
            if (job != null) {
                out.put("job", jobView(job, STATUS_READY.equals(video.getStatus())
                        ? renditionsOf(video.getId()) : List.of()));
            }
            if (STATUS_READY.equals(video.getStatus())) {
                out.put("renditions", renditionMaps(renditionsOf(video.getId())));
            }
        } else if (STATUS_READY.equals(video.getStatus())) {
            out.put("renditions", renditionMaps(renditionsOf(video.getId())));
        }
        return out;
    }

    private Map<String, Object> jobView(MediaTranscodeJob job, List<MediaTranscodeRendition> renditions) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", job.getId());
        out.put("video_id", job.getVideoId());
        out.put("status", job.getStatus());
        out.put("failure_reason", blankToNull(job.getFailureReason()));
        out.put("retry_count", job.getRetryCount() == null ? 0 : job.getRetryCount());
        out.put("started_at", job.getStartedAt());
        out.put("finished_at", job.getFinishedAt());
        if (STATUS_READY.equals(job.getStatus())) {
            out.put("renditions", renditionMaps(renditions));
        }
        return out;
    }

    private List<Map<String, Object>> renditionMaps(List<MediaTranscodeRendition> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (MediaTranscodeRendition row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bitrate_label", row.getBitrateLabel());
            m.put("height", row.getHeight());
            m.put("play_url", row.getPlayUrl());
            out.add(m);
        }
        return out;
    }

    private MediaTranscodeJob latestJob(Long videoId) {
        MediaTranscodeJobExample ex = new MediaTranscodeJobExample();
        ex.createCriteria().andVideoIdEqualTo(videoId);
        ex.setOrderByClause("id desc");
        List<MediaTranscodeJob> list = jobMapper.selectByExample(ex);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<MediaTranscodeRendition> renditionsOf(Long videoId) {
        MediaTranscodeRenditionExample ex = new MediaTranscodeRenditionExample();
        ex.createCriteria().andVideoIdEqualTo(videoId);
        ex.setOrderByClause("height asc");
        return renditionMapper.selectByExample(ex);
    }

    private Path videoDir(Long videoId) {
        return storageDir.resolve(String.valueOf(videoId));
    }

    private String playUrl(Long videoId, String asset) {
        return publicBase + "/" + videoId + "/" + asset;
    }

    private static String safeName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "upload.mp4";
        }
        String name = Path.of(originalFilename).getFileName().toString();
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isBlank() ? "upload.mp4" : name;
    }

    private static String trimToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String truncate(String reason) {
        if (reason == null || reason.isBlank()) {
            return "failed";
        }
        return reason.length() > 1024 ? reason.substring(0, 1024) : reason;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    private static void requireUser(Long userId) {
        if (userId == null) {
            throw new BizException(401, "login required");
        }
    }

    public static String contentTypeForAsset(String asset) {
        String lower = asset == null ? "" : asset.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        return "video/mp4";
    }
}
