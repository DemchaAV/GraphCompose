package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTabStop;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A page zone's row keeps its columns on the one line of Word's header or footer it is written as:
 * each part stands at a tab stop where the page sets it, so a label and the page number beside it
 * do not run together as one word.
 *
 * <p>The page here is 300pt wide with 36pt margins, so the zone's line is 228pt long. A row of two
 * parts and an 8pt gap sets them in two columns of (228 - 8) / 2 = 110pt, the second starting
 * 118pt in from the left margin.</p>
 */
class DocxZoneRowStopsTest {

    private static final DocumentTextStyle CHROME = DocumentTextStyle.DEFAULT.withSize(8);

    @Test
    void aPageNumberBesideALabelStandsAtItsColumnsStart() throws Exception {
        Footer footer = footer(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line").spacing(8)
                .addParagraph(p -> p.text("Kept blocks").textStyle(CHROME))
                .add(page.pageNumber(CHROME))
                .build()));

        assertThat(footer.stops()).containsExactly("left@" + twips(118));
        // The label at the left margin needs no tab; the number has one, which carries it to its stop.
        assertThat(footer.runs()).containsExactly("Kept blocks", "\t", "field");
        assertThat(footer.notes()).isEmpty();
    }

    @Test
    void eachPartIsHeldByItsOwnAlignmentInItsColumn() throws Exception {
        // Right: its end, at the column's end, the right margin. Centre: its middle, at the
        // column's middle, 118 + 110 / 2 = 173pt in.
        assertThat(footer(twoColumns(TextAlign.RIGHT)).stops()).containsExactly("right@" + twips(228));
        assertThat(footer(twoColumns(TextAlign.CENTER)).stops()).containsExactly("center@" + twips(173));
        assertThat(footer(twoColumns(TextAlign.LEFT)).stops()).containsExactly("left@" + twips(118));
    }

    @Test
    void aPageFieldIsHeldByItsStartWhateverItsAlignment() throws Exception {
        // The page sets a field in a box as wide as its number and a point, at its column's start,
        // and aligns the number within that box only: on a page where the number is wider, the
        // box grows to the right. Measured over twelve pages in Word, a right stop at the first
        // page's end set "10" 4.4pt left of the page; a left stop at its start holds it.
        Footer footer = footer(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line").spacing(8)
                .addParagraph(p -> p.text("Kept blocks").textStyle(CHROME))
                .add(new com.demcha.compose.document.node.PageFieldNode("Number",
                        com.demcha.compose.document.node.PageFieldKind.NUMBER, CHROME, TextAlign.RIGHT,
                        DocumentInsets.zero(), DocumentInsets.zero()))
                .build()));

        assertThat(footer.stops()).singleElement().asString().startsWith("left@");
    }

    @Test
    void aPartAfterAGrowSpacerIsHeldByItsEndAndTheSpacerWritesNoTabOfItsOwn() throws Exception {
        // Whatever its alignment, the spacer carries it to the row's right end. The number, first
        // after it, grows to the left into the spacer's room on a page where it is wider, as the
        // page grows it, and moves nothing after it.
        Footer footer = footer(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line").spacing(8)
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                .flexSpacer()
                .add(page.pageNumber(CHROME))
                .addParagraph(p -> p.text("v2.4").textStyle(CHROME))
                .build()));

        assertThat(footer.stops()).hasSize(2).allMatch(stop -> stop.startsWith("right@"));
        assertThat(footer.stops().get(1)).isEqualTo("right@" + twips(228));
        assertThat(footer.runs()).containsExactly("Confidential", "\t", "field", "\t", "v2.4");
        assertThat(footer.notes()).as("the gap the page keeps after the number is kept too").isEmpty();
    }

    @Test
    void aRowSizedToItsContentThatAWiderPageNumberMovesIsNotHeldAtStops() throws Exception {
        // The page moves "of" and the total right on a page where the number is wider, and "v2.4"
        // left; a stop would hold them where the first page sets them, and the number would run
        // into them. Such a row keeps its one right tab at the margin.
        for (DocumentPageZone zone : List.of(
                DocumentPageZone.footer(30, page -> new RowBuilder().name("Line").spacing(4)
                        .addParagraph(p -> p.text("Page").textStyle(CHROME))
                        .add(page.pageNumber(CHROME))
                        .addParagraph(p -> p.text("of").textStyle(CHROME))
                        .add(page.pageTotal(CHROME))
                        .flexSpacer()
                        .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                        .build()),
                DocumentPageZone.footer(30, page -> new RowBuilder().name("Line").spacing(4)
                        .flexSpacer()
                        .addParagraph(p -> p.text("v2.4").textStyle(CHROME))
                        .add(page.pageNumber(CHROME))
                        .build()),
                DocumentPageZone.footer(30, page -> new RowBuilder().name("Line").spacing(4)
                        .addParagraph(p -> p.text("Page").textStyle(CHROME))
                        .flexSpacer()
                        .add(page.pageNumber(CHROME))
                        .flexSpacer()
                        .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                        .build()))) {
            assertThat(footer(zone).stops()).containsExactly("right@" + twips(228));
        }
    }

    @Test
    void aRigidSpacerIsAGapNotACarryToTheRight() throws Exception {
        // A spacer that does not grow sets what follows it a gap further on: held by its
        // alignment, a left stop at its start, not by its end.
        Footer footer = footer(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                .addSpacer(10)
                .addParagraph(p -> p.text("Acme").textStyle(CHROME))
                .build()));

        assertThat(footer.stops()).singleElement().asString().startsWith("left@");
        assertThat(footer.notes()).isEmpty();
    }

    @Test
    void aRowOfOnePartAtTheLeftMarginHoldsNoEmptyListOfStops() throws Exception {
        // Word's schema asks a list of tab stops for one at least: the line keeps its right tab.
        Footer footer = footer(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                .build()));

        assertThat(footer.stops()).containsExactly("right@" + twips(228));
    }

    @Test
    void aPartAfterAnUnwrittenPrefixAtARightStopEndsWhereThePageEndsIt() throws Exception {
        // Word does not write the prefix; the part ends at its stop all the same, and only the
        // prefix's letters are named.
        Footer footer = footer(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line")
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME))
                .flexSpacer()
                .addParagraph(p -> p.text("Acme").textStyle(CHROME).bulletOffset("• ")
                        .indentStrategy(com.demcha.compose.document.style.DocumentTextIndent.FIRST_LINE))
                .build()));

        assertThat(footer.stops()).containsExactly("right@" + twips(228));
        assertThat(footer.notes()).containsExactly("a footer written as one line of Word's footer; a paragraph's "
                                                   + "bulletOffset letters, \"•\", are not written before its first line");
    }

    @Test
    void aRowOfAPartOfTwoLinesIsNotHeldAtStops() throws Exception {
        // Past its first line Word sets the rest on later lines: there is no one place to carry a
        // part to, and the line keeps its one right tab at the margin.
        Footer footer = footer(DocumentPageZone.footer(40, page -> new RowBuilder().name("Line").spacing(8)
                .addParagraph(p -> p.text("A label long enough to take two lines in its column of the row")
                        .textStyle(CHROME))
                .add(page.pageNumber(CHROME))
                .build()));

        assertThat(footer.stops()).containsExactly("right@" + twips(228));
        assertThat(footer.runs()).containsExactly("A label long enough to take two lines in its column of the row",
                "field");
    }

    @Test
    void aPartSetRightWhereTheOneBeforeItEndsFollowsItWithNoTab() throws Exception {
        // The first ends at its column's end, 114pt in, and the second starts there: Word's tab
        // goes to the first stop past the pen, so the second has none, and follows the first.
        Footer footer = footer(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line").spacing(0)
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME).align(TextAlign.RIGHT))
                .addParagraph(p -> p.text("Acme").textStyle(CHROME))
                .build()));

        assertThat(footer.stops()).containsExactly("right@" + twips(114));
        assertThat(footer.runs()).containsExactly("\t", "Confidential", "Acme");
        assertThat(footer.notes()).isEmpty();
    }

    @Test
    void aPartThePartBeforeItReachesPastIsNamed() throws Exception {
        // At 8.26pt the label, about 43.6pt long, fits its 44pt column on one line; Word sets it at
        // the half point above, 8.5pt, about 44.9pt long, past the stop the number stands at, and
        // the tab carries the number on to a later one.
        // Where Word sets it then, and the part after it, is not one the page tells.
        Footer footer = footer(DocumentPageZone.footer(30, page -> new RowBuilder().name("Line").spacing(0)
                .columns(DocumentRowColumn.fixed(44), DocumentRowColumn.fixed(44), DocumentRowColumn.weight(1))
                .addParagraph(p -> p.text("Confidential").textStyle(CHROME.withSize(8.26)))
                .add(page.pageNumber(CHROME))
                .addParagraph(p -> p.text("Acme").textStyle(CHROME))
                .build()));

        assertThat(footer.stops()).as("held at stops").containsExactly("left@" + twips(44), "left@" + twips(88));
        assertThat(footer.notes()).containsExactly("a footer written as one line of Word's footer; 1 of its 3 parts "
                                                   + "stands off where the page sets them; where 1 of its 3 parts stands is not measured");
    }

    private static DocumentPageZone twoColumns(TextAlign align) {
        return DocumentPageZone.footer(30, page -> new RowBuilder().name("Line").spacing(8)
                .addParagraph(p -> p.text("Kept blocks").textStyle(CHROME))
                .addParagraph(p -> p.text("Page").textStyle(CHROME).align(align))
                .build());
    }

    private static long twips(double points) {
        return Math.round(points * 20);
    }

    /** The zone's line as written: its tab stops, its runs, and the report's notes on it. */
    private record Footer(List<String> stops, List<String> runs, List<String> notes) {
    }

    private static Footer footer(DocumentPageZone zone) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(36))
                .create()) {
            session.chrome().zone(zone);
            session.pageFlow(page -> page.addParagraph("Body"));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            XWPFParagraph line = (zone.getZone() == com.demcha.compose.document.output.DocumentHeaderFooterZone.HEADER
                    ? document.getHeaderArray(0) : document.getFooterArray(0)).getParagraphs().get(0);
            List<String> stops = new ArrayList<>();
            for (CTTabStop stop : line.getCTP().getPPr().getTabs().getTabList()) {
                stops.add(stop.getVal() + "@" + DocxTwips.of(stop.getPos()));
            }
            List<String> runs = new ArrayList<>();
            boolean inField = false;
            for (CTR run : line.getCTP().getRList()) {
                if (!run.getFldCharList().isEmpty()) {
                    if (!inField) {
                        runs.add("field");
                    }
                    inField = run.getFldCharArray(0).getFldCharType().toString().equals("begin")
                              || inField && !run.getFldCharArray(0).getFldCharType().toString().equals("end");
                } else if (!inField && !run.getTabList().isEmpty()) {
                    runs.add("\t");
                } else if (!inField && !run.getTList().isEmpty()) {
                    runs.add(run.getTArray(0).getStringValue());
                }
            }
            List<String> notes = report.get().bySubject().getOrDefault("page zone", List.of()).stream()
                    .map(DocxExportReport.Note::detail).toList();
            return new Footer(stops, runs, notes);
        }
    }
}
