package com.demcha.compose.document.backend.fixed.pptx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * A registered font family that cannot be carried costs the deck nothing but the font.
 *
 * <p>Embedding is best-effort by contract: a family whose licence forbids it, or whose
 * bytes cannot be read, costs the deck the font program rather than failing a render the
 * PDF backend would complete. A source may refuse <em>unchecked</em> — the
 * classpath one reports a missing resource with an {@link IllegalArgumentException} — and
 * only the checked refusal was caught, so a family registered and never drawn with was
 * enough to lose a deck that had already laid out.</p>
 *
 * <p>Degrading is not the same as failing fast. A family the deck actually draws in still
 * fails the render, because the page cannot measure a face it cannot read; the second test
 * pins that side, so widening the catch cannot turn a loud failure into a silent one.</p>
 *
 * <p>And degrading is for the family, not the face: the third test holds a family whose
 * bold face cannot be read to leaving nothing of itself in the deck, since one boolean
 * answers for the whole family.</p>
 *
 * <p>"Cannot be read" is two readers, not one. The embedder parses table headers with
 * fontbox and the viewer metrics read the program whole through {@link java.awt.Font},
 * and the two do not accept the same files: fontbox skips a table whose directory entry
 * runs past the end of the file, {@code Font.createFont} rejects the font for it. A font
 * truncated below its last table is accepted by the first and refused by the second. The
 * fourth and fifth tests hold that pair to one answer — the family is not carried, and
 * the deck is written — where the refusal used to arrive after the family was already in
 * the presentation, as an {@code IllegalStateException} that cost the whole deck. The
 * sixth is their control: a whole copy of the same face still embeds, so reading the
 * metrics before the hand-over is not refusing fonts that work.</p>
 */
class PptxUnreadableFontSourceTest {

    /** A resource no module on the test classpath ships. */
    private static final String ABSENT = "fonts/absent/Absent-Regular.ttf";

