package com.bgssai.media.common.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global Jackson SNAKE_CASE does not rename Map keys, so annotation-mapper rows that
 * alias columns to camelCase must be re-keyed before leaving the API.
 */
public final class SnakeMaps {

    private SnakeMaps() {
    }

    public static Map<String, Object> of(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            out.put(toSnake(e.getKey()), e.getValue());
        }
        return out;
    }

    public static List<Map<String, Object>> list(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            out.add(of(row));
        }
        return out;
    }

    public static String toSnake(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        StringBuilder sb = new StringBuilder(key.length() + 4);
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static long longOf(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }
}
