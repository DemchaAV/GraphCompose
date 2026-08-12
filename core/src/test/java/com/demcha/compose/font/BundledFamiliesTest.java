package com.demcha.compose.font;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the catalog side of the bundled families: that they are listed, which faces each
 * one really has, and that asking for a recently-added one against an older font artifact
 * says so.
 *
 * <p>The font binaries ship in a separately-versioned artifact, so a consumer can hold a
 * current engine and a stale {@code graph-compose-fonts}. The resource then simply is not
 * on the classpath, and the failure surfaces lazily at first use — far from its cause. The
 * message has to carry the diagnosis, because the stack trace will not.</p>
 */
class BundledFamiliesTest {

    @Test
    void theScriptFamiliesAreInTheBundledCatalog() {
        assertThat(DefaultFonts.bundledFontNames())
                .contains(FontName.AMIRI, FontName.DAVID_LIBRE,
                        FontName.NOTO_SANS_GEORGIAN, FontName.NOTO_SANS_ARMENIAN,
                        FontName.GOTHIC_A1);
    }

    @Test
    void davidLibreDeclaresBoldButFallsBackForTheFacesUpstreamDoesNotShip() {
        FontFamilyDefinition.FontSourceSet sources = bundled(FontName.DAVID_LIBRE);

        assertThat(sources.regular().description()).endsWith("DavidLibre-Regular.ttf");
        assertThat(sources.bold().description()).endsWith("DavidLibre-Bold.ttf");
        assertThat(sources.italic().description())
                .describedAs("upstream ships no italic face, so the builder collapses it "
                        + "onto the regular one instead of leaving a null source")
                .endsWith("DavidLibre-Regular.ttf");
        assertThat(sources.boldItalic().description())
                .describedAs("bold-italic falls back to bold rather than to regular, so the "
                        + "weight survives even though the slant cannot")
                .endsWith("DavidLibre-Bold.ttf");
    }

    private static FontFamilyDefinition.FontSourceSet bundled(FontName name) {
        return DefaultFonts.bundledFamilies().stream()
                .filter(family -> family.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("not in the bundled catalog: " + name))
                .fontSourceSet()
                .orElseThrow();
    }

    @Test
    void aMissingResourceNamesTheVersionOfTheArtifactOnTheClasspath() {
        // The advice a consumer needs depends on which of two setups they are in, and the
        // artifact's own descriptor is what tells them apart — no list of which version
        // introduced which family, which was five entries and one more per font.
        for (FontName family : List.of(FontName.AMIRI, FontName.DAVID_LIBRE,
                FontName.NOTO_SANS_GEORGIAN, FontName.NOTO_SANS_ARMENIAN, FontName.GOTHIC_A1)) {
            String missingFace = folderOf(family) + "/NoSuchFace.ttf";
            FontFamilyDefinition definition = FontFamilyDefinition
                    .classpath(FontName.of(family.name() + " Probe"), missingFace)
                    .build();

            assertThatThrownBy(() -> definition.fontSourceSet().orElseThrow().regular().openStream())
                    .describedAs("%s resolves under %s", family, missingFace)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("graph-compose-fonts")
                    .hasMessageContaining(bundledFontsVersion());
        }
    }

    @Test
    void theVersionInTheMessageIsTheArtifactsOwnRatherThanAnythingHardcoded() {
        // Read from the descriptor the fonts module writes at build time, so this stays
        // true across a version bump without anyone editing a constant.
        assertThat(bundledFontsVersion())
                .describedAs("the fonts artifact on the test classpath states its version")
                .isNotBlank()
                .matches("[0-9]+[.][0-9]+[.][0-9]+(-.+)?");
    }

    @Test
    void anAbsentFontArtifactGetsTheMigrationPointerInstead() {
        // The branch that could never be exercised before: the artifact is on this test
        // classpath, so the only way to see the advice a consumer without it receives is
        // to ask for it directly. Without this the "add the dependency" wording could rot
        // untouched — it is the message for the setup nobody here is in.
        String message = FontFamilyDefinition.ClasspathFontSource.missingResourceMessage(
                "/fonts/google/amiri/Amiri-Regular.ttf", null, false);

        assertThat(message)
                .contains("graph-compose-fonts")
                .contains("v1.8.0")
                .contains("docs/migration/v1.8.0-fonts.md");
    }

    @Test
    void aStaleFontArtifactIsToldWhichVersionItIsRatherThanToAddADependencyItHas() {
        String message = FontFamilyDefinition.ClasspathFontSource.missingResourceMessage(
                "/fonts/google/gothica1/GothicA1-Regular.ttf", "1.2.0", true);

        assertThat(message)
                .describedAs("naming the version they have is what makes the problem "
                        + "recognisable; telling them to add a dependency they already "
                        + "have is the one answer that cannot help")
                .contains("1.2.0")
                .doesNotContain("v1.8.0");
    }

