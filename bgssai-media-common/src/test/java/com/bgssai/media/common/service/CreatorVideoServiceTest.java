package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaTranscodeJob;
import com.bgssai.media.common.transcode.FfmpegGateway;
import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreatorVideoServiceTest {

    @Test
    void missingFfmpegFailsJobAndNeverMarksReady() throws Exception {
        TrackingFfmpeg ffmpeg = new TrackingFfmpeg(false);
        CreatorCommunityHarness h = new CreatorCommunityHarness(ffmpeg);
        byte[] fixture = fixtureBytes();

        Map<String, Object> uploaded = h.creator.upload(1L, "Demo", null, "tiny.mp4", "video/mp4", fixture, null);
        assertEquals(CreatorVideoService.STATUS_QUEUED, uploaded.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, Object> job = (Map<String, Object>) uploaded.get("job");
        Long jobId = (Long) job.get("id");
        Long videoId = (Long) uploaded.get("id");

        h.creator.processJob(jobId);

        Map<String, Object> after = h.creator.getJob(1L, jobId);
        assertEquals(CreatorVideoService.STATUS_FAILED, after.get("status"));
        assertEquals(CreatorVideoService.REASON_FFMPEG_MISSING, after.get("failure_reason"));
        assertNotEquals(CreatorVideoService.STATUS_READY, after.get("status"));
        assertEquals(CreatorVideoService.STATUS_FAILED, h.videos.get(0).getStatus());
        assertTrue(h.renditions.isEmpty());
        assertEquals(0, ffmpeg.transcodeCalls);
        assertThrows(BizException.class, () -> h.creator.playback(2L, videoId));
        assertThrows(BizException.class, () -> h.creator.publicAsset(videoId, "720p.mp4"));
    }

    @Test
    void mockedFfmpegProducesMultiBitrateUrlsAndCover() throws Exception {
        TrackingFfmpeg ffmpeg = new TrackingFfmpeg(true);
        CreatorCommunityHarness h = new CreatorCommunityHarness(ffmpeg);
        Map<String, Object> uploaded = h.creator.upload(
                7L, "Ready clip", "desc", "tiny.mp4", "video/mp4", fixtureBytes(), null);
        @SuppressWarnings("unchecked")
        Map<String, Object> job = (Map<String, Object>) uploaded.get("job");
        Long jobId = (Long) job.get("id");
        Long videoId = (Long) uploaded.get("id");

        h.creator.processJob(jobId);

        Map<String, Object> playback = h.creator.playback(7L, videoId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> renditions = (List<Map<String, Object>>) playback.get("renditions");
        assertEquals(3, renditions.size());
        assertEquals("360p", renditions.get(0).get("bitrate_label"));
        assertEquals("720p", renditions.get(1).get("bitrate_label"));
        assertEquals("1080p", renditions.get(2).get("bitrate_label"));
        assertEquals("/api/creator/media/" + videoId + "/720p.mp4", renditions.get(1).get("play_url"));
        assertEquals("/api/creator/media/" + videoId + "/cover.jpg", playback.get("cover_url"));
        assertEquals(CreatorVideoService.STATUS_READY, h.creator.detail(7L, videoId).get("status"));
        assertEquals(3, ffmpeg.transcodeCalls);
        assertEquals(1, ffmpeg.coverCalls);
        Path ready = h.creator.publicAsset(videoId, "720p.mp4");
        assertTrue(Files.isRegularFile(ready));
        assertTrue(Files.size(ready) > 0);
    }

    @Test
    void retryFailedJobAndRejectCrossUserRetry() throws Exception {
        TrackingFfmpeg ffmpeg = new TrackingFfmpeg(false);
        CreatorCommunityHarness h = new CreatorCommunityHarness(ffmpeg);
        Map<String, Object> uploaded = h.creator.upload(
                3L, "Retry me", null, "tiny.mp4", "video/mp4", fixtureBytes(), null);
        @SuppressWarnings("unchecked")
        Map<String, Object> job = (Map<String, Object>) uploaded.get("job");
        Long jobId = (Long) job.get("id");
        h.creator.processJob(jobId);
        assertEquals(CreatorVideoService.STATUS_FAILED, h.creator.getJob(3L, jobId).get("status"));

        BizException forbidden = assertThrows(BizException.class, () -> h.creator.retry(99L, jobId));
        assertEquals(403, forbidden.getCode());

        ffmpeg.available = true;
        Map<String, Object> retried = h.creator.retry(3L, jobId);
        assertEquals(CreatorVideoService.STATUS_QUEUED, retried.get("status"));
        assertEquals(1, retried.get("retry_count"));
        h.creator.processJob(jobId);
        assertEquals(CreatorVideoService.STATUS_READY, h.creator.getJob(3L, jobId).get("status"));
        assertEquals(3, h.renditions.size());
        BizException ready = assertThrows(BizException.class, () -> h.creator.retry(3L, jobId));
        assertEquals(400, ready.getCode());
    }

    @Test
    void otherUserCannotSeeUnreadyUpload() throws Exception {
        CreatorCommunityHarness h = new CreatorCommunityHarness(new TrackingFfmpeg(false));
        Map<String, Object> uploaded = h.creator.upload(
                1L, "secret", null, "tiny.mp4", "video/mp4", fixtureBytes(), null);
        Long videoId = (Long) uploaded.get("id");
        @SuppressWarnings("unchecked")
        Map<String, Object> job = (Map<String, Object>) uploaded.get("job");
        h.creator.processJob((Long) job.get("id"));

        BizException hidden = assertThrows(BizException.class, () -> h.creator.detail(2L, videoId));
        assertEquals(404, hidden.getCode());
        BizException jobHidden = assertThrows(BizException.class,
                () -> h.creator.getJob(2L, (Long) job.get("id")));
        assertEquals(403, jobHidden.getCode());
        assertEquals(1, h.creator.listMine(1L).size());
        assertEquals(0, h.creator.listMine(2L).size());
    }

    private static byte[] fixtureBytes() throws Exception {
        try (InputStream in = CreatorVideoServiceTest.class.getResourceAsStream("/fixtures/tiny.mp4")) {
            assertNotNull(in);
            return in.readAllBytes();
        }
    }

    static final class TrackingFfmpeg implements FfmpegGateway {
        boolean available;
        int transcodeCalls;
        int coverCalls;

        TrackingFfmpeg(boolean available) {
            this.available = available;
        }

        @Override
        public boolean available() {
            return available;
        }

        @Override
        public void transcode(Path source, Path dest, int height) throws Exception {
            transcodeCalls++;
            Files.createDirectories(dest.getParent());
            Files.writeString(dest, "rendition-" + height);
        }

        @Override
        public void extractCover(Path source, Path dest) throws Exception {
            coverCalls++;
            Files.createDirectories(dest.getParent());
            Files.writeString(dest, "cover");
        }
    }
}
