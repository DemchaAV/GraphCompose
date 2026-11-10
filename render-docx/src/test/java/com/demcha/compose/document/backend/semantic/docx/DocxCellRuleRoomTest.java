package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table whose cells' rules are wider than the padding that takes them stands taller in Word,
 * which makes room for each rule in its row, than on the page, which draws it over the cell's
 * edge, where a cell of text then needs more than the page's height — and the report says about
 * how much, or the most it can be where the layout does not tell its rows' heights.
 *
 * <p>The "about" figures of four rows with no padding, of a point of padding, of padding past a
 * share and of a two-line cell beside a padded one are the measured ones: the paragraph under
 * the table that much lower in Word and LibreOffice on Windows than under the same table unruled,
 * to a tenth of a point. The others are what the same count comes to.</p>
 */
class DocxCellRuleRoomTest {

    private static final String SUBJECT = "cell rule";

    @Test
    void rulesNoPaddingTakesStandTheTableTallerByWhatEachRowGives() throws Exception {
        // The first row gives its own rule above and half the next row's below, the last row half
        // its own above and its own below, a row between half of each: 3pt rules, 4.5 + 3 + 3 + 4.5.
        assertThat(fourRows(3, 0)).containsExactly(note("about 15"));
        assertThat(fourRows(1, 0)).containsExactly(note("about 5"));
        // A point of padding takes a point of each share.
        assertThat(fourRows(3, 1)).containsExactly(note("about 7"));
        // Padding past a share holds the row by itself: 5pt above takes the rules above, nothing
        // below, so 2pt rules over two rows stand them 1 and 2pt taller.
        DocumentTableStyle above = DocumentTableStyle.builder().padding(new DocumentInsets(5, 0, 0, 0))
                .stroke(DocumentStroke.of(DocumentColor.BLACK, 2)).build();
        assertThat(details(page -> page.addTable(t -> t.defaultCellStyle(above)
                .columns(DocumentTableColumn.fixed(150)).row("Alpha").row("Beta"))))
                .containsExactly(note("about 3"));
    }

    @Test
    void aSmallRoomIsNamedToo() throws Exception {
        // A quarter-point rule is five twips: 5 + 2 above and below the first row, 3 + 5 the last.
        DocumentTableStyle hairline = DocumentTableStyle.builder().padding(DocumentInsets.zero())
                .stroke(DocumentStroke.of(DocumentColor.BLACK, 0.25)).build();
        assertThat(details(page -> page.addTable(t -> t.defaultCellStyle(hairline)
                .columns(DocumentTableColumn.fixed(150)).row("Alpha").row("Beta"))))
                .containsExactly(note("about 0.75"));
    }

    @Test
    void paddingThatTakesTheRulesOrNoRulesAreNotNamed() throws Exception {
        assertThat(fourRows(3, 3)).isEmpty();
        assertThat(fourRows(0, 0)).as("a stroke of no width is no rule").isEmpty();
        assertThat(details(page -> page.addTable(t -> t.columns(DocumentTableColumn.fixed(150))
                .row("Alpha").row("Beta")))).as("the engine's own rule and padding").isEmpty();
    }

