package com.demcha.examples.support;

import com.demcha.examples.GeneratedCatalogue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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
 * So the certification identity is the PDF itself.</p>
 *
 * <p>Each badged sample is rendered here through a deterministic backend and written under
 * {@link #CERTIFICATION_RENDERS}; its SHA-256 has to equal the hash recorded when its check ran.
 * The file its example publishes has to draw the same document, page by page. A sample that fails
 * either check fails until the ATS check has been run on the new render.</p>
 */
class ShowcaseAtsEvidenceTest {

    /** Where the certification renders are written, relative to the module directory. */
    static final Path CERTIFICATION_RENDERS = Path.of("target", "ats-certification");

    private static final Map<String, Path> RENDERED = new HashMap<>();

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
    void everyBadgedSampleIsTheExactPdfItsAtsCheckRead() throws Exception {
        Map<String, ShowcaseAtsEvidence.Certification> certifications = ShowcaseAtsEvidence.certifications();
        Map<String, String> uncertified = new TreeMap<>();
        for (String id : badgedCards()) {
            ShowcaseAtsEvidence.Certification certified = certifications.get(id);
            if (certified == null) {
                uncertified.put(id, "shows the badge, but no certified PDF hash is recorded for it");
            } else if (!ShowcaseAtsSamples.renderable().contains(id)) {
                uncertified.put(id, "shows the badge, but has no certification render to check");
            } else {
                Path render = certificationRender(id);
                if (!ShowcaseAtsEvidence.pdfSha256(render).equals(certified.pdfSha256())) {
                    uncertified.put(id, "renders to a different PDF than the one certified; "
                            + whatChanged(render, certified));
                }
            }
        }

        assertThat(uncertified)
                .describedAs("ATS certification must be rerun for these badged samples. The PDF each one "
                        + "renders is no longer the PDF ats-check read, and a badge is only as true as "
                        + "that file. Do not refresh a hash to turn this green: run the real ats-check on "
                        + "the renders this test wrote under %s, review the result, and only then record "
                        + "the new pdfSha256, textSha256 and validatedAt in ats-validated-samples.properties "
                        + "and move ShowcaseMetadata.ATS_LAST_VALIDATED — or withdraw the badge",
                        CERTIFICATION_RENDERS)
                .isEmpty();
    }

    @Test
    void everyPublishedSampleDrawsTheDocumentItsAtsCheckRead() throws Exception {
        Map<String, Path> documents = cvDocuments();
        Map<String, String> drifted = new TreeMap<>();
        for (String id : badgedCards()) {
            if (!ShowcaseAtsSamples.renderable().contains(id)) {
                // everyBadgedSampleIsTheExactPdfItsAtsCheckRead reports a badge with nothing to render.
                continue;
            }
            Path published = documents.get(id);
            if (published == null) {
                drifted.put(id, "shows the badge, but the runner published no sample for it");
                continue;
            }
            List<String> differences = ShowcaseAtsEvidence.documentDifferences(published, certificationRender(id));
            if (!differences.isEmpty()) {
                drifted.put(id, String.join("; ", differences));
            }
        }

        assertThat(drifted)
                .describedAs("ATS certification must be rerun for these badged samples. The sample each "
                        + "example publishes no longer draws the document its ATS check read, so its badge "
                        + "would describe a file nobody checked. Compose the example and its certification "
                        + "render in ShowcaseAtsSamples the same way again, run the real ats-check on the "
                        + "render, review the result, and only then record new evidence — or withdraw the "
                        + "badge")
                .isEmpty();
    }

    /** What a sample that stopped matching its certified PDF kept, so a rerun knows where to look. */
    private static String whatChanged(Path render, ShowcaseAtsEvidence.Certification certified)
            throws IOException {
        return ShowcaseAtsEvidence.textSha256(render).equals(certified.textSha256())
                ? "its text still matches the certified text, so the difference is in the layout, "
                        + "the geometry or the content stream, or in the recorded hash itself"
                : "its text changed as well";
    }

    /** The cards that show the badge. A design-first card has no certification to hold. */
    private static Set<String> badgedCards() {
        Set<String> badged = new TreeSet<>();
        ShowcaseMetadata.registeredAts().forEach((id, ats) -> {
            if (ats.status().earnsBadge()) {
                badged.add(id);
            }
        });
        return badged;
    }

    /** Renders a badged sample for certification once per run and writes it where a rerun reads it. */
    private static synchronized Path certificationRender(String id) throws Exception {
        Path render = RENDERED.get(id);
        if (render == null) {
            Files.createDirectories(CERTIFICATION_RENDERS);
            render = CERTIFICATION_RENDERS.resolve(id + ".pdf");
            Files.write(render, ShowcaseAtsSamples.render(id));
            RENDERED.put(id, render);
        }
        return render;
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
