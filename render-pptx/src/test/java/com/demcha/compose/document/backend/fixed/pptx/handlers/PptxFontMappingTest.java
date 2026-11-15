package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PptxFontMappingTest {

    /**
     * Anti-tautology pin: production and the geometry-test mirror share one
     * ascent resolver, so this hand-computed constant is what actually proves
     * the seat. Arial's hhea ascent is 1854/2048 em — at 22pt the viewer
     * ascent must be 22 × 1854 / 2048 = 19.916015625 pt exactly.
     */
    @Test
    void helveticaViewerAscentRatioIsArialsHheaAscent() {
        assertThat(PptxFontMapping.viewerAscentRatio(FontName.HELVETICA, 0.5) * 22.0)
                .isEqualTo(19.916015625);
        assertThat(PptxFontMapping.viewerAscentRatio(FontName.of("SomeCustom"), 0.5))
                .as("unknown families fall back to the supplied ratio")
                .isEqualTo(0.5);
    }

    @Test
    void mapsStandardFamiliesToMetricCompatibleViewerFonts() {
        assertThat(PptxFontMapping.familyFor(FontName.HELVETICA_BOLD)).isEqualTo("Arial");
        assertThat(PptxFontMapping.familyFor(FontName.TIMES_ITALIC)).isEqualTo("Times New Roman");
        assertThat(PptxFontMapping.familyFor(FontName.COURIER)).isEqualTo("Courier New");
        assertThat(PptxFontMapping.familyFor(FontName.of("Acme-Sans"))).isEqualTo("Acme-Sans");
        assertThat(PptxFontMapping.familyFor(FontName.of("Acme-Sans-Bold")))
                .as("anything but a standard-14 alias comes back as written: the style suffix "
                        + "used to be taken off here, which no reachable name ever was — a "
                        + "registered family declares its own wordFamily and never arrives, and "
                        + "a name nothing registers must resolve in the font library to be laid "
                        + "out at all")
                .isEqualTo("Acme-Sans-Bold");
    }

    @Test
    void decorationDrivesTheFace() {
        TextStyle decorated = new TextStyle(FontName.HELVETICA, 12,
                TextDecoration.BOLD_ITALIC, Color.BLACK);

        assertThat(PptxFontMapping.isBold(decorated, PptxFontMapping.ANY_TOKEN)).isTrue();
        assertThat(PptxFontMapping.isItalic(decorated, PptxFontMapping.ANY_TOKEN)).isTrue();
    }

    /**
     * The standard-14 style variants are family aliases: the engine resolves
     * {@code Helvetica-Bold} to the Helvetica family and takes the face from
     * the decoration, so a span naming one without a decoration is measured
     * with the regular metrics. Claiming the flag here would make the viewer
     * draw a wider face than the frame was sized for.
     */
    @Test
    void standard14FaceSuffixDoesNotBecomeARunFlag() {
        TextStyle aliasedBold = new TextStyle(FontName.HELVETICA_BOLD, 12,
                TextDecoration.DEFAULT, Color.BLACK);
        TextStyle aliasedOblique = new TextStyle(FontName.COURIER_BOLD_OBLIQUE, 12,
                TextDecoration.DEFAULT, Color.BLACK);

        assertThat(PptxFontMapping.isBold(aliasedBold, PptxFontMapping.ANY_TOKEN)).isFalse();
        assertThat(PptxFontMapping.isItalic(aliasedBold, PptxFontMapping.ANY_TOKEN)).isFalse();
        assertThat(PptxFontMapping.isBold(aliasedOblique, PptxFontMapping.ANY_TOKEN)).isFalse();
        assertThat(PptxFontMapping.isItalic(aliasedOblique, PptxFontMapping.ANY_TOKEN)).isFalse();
    }

    /**
     * A binary family keeps its own name and is measured as itself, so its
     * suffix is the only signal of the face and must still travel as a flag.
     *
     * <p>The reachable shape of that: one registration per face under one {@code wordFamily}.
     * Measured in PowerPoint 16, the flag is what draws the second of them from the bold program —
     * taken away, its paragraph came out 12.88pt short of the frame, from the regular one.</p>
     */
    @Test
    void binaryFamilySuffixStillCarriesTheFace() {
        TextStyle bold = new TextStyle(FontName.of("Acme-Sans-Bold"), 12,
                TextDecoration.DEFAULT, Color.BLACK);
        TextStyle italic = new TextStyle(FontName.of("Acme-Sans-Italic"), 12,
                TextDecoration.DEFAULT, Color.BLACK);

        assertThat(PptxFontMapping.isBold(bold, PptxFontMapping.ANY_TOKEN)).isTrue();
        assertThat(PptxFontMapping.isItalic(italic, PptxFontMapping.ANY_TOKEN)).isTrue();
    }

    /**
     * A style token that is part of the family's own name is not a face.
     *
     * <p>The flag comes from the suffix list and from no other reading of the name, where the
     * family's own faces leave the name free to speak at all — which is what {@code ANY_TOKEN}
     * stands for here. A token the name carries any other way is part of the family's own name,
     * whatever the family carries. Measured in
     * PowerPoint 16 on a 45-character line at 20pt, a family of two faces registered under such a
     * name had its <em>plain</em> paragraph drawn from the bold program, 12.90pt wider than the
     * frame the engine sized for it; with one regular face under the same name, the line came out
     * 9.17pt short of it instead. The four names below are the shapes a caller reaches: the token
     * behind a space, glued inside a word, glued at the end of one, and an italic one inside a
     * word.</p>
     */
    @Test
    void aStyleTokenTheNameKeepsIsNotAFace() {
        TextStyle spaced = new TextStyle(FontName.of("Acme Sans Bold"), 12,
                TextDecoration.DEFAULT, Color.BLACK);
        TextStyle inside = new TextStyle(FontName.of("Boldoni Text"), 12,
                TextDecoration.DEFAULT, Color.BLACK);
        TextStyle semi = new TextStyle(FontName.of("Oswald SemiBold"), 12,
                TextDecoration.DEFAULT, Color.BLACK);
        TextStyle italicInside = new TextStyle(FontName.of("Italica Display"), 12,
                TextDecoration.DEFAULT, Color.BLACK);

        assertThat(PptxFontMapping.isBold(spaced, PptxFontMapping.ANY_TOKEN)).isFalse();
        assertThat(PptxFontMapping.isBold(inside, PptxFontMapping.ANY_TOKEN)).isFalse();
        assertThat(PptxFontMapping.isBold(semi, PptxFontMapping.ANY_TOKEN)).isFalse();
        assertThat(PptxFontMapping.isItalic(italicInside, PptxFontMapping.ANY_TOKEN)).isFalse();
        assertThat(PptxFontMapping.isBold(new TextStyle(FontName.of("Acme Sans Bold"), 12,
                TextDecoration.BOLD, Color.BLACK), PptxFontMapping.ANY_TOKEN))
                .as("the decoration still says which face such a family wants")
                .isTrue();
    }

    /**
     * Which name says what, written out by hand rather than derived from the code under test.
     *
     * <p>The expectations are a table and not a second reading of the suffix list, so this fails
     * both ways round: a token read out of the middle of a name reddens the lower rows, and a
     * spelling dropped from the list reddens the upper ones. Every row is a name with no
     * registration behind it, which is the case where the name is allowed to speak at all —
     * what a <em>registered</em> family's faces leave for its name is the environment's to say,
     * and the deck cases pin that.</p>
     */
    @Test
    void whichNamesMaySpeakAndWhichMayNot() {
        assertFlags("Acme-Sans-Bold", true, false);
        assertFlags("Acme-Sans-Italic", false, true);
        assertFlags("Acme-Sans-Oblique", false, true);
        assertFlags("Acme-Sans-BoldItalic", true, true);
        assertFlags("Acme-Sans-Bold-Italic", true, true);
        assertFlags("Acme-Sans-BoldOblique", true, true);
        assertFlags("Acme-Sans-Bold-Oblique", true, true);
        assertFlags("Acme-Sans-Regular", false, false);
        assertFlags("Acme-Sans", false, false);
        assertFlags("Acme Sans Bold", false, false);
        assertFlags("Boldoni Text", false, false);
        assertFlags("Oswald SemiBold", false, false);
        assertFlags("Italica Display", false, false);
    }

    private static void assertFlags(String name, boolean bold, boolean italic) {
        TextStyle style = new TextStyle(FontName.of(name), 12, TextDecoration.DEFAULT, Color.BLACK);
        assertThat(PptxFontMapping.isBold(style, PptxFontMapping.ANY_TOKEN))
                .as("%s: bold", name)
                .isEqualTo(bold);
        assertThat(PptxFontMapping.isItalic(style, PptxFontMapping.ANY_TOKEN))
                .as("%s: italic", name)
                .isEqualTo(italic);
    }

    /**
     * And what the family's own faces leave for the name: the second argument, on its own.
     *
     * <p>The environment works this out from the faces a registered family carries; here it is
     * passed by hand, so that the two halves of the rule — which names <em>could</em> speak, and
     * which families <em>let</em> them — are pinned apart rather than only together.</p>
     */
    @Test
    void aFamilyThatNamesItsOwnFacesSilencesTheName() {
        TextStyle bold = new TextStyle(FontName.of("Acme-Sans-Bold"), 12,
                TextDecoration.DEFAULT, Color.BLACK);
        TextStyle italic = new TextStyle(FontName.of("Acme-Sans-Italic"), 12,
                TextDecoration.DEFAULT, Color.BLACK);

        assertThat(PptxFontMapping.isBold(bold, Set.of()))
                .as("a family carrying two or more faces distinguishes them by the slot a run "
                        + "asks for, so its name says nothing")
                .isFalse();
        assertThat(PptxFontMapping.isItalic(italic, Set.of())).isFalse();
        assertThat(PptxFontMapping.isBold(bold, Set.of("bold")))
                .as("a family carrying one bold program may be named bold")
                .isTrue();
        assertThat(PptxFontMapping.isItalic(italic, Set.of("bold")))
                .as("and not italic, which that program is not")
                .isFalse();
        assertThat(PptxFontMapping.isBold(new TextStyle(FontName.of("Acme-Sans-Bold"), 12,
                TextDecoration.BOLD, Color.BLACK), Set.of()))
                .as("the decoration is never silenced: it is the span's own word")
                .isTrue();
    }

    /**
     * A spelling with the hyphen inside the pair still carries both halves of the face.
     *
     * <p>The suffix list holds {@code -Bold-Italic} ahead of {@code -Italic} for this: read the
     * other way round, such a name would ask the viewer for an italic face that is not bold, on a
     * run the page measured in the bold-italic program — a lighter face than the frame was sized
     * for, which is the same defect in the other direction.</p>
     */
    @Test
    void aHyphenInsideThePairStillCarriesBothHalves() {
        TextStyle boldItalic = new TextStyle(FontName.of("Acme-Sans-Bold-Italic"), 12,
                TextDecoration.DEFAULT, Color.BLACK);
        TextStyle boldOblique = new TextStyle(FontName.of("Acme-Sans-Bold-Oblique"), 12,
                TextDecoration.DEFAULT, Color.BLACK);

        assertThat(PptxFontMapping.isBold(boldItalic, PptxFontMapping.ANY_TOKEN)).isTrue();
        assertThat(PptxFontMapping.isItalic(boldItalic, PptxFontMapping.ANY_TOKEN)).isTrue();
        assertThat(PptxFontMapping.isBold(boldOblique, PptxFontMapping.ANY_TOKEN)).isTrue();
        assertThat(PptxFontMapping.isItalic(boldOblique, PptxFontMapping.ANY_TOKEN)).isTrue();
    }
}
