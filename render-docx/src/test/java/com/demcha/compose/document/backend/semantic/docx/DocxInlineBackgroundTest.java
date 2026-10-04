package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An inline chip is a fill behind a phrase, and Word has one.
 *
 * <p>An inline {@code code} span and a status badge both exported as bare text: the
 * reduction to text runs keeps the glyphs and drops the chip, so a red badge reading
 * "overdue" came out the same colour as the sentence around it — a document losing the
 * part of itself that was doing the talking.</p>
 *
 * <p>Word shades a run with {@code w:shd}, which takes any RGB. What it cannot express is
 * the chip's shape: shading covers the glyph box, so the rounded corners go and the export
 * says so. The room its padding takes beside its letters is written as character spacing
 * after a letter, shaded on the chip's right only. A {@code w:shd} fill is also opaque, so a
 * translucent chip is flattened against what Word paints beneath it first.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxInlineBackgroundTest {

    private static final DocumentColor BADGE = DocumentColor.rgb(214, 56, 56);
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);

    @Test
    void aChipsFillBecomesTheRunsShading() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .inlineText("Status: ")
                        .inlineHighlight("overdue", DocumentTextStyle.DEFAULT, BADGE,
                                0, DocumentInsets.zero())))) {

            assertThat(fillOf(runReading(document, "overdue")))
                    .as("an opaque fill is written exactly as the document gave it")
                    .isEqualTo("D63838");
            assertThat(fillOf(runReading(document, "Status: ")))
                    .as("the sentence around it is not shaded")
                    .isNull();
        }
    }

    @Test
    void aTranslucentChipIsFlattenedAgainstThePage() throws Exception {
        // The chip this sugar reaches for most is a fifth-opacity grey, and w:shd has no
        // alpha. Written at full strength it is a solid slab where the page has a tint.
        // 175/184/193 at 20% over white is 239/241/243 — the same composite the PDF makes.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.inlineText("Call ").inlineCode("render()")))) {

            assertThat(fillOf(runReading(document, "render()"))).isEqualTo("EFF1F3");
        }
    }

    @Test
    void aChipInsideAShadedCardIsFlattenedAgainstTheCard() throws Exception {
        // Flattening against the page would be flattening against something that is not
        // under it: the card is a cell shaded with its fill, and Word paints the run's over it.
        try (XWPFDocument document = exported(page -> page
                .addSection("Card", card -> card
                        .fillColor(SURFACE)
                        .addParagraph(p -> p.inlineText("Call ").inlineCode("render()"))))) {

            // 175/184/193 at 20% over 238/243/249 is 225/231/238.
            assertThat(fillOf(runReading(document, "render()"))).isEqualTo("E1E7EE");
        }
    }

    @Test
    void aChipInARowInsideAShadedCardIsFlattenedAgainstTheCard() throws Exception {
        // The row's cells carry no shading, so the cell the chip is in says nothing about
        // what is under it: the card's fill shows through them.
        try (XWPFDocument document = exported(page -> page
                .addSection("Card", card -> card
                        .fillColor(SURFACE)
                        .addRow(r -> r
                                .addParagraph(p -> p.inlineText("Call ").inlineCode("render()"))
                                .addParagraph(p -> p.text("Beside")))))) {

            assertThat(fillOf(runReading(document, "render()"))).isEqualTo("E1E7EE");
        }
    }

    @Test
    void aChipInsideAShadedCellIsFlattenedAgainstTheCell() throws Exception {
        // The cell carries the fill, not the paragraph inside it, so this is the branch
        // that would silently flatten against white and look almost right.
        try (XWPFDocument document = exported(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Call"),
                        DocumentTableCell.node(codeParagraph())
                                .withStyle(DocumentTableStyle.builder().fillColor(SURFACE).build()))))) {

            // 175/184/193 at 20% over 238/243/249 is 225/231/238.
            assertThat(fillOf(runReading(document, "render()"))).isEqualTo("E1E7EE");
        }
    }

    @Test
    void readingWhatIsUnderAChipPaintsNothing() throws Exception {
        // POI's cell-properties accessor creates the w:tcPr it cannot find, so asking an
        // unpainted cell what colour it is would leave an empty one behind.
        try (XWPFDocument document = exported(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.auto(), DocumentTableColumn.auto())
                .rowCells(DocumentTableCell.text("Call"),
                        DocumentTableCell.node(codeParagraph()))))) {

            assertThat(fillOf(runReading(document, "render()")))
                    .as("nothing underneath it, so the page's white")
                    .isEqualTo("EFF1F3");
            assertThat(document.getTables().get(0).getRow(0).getTableCells().stream()
                    .noneMatch(cell -> cell.getCTTc().getTcPr().isSetShd()))
                    .as("a cell nobody painted stays unpainted")
                    .isTrue();
        }
    }

    @Test
    void aChipKeepsItsTextAndItsStyle() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .inlineText("Run ")
                        .inlineHighlight("build", DocumentTextStyle.builder()
                                        .color(DocumentColor.WHITE).size(9).build(),
                                BADGE, 0, DocumentInsets.zero())
                        .inlineText(" first")))) {

            XWPFRun chip = runReading(document, "build");
            assertThat(chip.getColor()).as("the chip's own ink survives beside its fill").isEqualTo("FFFFFF");
            assertThat(document.getParagraphs().get(0).getText())
                    .as("the sentence still reads as one")
                    .isEqualTo("Run build first");
        }
    }

    @Test
    void aChipsRightPaddingIsSpaceAfterItsLastLetterInsideItsShading() throws Exception {
        // ModernReceipt's status chip, right-aligned with 9pt of padding on each side, stood
        // 9.2pt right of the page's in Word: shading covers the letters and nothing more.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .inlineHighlight("Completed", DocumentTextStyle.DEFAULT, BADGE,
                                9, new DocumentInsets(3.5, 9, 3.5, 9))
                        .align(com.demcha.compose.document.node.TextAlign.RIGHT)))) {

            List<XWPFRun> runs = document.getParagraphs().get(0).getRuns();
            assertThat(runs).extracting(XWPFRun::text).containsExactly("Complete", "d");
            assertThat(runs.get(1).getCharacterSpacing() - runs.get(0).getCharacterSpacing())
                    .as("the last letter is 9pt further from what follows, in twentieths")
                    .isEqualTo(180);
            assertThat(runs).extracting(DocxInlineBackgroundTest::fillOf)
                    .as("both shaded, so the room is inside the chip")
                    .containsExactly("D63838", "D63838");
        }
    }

    @Test
    void aChipsLeftPaddingIsSpaceAfterTheLetterBeforeIt() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .inlineText("Status:")
                        .inlineHighlight("due", DocumentTextStyle.DEFAULT, BADGE,
                                0, new DocumentInsets(0, 0, 0, 6))
                        .inlineText(" today")))) {

            List<XWPFRun> runs = document.getParagraphs().get(0).getRuns();
            assertThat(runs).extracting(XWPFRun::text).containsExactly("Status", ":", "due", " today");
            assertThat(runs.get(1).getCharacterSpacing()).as("6pt after the colon").isEqualTo(120);
            assertThat(fillOf(runs.get(1))).as("outside the chip, so not shaded").isNull();
            assertThat(runs.get(2).getCharacterSpacing()).as("no right padding, nothing after it").isZero();
            assertThat(document.getParagraphs().get(0).getText()).isEqualTo("Status:due today");
        }
    }

    @Test
    void twoChipsSideBySideAreSpacedByBothTheirPaddings() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .inlineHighlight("one", DocumentTextStyle.DEFAULT, BADGE,
                                0, new DocumentInsets(0, 2, 0, 0))
                        .inlineHighlight("two", DocumentTextStyle.DEFAULT, BADGE,
                                0, new DocumentInsets(0, 0, 0, 3))))) {

            List<XWPFRun> runs = document.getParagraphs().get(0).getRuns();
            assertThat(runs).extracting(XWPFRun::text).containsExactly("on", "e", "two");
            assertThat(runs.get(1).getCharacterSpacing()).as("2pt + 3pt").isEqualTo(100);
        }
    }

    @Test
    void aChipInAListItemIsSpacedByItsPadding() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addList(list -> list
                        .bullet()
                        .hangingIndent(true)
                        .addItem(rich -> rich.plain("Invoice ").highlight("overdue",
                                DocumentTextStyle.DEFAULT, BADGE, 0, new DocumentInsets(0, 4, 0, 0)))))) {

            XWPFRun head = runReading(document, "overdue");
            assertThat(head.text()).isEqualTo("overdu");
            XWPFParagraph item = (XWPFParagraph) head.getParent();
            XWPFRun last = item.getRuns().get(item.getRuns().indexOf(head) + 1);
            assertThat(last.getCharacterSpacing()).isEqualTo(80);
        }
    }

    @Test
    void aCentredLinesLettersAreSpacedOnTopOfAChipsPadding() throws Exception {
        // 8.3pt is set at 8.5 in Word, so a centred line of it is spaced back to the page's
        // width; that spacing goes on every letter, the padding's on the last one too.
        DocumentTextStyle small = DocumentTextStyle.builder().size(8.3).build();
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .inlineHighlight("Settled by the beneficiary bank today", small, BADGE,
                                0, new DocumentInsets(0, 5, 0, 0))
                        .align(com.demcha.compose.document.node.TextAlign.CENTER)))) {

            List<XWPFRun> runs = document.getParagraphs().get(0).getRuns();
            assertThat(runs).hasSize(2);
            assertThat(runs.get(0).getCharacterSpacing()).as("the line is spaced to the page's").isNotZero();
            assertThat(runs.get(1).getCharacterSpacing() - runs.get(0).getCharacterSpacing())
                    .isEqualTo(100);
        }
    }

    @Test
    void aChipWithNoPaddingStaysOneRun() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .inlineText("Status: ")
                        .inlineHighlight("due", DocumentTextStyle.DEFAULT, BADGE, 0, DocumentInsets.zero())
                        .inlineText(" today")))) {

            List<XWPFRun> runs = document.getParagraphs().get(0).getRuns();
            assertThat(runs).extracting(XWPFRun::text).containsExactly("Status: ", "due", " today");
            assertThat(runs).extracting(XWPFRun::getCharacterSpacing).containsOnly(0);
        }
    }

    @Test
    void aLinkedPaddedChipIsOneLink() throws Exception {
        // The last letter's run goes in the link its letters are in: a second w:hyperlink would
        // make the phrase two links to a reader and to Word's Edit Hyperlink.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.inlineHighlight("Docs", DocumentTextStyle.DEFAULT,
                        BADGE, 0, new DocumentInsets(0, 4, 0, 0),
                        new DocumentLinkOptions("https://graphcompose.dev"))))) {

            var links = document.getParagraphs().get(0).getCTP().getHyperlinkList();
            assertThat(links).hasSize(1);
            assertThat(links.get(0).sizeOfRArray()).isEqualTo(2);
            assertThat(document.getParagraphs().get(0).getText()).isEqualTo("Docs");
        }
    }

    @Test
    void theLastLetterKeepsItsAccent() throws Exception {
        // A decomposed é is two code points, one letter: split between them, the accent would
        // be shaped in a run of its own.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.inlineHighlight("Resumé", DocumentTextStyle.DEFAULT,
                        BADGE, 0, new DocumentInsets(0, 4, 0, 0))))) {

            assertThat(document.getParagraphs().get(0).getRuns())
                    .extracting(XWPFRun::text).containsExactly("Resum", "é");
        }
    }

    @Test
    void aChipsPaddingIsNotGrownToWordsSize() throws Exception {
        // 8.3pt letters are set at 8.5 in Word, and the line is spaced back to the page's
        // width when it grows a point or more. The padding is written in points, not grown:
        // reckoned at Word's size, 80pt of it alone would have grown the line 1.9pt.
        DocumentTextStyle small = DocumentTextStyle.builder().size(8.3).build();
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .inlineHighlight("Paid", small, BADGE, 0, new DocumentInsets(0, 40, 0, 40))
                        .align(com.demcha.compose.document.node.TextAlign.CENTER)))) {

            List<XWPFRun> runs = document.getParagraphs().get(0).getRuns();
            assertThat(runs).extracting(XWPFRun::text).containsExactly("Pai", "d");
            assertThat(runs.get(0).getCharacterSpacing()).as("four letters grow under a point").isZero();
            assertThat(runs.get(1).getCharacterSpacing()).isEqualTo(800);
        }
    }

    @Test
    void aChipInAListItemTakesItsLeftPaddingAfterTheTextBeforeIt() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addList(list -> list
                        .bullet()
                        .hangingIndent(true)
                        .addItem(rich -> rich.plain("Invoice:").highlight("overdue",
                                DocumentTextStyle.DEFAULT, BADGE, 0, new DocumentInsets(0, 0, 0, 6)))))) {

            XWPFRun colon = runReading(document, ":");
            assertThat(colon.getCharacterSpacing()).isEqualTo(120);
        }
    }

    @Test
    void whatAChipsPaddingLosesIsSaidAsItIs() throws Exception {
        DocumentInsets sides = new DocumentInsets(0, 4, 0, 4);
        assertThat(reportOf(page -> page.addParagraph(p -> p
                        .inlineHighlight("due", DocumentTextStyle.DEFAULT, BADGE, 0, sides)))
                .bySubject().get("inline chip").get(0).detail())
                .as("left to right: the left side is unshaded space, the right is written")
                .contains("left padding is unshaded space")
                .doesNotContain("right padding");
        assertThat(reportOf(page -> page.addParagraph(p -> p
                        .direction(TextDirection.RTL)
                        .inlineHighlight("דחוף", DocumentTextStyle.DEFAULT, BADGE, 0, sides)))
                .bySubject().get("inline chip").get(0).detail())
                .contains("padding beside its letters is not in the file");
    }

    @Test
    void textBrokenOverLinesBeforeAChipSpacesTheLastLetterOfItsLastLine() throws Exception {
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .inlineText("Invoice 42\nStatus:")
                        .inlineHighlight("due", DocumentTextStyle.DEFAULT, BADGE,
                                0, new DocumentInsets(0, 0, 0, 6))))) {

            List<XWPFRun> runs = document.getParagraphs().get(0).getRuns();
            assertThat(runs).extracting(XWPFRun::text).containsExactly("Invoice 42\nStatus", ":", "due");
            assertThat(runs.get(1).getCharacterSpacing()).isEqualTo(120);
        }
    }

    @Test
    void aChipInARightToLeftParagraphIsNotSpaced() throws Exception {
        // Spacing goes after a letter in the run's order, which is not the side the
        // padding stands on in a right-to-left line.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .direction(TextDirection.RTL)
                        .inlineHighlight("דחוף", DocumentTextStyle.DEFAULT, BADGE,
                                0, new DocumentInsets(0, 4, 0, 4))))) {

            List<XWPFRun> runs = document.getParagraphs().get(0).getRuns();
            assertThat(runs).extracting(XWPFRun::text).containsExactly("דחוף");
            assertThat(runs.get(0).getCharacterSpacing()).isZero();
        }
    }

    @Test
    void aLinkedChipIsStillALink() throws Exception {
        // The chip and the link are written by different parts of the run path, and the
        // one that knows about the fill must not be the one that forgets the address.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.inlineHighlight("docs", DocumentTextStyle.DEFAULT,
                        BADGE, 0, DocumentInsets.zero(),
                        new DocumentLinkOptions("https://graphcompose.dev"))))) {

            assertThat(fillOf(runReading(document, "docs"))).isEqualTo("D63838");
            assertThat(document.getParagraphs().get(0).getCTP().getHyperlinkList())
                    .as("a chip carrying a link is exported as a hyperlink")
                    .hasSize(1);
        }
    }

    @Test
    void aChipInsideAListItemIsAChip() throws Exception {
        // A badge in a bulleted list is a badge for the same reason it is one in a
        // paragraph: the list path writes its own runs and used to write them plain.
        // hangingIndent, because an item made of runs is laid out only with the marker
        // column — without it this document cannot be laid out at all, and the assertion
        // would be proving the chip on the export's no-layout fallback instead.
        try (XWPFDocument document = exported(page -> page
                .addList(list -> list
                        .bullet()
                        .hangingIndent(true)
                        .addItem(rich -> rich.plain("Invoice ").highlight("overdue",
                                DocumentTextStyle.DEFAULT, BADGE, 0, DocumentInsets.zero()))))) {

            assertThat(fillOf(runReading(document, "overdue"))).isEqualTo("D63838");
        }
    }

    @Test
    void theShapeAChipLosesIsRecorded() throws Exception {
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.inlineText("Call ").inlineCode("render()")));

        assertThat(report.count(DocxExportReport.Severity.DROPPED)).isZero();
        assertThat(report.bySubject()).containsKey("inline chip");
        DocxExportReport.Note note = report.bySubject().get("inline chip").get(0);
        assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.APPROXIMATED);
        assertThat(note.detail())
                .as("the fill is written; what goes is the shape around it")
                .contains("rounded corners")
                .contains("left padding")
                .contains("above and below");
    }

    @Test
    void aFlattenedFillIsRecordedEvenWhenTheColourIsRight() throws Exception {
        // The colour on the page is right, so it is tempting to call this lossless. What
        // is gone is the translucency: shade that paragraph another colour in Word and a
        // chip that was a tint over it stays the tint it was flattened to.
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.inlineHighlight("overdue", DocumentTextStyle.DEFAULT,
                        BADGE.withOpacity(0.3), 0, DocumentInsets.zero())));

        assertThat(report.bySubject().get("inline chip").get(0).detail())
                .contains("flattened");
    }

    @Test
    void aChipInARightToLeftParagraphKeepsBothItsFillAndItsDirection() throws Exception {
        // The direction and the shading are written into the same w:rPr by two different
        // calls, and the second must not be the one that discards the first.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p
                        .direction(TextDirection.RTL)
                        .inlineHighlight("דחוף", DocumentTextStyle.DEFAULT, BADGE,
                                0, DocumentInsets.zero())))) {

            XWPFRun chip = runReading(document, "דחוף");
            assertThat(fillOf(chip)).isEqualTo("D63838");
            assertThat(chip.getCTR().getRPr().sizeOfRtlArray())
                    .as("the run is still declared right-to-left")
                    .isPositive();
        }
    }

    @Test
    void aSquareChipWithNoPaddingLosesNothingAndSaysNothing() throws Exception {
        // The report is a record of loss. A chip Word can hold exactly must not appear in
        // it, or a caller reading the report cannot tell the two cases apart.
        DocxExportReport report = reportOf(page -> page
                .addParagraph(p -> p.inlineHighlight("overdue", DocumentTextStyle.DEFAULT,
                        BADGE, 0, DocumentInsets.zero())));

        assertThat(report.isEmpty()).isTrue();
    }

    @Test
    void aParagraphWhoseRunsCarryNoTextStillReadsAsItsText() throws Exception {
        // The runs are walked in their authored form now, and an image run carries no text
        // at all. The paragraph's own text is what it reads, and it must still be written.
        try (XWPFDocument document = exported(page -> page
                .addParagraph(p -> p.text("The whole line")))) {

            assertThat(document.getParagraphs().get(0).getText()).isEqualTo("The whole line");
        }
    }

    /** A paragraph carrying the default code chip, for a cell to be built from. */
    private static com.demcha.compose.document.node.DocumentNode codeParagraph() {
        return new com.demcha.compose.document.dsl.ParagraphBuilder()
                .name("Call")
                .inlineCode("render()")
                .build();
    }

    private static String fillOf(XWPFRun run) {
        if (!run.getCTR().isSetRPr() || run.getCTR().getRPr().sizeOfShdArray() == 0) {
            return null;
        }
        CTShd shading = run.getCTR().getRPr().getShdArray(0);
        // XmlBeans hands a written hex colour back as its three bytes, not as the string
        // it was set from.
        Object fill = shading.getFill();
        return fill instanceof byte[] bytes
                ? java.util.HexFormat.of().withUpperCase().formatHex(bytes)
                : null;
    }

    /**
     * The run reading {@code text}, or the first of two that do: a padded chip's last letter is
     * a run of its own.
     */
    private static XWPFRun runReading(XWPFDocument document, String text) {
        for (XWPFParagraph para : everyParagraph(document)) {
            List<XWPFRun> runs = para.getRuns();
            for (int index = 0; index < runs.size(); index++) {
                String read = runs.get(index).text();
                if (text.equals(read)
                    || index + 1 < runs.size() && text.equals(read + runs.get(index + 1).text())) {
                    return runs.get(index);
                }
            }
        }
        throw new AssertionError("no run reading '" + text + "' among "
                + everyParagraph(document).stream().flatMap(p -> p.getRuns().stream())
                        .map(r -> "'" + r.text() + "'").toList());
    }

    /** Body paragraphs and the ones inside table cells, which the body list leaves out. */
    private static List<XWPFParagraph> everyParagraph(XWPFDocument document) {
        List<XWPFParagraph> paragraphs = new ArrayList<>(document.getParagraphs());
        addTableParagraphs(document.getTables(), paragraphs);
        return paragraphs;
    }

    /** A card is a table, and what it holds can be a table in its cell, so this descends. */
    private static void addTableParagraphs(List<XWPFTable> tables, List<XWPFParagraph> into) {
        for (XWPFTable table : tables) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    into.addAll(cell.getParagraphs());
                    addTableParagraphs(cell.getTables(), into);
                }
            }
        }
    }

    private static XWPFDocument exported(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 600, 20, content);
    }

    private static DocxExportReport reportOf(Consumer<PageFlowBuilder> content) throws Exception {
        AtomicReference<DocxExportReport> captured = new AtomicReference<>();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(content::accept);
            session.export(new DocxSemanticBackend(captured::set));
        }
        return captured.get();
    }
}
