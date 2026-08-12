package com.demcha.compose.font;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the catalog side of the bundled right-to-left families: that they are listed,
 * and that asking for one against an older font artifact says so.
 *
 * <p>The font binaries ship in a separately-versioned artifact, so a consumer can hold a
 * current engine and a stale {@code graph-compose-fonts}. The resource then simply is not
 * on the classpath, and the failure surfaces lazily at first use — far from its cause. The
 * message has to carry the diagnosis, because the stack trace will not.</p>
 */
class BundledRtlFamiliesTest {

    @Test
    void theRightToLeftFamiliesAreInTheBundledCatalog() {
        assertThat(DefaultFonts.bundledFontNames())
                .contains(FontName.AMIRI, FontName.DAVID_LIBRE);
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
    void aMissingNewFamilyResourceNamesTheFontsVersionThatCarriesIt() {
        FontFamilyDefinition definition = FontFamilyDefinition
                .classpath(FontName.of("Amiri Probe"), "fonts/google/amiri/Amiri-NoSuchFace.ttf")
                .build();

        // The font artifact is on this test classpath, so a missing face under a family it
        // should carry means the artifact predates the family — not that it is absent.
        assertThatThrownBy(() -> definition.fontSourceSet().orElseThrow().regular().openStream())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("graph-compose-fonts")
                .hasMessageContaining("1.1.0");
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
    void aMissingLongStandingFamilyResourceStillPointsAtTheArtifactSplit() {
        FontFamilyDefinition definition = FontFamilyDefinition
                .classpath(FontName.of("Lato Probe"), "fonts/google/lato/Lato-NoSuchFace.ttf")
                .build();

        // Asserted on the signal the split message carries rather than on the absence of the
        // version wording: a marker that has to stay absent silently stops guarding anything
        // once the wording changes.
        assertThatThrownBy(() -> definition.fontSourceSet().orElseThrow().regular().openStream())
                .isInstanceOf(IllegalArgumentException.class)
                .describedAs("a family that predates the split keeps the migration pointer "
                        + "instead of being diagnosed as a version problem")
                .hasMessageContaining("v1.8.0")
                .hasMessageContaining("docs/migration/v1.8.0-fonts.md");
    }
}
