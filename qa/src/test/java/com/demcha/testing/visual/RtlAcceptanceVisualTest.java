package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.compose.testing.visual.PdfVisualRegression;

import org.junit.jupiter.api.Test;

/**
 * The acceptance render for right-to-left support: a paragraph mixing Arabic, Hebrew,
 * English and digits, held against a pixel baseline.
 *
 * <p>Every other test in this feature asserts one mechanism — the order of the spans,
 * the direction of a run, the code points in the content stream. Each can pass while
 * the page still looks wrong, because none of them looks at the page. This one does:
 * the baseline was blessed after reading the render, and it fails on any visible
 * change to how these scripts are laid out, whichever mechanism caused it.</p>
 *
 * <p>Refresh it with {@code -Dgraphcompose.visual.approve=true} only after looking at
 * the new render and deciding the difference is wanted.</p>
 */
class RtlAcceptanceVisualTest {

    // The baselines are Windows-rendered PNGs and PDFBox antialiasing drifts across
    // platforms, so the budget matches the one the sibling regression test settled on.
    // A real change to how these scripts lay out moves far more than 2 500 pixels:
    // reordering or unjoining a line repaints it entirely.
    private static final PdfVisualRegression VISUAL = PdfVisualRegression.standard()
            .perPixelTolerance(6)
            .mismatchedPixelBudget(2_500);

    private static final DocumentColor INK = DocumentColor.rgb(28, 32, 44);

    private static final String ARABIC = "مرحبا بالعالم";
    private static final String HEBREW = "שלום עולם";

    @Test
    void theAcceptanceParagraphRendersAsBlessed() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(595, 300)
                .margin(DocumentInsets.of(36))
                .create()) {

            document.pageFlow(page -> page
                    // Hebrew, Latin and a year in one right-to-left line: the words
                    // reorder, the Latin and the digits keep running forwards.
                    .addParagraph(p -> p
                            .text(HEBREW + " GraphCompose 2026 " + HEBREW)
                            .direction(TextDirection.RTL)
                            .textStyle(hebrew()))
                    // Arabic joined through its presentation forms, with a mirrored
                    // parenthesis pair around an embedded Latin word.
                    .addParagraph(p -> p
                            .text(ARABIC + " (GraphCompose) " + ARABIC)
                            .direction(TextDirection.RTL)
                            .textStyle(arabic()))
                    // AUTO reads the direction off the first strong character.
                    .addParagraph(p -> p
                            .text(HEBREW + " 2026")
                            .direction(TextDirection.AUTO)
                            .textStyle(hebrew())));

            VISUAL.assertMatchesBaseline("rtl-acceptance", document);
        }
    }

    private static DocumentTextStyle hebrew() {
        return DocumentTextStyle.builder().fontName(FontName.DAVID_LIBRE).size(18).color(INK).build();
    }

    private static DocumentTextStyle arabic() {
        return DocumentTextStyle.builder().fontName(FontName.AMIRI).size(18).color(INK).build();
    }
}
