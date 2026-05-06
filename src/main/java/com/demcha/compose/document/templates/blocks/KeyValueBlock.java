package com.demcha.compose.document.templates.blocks;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A {@link Block} that renders as a list of bold-key followed-by-value
 * pairs on the same baseline.
 *
 * <p>Typical use is the Additional Information module of a CV
 * ({@code "Languages: English (Fluent), German (Intermediate)"},
 * {@code "Work Eligibility: Eligible to work in the UK"}). The key is
 * rendered in bold and a colon separator is appended automatically by
 * the Module composer.</p>
 *
 * @param entries key-value entries in source order; insertion order is
 *                preserved (must not be null; may be empty; individual
 *                entries must not be null)
 */
public record KeyValueBlock(List<Entry> entries) implements Block {

    /**
     * Compact constructor that defensively copies the supplied list and
     * validates that no entry reference is null.
     *
     * @throws NullPointerException if {@code entries} or any element is null
     */
    public KeyValueBlock {
        Objects.requireNonNull(entries, "entries");
        entries = List.copyOf(entries);
    }

    /**
     * Convenience factory that converts a {@link Map} into the ordered
     * list form. Useful when callers already have a {@code LinkedHashMap}
     * or {@code Map.of(...)} of key-value pairs.
     *
     * @param map source map; insertion order is preserved
     * @return new {@code KeyValueBlock} with one entry per map pair
     * @throws NullPointerException if {@code map} or any key/value is null
     */
    public static KeyValueBlock fromMap(Map<String, String> map) {
        Objects.requireNonNull(map, "map");
        List<Entry> entries = map.entrySet().stream()
                .map(e -> new Entry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        return new KeyValueBlock(entries);
    }

    /**
     * One bold-key plus value pair inside a {@link KeyValueBlock}.
     *
     * @param key   bold leading label such as {@code "Languages"} (must
     *              not be null; may be empty)
     * @param value value text following the colon separator (must not be
     *              null; may be empty)
     */
    public record Entry(String key, String value) {

        /**
         * Compact constructor that rejects null references for either
         * field.
         *
         * @throws NullPointerException if {@code key} or {@code value}
         *                              is null
         */
        public Entry {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
        }
    }
}
