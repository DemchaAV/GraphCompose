package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A font that cannot travel into the deck identically must say so: standard-14
 * families warn about their metric-compatible replacement, unregistered names
 * warn that the deck carries a name-only reference, and each family warns once
 * per render pass. An embedded family stays silent.
 *
 * <p>What the warning may claim is bounded by when it runs. The two lines for a
 * registered family are written while the environment is built, before a single
 * run is drawn, so neither can know whether the deck will use the family — and a
 * family whose program the deck cannot carry, that nothing draws in, is named
 * nowhere in the package. The last two tests hold both sides of that: no reference
 * when nothing draws with it, a reference on the slide once something does.</p>
 */
class PptxFontSubstitutionWarningTest {

    private static final FontName EMBEDDED = FontName.of("WarnLato");
    private static final FontFamilyDefinition EMBEDDED_FAMILY = FontFamilyDefinition.classpath(
                    EMBEDDED, "fonts/google/lato/Lato-Regular.ttf")
            .wordFamily("Lato")
            .build();

    /** Registered with no binary sources at all, so nothing of it can be embedded. */
    private static final FontName NAMED_ONLY = FontName.of("NamedOnly");
    private static final FontFamilyDefinition NAMED_ONLY_FAMILY = FontFamilyDefinition.standard14(
            NAMED_ONLY, "HELVETICA", "HELVETICA_BOLD", "HELVETICA_OBLIQUE",
            "HELVETICA_BOLD_OBLIQUE");

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
    void substitutedFamiliesWarnOncePerRenderPassAndEmbeddedFamiliesStaySilent() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow();
             PdfMeasurementResources measurement = PdfMeasurementResources.open(List.of(EMBEDDED_FAMILY))) {
            PptxRenderSession session = new PptxRenderSession(show, 300, 200, 1);
            PptxRenderEnvironment environment = new PptxRenderEnvironment(
                    show, session, 0, 200, measurement.fontLibrary(), List.of(EMBEDDED_FAMILY));

            assertThat(environment.fontFamily(FontName.HELVETICA)).isEqualTo("Arial");
            assertThat(environment.fontFamily(FontName.HELVETICA)).isEqualTo("Arial");
            assertThat(environment.fontFamily(FontName.of("Roboto-Regular"))).isEqualTo("Roboto");
            assertThat(environment.fontFamily(FontName.of("Roboto-Bold")))
                    .as("facet of an already-warned family")
                    .isEqualTo("Roboto");
            assertThat(environment.fontFamily(EMBEDDED)).isEqualTo("Lato");

            List<String> warnings = log.messages("render.pptx.font.substitution");
            assertThat(warnings)
                    .as("one warning per substituted family, none for the embedded one")
                    .hasSize(2);
            assertThat(warnings.get(0))
                    .contains("Helvetica")
                    .contains("Arial")
                    .contains("metric-compatible");
            assertThat(warnings.get(1))
                    .contains("Roboto-Regular")
                    .contains("by name");
            assertThat(warnings).noneMatch(message -> message.contains("WarnLato"));
        }
    }

    @Test
    void aRegisteredFamilyNothingDrawsWithIsNotReportedAsOneTheDeckReferences() throws Exception {
        // The premise below is the measurement: every part of this package is free of the
        // family name, and the line used to read "the deck references \"NamedOnly\" by name
        // only". Both halves are asserted here, so neither can rot unnoticed.
        byte[] deck = PptxDeck.registering(NAMED_ONLY_FAMILY,
                page -> page.addParagraph(paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(PptxDeck.partsNaming(deck, "NamedOnly"))
                .as("the premise: no part of the deck names the family")
                .isEmpty();
        assertThat(log.messages("render.pptx.font.substitution"))
                .filteredOn(message -> message.contains("NamedOnly"))
                .singleElement(InstanceOfAssertFactories.STRING)
                .as("so the line may not claim a reference the deck does not make, nor "
                        + "predict a substitution for text that is not there")
                .doesNotContain("the deck references")
                .contains("no program for \"NamedOnly\" travels with the deck")
                .contains("any text drawn with it is referenced by name only and "
                        + "substituted by viewers");
    }

    @Test
    void theSameFamilyIsReferencedByTheSlideOnceTextIsDrawnInIt() throws Exception {
        // The other side of the conditional, so "any text drawn with it" is not idle
        // hedging: the same registration, now drawn in, does put the family on the slide.
        byte[] deck = PptxDeck.registering(NAMED_ONLY_FAMILY,
                page -> page.addParagraph(paragraph -> paragraph
                        .text("Drawn in the registered face")
                        .textStyle(DocumentTextStyle.builder().fontName(NAMED_ONLY).size(10).build())));

        assertThat(PptxDeck.partsNaming(deck, "NamedOnly"))
                .as("the slide that drew the text names the family")
                .containsExactly("ppt/slides/slide1.xml");
        assertThat(PptxDeck.fontParts(deck))
                .as("and the deck still carries no program for it")
                .isEmpty();
        assertThat(log.messages("render.pptx.font.substitution"))
                .filteredOn(message -> message.contains("NamedOnly"))
                .singleElement(InstanceOfAssertFactories.STRING)
                .as("the same line, which is why its claim is conditional")
                .contains("any text drawn with it is referenced by name only and "
                        + "substituted by viewers");
    }
}
