package com.demcha.compose.document.backend.semantic.docx;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Whether two builds of a node tree hold the same values: what a page zone's content built for
 * the first page and for the pages after it are compared by before the export writes it once
 * for every page ({@link DocxSemanticBackend}).
 *
 * <p>GraphCompose's own values are read field by field: the nodes, down to every run, style,
 * inset and link, and the values that are no records — a colour, a picture's data, an SVG
 * drawing, a barcode's or a table's options — since one of those built afresh is not equal to
 * itself; lists, maps and arrays element by element. Any other value is read by its own
 * {@code equals}, so one that is not a value reads as otherwise once it is built afresh: the
 * zone is then written twice, alike, rather than once with one build's content where the
 * other's belonged.</p>
 */
final class DocxNodeValues {

    /** How deep a tree is read before it is taken for otherwise: a node tree is far shallower. */
    private static final int DEEPEST = 64;

    private DocxNodeValues() {
    }

    /**
     * Whether two values are the same.
     *
     * @param one   a value, a node tree among them
     * @param other another, or {@code null}
     */
    static boolean alike(Object one, Object other) {
        return alike(one, other, 0);
    }

    private static boolean alike(Object one, Object other, int depth) {
        if (one == other) {
            return true;
        }
        if (one == null || other == null || one.getClass() != other.getClass() || depth > DEEPEST) {
            return false;
        }
        if (one instanceof byte[] data) {
            return Arrays.equals(data, (byte[]) other);
        }
        if (one instanceof Optional<?> value) {
            Optional<?> drawn = (Optional<?>) other;
            return value.isPresent() == drawn.isPresent()
                   && (value.isEmpty() || alike(value.get(), drawn.get(), depth + 1));
        }
        if (one instanceof List<?> values) {
            List<?> drawn = (List<?>) other;
            if (values.size() != drawn.size()) {
                return false;
            }
            for (int index = 0; index < values.size(); index++) {
                if (!alike(values.get(index), drawn.get(index), depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        if (one instanceof Map<?, ?> values) {
            Map<?, ?> drawn = (Map<?, ?>) other;
            if (values.size() != drawn.size()) {
                return false;
            }
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                if (!drawn.containsKey(entry.getKey()) || !alike(entry.getValue(), drawn.get(entry.getKey()), depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        if (one.getClass().isArray()) {
            int length = Array.getLength(one);
            if (length != Array.getLength(other)) {
                return false;
            }
            for (int index = 0; index < length; index++) {
                if (!alike(Array.get(one, index), Array.get(other, index), depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        if (!one.getClass().isEnum() && one.getClass().getName().startsWith("com.demcha.compose.")) {
            try {
                return fieldsAlike(one, other, depth);
            } catch (ReflectiveOperationException | RuntimeException unread) {
                // A part it cannot read tells nothing of the two: they read as otherwise.
                return false;
            }
        }
        return one.equals(other);
    }

    /** Whether two of GraphCompose's own values hold the same fields, a record's its components. */
    private static boolean fieldsAlike(Object one, Object other, int depth) throws IllegalAccessException {
        for (Class<?> type = one.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                field.setAccessible(true);
                if (!alike(field.get(one), field.get(other), depth + 1)) {
                    return false;
                }
            }
        }
        return true;
    }
}
