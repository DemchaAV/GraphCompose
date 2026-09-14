package com.demcha.examples.support;

import com.demcha.examples.GeneratedCatalogue;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ties every "ATS-friendly" badge to the exact PDF its ATS check read.
 *
 * <p>A badge is earned by one rendering of a preset's showcase sample, and a parser reads that
 * rendering rather than the words in it. A block moved beside another, a line redrawn or a content
 * stream reordered can change what pdf.js or pdfplumber extracts while every word stays the same.
 * So the certification identity is the PDF itself. The badged samples render deterministically;
 * each one is rendered here, and its SHA-256 has to equal the hash recorded when its check ran. A
 * sample that renders to any other bytes fails until the check has been run on the new file.</p>
 */
class ShowcaseAtsEvidenceTest {

    /** The instant {@code PdfFixedLayoutBackend.builder().deterministic(true)} pins a PDF's dates to. */
    private static final Instant DETERMINISTIC_TIMESTAMP = Instant.parse("2000-01-01T00:00:00Z");

    @BeforeAll
    static void generateEveryExample() throws Exception {
        GeneratedCatalogue.generateOnce();
    }

    @Test
    void everyCvDocumentTheRunnerWritesIsClassified() throws IOException {
        Set<String> written = cvDocuments().keySet();
        assertThat(written)
                .describedAs("no CV document was generated under %s — the guard would check nothing",
                        GeneratedCatalogue.ROOT)
                .isNotEmpty();

        Set<String> unclassified = new TreeSet<>(written);
        unclassified.removeAll(ShowcaseMetadata.registeredAts().keySet());
        assertThat(unclassified)
                .describedAs("a CV document published without a classification carries no verdict; "
                        + "register it and classify it from a resume-parser check of its sample")
                .isEmpty();
    }

    @Test
    void everyBadgedSampleIsTheExactPdfItsAtsCheckRead() throws IOException {
        Map<String, Path> documents = cvDocuments();
        Map<String, ShowcaseAtsEvidence.Certification> certifications = ShowcaseAtsEvidence.certifications();
        Map<String, String> uncertified = new TreeMap<>();
        for (Map.Entry<String, ShowcaseMetadata.Ats> card : ShowcaseMetadata.registeredAts().entrySet()) {
            if (!card.getValue().status().earnsBadge()) {
                // A design-first sample shows no badge, so there is no certification to hold.
                continue;
            }
            String id = card.getKey();
            ShowcaseAtsEvidence.Certification certified = certifications.get(id);
            Path pdf = documents.get(id);
            if (certified == null) {
                uncertified.put(id, "shows the badge, but no certified PDF hash is recorded for it");
            } else if (pdf == null) {
                uncertified.put(id, "has a certified PDF hash, but the runner rendered no sample for it");
            } else if (!ShowcaseAtsEvidence.pdfSha256(pdf).equals(certified.pdfSha256())) {
                uncertified.put(id, "renders to a different PDF than the one certified; "
                        + whatChanged(pdf, certified));
            }
        }

        assertThat(uncertified)
                .describedAs("ATS certification must be rerun for these badged samples. The PDF each one "
                        + "renders is no longer the PDF ats-check read, and a badge is only as true as "
                        + "that file. Do not refresh a hash to turn this green: render the samples "
                        + "(GenerateAllExamples writes them under %s), run the real ats-check on those "
                        + "exact files, review the result, and only then record the new pdfSha256, "
                        + "textSha256 and validatedAt in ats-validated-samples.properties and move "
                        + "ShowcaseMetadata.ATS_LAST_VALIDATED — or withdraw the badge",
                        GeneratedCatalogue.ROOT.resolve("templates").resolve("cv"))
                .isEmpty();
    }

    /** What a sample that stopped matching its certified PDF kept, so a rerun knows where to look. */
    private static String whatChanged(Path pdf, ShowcaseAtsEvidence.Certification certified)
            throws IOException {
        String text = ShowcaseAtsEvidence.textSha256(pdf).equals(certified.textSha256())
                ? "its text still matches the certified text, so the difference is in the layout, "
                        + "the geometry or the content stream, or in the recorded hash itself"
                : "its text changed as well";
        return rendersDeterministically(pdf)
                ? text
                : text + "; it also carries a live timestamp, so no two renders can match: render it "
                        + "through PdfFixedLayoutBackend.builder().deterministic(true)";
    }

    private static boolean rendersDeterministically(Path pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            Calendar created = document.getDocumentInformation().getCreationDate();
            return created != null && created.toInstant().equals(DETERMINISTIC_TIMESTAMP);
        }
    }

    /** Every generated CV document, by card id. */
    private static Map<String, Path> cvDocuments() throws IOException {
        Path root = GeneratedCatalogue.ROOT.resolve("templates").resolve("cv");
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".pdf"))
                    .collect(Collectors.toMap(
                            path -> path.getFileName().toString().replaceFirst("\\.pdf$", ""),
                            path -> path,
                            (first, second) -> first,
                            TreeMap::new));
        }
    }
}
