package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A border the page strokes thinner or wider than any Word draws: written at Word's thinnest or
 * widest, a quarter of a point and twelve, and named in the report — a panel's, a table cell's,
 * a rule's and a page header's separator, whose width of none the page strokes as the thinnest
 * line it can.
 */
class DocxBorderWidthsTest {

    private static final DocumentColor INK = DocumentColor.rgb(20, 40, 90);
    private static final String RANGE = ": a Word border is no thinner than 0.25pt and no wider than 12pt";

    @Test
    void aWidthIsWrittenInEighthsHeldToWordsRange() {
        assertThat(DocxBorderWidths.eighths(1)).isEqualTo(8);
        assertThat(DocxBorderWidths.eighths(0.3)).as("rounded to the eighth").isEqualTo(2);
        assertThat(DocxBorderWidths.eighths(0.1)).isEqualTo(2);
        assertThat(DocxBorderWidths.eighths(0)).isEqualTo(2);
        assertThat(DocxBorderWidths.eighths(12)).isEqualTo(96);
        assertThat(DocxBorderWidths.eighths(20)).isEqualTo(96);
    }

    @Test
    void onlyAWidthOutsideWordsRangeIsNamed() {
        // Rounding to the eighth Word counts in is no width it cannot draw: 0.2pt is two eighths.
        assertThat(DocxBorderWidths.notHeld("its borders", true, List.of(0.2, 1.0, 12.0, 12.06))).isNull();
        assertThat(DocxBorderWidths.notHeld("the rule", false, List.of(0.1)))
                .isEqualTo("the rule is drawn 0.25pt wide where the page strokes it 0.1pt" + RANGE);
        assertThat(DocxBorderWidths.notHeld("its cells' rules", true, List.of(20.0, 0.05, 1.0, 0.001, 16.0)))
                .isEqualTo("its cells' rules are drawn 0.25pt wide where the page strokes them 0.001pt and 0.05pt, "
                           + "and 12pt wide where the page strokes them 16pt and 20pt" + RANGE);
    }

    @Test
    void aPanelsBorderWiderThanWordsWidestIsWrittenAtItAndNamed() throws Exception {
        try (Exported exported = export(null, page -> page
                .addSection("Card", card -> card.borders(DocumentBorders.all(DocumentStroke.of(INK, 20)))
                        .padding(DocumentInsets.of(24)).addParagraph("Inside")))) {
            CTBorder top = onlyCell(exported.document()).getCTTc().getTcPr().getTcBorders().getTop();
            assertThat(top.getSz()).isEqualTo(BigInteger.valueOf(96));
            assertThat(notes(exported)).containsExactly("its borders are drawn 12pt wide where the page strokes "
                                                        + "them 20pt" + RANGE);
        }
    }

    @Test
    void aPanelsHairlineIsWrittenAtWordsThinnestAndNamed() throws Exception {
        try (Exported exported = export(null, page -> page
                .addSection("Card", card -> card.borders(new DocumentBorders(DocumentStroke.of(INK, 0.1), null,
                                DocumentStroke.of(INK, 1), null))
                        .addParagraph("Inside")))) {
            var borders = onlyCell(exported.document()).getCTTc().getTcPr().getTcBorders();
            assertThat(borders.getTop().getSz()).isEqualTo(BigInteger.valueOf(2));
            assertThat(borders.getBottom().getSz()).isEqualTo(BigInteger.valueOf(8));
            assertThat(notes(exported)).containsExactly("its borders are drawn 0.25pt wide where the page strokes "
                                                        + "them 0.1pt" + RANGE);
        }
    }

