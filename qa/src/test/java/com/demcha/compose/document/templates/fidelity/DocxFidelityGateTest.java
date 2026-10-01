package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * The fidelity gate catches what it is there to catch: lines set lower, a line lost, a page
 * more — measured on PDFs drawn to differ in just that way, and held line by line against a
 * baseline — and lets a document that moved nearer the page through.
 */
class DocxFidelityGateTest {

    private static final List<String> LINES = List.of(
            "The first paragraph of the probe document.",
            "A second paragraph, set under the first.",
            "A third paragraph closes the page.");

    @TempDir
    Path dir;

    @Test
    void aDocumentMeasuredAgainstItselfDoesNotDrift() throws Exception {
        Path page = pdf("page", flow -> LINES.forEach(flow::addParagraph));

        FidelityMeasurement measured = FidelityMeasurement.of("probe", page, page);

        assertThat(measured.lines()).isEqualTo(3);
        assertThat(measured.matched()).isEqualTo(3);
        assertThat(measured.median()).isZero();
        assertThat(measured.over2()).isZero();
    }

    @Test
    void linesSetLowerDriftByAsMuch() throws Exception {
        Path page = pdf("page", flow -> LINES.forEach(flow::addParagraph));
        Path lower = pdf("lower", flow -> {
            flow.addSpacer(spacer -> spacer.height(6));
            LINES.forEach(flow::addParagraph);
        });

        FidelityMeasurement measured = FidelityMeasurement.of("probe", page, lower);

        assertThat(measured.matched()).isEqualTo(3);
        assertThat(measured.median()).isCloseTo(6, within(0.5));
        assertThat(measured.over2()).isEqualTo(3);
    }

    @Test
    void aLostLineIsNotFound() throws Exception {
        Path page = pdf("page", flow -> LINES.forEach(flow::addParagraph));
        Path lost = pdf("lost", flow -> LINES.subList(0, 2).forEach(flow::addParagraph));

        assertThat(FidelityMeasurement.of("probe", page, lost).matched()).isEqualTo(2);
    }

    @Test
    void aPageMoreIsCounted() throws Exception {
        Path page = pdf("page", flow -> LINES.forEach(flow::addParagraph));
        Path longer = pdf("longer", flow -> {
            LINES.forEach(flow::addParagraph);
            flow.addPageBreak(pageBreak -> { });
            flow.addParagraph("Spilled onto a page of its own.");
        });

        FidelityMeasurement measured = FidelityMeasurement.of("probe", page, longer);

        assertThat(measured.enginePages()).isEqualTo(1);
        assertThat(measured.editorPages()).isEqualTo(2);
    }

    @Test
    void theBaselineFailsALineSetFurtherFromThePage() {
        FidelityBaseline baseline = FidelityBaseline.of(List.of(doc("cv-probe", 1, 1, line(1, "aa", 0.4), line(1, "bb", -2.8))));

        assertThat(baseline.regressions(List.of(doc("cv-probe", 1, 1, line(1, "aa", 1.0), line(1, "bb", -2.8)))))
                .as("a line 0.6pt further").singleElement().asString().contains("1 lines further").contains("+1.00");
        assertThat(baseline.regressions(List.of(doc("cv-probe", 1, 1, line(1, "aa", 0.4), line(1, "bb", -3.4)))))
                .as("a line already 2.8pt off, set 0.6pt further").singleElement().asString().contains("further");
    }

    @Test
    void theBaselineFailsALineNoLongerFound() {
        FidelityBaseline baseline = FidelityBaseline.of(List.of(doc("cv-probe", 1, 1, line(1, "aa", 0.4), line(1, "bb", 2.8))));

        assertThat(baseline.regressions(List.of(doc("cv-probe", 1, 1, line(1, "aa", 0.4)))))
                .singleElement().asString().contains("1 lines no longer found").contains("\"bb\"");
    }

    @Test
    void theBaselineFailsAPageFurtherFromTheEnginesCountAndADocumentItDoesNotHold() {
        FidelityBaseline baseline = FidelityBaseline.of(List.of(doc("cv-probe", 1, 1, line(1, "aa", 0.4))));

        assertThat(baseline.regressions(List.of(doc("cv-probe", 1, 2, line(1, "aa", 0.4)))))
                .singleElement().asString().contains("2 pages");
        assertThat(baseline.regressions(List.of(doc("cv-probe", 1, 1, line(1, "aa", 0.4)), doc("cv-other", 1, 1, line(1, "aa", 0.4)))))
                .singleElement().asString().contains("not in the baseline");
    }

