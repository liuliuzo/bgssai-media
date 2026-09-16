package com.bgssai.media.common.probe;

/**
 * Outcome of really reaching out to a media asset (MEDIA-01).
 *
 * <p>The ingest gate used to accept any non-blank {@code http(s)} string as an asset.
 * A dead link, an HTML error page served with 200, a private object and a file that no
 * player can decode all looked identical to it, so every one of them could reach READY.
 * This result type is what makes those four cases distinguishable:
 *
 * <ul>
 *   <li>{@link Verdict#PLAYABLE} — bytes were fetched and a known container was recognised.</li>
 *   <li>{@link Verdict#REJECTED} — the asset is definitively not playable (404/410, 401/403,
 *       HTML masquerade, unknown container). Terminal; retrying the same URL will not help.</li>
 *   <li>{@link Verdict#RETRYABLE} — the answer is unknown right now (timeout, DNS, 5xx, 429).
 *       Must map to PENDING, never READY and never FAILED, so a retry is still allowed.</li>
 *   <li>{@link Verdict#SKIPPED} — verification did not run (probe disabled, or an OBS object
 *       with no playable URL). Also PENDING: not verified is not the same as playable.</li>
 * </ul>
 */
public final class MediaProbeResult {

    public enum Verdict {
        PLAYABLE,
        REJECTED,
        RETRYABLE,
        SKIPPED
    }

    private final Verdict verdict;
    private final String message;
    private final int httpStatus;
    private final String contentType;
    private final long contentLength;
    private final String container;
    private final boolean seekable;

    private MediaProbeResult(
            Verdict verdict,
            String message,
            int httpStatus,
            String contentType,
            long contentLength,
            String container,
            boolean seekable) {
        this.verdict = verdict;
        this.message = message;
        this.httpStatus = httpStatus;
        this.contentType = contentType == null ? "" : contentType;
        this.contentLength = contentLength;
        this.container = container == null ? "" : container;
        this.seekable = seekable;
    }

    public static MediaProbeResult playable(
            int httpStatus, String contentType, long contentLength, String container, boolean seekable) {
        String detail = "probe ok: " + container
                + (contentType == null || contentType.isBlank() ? "" : " (" + contentType + ")")
                + (contentLength > 0 ? ", " + contentLength + " bytes" : "")
                + (seekable ? ", seekable" : ", range requests not advertised");
        return new MediaProbeResult(
                Verdict.PLAYABLE, detail, httpStatus, contentType, contentLength, container, seekable);
    }

    public static MediaProbeResult rejected(String message, int httpStatus, String contentType) {
        return new MediaProbeResult(Verdict.REJECTED, message, httpStatus, contentType, -1, "", false);
    }

    public static MediaProbeResult retryable(String message, int httpStatus) {
        return new MediaProbeResult(Verdict.RETRYABLE, message, httpStatus, "", -1, "", false);
    }

    public static MediaProbeResult skipped(String message) {
        return new MediaProbeResult(Verdict.SKIPPED, message, 0, "", -1, "", false);
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContainer() {
        return container;
    }

    /** True when the origin advertises byte ranges, i.e. the player can seek and resume. */
    public boolean isSeekable() {
        return seekable;
    }

    public boolean isPlayable() {
        return verdict == Verdict.PLAYABLE;
    }

    public boolean isRejected() {
        return verdict == Verdict.REJECTED;
    }
}
