package com.bgssai.media.common.service;

import com.bgssai.media.common.web.BizException;
import com.bgssai.media.common.web.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestLibraryServiceTest {

    @Mock ShortDramaContractService shorts;
    @Mock LongDramaContractService longs;

    @Test
    void browseInterleavesReadyShortAndLong() {
        when(shorts.list(null, 1, 100)).thenReturn(page(item("m_ep_1", "Short A")));
        when(longs.list(null, 1, 100)).thenReturn(page(item("m_ep_2", "Long B"), item("m_ep_3", "Long C")));
        IngestLibraryService service = new IngestLibraryService(shorts, longs);

        Map<String, Object> body = service.browse(null, null, 1, 20);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) body.get("list");
        assertEquals(3, list.size());
        assertEquals("short", list.get(0).get("catalog"));
        assertEquals("/shorts/m_ep_1", list.get(0).get("play_path"));
        assertEquals("long", list.get(1).get("catalog"));
        assertEquals("/longs/m_ep_2", list.get(1).get("play_path"));
        assertEquals("long", list.get(2).get("catalog"));
        assertEquals("all", body.get("kind"));
        assertEquals(IngestLibraryService.RECOMMEND_BASIS, body.get("recommend_basis"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recommend = (List<Map<String, Object>>) body.get("recommend");
        assertEquals(3, recommend.size());
        assertEquals(3, body.get("total"));
    }

    @Test
    void kindShortDoesNotReadLongCatalog() {
        when(shorts.list("demo", 1, 100)).thenReturn(page(item("m_ep_9", "Demo")));
        IngestLibraryService service = new IngestLibraryService(shorts, longs);

        Map<String, Object> body = service.browse(" demo ", "SHORT", 1, 20);
        assertEquals("short", body.get("kind"));
        assertEquals("demo", body.get("q"));
        assertEquals("filtered", body.get("recommend_basis"));
        verify(longs, never()).list(any(), anyInt(), anyInt());
    }

    @Test
    void unknownKindRejected() {
        IngestLibraryService service = new IngestLibraryService(shorts, longs);
        BizException ex = assertThrows(BizException.class, () -> service.browse(null, "movie", 1, 20));
        assertEquals(400, ex.getCode());
    }

    @Test
    void secondPageSlicesInterleavedList() {
        when(shorts.list(null, 1, 100)).thenReturn(page(item("s1", "S1"), item("s2", "S2")));
        when(longs.list(null, 1, 100)).thenReturn(page(item("l1", "L1")));
        IngestLibraryService service = new IngestLibraryService(shorts, longs);

        Map<String, Object> body = service.browse(null, "all", 2, 2);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) body.get("list");
        assertEquals(1, list.size());
        assertEquals("s2", list.get(0).get("media_id"));
        assertEquals(3, body.get("total"));
    }

    private static PageResult<Map<String, Object>> page(Map<String, Object>... items) {
        return new PageResult<>(items.length, 1, 100, List.of(items));
    }

    private static Map<String, Object> item(String mediaId, String title) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("media_id", mediaId);
        row.put("title", title);
        row.put("playable", true);
        return row;
    }
}
