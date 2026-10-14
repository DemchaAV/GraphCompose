package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.AbstractFlowBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.InlineImageAlignment;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * What paragraphs' exact lines are off the page by, rounded to the twip, is carried from one to
 * the next and settled once it comes to more than one paragraph's own may.
 *
 * <p>The default face at 9pt sets a line 8.325pt tall on the page, written 8.3: a fortieth of a
 * point short, too little to owe below any one paragraph. Twenty-eight of them, each settled on
 * its own, wrote the last line 0.7pt higher than the page sets it.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLinesRoundingCarryTest {

    /** The default face at 9pt: a line written a fortieth of a point shorter than the page's. */
    private static final DocumentTextStyle SHORT = DocumentTextStyle.DEFAULT.withSize(9);
    /** What a {@link #SHORT} line is written short of the page's, in points. */
    private static final double SHORT_BY = 8.325 - 8.3;
    /** The default face at 7pt: a line written a fortieth of a point taller than the page's. */
    private static final DocumentTextStyle TALL = DocumentTextStyle.DEFAULT.withSize(7);
    /**
     * The space between the lines, in points: each line after the first, Word setting it low, is
     * raised into it, the line under it rising as far into what it owes, so the lines measured
     * stand alike.
     */
    private static final double GAP = 4;
    /** What may stay carried, unsettled, at the first line measured and at the last. */
    private static final double CARRIED = 2 * 0.1;
    /**
     * Where the second of four short lines stands from the fourth, nothing carried into them: two
     * lines' worth, too little to owe, so Word sets the fourth that much higher than the page.
     * Three fortieths of a point carried in, the second settles what the three come to.
     */
    private static final double TWO_SHORT = -2 * SHORT_BY;

    @Test
    void linesWrittenShortOweWhatTheyFallShortTogether() throws Exception {
        try (Export export = export(page -> lines(page, "Line", 30, SHORT))) {
            assertThat(lineOf(export.paragraph("Line 1"))).as("the premise: written short of the page's")
                    .isLessThan(export.placed("Line 1").placementHeight());
            assertThat(wordDrift(export, "Line 1", "Line 29"))
                    .as("the last line where the page sets it, to within what is still carried")
                    .isCloseTo(0, within(CARRIED));
        }
    }

    @Test
    void linesWrittenTallGiveWhatTheyPassTogetherOutOfTheSpaceBetweenThem() throws Exception {
        try (Export export = export(page -> lines(page, "Line", 30, TALL))) {
            assertThat(lineOf(export.paragraph("Line 1"))).as("the premise: written taller than the page's")
                    .isGreaterThan(export.placed("Line 1").placementHeight());
            assertThat(wordDrift(export, "Line 1", "Line 29"))
                    .as("the last line where the page sets it, to within what is still carried")
                    .isCloseTo(0, within(CARRIED));
        }
    }

    @Test
    void aCellCarriesNothingOutOfIt() throws Exception {
        // Three short lines in each column come to three fortieths of a point there.
        try (Export export = export(page -> {
            page.addRow(row -> row.addSection(left -> lines(left, "Left", 3, SHORT))
                    .addSection(right -> lines(right, "Right", 3, SHORT)));
            lines(page, "After", 4, SHORT);
        })) {
            assertThat(wordDrift(export, "After 1", "After 3")).as("the lines after the row, nothing carried into them")
                    .isCloseTo(TWO_SHORT, within(0.01));
        }
    }

    @Test
    void aCellStartsCarryingFromNothing() throws Exception {
        // Three short lines above the row come to three fortieths of a point.
        try (Export export = export(page -> {
            lines(page, "Before", 3, SHORT);
            page.addRow(row -> row.addSection(left -> lines(left, "Left", 4, SHORT))
                    .addSection(right -> lines(right, "Right", 4, SHORT)));
        })) {
            assertThat(wordDrift(export, "Left 1", "Left 3")).as("the lines in the column, nothing carried into them")
                    .isCloseTo(TWO_SHORT, within(0.01));
        }
    }

    @Test
    void aBandCarriesNothingOutOfIt() throws Exception {
        // Three short lines laid over a backdrop come to three fortieths of a point in the band.
        SectionBuilder layer = new SectionBuilder().name("Layer");
        lines(layer, "In", 3, SHORT);
        try (Export export = export(page -> {
            page.addLayerStack(stack -> stack.name("Band")
                    .back(new ShapeBuilder().name("Backdrop").size(200, 60).build())
                    .layer(layer.build(), LayerAlign.TOP_LEFT));
            lines(page, "Under", 4, SHORT);
        })) {
            assertThat(wordDrift(export, "Under 1", "Under 3")).as("the lines under the band, nothing carried into them")
                    .isCloseTo(TWO_SHORT, within(0.01));
        }
    }

    @Test
    void aPageStartsTheCarryAgain() throws Exception {
        // Three short lines at the foot of the first page come to three fortieths of a point.
        try (Export export = export(page -> {
            page.spacer(0, 500);
            lines(page, "Foot", 3, SHORT);
            page.spacer(0, 20);
            lines(page, "Top", 4, SHORT);
        })) {
            assertThat(export.placed("Top 0").startPage()).as("the premise: the second page opens with it").isOne();
            assertThat(wordDrift(export, "Top 1", "Top 3")).as("the lines at the top of the page, nothing carried into them")
                    .isCloseTo(TWO_SHORT, within(0.01));
        }
    }

    @Test
    void aPageAListOpensStartsTheCarryAgainToo() throws Exception {
        // Three short lines at the foot of the first page come to three fortieths of a point; a list
        // opens the second, and the lines under it are the first to meet what was carried.
        try (Export export = export(page -> {
            page.spacer(0, 500);
            lines(page, "Foot", 3, SHORT);
            page.spacer(0, 20);
            page.addList(list -> list.name("Points").bullet().items("One"));
            lines(page, "Top", 4, SHORT);
        })) {
            assertThat(export.placed("Points").startPage()).as("the premise: the list opens the second page").isOne();
            assertThat(wordDrift(export, "Top 1", "Top 3")).as("the lines under the list, nothing carried into them")
                    .isCloseTo(TWO_SHORT, within(0.01));
        }
    }

    @Test
    void aParagraphWhosePicturesGrowItsLinesCarriesOnWhatTheLinesAboveItCarried() throws Exception {
        // Word grows the lines to the pictures, past what their rounding took, and what the
        // paragraph owed for it is taken back. Three short lines above it carry three fortieths
        // of a point on, the lines under it adding theirs: the second settles it.
        String text = "rounded text that wraps onto many lines ".repeat(20);
        for (double size = 8; size < 12; size += 0.05) {
            DocumentTextStyle style = DocumentTextStyle.builder().size(size).build();
            double icon = size * 0.9;
            try (Export export = export(page -> {
                lines(page, "Above", 3, SHORT);
                page.addParagraph(p -> p.name("Pictures").margin(DocumentInsets.top(GAP))
                        .inlineImage(DocumentImageData.fromBytes(png()), icon, icon, InlineImageAlignment.CENTER, 0, null)
                        .inlineText(text, style));
                lines(page, "After", 5, SHORT);
            })) {
                var spacing = export.startingWith("rounded").getCTP().getPPr().getSpacing();
                double line = DocxTwips.of(spacing.getLine()) / 20.0;
                double page = export.placed("Pictures").placementHeight();
                if (spacing.getLineRule() == STLineSpacingRule.AT_LEAST
                    && page - Math.round(page / line) * line > 0.05) {
                    assertThat(before(export.paragraph("After 2")) - before(export.paragraph("After 3")))
                            .as("what the second line under it settles, written above the third").isBetween(2L, 3L);
                    return;
                }
            }
        }
        throw new AssertionError("the premise: no size from 8 to 12pt grows a rounded paragraph's lines to its picture");
    }

    /** Lines a paragraph each, {@link #GAP} apart. */
    private static void lines(AbstractFlowBuilder<?, ?> flow, String name, int count, DocumentTextStyle style) {
        for (int index = 0; index < count; index++) {
            String text = name + " " + index;
            double above = index == 0 ? 0 : GAP;
            flow.addParagraph(p -> p.name(text).text(text).textStyle(style).margin(DocumentInsets.top(above)));
        }
    }

    /**
     * How much lower Word sets the second line under the first than the page does, in points: each
     * paragraph between them taken as its space and its one exact line.
     */
    private static double wordDrift(Export export, String first, String last) {
        XWPFParagraph top = export.paragraph(first);
        List<XWPFParagraph> paragraphs = top.getBody().getParagraphs();
        int from = paragraphs.indexOf(top);
        int to = paragraphs.indexOf(export.paragraph(last));
        long twips = before(paragraphs.get(to)) - before(paragraphs.get(from));
        for (int index = from; index < to; index++) {
            XWPFParagraph paragraph = paragraphs.get(index);
            twips += before(paragraph) + Math.round(lineOf(paragraph) * 20) + after(paragraph);
        }
        return twips / 20.0 - (topOf(export.placed(first)) - topOf(export.placed(last)));
    }

    private static double topOf(PlacedNode placed) {
        return placed.placementY() + placed.placementHeight();
    }

    /** A paragraph's written exact line, in points. */
    private static double lineOf(XWPFParagraph paragraph) {
        return DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine()) / 20.0;
    }

    private static long before(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetBefore()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getBefore());
    }

    private static long after(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetSpacing() || !properties.getSpacing().isSetAfter()) {
            return 0;
        }
        return DocxTwips.of(properties.getSpacing().getAfter());
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8, 8,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (java.io.IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static Export export(Consumer<PageFlowBuilder> content) throws Exception {
        DocumentSession session = GraphCompose.document().pageSize(400, 600).margin(DocumentInsets.of(20)).create();
        session.pageFlow(content);
        byte[] bytes = session.export(new DocxSemanticBackend());
        return new Export(session, new XWPFDocument(new ByteArrayInputStream(bytes)));
    }

    private record Export(DocumentSession session, XWPFDocument document) implements AutoCloseable {

        /** The body's paragraph whose text starts so. */
        XWPFParagraph startingWith(String start) {
            return document.getParagraphs().stream().filter(paragraph -> paragraph.getText().startsWith(start))
                    .findFirst().orElseThrow(() -> new AssertionError("no paragraph starts with " + start));
        }

        /** The paragraph, in the body or a table's cell, reading exactly the text. */
        XWPFParagraph paragraph(String text) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (paragraph.getText().equals(text)) {
                    return paragraph;
                }
            }
            for (var table : document.getTables()) {
                for (var row : table.getRows()) {
                    for (var cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            if (paragraph.getText().equals(text)) {
                                return paragraph;
                            }
                        }
                    }
                }
            }
            throw new AssertionError("no paragraph reads " + text);
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
