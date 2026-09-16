package com.bgssai.media.common.ingest;

import com.bgssai.media.common.probe.MediaProbeResult;
import com.bgssai.media.common.probe.StubMediaAssetProbe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MEDIA-01: readiness maps probe verdicts onto ingest status. Configuration alone can never
 * reach READY, and an unverified asset lands on PENDING rather than a terminal FAILED.
 */
class IngestReadinessProbeTest {

    private static final MediaStorageGate REFERENCE =
            new MediaStorageGate("REFERENCE", "", "", "", "");
    private static final String URL = "https://cdn.example/a.mp4";

    @Test
    void playableAssetReachesReady() {
        IngestReadiness readiness = IngestReadiness.evaluate(
                REFERENCE, StubMediaAssetProbe.playable(), URL, "film:1");
        assertTrue(readiness.isReady(), readiness.getMessage());
    }

    @Test
    void rejectedAssetIsTerminalFailure() {
        IngestReadiness readiness = IngestReadiness.evaluate(
                REFERENCE,
                StubMediaAssetProbe.playable().on(URL,
                        MediaProbeResult.rejected("asset dead: origin returned 404", 404, "")),
                URL,
                "film:1");
        assertTrue(readiness.isFailed());
        assertFalse(readiness.isReady());
        assertTrue(readiness.getMessage().contains("404"));
    }

    @Test
    void retryableAssetIsPendingNotReadyAndNotFailed() {
        IngestReadiness readiness = IngestReadiness.evaluate(
                REFERENCE,
                StubMediaAssetProbe.playable().on(URL,
                        MediaProbeResult.retryable("asset probe inconclusive: origin returned 503", 503)),
                URL,
                "film:1");
        assertTrue(readiness.isPending());
        assertFalse(readiness.isReady());
        assertFalse(readiness.isFailed());
    }

    @Test
    void unverifiedAssetIsPending() {
        IngestReadiness readiness = IngestReadiness.evaluate(
                REFERENCE,
                StubMediaAssetProbe.playable().defaultResult(
                        MediaProbeResult.skipped("asset not verified: probe disabled")),
                URL,
                "film:1");
        assertTrue(readiness.isPending());
    }

    @Test
    void missingProbeCannotReachReady() {
        IngestReadiness readiness = IngestReadiness.evaluate(REFERENCE, null, URL, "film:1");
        assertTrue(readiness.isPending());
        assertFalse(readiness.isReady());
    }

    @Test
    void storageGateStillShortCircuitsBeforeAnyNetworkCall() {
        IngestReadiness readiness = IngestReadiness.evaluate(
                new MediaStorageGate("", "", "", "", ""),
                StubMediaAssetProbe.playable(),
                URL,
                "film:1");
        assertTrue(readiness.isFailed());
        assertEquals(IngestStatus.FAILED, readiness.getStatus());
    }
}
