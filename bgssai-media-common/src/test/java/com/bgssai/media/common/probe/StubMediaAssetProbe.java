package com.bgssai.media.common.probe;

import java.util.HashMap;
import java.util.Map;

/**
 * Test double for {@link MediaAssetProbe}. Defaults to "playable" so existing contract tests
 * keep asserting the happy path, and lets a single URL be overridden to any verdict.
 */
public final class StubMediaAssetProbe implements MediaAssetProbe {

    private final Map<String, MediaProbeResult> byUrl = new HashMap<>();
    private MediaProbeResult fallback =
            MediaProbeResult.playable(206, "video/mp4", 1024, "mp4", true);

    public static StubMediaAssetProbe playable() {
        return new StubMediaAssetProbe();
    }

    public StubMediaAssetProbe defaultResult(MediaProbeResult result) {
        this.fallback = result;
        return this;
    }

    public StubMediaAssetProbe on(String url, MediaProbeResult result) {
        byUrl.put(url, result);
        return this;
    }

    @Override
    public MediaProbeResult probe(String mediaUrl, String storageKey) {
        String url = mediaUrl == null ? "" : mediaUrl.trim();
        if (url.isEmpty()) {
            String key = storageKey == null ? "" : storageKey.trim();
            if (key.isEmpty()) {
                return MediaProbeResult.rejected("asset missing: no media_url and no storage_key", 0, "");
            }
            return MediaProbeResult.skipped("asset not verified: storage_key only");
        }
        MediaProbeResult hit = byUrl.get(url);
        return hit == null ? fallback : hit;
    }
}
