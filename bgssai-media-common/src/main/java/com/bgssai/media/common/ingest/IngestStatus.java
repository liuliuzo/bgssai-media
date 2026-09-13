package com.bgssai.media.common.ingest;

/**
 * Commercial ingest readiness for short-drama playback intake (MEDIA-01).
 * Only {@link #READY} may be treated as playable. Never invent READY.
 */
public final class IngestStatus {

    /** Accepted metadata; asset or storage work not finished. Not playable. */
    public static final String PENDING = "PENDING";

    /** Storage unconfigured, asset missing, or validation failed. Not playable. */
    public static final String FAILED = "FAILED";

    /** Storage configured and asset reference present. Playable claim allowed. */
    public static final String READY = "READY";

    private IngestStatus() {
    }

    public static boolean isPlayable(String status) {
        return READY.equals(status);
    }

    public static boolean isTerminalFailure(String status) {
        return FAILED.equals(status);
    }
}