    /** A face both readers take, as the control for the truncated one below. */
    private static final String WHOLE = "fonts/google/lato/Lato-Regular.ttf";

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
    void aRegisteredFamilyWhoseClasspathResourceIsMissingIsNamedAndTheDeckIsWritten() throws Exception {
        // Anchored on the class the production source reads through, not on this test's.
        assertThat(FontFamilyDefinition.class.getResource("/" + ABSENT))
                .as("the premise: the resource is not on the classpath")
                .isNull();

        byte[] deck = render(page -> page.addParagraph(paragraph -> paragraph
                .text("Drawn in the default face")));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(slideText(deck))
                .as("the premise: the page laid out, and the slide carries its text")
                .contains("Drawn in the default face");
        assertThat(PptxDeck.fontParts(deck))
                .as("the face that could not be read, not carried")
                .isEmpty();
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("and the presentation declares no embedded font it has not got")
                .doesNotContain("embeddedFontLst")
                .doesNotContain("Absent");

        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the face it could not carry is named, with why")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Absent")
                .contains(ABSENT)
                .as("the premise: the source refused unchecked, the path this test guards")
                .contains("IllegalArgumentException");
        assertThat(PptxDeck.partsNaming(deck, "Absent"))
                .as("the premise for the line below: no part of the deck names the family")
                .isEmpty();
        assertThat(log.messages("render.pptx.font.substitution"))
                .as("so the reader is told the family is not carried, not that the deck "
                        + "references a family it never mentions")
                .filteredOn(warning -> warning.contains("Absent"))
                .singleElement(InstanceOfAssertFactories.STRING)
                .doesNotContain("the deck references")
                .contains("\"Absent\" is not carried whole")
                // Whole, not up to "referenced by name": that much is common to this line
                // and to the one it replaced, so a tail that goes back to predicting what a
                // viewer does would leave this green.
                .contains("text drawn in a face that did not travel is referenced by name, "
                        + "and needs that font installed where the deck is opened for "
                        + "identical glyphs");
    }

    @Test
    void aDeckDrawnInAFamilyWhoseClasspathResourceIsMissingStillFailsTheRender() {
        // The other side of the contract. Embedding is best-effort, measuring is not: a face
        // the page has to set text in cannot be substituted silently, so the render fails and
        // names the resource. Widening the embed catch must not reach this.
        assertThatThrownBy(() -> render(page -> page.addParagraph(paragraph -> paragraph
                .text("Set in a face that stays home")
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.of("Absent")).size(10).build()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ABSENT);

        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the premise: measurement refused before a slide was built, so embedding "
                        + "never ran and the widened catch cannot be what let this through")
                .isEmpty();
    }

    @Test
    void aFamilyOneOfWhoseFacesCannotBeReadIsCarriedWholeOrNotAtAll() throws Exception {
        // Embedding answers for the family, not the face: one boolean decides whether the
        // viewer metrics are kept and whether the reader is told to install the font. So a
        // family whose bold face cannot be read must leave nothing of itself in the deck —
        // otherwise the reader is told to install a font whose regular face the deck is
        // already carrying, and the family loses its viewer metrics while a face of it
        // travels.
        String present = "fonts/google/lato/Lato-Regular.ttf";
        FontFamilyDefinition half = FontFamilyDefinition.classpath("Half", present)
                .boldResource(ABSENT)
                .wordFamily("Half")
                .build();

        // The premise, asserted rather than assumed: the regular face reads and the bold one
        // does not, so the failure arrives with a program already in hand. Were the builder
        // to fill every slot from one source, this test would quietly become a second copy of
        // the first and stop guarding the order at all.
        assertThat(FontFamilyDefinition.class.getResource("/" + present)).isNotNull();
        assertThat(half.fontSourceSet().orElseThrow())
                .satisfies(sources -> {
                    assertThat(sources.regular().description()).isEqualTo(present);
                    assertThat(sources.bold().description()).isEqualTo(ABSENT);
                });

        byte[] deck = PptxDeck.registering(half,
                page -> page.addParagraph(paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(PptxDeck.fontParts(deck))
                .as("the readable face of a family that could not be carried whole")
                .isEmpty();
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("and the presentation declares no embedded font it has not got")
                .doesNotContain("embeddedFontLst")
                .doesNotContain("Half");

        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the face it could not read is named")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains(ABSENT);
        assertThat(log.messages("render.pptx.font.substitution"))
                .filteredOn(warning -> warning.contains("Half"))
                .singleElement(InstanceOfAssertFactories.STRING)
                .as("and the reader is told the deck does not carry the family whole, and "
                        + "what the text drawn in the face that stayed behind needs")
                .contains("\"Half\" is not carried whole")
                .contains("text drawn in a face that did not travel is referenced by name, "
                        + "and needs that font installed where the deck is opened for "
                        + "identical glyphs");
    }

    @Test
    void aFamilyOnlyTheEmbeddersReaderAcceptsIsNotCarriedAndTheDeckIsWritten() throws Exception {
        Path truncated = truncatedByOneByte();

        // The premise, measured rather than assumed: this font divides the two readers.
        // Without it the case would be a second copy of the first test — a font neither
        // reader takes — and the order the fix is about would go unguarded.
        assertThat(PptxEmbeddedFont.wrap(Files.newInputStream(truncated)))
                .as("the premise: the embedder's reader takes this program")
                .isNotEmpty();
        assertThatThrownBy(() -> java.awt.Font.createFont(
                java.awt.Font.TRUETYPE_FONT, truncated.toFile()))
                .as("the premise: the reader the viewer metrics come from refuses it")
                .isInstanceOf(java.awt.FontFormatException.class);

        FontFamilyDefinition family = FontFamilyDefinition
                .files("Truncated", truncated).wordFamily("Truncated").build();
        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(PptxDeck.fontParts(deck))
                .as("the program the metrics reader refused, not carried")
                .isEmpty();
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("and nothing of the family was handed over before the refusal: the "
                        + "metrics are read while the presentation is still untouched")
                .doesNotContain("embeddedFontLst")
                .doesNotContain("Truncated");

        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the face it could not carry is named, under the same key as a program "
                        + "the embedder's own reader turns down, with the reader's reason")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Truncated")
                .contains(truncated.toString())
                .contains("FontFormatException");
        assertThat(log.messages("render.pptx.font.substitution"))
                .filteredOn(warning -> warning.contains("Truncated"))
                .singleElement(InstanceOfAssertFactories.STRING)
                .as("and the reader is told the family is not carried, as for any other "
                        + "face that could not be read")
                .contains("\"Truncated\" is not carried whole")
                .contains("text drawn in a face that did not travel is referenced by name, "
                        + "and needs that font installed where the deck is opened for "
                        + "identical glyphs");
    }

    @Test
    void aFamilyDrawnInWhoseMetricsCannotBeReadKeepsTheTextAndLosesOnlyTheFont() throws Exception {
        // The other half of the same font: the page can measure it, because measurement
        // reads it through the parser that accepts it. So the run lays out, the slide keeps
        // its text, and the only thing lost is the program — where this used to cost the
        // whole deck after the layout had already succeeded.
        FontFamilyDefinition family = FontFamilyDefinition
                .files("Truncated", truncatedByOneByte()).wordFamily("Truncated").build();

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph
                        .text("Set in the truncated face")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.of("Truncated")).size(10).build())));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(slideText(deck))
                .as("the page laid out in that very family, and the slide carries its text")
                .contains("Set in the truncated face");
        assertThat(PptxDeck.part(deck, "ppt/slides/slide1.xml"))
                .as("the slide names the family it drew in")
                .contains("Truncated");
        assertThat(PptxDeck.fontParts(deck)).as("and carries no program for it").isEmpty();
    }

    @Test
    void aFamilyBothReadersAcceptIsCarriedWithItsProgram() throws Exception {
        // The control for the gate above: a whole font of the same face still embeds, so
        // reading the metrics before the hand-over cannot be refusing fonts that work.
        Path whole = Files.write(directory.resolve("Whole-Regular.ttf"), wholeFace());
        FontFamilyDefinition family = FontFamilyDefinition
                .files("Whole", whole).wordFamily("Whole").build();

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph
                        .text("Set in a face that travels")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.of("Whole")).size(10).build())));

        assertThat(PptxDeck.fontParts(deck)).as("the program travels").isNotEmpty();
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("and the presentation declares it embedded")
                .contains("embeddedFontLst")
                .contains("Whole");
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("nothing was skipped")
                .isEmpty();
        assertThat(log.messages("render.pptx.font.substitution"))
                .as("and the reader is not told to install a font the deck carries")
                .filteredOn(warning -> warning.contains("Whole"))
                .isEmpty();
    }

    /**
     * A real face with its last byte missing, written where a family can point at it.
     *
     * <p>One byte, because that is all it takes: the file's last table is
     * {@code GSUB}, whose directory entry then runs one byte past the end. fontbox's
     * header parse skips such a table and reads on — {@code GSUB} is not among the ones
     * it requires — while {@code Font.createFont} validates every entry against the file
     * size and rejects the font. Truncation rather than a hand-built font because it is
     * the damage a partial download or copy leaves, and because it keeps every table the
     * embedder's reader does require intact.</p>
     */
    private Path truncatedByOneByte() throws Exception {
        byte[] whole = wholeFace();
        return Files.write(directory.resolve("Truncated-Regular.ttf"),
                Arrays.copyOf(whole, whole.length - 1));
    }

    private static byte[] wholeFace() throws Exception {
        try (InputStream stream = FontFamilyDefinition.class.getResourceAsStream("/" + WHOLE)) {
            assertThat(stream).as("the premise: the control face is on the classpath").isNotNull();
            return stream.readAllBytes();
        }
    }

    /** Renders a one-page deck that registers the absent family, drawn with or not. */
    private static byte[] render(Consumer<PageFlowBuilder> content) {
        return PptxDeck.registering(
                FontFamilyDefinition.classpath("Absent", ABSENT).build(), content);
    }

    /** Every run of text on the deck's slides. */
    private static List<String> slideText(byte[] pptx) throws Exception {
        List<String> text = new ArrayList<>();
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
            show.getSlides().forEach(slide -> slide.getShapes().forEach(shape -> {
                if (shape instanceof XSLFTextShape textShape) {
                    text.add(textShape.getText());
                }
            }));
        }
        return text;
    }
}
