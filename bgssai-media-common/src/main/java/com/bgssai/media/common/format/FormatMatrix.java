package com.bgssai.media.common.format;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Format-matrix hook shared by ingest responses and the public formats API.
 *
 * <p>Web playback is decided from {@code verified_rows}: a {@code web} container row
 * wins; otherwise a {@code desktop_vlc} container row supplies {@code web_decision}
 * (typically REJECT). Codec rows have empty extensions and are not URL matches.
 * Verification stays whatever the row records. This class does not mark a format verified.
 *
 * <p>Lockstep cases also live in {@code bgssai-media-desktop/shared/format-hook.js}
 * and bgssai-media-web {@code user/src/format/matrix.ts}: .mp4 PLAY, .mkv REJECT,
 * .m3u8 PLAY, no extension UNKNOWN.
 */
public final class FormatMatrix {

    public static final String PLAY = "PLAY";
    public static final String REJECT = "REJECT";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String NOT_VERIFIED = "NOT_VERIFIED";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Map<String, Object> DOCUMENT = load();

    private FormatMatrix() {
    }

    public static Map<String, Object> document() {
        return MAPPER.convertValue(DOCUMENT, MAP_TYPE);
    }

    public static void attach(Map<String, Object> result, String mediaUrl) {
        if (result == null) {
            return;
        }
        result.put("format_hook", hook(mediaUrl));
    }

    public static Map<String, Object> hook(String mediaUrl) {
        String extension = extensionOf(mediaUrl);
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("extension", extension);
        if (extension.isEmpty()) {
            hook.put("web_decision", UNKNOWN);
            hook.put("verification", NOT_VERIFIED);
            hook.put("declared", UNKNOWN);
            hook.put("matched_surface", null);
            hook.put("row_id", null);
            hook.put("label", null);
            return hook;
        }
        Map<String, Object> web = findContainer("web", extension);
        Map<String, Object> row = web != null ? web : findContainer("desktop_vlc", extension);
        if (row == null) {
            hook.put("web_decision", UNKNOWN);
            hook.put("verification", NOT_VERIFIED);
            hook.put("declared", UNKNOWN);
            hook.put("matched_surface", null);
            hook.put("row_id", null);
            hook.put("label", null);
            return hook;
        }
        hook.put("web_decision", stringOr(row.get("web_decision"), UNKNOWN));
        hook.put("verification", stringOr(row.get("verification"), NOT_VERIFIED));
        hook.put("declared", stringOr(row.get("declared"), UNKNOWN));
        hook.put("matched_surface", row.get("surface"));
        hook.put("row_id", row.get("id"));
        hook.put("label", row.get("label"));
        return hook;
    }

    public static String extensionOf(String input) {
        if (input == null) {
            return "";
        }
        String clean = input.trim();
        int query = clean.indexOf('?');
        if (query >= 0) {
            clean = clean.substring(0, query);
        }
        int hash = clean.indexOf('#');
        if (hash >= 0) {
            clean = clean.substring(0, hash);
        }
        int slash = clean.lastIndexOf('/');
        String name = slash >= 0 ? clean.substring(slash + 1) : clean;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot).toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> findContainer(String surface, String extension) {
        Object rows = DOCUMENT.get("verified_rows");
        if (!(rows instanceof List<?> list)) {
            return null;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                continue;
            }
            Map<String, Object> row = (Map<String, Object>) raw;
            if (!surface.equals(row.get("surface"))) {
                continue;
            }
            if (!"container".equals(row.get("kind"))) {
                continue;
            }
            Object extensions = row.get("extensions");
            if (!(extensions instanceof List<?> extList)) {
                continue;
            }
            for (Object ext : extList) {
                if (extension.equals(String.valueOf(ext).toLowerCase(Locale.ROOT))) {
                    return row;
                }
            }
        }
        return null;
    }

    private static String stringOr(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private static Map<String, Object> load() {
        try (InputStream in = FormatMatrix.class.getResourceAsStream("/format/format-matrix.json")) {
            if (in == null) {
                throw new IllegalStateException("format-matrix.json missing from classpath");
            }
            Map<String, Object> doc = MAPPER.readValue(in, MAP_TYPE);
            Object rows = doc.get("verified_rows");
            if (!(rows instanceof List<?> list) || list.isEmpty()) {
                throw new IllegalStateException("format-matrix.json has no verified_rows");
            }
            Object deployment = doc.get("deployment");
            if (!(deployment instanceof Map<?, ?> deploy)) {
                throw new IllegalStateException("format-matrix.json has no deployment");
            }
            if (!"UNDEPLOYED".equals(deploy.get("status"))) {
                throw new IllegalStateException("deployment.status must stay UNDEPLOYED until a host is allocated");
            }
            if (deploy.get("media_user_host") != null && !String.valueOf(deploy.get("media_user_host")).isEmpty()) {
                throw new IllegalStateException("media_user_host must stay empty");
            }
            if (deploy.get("media_admin_host") != null && !String.valueOf(deploy.get("media_admin_host")).isEmpty()) {
                throw new IllegalStateException("media_admin_host must stay empty");
            }
            doc.put("verified_rows", new ArrayList<>(list));
            return doc;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("failed to load format-matrix.json", ex);
        }
    }
}
