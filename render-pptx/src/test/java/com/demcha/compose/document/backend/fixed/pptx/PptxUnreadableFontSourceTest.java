package com.demcha.compose.document.backend.fixed.pptx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFFontInfo;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
 * bold face cannot be read to leaving nothing of itself in the deck, since one answer
 * covers the whole family.</p>
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
 *
 * <p>The last three go one reader further, to the presentation itself. It reads the
 * container's header through two thousand-byte windows — each name on its own when it
 * takes the face, and the whole header when it reads the written part back — and a header
 * past the first cost the render, <em>unchecked</em>, past the hand-over's catch, after
 * the page had laid out. The readers in front of the writer do not close it, because they
 * do not read the same names: {@code Font.createFont} rejects an over-long family and
 * never looks at the subfamily, while the container's full name is the two joined. A long
 * subfamily therefore passed every read — Lato-Regular rewritten with a 496-character one
 * makes a 1,002-byte full name — and that is the case a measurement varying only the
 * family name missed. The embedder now measures the header where it builds it, so such a
 * face ends as every other unreadable one does: nothing of the family carried, the face
 * named, the deck written, and the text kept where the page drew in it.</p>
 *
 * <p>The long-name case and its control each run at both name IDs, since one of the two is
 * the gap, and the control travels <em>and</em> reads back: a header the hand-over accepts
 * can still be one the presentation cannot read out of the part it wrote, and a control
 * that wrote such a deck would have called the guard sound while the deck was not. A
 * fourth case holds the band between the two readers, where the embedder takes a face the
 * metrics reader refuses for the same reason — a name's length — and the family degrades
 * just the same.</p>
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
        // Embedding answers for the family, not the face: one answer decides whether the
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
        try (InputStream program = Files.newInputStream(truncated)) {
            assertThat(PptxEmbeddedFont.wrap(program))
                    .as("the premise: the embedder's reader takes this program")
                    .isNotEmpty();
        }
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
                        + "the embedder's own reader turns down — and the reason says which "
                        + "of the two reads refused, which is what tells the reader the "
                        + "program itself was fine")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Truncated")
                .contains(truncated.toString())
                .contains("reason=viewer metrics:")
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

    @ParameterizedTest
    @CsvSource({"1, 2110", "2, 2098"})
    void aFaceWhoseNameThePresentationCannotReadBackCostsTheFontAndNotTheDeck(
            int nameId, int headerBytes) throws Exception {
        // Taking a face, the presentation reads each of the container's four names through
        // IOUtils.safelyAllocate(nameSize, 1000) and refuses a longer one unchecked — so
        // the refusal used to arrive past the hand-over's catch and cost the whole deck.
        // Both name IDs are here because the readers in front of the writer do not read the
        // same names: Font.createFont, which the viewer metrics come from, rejects an
        // over-long family (1) and never looks at the subfamily (2). A long subfamily
        // therefore passed every read, and what the writer choked on was the full name the
        // container joins from the two — "Lato" plus 496 characters is 1,002 bytes.
        //
        // 496 rather than something plainly over, because it is the measured case and the
        // one that pins the join: at 496 the record the font itself carries is 992 bytes,
        // inside the per-name limit at either ID, and only the joined name is past it. A
        // fixture at 520 would be caught by a check on the font's own names alone and
        // would leave the join unguarded.
        //
        // The number in the message is the whole header, which is what the embedder
        // measures, and it differs with which name is long — the other half of the join is
        // then "Regular" — so it is a parameter rather than a constant in the assertion.
        byte[] font = FontNameTable.with(wholeFace(), nameId, "N".repeat(496));
        Path file = Files.write(directory.resolve("Overlong-Regular.ttf"), font);

        // The premise, asserted against the reader whose limit the embedder now measures
        // against: this face is one the presentation could not read a name of. Without it
        // the case would be a copy of the truncated-font test.
        assertThatThrownBy(() -> {
            try (InputStream program = Files.newInputStream(file)) {
                PptxEmbeddedFont.wrap(program);
            }
        })
                .as("the embedder refuses the face, with the limit named")
                .isInstanceOf(IOException.class)
                .hasMessageContaining("1000");

        FontFamilyDefinition family = FontFamilyDefinition
                .files("Overlong", file).wordFamily("Overlong").build();
        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(PptxDeck.fontParts(deck))
                .as("the face whose name could not be read back, not carried")
                .isEmpty();
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("and the presentation was never handed the face, so it holds no "
                        + "declaration anything would have to take back")
                .doesNotContain("embeddedFontLst")
                .doesNotContain("Overlong");
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the face it could not carry is named, with the reason")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Overlong")
                .contains(file.toString())
                .contains(String.valueOf(headerBytes))
                .contains("past the 1000");
    }

    @ParameterizedTest
    @CsvSource({"1, 120", "2, 221"})
    void aFaceWithALongButReadableNameTravelsAndReadsBack(int nameId, int characters)
            throws Exception {
        // The control, and what keeps the test above honest at both name IDs: the same
        // rewritten table at a length every reader does take. Without it a refusal would
        // prove only that rewriting a name table breaks a font, and the limit — the thing
        // under test — would go unmeasured.
        //
        // The two lengths are not a tidiness: each name's binding limit belongs to a
        // different reader. A family name has to clear Font.createFont's 256-byte record
        // first, so it stops at 128 characters and this stays short of that cliff; a
        // subfamily that reader never reads, so what binds it is the header the
        // presentation reads back, and 221 characters is the longest subfamily that fits —
        // 902 bytes of names, 998 with the fixed fields, against a thousand.
        Path file = Files.write(directory.resolve("Long-Regular.ttf"),
                FontNameTable.with(wholeFace(), nameId, "N".repeat(characters)));
        FontFamilyDefinition family = FontFamilyDefinition
                .files("Long", file).wordFamily("Long").build();

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(PptxDeck.fontParts(deck)).as("the program travels").isNotEmpty();
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("and nothing was skipped: the rebuilt name table is sound")
                .isEmpty();
        // Travelling is not enough for a control here. A header the hand-over accepts can
        // still be one the presentation cannot read back out of the part it wrote, and a
        // control that wrote such a deck would have called the guard sound while the deck
        // it produced was not.
        try (XMLSlideShow written = new XMLSlideShow(new ByteArrayInputStream(deck))) {
            assertThat(XSLFFontInfo.getFonts(written))
                    .as("and the deck's own font declaration reads back")
                    .singleElement()
                    .satisfies(font -> assertThat(font.getFacets())
                            .singleElement()
                            .satisfies(facet ->
                                    assertThat(facet.getWeight()).isEqualTo(400)));
        }
    }

    @Test
    void aFamilyWhoseNameOnlyTheMetricsReaderRefusesIsNotCarriedEither() throws Exception {
        // The band between the two readers, and the other direction of their disagreement.
        // A 129-character family name is 258 bytes, past the 256 Font.createFont reads a
        // name record into, and makes a 642-byte header, well inside what the presentation
        // reads — so the embedder takes this face and the metrics reader turns it down.
        // The outcome is the same as every other refusal, which is the point; without this
        // the band would be reasoned about rather than measured, and the control at the
        // family stops at 120 characters precisely to stay clear of it.
        Path file = Files.write(directory.resolve("MetricsOnly-Regular.ttf"),
                FontNameTable.with(wholeFace(), 1, "N".repeat(129)));

        try (InputStream program = Files.newInputStream(file)) {
            assertThat(PptxEmbeddedFont.wrap(program))
                    .as("the premise: the embedder's own reader takes this face")
                    .isNotEmpty();
        }
        assertThatThrownBy(() -> java.awt.Font.createFont(
                java.awt.Font.TRUETYPE_FONT, file.toFile()))
                .as("the premise: the reader the viewer metrics come from does not")
                .isInstanceOf(java.awt.FontFormatException.class);

        FontFamilyDefinition family = FontFamilyDefinition
                .files("MetricsOnly", file).wordFamily("MetricsOnly").build();
        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(PptxDeck.fontParts(deck)).as("and carries no program").isEmpty();
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the face is named, and the reason says which read turned it down")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=MetricsOnly")
                .contains("reason=viewer metrics:");
    }

    @Test
    void aFamilyDrawnInWhoseNameIsTooLongKeepsTheTextAndLosesOnlyTheFont() throws Exception {
        // The user-visible half, and the same contract the metrics-refusal case has: the
        // page can measure this face — measurement reads it through fontbox, which has no
        // opinion about a name's length — so the run lays out, the slide keeps its text,
        // and the only thing lost is the program.
        Path file = Files.write(directory.resolve("Overlong-Drawn-Regular.ttf"),
                FontNameTable.with(wholeFace(), 2, "N".repeat(496)));
        FontFamilyDefinition family = FontFamilyDefinition
                .files("Overlong", file).wordFamily("Overlong").build();

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph
                        .text("Set in the overlong-named face")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.of("Overlong")).size(10).build())));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(slideText(deck))
                .as("the page laid out in that very family, and the slide carries its text")
                .contains("Set in the overlong-named face");
        assertThat(PptxDeck.part(deck, "ppt/slides/slide1.xml"))
                .as("the slide names the family it drew in")
                .contains("Overlong");
        assertThat(PptxDeck.fontParts(deck)).as("and carries no program for it").isEmpty();
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
        return PptxDeck.face(WHOLE);
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
