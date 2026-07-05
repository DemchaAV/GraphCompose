package com.demcha.compose.testing.layout;

import com.demcha.compose.document.snapshot.LayoutSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

/**
 * JSON helpers for deterministic layout snapshot baselines.
 *
 * <p>This utility keeps snapshot serialization stable so committed JSON
 * baselines stay readable, diffable, and byte-stable across platforms.</p>
 */
public final class LayoutSnapshotJson {
    private static final ObjectWriter WRITER = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .writerWithDefaultPrettyPrinter();

    private LayoutSnapshotJson() {
    }

    /**
     * Serializes a layout snapshot into normalized pretty-printed JSON.
     *
     * <p>The returned string always uses {@code \n} line endings and ends with a
     * trailing newline so baseline files stay consistent between environments.</p>
     *
     * @param snapshot resolved layout snapshot to serialize
     * @return normalized JSON payload with trailing newline
     * @throws IOException if JSON serialization fails
     */
    public static String toJson(LayoutSnapshot snapshot) throws IOException {
        return normalizeLineEndings(WRITER.writeValueAsString(snapshot)) + "\n";
    }

    /**
     * Serializes the legacy engine snapshot shape used by low-level engine tests.
     *
     * @param snapshot resolved engine snapshot to serialize
     * @return normalized JSON payload with trailing newline
     * @throws IOException if JSON serialization fails
     */
    public static String toJson(com.demcha.compose.engine.debug.LayoutSnapshot snapshot) throws IOException {
        return normalizeLineEndings(WRITER.writeValueAsString(snapshot)) + "\n";
    }

    /**
     * Normalizes any CRLF or CR line endings to LF.
     *
     * @param value input text to normalize
     * @return text using LF line endings only
     */
    public static String normalizeLineEndings(String value) {
        return value
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }
}
