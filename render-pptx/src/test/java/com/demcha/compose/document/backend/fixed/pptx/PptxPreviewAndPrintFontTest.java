package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A face whose licence allows embedding for preview and print only, which a deck does not carry.
 *
 * <p>{@code OS/2 fsType} bit {@code 0x0004} without {@code 0x0008}: the face may travel in a file
 * that is read and printed, not in one that is edited. The DOCX backend has read the bits that way
 * from the start and holds such a face back. A deck is a file people edit, so it holds the face
 * back too, and only that face: the family's other faces still travel.</p>
 *
 * <p>The bundled {@code ZillaSlab-BoldItalic} is the one shipped face this reaches. Measured in
 * PowerPoint 16.0 (build 20625) on a deck drawing Zilla Slab in all four styles at 24pt, on a
 * machine with no Zilla Slab of its own: carrying the face, the bold-italic line was drawn from it
 * at 428.06pt against the page's 428.35pt; held back, PowerPoint drew the carried italic program,
 * emboldened, at 414.34pt, and the other three lines did not move.</p>
 */
class PptxPreviewAndPrintFontTest {

    private static final String ZILLA = "fonts/google/zillaslab/ZillaSlab-";
    private static final String LATO_REGULAR = "fonts/google/lato/Lato-Regular.ttf";
    private static final String LATO_BOLD = "fonts/google/lato/Lato-Bold.ttf";

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
    void aPreviewAndPrintFaceIsRefusedOnItsOwnTerms() throws Exception {
        byte[] previewOnly = FontEmbeddingType.with(PptxDeck.face(LATO_REGULAR), 0x0004);

        assertThatThrownBy(() -> PptxEmbeddedFont.wrap(previewOnly))
                .as("refused as the one refusal that costs a face and not its family")
                .isInstanceOf(PptxEmbeddedFont.PreviewAndPrintOnlyException.class)
                .hasMessageContaining("0x0004")
                .hasMessageContaining("preview and print only");
    }

    @ParameterizedTest
    @ValueSource(ints = {0x0000, 0x0008, 0x000C})
    void aFaceThatMayBeEditedIsWrapped(int fsType) throws Exception {
        // Installable, editable, and preview-and-print with the editable bit beside it, which
        // the DOCX backend reads as editable too: bit 0x0008 is what makes a face editable,
        // whatever else is set.
        byte[] face = FontEmbeddingType.with(PptxDeck.face(LATO_REGULAR), fsType);

        assertThat(PptxEmbeddedFont.wrap(face)).isNotEmpty();
    }

    @Test
    void aRestrictedFaceIsStillRefusedAsBefore() throws Exception {
        // Restricted wins over preview-and-print, as it does in the DOCX reading, and stays the
        // refusal that costs the family.
        byte[] restricted = FontEmbeddingType.with(PptxDeck.face(LATO_REGULAR), 0x0006);

        assertThatThrownBy(() -> PptxEmbeddedFont.wrap(restricted))
                .isInstanceOf(IOException.class)
                .isNotInstanceOf(PptxEmbeddedFont.PreviewAndPrintOnlyException.class)
                .hasMessageContaining("prohibit embedding");
    }

