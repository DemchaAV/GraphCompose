package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Text typed at the end of a document stands below a block written over the flow that hangs
 * below where the flow ends.
 *
 * <p>{@code LumaStudioInvoice}'s closing banner takes 0.3pt of the flow and draws 62.7pt below
 * it, under the invoice's last table. The paragraph closing the table stood at the banner's top,
 * and text typed at the end ran under the banner in Word and in LibreOffice. An empty paragraph
 * of exact height, as tall as the banner hangs below the flow's end, now stands before the
 * closing paragraph.</p>
 */
class DocxTypedBelowTheFlowTest {

    private static final double HANG = 120;

    @Test
    void anEmptyLineAsTallAsTheBlockHangsStandsBeforeTheClosingParagraph() throws Exception {
        java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content = page -> page
                .addTable(t -> t.autoColumns(2).row("Item", "Amount"))
                .add(banner(HANG, DocumentInsets.bottom(-HANG)));
        // How far the band hangs below the table, as the layout places both, to the twip above.
        long hang;
        java.util.concurrent.atomic.AtomicReference<DocxExportReport> report = new java.util.concurrent.atomic.AtomicReference<>();
        byte[] docx;
        try (com.demcha.compose.document.api.DocumentSession session = section(content)) {
            com.demcha.compose.document.layout.LayoutGraph graph = session.layoutGraph();
            int last = graph.totalPages() - 1;
            double band = lowest(graph, last, path -> path.contains("/BannerBand"));
            double flow = lowest(graph, last, path -> !path.contains("/Banner"));
            hang = (long) Math.ceil((flow - band) * 20);
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        assertThat(hang).as("the band hangs its height below the table and more").isGreaterThanOrEqualTo(Math.round(HANG * 20));
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
            List<IBodyElement> body = document.getBodyElements();
            assertThat(body.get(body.size() - 3)).as("the table").isInstanceOf(XWPFTable.class);
            XWPFParagraph spacer = (XWPFParagraph) body.get(body.size() - 2);
            assertThat(exactTwips(spacer)).as("an empty line as tall as the band hangs below the table")
                    .isEqualTo(hang);
            assertThat(spacer.getText()).isEmpty();
            XWPFParagraph closing = (XWPFParagraph) body.get(body.size() - 1);
            assertThat(closing.getCTP().getPPr()).as("and the closing paragraph an ordinary one").isNull();
        }
        assertThat(report.get().bySubject()).as("the closing paragraph is not named").doesNotContainKey("closing paragraph");
    }

    @Test
    void aBlockWhoseOwnBoxIsDrawnCountsThatBoxToo() throws Exception {
        // Filled, the container's own box is drawn where the layout places it, which its bottom
        // margin sets below its band: typed text must clear the box as well.
        DocumentNode block = filled(banner(HANG, DocumentInsets.bottom(-HANG)));
        long hang;
        try (com.demcha.compose.document.api.DocumentSession session = section(page -> page
                .addTable(t -> t.autoColumns(2).row("Item", "Amount")).add(block))) {
            com.demcha.compose.document.layout.LayoutGraph graph = session.layoutGraph();
            int last = graph.totalPages() - 1;
            double box = lowest(graph, last, path -> path.contains("/Banner"));
            double band = lowest(graph, last, path -> path.contains("/BannerBand"));
            double flow = lowest(graph, last, path -> !path.contains("/Banner"));
            assertThat(box).as("the layout sets the box below its band").isLessThan(band);
            hang = (long) Math.ceil((flow - box) * 20);
        }
        assertThat(spacerTwips(block)).as("an empty line as tall as the box hangs below the table").isEqualTo(hang);
    }

