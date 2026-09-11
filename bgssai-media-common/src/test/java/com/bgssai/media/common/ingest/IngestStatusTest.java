package com.bgssai.media.common.ingest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngestStatusTest {

    @Test
    void constantsAreDistinct() {
        assertEquals("PENDING", IngestStatus.PENDING);
        assertEquals("FAILED", IngestStatus.FAILED);
        assertEquals("READY", IngestStatus.READY);
        assertNotEquals(IngestStatus.PENDING, IngestStatus.READY);
        assertNotEquals(IngestStatus.FAILED, IngestStatus.READY);
    }

    @Test
    void onlyReadyIsPlayable() {
        assertTrue(IngestStatus.isPlayable(IngestStatus.READY));
        assertFalse(IngestStatus.isPlayable(IngestStatus.PENDING));
        assertFalse(IngestStatus.isPlayable(IngestStatus.FAILED));
        assertFalse(IngestStatus.isPlayable(null));
        assertFalse(IngestStatus.isPlayable("PUBLISHED"));
        assertFalse(IngestStatus.isPlayable("success"));
    }
}
