package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shaded spaces closing a paragraph are written as no-break spaces, which Word fills.
 *
 * <p>Word draws nothing for the spaces at the end of a paragraph, their shading included: a chip of
 * spaces cutting a rule out behind a heading ({@code TealPulse}) lost its fill in Word, and the
 * rule ran on to the heading's letters. Written as no-break spaces, Word fills them; LibreOffice
 * fills neither, and the chip's note says so. A space Word draws stays a space.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxClosingSpacesTest {

    private static final String NBSP = String.valueOf((char) 0x00A0);
    private static final DocumentTextStyle STYLE = DocumentTextStyle.builder().size(10).build();
    private static final DocumentInsets NO_PADDING = DocumentInsets.zero();
    private static final String LIBREOFFICE = "LibreOffice draws none of them";

    // -- the rule, on the file ------------------------------------------------------------------

    @Test
    void shadedSpacesClosingAParagraphBecomeNoBreakSpaces() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            text(para, "HEADING", false);
            XWPFRun spaces = text(para, "   ", true);

            List<org.apache.xmlbeans.XmlObject> filled = DocxClosingSpaces.fill(document);

            assertThat(spaces.text()).isEqualTo(NBSP.repeat(3));
            assertThat(filled).containsExactly(spaces.getCTR());
        }
    }

    @Test
    void aShadedRunsLettersStayAndOnlyItsClosingSpacesChange() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun chip = text(para, "Paid  now  ", true);

            DocxClosingSpaces.fill(document);

            assertThat(chip.text()).isEqualTo("Paid  now" + NBSP.repeat(2));
        }
    }

    @Test
    void unshadedSpacesClosingAParagraphStay() throws Exception {
        // Nothing behind them to fill.
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            text(para, "Heading", true);
            XWPFRun spaces = text(para, "   ", false);

            assertThat(DocxClosingSpaces.fill(document)).isEmpty();
            assertThat(spaces.text()).isEqualTo("   ");
        }
    }

    @Test
    void shadedSpacesBehindUnshadedClosingSpacesAreFilledToo() throws Exception {
        // Both close the paragraph, so Word draws neither.
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            text(para, "Heading", false);
            XWPFRun chip = text(para, "  ", true);
            XWPFRun after = text(para, " ", false);

            DocxClosingSpaces.fill(document);

            assertThat(chip.text()).isEqualTo(NBSP.repeat(2));
            assertThat(after.text()).isEqualTo(" ");
        }
    }

    @Test
    void shadedSpacesBeforeTextStay() throws Exception {
        // Word draws a space the line goes on after.
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun chip = text(para, "Paid   ", true);
            text(para, "today", false);

            assertThat(DocxClosingSpaces.fill(document)).isEmpty();
            assertThat(chip.text()).isEqualTo("Paid   ");
        }
    }

    @Test
    void shadedSpacesBeforeATabStay() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun chip = text(para, "Role   ", true);
            para.createRun().addTab();

            assertThat(DocxClosingSpaces.fill(document)).isEmpty();
            assertThat(chip.text()).isEqualTo("Role   ");
        }
    }

    @Test
    void shadedSpacesBeforeABreakInTheirOwnRunStay() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun chip = text(para, "Role   ", true);
            chip.addBreak();

            assertThat(DocxClosingSpaces.fill(document)).isEmpty();
            assertThat(chip.getCTR().getTArray(0).getStringValue()).isEqualTo("Role   ");
        }
    }

    @Test
    void shadedSpacesBeforeAnInlinePictureStay() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun chip = text(para, "Call   ", true);
            para.createRun().getCTR().addNewDrawing().addNewInline();

            assertThat(DocxClosingSpaces.fill(document)).isEmpty();
            assertThat(chip.text()).isEqualTo("Call   ");
        }
    }

    @Test
    void anAnchoredDrawingAfterThemTakesNoRoomAndIsPassedOver() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun chip = text(para, "Heading   ", true);
            para.createRun().getCTR().addNewDrawing().addNewAnchor();

            assertThat(DocxClosingSpaces.fill(document)).containsExactly(chip.getCTR());
            assertThat(chip.text()).isEqualTo("Heading" + NBSP.repeat(3));
        }
    }

    @Test
    void spacesInALinkClosingTheParagraphAreFilled() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            text(para, "Site", false);
            CTR linked = para.getCTP().addNewHyperlink().addNewR();
            linked.addNewT().setStringValue("  ");
            shade(linked);

            assertThat(DocxClosingSpaces.fill(document)).containsExactly(linked);
            assertThat(linked.getTArray(0).getStringValue()).isEqualTo(NBSP.repeat(2));
        }
    }

    @Test
    void aLinkBeforeTheChipIsReadInItsPlaceAlongTheParagraph() throws Exception {
        // Links and plain runs are read back in the order they stand. A chip of letters after the
        // link closes the paragraph, so the link's spaces, before it, stay; a chip of spaces after
        // it is read first, and the link's spaces closing the paragraph with it are filled after.
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            CTR linked = para.getCTP().addNewHyperlink().addNewR();
            linked.addNewT().setStringValue("Site  ");
            shade(linked);
            text(para, "Go", true);

            assertThat(DocxClosingSpaces.fill(document)).isEmpty();
            assertThat(linked.getTArray(0).getStringValue()).isEqualTo("Site  ");
        }
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            CTR linked = para.getCTP().addNewHyperlink().addNewR();
            linked.addNewT().setStringValue("Site  ");
            shade(linked);
            XWPFRun chip = text(para, "  ", true);

            assertThat(DocxClosingSpaces.fill(document)).containsExactly(chip.getCTR(), linked);
            assertThat(linked.getTArray(0).getStringValue()).isEqualTo("Site" + NBSP.repeat(2));
        }
    }

    @Test
    void spacesInASimpleFieldClosingTheParagraphAreFilled() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            text(para, "Page", false);
            CTR inField = para.getCTP().addNewFldSimple().addNewR();
            inField.addNewT().setStringValue("1  ");
            shade(inField);

            assertThat(DocxClosingSpaces.fill(document)).containsExactly(inField);
            assertThat(inField.getTArray(0).getStringValue()).isEqualTo("1" + NBSP.repeat(2));
        }
    }

    @Test
    void shadedSpacesBeforeAFieldCharacterStay() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun chip = text(para, "Page   ", true);
            para.createRun().getCTR().addNewFldChar()
                    .setFldCharType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType.BEGIN);

            assertThat(DocxClosingSpaces.fill(document)).isEmpty();
            assertThat(chip.text()).isEqualTo("Page   ");
        }
    }

    @Test
    void spacesClosingAHeadersParagraphAreFilled() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph();
            XWPFParagraph para = document.createHeader(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT)
                    .createParagraph();
            XWPFRun chip = text(para, "Draft  ", true);

            DocxClosingSpaces.fill(document);

            assertThat(chip.text()).isEqualTo("Draft" + NBSP.repeat(2));
        }
    }

    @Test
    void spacesClosingACellsParagraphAreFilled() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createTable(1, 1).getRow(0).getCell(0).getParagraphs().get(0);
            XWPFRun chip = text(para, "Due  ", true);

            DocxClosingSpaces.fill(document);

            assertThat(chip.text()).isEqualTo("Due" + NBSP.repeat(2));
        }
    }

    @Test
    void aRunShadedNilOrAutoIsNotFilled() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun nil = text(para, "A  ", false);
            CTShd shading = nil.getCTR().addNewRPr().addNewShd();
            shading.setVal(STShd.NIL);
            shading.setFill("FFFFFF");
            XWPFParagraph other = document.createParagraph();
            XWPFRun auto = text(other, "B  ", false);
            CTShd autoShading = auto.getCTR().addNewRPr().addNewShd();
            autoShading.setVal(STShd.CLEAR);
            autoShading.setFill("auto");

            assertThat(DocxClosingSpaces.fill(document)).isEmpty();
        }
    }

    // -- through the export ---------------------------------------------------------------------

    @Test
    void aChipOfSpacesClosingAParagraphIsFilledAndItsNoteSaysLibreOfficeDoesNot() throws Exception {
        Exported exported = export(page -> page.addParagraph(p -> p.inlineText("HEADING", STYLE)
                .inlineHighlight("   ", STYLE, DocumentColor.WHITE, 0, NO_PADDING)));

        assertThat(exported.textOf("HEADING")).isEqualTo("HEADING" + NBSP.repeat(3));
        assertThat(exported.chipNotes()).singleElement().asString()
                .isEqualTo("the fill is written as run shading; the spaces closing its paragraph are written as "
                           + "no-break spaces, which Word fills and counts in its line; LibreOffice draws none of "
                           + "them, so its fill ends at its letters there, and a centred or right-aligned line stands "
                           + "about half their width or their width further right");
    }

    @Test
    void aParagraphWithAChipIsNotLaidOverTheFlowSoItsChipIsNamed() throws Exception {
        // A paragraph of plain runs laid over the flow is set in a text box; one holding a chip is
        // written in the flow, so no chip is in a text box this export cannot name.
        Exported plain = export(page -> page.add(DocxExports.laidOverTheFlow(new ParagraphBuilder()
                .inlineText("Lockup", STYLE).build())));
        assertThat(plain.document().getDocument().xmlText()).as("the fixture reaches the text box")
                .containsPattern("txbxContent[\\s\\S]*Lockup");

        Exported chip = export(page -> page.add(DocxExports.laidOverTheFlow(new ParagraphBuilder()
                .inlineText("Lockup", STYLE).inlineHighlight("  ", STYLE, DocumentColor.WHITE, 0, NO_PADDING).build())));
        assertThat(chip.document().getDocument().xmlText()).doesNotContain("txbxContent");
        assertThat(chip.textOf("Lockup")).isEqualTo("Lockup" + NBSP.repeat(2));
        assertThat(chip.chipNotes()).singleElement().asString().contains(LIBREOFFICE);
    }

    @Test
    void eachListItemClosedByAChipOfItsOwnIsNamedInItsChipsNote() throws Exception {
        // The items share the list's path; each chip has its note, and each note says so.
        Exported exported = export(page -> page.addList(list -> list.hangingIndent(true)
                .addItem(item -> item.plain("Java").highlight("  ", STYLE, DocumentColor.WHITE, 0,
                        new DocumentInsets(2, 0, 2, 0)))
                .addItem(item -> item.plain("Kotlin").highlight("  ", STYLE, DocumentColor.WHITE, 0,
                        new DocumentInsets(2, 0, 2, 0)))));

        assertThat(exported.chipNotes()).hasSize(2)
                .allSatisfy(note -> assertThat(note).contains("padding above and below").contains(LIBREOFFICE));
    }

    @Test
    void aChipInARowATableSplitAtAPageWithOtherMarginsMovesIsNamed() throws Exception {
        // The rows moved into the table's second part are copies; their chips are named before.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        int rows = 30;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 400).margin(DocumentInsets.of(36))
                .create()) {
            session.pageMargins(List.of(com.demcha.compose.document.api.PageMarginRule.from(2,
                    new DocumentInsets(36, 36, 136, 36))));
            session.pageFlow(page -> page.addTable(table -> {
                table.columns(com.demcha.compose.document.table.DocumentTableColumn.fixed(200),
                        com.demcha.compose.document.table.DocumentTableColumn.fixed(100));
                for (int row = 0; row < rows; row++) {
                    table.rowCells(com.demcha.compose.document.table.DocumentTableCell.node(new ParagraphBuilder()
                                    .inlineText("Item " + row, STYLE)
                                    .inlineHighlight("  ", STYLE, DocumentColor.WHITE, 0, NO_PADDING).build()),
                            com.demcha.compose.document.table.DocumentTableCell.text("$" + row));
                }
            }));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getTables()).as("split where page 2 starts").hasSize(2);
        }
        assertThat(report.get().notes()).filteredOn(note -> note.subject().equals("inline chip"))
                .as("every row's chip, in either part").hasSize(rows)
                .allSatisfy(note -> assertThat(note.detail()).contains(LIBREOFFICE));
    }

    @Test
    void spacesClosingATextBoxsParagraphAreFilled() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph para = document.createParagraph();
            text(para, "Beside", false);
            String w = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
            CTR box = CTR.Factory.parse("<w:r xmlns:w='" + w + "'"
                    + " xmlns:wp='http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing'"
                    + " xmlns:a='http://schemas.openxmlformats.org/drawingml/2006/main'"
                    + " xmlns:wps='http://schemas.microsoft.com/office/word/2010/wordprocessingShape'>"
                    + "<w:drawing><wp:anchor><a:graphic><a:graphicData"
                    + " uri='http://schemas.microsoft.com/office/word/2010/wordprocessingShape'><wps:wsp><wps:txbx>"
                    + "<w:txbxContent><w:p><w:r><w:rPr><w:shd w:val='clear' w:color='auto' w:fill='FFFFFF'/></w:rPr>"
                    + "<w:t xml:space='preserve'>Badge  </w:t></w:r></w:p></w:txbxContent>"
                    + "</wps:txbx></wps:wsp></a:graphicData></a:graphic></wp:anchor></w:drawing></w:r>");
            para.getCTP().addNewR().set(box);

            assertThat(DocxClosingSpaces.fill(document)).hasSize(1);
            assertThat(para.getCTP().xmlText()).contains("Badge" + NBSP.repeat(2)).contains(">Beside<");
        }
    }

    @Test
    void theLibreOfficeRemainderGoesIntoTheChipsOwnNote() throws Exception {
        // One chip, one note: what it loses already is followed by what LibreOffice does not fill.
        Exported exported = export(page -> page.addParagraph(p -> p.inlineText("HEADING", STYLE)
                .inlineHighlight("   ", STYLE, DocumentColor.WHITE, 0, new DocumentInsets(2, 0, 2, 0))));

        assertThat(exported.chipNotes()).singleElement().asString()
                .startsWith("the fill is written as run shading; its padding above and below its letters is not in "
                            + "the file; the spaces closing its paragraph")
                .contains(LIBREOFFICE);
    }

    @Test
    void aChipBeforeTextKeepsItsSpacesAndSaysNothingOfThem() throws Exception {
        Exported exported = export(page -> page.addParagraph(p -> p
                .inlineHighlight("Paid ", STYLE, DocumentColor.LIGHT_GRAY, 0, NO_PADDING)
                .inlineText(" today", STYLE)));

        assertThat(exported.textOf("Paid")).isEqualTo("Paid  today");
        assertThat(exported.chipNotes()).noneMatch(note -> note.contains(LIBREOFFICE));
    }

    @Test
    void aChipSplitAtItsLastLetterIsStillNamed() throws Exception {
        // Its right padding is written as spacing after its last character, which goes in a run of
        // its own: here its one closing space, alone in the run the walk fills. That run is the
        // chip's too.
        Exported exported = export(page -> page.addParagraph(p -> p.inlineText("Status ", STYLE)
                .inlineHighlight("Paid ", STYLE, DocumentColor.LIGHT_GRAY, 0, new DocumentInsets(0, 3, 0, 0))));

        assertThat(exported.textOf("Status")).isEqualTo("Status Paid" + NBSP);
        assertThat(exported.chipNotes()).singleElement().asString().contains(LIBREOFFICE);
    }

    @Test
    void aChipSplitWithSpacesInBothRunsIsNamedOnce() throws Exception {
        // Its last space goes in a run of its own, the two before it stay in the chip's: both runs
        // are filled, and the chip is one chip.
        Exported exported = export(page -> page.addParagraph(p -> p.inlineText("HEADING", STYLE)
                .inlineHighlight("   ", STYLE, DocumentColor.WHITE, 0, new DocumentInsets(0, 3, 0, 0))));

        assertThat(exported.textOf("HEADING")).isEqualTo("HEADING" + NBSP.repeat(3));
        assertThat(exported.chipNotes()).singleElement().asString().contains(LIBREOFFICE);
    }

    @Test
    void aListItemClosedByAChipOfSpacesIsFilledAndNamed() throws Exception {
        // Its right padding splits its closing space off into a run of its own, as in a paragraph.
        Exported exported = export(page -> page.addList(list -> list.hangingIndent(true)
                .addItem(item -> item.plain("Skill ")
                        .highlight("Go ", STYLE, DocumentColor.LIGHT_GRAY, 0, new DocumentInsets(0, 3, 0, 0)))));

        assertThat(exported.textOf("Skill")).endsWith("Go" + NBSP);
        assertThat(exported.chipNotes()).anySatisfy(note -> assertThat(note).contains(LIBREOFFICE));
    }

    @Test
    void theLeftOfALinePairKeepsItsSpacesBeforeTheTab() throws Exception {
        // Its spaces are followed by the tab to the right part, which Word draws them before.
        DocumentNode row = new com.demcha.compose.document.dsl.ShapeContainerBuilder()
                .name("Row")
                .rectangle(260, 14)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(new ParagraphBuilder().inlineText("ROLE", STYLE)
                        .inlineHighlight("  ", STYLE, DocumentColor.WHITE, 0, NO_PADDING).align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()).build(), 0, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder().text("2024").align(TextAlign.RIGHT).margin(DocumentInsets.zero())
                        .build(), 0, 0, LayerAlign.CENTER_RIGHT)
                .build();
        Exported exported = export(page -> page.add(row));

        assertThat(exported.textOf("ROLE")).isEqualTo("ROLE  \t2024");
        assertThat(exported.chipNotes()).noneMatch(note -> note.contains(LIBREOFFICE));
    }

    @Test
    void aZonesChipsAreFilledInEachHeaderTheyAreWrittenIntoAndNamedOnce() throws Exception {
        // A zone built otherwise for the first page is written into two kinds of header.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(300, 300).margin(DocumentInsets.of(30))
                .create()) {
            session.chrome().zone(DocumentPageZone.header(24, page -> new ParagraphBuilder()
                    .inlineText(page.isFirst() ? "Cover" : "Draft", STYLE)
                    .inlineHighlight("  ", STYLE, DocumentColor.WHITE, 0, NO_PADDING).build()));
            session.pageFlow(page -> page.addParagraph("One").addPageBreak(b -> { }).addParagraph("Two"));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            List<String> headers = document.getHeaderList().stream()
                    .map(org.apache.poi.xwpf.usermodel.XWPFHeader::getText).filter(text -> !text.isBlank()).toList();
            assertThat(headers).hasSizeGreaterThanOrEqualTo(2)
                    .allSatisfy(text -> assertThat(text).contains(NBSP).doesNotContain("  "));
        }
        List<DocxExportReport.Note> named = report.get().notes().stream()
                .filter(note -> note.subject().equals("inline chip") && note.detail().contains(LIBREOFFICE)).toList();
        assertThat(named).as("a zone's chips have no path: named once between them").singleElement()
                .satisfies(note -> assertThat(note.path()).isNull());
    }

    @Test
    void aPaddedZoneChipKeepsOneNoteThatSaysBoth() throws Exception {
        // Its note names its padding where the zone's line was first written; the line written again
        // gives no note, and the LibreOffice clause goes into that one.
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document().pageSize(300, 300).margin(DocumentInsets.of(30))
                .create()) {
            session.chrome().zone(DocumentPageZone.header(24, page -> new ParagraphBuilder()
                    .inlineText(page.isFirst() ? "Cover" : "Draft", STYLE)
                    .inlineHighlight("  ", STYLE, DocumentColor.WHITE, 0, new DocumentInsets(2, 0, 2, 0)).build()));
            session.pageFlow(page -> page.addParagraph("One").addPageBreak(b -> { }).addParagraph("Two"));
            session.export(new DocxSemanticBackend(report::set));
        }

        assertThat(report.get().notes()).filteredOn(note -> note.subject().equals("inline chip")).singleElement()
                .satisfies(note -> assertThat(note.detail()).contains("padding above and below").contains(LIBREOFFICE));
    }

    // -- helpers --------------------------------------------------------------------------------

    private static XWPFRun text(XWPFParagraph para, String text, boolean shaded) {
        XWPFRun run = para.createRun();
        run.setText(text);
        if (shaded) {
            shade(run.getCTR());
        }
        return run;
    }

    private static void shade(CTR run) {
        CTShd shading = (run.isSetRPr() ? run.getRPr() : run.addNewRPr()).addNewShd();
        shading.setVal(STShd.CLEAR);
        shading.setColor("auto");
        shading.setFill("FFFFFF");
    }

    private static Exported export(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(300, 300).margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get());
    }

    private record Exported(XWPFDocument document, DocxExportReport report) {

        String textOf(String containing) {
            List<XWPFParagraph> all = new java.util.ArrayList<>(document.getParagraphs());
            document.getTables().forEach(table -> table.getRows().forEach(row -> row.getTableCells()
                    .forEach(cell -> all.addAll(cell.getParagraphs()))));
            return all.stream().map(XWPFParagraph::getText).filter(text -> text.contains(containing))
                    .findFirst().orElseThrow();
        }

        List<String> chipNotes() {
            return report.notes().stream().filter(note -> note.subject().equals("inline chip"))
                    .map(DocxExportReport.Note::detail).toList();
        }
    }
}
