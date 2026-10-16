package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaChannelFollow;
import com.bgssai.media.common.domain.MediaChannelFollowExample;
import com.bgssai.media.common.domain.MediaCreatorVideo;
import com.bgssai.media.common.domain.MediaCreatorVideoExample;
import com.bgssai.media.common.domain.MediaDrama;
import com.bgssai.media.common.domain.MediaDramaExample;
import com.bgssai.media.common.domain.MediaEpisode;
import com.bgssai.media.common.domain.MediaEpisodeExample;
import com.bgssai.media.common.domain.MediaIngestLog;
import com.bgssai.media.common.domain.MediaRecommendItem;
import com.bgssai.media.common.domain.MediaRecommendRun;
import com.bgssai.media.common.domain.MediaUserChannel;
import com.bgssai.media.common.domain.MediaUserChannelExample;
import com.bgssai.media.common.domain.MediaVideoComment;
import com.bgssai.media.common.domain.MediaVideoCommentExample;
import com.bgssai.media.common.domain.MediaVideoLike;
import com.bgssai.media.common.domain.MediaVideoLikeExample;
import com.bgssai.media.common.ingest.IngestStatus;
import com.bgssai.media.common.mapper.MediaChannelFollowMapper;
import com.bgssai.media.common.mapper.MediaCreatorVideoMapper;
import com.bgssai.media.common.mapper.MediaDramaMapper;
import com.bgssai.media.common.mapper.MediaEpisodeMapper;
import com.bgssai.media.common.mapper.MediaIngestLogMapper;
import com.bgssai.media.common.mapper.MediaRecommendItemMapper;
import com.bgssai.media.common.mapper.MediaRecommendRunMapper;
import com.bgssai.media.common.mapper.MediaUserChannelMapper;
import com.bgssai.media.common.mapper.MediaVideoCommentMapper;
import com.bgssai.media.common.mapper.MediaVideoLikeMapper;
import com.bgssai.media.common.source.SourceRefs;
import com.bgssai.media.common.web.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Channel / follow / comment / like / rule-based recommend persistence (AUD20-021).
 * Recommend is two-stage recall + filter + sort. It is not machine learning.
 *
 * <p>Serves both ready creator uploads ({@code media_creator_video}) and published,
 * approved Short/Long drama episodes ({@code media_drama} / {@code media_episode}) via
 * deterministic unified content ID mapping ({@link #EPISODE_VIDEO_ID_OFFSET} + episodeId,
 * or {@code m_ep_{episodeId}}) without duplicating library tables.
 */
@Service
public class CommunityService {

    public static final String ALGO_TWO_STAGE = "two_stage_recall_filter";
    public static final String RECALL_FOLLOWING = "FOLLOWING";
    public static final String RECALL_RECENT = "RECENT";
    public static final String STAGE_CANDIDATE = "CANDIDATE";
    public static final String STAGE_KEPT = "KEPT";
    public static final String FILTER_DUPLICATE = "DUPLICATE";
    public static final String FILTER_NOT_READY = "NOT_READY";

    public static final long EPISODE_VIDEO_ID_OFFSET = 1_000_000_000L;
    public static final String SOURCE_TYPE_CREATOR = "CREATOR_VIDEO";
    public static final String SOURCE_TYPE_SHORT_DRAMA = "SHORT_DRAMA";
    public static final String SOURCE_TYPE_LONG_DRAMA = "LONG_DRAMA";

    private static final int RECENT_LIMIT = 50;
    private static final int MAX_COMMENT = 2000;

    private final MediaUserChannelMapper channelMapper;
    private final MediaChannelFollowMapper followMapper;
    private final MediaCreatorVideoMapper videoMapper;
    private final MediaVideoCommentMapper commentMapper;
    private final MediaVideoLikeMapper likeMapper;
    private final MediaRecommendRunMapper runMapper;
    private final MediaRecommendItemMapper itemMapper;
    private final MediaDramaMapper dramaMapper;
    private final MediaEpisodeMapper episodeMapper;
    private final MediaIngestLogMapper ingestLogMapper;

    public CommunityService(MediaUserChannelMapper channelMapper,
                            MediaChannelFollowMapper followMapper,
                            MediaCreatorVideoMapper videoMapper,
                            MediaVideoCommentMapper commentMapper,
                            MediaVideoLikeMapper likeMapper,
                            MediaRecommendRunMapper runMapper,
                            MediaRecommendItemMapper itemMapper) {
        this(channelMapper, followMapper, videoMapper, commentMapper, likeMapper,
                runMapper, itemMapper, null, null, null);
    }

    @Autowired
    public CommunityService(MediaUserChannelMapper channelMapper,
                            MediaChannelFollowMapper followMapper,
                            MediaCreatorVideoMapper videoMapper,
                            MediaVideoCommentMapper commentMapper,
                            MediaVideoLikeMapper likeMapper,
                            MediaRecommendRunMapper runMapper,
                            MediaRecommendItemMapper itemMapper,
                            @Autowired(required = false) MediaDramaMapper dramaMapper,
                            @Autowired(required = false) MediaEpisodeMapper episodeMapper,
                            @Autowired(required = false) MediaIngestLogMapper ingestLogMapper) {
        this.channelMapper = channelMapper;
        this.followMapper = followMapper;
        this.videoMapper = videoMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.runMapper = runMapper;
        this.itemMapper = itemMapper;
        this.dramaMapper = dramaMapper;
        this.episodeMapper = episodeMapper;
        this.ingestLogMapper = ingestLogMapper;
    }

    public static Long toEpisodeVideoId(Long episodeId) {
        return episodeId == null ? null : EPISODE_VIDEO_ID_OFFSET + episodeId;
    }

    public static boolean isEpisodeVideoId(Long videoId) {
        return videoId != null && videoId >= EPISODE_VIDEO_ID_OFFSET;
    }

    public static Long toEpisodeId(Long videoId) {
        return isEpisodeVideoId(videoId) ? (videoId - EPISODE_VIDEO_ID_OFFSET) : null;
    }

    public static String toContentId(Long videoId) {
        if (videoId == null) {
            return null;
        }
        return isEpisodeVideoId(videoId) ? ("m_ep_" + toEpisodeId(videoId)) : String.valueOf(videoId);
    }

    public Long resolveContentVideoId(Long videoId, String contentId) {
        if (videoId != null && videoId > 0) {
            if (isEpisodeVideoId(videoId)) {
                return videoId;
            }
            if (videoMapper.selectByPrimaryKey(videoId) != null) {
                return videoId;
            }
            if (episodeMapper != null && episodeMapper.selectByPrimaryKey(videoId) != null) {
                return toEpisodeVideoId(videoId);
            }
            return videoId;
        }
        if (contentId != null && !contentId.isBlank()) {
            String raw = contentId.trim();
            try {
                if (raw.startsWith("m_ep_")) {
                    return toEpisodeVideoId(Long.parseLong(raw.substring(5)));
                }
                if (raw.startsWith("ep_")) {
                    return toEpisodeVideoId(Long.parseLong(raw.substring(3)));
                }
                if (raw.startsWith("video_")) {
                    return Long.parseLong(raw.substring(6));
                }
                long parsed = Long.parseLong(raw);
                return resolveContentVideoId(parsed, null);
            } catch (NumberFormatException ex) {
                throw new BizException(400, "invalid content id: " + raw);
            }
        }
        throw new BizException(400, "video_id or media_id required");
    }

    @Transactional
    public MediaUserChannel ensureMine(Long userId) {
        requireUser(userId);
        MediaUserChannel existing = findByUser(userId);
        if (existing != null) {
            return existing;
        }
        MediaUserChannel row = new MediaUserChannel();
        row.setUserId(userId);
        row.setHandle("u" + userId);
        row.setDisplayName("channel-" + userId);
        row.setCreatedAt(new Date());
        channelMapper.insertSelective(row);
        return row;
    }

    @Transactional
    public MediaUserChannel updateMine(Long userId, String displayName, String bio, String handle) {
        MediaUserChannel row = ensureMine(userId);
        if (displayName != null) {
            String name = displayName.trim();
            if (name.isEmpty()) {
                throw new BizException(400, "display_name required");
            }
            if (name.length() > 128) {
                throw new BizException(400, "display_name too long");
            }
            row.setDisplayName(name);
        }
        if (bio != null) {
            String b = bio.trim();
            if (b.length() > 512) {
                throw new BizException(400, "bio too long");
            }
            row.setBio(b.isEmpty() ? null : b);
        }
        if (handle != null) {
            String h = handle.trim();
            if (!h.matches("[A-Za-z0-9_]{3,64}")) {
                throw new BizException(400, "invalid handle");
            }
            MediaUserChannelExample ex = new MediaUserChannelExample();
            ex.createCriteria().andHandleEqualTo(h);
            List<MediaUserChannel> clash = channelMapper.selectByExample(ex);
            for (MediaUserChannel other : clash) {
                if (!row.getId().equals(other.getId())) {
                    throw new BizException(409, "handle taken");
                }
            }
            row.setHandle(h);
        }
        channelMapper.updateByPrimaryKeySelective(row);
        return row;
    }

    public MediaUserChannel getPublic(Long channelId) {
        if (channelId == null) {
            throw new BizException(400, "channel_id required");
        }
        MediaUserChannel row = channelMapper.selectByPrimaryKey(channelId);
        if (row == null) {
            throw new BizException(404, "channel not found");
        }
        return row;
    }

    @Transactional
    public Map<String, Object> follow(Long userId, Long channelId) {
        requireUser(userId);
        MediaUserChannel channel = getPublic(channelId);
        if (userId.equals(channel.getUserId())) {
            throw new BizException(400, "cannot follow own channel");
        }
        MediaChannelFollowExample ex = new MediaChannelFollowExample();
        ex.createCriteria().andFollowerUserIdEqualTo(userId).andChannelIdEqualTo(channelId);
        if (followMapper.countByExample(ex) == 0) {
            MediaChannelFollow row = new MediaChannelFollow();
            row.setFollowerUserId(userId);
            row.setChannelId(channelId);
            row.setCreatedAt(new Date());
            followMapper.insertSelective(row);
        }
        return followView(userId, channelId, true);
    }

    @Transactional
    public Map<String, Object> unfollow(Long userId, Long channelId) {
        requireUser(userId);
        getPublic(channelId);
        MediaChannelFollowExample ex = new MediaChannelFollowExample();
        ex.createCriteria().andFollowerUserIdEqualTo(userId).andChannelIdEqualTo(channelId);
        followMapper.deleteByExample(ex);
        return followView(userId, channelId, false);
    }

    public List<MediaUserChannel> listFollowing(Long userId) {
        requireUser(userId);
        MediaChannelFollowExample ex = new MediaChannelFollowExample();
        ex.createCriteria().andFollowerUserIdEqualTo(userId);
        List<MediaChannelFollow> follows = followMapper.selectByExample(ex);
        List<MediaUserChannel> out = new ArrayList<>();
        for (MediaChannelFollow f : follows) {
            MediaUserChannel ch = channelMapper.selectByPrimaryKey(f.getChannelId());
            if (ch != null) {
                out.add(ch);
            }
        }
        return out;
    }

    public List<MediaCreatorVideo> listReadyVideos() {
        MediaCreatorVideoExample ex = new MediaCreatorVideoExample();
        ex.createCriteria().andStatusEqualTo(CreatorVideoService.STATUS_READY);
        ex.setOrderByClause("created_at desc");
        List<MediaCreatorVideo> combined = new ArrayList<>(videoMapper.selectByExample(ex));
        combined.addAll(listReadyEpisodeVideos());
        combined.sort((a, b) -> {
            long at = a.getCreatedAt() == null ? 0L : a.getCreatedAt().getTime();
            long bt = b.getCreatedAt() == null ? 0L : b.getCreatedAt().getTime();
            int cmp = Long.compare(bt, at);
            if (cmp != 0) {
                return cmp;
            }
            long aid = a.getId() == null ? 0L : a.getId();
            long bid = b.getId() == null ? 0L : b.getId();
            return Long.compare(bid, aid);
        });
        return combined;
    }

    public MediaCreatorVideo getReadyVideo(Long videoId) {
        if (videoId == null) {
            throw new BizException(404, "video not found");
        }
        if (isEpisodeVideoId(videoId)) {
            if (episodeMapper == null || dramaMapper == null) {
                throw new BizException(404, "video not found");
            }
            Long epId = toEpisodeId(videoId);
            MediaEpisode ep = episodeMapper.selectByPrimaryKey(epId);
            if (ep == null) {
                throw new BizException(404, "video not found");
            }
            MediaDrama drama = dramaMapper.selectByPrimaryKey(ep.getDramaId());
            if (!isEpisodeApprovedAndReady(drama, ep)) {
                throw new BizException(404, "video not found");
            }
            return projectEpisodeToVideo(drama, ep);
        }
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
        if (video == null || !CreatorVideoService.STATUS_READY.equals(video.getStatus())) {
            throw new BizException(404, "video not found");
        }
        return video;
    }

    public MediaCreatorVideo getReadyVideoByContentId(String contentId) {
        Long resolved = resolveContentVideoId(null, contentId);
        return getReadyVideo(resolved);
    }

    @Transactional
    public MediaVideoComment addComment(Long userId, Long videoId, String content) {
        requireUser(userId);
        getReadyVideo(videoId);
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BizException(400, "content required");
        }
        if (text.length() > MAX_COMMENT) {
            throw new BizException(400, "content too long");
        }
        MediaVideoComment row = new MediaVideoComment();
        row.setVideoId(videoId);
        row.setUserId(userId);
        row.setContent(text);
        row.setCreatedAt(new Date());
        commentMapper.insertSelective(row);
        bumpCommentCount(videoId, 1);
        return row;
    }

    public List<MediaVideoComment> listComments(Long videoId) {
        getReadyVideo(videoId);
        MediaVideoCommentExample ex = new MediaVideoCommentExample();
        ex.createCriteria().andVideoIdEqualTo(videoId);
        ex.setOrderByClause("id asc");
        return commentMapper.selectByExample(ex);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        requireUser(userId);
        if (commentId == null) {
            throw new BizException(400, "comment_id required");
        }
        MediaVideoComment row = commentMapper.selectByPrimaryKey(commentId);
        if (row == null) {
            throw new BizException(404, "comment not found");
        }
        if (!userId.equals(row.getUserId())) {
            throw new BizException(403, "forbidden");
        }
        commentMapper.deleteByPrimaryKey(commentId);
        bumpCommentCount(row.getVideoId(), -1);
    }

    @Transactional
    public Map<String, Object> like(Long userId, Long videoId) {
        requireUser(userId);
        getReadyVideo(videoId);
        MediaVideoLikeExample ex = new MediaVideoLikeExample();
        ex.createCriteria().andVideoIdEqualTo(videoId).andUserIdEqualTo(userId);
        if (likeMapper.countByExample(ex) == 0) {
            MediaVideoLike row = new MediaVideoLike();
            row.setVideoId(videoId);
            row.setUserId(userId);
            row.setCreatedAt(new Date());
            likeMapper.insertSelective(row);
            bumpLikeCount(videoId, 1);
        }
        return likeView(userId, videoId, true);
    }

    @Transactional
    public Map<String, Object> unlike(Long userId, Long videoId) {
        requireUser(userId);
        getReadyVideo(videoId);
        MediaVideoLikeExample ex = new MediaVideoLikeExample();
        ex.createCriteria().andVideoIdEqualTo(videoId).andUserIdEqualTo(userId);
        long n = likeMapper.countByExample(ex);
        if (n > 0) {
            likeMapper.deleteByExample(ex);
            bumpLikeCount(videoId, -1);
        }
        return likeView(userId, videoId, false);
    }

    @Transactional
    public Map<String, Object> toggleLike(Long userId, Long videoId) {
        requireUser(userId);
        getReadyVideo(videoId);
        MediaVideoLikeExample ex = new MediaVideoLikeExample();
        ex.createCriteria().andVideoIdEqualTo(videoId).andUserIdEqualTo(userId);
        if (likeMapper.countByExample(ex) > 0) {
            return unlike(userId, videoId);
        }
        return like(userId, videoId);
    }

    /**
     * Two-stage rule recommend: recall (FOLLOWING + RECENT across ready creator videos and
     * published/approved Short/Long drama episodes) then filter/sort.
     * Persists the run. Not ML.
     */
    @Transactional
    public Map<String, Object> recommend(Long userId) {
        requireUser(userId);
        List<MediaChannelFollow> follows = followsOf(userId);
        List<Long> channelIds = new ArrayList<>();
        for (MediaChannelFollow f : follows) {
            channelIds.add(f.getChannelId());
        }

        List<Scored> candidates = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        if (!channelIds.isEmpty()) {
            MediaCreatorVideoExample followingEx = new MediaCreatorVideoExample();
            followingEx.createCriteria().andChannelIdIn(channelIds).andStatusEqualTo(CreatorVideoService.STATUS_READY);
            followingEx.setOrderByClause("created_at desc");
            for (MediaCreatorVideo v : videoMapper.selectByExample(followingEx)) {
                candidates.add(new Scored(v, RECALL_FOLLOWING, seen.add(v.getId()) ? null : FILTER_DUPLICATE));
            }
        }

        List<MediaCreatorVideo> recent = listReadyVideos();
        int added = 0;
        for (MediaCreatorVideo v : recent) {
            if (added >= RECENT_LIMIT) {
                break;
            }
            added++;
            String filter = seen.add(v.getId()) ? null : FILTER_DUPLICATE;
            candidates.add(new Scored(v, RECALL_RECENT, filter));
        }

        List<Scored> kept = new ArrayList<>();
        for (Scored s : candidates) {
            if (s.video == null || s.video.getId() == null) {
                s.filterReason = FILTER_NOT_READY;
                continue;
            }
            if (!CreatorVideoService.STATUS_READY.equals(s.video.getStatus())) {
                s.filterReason = FILTER_NOT_READY;
                continue;
            }
            if (s.filterReason != null) {
                continue;
            }
            kept.add(s);
        }
        kept.sort((a, b) -> {
            int followCmp = Boolean.compare(RECALL_FOLLOWING.equals(b.recall), RECALL_FOLLOWING.equals(a.recall));
            if (followCmp != 0) {
                return followCmp;
            }
            int likes = Integer.compare(nz(b.video.getLikeCount()), nz(a.video.getLikeCount()));
            if (likes != 0) {
                return likes;
            }
            long at = a.video.getCreatedAt() == null ? 0L : a.video.getCreatedAt().getTime();
            long bt = b.video.getCreatedAt() == null ? 0L : b.video.getCreatedAt().getTime();
            return Long.compare(bt, at);
        });
        for (int i = 0; i < kept.size(); i++) {
            kept.get(i).rank = i + 1;
        }

        MediaRecommendRun run = new MediaRecommendRun();
        run.setUserId(userId);
        run.setCandidateCount(candidates.size());
        run.setKeptCount(kept.size());
        run.setAlgorithm(ALGO_TWO_STAGE);
        run.setCreatedAt(new Date());
        runMapper.insertSelective(run);

        for (Scored s : candidates) {
            MediaRecommendItem item = new MediaRecommendItem();
            item.setRunId(run.getId());
            item.setVideoId(s.video.getId());
            item.setRecallReason(s.recall);
            item.setFilterReason(s.filterReason);
            item.setKept(s.filterReason == null && s.rank != null ? 1 : 0);
            item.setStage(item.getKept() == 1 ? STAGE_KEPT : STAGE_CANDIDATE);
            item.setSortRank(s.rank);
            item.setCreatedAt(new Date());
            itemMapper.insertSelective(item);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Scored s : kept) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("video_id", s.video.getId());
            row.put("content_id", toContentId(s.video.getId()));
            row.put("source_type", sourceTypeOf(s.video));
            row.put("channel_id", s.video.getChannelId());
            row.put("title", s.video.getTitle());
            row.put("cover_url", s.video.getCoverUrl());
            row.put("like_count", nz(s.video.getLikeCount()));
            row.put("comment_count", nz(s.video.getCommentCount()));
            row.put("sort_rank", s.rank);
            row.put("recall_reason", s.recall);
            items.add(row);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("run_id", run.getId());
        out.put("algorithm", ALGO_TWO_STAGE);
        out.put("machine_learning", Boolean.FALSE);
        out.put("candidate_count", candidates.size());
        out.put("kept_count", kept.size());
        out.put("items", items);
        return out;
    }

    private List<MediaCreatorVideo> listReadyEpisodeVideos() {
        if (dramaMapper == null || episodeMapper == null) {
            return List.of();
        }
        MediaDramaExample dramaEx = new MediaDramaExample();
        dramaEx.createCriteria().andStatusEqualTo("published");
        dramaEx.setOrderByClause("id desc");
        List<MediaDrama> dramas = dramaMapper.selectByExample(dramaEx);
        if (dramas == null || dramas.isEmpty()) {
            return List.of();
        }
        List<MediaCreatorVideo> out = new ArrayList<>();
        Set<Long> seenEpisodeIds = new HashSet<>();
        for (MediaDrama drama : dramas) {
            if (drama == null || drama.getId() == null || !"published".equalsIgnoreCase(drama.getStatus())) {
                continue;
            }
            MediaEpisodeExample epEx = new MediaEpisodeExample();
            epEx.createCriteria().andDramaIdEqualTo(drama.getId()).andStatusEqualTo("published");
            epEx.setOrderByClause("ep_no asc");
            List<MediaEpisode> episodes = episodeMapper.selectByExample(epEx);
            if (episodes == null) {
                continue;
            }
            for (MediaEpisode ep : episodes) {
                if (ep == null || ep.getId() == null || !seenEpisodeIds.add(ep.getId())) {
                    continue;
                }
                if (!isEpisodeApprovedAndReady(drama, ep)) {
                    continue;
                }
                out.add(projectEpisodeToVideo(drama, ep));
            }
        }
        return out;
    }

    private boolean isEpisodeApprovedAndReady(MediaDrama drama, MediaEpisode ep) {
        if (drama == null || !"published".equalsIgnoreCase(drama.getStatus())) {
            return false;
        }
        if (ep == null || ep.getId() == null || !"published".equalsIgnoreCase(ep.getStatus())) {
            return false;
        }
        if (ep.getMediaUrl() == null || ep.getMediaUrl().isBlank()) {
            return false;
        }
        if (ingestLogMapper != null) {
            String mediaId = "m_ep_" + ep.getId();
            MediaIngestLog log = ingestLogMapper.selectByMediaId(mediaId);
            if (log != null && !IngestStatus.isPlayable(log.getStatus())) {
                return false;
            }
        }
        return true;
    }

    private MediaCreatorVideo projectEpisodeToVideo(MediaDrama drama, MediaEpisode ep) {
        Long mappedId = toEpisodeVideoId(ep.getId());
        MediaCreatorVideo v = new MediaCreatorVideo();
        v.setId(mappedId);
        v.setUserId(0L);
        v.setChannelId(0L);
        String epTitle = ep.getTitle() != null && !ep.getTitle().isBlank()
                ? ep.getTitle()
                : (drama.getTitle() + " Ep." + ep.getEpNo());
        v.setTitle(epTitle);
        v.setDescription(drama.getDescription());
        v.setStatus(CreatorVideoService.STATUS_READY);
        v.setOriginalFilename("m_ep_" + ep.getId());
        v.setContentType(drama.getSource());
        v.setCoverUrl(drama.getCoverUrl());
        v.setLikeCount(countLikes(mappedId));
        v.setCommentCount(countComments(mappedId));
        Date created = ep.getCreatedAt() != null ? ep.getCreatedAt()
                : (drama.getCreatedAt() != null ? drama.getCreatedAt() : new Date(0));
        v.setCreatedAt(created);
        v.setUpdatedAt(ep.getUpdatedAt() != null ? ep.getUpdatedAt() : drama.getUpdatedAt());
        return v;
    }

    private static String sourceTypeOf(MediaCreatorVideo video) {
        if (video == null || !isEpisodeVideoId(video.getId())) {
            return SOURCE_TYPE_CREATOR;
        }
        if (SourceRefs.SYSTEM_LONG.equalsIgnoreCase(video.getContentType())) {
            return SOURCE_TYPE_LONG_DRAMA;
        }
        return SOURCE_TYPE_SHORT_DRAMA;
    }

    private int countLikes(Long videoId) {
        MediaVideoLikeExample ex = new MediaVideoLikeExample();
        ex.createCriteria().andVideoIdEqualTo(videoId);
        return (int) likeMapper.countByExample(ex);
    }

    private int countComments(Long videoId) {
        MediaVideoCommentExample ex = new MediaVideoCommentExample();
        ex.createCriteria().andVideoIdEqualTo(videoId);
        List<MediaVideoComment> list = commentMapper.selectByExample(ex);
        return list == null ? 0 : list.size();
    }

    private List<MediaChannelFollow> followsOf(Long userId) {
        MediaChannelFollowExample ex = new MediaChannelFollowExample();
        ex.createCriteria().andFollowerUserIdEqualTo(userId);
        return followMapper.selectByExample(ex);
    }

    private MediaUserChannel findByUser(Long userId) {
        MediaUserChannelExample ex = new MediaUserChannelExample();
        ex.createCriteria().andUserIdEqualTo(userId);
        List<MediaUserChannel> list = channelMapper.selectByExample(ex);
        return list.isEmpty() ? null : list.get(0);
    }

    private Map<String, Object> followView(Long userId, Long channelId, boolean following) {
        MediaChannelFollowExample ex = new MediaChannelFollowExample();
        ex.createCriteria().andChannelIdEqualTo(channelId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("channel_id", channelId);
        out.put("following", following);
        out.put("follower_count", (int) followMapper.countByExample(ex));
        out.put("user_id", userId);
        return out;
    }

    private Map<String, Object> likeView(Long userId, Long videoId, boolean liked) {
        MediaVideoLikeExample ex = new MediaVideoLikeExample();
        ex.createCriteria().andVideoIdEqualTo(videoId);
        MediaCreatorVideo video = isEpisodeVideoId(videoId) ? null : videoMapper.selectByPrimaryKey(videoId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("video_id", videoId);
        out.put("content_id", toContentId(videoId));
        out.put("liked", liked);
        out.put("like_count", video == null ? (int) likeMapper.countByExample(ex) : nz(video.getLikeCount()));
        out.put("user_id", userId);
        return out;
    }

    private void bumpLikeCount(Long videoId, int delta) {
        if (isEpisodeVideoId(videoId)) {
            return;
        }
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
        if (video == null) {
            return;
        }
        int next = Math.max(0, nz(video.getLikeCount()) + delta);
        video.setLikeCount(next);
        videoMapper.updateByPrimaryKeySelective(video);
    }

    private void bumpCommentCount(Long videoId, int delta) {
        if (isEpisodeVideoId(videoId)) {
            return;
        }
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
        if (video == null) {
            return;
        }
        int next = Math.max(0, nz(video.getCommentCount()) + delta);
        video.setCommentCount(next);
        videoMapper.updateByPrimaryKeySelective(video);
    }

    private static void requireUser(Long userId) {
        if (userId == null) {
            throw new BizException(401, "login required");
        }
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static final class Scored {
        final MediaCreatorVideo video;
        final String recall;
        String filterReason;
        Integer rank;

        Scored(MediaCreatorVideo video, String recall, String filterReason) {
            this.video = video;
            this.recall = recall;
            this.filterReason = filterReason;
        }
    }
}