    @Test
    void theBaselineLetsThroughALineWithinRoundingOrNearerThePage() {
        FidelityBaseline baseline = FidelityBaseline.of(List.of(doc("cv-probe", 1, 2, line(1, "aa", 0.4), line(1, "bb", 2.8))));

        assertThat(baseline.regressions(List.of(doc("cv-probe", 1, 2, line(1, "aa", 0.8), line(1, "bb", 3.2)))))
                .as("within half a point").isEmpty();
        assertThat(baseline.regressions(List.of(doc("cv-probe", 1, 1, line(1, "aa", 0), line(1, "bb", 0.1),
                line(1, "cc", 0.2))))).as("nearer the page, a page fewer and a line more found").isEmpty();
    }

    @Test
    void aBaselineIsReadAsItWasWritten() throws Exception {
        Path file = dir.resolve("baseline.tsv");
        List<FidelityMeasurement> rows = List.of(doc("cv-a", 1, 1, line(1, "aa", 0.25)),
                doc("cv-b", 2, 3, line(1, "aa", -1.5), line(2, "cc", 3)));

        FidelityBaseline.write(file, "probe", rows);

        assertThat(FidelityBaseline.read(file).regressions(rows)).isEmpty();
        assertThat(FidelityBaseline.read(file).regressions(List.of(doc("cv-a", 1, 1, line(1, "aa", 0.86)),
                doc("cv-b", 2, 3, line(1, "aa", -1.5), line(2, "cc", 3)))))
                .as("a drift read back as written: 0.25 -> 0.86 is further").singleElement().asString().contains("further");
        assertThat(FidelityBaseline.read(file).regressions(List.of(doc("cv-a", 1, 1, line(1, "aa", 0.25)),
                doc("cv-b", 2, 3, line(1, "aa", -1.5)))))
                .as("the line file read back with the document file").singleElement().asString().contains("no longer found");
        assertThat(Files.readAllLines(file)).first().isEqualTo("# probe");
    }

    @Test
    void halfAPointFurtherIsRoundingAndMoreIsNot() {
        FidelityBaseline baseline = FidelityBaseline.of(List.of(doc("cv-probe", 1, 1, line(1, "aa", 1.0))));

        assertThat(baseline.regressions(List.of(doc("cv-probe", 1, 1, line(1, "aa", 1.5))))).isEmpty();
        assertThat(baseline.regressions(List.of(doc("cv-probe", 1, 1, line(1, "aa", 1.51))))).hasSize(1);
    }

    @Test
    void aDocumentTheCorpusNoLongerHasFails() {
        FidelityBaseline baseline = FidelityBaseline.of(List.of(doc("cv-gone", 1, 1, line(1, "aa", 0))));

        assertThat(baseline.regressions(List.of())).singleElement().asString().contains("no longer in the corpus");
    }

    @Test
    void aBaselineWithoutItsLinesIsRefused() throws Exception {
        Path file = dir.resolve("baseline.tsv");
        FidelityBaseline.write(file, "probe", List.of(doc("cv-a", 1, 1, line(1, "aa", 0.25), line(1, "bb", 0.5))));
        Path lines = dir.resolve("baseline-lines.tsv");
        List<String> rows = Files.readAllLines(lines);

        Files.write(lines, rows.subList(0, rows.size() - 1));
        assertThatThrownBy(() -> FidelityBaseline.read(file)).as("a line missing from the line file")
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("cv-a");
        Files.delete(lines);
        assertThatThrownBy(() -> FidelityBaseline.read(file)).as("no line file")
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no line file");
    }

