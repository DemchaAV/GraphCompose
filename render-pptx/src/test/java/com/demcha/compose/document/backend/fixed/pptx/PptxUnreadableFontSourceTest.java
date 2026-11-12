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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
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
 * <p>The seventh goes one reader further, to the writer itself. A face is handed to the
 * presentation after both reads pass, and from there a refusal is the presentation's,
 * which the hand-over does not undo. The one program this backend can produce that the
 * writer refuses is a face whose name needs more than POI's thousand bytes; the viewer
 * metrics' reader turns such a font down four times earlier, at 256 bytes, so the family
 * never reaches the writer at all. So the seventh test asserts the writer's refusal
 * against the writer, in isolation, and then holds the public path to what it does today:
 * the deck is written, the font is the only thing lost, and the presentation is left
 * holding nothing of the family. The eighth is its control — the same rewritten name
 * table at a length both readers take still travels — without which the seventh would
 * prove nothing about the length.</p>
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

    @Test
    void aFaceTheWriterWouldRefuseIsTurnedDownBeforeTheHandOver() throws Exception {
        // The one refusal the presentation makes over a program this backend hands it. POI
        // reads each of the EOT's names through IOUtils.safelyAllocate(nameSize, 1000), so a
        // family name of 520 characters — 1,040 bytes of UTF-16 — is past its limit.
        byte[] font = withFamilyName(wholeFace(), "N".repeat(520));
        Path file = Files.write(directory.resolve("Overlong-Regular.ttf"), font);

        // The premise, asserted against the writer this backend hands faces to rather than
        // reasoned about: it refuses this program, and it refuses it *unchecked*, so the
        // hand-over's catch would not cover it and the render would die on a font. That is
        // what the pre-flight below has to prevent, and it is the whole reason this test is
        // not a second copy of the truncated-font one.
        assertThatThrownBy(() -> {
            try (XMLSlideShow show = new XMLSlideShow();
                    InputStream program = Files.newInputStream(file)) {
                new XSLFFontInfo(show, "Overlong")
                        .addFacet(new ByteArrayInputStream(PptxEmbeddedFont.wrap(program)));
            }
        })
                .as("the premise: the presentation refuses the program the embedder wrapped")
                .isInstanceOf(org.apache.poi.util.RecordFormatException.class)
                .as("unchecked, which is what the hand-over does not catch")
                .isNotInstanceOf(java.io.IOException.class);

        FontFamilyDefinition family = FontFamilyDefinition
                .files("Overlong", file).wordFamily("Overlong").build();
        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(PptxDeck.fontParts(deck))
                .as("the program the readers turned down, not carried")
                .isEmpty();
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("and the presentation was never handed the face, so it holds no "
                        + "declaration the hand-over would have to take back")
                .doesNotContain("embeddedFontLst")
                .doesNotContain("Overlong");
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the face it could not carry is named")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Overlong")
                .contains(file.toString());
        // Which reader refuses is deliberately not asserted. Today it is the viewer metrics'
        // — java.awt.Font.createFont reads no name record over 256 bytes, so it turns the
        // font down at 129 characters, four times before POI's own limit — and the family
        // never reaches the presentation, which is why the assertions above are about the
        // deck and not about the writer. Were that door ever to open wider, the program
        // would reach the hand-over, which refuses it unchecked, and this test would fail on
        // the deck rather than on the reason, which is the thing worth holding.
    }

    @Test
    void aFaceWithALongButReadableNameStillTravels() throws Exception {
        // The control for the test above, and the one that keeps it honest: the same surgery
        // at a length both readers take. Without it a refusal would prove only that
        // rewriting a name table breaks a font, and the length — the thing under test —
        // would go unmeasured.
        Path file = Files.write(directory.resolve("Long-Regular.ttf"),
                withFamilyName(wholeFace(), "N".repeat(128)));
        FontFamilyDefinition family = FontFamilyDefinition
                .files("Long", file).wordFamily("Long").build();

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(PptxDeck.fontParts(deck)).as("the program travels").isNotEmpty();
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("and nothing was skipped: the rebuilt name table is sound")
                .isEmpty();
    }

    /**
     * The same face with a family name of the given length, and nothing else changed.
     *
     * <p>The name table is rebuilt record for record — the Mac platform entries, the full
     * name, the PostScript name all carried over — and only the string of each name ID 1
     * is replaced, so the single thing that differs from the shipped face is how long its
     * family name is. The rebuilt table is appended at the end of the file and the table
     * directory's entry for it re-pointed, which is how a name longer than the original
     * slot can be written without moving every other table.</p>
     */
    private static byte[] withFamilyName(byte[] font, String familyName) {
        ByteBuffer sfnt = ByteBuffer.wrap(font).order(ByteOrder.BIG_ENDIAN);
        int directoryEntry = -1;
        for (int index = 0, tables = Short.toUnsignedInt(sfnt.getShort(4));
                index < tables; index++) {
            if (sfnt.getInt(12 + index * 16) == 0x6e616d65) { // 'name'
                directoryEntry = 12 + index * 16;
                break;
            }
        }
        assertThat(directoryEntry).as("the premise: the face has a name table").isNotNegative();

        int table = sfnt.getInt(directoryEntry + 8);
        assertThat(Short.toUnsignedInt(sfnt.getShort(table)))
                .as("the premise: a format 0 name table, which is what this rebuilds — a "
                        + "format 1 source would lose its language-tag records here, and "
                        + "the face would then differ in more than the name's length")
                .isZero();
        int storage = table + Short.toUnsignedInt(sfnt.getShort(table + 4));
        List<int[]> records = new ArrayList<>();
        List<byte[]> strings = new ArrayList<>();
        for (int index = 0, count = Short.toUnsignedInt(sfnt.getShort(table + 2));
                index < count; index++) {
            int at = table + 6 + index * 12;
            int platform = Short.toUnsignedInt(sfnt.getShort(at));
            int nameId = Short.toUnsignedInt(sfnt.getShort(at + 6));
            int offset = storage + Short.toUnsignedInt(sfnt.getShort(at + 10));
            records.add(new int[] {platform, Short.toUnsignedInt(sfnt.getShort(at + 2)),
                    Short.toUnsignedInt(sfnt.getShort(at + 4)), nameId});
            strings.add(nameId == 1
                    // Platform 1 stores a name byte per character, platform 3 in UTF-16.
                    ? familyName.getBytes(platform == 1
                            ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_16BE)
                    : Arrays.copyOfRange(font, offset,
                            offset + Short.toUnsignedInt(sfnt.getShort(at + 8))));
        }

        int stringBytes = strings.stream().mapToInt(string -> string.length).sum();
        ByteBuffer rebuilt = ByteBuffer
                .allocate(6 + 12 * records.size() + stringBytes).order(ByteOrder.BIG_ENDIAN);
        rebuilt.putShort((short) 0); // format 0
        rebuilt.putShort((short) records.size());
        rebuilt.putShort((short) (6 + 12 * records.size()));
        for (int index = 0, offset = 0; index < records.size(); index++) {
            int[] record = records.get(index);
            rebuilt.putShort((short) record[0]);
            rebuilt.putShort((short) record[1]);
            rebuilt.putShort((short) record[2]);
            rebuilt.putShort((short) record[3]);
            rebuilt.putShort((short) strings.get(index).length);
            rebuilt.putShort((short) offset);
            offset += strings.get(index).length;
        }
        strings.forEach(rebuilt::put);

        int appendAt = (font.length + 3) & ~3; // tables start on a four-byte boundary
        byte[] patched = Arrays.copyOf(font, appendAt + rebuilt.capacity());
        System.arraycopy(rebuilt.array(), 0, patched, appendAt, rebuilt.capacity());
        ByteBuffer repointed = ByteBuffer.wrap(patched).order(ByteOrder.BIG_ENDIAN);
        repointed.putInt(directoryEntry + 8, appendAt);
        repointed.putInt(directoryEntry + 12, rebuilt.capacity());
        return patched;
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
