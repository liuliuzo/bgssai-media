package com.bgssai.media.common.ingest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fail-closed storage gate for MEDIA-01.
 * Blank / unknown mode = unconfigured → ingest must FAIL (never fake READY).
 */
@Component
public class MediaStorageGate {

    public static final String MODE_REFERENCE = "REFERENCE";
    public static final String MODE_OBS = "OBS";

    private final String mode;
    private final String obsEndpoint;
    private final String obsBucket;
    private final String obsAccessKey;
    private final String obsSecretKey;

    public MediaStorageGate(
            @Value("${bgssai.media.storage.mode:}") String mode,
            @Value("${bgssai.media.obs.endpoint:}") String obsEndpoint,
            @Value("${bgssai.media.obs.bucket:}") String obsBucket,
            @Value("${bgssai.media.obs.access-key:}") String obsAccessKey,
            @Value("${bgssai.media.obs.secret-key:}") String obsSecretKey) {
        this.mode = mode == null ? "" : mode.trim();
        this.obsEndpoint = blankToEmpty(obsEndpoint);
        this.obsBucket = blankToEmpty(obsBucket);
        this.obsAccessKey = blankToEmpty(obsAccessKey);
        this.obsSecretKey = blankToEmpty(obsSecretKey);
    }

    public String getMode() {
        return mode;
    }

    public boolean isConfigured() {
        if (mode.isEmpty()) {
            return false;
        }
        if (MODE_REFERENCE.equalsIgnoreCase(mode)) {
            return true;
        }
        if (MODE_OBS.equalsIgnoreCase(mode)) {
            return !obsEndpoint.isEmpty()
                    && !obsBucket.isEmpty()
                    && !obsAccessKey.isEmpty()
                    && !obsSecretKey.isEmpty();
        }
        return false;
    }

    /**
     * @return failure reason, or null when storage is configured
     */
    public String unconfiguredReason() {
        if (mode.isEmpty()) {
            return "storage not configured: bgssai.media.storage.mode is blank";
        }
        if (MODE_REFERENCE.equalsIgnoreCase(mode)) {
            return null;
        }
        if (MODE_OBS.equalsIgnoreCase(mode)) {
            if (isConfigured()) {
                return null;
            }
            return "storage not configured: OBS mode requires endpoint, bucket, access-key, secret-key";
        }
        return "storage not configured: unsupported mode " + mode;
    }

    public boolean isReferenceMode() {
        return MODE_REFERENCE.equalsIgnoreCase(mode);
    }

    public boolean isObsMode() {
        return MODE_OBS.equalsIgnoreCase(mode);
    }

    /**
     * Asset presence for ingest (fail-closed). Does not live-probe remote URLs;
     * live Short→play E2E remains a separate commercial gate.
     *
     * @return failure reason, or null when an asset reference is present for the active mode
     */
    public String missingAssetReason(String mediaUrl, String storageKey) {
        String url = blankToEmpty(mediaUrl);
        String key = blankToEmpty(storageKey);
        if (isReferenceMode()) {
            if (url.isEmpty()) {
                return "asset missing: media_url / video_url required for REFERENCE storage";
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return "asset missing: media_url / video_url must be http(s)";
            }
            return null;
        }
        if (isObsMode()) {
            if (key.isEmpty() && url.isEmpty()) {
                return "asset missing: storage_key or media_url required for OBS storage";
            }
            return null;
        }
        return unconfiguredReason();
    }

    private static String blankToEmpty(String v) {
        return v == null || v.isBlank() ? "" : v.trim();
    }
}
