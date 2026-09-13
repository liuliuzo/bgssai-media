package com.bgssai.media.common.ingest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaStorageGateTest {

    @Test
    void blankModeIsUnconfigured() {
        MediaStorageGate gate = new MediaStorageGate("", "", "", "", "");
        assertFalse(gate.isConfigured());
        assertNotNull(gate.unconfiguredReason());
        IngestReadiness readiness = IngestReadiness.evaluate(gate, "https://cdn.example/a.mp4", "film:1");
        assertTrue(readiness.isFailed());
        assertEquals(IngestStatus.FAILED, readiness.getStatus());
        assertFalse(IngestStatus.isPlayable(readiness.getStatus()));
    }

    @Test
    void referenceModeReadyWithHttpUrl() {
        MediaStorageGate gate = new MediaStorageGate("REFERENCE", "", "", "", "");
        assertTrue(gate.isConfigured());
        IngestReadiness readiness = IngestReadiness.evaluate(gate, "https://cdn.example/a.mp4", "film:1");
        assertTrue(readiness.isReady());
        assertEquals(IngestStatus.READY, readiness.getStatus());
    }

    @Test
    void referenceModeFailsWhenAssetMissing() {
        MediaStorageGate gate = new MediaStorageGate("REFERENCE", "", "", "", "");
        IngestReadiness readiness = IngestReadiness.evaluate(gate, "", "film:1");
        assertTrue(readiness.isFailed());
        assertTrue(readiness.getMessage().contains("asset missing"));
    }

    @Test
    void obsModeFailsWhenCredentialsBlank() {
        MediaStorageGate gate = new MediaStorageGate("OBS", "", "", "", "");
        assertFalse(gate.isConfigured());
        IngestReadiness readiness = IngestReadiness.evaluate(gate, null, "obj/key");
        assertTrue(readiness.isFailed());
        assertTrue(readiness.getMessage().contains("storage not configured"));
    }

    @Test
    void obsModeReadyWhenConfiguredAndKeyPresent() {
        MediaStorageGate gate = new MediaStorageGate(
                "OBS", "https://obs.example", "bucket", "ak", "sk");
        assertTrue(gate.isConfigured());
        IngestReadiness readiness = IngestReadiness.evaluate(gate, null, "obj/key");
        assertTrue(readiness.isReady());
    }

    @Test
    void pendingFactoryExistsForAsyncPath() {
        IngestReadiness pending = IngestReadiness.pending("awaiting OBS copy");
        assertTrue(pending.isPending());
        assertEquals(IngestStatus.PENDING, pending.getStatus());
        assertFalse(IngestStatus.isPlayable(pending.getStatus()));
    }
}
