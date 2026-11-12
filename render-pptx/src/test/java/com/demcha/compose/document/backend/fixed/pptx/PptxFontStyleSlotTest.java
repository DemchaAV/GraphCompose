package com.demcha.compose.document.backend.fixed.pptx;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.DefaultFonts;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A presentation seats one face per style, and reads the style off the face.
 *
 * <p>A deck declares an embedded face under one of four elements — {@code regular},
 * {@code bold}, {@code italic}, {@code boldItalic} — and which one is not the slot the
 * family registered the face under: {@code XSLFFontInfo.addFacet} reads the weight and
 * the italic flag out of the container and picks the element from those. A face a caller
 * put in the bold slot whose own {@code usWeightClass} is 400 or less therefore reads
 * back as the regular one, and lands where the regular face already is.</p>
 *
 * <p>There are four elements and each holds one face. Seating a second face in a taken
 * slot does not add a part: it writes over the part the first face wrote and keeps the
 * declaration the first one made. Measured on the public path, both halves of it: the
 * deck came out with one program that was the <em>second</em> face's bytes, declared
 * {@code regular}, with nothing in the log — and PowerPoint 16.0 reported the family as
 * embedded and drew every run of it, the regular ones included, from that one program.
 * In an exported PDF the subset of such a deck had the {@code glyf}, {@code hmtx},
 * {@code cmap} and {@code loca} tables of the face from the bold slot, byte for byte,
 * where a correctly seated family exported two subsets an order of magnitude larger. So
 * what the deck lost was not a face but the right one: text the page measured in the
 * regular face was drawn in another.</p>
 *
 * <p>Refused rather than resolved, and refused for the family rather than the face. The
 * slot is the presentation's answer and not a choice the backend has — there is no fifth
 * element to move a face to, and two faces that collide agree on the only two fields the
 * element is read from — so a program cannot be made to travel under a declaration that
 * fits it. And keeping the first face instead would leave the family its viewer metrics
 * while a face of it did not travel, which is the arrangement
 * {@code PptxUnreadableFontSourceTest} holds the embedder away from for every other
 * refusal. So this ends as those do: nothing of the family carried, both faces named, the
 * deck written, and the text kept where the page drew it.</p>
 *
 * <p>The controls are the load-bearing half here, because a guard on the slot sits one
 * step behind a dedupe that every ordinary family relies on. A family given one file and
 * no others has that file in all four of its slots, so a check that ran before the
 * sources were deduped by description would refuse every single-face family there is;
 * {@code aFamilyWhoseOneFaceFillsEverySlotIsCarried} pins that order, and
 * {@code aFamilyThatNamesTwoOfItsFourSlotsIsCarriedUnderBoth} pins it at the fallback
 * positions a regular-and-bold family resolves to — the other two slots filled from those
 * two rather than from a face that could collide. That arrangement is the one most of the
 * per-slot registrations here use: the {@code OrangeOps} preset, and the bundled David
 * Libre and Gothic A1. A correctly seated
 * four-face family still carries four programs under four elements, which is at once the
 * control for the guard and the check that the reader this backend asks agrees with the
 * one that writes the declarations. And {@code noBundledFamilySeatsTwoFacesInOneStyle}
 * measures the whole bundled catalogue against the guard, so a font bump that put two
 * cuts of one family at the same weight would fail here rather than quietly stop a deck
 * carrying a face.</p>
 *
 * <p>What none of these covers: the slot's capacity belongs to a declared typeface, while
 * the guard answers for one definition's faces. Two definitions registered under the same
 * {@code wordFamily} share one font-list entry, so each is measured on its own slots and
 * the second still writes over the first's part — measured, with nothing logged. That is a
 * different question, which of two families a refusal should cost, and nothing here
 * decides it.</p>
 */
class PptxFontStyleSlotTest {

    private static final String REGULAR = "fonts/google/lato/Lato-Regular.ttf";
    private static final String BOLD = "fonts/google/lato/Lato-Bold.ttf";
    private static final String ITALIC = "fonts/google/lato/Lato-Italic.ttf";
    private static final String BOLD_ITALIC = "fonts/google/lato/Lato-BoldItalic.ttf";

