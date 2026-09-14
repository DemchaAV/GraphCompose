package com.demcha.examples.support;

import com.demcha.examples.GeneratedCatalogue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ties every "ATS-friendly" badge to the sample the parsers read.
 *
 * <p>A badge is earned by one rendering of a preset's showcase sample. Every release renders the
 * samples again, and a later change — a heading renamed back, a block moved beside another, a
 * renderer regression — can change what a parser reads while the register still shows the badge.
 * Each badged sample's text is therefore fingerprinted and pinned beside the status its check
 * produced, and a sample whose text drifts fails here until it has been read with the parsers
 * again.</p>
 */
class ShowcaseAtsEvidenceTest {

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
    void everyBadgedSampleIsStillTheOneTheParsersRead() throws IOException {
        Map<String, Path> documents = cvDocuments();
        Map<String, String> drifted = new TreeMap<>();
        for (Map.Entry<String, ShowcaseAtsEvidence.Pin> pin : ShowcaseAtsEvidence.pins().entrySet()) {
            Path pdf = documents.get(pin.getKey());
            String actual = pdf == null ? "no rendered sample" : ShowcaseAtsEvidence.fingerprint(pdf);
            if (!actual.equals(pin.getValue().fingerprint())) {
                drifted.put(pin.getKey(), pin.getValue().status() + " " + actual);
            }
        }

        assertThat(drifted)
                .describedAs("the text a parser reads from these badged samples is no longer the text "
                        + "the ATS check read. Read each sample with the parsers again: if it still "
                        + "passes, pin the line shown here in ats-validated-samples.properties and move "
                        + "ShowcaseMetadata.ATS_LAST_VALIDATED to the day of the check; if it does not, "
                        + "withdraw the badge")
                .isEmpty();
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
