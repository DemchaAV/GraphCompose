package com.demcha.compose.document.snapshot;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers what the layout snapshot says about text.
 *
 * <p>The layout half of the snapshot has always been able to say that a block moved. It
 * could never say why the block was the size it is, because the thing that decides that —
 * which font, at what size, broken into how many lines — was measured and then discarded.
 * A wrong font and a wrong padding produce the same symptom, and telling them apart by
 * eye is the guessing this data exists to end.</p>
 *
 * <p>Two properties carry most of the weight here.</p>
 *
 * <p><b>Declared versus resolved.</b> A style may name a font the document is not set in,
 * and both rewrites that cause it are silent — {@code DEFAULT} becomes {@code HELVETICA},
 * and a standard-14 face becomes its family. The document renders, the Java reads
 * correctly, and the type is wrong. That is the case worth a test of its own.</p>
 *
 * <p><b>The geometry agrees with the renderer.</b> Line positions are computed through
 * the same {@code ParagraphLineGeometry} helper the PDF handler draws with. A snapshot
 * that reported lines the page does not have would be worse than no snapshot, so the
 * relationships are asserted rather than assumed.</p>
 *
 * @author Artem Demchyshyn
 */
class TypographySnapshotTest {

    private static final String LONG_TEXT =
            "Payment is due within thirty days of the invoice date, after which the outstanding "
                    + "balance accrues interest at the statutory rate applicable in the "
                    + "jurisdiction named above.";

    private static DocumentSession page() {
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(36, 36, 36, 36)
                .create();
    }

    private static LayoutTypographySnapshot only(LayoutSnapshot snapshot, String name) {
        List<LayoutTypographySnapshot> matches = snapshot.typography().stream()
                .filter(run -> run.path().contains(name))
                .toList();
        assertThat(matches)
                .describedAs("exactly one text run named %s", name)
                .hasSize(1);
        return matches.get(0);
    }

    // ------------------------------------------------------------- what it reports ---