    @Test
    void aMalformedLineRowIsRefusedByName() {
        assertThatThrownBy(() -> FidelityMeasurement.parseLine("cv-a\t1:abc\tfar\tletters", new LinkedHashMap<>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cv-a");
    }

    @Test
    void linesOfTheSameLettersArePairedNearestFirst() {
        // A rota's two "09:00-18:00" on one page: the editor draws them in the other order and
        // loses the first. The one left is the second, where it stood, and the first is lost.
        PdfLines page = new PdfLines(List.of(new PdfLines.Line(0, "0900-1800", 40, 100),
                new PdfLines.Line(0, "0900-1800", 40, 200)), 1);
        PdfLines set = new PdfLines(List.of(new PdfLines.Line(0, "0900-1800", 40, 200.5)), 1);

        FidelityMeasurement measured = FidelityMeasurement.of("rota-probe", page, set);

        assertThat(measured.lines()).isEqualTo(2);
        assertThat(measured.matched()).isEqualTo(1);
        assertThat(measured.found()).hasSize(1);
        var found = measured.found().entrySet().iterator().next();
        assertThat(found.getKey()).endsWith("#1");
        assertThat(found.getValue().drift()).isEqualTo(0.5);
    }

    @Test
    void twoColumnsOnOneBaselineAreTwoLines() throws Exception {
        Path page = pdf("columns", flow -> flow.addRow("Row", row -> row
                .columns(com.demcha.compose.document.style.DocumentRowColumn.fixed(180),
                        com.demcha.compose.document.style.DocumentRowColumn.weight(1))
                .addParagraph("Left")
                .addParagraph("Right")));

        assertThat(PdfLines.of(page).lines()).extracting(PdfLines.Line::key).containsExactly("left", "right");
    }

    @Test
    void aWordConversionIsMeasuredOnlyForTheDocxItConverted() throws Exception {
        byte[] docx = docx();
        // As convert-with-word.ps1 writes it: UTF-8 with a byte-order mark, the digest in hex.
        Files.write(dir.resolve("conversion.json"), ("﻿{\"version\": \"16.0 (16.0.20430)\", \"results\": ["
                + "{\"source\": \"cv-probe.docx\", \"sha256\": \"" + WordConversion.sha256(docx).toUpperCase() + "\"}]}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));

        WordConversion conversion = WordConversion.read(dir);
        byte[] edited = docx.clone();
        edited[edited.length / 2] ^= 1;

        assertThat(conversion.version()).isEqualTo("16.0 (16.0.20430)");
        assertThat(conversion.convertedFrom("cv-probe.docx", docx())).as("exported again, the same bytes").isTrue();
        assertThat(conversion.convertedFrom("cv-probe.docx", edited)).as("a byte otherwise").isFalse();
        assertThat(conversion.convertedFrom("cv-other.docx", docx)).as("a document it never converted").isFalse();
    }

    @Test
    void aWordConversionWithoutItsRecordOrItsBuildIsRefused() throws Exception {
        assertThatThrownBy(() -> WordConversion.read(dir)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no Word conversion recorded");
        Files.writeString(dir.resolve("conversion.json"), "{\"results\": []}");
        assertThatThrownBy(() -> WordConversion.read(dir)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("names no Word build");
    }

    /** A small document's DOCX, exported as the corpus exports it. */
    private static byte[] docx() throws Exception {
        try (DocumentSession session = GraphCompose.document().pageSize(400, 300)
                .margin(DocumentInsets.of(20)).create()) {
            session.pageFlow(flow -> LINES.forEach(flow::addParagraph));
            return session.export(com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend.builder()
                    .deterministic(true).build());
        }
    }

    @Test
    void aMalformedBaselineRowIsRefusedByName() {
        assertThatThrownBy(() -> FidelityMeasurement.parse("cv-a\t1\tone\t40\t39\t0.25\t0.5\t1", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cv-a");
    }

    @SafeVarargs
    private static FidelityMeasurement doc(String stem, int enginePages, int editorPages,
                                           Map.Entry<String, FidelityMeasurement.Found>... lines) {
        Map<String, FidelityMeasurement.Found> found = new LinkedHashMap<>();
        for (Map.Entry<String, FidelityMeasurement.Found> line : lines) {
            found.put(line.getKey(), line.getValue());
        }
        return new FidelityMeasurement(stem, enginePages, editorPages, lines.length, lines.length, 0, 0, 0, found);
    }

    private static Map.Entry<String, FidelityMeasurement.Found> line(int page, String letters, double drift) {
        return Map.entry(page + ":" + letters, new FidelityMeasurement.Found(page, letters, drift));
    }

    private Path pdf(String name, Consumer<PageFlowBuilder> content) throws Exception {
        Path file = dir.resolve(name + ".pdf");
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            Files.write(file, session.toPdfBytes());
        }
        return file;
    }
}
