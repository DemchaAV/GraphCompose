package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontFamilyDefinition;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A carried face whose program states a family of its own.
 *
 * <p>An embedded face can be matched to the declared typeface by the family the program states for
 * itself, not by the name the container carries. Measured in PowerPoint 16 on the bundled
 * {@code AsapCondensed-Italic}, whose own family record reads {@code Asap Condensed Condensed
 * Regular} where its three siblings read {@code Asap Condensed}: declared as {@code Asap
 * Condensed}, the italic paragraph came out in Arial Italic at 566.51pt where the program itself
 * draws 443.96pt, on a 45-character line at 20pt, while the same deck's regular paragraph was
 * drawn from its program. Rewriting that one record to the declared name had the program drawn;
 * rewriting it to the string it already held changed nothing, so the rewriting was not what the
 * viewer went by.</p>
 *
 * <p>What that costs is the arrangement above and not the record on its own: a family whose faces
 * state <em>different</em> names, where the odd one is the face replaced. Two arrangements of the
 * same condition were measured drawing the carried program after all — a face in the bold element
 * stating another family (439.86pt against the page's 439.70pt) and an italic face of a family
 * whose faces all state the <em>same</em> other name (497.95pt against the page's 497.68pt) — so
 * the line says what the two names are and names no outcome. Each of those declared a name no font
 * on the measuring machine answers to, which is where the carried program can be seen at all:
 * under a name it does have one for, the deck may be drawn from that font instead, which the
 * backend capability details carry as a limit of its own.</p>
 *
 * <p>Nothing is refused for it. The program travels as the caller asked and no deck's bytes change
 * — what the export adds is the warning, so the reason is in the log rather than only in the
 * picture.</p>
 */
class PptxFontNameMismatchTest {

    /** The bundled family whose italic face states a family of its own. */
    private static final String ASAP_REGULAR = "fonts/google/asapcondensed/AsapCondensed-Regular.ttf";
    private static final String ASAP_ITALIC = "fonts/google/asapcondensed/AsapCondensed-Italic.ttf";
    /** A family whose every face states the name the family declares. */
    private static final String LATO_REGULAR = "fonts/google/lato/Lato-Regular.ttf";
    private static final String LATO_ITALIC = "fonts/google/lato/Lato-Italic.ttf";

    @TempDir
    Path directory;

    private RenderLogCapture log;

    @BeforeEach
    void captureRenderLog() {
        log = RenderLogCapture.attach();
    }

    @AfterEach
    void releaseRenderLog() {
        log.close();
    }

    @Test
    void theFamilyAFaceStatesForItselfIsWhatAPresentationMatchesOn() throws Exception {
        // Read through the backend's own reader, so the two cases below rest on what the files
        // hold rather than on what this test says they hold.
        assertThat(PptxEmbeddedFont.statedFamilyOf(PptxDeck.face(ASAP_ITALIC)))
                .isEqualTo("Asap Condensed Condensed Regular");
        assertThat(PptxEmbeddedFont.statedFamilyOf(PptxDeck.face(ASAP_REGULAR)))
                .isEqualTo("Asap Condensed");
        assertThat(PptxEmbeddedFont.statedFamilyOf(PptxDeck.face(LATO_ITALIC)))
                .isEqualTo("Lato");
        assertThat(PptxEmbeddedFont.statedFamilyOf(PptxDeck.face(LATO_REGULAR)))
                .isEqualTo("Lato");
    }

    @Test
    void aFaceStatingAnotherFamilyThanTheDeckDeclaresIsCarriedAndNamed() throws Exception {
        Path regular = Files.write(directory.resolve("AsapCondensed-Regular.ttf"),
                PptxDeck.face(ASAP_REGULAR));
        Path italic = Files.write(directory.resolve("AsapCondensed-Italic.ttf"),
                PptxDeck.face(ASAP_ITALIC));
        FontFamilyDefinition family = FontFamilyDefinition.files("asap-condensed", regular)
                .italicPath(italic).wordFamily("Asap Condensed").build();

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(PptxDeck.fontParts(deck))
                .as("both faces travel: this is not a refusal")
                .hasSize(2);
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("and the deck declares the family the caller asked for, not the one the "
                        + "italic face states — the longer name contains the shorter, so the "
                        + "attribute is asserted whole")
                .contains("typeface=\"Asap Condensed\"");

        assertThat(log.messages("render.pptx.font.name.mismatch"))
                .as("the face is named, with the family it states and the one the deck declares "
                        + "— a reader has to see both to know which of the two to go after, and "
                        + "which face of the family it is")
                .singleElement(InstanceOfAssertFactories.STRING)
                .as("the declared name whole: the stated one begins with it, so a prefix match "
                        + "would pass for either")
                .contains("typeface=Asap Condensed face=")
                .contains("states=Asap Condensed Condensed Regular")
                .contains(italic.toString());
    }

    @Test
    void oneBundledFaceStatesAFamilyOtherThanItsOwnFamilyDeclares() throws Exception {
        // The catalogue against the reading, as its two siblings in this package measure their
        // own guards against it. The count is quoted in the capability details and on the
        // font-coverage page, and a font bump is exactly what would make both of them false
        // without a line of code changing — so it is measured here rather than written down
        // twice and believed. Deduped the way the embedder dedupes.
        Map<String, String> differing = new LinkedHashMap<>();
        int faces = 0;
        for (FontFamilyDefinition family : DefaultFonts.bundledFamilies()) {
            if (family.fontSourceSet().isEmpty()) {
                continue;
            }
            FontFamilyDefinition.FontSourceSet sources = family.fontSourceSet().get();
            Set<String> read = new LinkedHashSet<>();
            for (FontFamilyDefinition.FontBinarySource source : List.of(sources.regular(),
                    sources.bold(), sources.italic(), sources.boldItalic())) {
                if (!read.add(source.description())) {
                    continue;
                }
                faces++;
                byte[] face;
                try (InputStream stream = source.openStream()) {
                    face = stream.readAllBytes();
                }
                String stated = PptxEmbeddedFont.statedFamilyOf(face);
                if (stated != null && !stated.equalsIgnoreCase(family.wordFamily())) {
                    differing.put(source.description(), stated);
                }
            }
        }

        assertThat(faces)
                .as("the premise: this walked the catalogue rather than an empty list")
                .isGreaterThan(100);
        assertThat(differing)
                .as("the one bundled face whose own family record is not the name its family "
                        + "declares, which is the count both documents quote")
                .hasSize(1);
        assertThat(differing.keySet().iterator().next()).contains("AsapCondensed-Italic");
        assertThat(differing.values().iterator().next())
                .isEqualTo("Asap Condensed Condensed Regular");
    }

    @Test
    void aFamilyWhoseFacesAllStateItsOwnNameIsNotNamed() throws Exception {
        Path regular = Files.write(directory.resolve("Lato-Regular.ttf"),
                PptxDeck.face(LATO_REGULAR));
        Path italic = Files.write(directory.resolve("Lato-Italic.ttf"),
                PptxDeck.face(LATO_ITALIC));
        FontFamilyDefinition family = FontFamilyDefinition.files("lato-pair", regular)
                .italicPath(italic).wordFamily("Lato").build();

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(PptxDeck.fontParts(deck))
                .as("the premise: this family is carried, so silence is about the names and "
                        + "not about an empty deck")
                .hasSize(2);
        assertThat(log.messages("render.pptx.font.name.mismatch"))
                .as("every face states the name the deck declares, so there is nothing to say")
                .isEmpty();
    }
}
