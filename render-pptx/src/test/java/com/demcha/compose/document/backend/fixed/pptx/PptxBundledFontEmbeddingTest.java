package com.demcha.compose.document.backend.fixed.pptx;

import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.backend.fixed.FixedLayoutRenderContext;
import com.demcha.compose.document.backend.fixed.SectionUnit;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Holds a deck to carrying the bundled fonts it drew with.
 *
 * <p>A family a caller registers is embedded, and warned about when it cannot be. A family
 * this library <em>ships</em> was neither: the deck named it and embedded nothing, so a
 * viewer without it installed substituted — and for Georgian, Armenian or Hangul, where a
 * substitute rarely covers the script, the slide showed boxes. The asymmetry was the sharp
 * edge, because the shipped families are exactly what a deck reaches for when the reader is
 * least likely to have the font.</p>
 *
 * <p>Only what was drawn is embedded, down to the face. The bundled set is dozens of
 * families and embedding here is whole-font, so a deck that carried all of them would be
 * tens of megabytes — which is what {@code aDeckCarriesOnlyTheFamiliesItDrewWith} and the
 * two face cases are about.</p>
 */
class PptxBundledFontEmbeddingTest {

    @Test
    void aDeckDrawnWithABundledFamilyCarriesIt() throws Exception {
        List<String> parts = PptxDeck.fontParts(PptxDeck.of(page -> page.addParagraph(p -> p
                .text("გამარჯობა")
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.NOTO_SANS_GEORGIAN).size(18).build()))));

        assertThat(parts)
                .describedAs("without this the deck names Noto Sans Georgian and ships "
                        + "nothing, so a viewer that lacks it draws boxes")
                .isNotEmpty();
    }

    @Test
    void aRightToLeftDeckCarriesTheFamilyThatMakesItReadable() throws Exception {
        assertThat(PptxDeck.fontParts(PptxDeck.of(page -> page.addParagraph(p -> p
                .text("مرحبا بالعالم")
                .direction(TextDirection.RTL)
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.AMIRI).size(18).build())))))
                .isNotEmpty();
    }

    @Test
    void aDeckCarriesOnlyTheFamiliesItDrewWith() throws Exception {
        // The bundled set runs to dozens of families and embedding is whole-font, so a deck
        // that offered all of them rather than the ones it used would be tens of megabytes.
        byte[] one = PptxDeck.of(page -> page.addParagraph(p -> p
                .text("გამარჯობა")
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.NOTO_SANS_GEORGIAN).size(18).build())));
        byte[] two = PptxDeck.of(page -> page
                .addParagraph(p -> p.text("გამარჯობა")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.NOTO_SANS_GEORGIAN).size(18).build()))
                .addParagraph(p -> p.text("Բարև աշխարհ")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.NOTO_SANS_ARMENIAN).size(18).build())));

        assertThat(PptxDeck.fontParts(two))
                .describedAs("the second family is carried because it was drawn with")
                .hasSizeGreaterThan(PptxDeck.fontParts(one).size());
    }

    @Test
    void aStandardFamilyIsNamedRatherThanCarried() throws Exception {
        // Helvetica and its siblings are the viewer's by definition; carrying one would be
        // weight for nothing, and there is no binary to carry in the first place.
        assertThat(PptxDeck.fontParts(PptxDeck.of(page -> page.addParagraph(p -> p
                .text("Plain Latin text")
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.HELVETICA).size(18).build())))))
                .describedAs("a deck of standard-14 text embeds nothing")
                .isEmpty();
    }

    @Test
    void aDeckCanBeAskedToCarryNothing() throws Exception {
        // The escape hatch, and the reason it exists: embedding is whole-font, so the
        // shipped five-script catalogue goes from 27 KB to 1.4 MB. A deck whose readers
        // are known to have the fonts can decline that.
        assertThat(PptxDeck.fontParts(renderWith(PptxFixedLayoutBackend.builder()
                .embedBundledFonts(false)
                .build())))
                .describedAs("declined, so the deck names the family and carries none")
                .isEmpty();
    }

    @Test
    void aFamilyTheDeckCarriesIsNotReportedAsOneTheReaderMustInstall() throws Exception {
        // The warning fires while a run is drawn; the embedding happens after the last
        // one. Left alone, a deck that carries Amiri told its author "not registered with
        // binary sources ... register the family to embed it" — the opposite of what the
        // file ends up containing. PptxFontSubstitutionWarningTest already reads this log,
        // but only for Helvetica, a facet-suffixed name and a registered family, so no
        // case ever drew with a bundled one and the contradiction passed CI.
        List<String> warnings = substitutionWarningsFromACarryingRender();

        assertThat(warnings)
                .describedAs("a carried family is not a substituted one")
                .noneMatch(message -> message.contains("Amiri"));
    }

    @Test
    void decliningToCarryThemReportsANameOnlyFamilyAndPointsAtRegistering() throws Exception {
        // The other half: a render told not to carry the fonts references them by name, so
        // the warning says the family is name-only and tells the author to register it.
        // The name-only half is also read from the deck, not only taken from the log: no font
        // part, and the slide still names the family.
        byte[] deck;
        List<String> warnings;
        try (RenderLogCapture log = RenderLogCapture.attach()) {
            deck = renderWith(PptxFixedLayoutBackend.builder().embedBundledFonts(false).build(),
                    FontName.AMIRI, "مرحبا");
            warnings = log.messages("render.pptx.font.substitution");
        }

        assertThat(warnings)
                .describedAs("declined, so the family is reported as name-only, with registering "
                        + "as the way to carry it")
                .anyMatch(message -> message.contains("Amiri")
                        && message.contains("by name only")
                        && message.contains("registerFontFamily"));
        assertThat(PptxDeck.fontParts(deck))
                .describedAs("name-only: the deck carries no program")
                .isEmpty();
        assertThat(PptxDeck.partsNaming(deck, "Amiri"))
                .describedAs("name-only: the slide that drew the text still names the family")
                .anyMatch(part -> part.startsWith("ppt/slides/"));
    }

    @Test
    void registeringTheFamilyCarriesItWhereBundledFontsAreDeclined() throws Exception {
        // The advice the declined render gives, followed: a registered family travels whatever
        // embedBundledFonts says, so the same render that carried nothing now carries Amiri
        // and stops reporting it.
        FontFamilyDefinition amiri = DefaultFonts.bundledFamilies().stream()
                .filter(family -> family.name().equals(FontName.AMIRI))
                .findFirst()
                .orElseThrow();
        byte[] deck;
        List<String> warnings;
        try (RenderLogCapture log = RenderLogCapture.attach()) {
            deck = renderWith(PptxFixedLayoutBackend.builder().embedBundledFonts(false).build(),
                    FontName.AMIRI, "مرحبا", List.of(amiri));
            warnings = log.messages("render.pptx.font.substitution");
        }

        assertThat(PptxDeck.fontParts(deck))
                .describedAs("registered, so carried although bundled fonts are declined")
                .isNotEmpty();
        assertThat(PptxDeck.partsNaming(deck, "Amiri"))
                .describedAs("the face carried is Amiri's: the presentation declares it embedded")
                .contains("ppt/presentation.xml");
        assertThat(warnings)
                .describedAs("a carried family is not a substituted one")
                .noneMatch(message -> message.contains("Amiri"));
    }

    /** The substitution warnings a carrying render emits for a bundled Arabic family. */
    private static List<String> substitutionWarningsFromACarryingRender() throws Exception {
        try (RenderLogCapture log = RenderLogCapture.attach()) {
            renderWith(PptxFixedLayoutBackend.builder().embedBundledFonts(true).build(),
                    FontName.AMIRI, "مرحبا");
            return log.messages("render.pptx.font.substitution");
        }
    }

    /** Renders the Georgian deck through a configured backend. */
    private static byte[] renderWith(PptxFixedLayoutBackend backend) throws Exception {
        return renderWith(backend, FontName.NOTO_SANS_GEORGIAN, "გამარჯობა");
    }

    private static byte[] renderWith(PptxFixedLayoutBackend backend, FontName font, String text)
            throws Exception {
        return renderWith(backend, font, text, List.of());
    }

    private static byte[] renderWith(PptxFixedLayoutBackend backend, FontName font, String text,
                                     List<FontFamilyDefinition> registered) throws Exception {
        GraphCompose.DocumentBuilder builder = GraphCompose.document()
                .pageSize(400, 160)
                .margin(DocumentInsets.of(20));
        registered.forEach(builder::registerFontFamily);
        try (DocumentSession document = builder.create()) {

            document.pageFlow(page -> page.addParagraph(p -> p
                    .text(text)
                    .textStyle(DocumentTextStyle.builder().fontName(font).size(18).build())));

            LayoutGraph graph = document.render(new GraphCapturingBackend());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            // The backend is handed the families directly: render(GraphCapturingBackend) keeps
            // only the graph, so the registration has to reach the PPTX write here too.
            backend.write(graph, new FixedLayoutRenderContext(graph.canvas(), registered, null, output));
            return output.toByteArray();
        }
    }

    @Test
    void aSectionedDeckCarriesThemToo() throws Exception {
        // renderSections is its own assembly with its own environment and its own write,
        // and it had no embed call at all — a sectioned deck showed the reported boxes.
        // Driven through the @Beta backend surface directly, because the convenience
        // MultiSectionDocument path hands every section a PDF chrome and a PPTX render
        // rejects that before it gets here.
        PptxFixedLayoutBackend backend = PptxFixedLayoutBackend.builder().build();
        try (DocumentSession document = GraphCompose.document()
                .pageSize(400, 160).margin(DocumentInsets.of(20)).create()) {

            document.pageFlow(page -> page.addParagraph(p -> p
                    .text("გამარჯობა")
                    .textStyle(DocumentTextStyle.builder()
                            .fontName(FontName.NOTO_SANS_GEORGIAN).size(18).build())));

            LayoutGraph graph = document.render(new GraphCapturingBackend());
            byte[] combined = backend.renderSections(List.of(
                    new SectionUnit(graph, graph.canvas(), List.of(), backend)));

            assertThat(PptxDeck.fontParts(combined))
                    .describedAs("a sectioned deck carries what a single-document one carries")
                    .isNotEmpty();
        }
    }

    @Test
    void onlyTheFacesTheDeckDrewWithAreCarried() throws Exception {
        // Embedding carries a font program whole, so a face nobody drew is pure weight —
        // measured, the five-script catalogue was shipping Gothic A1 Bold at 2.2 MB for
        // glyphs no slide contains. David Libre is regular-and-bold, so a deck that only
        // draws regular must carry exactly one part.
        byte[] regularOnly = PptxDeck.of(page -> page.addParagraph(p -> p
                .text("שלום עולם")
                .textStyle(DocumentTextStyle.builder()
                        .fontName(FontName.DAVID_LIBRE).size(18).build())));

        assertThat(PptxDeck.fontParts(regularOnly))
                .describedAs("one face drawn, one face carried")
                .hasSize(1);
    }

    @Test
    void aSecondFaceIsCarriedOnceItIsDrawn() throws Exception {
        // The other side of it: asking for bold must bring the bold program, or the
        // reader sees PowerPoint's synthetic emboldening of the regular one.
        byte[] both = PptxDeck.of(page -> page
                .addParagraph(p -> p.text("שלום עולם")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.DAVID_LIBRE).size(18).build()))
                .addParagraph(p -> p.text("שלום עולם")
                        .textStyle(DocumentTextStyle.builder()
                                .fontName(FontName.DAVID_LIBRE).size(18)
                                .decoration(DocumentTextDecoration.BOLD).build())));

        assertThat(PptxDeck.fontParts(both))
                .describedAs("regular and bold both drawn, both carried")
                .hasSize(2);
    }
}
