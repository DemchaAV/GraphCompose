package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableColumn;
import com.demcha.compose.document.table.DocumentTableStyle;
import com.demcha.compose.font.FontName;

import java.util.List;
import java.util.function.Consumer;

/**
 * Documents of the DOCX fidelity corpus built for a shape no template preset has on its own
 * fixture, so that an editor setting one off the page fails the gate as a preset would.
 *
 * <p>Each sets a way the page breaks inside a panel or a column: a page break among a panel's
 * children and inside a panel nested in it, page breaks the page sets as boxes of no height in a
 * row's column and in a stack's layer, and a card whose table the layout runs over three pages,
 * with other margins from the second. Their text is set in a face the export embeds, so both
 * editors break its lines at the page's words.</p>
 */
final class SyntheticDocxCorpus {

    private static final DocumentTextStyle TEXT = DocumentTextStyle.builder().fontName(FontName.LATO).size(10).build();
    private static final DocumentTableStyle CELLS = DocumentTableStyle.builder().textStyle(TEXT).build();
    private static final DocumentColor SURFACE = DocumentColor.rgb(238, 243, 249);
    private static final DocumentColor INNER = DocumentColor.rgb(224, 233, 244);
    private static final DocumentColor INK = DocumentColor.rgb(26, 86, 148);
    private static final DocumentInsets MARGIN = DocumentInsets.of(36);

    private SyntheticDocxCorpus() {
    }

    /** The documents of the {@code synthetic} family, one for each shape. */
    static List<DocxCorpusDocument> documents() {
        return List.of(
                synthetic("break_in_panel", SyntheticDocxCorpus::breakInAPanel),
                synthetic("break_in_column", SyntheticDocxCorpus::breakInAColumn),
                synthetic("card_over_pages", SyntheticDocxCorpus::cardOverPages));
    }

    /**
     * A card with a page break among its children, and a card nested in it with a break of its
     * own: the card runs over three pages, each part where the page sets it.
     */
    private static void breakInAPanel(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "Before the card, on the first page."))
                .addSection(card -> card.fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .padding(DocumentInsets.of(8)).spacing(6)
                        .addParagraph(p -> text(p, "The card opens on the first page."))
                        .addParagraph(p -> text(p, "Its second line stands under the first."))
                        .addPageBreak(pageBreak -> { })
                        .addParagraph(p -> text(p, "After the card's own break, on the second page."))
                        .addSection(inner -> inner.fillColor(INNER).padding(DocumentInsets.of(6)).spacing(4)
                                .addParagraph(p -> text(p, "The inner card opens under it."))
                                .addPageBreak(pageBreak -> { })
                                .addParagraph(p -> text(p, "After the inner card's break, on the third page.")))
                        .addParagraph(p -> text(p, "The card closes on the third page.")))
                .addParagraph(p -> text(p, "After the card, on the third page.")));
    }

    /**
     * Page breaks the page sets as boxes of no height: in a row's column and in a stack's one
     * layer. The page goes on with what follows on the same page, and so does the editor.
     */
    private static void breakInAColumn(DocumentSession session) {
        session.pageSize(400, 500).margin(MARGIN);
        session.pageFlow(page -> page.spacing(8)
                .addParagraph(p -> text(p, "A row and a stack, each with a break in it."))
                .addRow(row -> row.spacing(12)
                        .addSection(column -> column.spacing(4)
                                .addParagraph(p -> text(p, "Part A of the column."))
                                .addPageBreak(pageBreak -> { })
                                .addParagraph(p -> text(p, "Part B of the column.")))
                        .addParagraph(p -> text(p, "The column beside it.")))
                .addLayerStack(stack -> stack.topLeft(new SectionBuilder().spacing(4)
                        .addParagraph(p -> text(p, "The layer before its break."))
                        .addPageBreak(pageBreak -> { })
                        .addParagraph(p -> text(p, "The layer after its break."))
                        .build()))
                .addParagraph(p -> text(p, "After the row and the stack, on the same page.")));
    }

    /**
     * A bordered card whose table of sixty rows the layout runs over three pages, its header
     * repeated at the top of each, the pages after the first set against a larger top margin.
     *
     * <p>Spaced so that page 1's rows leave room under them for the paragraph that ends a Word
     * section at the cut, as gaps of 2 to 6pt and 12 to 16pt do: at 8 or 10pt the export names
     * page 2's margins instead, and both editors set page 2 with page 1's.</p>
     */
    private static void cardOverPages(DocumentSession session) {
        session.pageSize(400, 600).margin(MARGIN);
        session.pageMargins(List.of(PageMarginRule.from(2, new DocumentInsets(90, 36, 36, 36))));
        session.pageFlow(page -> page.spacing(4)
                .addParagraph(p -> text(p, "The card's table runs over three pages."))
                .addSection(card -> card.fillColor(SURFACE).stroke(DocumentStroke.of(INK, 1))
                        .padding(DocumentInsets.of(6))
                        .addTable(table -> {
                            table.columns(DocumentTableColumn.fixed(220), DocumentTableColumn.fixed(90))
                                    .defaultCellStyle(CELLS).headerStyle(CELLS)
                                    .header("Item", "Amount").repeatHeader();
                            for (int row = 1; row <= 60; row++) {
                                table.row("Line item " + row, String.valueOf(row * 7));
                            }
                        }))
                .addParagraph(p -> text(p, "After the card, on its last page.")));
    }

    private static ParagraphBuilder text(ParagraphBuilder paragraph, String text) {
        return paragraph.text(text).textStyle(TEXT);
    }

    private static DocxCorpusDocument synthetic(String name, Consumer<DocumentSession> compose) {
        return new DocxCorpusDocument("synthetic", name, -1, compose);
    }
}
