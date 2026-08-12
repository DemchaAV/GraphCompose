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
    void aMissingResourceNamesTheFontsVersionForEveryFamilyThatNeedsOne() {
        // The version map is keyed on the resource folder segment, and those keys are string
        // literals written independently of the folder names in DefaultFonts. One typo and the
        // family silently falls back to the generic "add the dependency" message — advice that
        // is wrong for a consumer who already has the dependency, just an older one. So every
        // family that needs the diagnostic is driven through it rather than one sample.
        for (FontName family : List.of(FontName.AMIRI, FontName.DAVID_LIBRE,
                FontName.NOTO_SANS_GEORGIAN, FontName.NOTO_SANS_ARMENIAN, FontName.GOTHIC_A1)) {
            String missingFace = folderOf(family) + "/NoSuchFace.ttf";
            FontFamilyDefinition definition = FontFamilyDefinition
                    .classpath(FontName.of(family.name() + " Probe"), missingFace)
                    .build();

            // The font artifact is on this test classpath, so a missing face under a family it
            // should carry means the artifact predates the family — not that it is absent.
            assertThatThrownBy(() -> definition.fontSourceSet().orElseThrow().regular().openStream())
                    .describedAs("%s resolves under %s, which must be a key the version map "
                            + "knows", family, missingFace)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("graph-compose-fonts")
                    .hasMessageContaining("1.1.0");
        }
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
