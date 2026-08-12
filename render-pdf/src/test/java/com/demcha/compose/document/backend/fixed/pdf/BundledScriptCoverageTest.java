package com.demcha.compose.document.backend.fixed.pdf;

import static com.demcha.compose.document.backend.fixed.pdf.FontCoverageProbe.covers;
import static com.demcha.compose.document.backend.fixed.pdf.FontCoverageProbe.coversLetters;
import static org.assertj.core.api.Assertions.assertThat;

import com.demcha.compose.font.DefaultFonts;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds the "which script needs which family" table in {@code fonts/README.md} to the
 * shipped binaries.
 *
 * <p>That table is the thing a reader consults before choosing a font, and it is exactly
 * the kind of claim that goes quietly wrong: it was written from what the families are
 * known for rather than from their {@code cmap}, and two of its rows were false. Hebrew
 * was listed as reachable only through David Libre while Tinos and Cousine carry the whole
 * block, and Latin Extended-A and Vietnamese were listed as covered by "any bundled
 * family" when nine families cover almost none of them.</p>
 *
 * <p>So the table is asserted rather than trusted. The counts here are the table's counts;
 * changing the bundled set fails this test and names the row to update, which is the only
 * way a documentation table stays true across releases.</p>
 *
 * <p>Coverage means the family's regular face can encode <em>every</em> letter in the
 * range — the probe stops at the first miss, so sweeping the whole catalog over a large
 * block costs little more than the families that actually cover it.</p>
 */
class BundledScriptCoverageTest {

    /** The binary families — the standard-14 are WinAnsi and carry none of this. */
    private static final List<FontName> FAMILIES = DefaultFonts.googleFamilies().stream()
            .map(FontFamilyDefinition::name)
            .toList();

    @Test
    void theCatalogHoldsTheFamilyCountTheTableCountsAgainst() {
        assertThat(FAMILIES)
                .describedAs("every count in the fonts/README table is out of this many "
                        + "families; adding one means re-measuring the table")
                .hasSize(35);
    }

    @Test
    void arabicIsReachableOnlyThroughAmiri() {
        assertThat(familiesCovering(0x0621, 0x064A))
                .containsExactly(FontName.AMIRI);
    }

    @Test
    void georgianIsReachableOnlyThroughItsNotoFamily() {
        assertThat(familiesCovering(0x10D0, 0x10FA))
                .containsExactly(FontName.NOTO_SANS_GEORGIAN);
    }

    @Test
    void armenianIsReachableOnlyThroughItsNotoFamily() {
        assertThat(familiesCovering(0x0531, 0x0556))
                .containsExactly(FontName.NOTO_SANS_ARMENIAN);
    }

    @Test
    void hangulIsReachableOnlyThroughGothicA1() {
        assertThat(familiesCovering(0xAC00, 0xD7A3))
                .containsExactly(FontName.GOTHIC_A1);
    }

    @Test
    void hebrewIsCarriedByThreeFamiliesRatherThanOnlyTheHebrewOne() {
        assertThat(familiesCovering(0x05D0, 0x05EA))
                .describedAs("Tinos and Cousine are metric-compatible clones of Times and "
                        + "Courier and carry Hebrew as well; a document mixing Hebrew with "
                        + "Latin has more than one family to choose from")
                .containsExactlyInAnyOrder(FontName.DAVID_LIBRE, FontName.TINOS, FontName.COUSINE);
    }

    @Test
    void thaiIsCarriedByTheSixThaiFamilies() {
        assertThat(familiesCovering(0x0E01, 0x0E3A))
                .containsExactlyInAnyOrder(
                        FontName.SARABUN, FontName.KANIT, FontName.PROMPT,
                        FontName.TAVIRAJ, FontName.TRIRONG, FontName.BAI_JAMJUREE);
    }

    @Test
    void theWiderLatinAndGreekAndCyrillicRowsMatchTheirCounts() {
        assertThat(familiesCoveringLetters('A', 'z'))
                .describedAs("basic Latin is the one range every binary family carries")
                .hasSize(35);
        assertThat(familiesCoveringLetters(0x0100, 0x017F))
                .describedAs("Latin Extended-A — far from universal, which is why the table "
                        + "no longer says \"any bundled family\"")
                .hasSize(11);
        assertThat(familiesCoveringEach(VIETNAMESE))
                .describedAs("Vietnamese precomposed vowels")
                .hasSize(26);
        assertThat(familiesCoveringLetters(0x0386, 0x03CE))
                .describedAs("the modern Greek alphabet, both cases and the accented capitals "
                        + "— not the Greek and Coptic block, whose archaic letters no text "
                        + "face carries")
                .hasSize(11);
        assertThat(familiesCoveringLetters(0x0410, 0x044F))
                .describedAs("the Russian and Ukrainian letters most Cyrillic text is written in")
                .hasSize(18);
        assertThat(familiesCoveringLetters(0x0400, 0x04FF))
                .describedAs("the whole Cyrillic block is a different claim entirely — the "
                        + "extended letters the Central Asian and Caucasian languages need "
                        + "narrow it from eighteen families to four, which is the number a "
                        + "table saying \"Cyrillic\" has to be honest about")
                .hasSize(4);
    }

    /** The precomposed vowels that are Vietnamese rather than merely Latin-1. */
    private static final int[] VIETNAMESE = {
            0x1EA1, 0x1EA3, 0x1EA5, 0x1EAF, 0x1EBF, 0x1EC7, 0x1ED1,
            0x1EDD, 0x1EE7, 0x1EF1, 0x0102, 0x0110, 0x01A0, 0x01AF};

    private static List<FontName> familiesCovering(int first, int last) {
        return FAMILIES.stream()
                .filter(family -> covers(family, first, last))
                .collect(Collectors.toList());
    }

    /**
     * Like {@link #familiesCovering}, over the code points Unicode calls letters.
     *
     * <p>The wider rows span blocks with unassigned gaps and with punctuation among the
     * letters, so a plain range sweep would count nobody and the range would have to be
     * trimmed until it did — which is how the Cyrillic row came to be measured on the
     * Russian alphabet while the table said "Cyrillic".</p>
     */
    private static List<FontName> familiesCoveringLetters(int first, int last) {
        return FAMILIES.stream()
                .filter(family -> coversLetters(family, first, last))
                .collect(Collectors.toList());
    }

    private static List<FontName> familiesCoveringEach(int[] codePoints) {
        return FAMILIES.stream()
                .filter(family -> covers(family, codePoints))
                .collect(Collectors.toList());
    }
}
