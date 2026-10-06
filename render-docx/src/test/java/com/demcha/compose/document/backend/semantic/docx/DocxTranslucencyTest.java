package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageBackgroundFill;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A translucent colour in the Word file: text carries its transparency as Word's text fill, and
 * a cell's shading, a border and a rule — which hold only an opaque colour — are flattened
 * against the colour the page paints under them and named in the report.
 *
 * <p>The colour under is the layout's, so a fill over a page's background composites over that
 * background, not over white. The expected values are worked out by hand: a channel is
 * {@code round(over × a + under × (1 − a))}, {@code a} the alpha over 255.</p>
 */
class DocxTranslucencyTest {

    private static final String W14 = "http://schemas.microsoft.com/office/word/2010/wordml";
    private static final DocumentColor NAVY = DocumentColor.rgb(28, 39, 64);
    private static final DocumentColor HALF_BLUE = DocumentColor.rgba(0, 90, 200, 128);
    private static final String FILL_NOTE = "its fill is flattened against the colour under it, because a Word "
                                            + "cell's shading and borders are opaque";

    @Test
    void translucentTextIsWrittenWithItsTransparency() throws Exception {
        try (Exported exported = export(null, page -> page
                .addParagraph(p -> p.text("Faint")
                        .textStyle(DocumentTextStyle.DEFAULT.withColor(DocumentColor.rgba(200, 0, 0, 64))))
                .addParagraph(p -> p.text("Solid words, long enough to be the body of this page")
                        .textStyle(DocumentTextStyle.DEFAULT.withColor(DocumentColor.rgb(0, 0, 200)))))) {
            CTRPr faint = run(exported.document(), "Faint").getCTR().getRPr();
            // LibreOffice takes the colour from w:color and the transparency from the text fill, so
            // w:color keeps the colour as authored.
            assertThat(hex(faint.getColorArray(0).getVal())).isEqualTo("C80000");
            // Transparency, not opacity: (255 - 64) / 255 = 74.902%.
            assertThat(textFill(faint)).isEqualTo("C80000@74902");
            assertThat(textFill(run(exported.document(), "Solid").getCTR().getRPr()))
                    .as("an opaque colour writes no text fill").isNull();
            assertThat(exported.report().bySubject()).as("written, not reported")
                    .doesNotContainKey("translucency");
        }
    }

    @Test
    void aTranslucentBodyColourIsTheStylesAndAnOpaqueRunOverridesIt() throws Exception {
        DocumentTextStyle faint = DocumentTextStyle.DEFAULT.withColor(DocumentColor.rgba(0, 0, 0, 153));
        try (Exported exported = export(null, page -> page
                .addParagraph(p -> p.text("Heading").textStyle(DocumentTextStyle.DEFAULT.withSize(16)))
                .addParagraph(p -> p.text("The body of the page is set in a faint black, and there is "
                                          + "more of it than of the heading, so it is the style's.").textStyle(faint))
                .addParagraph(p -> p.text("A second paragraph in the same faint black.").textStyle(faint))
                .addList(list -> list.name("Points").textStyle(DocumentTextStyle.DEFAULT
                        .withColor(DocumentColor.rgb(0, 0, 0))).items("One", "Two")))) {
            CTRPr marker = exported.document().getNumbering().getAbstractNums().get(0).getCTAbstractNum()
                    .getLvlArray(0).getRPr();
            assertThat(textFill(marker)).as("an opaque list's marker over the translucent style").isEqualTo("000000@");
            CTRPr defaults = exported.document().getStyles().getStyle("Normal").getCTStyle().getRPr();
            assertThat(textFill(defaults)).as("the Normal style's text fill").isEqualTo("000000@40000");
            assertThat(textFill(run(exported.document(), "The body").getCTR().getRPr()))
                    .as("a run in the style's colour follows the style").isNull();
            // A run follows the style's text fill where it writes none, and Word draws the fill's
            // colour over the run's: the opaque heading would come out faint.
            assertThat(textFill(run(exported.document(), "Heading").getCTR().getRPr()))
                    .as("an opaque run under a translucent style writes an opaque fill").isEqualTo("000000@");
        }
    }