    @Test
    void aBlockOverTheFlowOnAnEarlierPageHoldsNoLineOnTheLast() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .addTable(t -> t.autoColumns(2).row("Item", "Amount"))
                .add(banner(HANG, DocumentInsets.bottom(-HANG)))
                .addPageBreak(pageBreak -> { })
                .addTable(t -> t.autoColumns(1).row("Total")))) {
            assertThat(document.getDocument().getBody().xmlText()).as("the block is written over the flow")
                    .contains("<w:txbxContent>");
            List<IBodyElement> body = document.getBodyElements();
            assertThat(body.get(body.size() - 2)).as("the last table, then the closing paragraph")
                    .isInstanceOf(XWPFTable.class);
        }
    }

    @Test
    void anExportWithNoLayoutHoldsNoLine() throws Exception {
        // A guard rather than a reach of the rule: with no layout nothing is written over the flow
        // and there is no room to measure, so the closing paragraph is the point-tall one.
        try (XWPFDocument document = DocxExports.withoutLayout(400, 600, 40, page -> page
                .addTable(t -> t.autoColumns(2).row("Item", "Amount"))
                .add(banner(HANG, DocumentInsets.bottom(-HANG))))) {
            List<IBodyElement> body = document.getBodyElements();
            assertThat(body.get(body.size() - 2)).as("the table, then the closing paragraph")
                    .isInstanceOf(XWPFTable.class);
        }
    }

    @Test
    void withNothingWrittenOverTheFlowTheClosingParagraphFollowsTheTable() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .addTable(t -> t.autoColumns(2).row("Item", "Amount")))) {
            List<IBodyElement> body = document.getBodyElements();
            assertThat(body.get(body.size() - 2)).as("the table, then").isInstanceOf(XWPFTable.class);
            assertThat(((XWPFParagraph) body.get(body.size() - 1)).getCTP().getPPr())
                    .as("the ordinary closing paragraph").isNull();
        }
    }

    @Test
    void aBlockOverTheFlowThatEndsAboveTheTableHoldsNoLineBelowIt() throws Exception {
        // Pulled up over the page's top and handing its height back below, as LumaStudio's
        // sidebar is: it ends above the table, so typing below the table runs under nothing.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .add(banner(HANG, new DocumentInsets(-40, 0, 40 - HANG, 0)))
                .addTable(t -> t.autoColumns(1).row("Row 1").row("Row 2").row("Row 3").row("Row 4")
                        .row("Row 5").row("Row 6").row("Row 7").row("Row 8")))) {
            assertThat(document.getDocument().getBody().xmlText()).as("the block is written over the flow")
                    .contains("<w:txbxContent>");
            List<IBodyElement> body = document.getBodyElements();
            assertThat(body.get(body.size() - 2)).as("the table, then the closing paragraph")
                    .isInstanceOf(XWPFTable.class);
        }
    }

    @Test
    void whereNoLineFitsUnderTheBlockTheClosingParagraphIsTheHairlineAndNothingMore() throws Exception {
        // The banner hangs to within a few points of the page's foot: no room for a line under
        // it, so the paragraph after the table is a point tall, as it is under a table reaching
        // the foot, and no line is written for the banner.
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .addTable(t -> t.autoColumns(2).row("Item", "Amount"))
                .add(banner(490, DocumentInsets.bottom(-490))))) {
            List<IBodyElement> body = document.getBodyElements();
            assertThat(body.get(body.size() - 2)).as("the table, then the closing paragraph")
                    .isInstanceOf(XWPFTable.class);
            assertThat(exactTwips((XWPFParagraph) body.get(body.size() - 1))).as("a point tall")
                    .isEqualTo(20);
        }
    }

    @Test
    void aSectionEndingWithAParagraphGetsNoLine() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .addTable(t -> t.autoColumns(2).row("Item", "Amount"))
                .add(banner(HANG, DocumentInsets.bottom(-HANG)))
                .addParagraph("Signed"))) {
            assertThat(document.getDocument().getBody().xmlText()).as("the block is written over the flow")
                    .contains("<w:txbxContent>");
            List<IBodyElement> body = document.getBodyElements();
            XWPFParagraph last = (XWPFParagraph) body.get(body.size() - 1);
            // Its text, after the banner's text box anchored in it.
            assertThat(last.getText()).as("the document ends on its own paragraph").endsWith("Signed");
            assertThat(body.stream().filter(e -> e instanceof XWPFParagraph p && p.getText().isEmpty()
                    && exactTwips(p) > 1000).count()).as("with no empty line written for the banner").isZero();
        }
    }

    @Test
    void whatOneSectionWroteOverTheFlowIsNotCountedInTheNext() throws Exception {
        // The second section holds a block of the same name and place that the page gives 20pt of
        // room: not written over the flow, so its band, hanging below the table after it, is the
        // page's own drawing and the section closes as any other.
        byte[] docx;
        try (com.demcha.compose.document.api.MultiSectionDocument sections = com.demcha.compose.GraphCompose.documents()
                .section(section(page -> page
                        .addTable(t -> t.autoColumns(2).row("Item", "Amount"))
                        .add(banner(HANG, DocumentInsets.bottom(-HANG)))
                        .addTable(t -> t.autoColumns(1).row("Total"))))
                .section(section(page -> page
                        .addTable(t -> t.autoColumns(2).row("Item", "Amount"))
                        .add(banner(HANG, DocumentInsets.bottom(20 - HANG)))
                        .addTable(t -> t.autoColumns(1).row("Total"))))
                .create()) {
            docx = sections.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
            List<IBodyElement> body = document.getBodyElements();
            assertThat(body.get(body.size() - 2)).as("the second section's table, then its closing paragraph")
                    .isInstanceOf(XWPFTable.class);
            List<IBodyElement> lines = body.stream().filter(e -> e instanceof XWPFParagraph p && p.getText().isEmpty()
                    && exactTwips(p) > 1000).toList();
            assertThat(lines).as("while the first section holds its line").hasSize(1);
            XWPFParagraph line = (XWPFParagraph) lines.get(0);
            XWPFParagraph closing = (XWPFParagraph) body.get(body.indexOf(line) + 1);
            assertThat(line.getCTP().getPPr().isSetSectPr()).as("the line does not end the section").isFalse();
            assertThat(closing.getCTP().getPPr() != null && closing.getCTP().getPPr().isSetSectPr())
                    .as("the closing paragraph after it does").isTrue();
        }
    }

    /** The height of the empty line an export of the block after a table writes, in twips. */
    private static long spacerTwips(DocumentNode block) throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 40, page -> page
                .addTable(t -> t.autoColumns(2).row("Item", "Amount"))
                .add(block))) {
            List<IBodyElement> body = document.getBodyElements();
            return exactTwips((XWPFParagraph) body.get(body.size() - 2));
        }
    }

    /** The bottom of the lowest fragment on a page whose path a test accepts, up from the foot. */
    private static double lowest(com.demcha.compose.document.layout.LayoutGraph graph, int page,
                                 java.util.function.Predicate<String> counted) {
        return graph.fragments().stream()
                .filter(f -> f.pageIndex() == page && f.path() != null && !f.path().startsWith("@")
                             && counted.test(f.path()))
                .mapToDouble(com.demcha.compose.document.layout.PlacedFragment::y).min().orElseThrow();
    }

    private static DocumentNode filled(DocumentNode banner) {
        var container = (com.demcha.compose.document.node.ShapeContainerNode) banner;
        return new ShapeContainerBuilder().name("Banner")
                .rectangle(300, HANG).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(container.margin())
                .fillColor(DocumentColor.rgb(200, 200, 200))
                .position(new ShapeBuilder().name("BannerBand").size(300, HANG)
                        .fillColor(DocumentColor.rgb(46, 46, 46)).build(), 0, 0, LayerAlign.TOP_LEFT, 0)
                .position(new ParagraphBuilder().name("SignOff").text("Thank you").build(),
                        10, 10, LayerAlign.TOP_LEFT, 1)
                .build();
    }

    private static com.demcha.compose.document.api.DocumentSession section(
            java.util.function.Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content) {
        com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(40))
                .create();
        session.pageFlow(content);
        return session;
    }

    /**
     * A band and a line of text over it, the band {@code height} tall, in a container whose margins
     * take back what the page would give it: written over the flow.
     */
    private static DocumentNode banner(double height, DocumentInsets margin) {
        return new ShapeContainerBuilder().name("Banner")
                .rectangle(300, height).clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(margin)
                .position(new ShapeBuilder().name("BannerBand").size(300, height)
                        .fillColor(DocumentColor.rgb(46, 46, 46)).build(), 0, 0, LayerAlign.TOP_LEFT, 0)
                .position(new ParagraphBuilder().name("SignOff").text("Thank you").build(),
                        10, 10, LayerAlign.TOP_LEFT, 1)
                .build();
    }

    /** The paragraph's exact line height in twips, or -1 where its lines are not exact. */
    private static long exactTwips(XWPFParagraph paragraph) {
        var properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing()) {
            return -1;
        }
        CTSpacing spacing = properties.getSpacing();
        return spacing.isSetLineRule() && spacing.getLineRule() == STLineSpacingRule.EXACT
                ? DocxTwips.of(spacing.getLine()) : -1;
    }
}
