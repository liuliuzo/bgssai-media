package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaChannelFollow;
import com.bgssai.media.common.domain.MediaChannelFollowExample;
import com.bgssai.media.common.domain.MediaCreatorVideo;
import com.bgssai.media.common.domain.MediaCreatorVideoExample;
import com.bgssai.media.common.domain.MediaRecommendItem;
import com.bgssai.media.common.domain.MediaRecommendRun;
import com.bgssai.media.common.domain.MediaUserChannel;
import com.bgssai.media.common.domain.MediaUserChannelExample;
import com.bgssai.media.common.domain.MediaVideoComment;
import com.bgssai.media.common.domain.MediaVideoCommentExample;
import com.bgssai.media.common.domain.MediaVideoLike;
import com.bgssai.media.common.domain.MediaVideoLikeExample;
import com.bgssai.media.common.mapper.MediaChannelFollowMapper;
import com.bgssai.media.common.mapper.MediaCreatorVideoMapper;
import com.bgssai.media.common.mapper.MediaRecommendItemMapper;
import com.bgssai.media.common.mapper.MediaRecommendRunMapper;
import com.bgssai.media.common.mapper.MediaUserChannelMapper;
import com.bgssai.media.common.mapper.MediaVideoCommentMapper;
import com.bgssai.media.common.mapper.MediaVideoLikeMapper;
import com.bgssai.media.common.web.BizException;
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
 * Catalog is media_creator_video only; media_episode is not used.
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

    private static final int RECENT_LIMIT = 50;
    private static final int MAX_COMMENT = 2000;

    private final MediaUserChannelMapper channelMapper;
    private final MediaChannelFollowMapper followMapper;
    private final MediaCreatorVideoMapper videoMapper;
    private final MediaVideoCommentMapper commentMapper;
    private final MediaVideoLikeMapper likeMapper;
    private final MediaRecommendRunMapper runMapper;
    private final MediaRecommendItemMapper itemMapper;

    public CommunityService(MediaUserChannelMapper channelMapper,
                            MediaChannelFollowMapper followMapper,
                            MediaCreatorVideoMapper videoMapper,
                            MediaVideoCommentMapper commentMapper,
                            MediaVideoLikeMapper likeMapper,
                            MediaRecommendRunMapper runMapper,
                            MediaRecommendItemMapper itemMapper) {
        this.channelMapper = channelMapper;
        this.followMapper = followMapper;
        this.videoMapper = videoMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.runMapper = runMapper;
        this.itemMapper = itemMapper;
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
        return videoMapper.selectByExample(ex);
    }

    public MediaCreatorVideo getReadyVideo(Long videoId) {
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
        if (video == null || !CreatorVideoService.STATUS_READY.equals(video.getStatus())) {
            throw new BizException(404, "video not found");
        }
        return video;
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

    /**
     * Two-stage rule recommend: recall (FOLLOWING + RECENT) then filter/sort.
     * Persists the run. Does not use media_episode. Not ML.
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

        MediaCreatorVideoExample recentEx = new MediaCreatorVideoExample();
        recentEx.createCriteria().andStatusEqualTo(CreatorVideoService.STATUS_READY);
        recentEx.setOrderByClause("created_at desc");
        List<MediaCreatorVideo> recent = videoMapper.selectByExample(recentEx);
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
            row.put("channel_id", s.video.getChannelId());
            row.put("title", s.video.getTitle());
            row.put("cover_url", s.video.getCoverUrl());
            row.put("like_count", nz(s.video.getLikeCount()));
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
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("video_id", videoId);
        out.put("liked", liked);
        out.put("like_count", video == null ? (int) likeMapper.countByExample(ex) : nz(video.getLikeCount()));
        out.put("user_id", userId);
        return out;
    }

    private void bumpLikeCount(Long videoId, int delta) {
        MediaCreatorVideo video = videoMapper.selectByPrimaryKey(videoId);
        if (video == null) {
            return;
        }
        int next = Math.max(0, nz(video.getLikeCount()) + delta);
        video.setLikeCount(next);
        videoMapper.updateByPrimaryKeySelective(video);
    }

    private void bumpCommentCount(Long videoId, int delta) {
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
