package com.bgssai.media.common.service;

import com.bgssai.media.common.domain.MediaCreatorVideo;
import com.bgssai.media.common.mapper.MediaCreatorVideoMapper;
import com.bgssai.media.common.mapper.OpcExtMapper;
import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserLibrarySyncServiceTest {

    OpcExtMapper opc = mock(OpcExtMapper.class);
    CommunityService community = mock(CommunityService.class);
    MediaCreatorVideoMapper videos = mock(MediaCreatorVideoMapper.class);
    UserLibrarySyncService service;

    @BeforeEach
    void setUp() {
        service = new UserLibrarySyncService(opc, community, videos);
    }

    @Test
    void watchLaterEnrichesTitleCoverPlayable() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 9L);
        row.put("videoId", 5L);
        row.put("sortOrder", 0);
        when(opc.listWatchLater(1L)).thenReturn(List.of(row));

        MediaCreatorVideo ready = new MediaCreatorVideo();
        ready.setId(5L);
        ready.setTitle("Demo");
        ready.setCoverUrl("https://cdn/cover.jpg");
        when(community.getReadyVideo(5L)).thenReturn(ready);

        List<Map<String, Object>> out = service.listWatchLater(1L);
        assertEquals(1, out.size());
        assertEquals("Demo", out.get(0).get("title"));
        assertEquals("https://cdn/cover.jpg", out.get(0).get("cover"));
        assertEquals(true, out.get(0).get("playable"));
        assertEquals(5L, out.get(0).get("video_id"));
    }

    @Test
    void watchLaterMarksUnplayableWhenNotReady() {
        Map<String, Object> row = new HashMap<>();
        row.put("videoId", 7L);
        when(opc.listWatchLater(1L)).thenReturn(new ArrayList<>(List.of(row)));
        when(community.getReadyVideo(7L)).thenThrow(new BizException(404, "video not found"));

        MediaCreatorVideo draft = new MediaCreatorVideo();
        draft.setId(7L);
        draft.setTitle("Draft");
        draft.setCoverPath("/local/cover.png");
        when(videos.selectByPrimaryKey(7L)).thenReturn(draft);

        Map<String, Object> item = service.listWatchLater(1L).get(0);
        assertEquals("Draft", item.get("title"));
        assertEquals("/local/cover.png", item.get("cover"));
        assertFalse((Boolean) item.get("playable"));
    }

    @Test
    void reorderPlaylistItemsWritesSortOrder() {
        Map<String, Object> pl = new HashMap<>();
        pl.put("userId", 1L);
        pl.put("id", 3L);
        pl.put("title", "Fav");
        pl.put("visibility", "private");
        when(opc.getPlaylist(3L)).thenReturn(pl);
        when(opc.listPlaylistItems(3L)).thenReturn(List.of());

        service.reorderPlaylistItems(1L, 3L, List.of(10L, 20L, 30L));
        verify(opc).updatePlaylistItemOrder(3L, 10L, 0);
        verify(opc).updatePlaylistItemOrder(3L, 20L, 1);
        verify(opc).updatePlaylistItemOrder(eq(3L), eq(30L), eq(2));
    }

    @Test
    void reorderRequiresVideoIds() {
        Map<String, Object> pl = new HashMap<>();
        pl.put("userId", 1L);
        when(opc.getPlaylist(3L)).thenReturn(pl);
        BizException ex = assertThrows(BizException.class,
                () -> service.reorderPlaylistItems(1L, 3L, List.of()));
        assertEquals(400, ex.getCode());
    }

    @Test
    void reorderForbiddenForNonOwner() {
        Map<String, Object> pl = new HashMap<>();
        pl.put("userId", 99L);
        when(opc.getPlaylist(3L)).thenReturn(pl);
        BizException ex = assertThrows(BizException.class,
                () -> service.reorderPlaylistItems(1L, 3L, List.of(1L)));
        assertEquals(403, ex.getCode());
    }
}
