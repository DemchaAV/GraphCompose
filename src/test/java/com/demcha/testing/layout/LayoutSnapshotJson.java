package com.demcha.testing.layout;

import com.demcha.compose.layout_core.debug.LayoutSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

final class LayoutSnapshotJson {
    private static final ObjectWriter WRITER = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .writerWithDefaultPrettyPrinter();

    private LayoutSnapshotJson() {
    }

    static String toJson(LayoutSnapshot snapshot) throws java.io.IOException {
        return normalizeLineEndings(WRITER.writeValueAsString(snapshot)) + "\n";
    }

    static String normalizeLineEndings(String value) {
        return value
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }
}
