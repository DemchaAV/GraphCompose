package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A line pair's shared line rises into the space above it, as a line of its own does, where
 * neither half is moved by its position.
 *
 * <p>A label and its value set to one line are one Word paragraph, each half seated by its own
 * position. Under half a point the position moves nothing, and the line stood where Word set it:
 * {@code ConsultingInvoice}'s bank details half a point low.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLinePairRaiseTest {

    /** What a line of the default text is raised into the space above it, in twips. */
    private static final long RAISE = DocxExports.DEFAULT_LINE_RAISE;
    private static final DocumentTextStyle SPECTRAL = DocumentTextStyle.builder().fontName(FontName.SPECTRAL).size(30).build();

    @Test
    void aPairOfHalvesBothSetLowRisesIntoTheSpaceAboveItAndOwesItBelow() throws Exception {
        try (Export export = export(page -> page
                .addParagraph(p -> p.name("Above").text("Above").margin(DocumentInsets.bottom(20)))
                .add(pair(new ParagraphBuilder().name("Label").text("Bank name:").build(),
                        new ParagraphBuilder().name("Value").text("Example Bank").build()))
                .addParagraph(p -> p.name("Below").text("Below")))) {
            XWPFParagraph pair = export.paragraph("Example Bank");
            XWPFParagraph below = export.paragraph("Below");
            double gapAbove = export.placed("Above").placementY()
                              - (export.placed("Label").placementY() + export.placed("Label").placementHeight());
            double gapBelow = export.placed("Label").placementY()
                              - (export.placed("Below").placementY() + export.placed("Below").placementHeight());

            assertThat(pair.getText()).as("the premise: one line").contains("Bank name:");
            assertThat(positions(pair)).as("neither half moved by its position").containsOnly(0);
            assertThat(after(export.paragraph("Above")) + before(pair))
                    .as("the page's gap, less the raise: the shared line moves up into it")
                    .isCloseTo(Math.round(gapAbove * 20) - RAISE, within(2L));
            assertThat(after(pair) + before(below))
                    .as("the page's gap, the line's raise owed back, less the next line's own")
                    .isCloseTo(Math.round(gapBelow * 20), within(2L));
        }
    }

    @Test
    void aPairWhoseHalvesItsPositionsMoveKeepsTheSpaceAboveIt() throws Exception {
        // Half a point and more the positions move: the line stays where it is.
        try (Export export = export(page -> page
                .addParagraph(p -> p.name("Above").text("Above").margin(DocumentInsets.bottom(20)))
                .add(pair(new ParagraphBuilder().name("Label").text("Label").textStyle(SPECTRAL).build(),
                        new ParagraphBuilder().name("Value").text("Value").textStyle(SPECTRAL).build())))) {
            XWPFParagraph pair = export.paragraph("Value");
            double gapAbove = export.placed("Above").placementY()
                              - (export.placed("Label").placementY() + export.placed("Label").placementHeight());

            assertThat(pair.getText()).as("the premise: one line").contains("Label");
            assertThat(positions(pair)).as("both halves moved by their positions").allMatch(position -> position > 0);
            assertThat(after(export.paragraph("Above")) + before(pair)).as("the page's gap, whole")
                    .isCloseTo(Math.round(gapAbove * 20), within(2L));
        }
    }

    @Test
    void aPairWithAHalfCutToItsOwnMeasureKeepsTheSpaceAboveIt() throws Exception {
        // The smaller half's line starts a little below the shared line's top: it is cut to its
        // own measure and seated by its position, under half a point as it is, and the line moved
        // for the other would move it too.
        try (Export export = export(page -> page
                .addParagraph(p -> p.name("Above").text("Above").margin(DocumentInsets.bottom(20)))
                .add(pair(new ParagraphBuilder().name("Label").text("Total").build(),
                        new ParagraphBuilder().name("Value").text("incl. VAT")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(13.5)).build())))) {
            XWPFParagraph pair = export.paragraph("Total");
            double gapAbove = export.placed("Above").placementY()
                              - (export.placed("Label").placementY() + export.placed("Label").placementHeight());

            assertThat(pair.getText()).as("the premise: one line").contains("incl. VAT");
            assertThat(positions(pair)).as("the premise: the cut half seated by its position, the other not")
                    .containsExactly(0, 1);
            assertThat(after(export.paragraph("Above")) + before(pair)).as("the page's gap, whole")
                    .isCloseTo(Math.round(gapAbove * 20), within(2L));
        }
    }

    @Test
    void aPairRisesByTheLesserOfWhatItsHalvesCallFor() throws Exception {
        // Set from the top of the line, a 6pt and a 6.5pt half are both uncut. The line is the 6.5pt
        // half's, where it calls for what a line of two of those does; the 6pt half, its baseline
        // higher in the same line, calls for more. The line moves by the lesser.
        long raised = raiseOf(DocumentTextStyle.DEFAULT.withSize(6), DocumentTextStyle.DEFAULT.withSize(6.5));
        long lesser = raiseOf(DocumentTextStyle.DEFAULT.withSize(6.5), DocumentTextStyle.DEFAULT.withSize(6.5));

        assertThat(lesser).as("the premise: a line of two 6.5pt halves rises").isPositive();
        assertThat(raised).isEqualTo(lesser);
    }

    /** How far a TOP aligned pair of these two styles is raised into the space above it, in twips. */
    private static long raiseOf(DocumentTextStyle left, DocumentTextStyle right) throws Exception {
        try (Export export = export(page -> page
                .addParagraph(p -> p.name("Above").text("Above").margin(DocumentInsets.bottom(20)))
                .add(new ShapeContainerBuilder().name("Pair").rectangle(360, 40)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .position(new ParagraphBuilder().name("Label").text("Label").textStyle(left).build(),
                                0, 0, LayerAlign.TOP_LEFT)
                        .position(new ParagraphBuilder().name("Value").text("Value").textStyle(right).build(),
                                0, 0, LayerAlign.TOP_RIGHT)
                        .build()))) {
            XWPFParagraph pair = export.paragraph("Value");
            double gapAbove = export.placed("Above").placementY()
                              - (export.placed("Pair").placementY() + export.placed("Pair").placementHeight());
            assertThat(pair.getText()).as("the premise: one line").contains("Label");
            assertThat(positions(pair)).as("the premise: neither half moved by its position").containsOnly(0);
            return Math.round(gapAbove * 20) - (after(export.paragraph("Above")) + before(pair));
        }
    }

    @Test
    void aPairOpeningAPageKeepsTheSpaceAboveIt() throws Exception {
        // Word drops the space above the first paragraph on a page: raised into it, the line would
        // stand where Word sets it, and what it owed would stand all below it lower.
        try (Export export = export(page -> page
                .spacer(0, 540)
                .add(pair(new ParagraphBuilder().name("Label").text("Bank name:").margin(DocumentInsets.top(10)).build(),
                        new ParagraphBuilder().name("Value").text("Example Bank").margin(DocumentInsets.top(10)).build()))
                .addParagraph(p -> p.name("Below").text("Below")))) {
            XWPFParagraph pair = export.paragraph("Example Bank");
            XWPFParagraph below = export.paragraph("Below");
            double gapBelow = export.placed("Label").placementY()
                              - (export.placed("Below").placementY() + export.placed("Below").placementHeight());

            assertThat(export.placed("Pair").startPage()).as("the premise: the pair opens the second page").isOne();
            assertThat(before(pair)).as("the premise: space above it, which Word drops").isPositive();
            assertThat(after(pair) + before(below)).as("the page's gap, less the next line's own raise: nothing owed")
                    .isCloseTo(Math.round(gapBelow * 20) - RAISE, within(2L));
        }
    }

    private static DocumentNode pair(DocumentNode left, DocumentNode right) {
        return new ShapeContainerBuilder().name("Pair").rectangle(360, 40)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(left, 0, 0, LayerAlign.CENTER_LEFT)
                .position(right, 0, 0, LayerAlign.CENTER_RIGHT)
                .build();
    }

    /** The position each run holding text is raised by, in half points. */
    private static List<Integer> positions(XWPFParagraph paragraph) {
        return paragraph.getRuns().stream().filter(run -> !run.text().isBlank()).map(DocxLinePairRaiseTest::position).toList();
    }

    private static int position(XWPFRun run) {
        var properties = run.getCTR().getRPr();
        return properties != null && properties.sizeOfPositionArray() > 0
                ? ((BigInteger) properties.getPositionArray(0).getVal()).intValue() : 0;
    }

    private static long before(XWPFParagraph paragraph) {
        var spacing = paragraph.getCTP().getPPr().getSpacing();
        return spacing.isSetBefore() ? ((BigInteger) spacing.getBefore()).longValue() : 0;
    }

    private static long after(XWPFParagraph paragraph) {
        var spacing = paragraph.getCTP().getPPr().getSpacing();
        return spacing.isSetAfter() ? ((BigInteger) spacing.getAfter()).longValue() : 0;
    }

    private static Export export(Consumer<PageFlowBuilder> content) throws Exception {
        DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create();
        session.pageFlow(content);
        byte[] bytes = session.export(new DocxSemanticBackend());
        return new Export(session, new XWPFDocument(new ByteArrayInputStream(bytes)));
    }

    private record Export(DocumentSession session, XWPFDocument document) implements AutoCloseable {

        XWPFParagraph paragraph(String text) {
            return document.getParagraphs().stream().filter(paragraph -> paragraph.getText().contains(text))
                    .findFirst().orElseThrow(() -> new AssertionError("no paragraph holds " + text));
        }

        PlacedNode placed(String name) {
            return session.layoutGraph().nodes().stream().filter(node -> name.equals(node.semanticName())).findFirst()
                    .orElseThrow(() -> new AssertionError("nothing placed is named " + name));
        }

        @Override
        public void close() throws Exception {
            document.close();
            session.close();
        }
    }
}
