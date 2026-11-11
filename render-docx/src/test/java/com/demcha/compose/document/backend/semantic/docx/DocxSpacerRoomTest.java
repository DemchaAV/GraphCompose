package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A line beside a row's spacer that grows is given, out of the spacer's column, the room Word sets
 * it in.
 *
 * <p>The page sizes a row's child without a grow factor to its own text. Word states a size to the
 * half point, so a title or a date set at 9.75pt is set at 10 there, wider than its column, and Word
 * cuts off what passes a cell's edge: measured on a CV template made outside this repository, Word
 * 16 cut "Jan 2022 – Present" to "Presen" and a certification's issuer by its last letter, and the
 * report named neither.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxSpacerRoomTest {

    private static final String RIGHT_END = "right end";
    /** A size Word states to the half point as 10. */
    private static final double ROUNDED_UP = 9.75;

    @Test
    void theTitleAndTheDateTakeTheRoomWordSetsThemInOutOfTheSpacer() throws Exception {
        Exported exported = export(ROUNDED_UP, (row, style) -> row
                .addParagraph(p -> p.name("Title").text("Job Title").textStyle(style))
                .flexSpacer()
                .addParagraph(p -> p.name("Date").text("Jan 2022 – Present").textStyle(style)));

        double growth = 10 / ROUNDED_UP;
        assertThat(exported.gridPoints(0)).as("the title's column holds its line as Word sets it")
                .isGreaterThanOrEqualTo(exported.placedWidth("Title") * growth);
        assertThat(exported.gridPoints(2)).as("and the date's")
                .isGreaterThanOrEqualTo(exported.placedWidth("Date") * growth);
        assertThat(exported.gridSum()).as("out of the spacer: the row keeps its width")
                .isCloseTo(exported.rowWidth(), org.assertj.core.data.Offset.offset(0.1));

        XWPFParagraph date = exported.cellParagraph(2);
        assertThat(date.getAlignment()).as("the date ends where the page ends it").isEqualTo(ParagraphAlignment.RIGHT);
        assertThat(rightIndentTwips(date)).as("and no indent carries it past its cell's edge")
                .isGreaterThanOrEqualTo(0);
        assertThat(exported.cellParagraph(0).getAlignment()).as("the title starts where the page starts it")
                .isNotEqualTo(ParagraphAlignment.RIGHT);
        assertThat(exported.notes(RIGHT_END)).as("the spacer had the room").isEmpty();
    }

    @Test
    void aSizeWordStatesAsThePageDoesKeepsItsColumns() throws Exception {
        Exported exported = export(10, (row, style) -> row
                .addParagraph(p -> p.name("Title").text("Job Title").textStyle(style))
                .flexSpacer()
                .addParagraph(p -> p.name("Date").text("Jan 2022 – Present").textStyle(style)));

        assertThat(exported.gridPoints(2)).as("the date's column is its text's, as on the page")
                .isCloseTo(exported.placedWidth("Date"), org.assertj.core.data.Offset.offset(0.1));
        assertThat(exported.cellParagraph(2).getAlignment()).isNotEqualTo(ParagraphAlignment.RIGHT);
    }

    @Test
    void aChildAfterTheSpacerThatDoesNotEndTheRowIsNotSetRight() throws Exception {
        // Two children after the spacer: the page sets the first against the second, not against
        // the row's end, so it keeps its alignment and its column.
        Exported exported = export(ROUNDED_UP, (row, style) -> row
                .addParagraph(p -> p.name("Title").text("Job Title").textStyle(style))
                .flexSpacer()
                .addParagraph(p -> p.name("Date").text("Jan 2022 – Present").textStyle(style))
                .addParagraph(p -> p.name("Place").text("Remote").textStyle(style)));

        assertThat(exported.cellParagraph(2).getAlignment()).isNotEqualTo(ParagraphAlignment.RIGHT);
        assertThat(exported.gridPoints(2)).as("its column is the page's")
                .isCloseTo(exported.placedWidth("Date"), org.assertj.core.data.Offset.offset(0.1));
        assertThat(exported.gridPoints(0)).as("the title, beside the spacer, still takes its room")
                .isGreaterThanOrEqualTo(exported.placedWidth("Title") * 10 / ROUNDED_UP);
    }

    @Test
    void whatTheSpacerCannotGiveIsNamedAtWhatWordsLineRunsPast() throws Exception {
        // Laid out once on a wide page for the two lines' widths, then on a page the title and the
        // date fill but for a tenth of a point: the spacer has nothing to give.
        Exported wide = export(ROUNDED_UP, 400, TITLE_DATE_ROW);
        double title = wide.placedWidth("Title");
        double date = wide.placedWidth("Date");
        Exported full = export(ROUNDED_UP, 2 * MARGIN + title + date + 0.1, TITLE_DATE_ROW);

        List<DocxExportReport.Note> notes = full.notes(RIGHT_END);
        assertThat(notes).as("one note for each line").hasSize(2);
        assertThat(notes).anySatisfy(note -> assertThat(note.path()).endsWith("Title[0]"));
        assertThat(notes).anySatisfy(note -> assertThat(note.path()).endsWith("Date[2]"));
        // The title took the tenth of a point the spacer had; the date runs past by what Word sets
        // its line wider, and no more: not the few hundredths asked for the face.
        String detail = notes.stream().filter(note -> note.path().endsWith("Date[2]")).findFirst()
                .orElseThrow().detail();
        java.util.regex.Matcher amount = java.util.regex.Pattern.compile("runs ([0-9.]+)pt").matcher(detail);
        assertThat(amount.find()).as(detail).isTrue();
        assertThat(Double.parseDouble(amount.group(1)))
                .isCloseTo(date * (10 / ROUNDED_UP - 1), org.assertj.core.data.Offset.offset(0.05));
    }

    @Test
    void aCentredAndATwoLineParagraphKeepTheirColumns() throws Exception {
        Exported exported = export(ROUNDED_UP, 400, (row, style) -> row
                .addParagraph(p -> p.name("Title").text("Job Title").textStyle(style)
                        .align(com.demcha.compose.document.node.TextAlign.CENTER))
                .flexSpacer()
                .addParagraph(p -> p.name("Date").text("Jan 2022 – Present").textStyle(style)));
        assertThat(exported.gridPoints(0)).as("centred: the page's column")
                .isCloseTo(exported.placedWidth("Title"), org.assertj.core.data.Offset.offset(0.1));
        assertThat(exported.cellParagraph(0).getAlignment()).isEqualTo(ParagraphAlignment.CENTER);

        Exported twoLines = export(ROUNDED_UP, 400, (row, style) -> row
                .addParagraph(p -> p.name("Title").text("Job Title\nTeam Lead").textStyle(style))
                .flexSpacer()
                .addParagraph(p -> p.name("Date").text("Present").textStyle(style)));
        assertThat(twoLines.gridPoints(0)).as("two lines: the page's column")
                .isCloseTo(twoLines.placedWidth("Title"), org.assertj.core.data.Offset.offset(0.1));
    }

    /** A title, a spacer that grows and a date. */
    private static final BiConsumer<RowBuilder, DocumentTextStyle> TITLE_DATE_ROW = (row, style) -> row
            .addParagraph(p -> p.name("Title").text("Job Title").textStyle(style))
            .flexSpacer()
            .addParagraph(p -> p.name("Date").text("Jan 2022 – Present").textStyle(style));

    private static long rightIndentTwips(XWPFParagraph paragraph) {
        CTPPr properties = paragraph.getCTP().getPPr();
        if (properties == null || !properties.isSetInd() || !properties.getInd().isSetRight()) {
            return 0;
        }
        return properties.getInd().getRight() instanceof Number twips ? twips.longValue() : 0;
    }

    private record Exported(XWPFDocument document, DocxExportReport report, List<PlacedNode> nodes) {

        XWPFTable table() {
            return document.getTables().get(0);
        }

        double gridPoints(int column) {
            return ((Number) table().getCTTbl().getTblGrid().getGridColArray(column).getW()).doubleValue() / 20.0;
        }

        double gridSum() {
            double sum = 0;
            for (int column = 0; column < table().getCTTbl().getTblGrid().sizeOfGridColArray(); column++) {
                sum += gridPoints(column);
            }
            return sum;
        }

        XWPFParagraph cellParagraph(int column) {
            return table().getRow(0).getCell(column).getParagraphs().get(0);
        }

        double placedWidth(String name) {
            return nodes.stream().filter(node -> name.equals(node.semanticName())).findFirst()
                    .orElseThrow().placementWidth();
        }

        double rowWidth() {
            return placedWidth("Row");
        }

        List<DocxExportReport.Note> notes(String subject) {
            return report.notes().stream().filter(note -> note.subject().equals(subject)).toList();
        }
    }

    private static final double MARGIN = 20;

    private static Exported export(double size, BiConsumer<RowBuilder, DocumentTextStyle> children) throws Exception {
        return export(size, 400, children);
    }

    private static Exported export(double size, double pageWidth, BiConsumer<RowBuilder, DocumentTextStyle> children)
            throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        DocumentTextStyle style = DocumentTextStyle.builder().fontName(FontName.LATO).size(size).build();
        try (DocumentSession session = GraphCompose.document().pageSize(pageWidth, 300)
                .margin(DocumentInsets.of(MARGIN)).create()) {
            session.pageFlow(page -> page.addRow("Row", row -> {
                row.spacing(0).verticalAlign(RowVerticalAlign.BOTTOM);
                children.accept(row, style);
            }));
            List<PlacedNode> nodes = session.layoutGraph().nodes();
            byte[] docx = session.export(new DocxSemanticBackend(report::set));
            return new Exported(new XWPFDocument(new ByteArrayInputStream(docx)), report.get(), nodes);
        }
    }
}
