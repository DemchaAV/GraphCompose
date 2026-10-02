package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A table the layout moves to a new page keeps its own space above it there in Word.
 *
 * <p>The page keeps a block's top edge where a page break moves the block down; the gap
 * between it and the block before stays at the foot of the page above. Word has no space
 * above a table, so the edge was written below the paragraph before it, on the page above:
 * the long {@code LumaStudioInvoice}'s closing pair stood 12pt high at the top of its third
 * page. Word drops a paragraph's space above at the top of a page, and keeps a line's height.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxSpaceOnANewPageTest {

    private static final double EDGE = 12;
    private static final double GAP = 4;

    @Test
    void aRowMovedToANewPageHoldsItsTopEdgeInALineKeptWithIt() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, content(8))) {
            XWPFParagraph spacer = paragraphBefore(document, onlyTable(document));
            CTSpacing spacing = spacer.getCTP().getPPr().getSpacing();

            assertThat(spacing.getLineRule()).isEqualTo(STLineSpacingRule.EXACT);
            assertThat(DocxTwips.of(spacing.getLine())).as("the row's top edge, a line's height")
                    .isEqualTo(Math.round(EDGE * 20));
            assertThat(spacer.getCTP().getPPr().isSetKeepNext()).as("kept with the row").isTrue();
            assertThat(spacingAfter(paragraphBefore(document, spacer)))
                    .as("the gap stays at the foot of the page above, the edge does not")
                    .isEqualTo(Math.round(GAP * 20));
        }
    }

    @Test
    void aTableMovedToANewPageHoldsItsTopEdgeTheSameWay() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 200, 20, page -> {
            page.spacing(GAP);
            for (int i = 0; i < 8; i++) {
                page.addParagraph("Line " + i);
            }
            page.addTable(t -> t.name("Totals").margin(new DocumentInsets(EDGE, 0, 0, 0))
                    .autoColumns(2).row("Subtotal", "8,500.00").row("VAT", "1,700.00"));
        })) {
            XWPFParagraph spacer = paragraphBefore(document, onlyTable(document));

            assertThat(DocxTwips.of(spacer.getCTP().getPPr().getSpacing().getLine())).isEqualTo(Math.round(EDGE * 20));
            assertThat(spacer.getCTP().getPPr().isSetKeepNext()).isTrue();
        }
    }

    @Test
    void aRowOnThePageOfTheBlockBeforeItIsWrittenAsBefore() throws Exception {
        try (XWPFDocument document = DocxExports.withLayout(300, 600, 20, content(2))) {
            XWPFParagraph above = paragraphBefore(document, onlyTable(document));

            assertThat(above.getText()).as("no line between them").isNotEmpty();
            assertThat(spacingAfter(above)).isEqualTo(Math.round((GAP + EDGE) * 20));
        }
    }

    /** Lines of text, then a row with a top edge (a row is never broken), the flow's gap between each. */
    private static Consumer<PageFlowBuilder> content(int lines) {
        return page -> {
            page.spacing(GAP);
            for (int i = 0; i < lines; i++) {
                page.addParagraph("Line " + i);
            }
            page.addRow(row -> row.name("Closing")
                    .margin(new DocumentInsets(EDGE, 0, 0, 0))
                    .addParagraph("Notes").addParagraph("Payment"));
        };
    }

    private static XWPFTable onlyTable(XWPFDocument document) {
        return document.getTables().get(0);
    }

    private static XWPFParagraph paragraphBefore(XWPFDocument document, IBodyElement element) {
        List<IBodyElement> body = document.getBodyElements();
        return (XWPFParagraph) body.get(body.indexOf(element) - 1);
    }

    private static long spacingAfter(XWPFParagraph paragraph) {
        CTSpacing spacing = paragraph.getCTP().getPPr() == null ? null : paragraph.getCTP().getPPr().getSpacing();
        return spacing == null || !spacing.isSetAfter() ? 0 : DocxTwips.of(spacing.getAfter());
    }
}
