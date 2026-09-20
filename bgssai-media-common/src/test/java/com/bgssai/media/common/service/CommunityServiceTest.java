package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaRecommendItem;
import com.bgssai.media.common.domain.MediaUserChannel;
import com.bgssai.media.common.domain.MediaVideoComment;
import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommunityServiceTest {

    @Test
    void channelFollowCommentLikeAreIsolatedAcrossUsers() throws Exception {
        CreatorCommunityHarness h = new CreatorCommunityHarness(new CreatorVideoServiceTest.TrackingFfmpeg(true));
        Long alice = 10L;
        Long bob = 20L;
        Long carol = 30L;

        MediaUserChannel aliceCh = h.community.ensureMine(alice);
        MediaUserChannel carolCh = h.community.ensureMine(carol);
        assertNotEquals(aliceCh.getId(), carolCh.getId());
        assertEquals(0, h.community.listFollowing(bob).size());

        h.community.follow(alice, carolCh.getId());
        assertEquals(1, h.community.listFollowing(alice).size());
        assertEquals(0, h.community.listFollowing(bob).size());
        BizException self = assertThrows(BizException.class, () -> h.community.follow(alice, aliceCh.getId()));
        assertEquals(400, self.getCode());

        Long carolVideo = readyVideo(h, carol, "Carol clip");
        Long aliceFailed = failedVideo(h, alice, "Alice draft");

        MediaVideoComment comment = h.community.addComment(alice, carolVideo, "nice");
        assertEquals(alice, comment.getUserId());
        BizException deleteForbidden = assertThrows(BizException.class,
                () -> h.community.deleteComment(bob, comment.getId()));
        assertEquals(403, deleteForbidden.getCode());
        assertEquals(1, h.community.listComments(carolVideo).size());

        h.community.like(alice, carolVideo);
        h.community.like(bob, carolVideo);
        assertEquals(2, h.community.like(alice, carolVideo).get("like_count"));
        h.community.unlike(bob, carolVideo);
        assertEquals(1, h.community.like(alice, carolVideo).get("like_count"));

        BizException notReady = assertThrows(BizException.class,
                () -> h.community.addComment(bob, aliceFailed, "leak"));
        assertEquals(404, notReady.getCode());
        assertThrows(BizException.class, () -> h.community.like(bob, aliceFailed));
        assertThrows(BizException.class, () -> h.community.getReadyVideo(aliceFailed));
        assertTrue(h.community.listReadyVideos().stream().noneMatch(v -> aliceFailed.equals(v.getId())));
        assertTrue(h.community.listReadyVideos().stream().anyMatch(v -> carolVideo.equals(v.getId())));
    }

    @Test
    void recommendTwoStagePersistsPerUserAndSkipsUnready() throws Exception {
        CreatorCommunityHarness h = new CreatorCommunityHarness(new CreatorVideoServiceTest.TrackingFfmpeg(true));
        Long alice = 11L;
        Long bob = 22L;
        Long carol = 33L;
        MediaUserChannel carolCh = h.community.ensureMine(carol);
        h.community.follow(alice, carolCh.getId());

        Long carolVideo = readyVideo(h, carol, "Followed");
        Long bobVideo = readyVideo(h, bob, "Recent other");
        Long aliceFailed = failedVideo(h, alice, "Not public");

        Map<String, Object> forAlice = h.community.recommend(alice);
        Map<String, Object> forBob = h.community.recommend(bob);

        assertEquals(Boolean.FALSE, forAlice.get("machine_learning"));
        assertEquals(CommunityService.ALGO_TWO_STAGE, forAlice.get("algorithm"));
        assertNotEquals(forAlice.get("run_id"), forBob.get("run_id"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aliceItems = (List<Map<String, Object>>) forAlice.get("items");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bobItems = (List<Map<String, Object>>) forBob.get("items");

        assertTrue(aliceItems.stream().anyMatch(i -> carolVideo.equals(i.get("video_id"))
                && CommunityService.RECALL_FOLLOWING.equals(i.get("recall_reason"))));
        assertTrue(aliceItems.stream().noneMatch(i -> aliceFailed.equals(i.get("video_id"))));
        assertTrue(bobItems.stream().noneMatch(i -> aliceFailed.equals(i.get("video_id"))));
        assertTrue(bobItems.stream().noneMatch(i -> CommunityService.RECALL_FOLLOWING.equals(i.get("recall_reason"))));
        assertTrue(bobItems.stream().anyMatch(i -> bobVideo.equals(i.get("video_id"))));

        assertTrue(h.items.stream().anyMatch(i ->
                Integer.valueOf(1).equals(i.getKept()) && carolVideo.equals(i.getVideoId())));
        assertTrue(h.items.stream().anyMatch(i ->
                CommunityService.FILTER_DUPLICATE.equals(i.getFilterReason())));
        assertEquals(2, h.runs.size());
        assertEquals(alice, h.runs.get(0).getUserId());
        assertEquals(bob, h.runs.get(1).getUserId());
        assertTrue(h.items.stream().map(MediaRecommendItem::getVideoId).noneMatch(aliceFailed::equals));
    }

    private static Long readyVideo(CreatorCommunityHarness h, Long userId, String title) throws Exception {
        Map<String, Object> uploaded = h.creator.upload(
                userId, title, null, "tiny.mp4", "video/mp4", fixtureBytes(), null);
        @SuppressWarnings("unchecked")
        Map<String, Object> job = (Map<String, Object>) uploaded.get("job");
        h.creator.processJob((Long) job.get("id"));
        assertEquals(CreatorVideoService.STATUS_READY, h.creator.detail(userId, (Long) uploaded.get("id")).get("status"));
        return (Long) uploaded.get("id");
    }

    private static Long failedVideo(CreatorCommunityHarness h, Long userId, String title) throws Exception {
        Map<String, Object> uploaded = h.creator.upload(
                userId, title, null, "tiny.mp4", "video/mp4", fixtureBytes(), null);
        Long videoId = (Long) uploaded.get("id");
        h.videos.stream().filter(v -> videoId.equals(v.getId())).findFirst()
                .ifPresent(v -> v.setStatus(CreatorVideoService.STATUS_FAILED));
        @SuppressWarnings("unchecked")
        Map<String, Object> job = (Map<String, Object>) uploaded.get("job");
        h.jobs.stream().filter(j -> job.get("id").equals(j.getId())).findFirst()
                .ifPresent(j -> {
                    j.setStatus(CreatorVideoService.STATUS_FAILED);
                    j.setFailureReason(CreatorVideoService.REASON_FFMPEG_MISSING);
                });
        return videoId;
    }

    private static byte[] fixtureBytes() throws Exception {
        try (InputStream in = CommunityServiceTest.class.getResourceAsStream("/fixtures/tiny.mp4")) {
            assertNotNull(in);
            return in.readAllBytes();
        }
    }
}
