package com.demcha.examples.support;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * The evidence each "ATS-friendly" badge stands on, as recorded in
 * {@code ats-validated-samples.properties}: the preset a badged sample renders, the status its ATS
 * check produced, the day the check ran, and the SHA-256 of the exact PDF the check read.
 *
 * <p>The PDF hash is the certification identity. A parser reads a rendering, not a list of words: a
 * block moved beside another, a line redrawn or a content stream reordered can change what pdf.js
 * or pdfplumber extracts while every word stays the same. So a sample stays certified only while it
 * renders to the very bytes its check read. The text hash beside it says, once those bytes change,
 * whether the words changed with them.</p>
 */
final class ShowcaseAtsEvidence {

    private static final String RESOURCE = "/ats-validated-samples.properties";

    /** Every field a sample's evidence carries, in the order a missing one is reported. */
    static final List<String> FIELDS = List.of("preset", "status", "validatedAt", "pdfSha256", "textSha256");

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private ShowcaseAtsEvidence() {
    }

    /**
     * What one ATS check certified about one badged sample.
     *
     * @param preset      the preset class the sample renders
     * @param status      the status the check produced
     * @param validatedAt the day the check ran
     * @param pdfSha256   the SHA-256 of the exact PDF the check read, as lowercase hex
     * @param textSha256  the {@link #textSha256(Path) text hash} of that PDF
     */
    record Certification(String preset, ShowcaseMetadata.AtsStatus status, LocalDate validatedAt,
                         String pdfSha256, String textSha256) {
    }

