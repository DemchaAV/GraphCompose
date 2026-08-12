package com.demcha.testing.visual;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import com.demcha.compose.testing.visual.PdfVisualRegression;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

/**
 * Holds the situations right-to-left text actually gets into, against two kinds of baseline.
 *
 * <p>Each scenario is asserted twice, because the two catch different breakages. The layout
 * snapshot pins <b>coordinates</b> — where every line and span was placed — so a change in
 * wrapping or pagination shows up as a number that moved, and the failure names the node.
 * The pixel baseline pins <b>what was drawn</b>, and that is not a luxury: switching the
 * right-to-left reversal off leaves every coordinate identical — same spans, same widths,
 * same positions — and changes only the order the glyphs are painted in. Measured, not
 * assumed: with the reversal disabled the layout snapshots all pass and four of the five
 * pixel baselines fail. Coordinates cannot see a line drawn backwards.
 *
 * <p>The scenarios are the ones a demo page does not reach. A paragraph long enough to wrap
 * over many lines, each of them reordered on its own; a flow long enough to break across a
 * page and carry its direction to the other side; a line that mixes scripts with Latin and
 * digits; and one page holding every bundled script at once, so a font swap in any of them
 * fails here.</p>
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} for the coordinates and
 * {@code -Dgraphcompose.visual.approve=true} for the pixels — after looking at the render and
 * deciding the difference is wanted.</p>
 */
class RtlScenariosVisualTest {

    /**
     * The budget scales with the page, because a fixed one does not guard a small page.
     *
     * <p>These scenarios use pages a quarter the area of the sibling acceptance render, and
     * a budget copied from it absorbed a real regression on the shortest of them — the
     * reversal was switched off and two scenarios still passed. Antialiasing drift is
     * proportional to how much text is drawn, so the budget is too: one part in seventy of
     * the page, which is the ratio the sibling settled on, and the per-pixel tolerance
     * absorbs the edge softness underneath it.</p>
     */
    private static PdfVisualRegression visualFor(int width, int height) {
        return PdfVisualRegression.standard()
                .perPixelTolerance(6)
                .mismatchedPixelBudget((long) width * height / 70);
    }

    private static final DocumentColor INK = DocumentColor.rgb(28, 32, 44);

    private static final String ARABIC_PROSE =
            "تتغير أشكال الحروف العربية حسب موضعها في الكلمة، فلكل حرف صورة في أول الكلمة "
            + "وأخرى في وسطها وثالثة في آخرها ورابعة حين يقف وحده. هذه القاعدة هي ما يجعل "
            + "النص العربي متصلاً، وهي أيضاً ما يجعل رسمه داخل ملف PDF مسألة تحتاج إلى عمل "
            + "إضافي من المحرك نفسه قبل قياس السطر.";

    private static final String HEBREW_PROSE =
            "הטקסט העברי נקרא מימין לשמאל, והמנוע מסדר כל שורה בנפרד לפי האלגוריתם "
            + "הדו־כיווני של יוניקוד. מילים לטיניות וספרות שבתוך המשפט ממשיכות לרוץ קדימה, "
            + "והפסקה כולה עדיין מתחילה מהקצה הימני של העמודה.";

    @Test
    void anArabicParagraphWrapsWithEveryLineReordered() throws Exception {
        assertBothWays("arabic-wrapping", 300, 260, page -> page
                .addParagraph(p -> p.text(ARABIC_PROSE)
                        .direction(TextDirection.RTL)
                        .textStyle(style(FontName.AMIRI, 13))));
    }

    @Test
    void aHebrewParagraphWrapsWithEveryLineReordered() throws Exception {
        assertBothWays("hebrew-wrapping", 300, 240, page -> page
                .addParagraph(p -> p.text(HEBREW_PROSE)
                        .direction(TextDirection.RTL)
                        .textStyle(style(FontName.DAVID_LIBRE, 13))));
    }

    @Test
    void aRightToLeftFlowKeepsItsDirectionAcrossAPageBreak() throws Exception {
        // Short page, long text: the break lands inside a paragraph, so the second page
        // opens mid-sentence and has to start from the right edge like the first.
        assertBothWays("arabic-pagination", 300, 170, page -> page
                .addParagraph(p -> p.text(ARABIC_PROSE)
                        .direction(TextDirection.RTL)
                        .textStyle(style(FontName.AMIRI, 13)))
                .addParagraph(p -> p.text(ARABIC_PROSE)
                        .direction(TextDirection.RTL)
                        .textStyle(style(FontName.AMIRI, 13))));
    }

    @Test
    void aMixedLineKeepsItsLatinAndDigitsRunningForwards() throws Exception {
        assertBothWays("mixed-runs", 360, 200, page -> page
                .addParagraph(p -> p.text("שלום GraphCompose 2.2.0 עולם")
                        .direction(TextDirection.RTL)
                        .textStyle(style(FontName.DAVID_LIBRE, 15)))
                .addParagraph(p -> p.text("مرحبا (GraphCompose) بالعالم")
                        .direction(TextDirection.RTL)
                        .textStyle(style(FontName.AMIRI, 15)))
                .addParagraph(p -> p.text("2026 שנה טובה")
                        .direction(TextDirection.AUTO)
                        .textStyle(style(FontName.DAVID_LIBRE, 15))));
    }

    @Test
    void everyBundledScriptOnOnePage() throws Exception {
        // One page holding all five, so swapping any bundled family fails here rather than
        // in whichever document happens to use that script.
        assertBothWays("bundled-scripts", 360, 260, page -> page
                .addParagraph(p -> p.text("مرحبا بالعالم")
                        .direction(TextDirection.RTL).textStyle(style(FontName.AMIRI, 15)))
                .addParagraph(p -> p.text("שלום עולם")
                        .direction(TextDirection.RTL).textStyle(style(FontName.DAVID_LIBRE, 15)))
                .addParagraph(p -> p.text("გამარჯობა · ᲒᲐᲛᲐᲠᲯᲝᲑᲐ")
                        .textStyle(style(FontName.NOTO_SANS_GEORGIAN, 15)))
                .addParagraph(p -> p.text("Բարև աշխարհ")
                        .textStyle(style(FontName.NOTO_SANS_ARMENIAN, 15)))
                .addParagraph(p -> p.text("안녕하세요 · Müller")
                        .textStyle(style(FontName.GOTHIC_A1, 15))));
    }

    /**
     * Lays the scenario out once and holds it to both baselines.
     *
     * <p>One document, two assertions: the coordinates first, because when both fail that is
     * the one that says which node moved.</p>
     */
    private static void assertBothWays(String name, int width, int height,
                                       Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content)
            throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(width, height)
                .margin(DocumentInsets.of(18))
                .create()) {

            document.pageFlow(content::accept);

            LayoutSnapshotAssertions.assertMatches(document, "rtl/" + name);
            visualFor(width, height).assertMatchesBaseline("rtl-" + name, document);
        }
    }

    private static DocumentTextStyle style(FontName font, double size) {
        return DocumentTextStyle.builder().fontName(font).size(size).color(INK).build();
    }
}
