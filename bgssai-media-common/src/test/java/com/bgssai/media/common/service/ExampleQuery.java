package com.bgssai.media.common.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class ExampleQuery {
    private ExampleQuery() {
    }

    @SuppressWarnings("unchecked")
    static <T> List<T> select(List<T> rows, Object example) {
        List<T> matched = new ArrayList<>();
        for (T row : rows) {
            if (matches(row, example)) {
                matched.add(row);
            }
        }
        String order = orderBy(example);
        if (order != null && !order.isBlank()) {
            matched.sort(comparator(order));
        }
        return matched;
    }

    static long count(List<?> rows, Object example) {
        return select(new ArrayList<>(rows), example).size();
    }

    static <T> int delete(List<T> rows, Object example) {
        List<T> remove = select(rows, example);
        rows.removeAll(remove);
        return remove.size();
    }

    static boolean matches(Object row, Object example) {
        try {
            Method getOred = example.getClass().getMethod("getOredCriteria");
            List<?> ored = (List<?>) getOred.invoke(example);
            if (ored == null || ored.isEmpty()) {
                return true;
            }
            boolean anyValid = false;
            for (Object criteria : ored) {
                Method isValid = criteria.getClass().getMethod("isValid");
                if (!Boolean.TRUE.equals(isValid.invoke(criteria))) {
                    continue;
                }
                anyValid = true;
                Method getCriteria = criteria.getClass().getMethod("getCriteria");
                List<?> crits = (List<?>) getCriteria.invoke(criteria);
                boolean and = true;
                for (Object crit : crits) {
                    if (!matchCriterion(row, crit)) {
                        and = false;
                        break;
                    }
                }
                if (and) {
                    return true;
                }
            }
            return !anyValid;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean matchCriterion(Object row, Object crit) throws ReflectiveOperationException {
        String condition = (String) crit.getClass().getMethod("getCondition").invoke(crit);
        Object value = crit.getClass().getMethod("getValue").invoke(crit);
        String cond = condition.trim().toLowerCase(Locale.ROOT);
        String column;
        String op;
        if (cond.endsWith(" in")) {
            column = cond.substring(0, cond.length() - 3).trim();
            op = "in";
        } else if (cond.endsWith(" <>")) {
            column = cond.substring(0, cond.length() - 3).trim();
            op = "<>";
        } else if (cond.endsWith(" =")) {
            column = cond.substring(0, cond.length() - 2).trim();
            op = "=";
        } else {
            throw new IllegalStateException("unsupported condition: " + condition);
        }
        Object actual = readColumn(row, column);
        if ("in".equals(op)) {
            return value instanceof List<?> list && list.contains(actual);
        }
        if ("<>".equals(op)) {
            return actual == null ? value != null : !actual.equals(value);
        }
        return actual == null ? value == null : actual.equals(value);
    }

    private static Object readColumn(Object row, String column) throws ReflectiveOperationException {
        StringBuilder getter = new StringBuilder("get");
        for (String part : column.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            getter.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return row.getClass().getMethod(getter.toString()).invoke(row);
    }

    private static String orderBy(Object example) {
        try {
            Method m = example.getClass().getMethod("getOrderByClause");
            Object v = m.invoke(example);
            return v == null ? null : String.valueOf(v);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static <T> Comparator<T> comparator(String orderByClause) {
        String[] parts = orderByClause.trim().split("\\s+");
        String column = parts[0];
        boolean desc = parts.length > 1 && parts[1].equalsIgnoreCase("desc");
        return (a, b) -> {
            try {
                Object av = readColumn(a, column);
                Object bv = readColumn(b, column);
                int cmp;
                if (av == null && bv == null) {
                    cmp = 0;
                } else if (av == null) {
                    cmp = -1;
                } else if (bv == null) {
                    cmp = 1;
                } else if (av instanceof Comparable<?> && av.getClass().isInstance(bv)) {
                    @SuppressWarnings("unchecked")
                    Comparable<Object> ca = (Comparable<Object>) av;
                    cmp = ca.compareTo(bv);
                } else {
                    cmp = String.valueOf(av).compareTo(String.valueOf(bv));
                }
                return desc ? -cmp : cmp;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    static void copyNonNull(Object patch, Object existing) {
        try {
            for (Method getter : patch.getClass().getMethods()) {
                if (!getter.getName().startsWith("get") || getter.getParameterCount() != 0
                        || getter.getName().equals("getClass")) {
                    continue;
                }
                Object value = getter.invoke(patch);
                if (value == null) {
                    continue;
                }
                String setterName = "set" + getter.getName().substring(3);
                try {
                    Method setter = existing.getClass().getMethod(setterName, getter.getReturnType());
                    setter.invoke(existing, value);
                } catch (NoSuchMethodException ignored) {
                    // skip
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static Date now() {
        return new Date();
    }
}
