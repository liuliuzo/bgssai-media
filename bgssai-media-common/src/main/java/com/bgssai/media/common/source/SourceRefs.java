package com.bgssai.media.common.source;

import com.bgssai.media.common.web.BizException;

/**
 * MEDIA-02: shared source-namespace helpers for short / long publish contracts.
 *
 * <p>Work / episode / film / version refs are namespaced so identical upstream IDs
 * from {@code bgssai-short} and {@code bgssai-long} never collide on
 * {@code media_drama.external_ref} or {@code media_ingest_log.idempotency_key}.
 *
 * <p>Short wire formats stay byte-compatible with the pre-MEDIA-02 contract:
 * {@code short:work:{id}}, {@code short:{work}:{episode}:{film}}, {@code film:{filmId}}.
 */
public final class SourceRefs {

    public static final String NS_SHORT = "short";
    public static final String NS_LONG = "long";

    public static final String SYSTEM_SHORT = "bgssai-short";
    public static final String SYSTEM_LONG = "bgssai-long";

    private SourceRefs() {
    }

    public static String workRef(String namespace, String sourceWorkId) {
        requireNs(namespace);
        require(sourceWorkId, "source_work_id");
        return namespace + ":work:" + sourceWorkId.trim();
    }

    /**
     * Storage key for the film asset. Short keeps the legacy {@code film:{id}} form;
     * Long uses {@code long:film:{id}}.
     */
    public static String filmStorageKey(String namespace, String sourceFilmId) {
        requireNs(namespace);
        require(sourceFilmId, "source_film_id");
        if (NS_SHORT.equals(namespace)) {
            return "film:" + sourceFilmId.trim();
        }
        return namespace + ":film:" + sourceFilmId.trim();
    }

    public static String shortIdempotencyKey(String sourceWorkId, String sourceEpisodeId, String sourceFilmId) {
        require(sourceWorkId, "source_work_id");
        require(sourceEpisodeId, "source_episode_id");
        require(sourceFilmId, "source_film_id");
        return NS_SHORT + ":" + sourceWorkId.trim() + ":" + sourceEpisodeId.trim() + ":" + sourceFilmId.trim();
    }

    /**
     * Long idempotency includes version so a new cut of the same episode is a new key
     * (replace-in-place on {@code (drama_id, ep_no)}), while retries of the same version replay.
     */
    public static String longIdempotencyKey(
            String sourceWorkId, String sourceEpisodeId, String sourceFilmId, String sourceVersion) {
        require(sourceWorkId, "source_work_id");
        require(sourceEpisodeId, "source_episode_id");
        require(sourceFilmId, "source_film_id");
        require(sourceVersion, "source_version");
        return NS_LONG + ":" + sourceWorkId.trim() + ":" + sourceEpisodeId.trim() + ":"
                + sourceFilmId.trim() + ":v" + sourceVersion.trim();
    }

    public static String namespaceForSystem(String sourceSystem) {
        if (SYSTEM_SHORT.equals(sourceSystem)) {
            return NS_SHORT;
        }
        if (SYSTEM_LONG.equals(sourceSystem)) {
            return NS_LONG;
        }
        throw new BizException(400, "unsupported source_system");
    }

    public static boolean isShortWorkRef(String externalRef) {
        return externalRef != null && externalRef.startsWith(NS_SHORT + ":work:");
    }

    public static boolean isLongWorkRef(String externalRef) {
        return externalRef != null && externalRef.startsWith(NS_LONG + ":work:");
    }

    public static String workRefPrefix(String namespace) {
        requireNs(namespace);
        return namespace + ":work:";
    }

    public static String contractPrefix(String namespace) {
        requireNs(namespace);
        return namespace + ":";
    }

    private static void requireNs(String namespace) {
        if (!NS_SHORT.equals(namespace) && !NS_LONG.equals(namespace)) {
            throw new BizException(400, "unsupported source namespace");
        }
    }

    private static void require(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new BizException(400, field + " required");
        }
    }
}
