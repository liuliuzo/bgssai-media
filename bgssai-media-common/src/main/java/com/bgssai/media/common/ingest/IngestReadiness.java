package com.bgssai.media.common.ingest;

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
     * Evaluate storage + asset. Never returns READY when either gate fails.
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
}