    @Test
    void aPanelsTranslucentFillIsFlattenedAgainstThePagesBackground() throws Exception {
        try (Exported exported = export(navyPage(), page -> page
                .addSection("Card", card -> card.fillColor(HALF_BLUE).addParagraph("Inside")))) {
            // 0/90/200 at 128/255 over 28/39/64.
            assertThat(shading(onlyCell(exported.document()))).isEqualTo("0E4184");
            assertThat(notes(exported)).containsExactly(FILL_NOTE);
        }
        try (Exported exported = export(null, page -> page
                .addSection("Card", card -> card.fillColor(HALF_BLUE).addParagraph("Inside")))) {
            assertThat(shading(onlyCell(exported.document()))).as("over the page's white").isEqualTo("7FACE3");
        }
    }

    @Test
    void aChipOnAPanelThatWasFlattenedCompositesOverWhatWasWritten() throws Exception {
        try (Exported exported = export(navyPage(), page -> page
                .addSection("Card", card -> card.fillColor(HALF_BLUE)
                        .addParagraph(p -> p.inlineText("Call ").inlineCode("render()"))))) {
            String card = shading(onlyCell(exported.document()));
            assertThat(card).isEqualTo("0E4184");
            // 175/184/193 at 51/255 over the card as written, 14/65/132: what Word paints under it.
            // The chip's last letter is a run of its own, which carries the space after it.
            assertThat(runShading(run(exported.document(), "render("))).isEqualTo("2E5990");
        }
    }

    @Test
    void aChipOnAPagesBackgroundCompositesOverThatBackground() throws Exception {
        try (Exported exported = export(navyPage(), page -> page
                .addParagraph(p -> p.inlineText("Call ").inlineCode("render()")))) {
            // 175/184/193 at 51/255 over 28/39/64 — over white it was 239/241/243.
            assertThat(runShading(run(exported.document(), "render("))).isEqualTo("39445A");
        }
    }

    @Test
    void aPanelsTranslucentBordersAreFlattenedAndNamed() throws Exception {
        try (Exported exported = export(null, page -> page
                .addSection("Card", card -> card.borders(DocumentBorders.all(
                                DocumentStroke.of(DocumentColor.rgba(200, 0, 0, 100), 2)))
                        .addParagraph("Inside")))) {
            CTBorder top = onlyCell(exported.document()).getCTTc().getTcPr().getTcBorders().getTop();
            // 200/0/0 at 100/255 over white.
            assertThat(hex(top.getColor())).isEqualTo("E99B9B");
            assertThat(notes(exported)).containsExactly("its borders are flattened against the colour under them, "
                                                        + "because a Word cell's shading and borders are opaque");
        }
    }

    @Test
    void aWhollyTransparentFillIsNoShadingAndARuleKeepsItsRoomInTheColourUnder() throws Exception {
        try (Exported exported = export(navyPage(), page -> page
                .addSection("Card", card -> card.fillColor(DocumentColor.rgba(0, 90, 200, 0))
                        .borders(DocumentBorders.all(DocumentStroke.of(DocumentColor.rgba(200, 0, 0, 0), 2)))
                        .addParagraph("Inside")))) {
            XWPFTableCell cell = onlyCell(exported.document());
            assertThat(shading(cell)).as("the page draws no fill").isNull();
            assertThat(hex(cell.getCTTc().getTcPr().getTcBorders().getTop().getColor()))
                    .as("the border holds its room, in the navy under it").isEqualTo("1C2740");
            assertThat(notes(exported)).containsExactly("its borders are flattened against the colour under them, "
                                                        + "because a Word cell's shading and borders are opaque");
        }
    }

