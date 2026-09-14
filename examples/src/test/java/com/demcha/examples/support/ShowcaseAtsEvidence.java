package com.demcha.examples.support;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * The badged CV samples a resume-parser check validated, as pinned in
 * {@code ats-validated-samples.properties}, and the fingerprint that pins them.
 */
final class ShowcaseAtsEvidence {

    private static final String RESOURCE = "/ats-validated-samples.properties";

    private ShowcaseAtsEvidence() {
    }

    /**
     * One validated sample.
     *
     * @param status      the status the check produced
     * @param fingerprint the fingerprint of the text the parsers read
     */
    record Pin(ShowcaseMetadata.AtsStatus status, String fingerprint) {
    }

    /**
     * Every pinned sample.
     *
     * @return the pins, by card id
     * @throws IOException when the resource cannot be read
     */
    static Map<String, Pin> pins() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = ShowcaseAtsEvidence.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(RESOURCE + " is missing from the test resources");
            }
            properties.load(in);
        }
        Map<String, Pin> pins = new TreeMap<>();
        for (String id : properties.stringPropertyNames()) {
            String[] parts = properties.getProperty(id).trim().split("\\s+");
            if (parts.length != 2) {
                throw new IllegalStateException(RESOURCE + ": " + id
                        + " needs a status and a fingerprint, separated by a space");
            }
            pins.put(id, new Pin(ShowcaseMetadata.AtsStatus.valueOf(parts[0]), parts[1]));
        }
        return pins;
    }

    /**
     * The text a resume parser reads from a PDF, reduced to a fingerprint: the PDFBox text in
     * content-stream order, then in position order, hashed with SHA-256.
     *
     * <p>Stream order is the order a pdf.js-based parser follows. Position order is closer to
     * what a layout-mode extractor sees, and it changes when a block moves beside another even
     * if nothing is redrawn in a different order. Line breaks are fixed to {@code \n} so the
     * fingerprint is the same on every platform.</p>
     *
     * @param pdf the document
     * @return the fingerprint, as lowercase hex
     * @throws IOException when the document cannot be read
     */
    static String fingerprint(Path pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            PDFTextStripper inStreamOrder = new PDFTextStripper();
            inStreamOrder.setLineSeparator("\n");
            PDFTextStripper inPositionOrder = new PDFTextStripper();
            inPositionOrder.setLineSeparator("\n");
            inPositionOrder.setSortByPosition(true);
            String text = inStreamOrder.getText(document) + "\f" + inPositionOrder.getText(document);
            return HexFormat.of().formatHex(sha256().digest(text.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("every Java runtime provides SHA-256", impossible);
        }
    }
}
