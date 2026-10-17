package com.bgssai.media.common.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaVideoCommentPinnedJsonTest {

    @Test
    void pinnedSerializesAsBoolean() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        MediaVideoComment pinned = new MediaVideoComment();
        pinned.setId(1L);
        pinned.setContent("hi");
        pinned.setPinned(1);
        String json = mapper.writeValueAsString(pinned);
        assertTrue(json.contains("\"pinned\":true"), () -> "json=" + json);
        assertFalse(json.contains("\"pinned\":1"), () -> "json=" + json);

        MediaVideoComment unpinned = new MediaVideoComment();
        unpinned.setPinned(0);
        String json0 = mapper.writeValueAsString(unpinned);
        assertTrue(json0.contains("\"pinned\":false"), () -> "json=" + json0);

        assertEquals(true, pinned.isPinned());
        assertEquals(false, unpinned.isPinned());
    }
}