    @Test
    void aRowStandsTallerOnlyWhereTheCellItsPaddingFallsShortInSetsItsHeight() throws Exception {
        // 2pt rules over three rows: a cell with no padding holds 2 + 1, 1 + 1 and 1 + 2 round its
        // text, each cell counted on its own — the padded cell's 4 beside the bare cell's two lines
        // would make it 12 (measured: 8).
        DocumentTableStyle bare = style(2, 0);
        DocumentTableStyle padded = style(2, 2);
        DocumentTableCell tall = DocumentTableCell.lines("Alpha", "Beta").withStyle(bare);
        DocumentTableCell beside = DocumentTableCell.text("One").withStyle(padded);
        assertThat(details(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(150), DocumentTableColumn.fixed(150))
                .rowCells(tall, beside).rowCells(tall, beside).rowCells(tall, beside))))
                .as("the bare cell's two lines set each row's height").containsExactly(note("about 8"));
        assertThat(details(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(150), DocumentTableColumn.fixed(150))
                .rowCells(DocumentTableCell.text("Alpha").withStyle(bare),
                        DocumentTableCell.lines("One", "Two").withStyle(padded)))))
                .as("the padded cell's two lines set it, and the bare cell's line has room in it").isEmpty();
        assertThat(details(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(150), DocumentTableColumn.fixed(150))
                .rowCells(DocumentTableCell.text("Alpha").withStyle(bare),
                        DocumentTableCell.text("One").withStyle(padded)))))
                .as("one line each: the padded cell sets the row at the height the bare one needs").isEmpty();
    }

    @Test
    void aCellsLineSpacingIsPartOfItsHeight() throws Exception {
        // Two lines 6pt apart with no padding against two lines padded 2pt: the spaced cell sets the
        // row, and its 2pt rules, both whole to the one row, stand it 4pt taller.
        DocumentTableStyle spaced = DocumentTableStyle.builder().padding(DocumentInsets.zero()).lineSpacing(6.0)
                .stroke(DocumentStroke.of(DocumentColor.BLACK, 2)).build();
        assertThat(details(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(150), DocumentTableColumn.fixed(150))
                .rowCells(DocumentTableCell.lines("Alpha", "Beta").withStyle(spaced),
                        DocumentTableCell.lines("One", "Two").withStyle(style(2, 2))))))
                .containsExactly(note("about 4"));
    }

    @Test
    void aHeavierHeaderGivesItsOwnRuleAboveAndHalfTheRowsBelow() throws Exception {
        // 2pt over 0.5pt rules: 2 + 0.25 the header, 0.25 + 0.25 a row between, 0.25 + 0.5 the last.
        assertThat(details(page -> page.addTable(t -> t.defaultCellStyle(style(0.5, 0))
                .columns(DocumentTableColumn.fixed(150))
                .rowCells(DocumentTableCell.text("Head").withStyle(style(2, 0)))
                .row("Alpha").row("Beta"))))
                .containsExactly(note("about 3.5"));
    }

    @Test
    void aHeaderRowRepeatedOnEveryPageCountsOnEach() throws Exception {
        double once = points(details(table(false)));
        double repeated = points(details(table(true)));
        // 1pt rules: the header row gives 1 + 0.5 on each page it is drawn on.
        assertThat(repeated - once).as("the header's room again on each further page")
                .isGreaterThanOrEqualTo(1.5).satisfies(more -> assertThat(more % 1.5).isCloseTo(0, within()));
    }

    @Test
    void aCellBuiltFromANodeOrEmptyIsNotCountedAndOneOfASpaceIs() throws Exception {
        // Ruled past their padding as the corpus's invoices and rota are, whose rows stood within
        // half a point of the page's in Word; an empty cell's line is cut to its row's room.
        DocumentTableStyle bare = style(3, 0);
        assertThat(details(page -> page.addTable(t -> t.defaultCellStyle(bare)
                .columns(DocumentTableColumn.fixed(150), DocumentTableColumn.fixed(150))
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("Alpha").build()),
                        DocumentTableCell.text(""))
                .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("Beta").build()),
                        DocumentTableCell.text(""))))).isEmpty();
        // A space is written as a run, whose line keeps its height.
        assertThat(details(page -> page.addTable(t -> t.defaultCellStyle(bare)
                .columns(DocumentTableColumn.fixed(150)).row(" ").row(" ")))).containsExactly(note("about 9"));
    }

    @Test
    void aCellSpanningRowsIsNotCounted() throws Exception {
        // Its three lines stand across both rows; the cells beside it take their rules in their padding.
        assertThat(details(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(150), DocumentTableColumn.fixed(150))
                .rowCells(DocumentTableCell.lines("A", "B", "C").withStyle(style(3, 0)).rowSpan(2),
                        DocumentTableCell.text("One").withStyle(style(3, 3)))
                .rowCells(DocumentTableCell.text("Two").withStyle(style(3, 3)))))).isEmpty();
    }

    @Test
    void aTableComposedInACellOrWithNoLayoutIsNamedAtTheMostItCanBe() throws Exception {
        // The outer table laid out: what is not told is the composed table's rows alone.
        List<DocxExportReport.Note> composed = laidOut(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(320))
                .rowCells(DocumentTableCell.node(new TableBuilder().defaultCellStyle(style(3, 0))
                        .columns(DocumentTableColumn.fixed(150)).row("Alpha").row("").build()))))
                .bySubject().getOrDefault(SUBJECT, List.of());
        assertThat(composed).singleElement().satisfies(note -> {
            assertThat(note.detail()).as("its rows not held, an empty cell's line keeps its height: 4.5 + 4.5")
                    .isEqualTo(note("up to 9"));
            assertThat(note.path()).as("composed content is named under its table, by its cell")
                    .endsWith("TableNode[0]/row[0]/cell[0]/TableNode[0]");
        });
        assertThat(DocxExports.reportWithoutLayout(500, 400, 30, rows(3, 0)).bySubject().get(SUBJECT))
                .as("what the padding does not take needs no layout to tell").extracting(DocxExportReport.Note::detail)
                .containsExactly(note("up to 15"));
        assertThat(laidOut(page -> page.addTable(t -> t
                .columns(DocumentTableColumn.fixed(320))
                .rowCells(DocumentTableCell.node(new TableBuilder().defaultCellStyle(style(3, 0))
                        .columns(DocumentTableColumn.fixed(150))
                        .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("Alpha").build()))
                        .rowCells(DocumentTableCell.node(new ParagraphBuilder().text("Beta").build())).build()))))
                .bySubject()).as("a cell built from a node is not counted in the bound either")
                .doesNotContainKey(SUBJECT);
    }

    /** The report of an export the layout stands behind: a document it refuses is another case. */
    private static DocxExportReport laidOut(Consumer<PageFlowBuilder> content) throws Exception {
        DocxExportReport report = DocxExports.report(500, 400, 30, content);
        assertThat(report.bySubject()).as("laid out").doesNotContainKey("measured geometry");
        return report;
    }

    private static String note(String howMuch) {
        return "its cells' rules are wider than the padding that takes them: Word makes room for each in its row, "
               + "where the page draws it over the cell's edge, and sets the table " + howMuch
               + "pt taller than the page";
    }

    private static final java.util.regex.Pattern ABOUT =
            java.util.regex.Pattern.compile("sets the table about (\\d+(?:\\.\\d+)?)pt taller");

    /** The figure a single note names, the note failing the test where it names none. */
    private static double points(List<String> notes) {
        assertThat(notes).hasSize(1);
        java.util.regex.Matcher about = ABOUT.matcher(notes.get(0));
        assertThat(about.find()).as("a note naming about how much: %s", notes.get(0)).isTrue();
        return Double.parseDouble(about.group(1));
    }

    private static org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.data.Offset.offset(1e-6);
    }

    /** Forty rows of 1pt rules and no padding, over pages, its first row repeated or not. */
    private static Consumer<PageFlowBuilder> table(boolean repeated) {
        return page -> page.addTable(t -> {
            t.defaultCellStyle(style(1, 0)).columns(DocumentTableColumn.fixed(150));
            if (repeated) {
                t.repeatHeader(1);
            }
            for (int row = 0; row < 40; row++) {
                t.row("Row " + row);
            }
        });
    }

    private static List<String> fourRows(double rule, double padding) throws Exception {
        return details(rows(rule, padding));
    }

    private static Consumer<PageFlowBuilder> rows(double rule, double padding) {
        return page -> page.addTable(t -> t.defaultCellStyle(style(rule, padding))
                .columns(DocumentTableColumn.fixed(150), DocumentTableColumn.fixed(150))
                .row("Alpha", "One").row("Beta", "Two").row("Gamma", "Three").row("Delta", "Four"));
    }

    private static DocumentTableStyle style(double rule, double padding) {
        return DocumentTableStyle.builder().padding(DocumentInsets.of(padding))
                .stroke(DocumentStroke.of(DocumentColor.BLACK, rule)).build();
    }

    private static List<String> details(Consumer<PageFlowBuilder> content) throws Exception {
        return laidOut(content).bySubject().getOrDefault(SUBJECT, List.of()).stream()
                .map(DocxExportReport.Note::detail).toList();
    }
}
