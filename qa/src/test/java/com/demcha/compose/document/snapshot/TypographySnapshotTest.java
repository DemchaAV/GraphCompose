package com.demcha.compose.document.snapshot;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.compose.testing.layout.LayoutSnapshotJson;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
 * <p>Three properties carry most of the weight here.</p>
 *
 * <p><b>It is opt-in.</b> A snapshot taken the ordinary way must be byte-identical to the
 * one baselines were recorded against, or every consumer's suite goes red on an upgrade
 * that changed nothing about their document.</p>
 *
 * <p><b>Declared versus resolved, including the face.</b> A style may name a font the
 * document is not set in, and the rewrites that cause it are silent. The family alone
 * does not settle it either: the face comes from the decoration, so the wrong document
 * and the right one are only distinguishable once both are reported.</p>
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

    private static final LayoutSnapshotOptions WITH_TYPOGRAPHY =
            LayoutSnapshotOptions.builder().typography(true).build();

    private static DocumentSession page() {
        return GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(36, 36, 36, 36)
                .create();
    }

    private static DocumentTextStyle style(FontName font, double size, DocumentTextDecoration decoration) {
        return DocumentTextStyle.builder().fontName(font).size(size).decoration(decoration).build();
    }

    /** Lays out {@code LONG_TEXT} as a single named paragraph and returns its run. */
    private static LayoutTypographySnapshot terms(DocumentSession document) {
        document.dsl().pageFlow().name("Root")
                .addParagraph(p -> p.name("Terms").text(LONG_TEXT)
                        .textStyle(style(FontName.HELVETICA, 11, DocumentTextDecoration.DEFAULT)))
                .build();
        return only(document.layoutSnapshot(WITH_TYPOGRAPHY), "Terms");
    }

    private static LayoutTypographySnapshot only(LayoutDiagnosticSnapshot snapshot, String name) {
        List<LayoutTypographySnapshot> matches = snapshot.typography().stream()
                .filter(run -> run.path().contains(name))
                .toList();
        assertThat(matches)
                .describedAs("exactly one text run named %s", name)
                .hasSize(1);
        return matches.get(0);
    }

    // ------------------------------------------------------------------- opt-in ---

    @Test
    void theDefaultSnapshotShowsNoTraceOfDiagnostics() throws Exception {
        // The whole backward-compatibility contract. LayoutSnapshot is what consumers
        // hold baselines of, so it must carry no diagnostic field at all — not an empty
        // one. toString() is the cheapest proof that the record's own shape is clean,
        // which is what a hand-rolled ObjectMapper would serialize too.
        try (DocumentSession document = page()) {
            compose(document);

            LayoutSnapshot snapshot = document.layoutSnapshot();

            assertThat(snapshot.formatVersion()).isEqualTo("2.0");
            assertThat(snapshot.toString())
                    .describedAs("a diagnostic field would leak into every consumer's serializer")
                    .doesNotContain("typography");
            assertThat(LayoutSnapshotJson.toJson(snapshot)).doesNotContain("typography");
        }
    }

    @Test
    void askingForTypographyAddsTheSectionWithoutTouchingTheLayoutSnapshot() throws Exception {
        try (DocumentSession document = page()) {
            compose(document);

            LayoutSnapshot plain = document.layoutSnapshot();
            LayoutDiagnosticSnapshot rich = document.layoutSnapshot(WITH_TYPOGRAPHY);

            assertThat(rich.typography()).isNotEmpty();
            assertThat(rich.formatVersion())
                    .describedAs("the envelope versions itself, not the layout snapshot")
                    .isEqualTo("1.0");
            assertThat(rich.layout())
                    .describedAs("the nested snapshot is the plain one, whole and unchanged")
                    .isEqualTo(plain);
            assertThat(LayoutSnapshotJson.toJson(rich.layout()))
                    .isEqualTo(LayoutSnapshotJson.toJson(plain));
        }
    }

    @Test
    void notAskingForASectionLeavesItEmpty() throws Exception {
        // Options are per-section: an envelope requested with everything off still wraps
        // the same layout snapshot and reports nothing.
        try (DocumentSession document = page()) {
            compose(document);

            LayoutDiagnosticSnapshot empty = document.layoutSnapshot(LayoutSnapshotOptions.defaults());

            assertThat(empty.typography()).isEmpty();
            assertThat(empty.layout()).isEqualTo(document.layoutSnapshot());
        }
    }

    @Test
    void takingARichSnapshotDoesNotPoisonTheCachedDefaultOne() throws Exception {
        // The default snapshot is memoized per layout revision. If the diagnostic call
        // shared that slot, one diagnostic pass would change what every later caller
        // sees — including the assertion helper comparing against a committed baseline.
        try (DocumentSession document = page()) {
            compose(document);
            LayoutSnapshot before = document.layoutSnapshot();

            document.layoutSnapshot(WITH_TYPOGRAPHY);

            assertThat(document.layoutSnapshot()).isEqualTo(before);
            assertThat(document.layoutSnapshot().formatVersion()).isEqualTo("2.0");
        }
    }

    // ------------------------------------------------------------- what it reports ---

    @Test
    void aParagraphReportsTheFontItWasSetIn() throws Exception {
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Body").text("Handgloves 0123")
                            .textStyle(style(FontName.LATO, 18, DocumentTextDecoration.DEFAULT)))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(WITH_TYPOGRAPHY), "Body");

            assertThat(run.declaredFont()).isEqualTo(FontName.LATO.name());
            assertThat(run.resolvedFamily()).isEqualTo(FontName.LATO.name());
            assertThat(run.decoration()).isEqualTo("DEFAULT");
            assertThat(run.fontSubstituted()).isFalse();
            assertThat(run.fontSize()).isEqualTo(18.0);
            assertThat(run.lineCount()).isEqualTo(1);
            assertThat(run.lines()).hasSize(1);
        }
    }

    @ParameterizedTest(name = "{0} is reported as {1}")
    @CsvSource({"DEFAULT,DEFAULT", "BOLD,BOLD", "ITALIC,ITALIC", "BOLD_ITALIC,BOLD_ITALIC"})
    void theDecorationThatPicksTheFaceIsReported(String declared, String expected) throws Exception {
        // Without this field Helvetica+DEFAULT and Helvetica+BOLD are the same two
        // strings in the snapshot, while the page measures and draws two different faces.
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Face").text("Handgloves")
                            .textStyle(style(FontName.HELVETICA, 12, DocumentTextDecoration.valueOf(declared))))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(WITH_TYPOGRAPHY), "Face");

            assertThat(run.decoration()).isEqualTo(expected);
            assertThat(run.resolvedFamily()).isEqualTo(FontName.HELVETICA.name());
            assertThat(run.fontSubstituted())
                    .describedAs("asking a family for a face it has is not a substitution")
                    .isFalse();
        }
    }

    @Test
    void namingABoldFaceWithoutTheDecorationIsReportedAsASubstitution() throws Exception {
        // The expensive case. HELVETICA_BOLD is an alias of its family and contributes
        // nothing on its own, so this document is set in regular Helvetica — it renders,
        // it measures, and nothing about the output says the bold never arrived.
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Heading").text("Quarterly report")
                            .textStyle(style(FontName.HELVETICA_BOLD, 20, DocumentTextDecoration.DEFAULT)))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(WITH_TYPOGRAPHY), "Heading");

            assertThat(run.declaredFont()).isEqualTo(FontName.HELVETICA_BOLD.name());
            assertThat(run.resolvedFamily()).isEqualTo(FontName.HELVETICA.name());
            assertThat(run.decoration()).isEqualTo("DEFAULT");
            assertThat(run.fontSubstituted())
                    .describedAs("a document set in a font its author did not name must say so")
                    .isTrue();
        }
    }

    @Test
    void namingABoldFaceAndAskingForBoldIsNotASubstitution() throws Exception {
        // The other half, and the reason substitution cannot be decided from the family
        // alone: the face this style named is the face the page draws, so flagging it
        // would cry wolf on a correct document.
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Heading").text("Quarterly report")
                            .textStyle(style(FontName.HELVETICA_BOLD, 20, DocumentTextDecoration.BOLD)))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(WITH_TYPOGRAPHY), "Heading");

            assertThat(run.declaredFont()).isEqualTo(FontName.HELVETICA_BOLD.name());
            assertThat(run.resolvedFamily()).isEqualTo(FontName.HELVETICA.name());
            assertThat(run.decoration()).isEqualTo("BOLD");
            assertThat(run.fontSubstituted())
                    .describedAs("the named face is the face drawn — nothing was substituted")
                    .isFalse();
        }
    }

    static Stream<Arguments> faceAliasCases() {
        return Stream.of(
                Arguments.of(FontName.TIMES_BOLD, DocumentTextDecoration.BOLD, false),
                Arguments.of(FontName.TIMES_BOLD, DocumentTextDecoration.DEFAULT, true),
                Arguments.of(FontName.TIMES_ITALIC, DocumentTextDecoration.ITALIC, false),
                Arguments.of(FontName.TIMES_ITALIC, DocumentTextDecoration.BOLD, true),
                Arguments.of(FontName.COURIER_BOLD_OBLIQUE, DocumentTextDecoration.BOLD_ITALIC, false),
                Arguments.of(FontName.COURIER_BOLD_OBLIQUE, DocumentTextDecoration.BOLD, true));
    }

    @ParameterizedTest(name = "{0} + {1} substituted={2}")
    @MethodSource("faceAliasCases")
    void everyFaceAliasIsJudgedAgainstTheDecorationThatWouldRecoverIt(FontName font,
                                                                      DocumentTextDecoration decoration,
                                                                      boolean substituted) throws Exception {
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Alias").text("Handgloves")
                            .textStyle(style(font, 12, decoration)))
                    .build();

            assertThat(only(document.layoutSnapshot(WITH_TYPOGRAPHY), "Alias").fontSubstituted())
                    .isEqualTo(substituted);
        }
    }

    @Test
    void namingNoFontIsReportedAsASubstitutionToo() throws Exception {
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Plain").text("No font named")
                            .textStyle(style(FontName.DEFAULT, 12, DocumentTextDecoration.DEFAULT)))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(WITH_TYPOGRAPHY), "Plain");

            assertThat(run.declaredFont()).isEqualTo(FontName.DEFAULT.name());
            assertThat(run.resolvedFamily()).isEqualTo(FontName.HELVETICA.name());
            assertThat(run.fontSubstituted()).isTrue();
        }
    }

    @Test
    void anAutoSizedParagraphReportsTheSizeItWasActuallyLaidOutAt() throws Exception {
        // autoSize shrinks the text to fit and the engine measures the shrunk style.
        // Reporting the declared size beside line boxes measured at another one would
        // make the record contradict itself, and would answer "why is this block this
        // size" with the one number that is wrong.
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Shrunk")
                            .text("A headline far too wide to fit on one line at its declared size")
                            .autoSize(40.0, 6.0)
                            .textStyle(style(FontName.HELVETICA, 40, DocumentTextDecoration.DEFAULT)))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(WITH_TYPOGRAPHY), "Shrunk");

            assertThat(run.fontSize())
                    .describedAs("the reported size is the one the lines were measured at")
                    .isLessThan(40.0);
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

            assertThat(document.layoutSnapshot(WITH_TYPOGRAPHY).typography()).isEmpty();
        }
    }

    // ------------------------------------------------------------------ the lines ---

    @Test
    void wrappedTextReportsEveryLineItBrokeInto() throws Exception {
        try (DocumentSession document = page()) {
            LayoutTypographySnapshot run = terms(document);

            assertThat(run.lineCount()).isGreaterThan(1);
            assertThat(run.lines()).hasSize(run.lineCount());
            assertThat(run.lines()).extracting(LayoutTextLineSnapshot::index)
                    .containsExactlyElementsOf(IntStream.range(0, run.lineCount()).boxed().toList());
            assertThat(run.lines()).allSatisfy(line -> {
                assertThat(line.width()).isPositive();
                assertThat(line.height()).isPositive();
            });
        }
    }

    @Test
    void linesStackDownTheStackTheRendererWalks() throws Exception {
        try (DocumentSession document = page()) {
            List<LayoutTextLineSnapshot> lines = terms(document).lines();

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
            assertThat(terms(document).lines()).allSatisfy(line -> {
                assertThat(line.baseline()).isBetween(line.y(), line.y() + line.height());
                assertThat(line.baselineExact())
                        .describedAs("default vertical seating needs no backend font, so it is exact")
                        .isTrue();
            });
        }
    }

    @ParameterizedTest(name = "{0} seating reports baselineExact={1}")
    @CsvSource({"DEFAULT,true", "TOP,false", "CENTER,false", "BOTTOM,false"})
    void onlyDefaultSeatingYieldsAnExactBaseline(String align, boolean exact) throws Exception {
        // A non-default seating shifts the drawn baseline by a correction read from the
        // backend font's cap height, which nothing renderer-neutral can compute. Without
        // this case the false branch is unreachable and the flag could be hardcoded true.
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Seated").text("Handgloves")
                            .verticalAlign(TextVerticalAlign.valueOf(align))
                            .textStyle(style(FontName.HELVETICA, 12, DocumentTextDecoration.DEFAULT)))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(WITH_TYPOGRAPHY), "Seated");

            assertThat(run.verticalAlign()).isEqualTo(align);
            assertThat(run.lines()).isNotEmpty();
            assertThat(run.lines()).allSatisfy(line ->
                    assertThat(line.baselineExact()).isEqualTo(exact));
        }
    }

    @Test
    void alignmentMovesTheLinesTheWayThePageShowsThem() throws Exception {
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Left").text("Aligned")
                            .align(TextAlign.LEFT)
                            .textStyle(style(FontName.HELVETICA, 12, DocumentTextDecoration.DEFAULT)))
                    .addParagraph(p -> p.name("Right").text("Aligned")
                            .align(TextAlign.RIGHT)
                            .textStyle(style(FontName.HELVETICA, 12, DocumentTextDecoration.DEFAULT)))
                    .build();

            LayoutDiagnosticSnapshot snapshot = document.layoutSnapshot(WITH_TYPOGRAPHY);
            LayoutTextLineSnapshot left = only(snapshot, "Left").lines().get(0);
            LayoutTextLineSnapshot right = only(snapshot, "Right").lines().get(0);

            assertThat(right.x())
                    .describedAs("the same words aligned right start further along the page")
                    .isGreaterThan(left.x());
            assertThat(right.width()).isEqualTo(left.width());
        }
    }

    @Test
    void theTextBoxCoversTheLinesItReportsExactly() throws Exception {
        try (DocumentSession document = page()) {
            LayoutTypographySnapshot run = terms(document);

            // No epsilon on purpose. The box is derived from the same rounded line values
            // it is compared against, so the containment holds exactly; a tolerance here
            // would hide the rounding drift that made it not.
            for (LayoutTextLineSnapshot line : run.lines()) {
                assertThat(line.x())
                        .describedAs("line %d starts at or after the text box left", line.index())
                        .isGreaterThanOrEqualTo(run.textX());
                assertThat(line.x() + line.width())
                        .describedAs("line %d ends at or before the text box right", line.index())
                        .isLessThanOrEqualTo(run.textX() + run.textWidth());
                assertThat(line.y())
                        .isGreaterThanOrEqualTo(run.textY());
                assertThat(line.y() + line.height())
                        .describedAs("line %d sits at or below the text box top", line.index())
                        .isLessThanOrEqualTo(run.textY() + run.textHeight());
            }
        }
    }

    @Test
    void aSingleLineRunsBoxIsExactlyItsLine() throws Exception {
        // Deriving the box from a parallel accumulation of raw doubles let
        // (x + width) - x round a thousandth away from width, so a one-line run reported
        // a box its own only line escaped.
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("One").text("Handgloves 0123")
                            .align(TextAlign.RIGHT)
                            .textStyle(style(FontName.HELVETICA, 11, DocumentTextDecoration.DEFAULT)))
                    .build();

            LayoutTypographySnapshot run = only(document.layoutSnapshot(WITH_TYPOGRAPHY), "One");
            LayoutTextLineSnapshot line = run.lines().get(0);

            assertThat(run.textX()).isEqualTo(line.x());
            assertThat(run.textWidth()).isEqualTo(line.width());
            assertThat(run.textY()).isEqualTo(line.y());
            assertThat(run.textHeight()).isEqualTo(line.height());
        }
    }

    // -------------------------------------------------------------------- paging ---

    @Test
    void aParagraphSplitAcrossPagesReportsOneRunPerPage() throws Exception {
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Long").text(LONG_TEXT.repeat(40))
                            .textStyle(style(FontName.HELVETICA, 12, DocumentTextDecoration.DEFAULT)))
                    .build();

            List<LayoutTypographySnapshot> runs = document.layoutSnapshot(WITH_TYPOGRAPHY).typography().stream()
                    .filter(run -> run.path().contains("Long"))
                    .toList();

            assertThat(runs).hasSizeGreaterThan(1);
            assertThat(runs).extracting(LayoutTypographySnapshot::path).containsOnly(runs.get(0).path());
            assertThat(runs).extracting(LayoutTypographySnapshot::page)
                    .describedAs("one run per page, in page order")
                    .isSorted();
            assertThat(runs).extracting(LayoutTypographySnapshot::page).doesNotHaveDuplicates();
            assertThat(runs).allSatisfy(run -> {
                assertThat(run.lineCount()).isPositive();
                assertThat(run.lines()).hasSize(run.lineCount());
            });
        }
    }

    @Test
    void aSplitParagraphIsOrderedByPageNotByEmissionOrder() throws Exception {
        // Every page-slice of a split paragraph carries fragmentIndex 0, so (path,
        // fragmentIndex) is not a unique key: without page in the sort the tie falls back
        // to the order pagination happened to emit the fragments in.
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Long").text(LONG_TEXT.repeat(40))
                            .textStyle(style(FontName.HELVETICA, 12, DocumentTextDecoration.DEFAULT)))
                    .build();

            List<LayoutTypographySnapshot> runs = document.layoutSnapshot(WITH_TYPOGRAPHY).typography();

            assertThat(runs).isSortedAccordingTo(
                    Comparator.comparing(LayoutTypographySnapshot::path)
                            .thenComparingInt(LayoutTypographySnapshot::page)
                            .thenComparingInt(LayoutTypographySnapshot::fragmentIndex));
        }
    }

    // ------------------------------------------------------------ stability + shape ---

    @Test
    void theSameDocumentProducesTheSameTypographyTwice() throws Exception {
        String first;
        String second;
        try (DocumentSession document = page()) {
            compose(document);
            first = document.layoutSnapshot(WITH_TYPOGRAPHY).typography().toString();
        }
        try (DocumentSession document = page()) {
            compose(document);
            second = document.layoutSnapshot(WITH_TYPOGRAPHY).typography().toString();
        }

        assertThat(second).isEqualTo(first);
    }

    @Test
    void orderingDoesNotDependOnTheOrderFragmentsWereEmittedIn() throws Exception {
        // The fixture is named so that emission order and sorted order disagree: a
        // document composed Zulu, Alpha must come back Alpha, Zulu. A test whose fixture
        // is already in sorted order cannot tell a sort from no sort at all.
        try (DocumentSession document = page()) {
            document.dsl().pageFlow().name("Root")
                    .addParagraph(p -> p.name("Zulu").text("last by name, first on the page")
                            .textStyle(style(FontName.HELVETICA, 11, DocumentTextDecoration.DEFAULT)))
                    .addParagraph(p -> p.name("Alpha").text("first by name, last on the page")
                            .textStyle(style(FontName.HELVETICA, 11, DocumentTextDecoration.DEFAULT)))
                    .build();

            List<String> paths = document.layoutSnapshot(WITH_TYPOGRAPHY).typography().stream()
                    .map(LayoutTypographySnapshot::path)
                    .toList();

            assertThat(paths).isSorted();
            assertThat(paths.get(0)).contains("Alpha");
        }
    }

    @Test
    void everyRunPointsAtANodeTheSnapshotAlsoReports() throws Exception {
        // Typography is a parallel list joined on path. A run naming a node that is not
        // in the node list could not be located by any consumer.
        try (DocumentSession document = page()) {
            compose(document);

            LayoutDiagnosticSnapshot snapshot = document.layoutSnapshot(WITH_TYPOGRAPHY);
            List<String> nodePaths = snapshot.layout().nodes().stream()
                    .map(LayoutNodeSnapshot::path).toList();

            assertThat(snapshot.typography()).isNotEmpty();
            assertThat(snapshot.typography()).allSatisfy(run ->
                    assertThat(nodePaths).contains(run.path()));
        }
    }

    @Test
    void theDocumentStillRenders() throws Exception {
        // The projection reads the same fragments the PDF handler draws from, and the
        // PDF handler now shares its line walk with it. Rendering proves the shared
        // helper still produces a page.
        try (DocumentSession document = page()) {
            compose(document);

            Path output = VisualTestOutputs.preparePdf("typography-snapshot", "typography");
            byte[] bytes = document.toPdfBytes();
            Files.write(output, bytes);

            assertThat(bytes.length).isGreaterThan(8);
            assertThat(new String(bytes, 0, 8, US_ASCII)).startsWith("%PDF-");
            assertThat(output).exists();
        }
    }

    private static void compose(DocumentSession document) {
        document.dsl().pageFlow().name("Root").spacing(8)
                .addParagraph(p -> p.name("Heading").text("Quarterly report")
                        .textStyle(style(FontName.HELVETICA_BOLD, 20, DocumentTextDecoration.BOLD)))
                .addParagraph(p -> p.name("Terms").text(LONG_TEXT)
                        .textStyle(style(FontName.HELVETICA, 11, DocumentTextDecoration.DEFAULT)))
                .addShape(shape -> shape.name("Rule").size(200, 2))
                .build();
    }
}
