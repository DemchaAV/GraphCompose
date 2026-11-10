package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.xmlbeans.XmlCursor;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A hyphen in the word the page starts a line with is written as Word's non-breaking hyphen, and
 * no other hyphen is.
 *
 * <p>The page breaks a line only at a space, and Word and LibreOffice also after a hyphen between
 * two letters or digits: where the page carried {@code developer-facing} whole to its next line,
 * both editors set {@code developer-} at the end of the line above, and every line of the
 * paragraph after it broke at other words. Measured on the fidelity corpus in Word 16 and
 * LibreOffice 24.2: each of the six documents short of the lines found — five CVs and a letter —
 * lost its lines to that alone, and with the line-starting words' hyphens non-breaking, five
 * lost none in either editor and the sixth lost two, to a line Word sets wider. Both editors
 * carry the non-breaking hyphen into their PDF as an ordinary one.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxLineStartHyphensTest {

    /** What the paragraph holds, a non-breaking hyphen read as {@code ~} and a break as {@code |}. */
    private static String written(CTP paragraph) {
        StringBuilder text = new StringBuilder();
        try (XmlCursor runs = paragraph.newCursor()) {
            runs.selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' "
                            + "./w:r | ./w:hyperlink/w:r");
            while (runs.toNextSelection()) {
                try (XmlCursor child = runs.getObject().newCursor()) {
                    if (!child.toFirstChild()) {
                        continue;
                    }
                    do {
                        switch (child.getName().getLocalPart()) {
                            case "t" -> text.append(child.getTextValue());
                            case "noBreakHyphen" -> text.append('~');
                            case "br" -> text.append('|');
                            default -> {
                            }
                        }
                    } while (child.toNextSibling());
                }
            }
        }
        return text.toString();
    }

    private static CTP paragraphOf(String... runs) {
        CTP paragraph = CTP.Factory.newInstance();
        for (String run : runs) {
            paragraph.addNewR().addNewT().setStringValue(run);
        }
        return paragraph;
    }

    private static List<ParagraphLine> lines(String... texts) {
        return Arrays.stream(texts).map(text -> new ParagraphLine(text, 100, 12, 12, 9, 3, List.of())).toList();
    }

    @Test
    void holdsTheHyphenOfTheWordTheNextLineStartsWith() {
        CTP paragraph = paragraphOf("Platform engineer and developer-facing systems");

        int held = DocxLineStartHyphens.hold(paragraph, lines("Platform engineer and", "developer-facing systems"));

        assertThat(held).isEqualTo(1);
        assertThat(written(paragraph)).isEqualTo("Platform engineer and developer~facing systems");
    }

    @Test
    void holdsEveryHyphenOfThatWordAndNoneAfterIt() {
        CTP paragraph = paragraphOf("Pixel diffing, side-by-side layout, slot-based");

        int held = DocxLineStartHyphens.hold(paragraph, lines("Pixel diffing,", "side-by-side layout, slot-based"));

        assertThat(held).isEqualTo(2);
        assertThat(written(paragraph)).isEqualTo("Pixel diffing, side~by~side layout, slot-based");
    }

    @Test
    void leavesTheFirstLinesFirstWord() {
        // Nothing stands above a paragraph's first line for an editor to set part of it on.
        CTP paragraph = paragraphOf("Well-tested services and more text");

        int held = DocxLineStartHyphens.hold(paragraph, lines("Well-tested services", "and more text"));

        assertThat(held).isZero();
        assertThat(written(paragraph)).isEqualTo("Well-tested services and more text");
    }

    @Test
    void leavesAWordThePageBreaksItself() {
        // The page broke a word too wide for its line at its hyphen: an editor breaks that word
        // its own way, and none of what the next line starts with is held.
        CTP paragraph = paragraphOf("see com.example-artifact-name-x now");

        int held = DocxLineStartHyphens.hold(paragraph, lines("see com.example-", "artifact-name-x now"));

        assertThat(held).isZero();
        assertThat(written(paragraph)).isEqualTo("see com.example-artifact-name-x now");
    }

    @Test
    void leavesAWordThePageBreaksItselfOnAnyLine() {
        // The word starts a line after the first and runs off its end onto the next: too wide for
        // the line, broken by the page. Held, it would leave an editor no place to break it but
        // between its letters.
        CTP paragraph = paragraphOf("see the graph-compose-render-docx module now");

        int held = DocxLineStartHyphens.hold(paragraph,
                lines("see the", "graph-compose-", "render-docx module now"));

        assertThat(held).isZero();
        assertThat(written(paragraph)).isEqualTo("see the graph-compose-render-docx module now");
    }

    @Test
    void readsATabAsSpace() {
        CTP paragraph = CTP.Factory.newInstance();
        CTR run = paragraph.addNewR();
        run.addNewT().setStringValue("Item");
        run.addNewTab();
        run.addNewT().setStringValue("well-tested parts");

        int held = DocxLineStartHyphens.hold(paragraph, lines("Item", "well-tested parts"));

        assertThat(held).isEqualTo(1);
        assertThat(written(paragraph)).isEqualTo("Itemwell~tested parts");
    }

    @Test
    void holdsOnlyAHyphenBetweenLettersOrDigits() {
        CTP paragraph = paragraphOf("Range a -flag x pre- 2023-2024");

        int held = DocxLineStartHyphens.hold(paragraph, lines("Range a", "-flag x", "pre- 2023-2024"));

        assertThat(held).isZero();
        assertThat(written(paragraph)).isEqualTo("Range a -flag x pre- 2023-2024");

        // Inside a word, beside a mark that is no letter: a doubled hyphen, one after a bracket.
        CTP marks = paragraphOf("Start of it well--known (x)-(y)");
        assertThat(DocxLineStartHyphens.hold(marks, lines("Start of it", "well--known (x)-(y)"))).isZero();
        assertThat(written(marks)).isEqualTo("Start of it well--known (x)-(y)");

        CTP dates = paragraphOf("Oracle Java Certification 2023-2024");
        assertThat(DocxLineStartHyphens.hold(dates, lines("Oracle Java Certification", "2023-2024"))).isEqualTo(1);
        assertThat(written(dates)).isEqualTo("Oracle Java Certification 2023~2024");
    }

    @Test
    void readsAWordAcrossItsRuns() {
        // A run's edge is no space: the word goes on into the next run, a bold or linked part of it.
        CTP paragraph = paragraphOf("Built for the ", "data", "-driven products");

        int held = DocxLineStartHyphens.hold(paragraph, lines("Built for the", "data-driven products"));

        assertThat(held).isEqualTo(1);
        assertThat(written(paragraph)).isEqualTo("Built for the data~driven products");
    }

    @Test
    void readsALinksRunsAndABreakAsSpace() {
        CTP paragraph = CTP.Factory.newInstance();
        CTR before = paragraph.addNewR();
        before.addNewT().setStringValue("Contact");
        before.addNewBr();
        paragraph.addNewHyperlink().addNewR().addNewT().setStringValue("on-call desk");

        int held = DocxLineStartHyphens.hold(paragraph, lines("Contact", "on-call desk"));

        assertThat(held).isEqualTo(1);
        assertThat(written(paragraph)).isEqualTo("Contact|on~call desk");
    }

    @Test
    void findsThePagesLinesInTextWrittenAroundThem() {
        // A list's marker written as characters before the item's text, which the page's lines
        // of the item do not hold.
        CTP paragraph = paragraphOf("  • ", "Testing with snapshot-based checks");

        int held = DocxLineStartHyphens.hold(paragraph, lines("Testing with", "snapshot-based checks"));

        assertThat(held).isEqualTo(1);
        assertThat(written(paragraph)).isEqualTo("  • Testing with snapshot~based checks");
    }

    @Test
    void findsTheWrittenTextInLinesThatHoldAMarkerWordDraws() {
        // A Word list's marker is drawn by its numbering, and the page's lines of the item hold it.
        CTP paragraph = paragraphOf("Analysis to support strategic decision-making.");

        int held = DocxLineStartHyphens.hold(paragraph,
                lines("· Analysis to support strategic", "decision-making."));

        assertThat(held).isEqualTo(1);
        assertThat(written(paragraph)).isEqualTo("Analysis to support strategic decision~making.");
    }

    @Test
    void anExportHoldsTheHyphenThatStartsAListItemsLine() throws Exception {
        String item = String.join(" ", java.util.Collections.nCopies(30, "data-driven"));
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addList(list -> list.items(item)))) {
            XWPFParagraph paragraph = document.getParagraphs().stream()
                    .filter(candidate -> candidate.getText().contains("data")).findFirst().orElseThrow();
            String written = written(paragraph.getCTP());
            assertThat(written.chars().filter(c -> c == '~').count()).isPositive();
            assertThat(written.replace('~', '-')).endsWith(item);
        }
    }

    @Test
    void anExportHoldsTheHyphenThatStartsARichListItemsLine() throws Exception {
        // A bulleted item of runs, as the CV presets write one: its marker in a column of its own.
        String item = String.join(" ", java.util.Collections.nCopies(30, "data-driven"));
        try (XWPFDocument document = DocxExports.withLayout(400, 600, 20, page -> page
                .addList(list -> list.marker("•").hangingIndent(true).markerGap(4).normalizeMarkers(false)
                        .addItem(text -> text.bold("Lead: ").plain(item))))) {
            XWPFParagraph paragraph = document.getParagraphs().stream()
                    .filter(candidate -> candidate.getText().contains("data")).findFirst().orElseThrow();
            String written = written(paragraph.getCTP());
            assertThat(written.chars().filter(c -> c == '~').count()).isPositive();
            assertThat(written.replace('~', '-')).endsWith(item);
        }
    }

    @Test
    void leavesAParagraphWhoseTextIsNotItsLinesLetters() {
        CTP paragraph = paragraphOf("Platform engineer and developer-facing systems");

        int held = DocxLineStartHyphens.hold(paragraph, lines("Platform engineer and", "developer-friendly systems"));

        assertThat(held).isZero();
        assertThat(written(paragraph)).isEqualTo("Platform engineer and developer-facing systems");
    }

    @Test
    void leavesAParagraphThatHoldsItsLinesTwice() {
        // Where the letters stand twice, which of them are the line's cannot be told.
        CTP paragraph = paragraphOf("ab ab-cd ab ab-cd");

        int held = DocxLineStartHyphens.hold(paragraph, lines("ab", "ab-cd"));

        assertThat(held).isZero();
    }

    @Test
    void leavesLinesSetRightToLeft() {
        CTP paragraph = paragraphOf("first line next-word end");
        ParagraphTextSpan one = new ParagraphTextSpan("next-word", null, 50, 12, null, null, true);
        ParagraphTextSpan two = new ParagraphTextSpan(" end", null, 20, 12, null, null, true);
        List<ParagraphLine> lines = List.of(
                new ParagraphLine("first line", 100, 12, 12, 9, 3, List.of()),
                new ParagraphLine("next-word end", 70, 12, 12, 9, 3, List.of(one, two), List.of(1, 0)));

        assertThat(DocxLineStartHyphens.hold(paragraph, lines)).isZero();
        assertThat(written(paragraph)).isEqualTo("first line next-word end");
    }

    @Test
    void writesTheHyphenAsWordsNonBreakingOneInTheRun() {
        CTP paragraph = paragraphOf("Platform engineer and developer-facing systems");

        DocxLineStartHyphens.hold(paragraph, lines("Platform engineer and", "developer-facing systems"));

        CTR run = paragraph.getRArray(0);
        assertThat(run.sizeOfNoBreakHyphenArray()).isEqualTo(1);
        assertThat(run.sizeOfTArray()).isEqualTo(2);
        assertThat(run.getTArray(0).getStringValue()).isEqualTo("Platform engineer and developer");
        assertThat(run.getTArray(1).getStringValue()).isEqualTo("facing systems");
        assertThat(run.xmlText()).contains("xml:space=\"preserve\"");
    }

    @Test
    void anExportHoldsTheHyphenOfEveryLineStartingWordOnly() throws Exception {
        // Every word hyphenated: the page breaks at the spaces between them, so each line but the
        // first starts with one, and only those are held.
        String text = String.join(" ", java.util.Collections.nCopies(40, "data-driven"));
        byte[] docx;
        int lines;
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 600).margin(com.demcha.compose.document.style.DocumentInsets.of(20)).create()) {
            session.pageFlow(page -> page.addParagraph(text));
            docx = session.export(new DocxSemanticBackend());
            DocxLayoutMetrics layout = DocxLayoutMetrics.of(session.documentGraph(), session.layoutGraph());
            com.demcha.compose.document.node.DocumentNode paragraphNode = session.documentGraph().roots().stream()
                    .flatMap(root -> java.util.stream.Stream.concat(java.util.stream.Stream.of(root),
                            root.children().stream()))
                    .filter(node -> node instanceof com.demcha.compose.document.node.ParagraphNode)
                    .findFirst().orElseThrow();
            lines = layout.lines(paragraphNode).size();
        }
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
            XWPFParagraph paragraph = document.getParagraphs().stream()
                    .filter(candidate -> candidate.getText().contains("data")).findFirst().orElseThrow();
            String written = written(paragraph.getCTP());
            assertThat(lines).as("the premise: the page sets the paragraph on several lines").isGreaterThan(2);
            assertThat(written.chars().filter(c -> c == '~').count())
                    .as("one held hyphen for each line after the first, each of which starts with one word of them")
                    .isEqualTo(lines - 1);
            assertThat(written.replace('~', '-')).isEqualTo(text);
        }
    }

    @Test
    void aPanelCutAtItsPageKeepsTheHyphensOfTheTextAfterItsTable() throws Exception {
        // A bordered section whose table runs onto a second page is cut there into two tables,
        // and what follows its table goes with the second: the paragraph written into the first
        // is taken out of it. Held before the cut, the copy carries the held hyphen with it.
        // A few lines, which stand whole on the second page: a block broken over pages beside the
        // table leaves the panel uncut.
        String closing = "Closing " + String.join(" ", java.util.Collections.nCopies(12, "data-driven"));
        try (XWPFDocument document = DocxExports.withLayout(400, 300, 20, page -> page.addSection(card -> {
            card.fillColor(com.demcha.compose.document.style.DocumentColor.rgb(238, 243, 249))
                    .stroke(com.demcha.compose.document.style.DocumentStroke.of(
                            com.demcha.compose.document.style.DocumentColor.rgb(26, 86, 148), 1))
                    .padding(com.demcha.compose.document.style.DocumentInsets.of(6));
            card.addTable(table -> {
                table.columns(com.demcha.compose.document.table.DocumentTableColumn.fixed(200),
                        com.demcha.compose.document.table.DocumentTableColumn.fixed(100));
                table.header("Item", "Amount").repeatHeader();
                for (int row = 1; row <= 20; row++) {
                    table.row("Row " + row, String.valueOf(row));
                }
            });
            card.addParagraph(closing);
        }))) {
            assertThat(document.getTables()).as("the premise: the panel is cut at its page").hasSizeGreaterThan(1);
            // The last part, which the cut copies the text after the table into.
            String written = document.getTables().get(document.getTables().size() - 1).getRows().stream()
                    .flatMap(row -> row.getTableCells().stream())
                    .flatMap(cell -> cell.getParagraphs().stream())
                    .filter(paragraph -> paragraph.getText().startsWith("Closing"))
                    .map(paragraph -> written(paragraph.getCTP()))
                    .findFirst().orElseThrow();
            assertThat(written.chars().filter(c -> c == '~').count()).isPositive();
            assertThat(written.replace('~', '-')).isEqualTo(closing);
        }
    }

    @Test
    void aTableSplitForItsPagesMarginsKeepsTheHyphensOfTheRowsItMoves() throws Exception {
        // A page with margins of its own starts a section, and a table running onto it is split
        // there: the rows of the later page move to a table of their own. Held before the split,
        // the moved row's paragraph carries its held hyphen with it.
        String text = String.join(" ", java.util.Collections.nCopies(12, "data-driven"));
        byte[] docx;
        try (com.demcha.compose.document.api.DocumentSession session = com.demcha.compose.GraphCompose.document()
                .pageSize(400, 400).margin(com.demcha.compose.document.style.DocumentInsets.of(36)).create()) {
            session.pageMargins(List.of(com.demcha.compose.document.api.PageMarginRule.from(2,
                    new com.demcha.compose.document.style.DocumentInsets(36, 36, 136, 36))));
            session.pageFlow(page -> page.addTable(table -> {
                table.autoColumns(1);
                for (int row = 1; row <= 30; row++) {
                    table.row("Item " + row);
                }
                table.rowCells(com.demcha.compose.document.table.DocumentTableCell.node(
                        new com.demcha.compose.document.dsl.ParagraphBuilder().text(text).build()));
            }));
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(docx))) {
            assertThat(document.getTables()).as("the premise: the table is split at page 2").hasSizeGreaterThan(1);
            XWPFParagraph moved = document.getTables().get(document.getTables().size() - 1).getRows().stream()
                    .flatMap(row -> row.getTableCells().stream())
                    .flatMap(cell -> cell.getParagraphs().stream())
                    .filter(paragraph -> paragraph.getText().contains("data"))
                    .findFirst().orElseThrow();
            String written = written(moved.getCTP());
            assertThat(written.chars().filter(c -> c == '~').count()).isPositive();
            assertThat(written.replace('~', '-')).isEqualTo(text);
        }
    }

    @Test
    void leavesAParagraphTakenOutAfterItWasWritten() {
        DocxLineStartHyphens hyphens = new DocxLineStartHyphens();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody body =
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody.Factory.newInstance();
        CTP paragraph = body.addNewP();
        paragraph.addNewR().addNewT().setStringValue("Platform engineer and developer-facing systems");
        hyphens.record(paragraph, lines("Platform engineer and", "developer-facing systems"));
        body.removeP(0);

        assertThat(hyphens.hold()).isZero();
    }

    @Test
    void anExportWithoutTheLayoutHoldsNone() throws Exception {
        String text = String.join(" ", java.util.Collections.nCopies(40, "data-driven"));
        try (XWPFDocument document = DocxExports.withoutLayout(400, 600, 20, page -> page.addParagraph(text))) {
            assertThat(document.getDocument().xmlText()).doesNotContain("noBreakHyphen");
        }
    }
}
