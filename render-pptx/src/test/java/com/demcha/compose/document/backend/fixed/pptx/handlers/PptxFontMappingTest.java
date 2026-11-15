package com.demcha.compose.document.backend.fixed.pptx.handlers;

import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Locale;

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
        assertThat(PptxFontMapping.familyFor(FontName.of("Acme-Sans-Bold"))).isEqualTo("Acme-Sans");
    }

    @Test
    void decorationDrivesTheFace() {
        TextStyle decorated = new TextStyle(FontName.HELVETICA, 12,
                TextDecoration.BOLD_ITALIC, Color.BLACK);

        assertThat(PptxFontMapping.isBold(decorated)).isTrue();
        assertThat(PptxFontMapping.isItalic(decorated)).isTrue();
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

        assertThat(PptxFontMapping.isBold(aliasedBold)).isFalse();
        assertThat(PptxFontMapping.isItalic(aliasedBold)).isFalse();
        assertThat(PptxFontMapping.isBold(aliasedOblique)).isFalse();
        assertThat(PptxFontMapping.isItalic(aliasedOblique)).isFalse();
    }

    /**
     * A binary family keeps its own name and is measured as itself, so its
     * suffix is the only signal of the face and must still travel as a flag.
     *
     * <p>The reachable shape of that: one registration per face under one {@code wordFamily}.
     * Measured in PowerPoint 16, the flag is what draws the second of them from the bold program —
     * taken away, its paragraph came out 12.88pt narrower, from the regular one.</p>
     */
    @Test
    void binaryFamilySuffixStillCarriesTheFace() {
        TextStyle bold = new TextStyle(FontName.of("Acme-Sans-Bold"), 12,
                TextDecoration.DEFAULT, Color.BLACK);
        TextStyle italic = new TextStyle(FontName.of("Acme-Sans-Italic"), 12,
                TextDecoration.DEFAULT, Color.BLACK);

        assertThat(PptxFontMapping.isBold(bold)).isTrue();
        assertThat(PptxFontMapping.isItalic(italic)).isTrue();
    }

    /**
     * A style token that is part of the family's own name is not a face.
     *
     * <p>The flag and the declared family are two readings of one name: {@code familyFor} takes a
     * suffix off the end and nothing else, so a token the name keeps must not become a flag.
     * Measured in PowerPoint 16 on a 45-character line at 20pt, a family of two faces registered
     * under such a name had its <em>plain</em> paragraph drawn from the bold program, 12.90pt wider
     * than the frame the engine sized for it; with one regular face under the same name, the line
     * came out 9.17pt short of it instead. The pairs below are the two shapes a caller reaches:
     * the token at the end behind a space, and the token inside a word.</p>
     */
    @Test
    void aStyleTokenTheNameKeepsIsNotAFace() {
        TextStyle spaced = new TextStyle(FontName.of("Acme Sans Bold"), 12,
                TextDecoration.DEFAULT, Color.BLACK);
        TextStyle inside = new TextStyle(FontName.of("Boldoni Text"), 12,
                TextDecoration.DEFAULT, Color.BLACK);
        TextStyle semi = new TextStyle(FontName.of("Oswald SemiBold"), 12,
                TextDecoration.DEFAULT, Color.BLACK);
        TextStyle obliqueInside = new TextStyle(FontName.of("Italica Display"), 12,
                TextDecoration.DEFAULT, Color.BLACK);

        assertThat(PptxFontMapping.isBold(spaced)).isFalse();
        assertThat(PptxFontMapping.isBold(inside)).isFalse();
        assertThat(PptxFontMapping.isBold(semi)).isFalse();
        assertThat(PptxFontMapping.isItalic(obliqueInside)).isFalse();
        assertThat(PptxFontMapping.isBold(new TextStyle(FontName.of("Acme Sans Bold"), 12,
                TextDecoration.BOLD, Color.BLACK)))
                .as("the decoration still says which face such a family wants")
                .isTrue();
    }

    /**
     * The agreement between the two readings of a name, rather than the cases: a name contributes
     * a flag exactly where this class's own stripping takes that token off it.
     *
     * <p>Written as the two readings side by side, so a change to either one that leaves them
     * disagreeing fails here rather than in a deck. It cannot catch a change to the <em>list</em>
     * they share, since both move together then — the deck cases beside it catch that.
     * {@code -Regular} is the suffix that strips and carries no face, which is what keeps the rule
     * from reading "ends in something" as "is bold".</p>
     */
    @Test
    void theFlagFollowsWhatTheDeclaredFamilySheds() {
        for (String name : new String[]{
                "Acme-Sans-Bold", "Acme-Sans-Italic", "Acme-Sans-BoldItalic",
                "Acme-Sans-Oblique", "Acme-Sans-BoldOblique", "Acme-Sans-Regular",
                "Acme Sans Bold", "Boldoni Text", "Oswald SemiBold", "Acme-Sans"}) {
            FontName fontName = FontName.of(name);
            TextStyle style = new TextStyle(fontName, 12, TextDecoration.DEFAULT, Color.BLACK);
            boolean sheds = !PptxFontMapping.familyFor(fontName).equals(name);
            boolean flagged = PptxFontMapping.isBold(style) || PptxFontMapping.isItalic(style);
            assertThat(flagged)
                    .as("%s: flagged %s, declared as \"%s\"", name, flagged,
                            PptxFontMapping.familyFor(fontName))
                    .isEqualTo(sheds && !name.toLowerCase(Locale.ROOT).endsWith("-regular"));
        }
    }
}