    @Test
    void bordersWordDrawsAsThePageDoesAreNotNamed() throws Exception {
        // A stroke of no width is no border, on the page as in the file.
        try (Exported exported = export(null, page -> page
                .addSection("Card", card -> card.borders(new DocumentBorders(DocumentStroke.of(INK, 0.5),
                                DocumentStroke.of(INK, 0), DocumentStroke.of(INK, 12), DocumentStroke.of(INK, 0)))
                        .addParagraph("Inside"))
                .addDivider(d -> d.width(200).thickness(12).color(INK))
                .add(table(DocumentStroke.of(INK, 1), DocumentStroke.of(INK, 0.25)))
                .add(table(DocumentStroke.of(INK, 0), DocumentStroke.of(INK, 0))))) {
            assertThat(notes(exported)).isEmpty();
        }
    }

    @Test
    void aRowsBorderIsItsPanelsAndNamedSo() throws Exception {
        try (Exported exported = export(null, page -> page
                .addRow(row -> row.stroke(DocumentStroke.of(INK, 14)).padding(DocumentInsets.of(16))
                        .addParagraph("Left").addParagraph("Right")))) {
            assertThat(notes(exported)).containsExactly("its borders are drawn 12pt wide where the page strokes "
                                                        + "them 14pt" + RANGE);
        }
    }

    @Test
    void aTablesCellRulesOutsideWordsRangeAreNamedOnceForTheTable() throws Exception {
        try (Exported exported = export(null, page -> page
                .add(table(DocumentStroke.of(INK, 0.05), DocumentStroke.of(INK, 20))))) {
            var first = exported.document().getTables().get(0).getRow(0).getCell(0).getCTTc().getTcPr()
                    .getTcBorders();
            var last = exported.document().getTables().get(0).getRow(1).getCell(1).getCTTc().getTcPr()
                    .getTcBorders();
            assertThat(first.getTop().getSz()).isEqualTo(BigInteger.valueOf(2));
            assertThat(last.getTop().getSz()).isEqualTo(BigInteger.valueOf(96));
            assertThat(notes(exported)).containsExactly("its cells' rules are drawn 0.25pt wide where the page "
                                                        + "strokes them 0.05pt, and 12pt wide where the page "
                                                        + "strokes them 20pt" + RANGE);
        }
    }

    @Test
    void aHairlineRuleIsWrittenAtWordsThinnestAndNamed() throws Exception {
        try (Exported exported = export(null, page -> page.addDivider(d -> d.width(200).thickness(0.1).color(INK)))) {
            CTBorder bottom = ruleParagraph(exported.document()).getCTP().getPPr().getPBdr().getBottom();
            assertThat(bottom.getSz()).isEqualTo(BigInteger.valueOf(2));
            assertThat(notes(exported)).containsExactly("the rule is drawn 0.25pt wide where the page strokes it "
                                                        + "0.1pt" + RANGE);
        }
    }

    @Test
    void aPanelSplitByAPageBreakIsNamedOnce() throws Exception {
        try (Exported exported = export(null, page -> page
                .addSection("Card", card -> card.borders(DocumentBorders.all(DocumentStroke.of(INK, 20)))
                        .addParagraph("First").addPageBreak(pageBreak -> { }).addParagraph("Second")))) {
            assertThat(exported.document().getTables()).as("a table a piece").hasSize(2);
            assertThat(notes(exported)).hasSize(1);
        }
    }

    @Test
    void aHairlineLineIsARuleWrittenAtWordsThinnestAndNamed() throws Exception {
        try (Exported exported = export(null, page -> page.add(new com.demcha.compose.document.dsl.LineBuilder()
                .horizontal(200).thickness(0.1).color(INK).build()))) {
            CTBorder bottom = ruleParagraph(exported.document()).getCTP().getPPr().getPBdr().getBottom();
            assertThat(bottom.getSz()).isEqualTo(BigInteger.valueOf(2));
            assertThat(notes(exported)).containsExactly("the rule is drawn 0.25pt wide where the page strokes it "
                                                        + "0.1pt" + RANGE);
        }
    }