    /** A second family's regular cut: a real face that collides with the one above. */
    private static final String OTHER_REGULAR = "fonts/google/poppins/Poppins-Regular.ttf";

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
    void aFamilyWhoseBoldFaceIsLighterThanRegularIsNotCarriedAndTheDeckIsWritten()
            throws Exception {
        // The measured case, and the one a caller reaches by accident: a family whose
        // "bold" is a lighter cut of itself. 300 rather than merely "not bold" because it
        // is the weight the case was measured at, and because it puts the face on the far
        // side of the only threshold the element is read from.
        byte[] regular = PptxDeck.face(REGULAR);
        byte[] light = FontWeightClass.with(regular, 300);
        Path regularFile = Files.write(directory.resolve("Airy-Regular.ttf"), regular);
        Path lightFile = Files.write(directory.resolve("Airy-Light.ttf"), light);

        // The premise, read through the backend's own reader rather than assumed: both
        // faces read back as the regular element, which is what makes this a collision and
        // not two faces of a family. Without it the test would pass just as well on a pair
        // that never shared a slot, and would be guarding nothing.
        assertThat(slotOf(regular)).isEqualTo("regular");
        assertThat(slotOf(light))
                .as("the face in the bold slot reads back as the regular one")
                .isEqualTo("regular");

        FontFamilyDefinition family = FontFamilyDefinition.files("Airy", regularFile)
                .boldPath(lightFile).wordFamily("Airy").build();
        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(slideText(deck))
                .as("the premise: the page laid out, and the slide carries its text")
                .contains("Drawn in the default face");
        assertThat(PptxDeck.fontParts(deck))
                .as("and carries neither face, where it used to carry the second one's "
                        + "bytes under the first one's declaration")
                .isEmpty();
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("nothing was handed over, so there is no declaration to take back")
                .doesNotContain("embeddedFontLst")
                .doesNotContain("Airy");

        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("both faces are named, each with its OWN weight — the two differ, and "
                        + "the weight is what a caller goes looking for in their font, so "
                        + "one weight shown for both would send them after the wrong face")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Airy")
                .contains("\"" + regularFile + "\" (weight 400)")
                .contains("\"" + lightFile + "\" (weight 300)")
                .contains("both read back as regular")
                .contains("italic false");
        assertThat(log.messages("render.pptx.font.substitution"))
                .as("and the reader is told the family is not carried, as for any other "
                        + "family a face of which did not travel")
                .filteredOn(warning -> warning.contains("Airy"))
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("\"Airy\" is not carried whole");
    }

    @Test
    void facesCollidingInTheItalicSlotAreRefusedAndNamedByThatSlot() throws Exception {
        // The other side of the element, and the only case that drives the guard off the
        // regular key: two upright-flag-set faces of different weights, both below the
        // threshold, land in italic together. Without it every refusal here would read
        // "both read back as regular", the three other keys would go unexercised, and the
        // message's rendering of a set italic flag would never be seen — nor would the
        // claim that two faces sharing a slot share that flag.
        byte[] italic = PptxDeck.face(ITALIC);
        byte[] lightItalic = FontWeightClass.with(italic, 300);
        Path italicFile = Files.write(directory.resolve("Slanted-Italic.ttf"), italic);
        Path lightFile = Files.write(directory.resolve("Slanted-LightItalic.ttf"), lightItalic);

        assertThat(slotOf(italic))
                .as("the premise: the face registered as the family's own reads back italic")
                .isEqualTo("italic");
        assertThat(slotOf(lightItalic))
                .as("and so does the lighter one put in the italic slot")
                .isEqualTo("italic");

        FontFamilyDefinition family = FontFamilyDefinition.files("Slanted", italicFile)
                .italicPath(lightFile).wordFamily("Slanted").build();
        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(PptxDeck.fontParts(deck)).as("and carries neither face").isEmpty();
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("named by the element they actually agreed on, with the flag that put "
                        + "them there rather than the one the regular cases show")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Slanted")
                .contains("\"" + italicFile + "\" (weight 400)")
                .contains("\"" + lightFile + "\" (weight 300)")
                .contains("both read back as italic")
                .contains("italic true");
    }

