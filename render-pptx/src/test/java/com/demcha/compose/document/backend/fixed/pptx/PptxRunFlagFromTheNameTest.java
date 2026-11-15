package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which face a deck asks the viewer for, read off the written runs rather than off the mapping.
 *
 * <p>A run's bold flag is what a viewer picks the face by, and a deck takes it from two places: the
 * span's decoration, and a style suffix at the end of the name the family is registered under. The
 * unit cases beside these, in {@code PptxFontMappingTest}, answer the rule; these answer the deck.
 * </p>
 *
 * <p>Measured in PowerPoint 16 on a 45-character line at 20pt, with the faces marked in one glyph
 * so no other build of them could pass for them: under a name that merely contains the token, the
 * plain paragraph was drawn from the bold program and came out 12.90pt wider than the frame the
 * engine had sized for it. Under a registration per face, the flag is the only signal there is —
 * taken away, that paragraph came out 12.88pt short of the frame, from the regular program.</p>
 */
class PptxRunFlagFromTheNameTest {

    private static final String LATO_REGULAR = "fonts/google/lato/Lato-Regular.ttf";
    private static final String LATO_BOLD = "fonts/google/lato/Lato-Bold.ttf";
    private static final String LATO_ITALIC = "fonts/google/lato/Lato-Italic.ttf";

    @TempDir
    Path directory;

    /**
     * A family of two faces under a name that merely contains the token: the plain run stays plain.
     *
     * <p>Both runs are asserted, because the defect was not that the bold run lost its flag but
     * that the plain one gained one: the deck asked for a face the page had not measured.</p>
     */
    @Test
    void aTokenTheNameKeepsLeavesThePlainRunPlain() throws Exception {
        byte[] deck = twoFaceDeck("Acme Sans Bold");

        assertThat(boldFlags(deck))
                .as("the plain paragraph first, then the bold one: the decoration decides for a "
                        + "family whose name is not a face")
                .containsExactly(false, true);
    }

    /**
     * The arrangement where the name is the only signal: one registration per face.
     *
     * <p>Two definitions under one {@code wordFamily}, each holding one file, and two paragraphs
     * neither of which carries a decoration. Nothing but the name can say that the second wants
     * the bold program, and the flag says it.</p>
     */
    @Test
    void aPerFaceRegistrationStillAsksForItsFace() throws Exception {
        Path regular = Files.write(directory.resolve("regular.ttf"), PptxDeck.face(LATO_REGULAR));
        Path bold = Files.write(directory.resolve("bold.ttf"), PptxDeck.face(LATO_BOLD));
        FontName plainName = FontName.of("Acme-Sans");
        FontName faceName = FontName.of("Acme-Sans-Bold");
        List<FontFamilyDefinition> families = List.of(
                FontFamilyDefinition.files(plainName, regular).wordFamily("Acme Sans").build(),
                FontFamilyDefinition.files(faceName, bold).wordFamily("Acme Sans").build());
        DocumentTextStyle plain = DocumentTextStyle.builder().fontName(plainName).size(18).build();
        DocumentTextStyle face = DocumentTextStyle.builder().fontName(faceName).size(18).build();

        byte[] deck = PptxDeck.registering(families, page -> page
                .addParagraph(p -> p.text("Plain").textStyle(plain))
                .addParagraph(p -> p.text("Face").textStyle(face)));

        assertThat(boldFlags(deck))
                .as("the second paragraph names the face and nothing else does")
                .containsExactly(false, true);
    }

    /**
     * The limit this rule does not reach, pinned so that it is a known state and not a surprise.
     *
     * <p>One definition under a name ending in the suffix, holding a regular file <em>and</em> a
     * bold one: every run of it is flagged, the plain ones included, and PowerPoint 16 drew that
     * plain paragraph from the bold program at 544.80pt where the page laid out 531.90pt — the same
     * 12.90pt over the frame as the name that merely contains the token. Closing this needs the
     * flag keyed on the registration rather than on the name, which is not what this rule does.</p>
     */
    @Test
    void aShedSuffixOverTwoFacesStillFlagsThePlainRun() throws Exception {
        byte[] deck = twoFaceDeck("Acme-Sans-Bold");

        assertThat(boldFlags(deck))
                .as("the known limit: the name speaks for a family the decoration could have "
                        + "spoken for")
                .containsExactly(true, true);
    }