    @Test
    void aTranslucentSeparatorWiderThanWordsWidestIsNamedForBoth() throws Exception {
        try (Exported exported = export(session -> session.header(DocumentHeaderFooter.builder()
                .zone(DocumentHeaderFooterZone.HEADER).height(40).fontSize(10).leftText("Header")
                .showSeparator(true).separatorColor(DocumentColor.rgba(200, 0, 0, 100)).separatorThickness(20)
                .build()), page -> page.addParagraph("Body"))) {
            assertThat(notes(exported)).containsExactly("a page header's separator is drawn 12pt wide where the "
                                                        + "page strokes it 20pt" + RANGE);
            assertThat(exported.report().bySubject().get("translucency")).singleElement()
                    .extracting(DocxExportReport.Note::detail).isEqualTo("a page header's separator is flattened "
                                                                         + "against white, because a paragraph border "
                                                                         + "is opaque");
        }
    }

    @Test
    void aFramedSeparatorOfNoWidthFitsInItsFrameAsTheBorderIsWritten() throws Exception {
        // Two footers share Word's one: each stands in a frame as tall as its line and its border.
        try (Exported exported = export(session -> {
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER).height(26)
                    .fontSize(7).leftText("Legal line").showSeparator(true).separatorColor(INK)
                    .separatorThickness(0).build());
            session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER).height(14)
                    .fontSize(7).rightText("Page {page}").build());
        }, page -> page.addParagraph("Body"))) {
            XWPFParagraph legal = exported.document().getFooterList().get(0).getParagraphs().get(0);
            CTBorder top = legal.getCTP().getPPr().getPBdr().getTop();
            assertThat(top.getSz()).isEqualTo(BigInteger.valueOf(2));
            double border = ((Number) top.getSpace()).doubleValue() + 2 / 8.0;
            assertThat(((Number) legal.getCTP().getPPr().getFramePr().getH()).longValue())
                    .isEqualTo(Math.round((7 * DocxTextBands.LINE_FACTOR + border) * 20));
        }
    }

    @Test
    void aSeparatorOfNoWidthAtTheMarginReachesPastItAsWordDrawsIt() throws Exception {
        // Its band ends at the 20pt margin; Word draws its border a quarter of a point past it, so
        // the margin is written negative and the body held at it, as for any band reaching past.
        try (Exported exported = export(separator(0, null, 20), page -> page.addParagraph("Body"))) {
            Object top = exported.document().getDocument().getBody().getSectPr().getPgMar().getTop();
            assertThat(((Number) top).longValue()).isEqualTo(-400);
        }
        try (Exported exported = export(separator(-1, null, 20), page -> page.addParagraph("Body"))) {
            Object top = exported.document().getDocument().getBody().getSectPr().getPgMar().getTop();
            assertThat(((Number) top).longValue()).as("a separator not written reaches nothing").isEqualTo(400);
        }
    }

    @Test
    void aSeparatorOfNoWidthIsWrittenAtWordsThinnestAndNamed() throws Exception {
        // The page strokes it at a line width of 0, the thinnest line a screen or printer shows;
        // the export wrote no border at all.
        try (Exported exported = export(separator(0, null), page -> page.addParagraph("Body"))) {
            CTBorder bottom = band(exported.document()).getCTP().getPPr().getPBdr().getBottom();
            assertThat(bottom.getSz()).isEqualTo(BigInteger.valueOf(2));
            assertThat(notes(exported)).containsExactly("a page header's separator, which the page strokes 0pt "
                                                        + "wide, the thinnest line a screen or a printer shows, is "
                                                        + "drawn 0.25pt wide: a Word border is no thinner");
        }
    }

    @Test
    void aSeparatorWiderThanWordsWidestIsNamedOnceThoughWrittenIntoSeveralParts() throws Exception {
        try (Exported exported = export(separator(20, footerOffTheFirstPage()), page -> page.addParagraph("Body"))) {
            assertThat(exported.document().getHeaderList()).as("written into more than one part").hasSizeGreaterThan(1);
            assertThat(band(exported.document()).getCTP().getPPr().getPBdr().getBottom().getSz())
                    .isEqualTo(BigInteger.valueOf(96));
            assertThat(notes(exported)).containsExactly("a page header's separator is drawn 12pt wide where the "
                                                        + "page strokes it 20pt" + RANGE);
        }
    }

    @Test
    void aSeparatorBelowZeroOrNotANumberIsNotWrittenAndIsNamedOnce() throws Exception {
        for (float thickness : new float[]{-1, Float.NaN}) {
            try (Exported exported = export(separator(thickness, footerOffTheFirstPage()),
                    page -> page.addParagraph("Body"))) {
                assertThat(band(exported.document()).getCTP().getPPr().isSetPBdr()).as("%s", thickness).isFalse();
                assertThat(exported.report().bySubject().get("border width")).singleElement().satisfies(note -> {
                    assertThat(note.severity()).isEqualTo(DocxExportReport.Severity.DROPPED);
                    assertThat(note.detail()).isEqualTo("a page header's separator is set at no width a border "
                                                        + "has, below zero or not a number, so it is not written");
                });
            }
        }
    }

    private static Consumer<DocumentSession> separator(float thickness, Consumer<DocumentSession> more) {
        return separator(thickness, more, 40);
    }

    private static Consumer<DocumentSession> separator(float thickness, Consumer<DocumentSession> more,
                                                       float height) {
        return session -> {
            session.header(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.HEADER).height(height)
                    .fontSize(10).leftText("Header").showSeparator(true).separatorColor(INK)
                    .separatorThickness(thickness).build());
            if (more != null) {
                more.accept(session);
            }
        };
    }

    /** A footer kept off the first page, which gives the section a first page's part of its own. */
    private static Consumer<DocumentSession> footerOffTheFirstPage() {
        return session -> session.footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                .height(20).fontSize(8).rightText("{page}")
                .numbering(DocumentPageNumbering.builder().showOnFirstPage(false).build()).build());
    }

    private static com.demcha.compose.document.node.TableNode table(DocumentStroke first, DocumentStroke last) {
        DocumentTableStyle one = DocumentTableStyle.builder().stroke(first).build();
        DocumentTableStyle other = DocumentTableStyle.builder().stroke(last).build();
        return new TableBuilder().name("Rota")
                .columns(DocumentTableColumn.fixed(100), DocumentTableColumn.fixed(100))
                .rowCells(DocumentTableCell.text("A").withStyle(one), DocumentTableCell.text("B").withStyle(one))
                .rowCells(DocumentTableCell.text("C").withStyle(other), DocumentTableCell.text("D").withStyle(other))
                .build();
    }

    private record Exported(XWPFDocument document, DocxExportReport report) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            document.close();
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
        return new Exported(new XWPFDocument(new ByteArrayInputStream(bytes)), captured.get());
    }

    private static List<String> notes(Exported exported) {
        return exported.report().bySubject().getOrDefault("border width", List.of()).stream()
                .map(DocxExportReport.Note::detail).toList();
    }

    private static org.apache.poi.xwpf.usermodel.XWPFTableCell onlyCell(XWPFDocument document) {
        assertThat(document.getTables()).hasSize(1);
        return document.getTables().get(0).getRow(0).getCell(0);
    }

    private static XWPFParagraph ruleParagraph(XWPFDocument document) {
        return document.getParagraphs().stream()
                .filter(p -> p.getCTP().getPPr() != null && p.getCTP().getPPr().isSetPBdr())
                .findFirst().orElseThrow();
    }

    /** The header band's paragraph, in the default header. */
    private static XWPFParagraph band(XWPFDocument document) {
        return document.getHeaderFooterPolicy().getDefaultHeader().getParagraphs().stream()
                .filter(p -> p.getText().contains("Header")).findFirst().orElseThrow();
    }
}
