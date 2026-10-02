package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.layout.payloads.ParagraphFragmentPayload;
import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Assertions.within;

/**
 * A paragraph opening a padded cell takes the gap above its first line from the cell's padding,
 * so its lines stand the page's distance apart.
 *
 * <p>Word sets a paragraph's lines one height apart, the gap between them inside the line; a
 * paragraph of {@code n} lines then holds a gap too many, and it comes off the space above it.
 * A paragraph opening a cell has none, and its gaps were shared out over its lines instead:
 * {@code EditorialProposal}'s timeline describes each phase in a cell of two lines 13.85pt apart
 * on the page, and Word set them 12.35pt apart, the first line 1.85pt low.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxCellLineGapTest {

    private static final DocumentTextStyle BODY = DocumentTextStyle.builder().fontName(FontName.LATO).size(9).build();
    private static final double PADDING = 6.65;
    private static final double GAP = 3;
    private static final String WRAPPING = "Research, audit, insights, and strategy across the brand";

    @Test
    void aParagraphOpeningACellTakesTheGapFromTheCellsTopPadding() throws Exception {
        Exported exported = export(section -> section.addParagraph(p -> p.text(WRAPPING).textStyle(BODY).lineSpacing(GAP)));
        List<ParagraphLine> lines = exported.lines();
        assertThat(lines).hasSize(2);
        double line = lines.get(0).lineHeight();

        long padding = untakenPaddingTwips();

        assertThat(exported.lineTwips()).as("the line and the whole gap, the page's distance between the lines")
                .isCloseTo(Math.round((line + GAP) * 20), within(1L));
        assertThat(exported.topPaddingTwips()).isEqualTo(padding - Math.round(GAP * 20));
        assertThat(exported.topPaddingTwips() / 20.0 + 2 * exported.lineTwips() / 20.0)
                .as("the cell as tall as before: the padding and two lines a gap apart")
                .isCloseTo(padding / 20.0 + 2 * line + GAP, offset(0.06));
    }

    @Test
    void linesOfDifferentHeightsTakeOnlyWhatStepsThemThePagesDistanceApart() throws Exception {
        // An 11pt line over a 9pt one, 6pt apart: their baselines are closer than the taller
        // line and the gap, and further than the taller line and half the gap.
        DocumentTextStyle title = DocumentTextStyle.builder().fontName(FontName.LATO).size(11).build();
        double gap = 6;
        Exported exported = export(section -> section.addParagraph(p -> p
                .inlineText("Strategic Consultation\n", title)
                .inlineText("Leadership workshops", BODY)
                .textStyle(BODY).lineSpacing(gap)));
        List<ParagraphLine> lines = exported.lines();
        assertThat(lines).hasSize(2);
        double tallest = lines.get(0).lineHeight();
        double pitch = lines.get(0).baselineOffsetFromBottom() + gap
                       + lines.get(1).lineHeight() - lines.get(1).baselineOffsetFromBottom();
        assertThat(pitch).isBetween(tallest + gap / 2 + 0.5, tallest + gap - 0.5);

        assertThat(exported.lineTwips()).isCloseTo(Math.round(pitch * 20), within(1L));
        long padding = untakenPaddingTwips();
        assertThat(exported.topPaddingTwips()).as("some of the gap, not all of it")
                .isGreaterThan(padding - Math.round(gap * 20))
                .isLessThan(padding);
    }

    @Test
    void aParagraphUnderAnotherInTheCellLeavesThePaddingAlone() throws Exception {
        long padding = untakenPaddingTwips();
        Exported exported = export(section -> section
                .addParagraph(p -> p.text("Phase").textStyle(BODY))
                .addParagraph(p -> p.text(WRAPPING).textStyle(BODY).lineSpacing(GAP)));

        assertThat(exported.topPaddingTwips()).isEqualTo(padding);
    }

    @Test
    void aCellPaddedLessThanTheGapGivesAllItHasAndTheRestIsSharedOut() throws Exception {
        // 2pt of padding, of which the export writes 1pt: 20 twips taken of the 60-twip gap, and
        // the other 40 shared over the two lines.
        Exported exported = export(2, false, section -> section.addParagraph(p -> p.text(WRAPPING)
                .textStyle(BODY).lineSpacing(GAP)));
        long padding = export(2, false, section -> section.addParagraph(p -> p.text("Phase")
                .textStyle(BODY).lineSpacing(GAP))).topPaddingTwips();
        double line = exported.lines().get(0).lineHeight();
        assertThat(padding).isPositive().isLessThan(Math.round(GAP * 20));

        assertThat(exported.topPaddingTwips()).isZero();
        assertThat(exported.lineTwips()).isCloseTo(Math.round(line * 20) + Math.round((60 + padding) / 2.0), within(1L));
    }

    @Test
    void aRowKeepingALargerMarginGetsThePaddingBack() throws Exception {
        // Word sets a row's cells at its largest top margin; a merged cell's cannot be evened
        // down, so the padding taken would come back and the grown lines stand low. The lines
        // are written as they were, the gaps shared out.
        long padding = untakenPaddingTwips();
        Exported exported = export(PADDING, true, section -> section.addParagraph(p -> p.text(WRAPPING)
                .textStyle(BODY).lineSpacing(GAP)));
        double line = exported.lines().get(0).lineHeight();

        assertThat(exported.topPaddingTwips()).isEqualTo(padding);
        assertThat(exported.lineTwips()).isCloseTo(Math.round(line * 20) + Math.round(GAP * 20 / 2), within(1L));
    }

    /** The top padding of the same cell holding a single line, which takes nothing from it. */
    private static long untakenPaddingTwips() throws Exception {
        return export(section -> section.addParagraph(p -> p.text("Phase").textStyle(BODY)
                .lineSpacing(GAP))).topPaddingTwips();
    }

    private record Exported(XWPFTableCell cell, XWPFParagraph paragraph, List<ParagraphLine> lines) {

        long lineTwips() {
            return DocxTwips.of(paragraph.getCTP().getPPr().getSpacing().getLine());
        }

        long topPaddingTwips() {
            return DocxTwips.of(cell.getCTTc().getTcPr().getTcMar().getTop().getW());
        }
    }

    private static Exported export(Consumer<SectionBuilder> content) throws Exception {
        return export(PADDING, false, content);
    }

    /**
     * A table whose second cell holds the section, beside a text cell — merged down over a second
     * row where {@code merged}.
     */
    private static Exported export(double padding, boolean merged, Consumer<SectionBuilder> content) throws Exception {
        SectionBuilder section = new SectionBuilder().name("Focus");
        content.accept(section);
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page.addTable(t -> {
                t.name("Timeline")
                        .columns(DocumentTableColumn.fixed(120), DocumentTableColumn.fixed(140))
                        .defaultCellStyle(DocumentTableStyle.builder().padding(DocumentInsets.of(padding)).build());
                if (merged) {
                    t.rowCells(DocumentTableCell.text("01").rowSpan(2), DocumentTableCell.node(section.build()))
                            .rowCells(DocumentTableCell.text("Next"));
                } else {
                    t.rowCells(DocumentTableCell.text("01"), DocumentTableCell.node(section.build()));
                }
            }));
            List<ParagraphLine> lines = session.layoutGraph().fragments().stream()
                    .map(fragment -> fragment.payload())
                    .filter(ParagraphFragmentPayload.class::isInstance)
                    .map(ParagraphFragmentPayload.class::cast)
                    .filter(paragraph -> paragraph.lines().size() > 1)
                    .findFirst()
                    .map(ParagraphFragmentPayload::lines)
                    .orElse(List.of());
            XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(session.export(new DocxSemanticBackend())));
            XWPFTableCell cell = document.getTables().get(0).getRow(0).getCell(1);
            XWPFParagraph paragraph = cell.getParagraphs().stream()
                    .filter(p -> !p.getText().equals("Phase"))
                    .findFirst()
                    .orElse(cell.getParagraphs().get(0));
            return new Exported(cell, paragraph, lines);
        }
    }
}
