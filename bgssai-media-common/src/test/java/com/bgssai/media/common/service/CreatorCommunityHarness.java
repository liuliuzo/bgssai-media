package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaChannelFollow;
import com.bgssai.media.common.domain.MediaCreatorVideo;
import com.bgssai.media.common.domain.MediaRecommendItem;
import com.bgssai.media.common.domain.MediaRecommendRun;
import com.bgssai.media.common.domain.MediaTranscodeJob;
import com.bgssai.media.common.domain.MediaTranscodeRendition;
import com.bgssai.media.common.domain.MediaUserChannel;
import com.bgssai.media.common.domain.MediaVideoComment;
import com.bgssai.media.common.domain.MediaVideoLike;
import com.bgssai.media.common.mapper.MediaChannelFollowMapper;
import com.bgssai.media.common.mapper.MediaCreatorVideoMapper;
import com.bgssai.media.common.mapper.MediaRecommendItemMapper;
import com.bgssai.media.common.mapper.MediaRecommendRunMapper;
import com.bgssai.media.common.mapper.MediaTranscodeJobMapper;
import com.bgssai.media.common.mapper.MediaTranscodeRenditionMapper;
import com.bgssai.media.common.mapper.MediaUserChannelMapper;
import com.bgssai.media.common.mapper.MediaVideoCommentMapper;
import com.bgssai.media.common.mapper.MediaVideoLikeMapper;
import com.bgssai.media.common.transcode.FfmpegGateway;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class CreatorCommunityHarness {
    final List<MediaUserChannel> channels = new ArrayList<>();
    final List<MediaCreatorVideo> videos = new ArrayList<>();
    final List<MediaTranscodeJob> jobs = new ArrayList<>();
    final List<MediaTranscodeRendition> renditions = new ArrayList<>();
    final List<MediaChannelFollow> follows = new ArrayList<>();
    final List<MediaVideoComment> comments = new ArrayList<>();
    final List<MediaVideoLike> likes = new ArrayList<>();
    final List<MediaRecommendRun> runs = new ArrayList<>();
    final List<MediaRecommendItem> items = new ArrayList<>();

    final AtomicLong ids = new AtomicLong(1);
    final Path storage;
    final CommunityService community;
    final CreatorVideoService creator;

    CreatorCommunityHarness(FfmpegGateway ffmpeg) throws Exception {
        this.storage = Files.createTempDirectory("creator-community-");
        MediaUserChannelMapper channelMapper = mock(MediaUserChannelMapper.class);
        MediaChannelFollowMapper followMapper = mock(MediaChannelFollowMapper.class);
        MediaCreatorVideoMapper videoMapper = mock(MediaCreatorVideoMapper.class);
        MediaVideoCommentMapper commentMapper = mock(MediaVideoCommentMapper.class);
        MediaVideoLikeMapper likeMapper = mock(MediaVideoLikeMapper.class);
        MediaRecommendRunMapper runMapper = mock(MediaRecommendRunMapper.class);
        MediaRecommendItemMapper itemMapper = mock(MediaRecommendItemMapper.class);
        MediaTranscodeJobMapper jobMapper = mock(MediaTranscodeJobMapper.class);
        MediaTranscodeRenditionMapper renditionMapper = mock(MediaTranscodeRenditionMapper.class);

        when(channelMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaUserChannel row = inv.getArgument(0);
            row.setId(ids.getAndIncrement());
            channels.add(row);
            return 1;
        });
        when(channelMapper.selectByPrimaryKey(anyLong())).thenAnswer(inv ->
                channels.stream().filter(r -> inv.getArgument(0).equals(r.getId())).findFirst().orElse(null));
        when(channelMapper.selectByExample(any())).thenAnswer(inv ->
                ExampleQuery.select(channels, inv.getArgument(0)));
        when(channelMapper.updateByPrimaryKeySelective(any())).thenAnswer(inv -> {
            MediaUserChannel patch = inv.getArgument(0);
            MediaUserChannel existing = channels.stream().filter(r -> patch.getId().equals(r.getId()))
                    .findFirst().orElse(null);
            if (existing == null) {
                return 0;
            }
            ExampleQuery.copyNonNull(patch, existing);
            return 1;
        });

        when(followMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaChannelFollow row = inv.getArgument(0);
            row.setId(ids.getAndIncrement());
            follows.add(row);
            return 1;
        });
        when(followMapper.selectByExample(any())).thenAnswer(inv ->
                ExampleQuery.select(follows, inv.getArgument(0)));
        when(followMapper.countByExample(any())).thenAnswer(inv ->
                ExampleQuery.count(follows, inv.getArgument(0)));
        when(followMapper.deleteByExample(any())).thenAnswer(inv ->
                ExampleQuery.delete(follows, inv.getArgument(0)));

        when(videoMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaCreatorVideo row = inv.getArgument(0);
            row.setId(ids.getAndIncrement());
            videos.add(row);
            return 1;
        });
        when(videoMapper.selectByPrimaryKey(anyLong())).thenAnswer(inv ->
                videos.stream().filter(r -> inv.getArgument(0).equals(r.getId())).findFirst().orElse(null));
        when(videoMapper.selectByExample(any())).thenAnswer(inv ->
                ExampleQuery.select(videos, inv.getArgument(0)));
        when(videoMapper.updateByPrimaryKeySelective(any())).thenAnswer(inv -> {
            MediaCreatorVideo patch = inv.getArgument(0);
            MediaCreatorVideo existing = videos.stream().filter(r -> patch.getId().equals(r.getId()))
                    .findFirst().orElse(null);
            if (existing == null) {
                return 0;
            }
            ExampleQuery.copyNonNull(patch, existing);
            return 1;
        });

        when(commentMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaVideoComment row = inv.getArgument(0);
            row.setId(ids.getAndIncrement());
            comments.add(row);
            return 1;
        });
        when(commentMapper.selectByPrimaryKey(anyLong())).thenAnswer(inv ->
                comments.stream().filter(r -> inv.getArgument(0).equals(r.getId())).findFirst().orElse(null));
        when(commentMapper.selectByExample(any())).thenAnswer(inv ->
                ExampleQuery.select(comments, inv.getArgument(0)));
        when(commentMapper.deleteByPrimaryKey(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return comments.removeIf(r -> id.equals(r.getId())) ? 1 : 0;
        });

        when(likeMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaVideoLike row = inv.getArgument(0);
            row.setId(ids.getAndIncrement());
            likes.add(row);
            return 1;
        });
        when(likeMapper.selectByExample(any())).thenAnswer(inv ->
                ExampleQuery.select(likes, inv.getArgument(0)));
        when(likeMapper.countByExample(any())).thenAnswer(inv ->
                ExampleQuery.count(likes, inv.getArgument(0)));
        when(likeMapper.deleteByExample(any())).thenAnswer(inv ->
                ExampleQuery.delete(likes, inv.getArgument(0)));

        when(runMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaRecommendRun row = inv.getArgument(0);
            row.setId(ids.getAndIncrement());
            runs.add(row);
            return 1;
        });
        when(runMapper.selectByPrimaryKey(anyLong())).thenAnswer(inv ->
                runs.stream().filter(r -> inv.getArgument(0).equals(r.getId())).findFirst().orElse(null));
        when(itemMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaRecommendItem row = inv.getArgument(0);
            row.setId(ids.getAndIncrement());
            items.add(row);
            return 1;
        });
        when(itemMapper.selectByExample(any())).thenAnswer(inv ->
                ExampleQuery.select(items, inv.getArgument(0)));

        when(jobMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaTranscodeJob row = inv.getArgument(0);
            row.setId(ids.getAndIncrement());
            jobs.add(row);
            return 1;
        });
        when(jobMapper.selectByPrimaryKey(anyLong())).thenAnswer(inv ->
                jobs.stream().filter(r -> inv.getArgument(0).equals(r.getId())).findFirst().orElse(null));
        when(jobMapper.selectByExample(any())).thenAnswer(inv ->
                ExampleQuery.select(jobs, inv.getArgument(0)));
        when(jobMapper.updateByPrimaryKeySelective(any())).thenAnswer(inv -> {
            MediaTranscodeJob patch = inv.getArgument(0);
            MediaTranscodeJob existing = jobs.stream().filter(r -> patch.getId().equals(r.getId()))
                    .findFirst().orElse(null);
            if (existing == null) {
                return 0;
            }
            ExampleQuery.copyNonNull(patch, existing);
            return 1;
        });

        when(renditionMapper.insertSelective(any())).thenAnswer(inv -> {
            MediaTranscodeRendition row = inv.getArgument(0);
            row.setId(ids.getAndIncrement());
            renditions.add(row);
            return 1;
        });
        when(renditionMapper.selectByExample(any())).thenAnswer(inv ->
                ExampleQuery.select(renditions, inv.getArgument(0)));
        when(renditionMapper.deleteByExample(any())).thenAnswer(inv ->
                ExampleQuery.delete(renditions, inv.getArgument(0)));

        this.community = new CommunityService(
                channelMapper, followMapper, videoMapper, commentMapper, likeMapper, runMapper, itemMapper);
        this.creator = new CreatorVideoService(
                videoMapper, jobMapper, renditionMapper, community, ffmpeg,
                storage.toString(), "/api/creator/media", null);
    }
}
