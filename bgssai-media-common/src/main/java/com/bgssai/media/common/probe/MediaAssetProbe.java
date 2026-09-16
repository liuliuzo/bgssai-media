package com.bgssai.media.common.probe;

/**
 * Verifies that an ingest asset can actually be played (MEDIA-01).
 *
 * <p>Implementations must never answer {@link MediaProbeResult.Verdict#PLAYABLE} on the
 * strength of the URL string alone: bytes have to come back and be recognised.
 */
public interface MediaAssetProbe {

    /**
     * @param mediaUrl   playable URL supplied by the publisher, may be blank
     * @param storageKey object key when the asset lives in managed storage, may be blank
     */
    MediaProbeResult probe(String mediaUrl, String storageKey);
}
