package com.demcha.examples.features.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.PageMarginRule;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;
import java.util.List;

/**
 * Runnable showcase for v1.9 {@code pageMargins(...)}: different page margins for
 * different page ranges in a single document. Page&nbsp;1 is a full-bleed cover
 * ({@link DocumentInsets#zero()} margin), so the same {@code fillColor} section
 * spans the whole sheet; pages&nbsp;2+ use wide book margins, so the body sits in a
 * narrow centred column. The content is measured at the width of the page it begins
 * on — no manual geometry, no separate documents.
 *
 * <pre>{@code
 * document.pageMargins(List.of(
 *     PageMarginRule.page(1, DocumentInsets.zero()),                 // full-bleed cover
 *     PageMarginRule.from(2, DocumentInsets.symmetric(40, 86))));    // book body
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class PerPageMarginExample {

    private static final DocumentColor INK = DocumentColor.rgb(22, 27, 38);
    private static final DocumentColor PAPER = DocumentColor.rgb(244, 241, 234);
    private static final DocumentColor MUTED = DocumentColor.rgb(96, 102, 112);
    private static final DocumentColor WHITE = DocumentColor.rgb(255, 255, 255);

    private static final String BODY =
            "The cover above bleeds to all four edges because page one has a zero "
            + "margin. This paragraph, on page two, flows inside the wide book margins "
            + "set for the rest of the document — a narrower measure that is easier to "
            + "read across a full page. Both pages live in one session: the engine lays "
            + "each block out at the width of the page it begins on.";

    private PerPageMarginExample() {
    }

    /**
     * Renders a two-page document: a full-bleed cover and a book-margin body.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/layout", "per-page-margin.pdf");

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(360, 300)
                .margin(DocumentInsets.of(28))
                .create()) {
            document.pageMargins(List.of(
                    PageMarginRule.page(1, DocumentInsets.zero()),                // full-bleed cover
                    PageMarginRule.from(2, DocumentInsets.symmetric(36, 86))));   // book body

            document.pageFlow(page -> {
                // Page 1 — zero margin, so this full-width filled band spans the sheet.
                page.addRow(r -> r
                        .fillColor(INK)
                        .weights(1.0)
                        .padding(DocumentInsets.symmetric(104, 28))
                        .addParagraph(p -> p.text("Field Manual")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(26).withColor(WHITE))));

                page.addPageBreak(b -> { });

                // Pages 2+ — wide book margins, so the same band is now a narrow column.
                page.addParagraph(p -> p.text("Chapter One")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(15).withColor(INK)));
                page.addRow(r -> r
                        .fillColor(PAPER)
                        .weights(1.0)
                        .padding(DocumentInsets.of(14))
                        .margin(DocumentInsets.top(8))
                        .addParagraph(p -> p.text(BODY)
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(10).withColor(MUTED))));
            });

            document.buildPdf();
        }

        return pdfFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
