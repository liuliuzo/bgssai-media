package com.bgssai.media.common.source;

import com.bgssai.media.common.web.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceRefsTest {

    @Test
    void shortAndLongWorkRefsDoNotCollideOnSameUpstreamId() {
        assertEquals("short:work:w1", SourceRefs.workRef(SourceRefs.NS_SHORT, "w1"));
        assertEquals("long:work:w1", SourceRefs.workRef(SourceRefs.NS_LONG, "w1"));
        assertNotEquals(
                SourceRefs.workRef(SourceRefs.NS_SHORT, "w1"),
                SourceRefs.workRef(SourceRefs.NS_LONG, "w1"));
    }

    @Test
    void shortIdempotencyKeepsLegacyWireFormat() {
        assertEquals("short:w1:e1:f1", SourceRefs.shortIdempotencyKey("w1", "e1", "f1"));
    }

    @Test
    void longIdempotencyIncludesVersion() {
        assertEquals("long:w1:e1:f1:v2", SourceRefs.longIdempotencyKey("w1", "e1", "f1", "2"));
        assertNotEquals(
                SourceRefs.longIdempotencyKey("w1", "e1", "f1", "1"),
                SourceRefs.longIdempotencyKey("w1", "e1", "f1", "2"));
    }

    @Test
    void filmStorageKeyShortStaysLegacy() {
        assertEquals("film:f1", SourceRefs.filmStorageKey(SourceRefs.NS_SHORT, "f1"));
        assertEquals("long:film:f1", SourceRefs.filmStorageKey(SourceRefs.NS_LONG, "f1"));
    }

    @Test
    void unknownSystemRejected() {
        BizException ex = assertThrows(BizException.class, () -> SourceRefs.namespaceForSystem("other"));
        assertEquals(400, ex.getCode());
    }
}