    @Test
    void theClasspathPathRefusesACollidingFamilyToo() throws Exception {
        // The other public path to the same guard. It is not a copy: the sources differ in
        // how they open and in what they call themselves, and the bundled families travel
        // this road, so a guard that only held for files on disk would leave it open.
        FontFamilyDefinition family = FontFamilyDefinition.classpath("Paired", REGULAR)
                .boldResource(OTHER_REGULAR).wordFamily("Paired").build();

        // The premise, and the reason this case needs no rewritten font: two real shipped
        // faces, each the regular cut of its own family, agree on the element.
        assertThat(slotOf(PptxDeck.face(REGULAR))).isEqualTo("regular");
        assertThat(slotOf(PptxDeck.face(OTHER_REGULAR))).isEqualTo("regular");

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(PptxDeck.fontParts(deck)).as("and carries neither face").isEmpty();
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("both resources are named, by the path a caller registered them under")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Paired")
                .contains(REGULAR)
                .contains(OTHER_REGULAR)
                .contains("both read back as regular");
    }

    @Test
    void aFamilyDrawnInWhoseFacesCollideKeepsTheTextAndLosesOnlyTheFont() throws Exception {
        // The user-visible half, and the same contract every other refusal has: the page
        // can measure both faces — measurement never reads the container — so the run lays
        // out, the slide keeps its text and names the family, and the only thing lost is
        // the program. This is the side that says the guard did not turn silent data loss
        // into a lost deck.
        Path regularFile = Files.write(directory.resolve("Drawn-Regular.ttf"),
                PptxDeck.face(REGULAR));
        Path lightFile = Files.write(directory.resolve("Drawn-Light.ttf"),
                FontWeightClass.with(PptxDeck.face(REGULAR), 300));
        FontFamilyDefinition family = FontFamilyDefinition.files("Drawn", regularFile)
                .boldPath(lightFile).wordFamily("Drawn").build();

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph
                        .text("Set in a face that stays home")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.of("Drawn")).size(10).build())));

        assertThat(deck).as("the deck is written").isNotEmpty();
        assertThat(slideText(deck))
                .as("the page laid out in that very family, and the slide carries its text")
                .contains("Set in a face that stays home");
        assertThat(PptxDeck.part(deck, "ppt/slides/slide1.xml"))
                .as("the slide names the family it drew in")
                .contains("Drawn");
        assertThat(PptxDeck.fontParts(deck)).as("and carries no program for it").isEmpty();
        // Why it carries none, not only that it does. Both faces here read and license
        // cleanly, so without this the case would stay green if the family were refused for
        // an unreadable face, a licence bit or the metrics read — and it would then be the
        // first test over again with a font name on the run.
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("refused over the slot, which is the path this case is here for")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("both read back as regular");
    }

    @Test
    void aCorrectlySeatedFamilyCarriesEveryFaceUnderItsOwnStyle() throws Exception {
        // The control for the guard, and the check that the reader the guard asks agrees
        // with the one that writes the declarations: these four elements are POI's answer,
        // not this test's, so a slot read here that drifted from addFacet's would show up
        // as a family refused or an element missing.
        FontFamilyDefinition family = FontFamilyDefinition.classpath("Seated", REGULAR)
                .boldResource(BOLD)
                .italicResource(ITALIC)
                .boldItalicResource(BOLD_ITALIC)
                .wordFamily("Seated")
                .build();

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph
                        .text("Set in a family that travels whole")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.of("Seated")).size(10).build())));

        assertThat(PptxDeck.fontParts(deck))
                .as("one part per face, so no face was written over another")
                .hasSize(4);
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("declared under all four elements")
                .contains("<p:regular")
                .contains("<p:bold")
                .contains("<p:italic")
                .contains("<p:boldItalic");
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("nothing was skipped: a family seated as its faces read back is not "
                        + "what the guard is for")
                .isEmpty();
        try (XMLSlideShow written = new XMLSlideShow(new ByteArrayInputStream(deck))) {
            assertThat(XSLFFontInfo.getFonts(written))
                    .singleElement()
                    .satisfies(font -> assertThat(font.getFacets())
                            .as("and the deck reads back as four faces, one per style")
                            .hasSize(4)
                            .extracting(facet -> facet.getWeight() + "/" + facet.isItalic())
                            .containsExactlyInAnyOrder(
                                    "400/false", "700/false", "400/true", "700/true"));
        }
    }

    @Test
    void aFamilyWhoseOneFaceFillsEverySlotIsCarried() throws Exception {
        // The order the guard depends on, and the case that would break first if it moved.
        // A family given one file has that file in all four of its slots, and the sources
        // are deduped by description before any container is built — so a slot check ahead
        // of that dedupe would refuse every single-face family, which is most of what
        // callers register.
        FontFamilyDefinition family =
                FontFamilyDefinition.classpath("Alone", REGULAR).wordFamily("Alone").build();

        // The premise, asserted rather than assumed: this family really does name the same
        // source in every slot. Were the builder to leave the other three empty, the test
        // would stop guarding the dedupe and nothing would say so.
        FontFamilyDefinition.FontSourceSet sources = family.fontSourceSet().orElseThrow();
        assertThat(List.of(sources.regular().description(), sources.bold().description(),
                        sources.italic().description(), sources.boldItalic().description()))
                .as("one source, four slots")
                .containsOnly(REGULAR);

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph
                        .text("Set in a one-face family")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.of("Alone")).size(10).build())));

        assertThat(PptxDeck.fontParts(deck))
                .as("carried once, not refused for colliding with itself")
                .hasSize(1);
        assertThat(log.messages("render.pptx.font.embed.skipped")).isEmpty();
    }

    @Test
    void aFamilyThatNamesTwoOfItsFourSlotsIsCarriedUnderBoth() throws Exception {
        // A regular and a heavier cut named as bold, the other two left to the builder,
        // which fills them from the sources already given — the arrangement most of the
        // per-slot registrations in this repository use (the OrangeOps preset, and the
        // bundled David Libre and Gothic A1). So every face after the first two repeats a
        // description and never reaches the slot comparison, which is why the fallback
        // cannot manufacture a collision. The one-file family above pins the dedupe where
        // all four descriptions are equal; this pins it where two are, and at the fallback
        // positions that arrangement actually reaches (italic from the regular, bold-italic
        // from the bold), which are not the ones a regular-plus-italic family would use.
        FontFamilyDefinition family = FontFamilyDefinition.classpath("Partial", REGULAR)
                .boldResource(BOLD)
                .wordFamily("Partial")
                .build();

        // The premise: the two slots nobody named resolve onto the two sources that were,
        // so the family has two distinct faces and not four.
        FontFamilyDefinition.FontSourceSet sources = family.fontSourceSet().orElseThrow();
        assertThat(List.of(sources.regular().description(), sources.bold().description(),
                        sources.italic().description(), sources.boldItalic().description()))
                .as("two sources across four slots, as the preset's own set resolves")
                .containsExactly(REGULAR, BOLD, REGULAR, BOLD);

        byte[] deck = PptxDeck.registering(family, page -> page.addParagraph(
                paragraph -> paragraph
                        .text("Set in a family that names two of its slots")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.of("Partial")).size(10).build())));

        assertThat(PptxDeck.fontParts(deck))
                .as("both faces carried, one part each")
                .hasSize(2);
        assertThat(PptxDeck.part(deck, "ppt/presentation.xml"))
                .as("declared under the two elements their own containers name")
                .contains("<p:regular")
                .contains("<p:bold");
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("and nothing was skipped: the builder's fallback repeats a source "
                        + "rather than adding a face that could collide")
                .isEmpty();
    }

    @Test
    void noBundledFamilySeatsTwoFacesInOneStyle() throws Exception {
        // The catalogue against the guard. The bundled families take the same road as a
        // registered one, so a font bump that brought in two cuts at one weight would stop
        // a deck carrying a face — and, before the guard, would have shipped the wrong
        // bytes. Measured through the backend's own reader on every face the embedder can
        // take, deduped the way the embedder dedupes.
        Map<String, List<String>> colliding = new LinkedHashMap<>();
        int faces = 0;
        for (FontFamilyDefinition family : DefaultFonts.bundledFamilies()) {
            if (family.fontSourceSet().isEmpty()) {
                continue;
            }
            FontFamilyDefinition.FontSourceSet sources = family.fontSourceSet().get();
            Set<String> read = new LinkedHashSet<>();
            Map<String, String> bySlot = new LinkedHashMap<>();
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
                String taken = bySlot.putIfAbsent(slotOf(face), source.description());
                if (taken != null) {
                    colliding.computeIfAbsent(family.name().name(), key -> new ArrayList<>())
                            .add(taken + " and " + source.description());
                }
            }
        }

        assertThat(faces)
                .as("the premise: this walked the catalogue rather than an empty list")
                .isGreaterThan(100);
        assertThat(colliding).as("no bundled family would be refused").isEmpty();
    }

    /** The element a deck would declare this face under, read as the backend reads it. */
    private static String slotOf(byte[] face) throws Exception {
        return PptxEmbeddedFont.styleOf(
                PptxEmbeddedFont.wrap(new ByteArrayInputStream(face))).slot();
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
