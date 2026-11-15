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
     * taken away, its paragraph came out 12.88pt short of the frame, from the regular one.</p>
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
     * <p>The flag comes from the suffix list this class strips names by, and from nothing else; a
     * token the name carries any other way is part of the family's own name. Measured in
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

        assertThat(PptxFontMapping.isBold(spaced)).isFalse();
        assertThat(PptxFontMapping.isBold(inside)).isFalse();
        assertThat(PptxFontMapping.isBold(semi)).isFalse();
        assertThat(PptxFontMapping.isItalic(italicInside)).isFalse();
        assertThat(PptxFontMapping.isBold(new TextStyle(FontName.of("Acme Sans Bold"), 12,
                TextDecoration.BOLD, Color.BLACK)))
                .as("the decoration still says which face such a family wants")
                .isTrue();
    }

    /**
     * The rule itself rather than the cases: a name contributes a flag exactly where this class's
     * own stripping takes a style suffix off it.
     *
     * <p>Written as the two readings side by side — the flag, and what {@code familyFor} strips the
     * name to — so that a change to either one that leaves them disagreeing fails here rather than
     * in a deck. What a deck <em>declares</em> is not either of them: a registered family declares
     * its {@code wordFamily()}, suffix and all. This cannot catch a change to the <em>list</em> the
     * two share, since both move together then — the deck cases beside it catch that.
     * {@code -Regular} is the suffix that strips and carries no face, which is what keeps the rule
     * from reading "ends in something" as "is bold".</p>
     */
    @Test
    void theFlagFollowsTheSuffixThisClassStripsBy() {
        for (String name : new String[]{
                "Acme-Sans-Bold", "Acme-Sans-Italic", "Acme-Sans-BoldItalic",
                "Acme-Sans-Bold-Italic", "Acme-Sans-Oblique", "Acme-Sans-BoldOblique",
                "Acme-Sans-Bold-Oblique", "Acme-Sans-Regular",
                "Acme Sans Bold", "Boldoni Text", "Oswald SemiBold", "Acme-Sans"}) {
            FontName fontName = FontName.of(name);
            TextStyle style = new TextStyle(fontName, 12, TextDecoration.DEFAULT, Color.BLACK);
            boolean stripped = !PptxFontMapping.familyFor(fontName).equals(name);
            boolean flagged = PptxFontMapping.isBold(style) || PptxFontMapping.isItalic(style);
            assertThat(flagged)
                    .as("%s: flagged %s, stripped to \"%s\"", name, flagged,
                            PptxFontMapping.familyFor(fontName))
                    .isEqualTo(stripped && !name.toLowerCase(Locale.ROOT).endsWith("-regular"));
        }
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

        assertThat(PptxFontMapping.isBold(boldItalic)).isTrue();
        assertThat(PptxFontMapping.isItalic(boldItalic)).isTrue();
        assertThat(PptxFontMapping.isBold(boldOblique)).isTrue();
        assertThat(PptxFontMapping.isItalic(boldOblique)).isTrue();
        assertThat(PptxFontMapping.familyFor(FontName.of("Acme-Sans-Bold-Italic")))
                .as("and the name strips to the family, not to \"Acme-Sans-Bold\"")
                .isEqualTo("Acme-Sans");
    }
}
