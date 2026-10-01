package com.demcha.compose.document.templates.fidelity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * What a Word conversion of the corpus records beside its PDFs, in {@code conversion.json}
 * ({@code scripts/docx-visual/convert-with-word.ps1}): the Word build that converted, and the
 * SHA-256 of each DOCX it converted.
 *
 * <p>Word converts outside the build, so its PDFs can be of DOCX another tree exported. A PDF is
 * measured only when the DOCX it was converted from is, to the byte, the one this tree exports.</p>
 *
 * @param version   the Word build, as Word states it
 * @param converted the SHA-256 of each converted DOCX, in lower-case hex, by its file name
 */
record WordConversion(String version, Map<String, String> converted) {

    /** Reads the record a conversion left in {@code dir}; refuses one that is missing or lacks a build. */
    static WordConversion read(Path dir) throws IOException {
        Path record = dir.resolve("conversion.json");
        if (!Files.isRegularFile(record)) {
            throw new IllegalStateException("no Word conversion recorded in " + record
                                            + ": run scripts/docx-visual/word-fidelity.ps1");
        }
        JsonNode tree = new ObjectMapper().readTree(record.toFile());
        String version = tree.path("version").asText("");
        if (version.isBlank()) {
            throw new IllegalStateException(record + " names no Word build");
        }
        Map<String, String> converted = new LinkedHashMap<>();
        for (JsonNode result : tree.path("results")) {
            String source = result.path("source").asText("");
            String hash = result.path("sha256").asText("");
            if (!source.isBlank() && !hash.isBlank()) {
                converted.put(source, hash.toLowerCase(Locale.ROOT));
            }
        }
        return new WordConversion(version, converted);
    }

    /** Whether Word converted, to the byte, the DOCX given for {@code fileName}. */
    boolean convertedFrom(String fileName, byte[] docx) {
        return sha256(docx).equals(converted.get(fileName));
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }
}
