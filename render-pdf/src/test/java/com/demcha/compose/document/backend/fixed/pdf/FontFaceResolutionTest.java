package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.document.chart.ChartDefaults;
import com.demcha.compose.engine.render.pdf.PdfFont;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.font.FontLibrary;
import com.demcha.compose.font.FontName;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the rule that decides which glyph program a style actually gets: the font
 * <em>name</em> selects the family, the <em>decoration</em> selects the face within it.
 *
 * <p>The rule is easy to state and was invisible in the suite. {@code FontLibrary}
 * rewrites every standard-14 face constant to its base family before any lookup, so
 * {@code fontName(HELVETICA_BOLD)} contributes nothing and a style that sets no
 * decoration renders regular — silently, because the text still measures and still
 * draws. That is what the donut-centre KPI did.</p>
 *
 * <p>Asserted on the resolved {@code PDFont}, which is the glyph program the page
 * actually references, rather than on a rendered image: a weight difference is a font
 * resource difference, and reading it directly says which face without a pixel
 * threshold to argue about.</p>
 *
 * <p>These assertions are the ones that must change if the alias is ever made a real
 * fallback — which makes the behaviour change deliberate instead of incidental.</p>
 */
class FontFaceResolutionTest {

    private static final FontLibrary LIBRARY = PdfFontLibraryFactory.standardLibrary();

    private static String face(FontName name, TextDecoration decoration) {
        PdfFont font = LIBRARY.getFont(name, PdfFont.class).orElseThrow();
        return font.fontType(decoration).getName();
    }

    @Test
    void theDecorationSelectsTheFaceWithinTheFamily() {
        assertThat(face(FontName.HELVETICA, TextDecoration.DEFAULT)).isEqualTo("Helvetica");
        assertThat(face(FontName.HELVETICA, TextDecoration.BOLD)).isEqualTo("Helvetica-Bold");
        assertThat(face(FontName.TIMES_ROMAN, TextDecoration.BOLD)).isEqualTo("Times-Bold");
        assertThat(face(FontName.COURIER, TextDecoration.BOLD)).isEqualTo("Courier-Bold");
    }

    @Test
    void aFaceConstantIsAnAliasOfItsFamilyAndCarriesNoWeightOfItsOwn() {
        assertThat(face(FontName.HELVETICA_BOLD, TextDecoration.DEFAULT))
                .describedAs("naming the bold face without a decoration renders regular — "
                        + "the constant is rewritten to its family before the lookup")
                .isEqualTo("Helvetica");
        assertThat(face(FontName.TIMES_BOLD, TextDecoration.DEFAULT)).isEqualTo("Times-Roman");
        assertThat(face(FontName.COURIER_BOLD, TextDecoration.DEFAULT)).isEqualTo("Courier");

        assertThat(face(FontName.HELVETICA_BOLD, TextDecoration.BOLD))
                .describedAs("the alias and the family resolve identically once a decoration "
                        + "is set, which is why naming the family is the clearer form")
                .isEqualTo(face(FontName.HELVETICA, TextDecoration.BOLD));
    }

    /**
     * The defect this rule was traced from, pinned at its own site.
     *
     * <p>Asserting the rule alone would not catch the regression: a style that goes back
     * to naming the bold face and dropping the decoration still satisfies every general
     * assertion above. The chart layout tests do not catch it either — they measure
     * through fake metrics whose width depends on the character count and ignores the
     * face — so without this the donut KPI could quietly return to regular.</p>
     */
    @Test
    void theDonutCentreDefaultResolvesToABoldFace() {
        assertThat(face(ChartDefaults.DONUT_CENTER_TEXT_STYLE.fontName(),
                        TextDecoration.valueOf(ChartDefaults.DONUT_CENTER_TEXT_STYLE.decoration().name())))
                .describedAs("the donut-centre KPI must resolve to a bold glyph program. Naming "
                        + "the bold face and setting no decoration renders regular — that is the "
                        + "defect, and it is invisible to the chart layout tests")
                .isEqualTo("Helvetica-Bold");
    }

    @Test
    void anAliasedNameFollowsTheDecorationEvenWhenTheyDisagree() {
        assertThat(face(FontName.HELVETICA_BOLD, TextDecoration.ITALIC))
                .describedAs("the name says bold and the decoration says italic; the decoration "
                        + "wins, so the constant is actively misleading rather than redundant")
                .isEqualTo("Helvetica-Oblique");
    }
}
