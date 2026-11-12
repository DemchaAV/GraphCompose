package com.demcha.compose.document.backend.fixed.pptx;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * What a deck declares about the font programs it carries.
 *
 * <p>Embedding here is whole-font: a face travels with every glyph it has, because
 * nothing in this backend subsets one. A presentation says which of the two PowerPoint
 * offers it holds, and the one POI's blank template already carries is the other —
 * {@code saveSubsetFonts} is set in it before this backend touches anything, and
 * {@code XSLFFontInfo.addFacet} sets it again for every face it takes. So the only thing
 * the backend does with that flag is clear it, once, when a program did travel.</p>
 *
 * <p>The pairing that would misdescribe a deck is therefore a program in the package with
 * the flag left as it came: a reader asked to keep only the characters the slides use,
 * holding a face that was embedded whole. The first test holds every shape that carries a
 * program to the opposite of that, so a path that carries one without clearing the flag
 * fails here rather than in a viewer.</p>
 *
 * <p>The last holds the other side. A deck that carries nothing says so by leaving
 * embedding off, which is what makes the subset flag inert — whatever POI's template left
 * in it. The first test pins that template value, since everything here rests on the flag
 * arriving set. Reading the flags back through POI rather than out of the XML keeps the
 * tests off how the writer spells a boolean.</p>
 */
class PptxEmbeddedFontDeclarationTest {

    /** A face both readers take, so it reaches the package. */
    private static final String WHOLE = "fonts/google/lato/Lato-Regular.ttf";

    @TempDir
    Path directory;

    @Test
    void poisBlankPresentationAlreadyAsksForSubsetting() throws Exception {
        // The premise both tests below rest on, and the reason this backend only ever clears
        // the flag. A POI release that stopped setting it in the template would leave the
        // clear doing nothing visible and the two tests passing for the wrong reason.
        try (XMLSlideShow blank = new XMLSlideShow()) {
            assertThat(blank.getCTPresentation().isSetSaveSubsetFonts())
                    .as("the blank template carries the subset flag")
                    .isTrue();
            assertThat(blank.getCTPresentation().getSaveSubsetFonts())
                    .as("and carries it set")
                    .isTrue();
            assertThat(blank.getCTPresentation().isSetEmbedTrueTypeFonts())
                    .as("while embedding is not declared until a face is taken")
                    .isFalse();
        }
    }

    @Test
    void aDeckThatCarriesAProgramAsksForEveryCharacterOfIt() throws Exception {
        Path whole = Files.write(directory.resolve("Whole-Regular.ttf"), wholeFace());
        FontFamilyDefinition registered = FontFamilyDefinition
                .files("Whole", whole).wordFamily("Whole").build();

        byte[] registeredOnly = PptxDeck.registering(registered, drawnIn("Whole"));
        byte[] bundledOnly = PptxDeck.of(drawnIn(FontName.AMIRI.name(), "مرحبا بالعالم"));
        byte[] both = PptxDeck.registering(registered, page -> {
            drawnIn("Whole").accept(page);
            drawnIn(FontName.AMIRI.name(), "مرحبا بالعالم").accept(page);
        });

        // The premise of each shape, asserted rather than assumed: a shape that quietly
        // stopped carrying a program would satisfy everything below for the wrong reason.
        assertThat(PptxDeck.fontParts(registeredOnly))
                .as("a registered family travels").hasSize(1);
        assertThat(PptxDeck.fontParts(bundledOnly))
                .as("a bundled family the deck drew with travels").hasSize(1);
        assertThat(PptxDeck.fontParts(both))
                .as("and the two paths together carry one program each").hasSize(2);

        for (byte[] deck : new byte[][] {registeredOnly, bundledOnly, both}) {
            try (XMLSlideShow written = new XMLSlideShow(new ByteArrayInputStream(deck))) {
                assertThat(written.getCTPresentation().getEmbedTrueTypeFonts())
                        .as("a deck that carries a program says it is embedded")
                        .isTrue();
                assertThat(written.getCTPresentation().getSaveSubsetFonts())
                        .as("and asks the reader to keep every character of it, because "
                                + "nothing here is subsetted")
                        .isFalse();
            }
        }
    }

    @Test
    void aDeckThatCarriesNoProgramLeavesEmbeddingOff() throws Exception {
        byte[] face = wholeFace();
        Path truncated = Files.write(directory.resolve("Truncated-Regular.ttf"),
                Arrays.copyOf(face, face.length - 1));

        byte[] nothingRegistered = PptxDeck.of(page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));
        // A family the metrics reader refuses: registered, drawn in, and not carried.
        byte[] refused = PptxDeck.registering(FontFamilyDefinition
                .files("Truncated", truncated).wordFamily("Truncated").build(),
                drawnIn("Truncated"));

        for (byte[] deck : new byte[][] {nothingRegistered, refused}) {
            assertThat(PptxDeck.fontParts(deck))
                    .as("the premise: this deck carries no program").isEmpty();
            try (XMLSlideShow written = new XMLSlideShow(new ByteArrayInputStream(deck))) {
                assertThat(written.getCTPresentation().getEmbedTrueTypeFonts())
                        .as("so it leaves embedding off, which is what makes the subset "
                                + "flag POI's template carries inert")
                        .isFalse();
            }
        }
    }

    private static Consumer<PageFlowBuilder> drawnIn(String family) {
        return drawnIn(family, "Drawn in the family under test");
    }

    private static Consumer<PageFlowBuilder> drawnIn(String family, String text) {
        return page -> page.addParagraph(paragraph -> paragraph.text(text)
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.of(family)).size(18).build()));
    }

    private static byte[] wholeFace() throws Exception {
        return PptxDeck.face(WHOLE);
    }
}