    /** A name with no style token at all, so that the comparisons above have a floor. */
    @Test
    void aNameWithoutATokenLeavesTheDecorationInCharge() throws Exception {
        byte[] deck = twoFaceDeck("Acme Sans");

        assertThat(boldFlags(deck))
                .as("nothing in this name can speak, so only the decoration does")
                .containsExactly(false, true);
    }

    /**
     * The same rule on the other axis, since the mapping reads two tokens and the cases above read
     * one: a family of a regular and an italic face under a name that merely contains {@code
     * Italic} leaves the plain run upright.
     */
    @Test
    void theItalicAxisFollowsTheSameRule() throws Exception {
        Path regular = Files.write(directory.resolve("regular.ttf"), PptxDeck.face(LATO_REGULAR));
        Path italic = Files.write(directory.resolve("italic.ttf"), PptxDeck.face(LATO_ITALIC));
        FontName kept = FontName.of("Acme Sans Italic");
        FontName shed = FontName.of("Acme-Sans-Italic");

        assertThat(italicFlags(italicDeck(kept, regular, italic)))
                .as("a token the name keeps leaves the upright run upright")
                .containsExactly(false, true);
        assertThat(italicFlags(italicDeck(shed, regular, italic)))
                .as("a name ending in the suffix asks for the italic face on both runs")
                .containsExactly(true, true);
    }

    private byte[] italicDeck(FontName name, Path regular, Path italic) {
        FontFamilyDefinition family = FontFamilyDefinition.files(name, regular)
                .italicPath(italic).wordFamily("Acme Sans").build();
        DocumentTextStyle plain = DocumentTextStyle.builder().fontName(name).size(18).build();
        DocumentTextStyle slanted = DocumentTextStyle.builder().fontName(name).size(18)
                .decoration(DocumentTextDecoration.ITALIC).build();
        return PptxDeck.registering(family, page -> page
                .addParagraph(p -> p.text("Upright").textStyle(plain))
                .addParagraph(p -> p.text("Italic").textStyle(slanted)));
    }

    private byte[] twoFaceDeck(String registered) throws Exception {
        Path regular = Files.write(directory.resolve("regular.ttf"), PptxDeck.face(LATO_REGULAR));
        Path bold = Files.write(directory.resolve("bold.ttf"), PptxDeck.face(LATO_BOLD));
        FontName name = FontName.of(registered);
        FontFamilyDefinition family = FontFamilyDefinition.files(name, regular)
                .boldPath(bold).wordFamily("Acme Sans").build();
        DocumentTextStyle plain = DocumentTextStyle.builder().fontName(name).size(18).build();
        DocumentTextStyle bolder = DocumentTextStyle.builder().fontName(name).size(18)
                .decoration(DocumentTextDecoration.BOLD).build();
        return PptxDeck.registering(family, page -> page
                .addParagraph(p -> p.text("Plain").textStyle(plain))
                .addParagraph(p -> p.text("Bold").textStyle(bolder)));
    }

    /**
     * Every run's bold flag, in the order the slide holds them, read through POI as the tests
     * beside this one read runs — the attribute a viewer goes by, not a text match on the part.
     */
    private static List<Boolean> boldFlags(byte[] deck) throws Exception {
        return flagsOf(deck, XSLFTextRun::isBold);
    }

    /** The same, on the italic axis. */
    private static List<Boolean> italicFlags(byte[] deck) throws Exception {
        return flagsOf(deck, XSLFTextRun::isItalic);
    }

    private static List<Boolean> flagsOf(byte[] deck, Function<XSLFTextRun, Boolean> flag)
            throws Exception {
        List<Boolean> flags = new ArrayList<>();
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(deck))) {
            for (XSLFShape shape : show.getSlides().get(0).getShapes()) {
                if (shape instanceof XSLFTextShape textShape) {
                    for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
                        for (XSLFTextRun run : paragraph.getTextRuns()) {
                            flags.add(flag.apply(run));
                        }
                    }
                }
            }
        }
        return flags;
    }
}