    @Test
    void anArtifactTooOldToNameItselfIsStillNotToldToAddADependencyItHas() {
        // The state every consumer of the published 1.0.0 is in: the fonts are on the
        // classpath and the descriptor that would name their version is not, because the
        // descriptor ships from 1.1.0. Reading only the descriptor makes that look
        // identical to having no artifact at all — and sends the one reader who can
        // actually hit this today to add a dependency they already depend on.
        String message = FontFamilyDefinition.ClasspathFontSource.missingResourceMessage(
                "/fonts/google/amiri/Amiri-Regular.ttf", null, true);

        assertThat(message)
                .describedAs("presence and version are separate questions; only the first "
                        + "can be asked of a release that shipped before either mechanism")
                .contains("predates 1.1.0")
                .doesNotContain("v1.8.0");
    }

    /** The version the fonts artifact on the classpath reports for itself. */
    private static String bundledFontsVersion() {
        java.util.Properties properties = new java.util.Properties();
        try (java.io.InputStream descriptor = BundledFamiliesTest.class
                .getResourceAsStream("/fonts/graph-compose-fonts.properties")) {
            assertThat(descriptor).describedAs("the fonts artifact ships its descriptor").isNotNull();
            properties.load(descriptor);
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        return properties.getProperty("version");
    }

    /** {@code fonts/google/amiri} — the folder the family's real faces resolve under. */
    private static String folderOf(FontName family) {
        String regular = bundled(family).regular().description();
        return regular.substring(0, regular.lastIndexOf('/'));
    }

    @Test
    void theBundledArabicFamilyKeepsFourDistinctFaces() {
        FontFamilyDefinition.FontSourceSet sources = bundled(FontName.AMIRI);

        assertThat(List.of(
                sources.regular().description(),
                sources.bold().description(),
                sources.italic().description(),
                sources.boldItalic().description()))
                .describedAs("Amiri ships all four faces upstream, so none of them may collapse "
                        + "onto another — a collapse would render bold or italic Arabic upright "
                        + "and identical to regular, with every coverage assertion still passing")
                .doesNotHaveDuplicates();
    }

    @Test
    void theSingleInstanceFamiliesCollapseEveryFaceOntoTheirRegularOne() {
        for (FontName family : List.of(FontName.NOTO_SANS_GEORGIAN, FontName.NOTO_SANS_ARMENIAN)) {
            FontFamilyDefinition.FontSourceSet sources = bundled(family);

            assertThat(List.of(
                    sources.bold().description(),
                    sources.italic().description(),
                    sources.boldItalic().description()))
                    .describedAs("%s is bundled as the single variable-font file upstream "
                            + "publishes, so every face must resolve to that one resource "
                            + "rather than to a path with nothing behind it", family)
                    .containsOnly(sources.regular().description());
        }
    }

    @Test
    void theKoreanFamilyKeepsItsDrawnBoldAndFallsBackOnlyForTheSlant() {
        FontFamilyDefinition.FontSourceSet sources = bundled(FontName.GOTHIC_A1);

        assertThat(sources.regular().description()).endsWith("GothicA1-Regular.ttf");
        assertThat(sources.bold().description())
                .describedAs("upstream draws a bold; collapsing it onto regular would set "
                        + "every Korean heading at body weight without anything failing")
                .endsWith("GothicA1-Bold.ttf");
        assertThat(sources.italic().description()).endsWith("GothicA1-Regular.ttf");
        assertThat(sources.boldItalic().description())
                .describedAs("bold-italic falls back to bold rather than to regular, so the "
                        + "weight survives even though the slant cannot")
                .endsWith("GothicA1-Bold.ttf");
    }

    @Test
    void aMissingFaceOfALongStandingFamilyIsDiagnosedTheSameWay() {
        FontFamilyDefinition definition = FontFamilyDefinition
                .classpath(FontName.of("Lato Probe"), "fonts/google/lato/Lato-NoSuchFace.ttf")
                .build();

        // Lato predates the artifact split, but that says nothing about why one of its
        // faces is missing here: the artifact is present, so the answer is the same as for
        // any other family. It used to be told to add a dependency it already had.
        assertThatThrownBy(() -> definition.fontSourceSet().orElseThrow().regular().openStream())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(bundledFontsVersion())
                .hasMessageNotContaining("v1.8.0");
    }
}
