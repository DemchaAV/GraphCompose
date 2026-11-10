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
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
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
    /** The default face at 8pt: a line the page sets a whole number of twips tall, 7.4pt, written as tall. */
    private static final DocumentTextStyle EVEN = DocumentTextStyle.DEFAULT.withSize(8);
    /**
     * How far from the page a block under a cell may stand once the cell's end settles what its
     * lines carry: the twip that settles it rounds it by up to half a twip.
     */
    private static final double SETTLED = 0.03;
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
        // opens the second, its item the first to meet what was carried, and the lines under it
        // carry on from that item alone.
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

    @Test
    void anItemWhosePicturesGrowItsLinesCarriesOnWhatTheLinesAboveItCarried() throws Exception {
        // As a paragraph's: the item owed what its lines fell short, and its pictures grow them past
        // it; what the three short lines above carried is carried on, the second line under it
        // settling it.
        String text = "rounded text that wraps onto many lines ".repeat(20);
        for (double size = 8; size < 12; size += 0.05) {
            DocumentTextStyle style = DocumentTextStyle.builder().size(size).build();
            double icon = size * 0.9;
            try (Export export = export(page -> {
                lines(page, "Above", 3, SHORT);
                page.addList(list -> list.name("Pictures").textStyle(style).margin(DocumentInsets.top(GAP))
                        .hangingIndent(true).addItem(item -> item.image(DocumentImageData.fromBytes(png()), icon, icon, InlineImageAlignment.CENTER)
                                .style(text, style)));
                lines(page, "After", 5, SHORT);
            })) {
                var spacing = export.containing("rounded").getCTP().getPPr().getSpacing();
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
        throw new AssertionError("the premise: no size from 8 to 12pt grows a rounded item's lines to its picture");
    }

    @Test
    void itemsOfAListOweWhatTheirLinesFallShortTogether() throws Exception {
        List<String> items = java.util.stream.IntStream.range(0, 30).mapToObj(index -> "Item " + index).toList();
        try (Export export = export(page -> page.addList(list -> list.name("Points").textStyle(SHORT).itemSpacing(GAP)
                .items(items)))) {
            // Thirty items of a line each, GAP apart: the page's line is what the list's height leaves.
            double line = (export.placed("Points").placementHeight() - 29 * GAP) / 30;
            assertThat(lineOf(export.paragraph("Item 1"))).as("the premise: written short of the page's").isLessThan(line);
            assertThat(wordDistance(export, "Item 1", "Item 29") - 28 * (line + GAP))
                    .as("the last item where the page sets it, to within what is still carried")
                    .isCloseTo(0, within(CARRIED));
        }
    }

    @Test
    void itemsWrittenAtTheirListsTallestLineGiveWhatTheyPassOutOfTheSpaceBetweenThem() throws Exception {
        // One item holds a word twice the size: Word has one line height for the list, and the
        // others, written at it, pass the page by points each, which the space under each takes.
        DocumentTextStyle big = DocumentTextStyle.DEFAULT.withSize(16);
        try (Export export = export(page -> page.addList(list -> {
            list.name("Points").textStyle(SHORT).itemSpacing(12).hangingIndent(true)
                    .addItem(item -> item.plain("Large ").style("word", big));
            for (int index = 1; index <= 6; index++) {
                String name = "Item " + index;
                list.addItem(item -> item.plain(name));
            }
        }))) {
            double line = lineOf(export.paragraph("Item 1"));
            assertThat(line).as("the premise: written at the large word's line").isGreaterThan(8.325 + 5);
            assertThat(wordDistance(export, "Item 1", "Item 5") - 4 * (8.325 + 12))
                    .as("the fifth item where the page sets it, to within what is still carried")
                    .isCloseTo(0, within(CARRIED));
        }
    }

    @Test
    void wrappedItemsAreSettledWithTheGapBetweenTheirLines() throws Exception {
        // Each item two lines and more with 1.2pt between them, as Panel's are: Word puts the gap
        // into every line, and the one too many comes off the space above the item.
        List<String> items = java.util.stream.IntStream.range(0, 18)
                .mapToObj(index -> "wrapped text ".repeat(9) + "Item " + index).toList();
        try (Export export = export(page -> page.addList(list -> list.name("Points").textStyle(SHORT).itemSpacing(GAP)
                .lineSpacing(1.2).items(items)))) {
            assertThat(export.placed("Points").endPage()).as("the premise: on one page").isZero();
            double item = (export.placed("Points").placementHeight() - 17 * GAP) / 18;
            assertThat(item).as("the premise: each item two lines").isCloseTo(2 * 8.325 + 1.2, within(0.01));
            assertThat(wordDistance(export, "Item 1", "Item 17", 2) - 16 * (item + GAP))
                    .as("the last item where the page sets it, to within what is still carried")
                    .isCloseTo(0, within(CARRIED));
        }
    }

    @Test
    void aPageAnItemOpensStartsTheCarryAgain() throws Exception {
        // Two short items at the foot of the first page come to two fortieths of a point; the third
        // opens the second page. Carried on, the fourth would settle four of them, owing a tenth of a
        // point above the fifth; started again, the sixth settles four.
        List<String> items = java.util.stream.IntStream.range(0, 8).mapToObj(index -> "Item " + index).toList();
        try (Export export = export(page -> {
            page.spacer(0, 533);
            page.addList(list -> list.name("Points").textStyle(SHORT).itemSpacing(GAP).items(items));
        })) {
            assertThat(export.placed("Points").endPage()).as("the premise: the list runs onto the second page").isOne();
            assertThat(wordDistance(export, "Item 3", "Item 5") - 2 * (8.325 + GAP))
                    .as("the items on the second page, nothing carried into them").isCloseTo(TWO_SHORT, within(0.01));
        }
    }

    @Test
    void listsOfAnItemEachGiveWhatTheirLinesPassTogetherOutOfTheSpaceBetweenThem() throws Exception {
        // As Panel's skills are written: a list to each item, the lists GAP apart.
        try (Export export = export(page -> {
            for (int index = 0; index < 30; index++) {
                String name = "Item " + index;
                double above = index == 0 ? 0 : GAP;
                page.addList(list -> list.name(name).textStyle(TALL).margin(DocumentInsets.top(above)).items(name));
            }
        })) {
            assertThat(lineOf(export.paragraph("Item 1"))).as("the premise: written taller than the page's")
                    .isGreaterThan(export.placed("Item 1").placementHeight());
            assertThat(wordDrift(export, "Item 1", "Item 29"))
                    .as("the last item where the page sets it, to within what is still carried")
                    .isCloseTo(0, within(CARRIED));
        }
    }

    @Test
    void aCellsEndOwesWhatItsLinesFallShortThere() throws Exception {
        // Three short lines in the row's one column come to three fortieths of a point there, the row
        // as tall in Word as the column.
        try (Export export = export(page -> aRowOf(page, SHORT))) {
            assertThat(export.document().getTables().get(0).getRow(0).getHeight()).as("the premise: a row its column makes")
                    .isZero();
            assertThat(wordDrift(export, "Above 1", "Under 1")).as("the lines under the row where the page sets them")
                    .isCloseTo(0, within(SETTLED));
        }
    }

    @Test
    void aCellsEndHangsWhatItsLinesPassThePageByThere() throws Exception {
        // Three tall lines in the row's one column come to three fortieths of a point there: the gap
        // under the row takes them.
        try (Export export = export(page -> aRowOf(page, TALL))) {
            assertThat(wordDrift(export, "Above 1", "Under 1")).as("the lines under the row where the page sets them")
                    .isCloseTo(0, within(SETTLED));
        }
    }

    @Test
    void aPanelsPaddingBelowTakesWhatItsLinesPassThePageByAtItsEnd() throws Exception {
        // A card held at the page's height: three tall lines in it pass the page by three fortieths
        // of a point, and its padding below holds them, as Panel's experience card's does.
        try (Export export = export(page -> {
            page.spacing(GAP);
            lines(page, "Above", 3, EVEN);
            page.addSection("Card", card -> {
                card.fillColor(com.demcha.compose.document.style.DocumentColor.rgb(240, 244, 248))
                        .padding(DocumentInsets.of(8)).spacing(GAP);
                lines(card, "In", 3, TALL);
            });
            lines(page, "Under", 3, EVEN);
        })) {
            assertThat(export.document().getTables().get(0).getRow(0).getHeight()).as("the premise: held at its height")
                    .isPositive();
            assertThat(wordDrift(export, "Above 1", "Under 1")).as("the lines under the card where the page sets them")
                    .isCloseTo(0, within(SETTLED));
        }
    }

    /** Three even lines, a row of one column of three lines in the style, and three even lines, GAP apart. */
    private static void aRowOf(PageFlowBuilder page, DocumentTextStyle style) {
        page.spacing(GAP);
        lines(page, "Above", 3, EVEN);
        page.addRow(row -> row.name("Row").addSection(column -> {
            column.spacing(GAP);
            lines(column, "In", 3, style);
        }));
        lines(page, "Under", 3, EVEN);
    }

    /** Lines a paragraph each, {@link #GAP} apart. */
    private static void lines(AbstractFlowBuilder<?, ?> flow, String name, int count, DocumentTextStyle style) {
        for (int index = 0; index < count; index++) {
            String text = name + " " + index;
            double above = index == 0 ? 0 : GAP;
            flow.addParagraph(p -> p.name(text).text(text).textStyle(style).margin(DocumentInsets.top(above)));
        }
    }

    /** How much lower Word sets the second line under the first than the page does, in points. */
    private static double wordDrift(Export export, String first, String last) {
        return wordDistance(export, first, last) - (topOf(export.placed(first)) - topOf(export.placed(last)));
    }

    /**
     * How far under the first line's top Word sets the second's, in points: each paragraph between
     * them taken as its space and its one exact line, each table as its rows.
     */
    private static double wordDistance(Export export, String first, String last) {
        return wordDistance(export, first, last, 1);
    }

    /** {@link #wordDistance(Export, String, String)}, each paragraph between them {@code lines} lines. */
    private static double wordDistance(Export export, String first, String last, int lines) {
        XWPFParagraph top = export.paragraph(first);
        XWPFParagraph bottom = export.paragraph(last);
        List<IBodyElement> elements = top.getBody().getBodyElements();
        int from = elements.indexOf(top);
        int to = elements.indexOf(bottom);
        double points = (before(bottom) - before(top)) / 20.0;
        for (int index = from; index < to; index++) {
            points += heightOf(elements.get(index), lines);
        }
        return points;
    }

    /**
     * How tall Word sets a paragraph of so many lines or a table, in points: a row as tall as its
     * tallest cell's content, its paragraphs a line each, or the height it is held at, whichever is
     * more, and its cells' margins.
     */
    private static double heightOf(IBodyElement element, int lines) {
        if (element instanceof XWPFParagraph paragraph) {
            return (before(paragraph) + lines * Math.round(lineOf(paragraph) * 20) + after(paragraph)) / 20.0;
        }
        double height = 0;
        for (XWPFTableRow row : ((XWPFTable) element).getRows()) {
            double tallest = 0;
            long margins = 0;
            for (XWPFTableCell cell : row.getTableCells()) {
                double content = 0;
                for (IBodyElement inner : cell.getBodyElements()) {
                    content += heightOf(inner, 1);
                }
                tallest = Math.max(tallest, content);
                margins = Math.max(margins, verticalMargins(cell));
            }
            height += Math.max(row.getHeight() / 20.0, tallest) + margins / 20.0;
        }
        return height;
    }

    private static long verticalMargins(XWPFTableCell cell) {
        var properties = cell.getCTTc().getTcPr();
        if (properties == null || !properties.isSetTcMar()) {
            return 0;
        }
        var margins = properties.getTcMar();
        return (margins.isSetTop() ? DocxTwips.of(margins.getTop().getW()) : 0)
               + (margins.isSetBottom() ? DocxTwips.of(margins.getBottom().getW()) : 0);
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

        /** The body's paragraph whose text holds the given. */
        XWPFParagraph containing(String text) {
            return document.getParagraphs().stream().filter(paragraph -> paragraph.getText().contains(text))
                    .findFirst().orElseThrow(() -> new AssertionError("no paragraph holds " + text));
        }

        /** The body's paragraph whose text starts so. */
        XWPFParagraph startingWith(String start) {
            return document.getParagraphs().stream().filter(paragraph -> paragraph.getText().startsWith(start))
                    .findFirst().orElseThrow(() -> new AssertionError("no paragraph starts with " + start));
        }

        /**
         * The paragraph, in the body or a table's cell, reading exactly the text, or the text after a
         * list's marker written before it.
         */
        XWPFParagraph paragraph(String text) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (reads(paragraph, text)) {
                    return paragraph;
                }
            }
            for (var table : document.getTables()) {
                for (var row : table.getRows()) {
                    for (var cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            if (reads(paragraph, text)) {
                                return paragraph;
                            }
                        }
                    }
                }
            }
            throw new AssertionError("no paragraph reads " + text);
        }

        private static boolean reads(XWPFParagraph paragraph, String text) {
            String read = paragraph.getText();
            return read.equals(text) || read.endsWith(" " + text) || read.endsWith("\t" + text);
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
