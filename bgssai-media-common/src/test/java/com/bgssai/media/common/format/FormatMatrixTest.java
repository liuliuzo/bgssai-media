package com.bgssai.media.common.format;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatMatrixTest {

    @Test
    void deploymentStaysUndeployed() {
        Map<String, Object> doc = FormatMatrix.document();
        @SuppressWarnings("unchecked")
        Map<String, Object> deployment = (Map<String, Object>) doc.get("deployment");
        assertEquals("UNDEPLOYED", deployment.get("status"));
        assertEquals("", deployment.get("media_user_host"));
        assertEquals("", deployment.get("media_admin_host"));
        assertFalse(String.valueOf(deployment.get("note")).isBlank());
    }

    @Test
    void hookMatchesSharedCases() {
        Map<String, Object> mp4 = FormatMatrix.hook("https://cdn.example/a.mp4?token=1");
        assertEquals(".mp4", mp4.get("extension"));
        assertEquals(FormatMatrix.PLAY, mp4.get("web_decision"));
        assertEquals(FormatMatrix.NOT_VERIFIED, mp4.get("verification"));
        assertEquals("web", mp4.get("matched_surface"));

        Map<String, Object> mkv = FormatMatrix.hook("https://cdn.example/movie.MKV");
        assertEquals(FormatMatrix.REJECT, mkv.get("web_decision"));
        assertEquals("desktop_vlc", mkv.get("matched_surface"));
        assertEquals("desk-mkv", mkv.get("row_id"));

        Map<String, Object> hls = FormatMatrix.hook("https://cdn.example/live/index.m3u8");
        assertEquals(FormatMatrix.PLAY, hls.get("web_decision"));
        assertEquals("web-hls", hls.get("row_id"));

        Map<String, Object> bare = FormatMatrix.hook("https://cdn.example/file");
        assertEquals("", bare.get("extension"));
        assertEquals(FormatMatrix.UNKNOWN, bare.get("web_decision"));

        Map<String, Object> rmvb = FormatMatrix.hook("clip.rmvb");
        assertEquals(FormatMatrix.REJECT, rmvb.get("web_decision"));
    }

    @Test
    void noRowClaimsASamplePass() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) FormatMatrix.document().get("verified_rows");
        assertFalse(rows.isEmpty());
        for (Map<String, Object> row : rows) {
            assertEquals(FormatMatrix.NOT_VERIFIED, row.get("verification"));
            assertNotEquals("VERIFIED_LOCAL", row.get("verification"));
        }
    }
}