    /**
     * Every badged sample's evidence, by card id.
     *
     * <p>Read fail-closed. A sample missing a field, a key that is not
     * {@code <card id>.<field>}, a malformed date or hash, or a status that earns no badge stops the
     * read, rather than leaving a sample with less evidence than its badge needs.</p>
     *
     * @return the certifications, by card id
     * @throws IOException when the resource cannot be read
     */
    static Map<String, Certification> certifications() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = ShowcaseAtsEvidence.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(RESOURCE + " is missing from the test resources");
            }
            properties.load(in);
        }
        Map<String, Map<String, String>> fieldsBySample = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            int dot = key.lastIndexOf('.');
            if (dot <= 0 || !FIELDS.contains(key.substring(dot + 1))) {
                throw new IllegalStateException(RESOURCE + ": " + key
                        + " is not <card id>.<field> for a field in " + FIELDS);
            }
            fieldsBySample.computeIfAbsent(key.substring(0, dot), id -> new TreeMap<>())
                    .put(key.substring(dot + 1), properties.getProperty(key).trim());
        }
        Map<String, Certification> certifications = new TreeMap<>();
        for (Map.Entry<String, Map<String, String>> sample : fieldsBySample.entrySet()) {
            certifications.put(sample.getKey(), certification(sample.getKey(), sample.getValue()));
        }
        return certifications;
    }

    private static Certification certification(String id, Map<String, String> fields) {
        for (String field : FIELDS) {
            if (fields.getOrDefault(field, "").isEmpty()) {
                throw new IllegalStateException(RESOURCE + ": " + id + " has no " + field
                        + ". A badge stands on the whole record of the check that earned it");
            }
        }
        ShowcaseMetadata.AtsStatus status;
        try {
            status = ShowcaseMetadata.AtsStatus.valueOf(fields.get("status"));
        } catch (IllegalArgumentException unknown) {
            throw new IllegalStateException(RESOURCE + ": " + id + " has an unknown status "
                    + fields.get("status"), unknown);
        }
        if (!status.earnsBadge()) {
            throw new IllegalStateException(RESOURCE + ": " + id + " records " + status
                    + ", which earns no badge; only an ATS-friendly sample keeps certification evidence");
        }
        LocalDate validatedAt;
        try {
            validatedAt = LocalDate.parse(fields.get("validatedAt"));
        } catch (DateTimeParseException notADate) {
            throw new IllegalStateException(RESOURCE + ": " + id + ".validatedAt is not an ISO date: "
                    + fields.get("validatedAt"), notADate);
        }
        for (String hash : List.of("pdfSha256", "textSha256")) {
            if (!SHA_256.matcher(fields.get(hash)).matches()) {
                throw new IllegalStateException(RESOURCE + ": " + id + "." + hash
                        + " is not a lowercase SHA-256: " + fields.get(hash));
            }
        }
        return new Certification(fields.get("preset"), status, validatedAt,
                fields.get("pdfSha256"), fields.get("textSha256"));
    }

    /**
     * The SHA-256 of a PDF exactly as it lies on disk: every byte, nothing normalised away.
     *
     * @param pdf the document
     * @return the hash, as lowercase hex
     * @throws IOException when the document cannot be read
     */
    static String pdfSha256(Path pdf) throws IOException {
        return HexFormat.of().formatHex(sha256().digest(Files.readAllBytes(pdf)));
    }

    /**
     * The text a resume parser reads from a PDF, reduced to a hash: the PDFBox text in
     * content-stream order, then in position order, hashed with SHA-256.
     *
     * <p>Stream order is the order a pdf.js-based parser follows. Position order is closer to what
     * a layout-mode extractor sees, and it changes when a block moves beside another even if nothing
     * is redrawn in a different order. Line breaks are fixed to {@code \n} so the hash is the same
     * on every platform.</p>
     *
     * @param pdf the document
     * @return the hash, as lowercase hex
     * @throws IOException when the document cannot be read
     */
    static String textSha256(Path pdf) throws IOException {
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

    /**
     * What a published sample does not share with the certified render of the same document.
     *
     * <p>Two renders of one document through differently configured PDF backends still draw the
     * same pages: the same page sizes, byte for byte the same content streams, the same fonts
     * under the same resource names, the same links. Only the document ID and dates may differ.
     * Anything else means the published sample is no longer the document the ATS check read.</p>
     *
     * @param published the sample as its example writes it
     * @param certified the certified render
     * @return what differs; empty when the two draw the same document
     * @throws IOException when either document cannot be read
     */
    static List<String> documentDifferences(Path published, Path certified) throws IOException {
        List<String> differences = new ArrayList<>();
        try (PDDocument drawn = Loader.loadPDF(published.toFile());
             PDDocument read = Loader.loadPDF(certified.toFile())) {
            if (drawn.getNumberOfPages() != read.getNumberOfPages()) {
                differences.add(drawn.getNumberOfPages() + " pages instead of " + read.getNumberOfPages());
                return differences;
            }
            for (int index = 0; index < drawn.getNumberOfPages(); index++) {
                PDPage page = drawn.getPage(index);
                PDPage certifiedPage = read.getPage(index);
                String where = "page " + (index + 1) + " has ";
                if (!page.getMediaBox().toString().equals(certifiedPage.getMediaBox().toString())) {
                    differences.add(where + "a different size");
                }
                if (!Arrays.equals(contents(page), contents(certifiedPage))) {
                    differences.add(where + "a different content stream");
                }
                if (!fonts(page).equals(fonts(certifiedPage))) {
                    differences.add(where + "different fonts");
                }
                if (!links(page).equals(links(certifiedPage))) {
                    differences.add(where + "different links");
                }
            }
        }
        return differences;
    }

    private static byte[] contents(PDPage page) throws IOException {
        try (InputStream in = page.getContents()) {
            return in.readAllBytes();
        }
    }

    private static List<String> fonts(PDPage page) throws IOException {
        List<String> fonts = new ArrayList<>();
        PDResources resources = page.getResources();
        for (COSName name : resources.getFontNames()) {
            fonts.add(name.getName() + "=" + resources.getFont(name).getName());
        }
        Collections.sort(fonts);
        return fonts;
    }

    private static List<String> links(PDPage page) throws IOException {
        List<String> links = new ArrayList<>();
        for (PDAnnotation annotation : page.getAnnotations()) {
            String target = annotation instanceof PDAnnotationLink link
                    && link.getAction() instanceof PDActionURI uri ? uri.getURI() : "";
            links.add(annotation.getSubtype() + " " + annotation.getRectangle() + " " + target);
        }
        return links;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("every Java runtime provides SHA-256", impossible);
        }
    }
}
