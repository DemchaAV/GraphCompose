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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A font that cannot travel into the deck identically must say so: standard-14
 * families warn about their metric-compatible replacement, unregistered names
 * warn that the deck carries a name-only reference, and each <em>name</em> warns
 * once per render pass — a standard-14 alias and its base name share a line,
 * while two spellings of an unregistered name are counted apart. An embedded
 * family stays silent.
 *
 * <p>What the warning may claim is bounded by when it runs. The two lines for a
 * registered family are written while the environment is built, before a single
 * run is drawn, so neither can know whether the deck will use the family — and a
 * family whose program the deck cannot carry, that nothing draws in, is named
 * nowhere in the package. Two tests hold both sides of that:
 * {@code aRegisteredFamilyNothingDrawsWithIsNotReportedAsOneTheDeckReferences} — no
 * reference when nothing draws with it — and
 * {@code theSameFamilyIsReferencedByTheSlideOnceTextIsDrawnInIt} — a reference on the
 * slide once something does.</p>
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
    void substitutedFamiliesWarnOncePerRenderPassAndEmbeddedFamiliesStaySilent() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow();
             PdfMeasurementResources measurement = PdfMeasurementResources.open(List.of(EMBEDDED_FAMILY))) {
            PptxRenderSession session = new PptxRenderSession(show, 300, 200, 1);
            PptxRenderEnvironment environment = new PptxRenderEnvironment(
                    show, session, 0, 200, measurement.fontLibrary(), List.of(EMBEDDED_FAMILY));

            assertThat(environment.fontFamily(FontName.HELVETICA)).isEqualTo("Arial");
            assertThat(environment.fontFamily(FontName.HELVETICA)).isEqualTo("Arial");
            // A name no registration covers comes back as it was written. It used to come back
            // with a style suffix taken off, which grouped these two spellings into one warning —
            // a grouping no render could reach, since a name the font library cannot resolve fails
            // the layout before any of this runs, and nothing bundled carries a suffix. Reached
            // here only because the method is called directly.
            assertThat(environment.fontFamily(FontName.of("Roboto-Regular")))
                    .isEqualTo("Roboto-Regular");
            assertThat(environment.fontFamily(FontName.of("Roboto-Bold")))
                    .as("counted apart from the spelling above, not as a facet of it")
                    .isEqualTo("Roboto-Bold");
            assertThat(environment.fontFamily(EMBEDDED)).isEqualTo("Lato");

            List<String> warnings = log.messages("render.pptx.font.substitution");
            assertThat(warnings)
                    .as("one warning per substituted name, none for the embedded family")
                    .hasSize(3);
            assertThat(warnings.get(0))
                    .contains("Helvetica")
                    .contains("Arial")
                    .contains("metric-compatible");
            assertThat(warnings.get(1))
                    .contains("Roboto-Regular")
                    .contains("by name");
            assertThat(warnings.get(2))
                    .contains("Roboto-Bold")
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
        assertThat(PptxDeck.fontParts(deck))
                .as("and no program travels under any name, which a search for the family "
                        + "name cannot see on its own — a program names itself in UTF-16")
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

    @Test
    void aFamilyRegisteredTwiceIsWarnedAboutOnce() throws Exception {
        // The registrations are a list, and the lines for a registered family used to be
        // written per definition, so a family registered twice was announced twice — the
        // README promises one line per family per render. Both of the lines a registered
        // family can get, each from a family registered twice and drawn in.
        byte[] lato = PptxDeck.face("fonts/google/lato/Lato-Regular.ttf");
        Path truncated = Files.write(directory.resolve("Truncated-Regular.ttf"),
                Arrays.copyOf(lato, lato.length - 1));
        FontFamilyDefinition unreadable =
                FontFamilyDefinition.files("Unreadable", truncated).build();

        PptxDeck.registering(List.of(NAMED_ONLY_FAMILY, NAMED_ONLY_FAMILY, unreadable, unreadable),
                page -> {
                    page.addParagraph(paragraph -> paragraph.text("Drawn in the named family")
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(NAMED_ONLY).size(10).build()));
                    page.addParagraph(paragraph -> paragraph.text("Drawn in the unreadable one")
                            .textStyle(DocumentTextStyle.builder()
                                    .fontName(unreadable.name()).size(10).build()));
                });

        List<String> warnings = log.messages("render.pptx.font.substitution");
        assertThat(warnings)
                .filteredOn(message -> message.contains("family=NamedOnly"))
                .as("one line for the family registered without sources")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("registered without binary sources");
        assertThat(warnings)
                .filteredOn(message -> message.contains("family=Unreadable"))
                .as("and one for the family that could not be embedded")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("could not be embedded");
        assertThat(warnings).as("and nothing else").hasSize(2);
    }

    @Test
    void aNameAnEarlierRegistrationCarriedIsNotToldNoFaceOfItTravels() throws Exception {
        // The last registration of a name decides its line, and here it is not carried — but
        // an earlier registration of the same name was, so a program of it is in the deck.
        // "No face of it travels" would be untrue of that deck; the line stays the one that
        // says the family is not carried whole.
        byte[] lato = PptxDeck.face("fonts/google/lato/Lato-Regular.ttf");
        Path whole = Files.write(directory.resolve("Whole-Regular.ttf"), lato);
        Path truncated = Files.write(directory.resolve("Truncated-Regular.ttf"),
                Arrays.copyOf(lato, lato.length - 1));

        byte[] deck = PptxDeck.registering(List.of(
                        FontFamilyDefinition.files("Brand", whole).build(),
                        FontFamilyDefinition.files("Brand", truncated).build()),
                page -> page.addParagraph(paragraph -> paragraph.text("Drawn in the default face")));

        assertThat(PptxDeck.fontParts(deck))
                .as("the premise: the earlier registration's program travelled")
                .hasSize(1);
        assertThat(log.messages("render.pptx.font.embed.skipped"))
                .as("the premise: the later one was given up")
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("family=Brand")
                .contains(truncated.toString())
                .contains("reason=viewer metrics");
        assertThat(log.messages("render.pptx.font.substitution"))
                .filteredOn(message -> message.contains("family=Brand"))
                .singleElement(InstanceOfAssertFactories.STRING)
                .contains("\"Brand\" is not carried whole")
                .doesNotContain("no face of it travels");
    }
}
