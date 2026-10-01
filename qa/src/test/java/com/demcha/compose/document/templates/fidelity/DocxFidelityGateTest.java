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
        assertThat(baseline.regressions(List.of(doc("cv-other", 1, 1, line(1, "aa", 0.4)))))
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
        assertThat(FidelityBaseline.read(file).regressions(List.of(doc("cv-b", 2, 3, line(1, "aa", -1.5)))))
                .as("the line file read back with the document file").singleElement().asString().contains("no longer found");
        assertThat(Files.readAllLines(file)).first().isEqualTo("# probe");
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