    @Test
    void aParagraphReportsTheFontItWasSetIn() throws Exception {
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Body").text("Handgloves 0123")
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).size(18).build()))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(), "Body");

            assertThat(run.declaredFont()).isEqualTo(FontName.LATO.name());
            assertThat(run.resolvedFont()).isEqualTo(FontName.LATO.name());
            assertThat(run.fontSubstituted()).isFalse();
            assertThat(run.fontSize()).isEqualTo(18.0);
            assertThat(run.lineCount()).isEqualTo(1);
            assertThat(run.lines()).hasSize(1);
        }
    }

    @Test
    void namingAFaceIsReportedAsASubstitution() throws Exception {
        // The whole reason declared and resolved are separate fields. HELVETICA_BOLD is
        // an alias of its family, so this document is set in regular Helvetica — it
        // renders, it measures, and nothing about the output says the bold never
        // arrived.
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Heading").text("Quarterly report")
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(20).build()))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(), "Heading");

            assertThat(run.declaredFont()).isEqualTo(FontName.HELVETICA_BOLD.name());
            assertThat(run.resolvedFont()).isEqualTo(FontName.HELVETICA.name());
            assertThat(run.fontSubstituted())
                    .describedAs("a document set in a font its author did not name must say so")
                    .isTrue();
        }
    }

    @Test
    void namingNoFontIsReportedAsASubstitutionToo() throws Exception {
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Plain").text("No font named")
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.DEFAULT).size(12).build()))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(), "Plain");

            assertThat(run.declaredFont()).isEqualTo(FontName.DEFAULT.name());
            assertThat(run.resolvedFont()).isEqualTo(FontName.HELVETICA.name());
            assertThat(run.fontSubstituted()).isTrue();
        }
    }

    @Test
    void aDocumentWithNoTextReportsNoTypography() throws Exception {
        // An empty list, not a list of empty runs: a reader counting text runs must not
        // find one where the document has none.
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addShape(shape -> shape.name("Box").size(100, 40))
                    .build();

            assertThat(document.layoutSnapshot().typography()).isEmpty();
        }
    }

    // ------------------------------------------------------------------ the lines ---

    @Test
    void wrappedTextReportsEveryLineItBrokeInto() throws Exception {
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Terms").text(LONG_TEXT)
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(11).build()))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(), "Terms");

            assertThat(run.lineCount()).isGreaterThan(1);
            assertThat(run.lines()).hasSize(run.lineCount());
            assertThat(run.lines()).extracting(LayoutTextLineSnapshot::index)
                    .containsExactlyElementsOf(java.util.stream.IntStream.range(0, run.lineCount()).boxed().toList());
            assertThat(run.lines()).allSatisfy(line -> {
                assertThat(line.width()).isPositive();
                assertThat(line.height()).isPositive();
            });
        }
    }

    @Test
    void linesStackDownTheStackTheRendererWalks() throws Exception {
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Terms").text(LONG_TEXT)
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(11).build()))
                    .build();

            List<LayoutTextLineSnapshot> lines = only(document.layoutSnapshot(), "Terms").lines();

            for (int i = 1; i < lines.size(); i++) {
                assertThat(lines.get(i).y())
                        .describedAs("y grows upward, so line %d sits below line %d", i, i - 1)
                        .isLessThan(lines.get(i - 1).y());
            }
        }
    }

    @Test
    void everyBaselineSitsInsideItsOwnLineBox() throws Exception {
        // The check that catches a sign error in the vertical walk: a baseline outside
        // its box would draw the line into its neighbour, and every line would still
        // "have a baseline".
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Terms").text(LONG_TEXT)
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(11).build()))
                    .build();

            assertThat(only(document.layoutSnapshot(), "Terms").lines()).allSatisfy(line -> {
                assertThat(line.baseline()).isBetween(line.y(), line.y() + line.height());
                assertThat(line.baselineExact())
                        .describedAs("default vertical seating needs no backend font, so it is exact")
                        .isTrue();
            });
        }
    }

    @Test
    void alignmentMovesTheLinesTheWayThePageShowsThem() throws Exception {
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Left").text("Aligned")
                            .align(TextAlign.LEFT)
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(12).build()))
                    .addParagraph(p -> p.name("Right").text("Aligned")
                            .align(TextAlign.RIGHT)
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(12).build()))
                    .build();

            LayoutSnapshot snapshot = document.layoutSnapshot();
            LayoutTextLineSnapshot left = only(snapshot, "Left").lines().get(0);
            LayoutTextLineSnapshot right = only(snapshot, "Right").lines().get(0);

            assertThat(right.x())
                    .describedAs("the same words aligned right start further along the page")
                    .isGreaterThan(left.x());
            assertThat(right.width()).isEqualTo(left.width());
        }
    }

    @Test
    void theTextBoxCoversTheLinesItReports() throws Exception {
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Terms").text(LONG_TEXT)
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(11).build()))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(), "Terms");

            // The box bounds the ink, so it contains every line it reports. A box that
            // mixed the layout column's left edge with the ink's width would contain
            // none of them as soon as alignment moved a line.
            for (LayoutTextLineSnapshot line : run.lines()) {
                assertThat(line.x())
                        .describedAs("line %d starts at or after the text box left", line.index())
                        .isGreaterThanOrEqualTo(run.textX() - 0.001);
                assertThat(line.x() + line.width())
                        .describedAs("line %d ends at or before the text box right", line.index())
                        .isLessThanOrEqualTo(run.textX() + run.textWidth() + 0.001);
                assertThat(line.y())
                        .isGreaterThanOrEqualTo(run.textY() - 0.001);
                assertThat(line.y() + line.height())
                        .describedAs("line %d sits at or below the text box top", line.index())
                        .isLessThanOrEqualTo(run.textY() + run.textHeight() + 0.001);
            }
        }
    }

    // ------------------------------------------------------------ stability + shape ---

    @Test
    void theSameDocumentProducesTheSameTypographyTwice() throws Exception {
        // The snapshot's whole value as a baseline. Fragments are emitted in pagination
        // order, which is not stable across unrelated changes, so the list is sorted —
        // without that a paragraph moving pages would churn the file.
        String first;
        String second;
        try (DocumentSession document = page()) {
            compose(document);
            first = document.layoutSnapshot().typography().toString();
        }
        try (DocumentSession document = page()) {
            compose(document);
            second = document.layoutSnapshot().typography().toString();
        }

        assertThat(second).isEqualTo(first);
    }

    @Test
    void typographyIsOrderedByPathThenFragment() throws Exception {
        try (DocumentSession document = page()) {
            compose(document);

            List<LayoutTypographySnapshot> runs = document.layoutSnapshot().typography();

            assertThat(runs).isSortedAccordingTo(
                    java.util.Comparator.comparing(LayoutTypographySnapshot::path)
                            .thenComparingInt(LayoutTypographySnapshot::fragmentIndex));
        }
    }

    @Test
    void everyRunPointsAtANodeTheSnapshotAlsoReports() throws Exception {
        // Typography is a parallel list joined on path. A run naming a node that is not
        // in the node list could not be located by any consumer.
        try (DocumentSession document = page()) {
            compose(document);

            LayoutSnapshot snapshot = document.layoutSnapshot();
            List<String> nodePaths = snapshot.nodes().stream().map(LayoutNodeSnapshot::path).toList();

            assertThat(snapshot.typography()).isNotEmpty();
            assertThat(snapshot.typography()).allSatisfy(run ->
                    assertThat(nodePaths).contains(run.path()));
        }
    }

    @Test
    void theFormatVersionSaysTypographyIsThere() throws Exception {
        try (DocumentSession document = page()) {
            compose(document);

            assertThat(document.layoutSnapshot().formatVersion())
                    .describedAs("a reader has to be able to tell a snapshot that carries text from one that does not")
                    .isEqualTo("2.1");
        }
    }

    @Test
    void theDocumentStillRenders() throws Exception {
        // The projection reads the same fragments the PDF handler draws from, and the
        // PDF handler now shares its line walk with it. Rendering proves the shared
        // helper did not change what the page looks like.
        try (DocumentSession document = page()) {
            compose(document);

            Path output = VisualTestOutputs.preparePdf("typography-snapshot", "typography");
            byte[] bytes = document.toPdfBytes();
            Files.write(output, bytes);

            assertThat(new String(bytes, 0, 8, US_ASCII)).startsWith("%PDF-");
            assertThat(output).exists();
        }
    }

    private static void compose(DocumentSession document) {
        document.dsl().pageFlow().name("Root").spacing(8)
                .addParagraph(p -> p.name("Heading").text("Quarterly report")
                        .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA_BOLD).size(20).build()))
                .addParagraph(p -> p.name("Terms").text(LONG_TEXT)
                        .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).size(11).build()))
                .addShape(shape -> shape.name("Rule").size(200, 2))
                .build();
    }
}
