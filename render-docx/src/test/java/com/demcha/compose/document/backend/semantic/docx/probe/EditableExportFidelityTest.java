package com.demcha.compose.document.backend.semantic.docx.probe;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Measures how close a DOCX export lands to its reference PDF, once a desktop editor
 * has rendered the DOCX back to PDF.
 *
 * <p>The editor half cannot run inside the build: Word and LibreOffice are not build
 * dependencies, and rendering the DOCX with GraphCompose instead would compare the
 * engine against itself. So this reads what
 * {@code scripts/docx-visual/convert-with-word.ps1} left behind and skips when it is not
 * there. A skip is an honest "not measured here" — it is not a pass, and the manifest
 * says so.</p>
 *
 * <p><strong>This records, it does not gate.</strong> The exporter it measures is the
 * semantic one, which the capability matrix already documents as dropping fills,
 * outlines, borders and every other piece of fixed-layout geometry. Asserting that no
 * region of content is lost would be asserting something already known to be false, and
 * a red build is not a measurement. So the loss is counted and written down, and the
 * numbers become the baseline a later exporter has to beat. The gate arrives with the
 * exporter that claims the capability.</p>
 *
 * <p>Two things are asserted, because they are settled today: the editor must produce
 * the same number of pages, at the same physical size. Those held on first measurement,
 * and a regression in them is a defect rather than a known gap.</p>
 *
 * @author Artem Demchyshyn
 */
class EditableExportFidelityTest {

    private static final Path PROBE = Path.of("target", "docx-probe");
    private static final String[] FIXTURES = {"mixed-two-pager", "boundary-cases"};

    /** Suffixes of the exports compared against one reference: the shipped one, and the probe's. */
    private static final String[] VARIANTS = {"", "-prototype"};

    @Test
    void editorRenderShouldKeepPaginationAndRecordEveryRegionItLoses() throws Exception {
        Path wordDir = PROBE.resolve("word");
        Assumptions.assumeTrue(Files.isDirectory(wordDir),
                "no editor render present — run scripts/docx-visual/convert-with-word.ps1 first");

        List<String> entries = new ArrayList<>();
        for (String fixture : FIXTURES) {
            Path reference = PROBE.resolve(fixture + ".pdf");
            if (!Files.exists(reference)) {
                continue;
            }
            // Both exports of the same document are measured against the one reference,
            // so the two numbers are comparable by construction. A prototype that scored
            // well against its own render would be scoring nothing.
            for (String variant : VARIANTS) {
                String id = fixture + variant;
                Path candidate = wordDir.resolve(id + ".pdf");
                if (!Files.exists(candidate)) {
                    continue;
                }

                PdfRegionDiff.Report report = PdfRegionDiff.compare(
                        reference, candidate, PROBE.resolve("diff").resolve(id));

                assertThat(report.pageCountMatches())
                        .as("%s: reference has %d pages, the editor's render has %d",
                                id, report.referencePages(), report.candidatePages())
                        .isTrue();
                assertThat(report.sizeMismatches())
                        .as("%s: page sizes must survive the round trip", id)
                        .isEmpty();

                entries.add("""
                            {
                              "id": "%s",
                              "export": "%s",
                              "pages": %d,
                              "worstCellDifferingFraction": %.4f,
                              "cellsOver10pct": %d,
                              "cellsOver25pct": %d,
                              "lostRegions": %d,
                              "lostRegionCells": [%s],
                              "verdict": "MEASURED_NO_BUDGET_AGREED"
                            }"""
                        .formatted(fixture, variant.isEmpty() ? "semantic-backend" : "prototype",
                                report.referencePages(), report.worstCell(),
                                report.over(0.10).size(), report.over(0.25).size(),
                                report.lostContent().size(), describe(report.lostContent()))
                        .indent(2).stripTrailing());
            }
        }

        Files.writeString(PROBE.resolve("fidelity.json"),
                ("""
                 {
                   "note": "Baseline of the semantic exporter, measured through a desktop editor. \
                 Lost regions are counted, not tolerated: they are the number a later exporter has to reduce.",
                   "grid": %d,
                   "dpi": %d,
                   "fixtures": [
                 %s
                   ]
                 }
                 """).formatted(PdfRegionDiff.GRID, PdfRegionDiff.DPI, String.join(",\n", entries)),
                StandardCharsets.UTF_8);
    }

    /** Renders lost cells as {@code "p1 c7 r4"} tokens so a reader can find them on the page. */
    private static String describe(List<PdfRegionDiff.Cell> cells) {
        return cells.stream()
                .map(c -> "\"p%d c%d r%d\"".formatted(c.page() + 1, c.column(), c.row()))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}
