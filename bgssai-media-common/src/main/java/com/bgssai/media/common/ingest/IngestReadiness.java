package com.bgssai.media.common.ingest;

import com.bgssai.media.common.probe.MediaAssetProbe;
import com.bgssai.media.common.probe.MediaProbeResult;

/**
 * Result of evaluating whether an ingest may claim {@link IngestStatus#READY}.
 */
public final class IngestReadiness {

    private final String status;
    private final String message;

    private IngestReadiness(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public static IngestReadiness ready(String message) {
        return new IngestReadiness(IngestStatus.READY, message);
    }

    public static IngestReadiness failed(String message) {
        return new IngestReadiness(IngestStatus.FAILED, message);
    }

    public static IngestReadiness pending(String message) {
        return new IngestReadiness(IngestStatus.PENDING, message);
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isReady() {
        return IngestStatus.READY.equals(status);
    }

    public boolean isFailed() {
        return IngestStatus.FAILED.equals(status);
    }

    public boolean isPending() {
        return IngestStatus.PENDING.equals(status);
    }

    /**
     * Configuration-only check: storage is set up and an asset reference is present.
     *
     * <p>This proves nothing about the asset itself, so it can never be the last word on
     * READY. Callers that decide playability must use
     * {@link #evaluate(MediaStorageGate, MediaAssetProbe, String, String)}.
     */
    public static IngestReadiness evaluate(MediaStorageGate gate, String mediaUrl, String storageKey) {
        if (gate == null) {
            return failed("storage not configured: MediaStorageGate missing");
        }
        String storageFail = gate.unconfiguredReason();
        if (storageFail != null) {
            return failed(storageFail);
        }
        String assetFail = gate.missingAssetReason(mediaUrl, storageKey);
        if (assetFail != null) {
            return failed(assetFail);
        }
        return ready("storage configured and asset reference present");
    }

    /**
     * Full MEDIA-01 readiness: storage configuration, asset reference, and a real fetch of the
     * asset's first bytes.
     *
     * <p>The three outcomes are kept distinct on purpose. FAILED means the asset is known to be
     * unusable and resubmitting the same URL will not change that. PENDING means the answer is
     * not known yet — the origin timed out, returned 5xx, or the publisher only gave a storage
     * key — so a retry is worthwhile and the publisher must not treat it as publishable. READY
     * is reached only when bytes came back and a container was recognised.
     */
    public static IngestReadiness evaluate(
            MediaStorageGate gate, MediaAssetProbe probe, String mediaUrl, String storageKey) {
        IngestReadiness configured = evaluate(gate, mediaUrl, storageKey);
        if (!configured.isReady()) {
            return configured;
        }
        if (probe == null) {
            return pending("asset not verified: no media probe configured");
        }
        MediaProbeResult result = probe.probe(mediaUrl, storageKey);
        return switch (result.getVerdict()) {
            case PLAYABLE -> ready(result.getMessage());
            case REJECTED -> failed(result.getMessage());
            case RETRYABLE, SKIPPED -> pending(result.getMessage());
        };
    }
}
