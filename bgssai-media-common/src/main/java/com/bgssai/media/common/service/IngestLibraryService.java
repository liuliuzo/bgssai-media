package com.bgssai.media.common.service;

import com.bgssai.media.common.web.BizException;
import com.bgssai.media.common.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimum browse surface for READY short and long ingest rows.
 *
 * <p>Recommend is the recent READY mix (short and long interleaved), not a ranked model.
 * Each catalog read is capped at 100 rows, the same limit as the existing list APIs.
 */
@Service
public class IngestLibraryService {

    public static final String KIND_ALL = "all";
    public static final String KIND_SHORT = "short";
    public static final String KIND_LONG = "long";
    public static final String RECOMMEND_BASIS = "recent_ready_ingest";
    public static final int RECOMMEND_LIMIT = 8;
    public static final int SOURCE_LIMIT = 100;

    private final ShortDramaContractService shortDramaContractService;
    private final LongDramaContractService longDramaContractService;

    public IngestLibraryService(
            ShortDramaContractService shortDramaContractService,
            LongDramaContractService longDramaContractService) {
        this.shortDramaContractService = shortDramaContractService;
        this.longDramaContractService = longDramaContractService;
    }

    public Map<String, Object> browse(String q, String kind, int page, int pageSize) {
        String normalized = normalizeKind(kind);
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1 || pageSize > SOURCE_LIMIT) {
            pageSize = 20;
        }
        String query = q == null || q.isBlank() ? null : q.trim();

        List<Map<String, Object>> shorts = List.of();
        List<Map<String, Object>> longs = List.of();
        long shortTotal = 0;
        long longTotal = 0;
        if (!KIND_LONG.equals(normalized)) {
            PageResult<Map<String, Object>> shortPage = shortDramaContractService.list(query, 1, SOURCE_LIMIT);
            shorts = tagAll(shortPage.getList(), KIND_SHORT);
            shortTotal = shortPage.getTotal();
        }
        if (!KIND_SHORT.equals(normalized)) {
            PageResult<Map<String, Object>> longPage = longDramaContractService.list(query, 1, SOURCE_LIMIT);
            longs = tagAll(longPage.getList(), KIND_LONG);
            longTotal = longPage.getTotal();
        }

        List<Map<String, Object>> merged = KIND_ALL.equals(normalized)
                ? interleave(shorts, longs)
                : new ArrayList<>(KIND_SHORT.equals(normalized) ? shorts : longs);

        int from = (page - 1) * pageSize;
        List<Map<String, Object>> slice = new ArrayList<>();
        if (from < merged.size()) {
            int to = Math.min(from + pageSize, merged.size());
            slice.addAll(merged.subList(from, to));
        }

        boolean unfiltered = query == null && KIND_ALL.equals(normalized);
        List<Map<String, Object>> recommend = List.of();
        String basis = "filtered";
        if (unfiltered) {
            int n = Math.min(RECOMMEND_LIMIT, merged.size());
            recommend = new ArrayList<>(merged.subList(0, n));
            basis = RECOMMEND_BASIS;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("list", slice);
        body.put("total", merged.size());
        body.put("page", page);
        body.put("page_size", pageSize);
        body.put("q", query);
        body.put("kind", normalized);
        body.put("source_total", shortTotal + longTotal);
        body.put("capped", shortTotal > SOURCE_LIMIT || longTotal > SOURCE_LIMIT);
        body.put("recommend", recommend);
        body.put("recommend_basis", basis);
        return body;
    }

    static String normalizeKind(String kind) {
        if (kind == null || kind.isBlank() || KIND_ALL.equalsIgnoreCase(kind.trim())) {
            return KIND_ALL;
        }
        String value = kind.trim().toLowerCase();
        if (KIND_SHORT.equals(value) || KIND_LONG.equals(value)) {
            return value;
        }
        throw new BizException(400, "kind must be all, short, or long");
    }

    static List<Map<String, Object>> interleave(
            List<Map<String, Object>> shorts, List<Map<String, Object>> longs) {
        List<Map<String, Object>> out = new ArrayList<>();
        int n = Math.max(shorts.size(), longs.size());
        for (int i = 0; i < n; i++) {
            if (i < shorts.size()) {
                out.add(shorts.get(i));
            }
            if (i < longs.size()) {
                out.add(longs.get(i));
            }
        }
        return out;
    }

    private static List<Map<String, Object>> tagAll(List<Map<String, Object>> source, String catalog) {
        List<Map<String, Object>> tagged = new ArrayList<>();
        if (source == null) {
            return tagged;
        }
        for (Map<String, Object> item : source) {
            tagged.add(tag(item, catalog));
        }
        return tagged;
    }

    private static Map<String, Object> tag(Map<String, Object> source, String catalog) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source != null) {
            copy.putAll(source);
        }
        Object mediaId = copy.get("media_id");
        String id = mediaId == null ? "" : String.valueOf(mediaId);
        copy.put("catalog", catalog);
        String encoded = UriUtils.encodePathSegment(id, StandardCharsets.UTF_8);
        copy.put("play_path", (KIND_SHORT.equals(catalog) ? "/shorts/" : "/longs/") + encoded);
        return copy;
    }
}