    @Test
    void aTablesTranslucentCellsAreFlattenedAndNamedOnce() throws Exception {
        DocumentTableStyle tint = DocumentTableStyle.builder().fillColor(HALF_BLUE)
                .stroke(DocumentStroke.of(DocumentColor.rgba(200, 0, 0, 100), 1)).build();
        try (Exported exported = export(navyPage(), page -> page.add(new com.demcha.compose.document.dsl.TableBuilder()
                .name("Rota").columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("A").withStyle(tint), DocumentTableCell.text("B").withStyle(tint))
                .rowCells(DocumentTableCell.text("C").withStyle(tint), DocumentTableCell.text("D").withStyle(tint))
                .build()))) {
            XWPFTableCell cell = exported.document().getTables().get(0).getRow(1).getCell(1);
            assertThat(shading(cell)).isEqualTo("0E4184");
            // The page draws the rules over the cells' fills: 200/0/0 at 100/255 over 14/65/132.
            assertThat(hex(cell.getCTTc().getTcPr().getTcBorders().getTop().getColor())).isEqualTo("572850");
            assertThat(notes(exported)).containsExactly("its cells' fills and its cells' rules are flattened against "
                                                        + "the colour under them, because a Word cell's shading and "
                                                        + "borders are opaque");
        }
    }

    @Test
    void aTranslucentRuleIsFlattenedAgainstThePagesBackground() throws Exception {
        try (Exported exported = export(navyPage(), page -> page
                .addDivider(d -> d.width(200).thickness(1).color(DocumentColor.rgba(255, 255, 255, 115))))) {
            CTBorder bottom = ruleParagraph(exported.document()).getCTP().getPPr().getPBdr().getBottom();
            // White at 115/255 over 28/39/64: a pale navy, as the page shows it, and not white.
            assertThat(hex(bottom.getColor())).isEqualTo("828896");
            assertThat(notes(exported)).containsExactly(
                    "the rule is flattened against the colour under it, because a paragraph border is opaque");
        }
    }

    @Test
    void aTranslucentSeparatorIsFlattenedAgainstWhite() throws Exception {
        try (Exported exported = export(session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(30).fontSize(10).leftText("Header")
                .showSeparator(true).separatorColor(DocumentColor.rgba(200, 0, 0, 100)).separatorThickness(1)
                .build()), page -> page.addParagraph("Body"))) {
            XWPFParagraph band = exported.document().getHeaderList().get(0).getParagraphs().get(0);
            assertThat(hex(band.getCTP().getPPr().getPBdr().getBottom().getColor())).isEqualTo("E99B9B");
            assertThat(notes(exported)).containsExactly("a page header's separator is flattened against white, "
                                                        + "because a paragraph border is opaque");
        }
    }

    @Test
    void aPanelOverATableCellIsFlattenedAgainstTheCell() throws Exception {
        // The table is the stack's back layer and the panel stands over its cell, which the page
        // paints first: what is under the panel is the cell's red, not the page.
        DocumentTableStyle red = DocumentTableStyle.builder().fillColor(DocumentColor.rgb(200, 40, 40)).build();
        try (Exported exported = export(null, page -> page.addLayerStack(stack -> stack.name("Stack")
                .back(new com.demcha.compose.document.dsl.TableBuilder().name("Under")
                        .columns(DocumentTableColumn.fixed(200))
                        .rowCells(DocumentTableCell.text("Under the panel").withStyle(red)).build())
                .center(new com.demcha.compose.document.dsl.SectionBuilder().name("Over").fillColor(HALF_BLUE)
                        .addParagraph("Over").build())))) {
            // 0/90/200 at 128/255 over 200/40/40.
            assertThat(allCells(exported.document())).extracting(DocxTranslucencyTest::shading).contains("644178");
        }
    }

    @Test
    void withNoLayoutAChipInARowOnATranslucentPanelCompositesOverThePanelAsWritten() throws Exception {
        // The row's cell has no shading, and nothing tells the colour under the paragraph but the
        // panel the export set it on — the panel as written, not its translucent colour.
        try (XWPFDocument document = DocxExports.withoutLayout(400, 400, 20, page -> page
                .addSection("Card", card -> card.fillColor(HALF_BLUE)
                        .addRow(row -> row.addParagraph(p -> p.inlineText("Call ").inlineCode("render()"))
                                .addParagraph(p -> p.text("Beside")))))) {
            assertThat(shading(document.getTables().get(0).getRow(0).getCell(0))).isEqualTo("7FACE3");
            // 175/184/193 at 51/255 over 127/172/227.
            assertThat(runShading(run(document, "render("))).isEqualTo("89AEDC");
        }
    }

    @Test
    void aPanelsBordersAreFlattenedAgainstItsOwnFill() throws Exception {
        // The page draws the borders over the panel's fill, not over what is under the panel.
        try (Exported exported = export(null, page -> page
                .addSection("Card", card -> card.fillColor(NAVY).borders(DocumentBorders.all(
                                DocumentStroke.of(DocumentColor.rgba(255, 255, 255, 51), 2)))
                        .addParagraph("Inside")))) {
            CTBorder top = onlyCell(exported.document()).getCTTc().getTcPr().getTcBorders().getTop();
            // White at 51/255 over 28/39/64; over the white page it would be white.
            assertThat(hex(top.getColor())).isEqualTo("495266");
        }
    }

    @Test
    void aRowsOwnFillIsNotWhatIsUnderItsChip() throws Exception {
        // The export does not write a row's own fill — the report names it as row paint — so what
        // Word shows under the chip is the page's white, and the chip is flattened against that.
        try (Exported exported = export(null, page -> page.addRow(row -> row.fillColor(NAVY)
                .addParagraph(p -> p.inlineText("Call ").inlineCode("render()")).addParagraph("Beside")))) {
            assertThat(exported.report().bySubject()).as("the row's fill is not written").containsKey("row paint");
            assertThat(runShading(run(exported.document(), "render("))).isEqualTo("EFF1F3");
        }
    }

    @Test
    void aSeparatorWrittenIntoSeveralPartsIsNamedOnce() throws Exception {
        // A footer kept off the first page gives the section a first page of its own, so the header
        // band is written into the first page's part and the default one.
        try (Exported exported = export(session -> session.header(DocumentHeaderFooter.builder()
                        .zone(DocumentHeaderFooterZone.HEADER).height(30).fontSize(10).leftText("Header")
                        .showSeparator(true).separatorColor(DocumentColor.rgba(200, 0, 0, 100))
                        .separatorThickness(1).build())
                .footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER).height(20)
                        .fontSize(8).rightText("{page}").numbering(com.demcha.compose.document.output
                                .DocumentPageNumbering.builder().showOnFirstPage(false).build()).build()),
                page -> page.addParagraph("Body"))) {
            assertThat(exported.document().getHeaderList()).as("written into more than one part").hasSizeGreaterThan(1);
            assertThat(notes(exported)).containsExactly("a page header's separator is flattened against white, "
                                                        + "because a paragraph border is opaque");
        }
    }

    @Test
    void aTextFillIsTheLastOfARunsPropertiesAndItsPartSaysItMayBeSkipped() throws Exception {
        DocumentTextStyle faint = new DocumentTextStyle(DocumentTextStyle.DEFAULT.fontName(), 10,
                com.demcha.compose.document.style.DocumentTextDecoration.UNDERLINE, DocumentColor.rgba(200, 0, 0, 64));
        try (Exported exported = export(null, page -> page
                .addParagraph(p -> p.text("Underlined").textStyle(faint))
                .addParagraph(p -> p.text("The body of the page, in an opaque colour and longer than the rest")))) {
            CTRPr properties = run(exported.document(), "Underlined").getCTR().getRPr();
            assertThat(properties.sizeOfUArray()).as("the underline is written").isEqualTo(1);
            // Word writes its 2010 run properties after the ones Word 2007 had.
            try (org.apache.xmlbeans.XmlCursor cursor = properties.newCursor()) {
                cursor.toLastChild();
                assertThat(cursor.getName()).isEqualTo(new QName(W14, "textFill"));
            }
            QName ignorable = new QName("http://schemas.openxmlformats.org/markup-compatibility/2006", "Ignorable");
            try (org.apache.xmlbeans.XmlCursor root = exported.document().getDocument().newCursor()) {
                assertThat(root.getAttributeText(ignorable)).as("the body part").isEqualTo("w14");
            }
            try (org.apache.xmlbeans.XmlCursor root = exported.document().getStyle().newCursor()) {
                assertThat(root.getAttributeText(ignorable)).as("the styles part holds no text fill").isNull();
            }
        }
    }

    @Test
    void anOpaqueDocumentWritesNothingOfWord2010() throws Exception {
        try (Exported exported = export(session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(30).fontSize(10).leftText("Header").build()),
                page -> page.addParagraph("Body").addList(list -> list.name("Points").items("One", "Two"))
                        .addSection("Card", card -> card.fillColor(NAVY).addParagraph("Inside")))) {
            try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(
                    new ByteArrayInputStream(exported.bytes()))) {
                for (java.util.zip.ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                    if (entry.getName().endsWith(".xml")) {
                        assertThat(new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                                .as(entry.getName()).doesNotContain("wordprocessingml/2010").doesNotContain("Ignorable");
                    }
                }
            }
        }
    }

    @Test
    void aNestedItemsMarkTakesTheTextFillItsMarkerIsDrawnIn() throws Exception {
        DocumentTextStyle faint = DocumentTextStyle.DEFAULT.withColor(DocumentColor.rgba(0, 0, 0, 153));
        try (Exported exported = export(null, page -> page
                .addParagraph(p -> p.text("The body of the page is set in a faint black, and there is more of it "
                                          + "than of the list, so it is the style's.").textStyle(faint))
                .addList(list -> list.name("Points").textStyle(DocumentTextStyle.DEFAULT
                                .withColor(DocumentColor.rgb(0, 0, 0)))
                        .addItem("Languages", child -> child.addItem("Java").addItem("Kotlin"))))) {
            XWPFParagraph nested = run(exported.document(), "Java").getParent() instanceof XWPFParagraph paragraph
                    ? paragraph : null;
            assertThat(nested).isNotNull();
            // A nested level states no style of its own, so its marker is drawn in the mark's.
            assertThat(textFill(nested.getCTP().getPPr().getRPr())).as("an opaque fill over the faint style")
                    .isEqualTo("000000@");
        }
    }

    @Test
    void aContainersTranslucentFillAndALinesStrokeAreFlattenedAndNamed() throws Exception {
        try (Exported exported = export(null, page -> page
                .add(new com.demcha.compose.document.node.ContainerNode("Box",
                        List.of(new com.demcha.compose.document.dsl.ParagraphBuilder().name("Inside").text("Inside")
                                .build()), 0, DocumentInsets.of(4), DocumentInsets.zero(), HALF_BLUE, null))
                .addLine(line -> line.horizontal(200).stroke(DocumentStroke.of(DocumentColor.rgba(0, 0, 0, 128), 1))))) {
            assertThat(shading(onlyCell(exported.document()))).isEqualTo("7FACE3");
            // Black at 128/255 over white.
            assertThat(hex(ruleParagraph(exported.document()).getCTP().getPPr().getPBdr().getBottom().getColor()))
                    .isEqualTo("7F7F7F");
            assertThat(notes(exported)).containsExactly(FILL_NOTE,
                    "the rule is flattened against the colour under it, because a paragraph border is opaque");
        }
    }

    @Test
    void underAPictureTheSurfaceStandsInAndTheFillIsStillNamed() throws Exception {
        // The layout carries no colour for a picture's pixels: the panel over it is flattened against
        // the surface it is written on — the page's white — and named all the same.
        try (Exported exported = export(navyPage(), page -> page.addLayerStack(stack -> stack.name("Stack")
                .back(new com.demcha.compose.document.dsl.ImageBuilder().name("Photo")
                        .source(com.demcha.compose.document.image.DocumentImageData.fromBytes(png())).size(200, 80)
                        .build())
                .center(new com.demcha.compose.document.dsl.SectionBuilder().name("Over").fillColor(HALF_BLUE)
                        .addParagraph("Over").build())))) {
            assertThat(allCells(exported.document())).extracting(DocxTranslucencyTest::shading).contains("7FACE3");
            assertThat(notes(exported)).contains(FILL_NOTE);
        }
    }

    @Test
    void withNoLayoutAFillIsFlattenedAgainstTheSurfaceItIsWrittenOn() throws Exception {
        DocxExportReport report = DocxExports.reportWithoutLayout(400, 400, 20, page -> page
                .addSection("Card", card -> card.fillColor(HALF_BLUE).addParagraph("Inside")));
        assertThat(report.bySubject().get("translucency")).extracting(DocxExportReport.Note::detail)
                .containsExactly(FILL_NOTE);
    }

    private static Consumer<DocumentSession> navyPage() {
        return session -> session.pageBackgrounds(List.of(PageBackgroundFill.fullPage(NAVY)));
    }

    private record Exported(XWPFDocument document, DocxExportReport report, byte[] bytes) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            document.close();
        }
    }

    private static byte[] png() {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(40, 40,
                    java.awt.image.BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static Exported export(Consumer<DocumentSession> setup, Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        byte[] bytes;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400).margin(DocumentInsets.of(20))
                .create()) {
            if (setup != null) {
                setup.accept(session);
            }
            session.pageFlow(content::accept);
            bytes = session.export(new DocxSemanticBackend(captured::set));
        }
        return new Exported(new XWPFDocument(new ByteArrayInputStream(bytes)), captured.get(), bytes);
    }

    private static List<String> notes(Exported exported) {
        return exported.report().bySubject().getOrDefault("translucency", List.of()).stream()
                .map(DocxExportReport.Note::detail).toList();
    }

    private static XWPFRun run(XWPFDocument document, String startsWith) {
        for (XWPFParagraph paragraph : allParagraphs(document)) {
            for (XWPFRun run : paragraph.getRuns()) {
                if (run.text().startsWith(startsWith)) {
                    return run;
                }
            }
        }
        throw new AssertionError("no run starts with " + startsWith);
    }

    private static List<XWPFParagraph> allParagraphs(XWPFDocument document) {
        List<XWPFParagraph> paragraphs = new java.util.ArrayList<>(document.getParagraphs());
        allCells(document).forEach(cell -> paragraphs.addAll(cell.getParagraphs()));
        return paragraphs;
    }

    /** Every cell, nested tables' included: a row in a panel is a table in the panel's cell. */
    private static List<XWPFTableCell> allCells(XWPFDocument document) {
        List<XWPFTableCell> cells = new java.util.ArrayList<>();
        document.getTables().forEach(table -> collectCells(table, cells));
        return cells;
    }

    private static void collectCells(org.apache.poi.xwpf.usermodel.XWPFTable table, List<XWPFTableCell> cells) {
        table.getRows().forEach(row -> row.getTableCells().forEach(cell -> {
            cells.add(cell);
            cell.getTables().forEach(nested -> collectCells(nested, cells));
        }));
    }

    private static XWPFParagraph ruleParagraph(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(p -> p.getCTP().getPPr() != null && p.getCTP().getPPr().isSetPBdr())
                .findFirst().orElseThrow();
    }

    private static XWPFTableCell onlyCell(XWPFDocument document) {
        assertThat(document.getTables()).hasSize(1);
        return document.getTables().get(0).getRow(0).getCell(0);
    }

    private static String shading(XWPFTableCell cell) {
        var properties = cell.getCTTc().getTcPr();
        return properties == null || !properties.isSetShd() ? null : hex(properties.getShd().getFill());
    }

    private static String runShading(XWPFRun run) {
        CTRPr properties = run.getCTR().getRPr();
        return properties.sizeOfShdArray() == 0 ? null : hex(properties.getShdArray(0).getFill());
    }

    /** A text fill as {@code RRGGBB@transparency}, the transparency empty for an opaque one; null for none. */
    private static String textFill(XmlObject properties) {
        if (properties == null) {
            return null;
        }
        XmlObject[] fills = properties.selectChildren(new QName(W14, "textFill"));
        if (fills.length == 0) {
            return null;
        }
        assertThat(fills).as("one text fill").hasSize(1);
        XmlObject colour = fills[0].selectChildren(new QName(W14, "solidFill"))[0]
                .selectChildren(new QName(W14, "srgbClr"))[0];
        XmlObject[] alpha = colour.selectChildren(new QName(W14, "alpha"));
        return attribute(colour) + "@" + (alpha.length == 0 ? "" : attribute(alpha[0]));
    }

    private static String attribute(XmlObject element) {
        try (org.apache.xmlbeans.XmlCursor cursor = element.selectAttribute(new QName(W14, "val")).newCursor()) {
            return cursor.getTextValue();
        }
    }

    /** XmlBeans hands an ST_HexColor back as bytes, so read it as the colour it encodes. */
    private static String hex(Object value) {
        if (value instanceof byte[] bytes) {
            StringBuilder text = new StringBuilder();
            for (byte part : bytes) {
                text.append(String.format("%02X", part & 0xFF));
            }
            return text.toString();
        }
        return String.valueOf(value);
    }
}
