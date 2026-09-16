package com.bgssai.media.common.probe;

import java.nio.charset.StandardCharsets;

/**
 * Recognises a media container from the first bytes of the response body (MEDIA-01).
 *
 * <p>Content-Type is publisher-controlled and routinely wrong — object stores hand out
 * {@code application/octet-stream}, and a login wall or an error page answers 200 with
 * {@code text/html}. Only the bytes say what the file really is, so readiness is decided
 * here and Content-Type is kept for the audit trail.
 */
public final class MediaContainerSniffer {

    public static final String UNKNOWN = "";
    public static final String HTML = "html";

    private MediaContainerSniffer() {
    }

    /**
     * @return container name (e.g. {@code mp4}, {@code hls}), {@link #HTML} for a web page
     *         pretending to be media, or {@link #UNKNOWN} when nothing matched
     */
    public static String sniff(byte[] head) {
        if (head == null || head.length == 0) {
            return UNKNOWN;
        }
        String text = new String(head, 0, Math.min(head.length, 512), StandardCharsets.ISO_8859_1);
        String trimmed = text.stripLeading();
        String lower = trimmed.toLowerCase();

        // Text-based playlists are legitimate playable assets.
        if (trimmed.startsWith("#EXTM3U")) {
            return "hls";
        }
        if (lower.startsWith("<?xml") && lower.contains("<mpd")) {
            return "dash";
        }
        // A login wall or a 200-with-error-page is the classic fake READY.
        if (lower.startsWith("<!doctype html") || lower.startsWith("<html") || lower.startsWith("<head")) {
            return HTML;
        }

        if (head.length >= 12 && matches(head, 4, "ftyp")) {
            return "mp4";
        }
        if (head.length >= 4 && head[0] == (byte) 0x1A && head[1] == (byte) 0x45
                && head[2] == (byte) 0xDF && head[3] == (byte) 0xA3) {
            return "matroska";
        }
        if (head.length >= 12 && matches(head, 0, "RIFF")) {
            if (matches(head, 8, "AVI ")) {
                return "avi";
            }
            if (matches(head, 8, "WAVE")) {
                return "wav";
            }
        }
        if (head.length >= 3 && matches(head, 0, "FLV")) {
            return "flv";
        }
        if (head.length >= 4 && matches(head, 0, "OggS")) {
            return "ogg";
        }
        if (head.length >= 4 && matches(head, 0, "fLaC")) {
            return "flac";
        }
        if (head.length >= 4 && head[0] == (byte) 0x30 && head[1] == (byte) 0x26
                && head[2] == (byte) 0xB2 && head[3] == (byte) 0x75) {
            return "asf";
        }
        if (head.length >= 3 && matches(head, 0, "ID3")) {
            return "mp3";
        }
        if (head.length >= 2 && head[0] == (byte) 0xFF && (head[1] & (byte) 0xE0) == (byte) 0xE0) {
            // MPEG audio / ADTS AAC sync word.
            return "mpeg-audio";
        }
        // MPEG-TS repeats its 0x47 sync byte every 188 bytes; one 0x47 alone proves nothing.
        if (head.length >= 189 && head[0] == (byte) 0x47 && head[188] == (byte) 0x47) {
            return "mpeg-ts";
        }
        return UNKNOWN;
    }

    private static boolean matches(byte[] head, int offset, String ascii) {
        if (head.length < offset + ascii.length()) {
            return false;
        }
        for (int i = 0; i < ascii.length(); i++) {
            if (head[offset + i] != (byte) ascii.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