    @Test
    void zillaSlabBoldItalicIsTheOnlyBundledFaceHeldBack() throws Exception {
        // The count the changelog and the capability details quote, measured against the
        // catalogue rather than written down and believed: a font bump could change it with no
        // line of code changing. Deduped the way the embedder dedupes, within a family: no two
        // bundled families share a file, so that is also the count of distinct files.
        Set<String> heldBack = new LinkedHashSet<>();
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
                try {
                    PptxEmbeddedFont.wrap(face);
                } catch (PptxEmbeddedFont.PreviewAndPrintOnlyException previewOnly) {
                    heldBack.add(source.description());
                }
            }
        }

        assertThat(faces).as("the catalogue's distinct faces").isEqualTo(130);
        assertThat(heldBack).containsExactly(ZILLA + "BoldItalic.ttf");
        assertThat(FontEmbeddingType.of(PptxDeck.face(ZILLA + "BoldItalic.ttf"))).isEqualTo(0x0004);
    }

    @Test
    void aBundledFamilyTravelsWithoutItsPreviewAndPrintFace() throws Exception {
        byte[] deck = PptxDeck.of(page -> {
            for (DocumentTextDecoration decoration : List.of(DocumentTextDecoration.DEFAULT,
                    DocumentTextDecoration.BOLD, DocumentTextDecoration.ITALIC,
                    DocumentTextDecoration.BOLD_ITALIC)) {
                page.addParagraph(paragraph -> paragraph.text("Set in Zilla Slab")
                        .textStyle(zilla(decoration)));
            }
        });

        assertThat(PptxDeck.fontParts(deck))
                .as("regular, bold and italic travel; bold italic does not")
                .hasSize(3);
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .contains("<p:regular ")
                .contains("<p:bold ")
                .contains("<p:italic ")
                .doesNotContain("<p:boldItalic");
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the face is named once, with the reason")
                .singleElement()
                .asString()
                .contains("family=Zilla Slab")
                .contains("source=" + ZILLA + "BoldItalic.ttf")
                .contains("preview and print only");
        assertThat(log.messages("render.pptx.font.substitution"))
                .as("and the family is told it is not carried whole, not that none of it travels")
                .singleElement()
                .asString()
                .contains("Zilla Slab")
                .contains("not carried whole");
    }

    @Test
    void aBundledFamilyDrawnOnlyInThatFaceIsNotDeclaredAtAll() throws Exception {
        byte[] deck = PptxDeck.of(page -> page.addParagraph(paragraph -> paragraph
                .text("Set in Zilla Slab")
                .textStyle(zilla(DocumentTextDecoration.BOLD_ITALIC))));

        assertThat(PptxDeck.fontParts(deck)).as("nothing to carry").isEmpty();
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("and no declaration of a family the deck holds no face of")
                .doesNotContain("embeddedFont");
        assertThat(log.messages("render.pptx.font.substitution"))
                .singleElement()
                .asString()
                .contains("no face of it travels");
    }

    @Test
    void aFamilyThatDoesNotDrawTheFaceIsUnchanged() throws Exception {
        // The bundled path reads only the faces drawn, so a deck in Zilla Slab regular never
        // meets the bold italic face and carries what it always carried.
        byte[] deck = PptxDeck.of(page -> page.addParagraph(paragraph -> paragraph
                .text("Set in Zilla Slab")
                .textStyle(zilla(DocumentTextDecoration.DEFAULT))));

        assertThat(PptxDeck.fontParts(deck)).hasSize(1);
        assertThat(log.messages("render.pptx.font.embed.skipped")).isEmpty();
        assertThat(log.messages("render.pptx.font.substitution")).isEmpty();
    }

    @Test
    void aRegisteredFamilyKeepsItsOtherFacesAndItsMetrics() throws Exception {
        FontFamilyDefinition family = FontFamilyDefinition
                .files("Slab", copy(ZILLA + "Regular.ttf"))
                .boldPath(copy(ZILLA + "Bold.ttf"))
                .italicPath(copy(ZILLA + "Italic.ttf"))
                .boldItalicPath(copy(ZILLA + "BoldItalic.ttf"))
                .wordFamily("Slab")
                .build();

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(paragraph -> paragraph
                .text("Set in Slab")
                .textStyle(DocumentTextStyle.builder().fontName(FontName.of("Slab")).size(10)
                        .build())));

        assertThat(PptxDeck.fontParts(deck)).hasSize(3);
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml")).doesNotContain("<p:boldItalic");
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .singleElement()
                .asString()
                .contains("family=Slab")
                .contains("ZillaSlab-BoldItalic.ttf");
        assertThat(log.messages("render.pptx.font.substitution"))
                .singleElement()
                .asString()
                .contains("not carried whole");

        // The faces it carries are in the deck as handed over, so the ascents they are seated by
        // are kept; without them the family's text would be seated from the PDF descriptor.
        PptxDeckFonts fonts = new PptxDeckFonts(new XMLSlideShow(), List.of(family));
        assertThat(fonts.viewerMetrics(FontName.of("Slab"))).isNotNull();
    }

    @Test
    void aRegisteredFamilyOfOnlyThatFaceIsNotCarriedOrDeclared() throws Exception {
        FontFamilyDefinition family = FontFamilyDefinition
                .files("Slab", copy(ZILLA + "BoldItalic.ttf"))
                .wordFamily("Slab")
                .build();

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(paragraph -> paragraph
                .text("Set in Slab")
                .textStyle(DocumentTextStyle.builder().fontName(FontName.of("Slab")).size(10)
                        .build())));

        assertThat(PptxDeck.fontParts(deck)).isEmpty();
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml")).doesNotContain("embeddedFont");
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("one source behind all four slots, named once")
                .hasSize(1);
        assertThat(log.messages("render.pptx.font.substitution"))
                .singleElement()
                .asString()
                .contains("no face of it travels");
        PptxDeckFonts fonts = new PptxDeckFonts(new XMLSlideShow(), List.of(family));
        assertThat(fonts.viewerMetrics(FontName.of("Slab"))).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0x0002, 0x0200})
    void aRestrictedOrBitmapOnlyFaceStillCostsTheFamily(int fsType) throws Exception {
        // The refusals that were there before keep their reach: one such face and the family's
        // editable regular face do not travel either. Drawn in the default face, because the
        // page cannot measure a face whose bits forbid embedding and fails the render instead;
        // a registered family is offered to the deck whether or not it is drawn in.
        byte[] deck = PptxDeck.registering(twoFaces(0x0000, fsType), page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(PptxDeck.fontParts(deck)).isEmpty();
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml")).doesNotContain("embeddedFont");
        assertThat(log.messages("render.pptx.font.substitution"))
                .filteredOn(line -> line.contains("family=Sans"))
                .singleElement()
                .asString()
                .contains("no face of it travels");
    }

    @Test
    void aHeldBackFaceDoesNotHideALaterRefusalOfTheFamily() throws Exception {
        // The held-back face is read first, and the restricted one after it still refuses the
        // family: holding one face back does not stop the loop checking the rest. Drawn in the
        // default face for the reason the case above is.
        byte[] deck = PptxDeck.registering(twoFaces(0x0004, 0x0002), page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(PptxDeck.fontParts(deck)).isEmpty();
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml")).doesNotContain("embeddedFont");
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the held-back face, then the refusal")
                .hasSize(2)
                .satisfies(lines -> {
                    assertThat(lines.get(0)).contains("Sans-Regular.ttf").contains("preview and print only");
                    assertThat(lines.get(1)).contains("Sans-Bold.ttf").contains("prohibit embedding");
                });
        assertThat(log.messages("render.pptx.font.substitution"))
                .filteredOn(line -> line.contains("family=Sans"))
                .singleElement()
                .asString()
                .contains("no face of it travels");
    }

    @Test
    void anEditableFaceBesideItTravelsWhole() throws Exception {
        // The negative case on the registered path: the same family shape with every face
        // editable is carried whole and nothing is said.
        byte[] deck = PptxDeck.registering(twoFaces(0x0008, 0x0000), page -> page.addParagraph(
                paragraph -> paragraph.text("Set in Sans").textStyle(sans())));

        assertThat(PptxDeck.fontParts(deck)).hasSize(2);
        assertThat(log.messages("render.pptx.font.embed.skipped")).isEmpty();
        assertThat(log.messages("render.pptx.font.substitution")).isEmpty();
    }

    /** "Sans": Lato regular and Lato bold, each rewritten to the given licence bits. */
    private FontFamilyDefinition twoFaces(int regularFsType, int boldFsType) throws IOException {
        return FontFamilyDefinition
                .files("Sans", write("Sans-Regular.ttf",
                        FontEmbeddingType.with(PptxDeck.face(LATO_REGULAR), regularFsType)))
                .boldPath(write("Sans-Bold.ttf",
                        FontEmbeddingType.with(PptxDeck.face(LATO_BOLD), boldFsType)))
                .wordFamily("Sans")
                .build();
    }

    private static DocumentTextStyle sans() {
        return DocumentTextStyle.builder().fontName(FontName.of("Sans")).size(10).build();
    }

    private static DocumentTextStyle zilla(DocumentTextDecoration decoration) {
        return DocumentTextStyle.builder()
                .fontName(FontName.ZILLA_SLAB).size(12).decoration(decoration).build();
    }

    private Path copy(String resource) throws IOException {
        return write(resource.substring(resource.lastIndexOf('/') + 1), PptxDeck.face(resource));
    }

    private Path write(String name, byte[] bytes) throws IOException {
        return Files.write(directory.resolve(name), bytes);
    }
}
