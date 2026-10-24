package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The line an editor sets a face in, read from the face, against the line Word set it in.
 *
 * <p>The expected heights and descents are Word's (16.0, Windows), measured from the PDF it made
 * of a 20pt paragraph of each face in single spacing: the distance between its baselines, and
 * from a baseline to the line's foot. Word counts on a grid of a six-hundredth of an inch, so
 * they are held to 0.15pt.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxNaturalLineTest {

    private static final double SIZE = 20;
    private static final double WORDS_GRID = 0.15;

    @Test
    void aFaceThatUsesItsTypographicMetricsIsSetOnThemWithTheirLineGapAbove() throws IOException {
        // Lato, Poppins and Gothic A1 set USE_TYPO_METRICS; Poppins and Gothic A1 have a line gap,
        // which Word adds above the text.
        assertLine(FontName.LATO, 24.00, 4.30);
        assertLine(FontName.POPPINS, 30.00, 6.94);
        assertLine(FontName.GOTHIC_A1, 25.02, 4.12);
        assertLine(FontName.SPECTRAL, 30.42, 9.16);
    }

    @Test
    void aFaceThatDoesNotIsSetOnItsWindowsMetricsWithWhatItsLineGapAsksBeyondThem() throws IOException {
        // Tinos asks for 87 units of line gap beyond its Windows ascent and descent; Carlito and
        // Volkhov for none.
        assertLine(FontName.TINOS, 22.98, 4.24);
        assertLine(FontName.CARLITO, 24.42, 5.44);
        assertLine(FontName.VOLKHOV, 25.80, 6.34);
    }

    @Test
    void aFileWithoutTheTablesHasNoLine() throws IOException {
        byte[] lato = regularOf(FontName.LATO);

        assertThat(DocxNaturalLine.of(null)).isNull();
        assertThat(DocxNaturalLine.of(new byte[8])).isNull();
        assertThat(DocxNaturalLine.of(Arrays.copyOf(lato, 400)))
                .as("a file cut off inside its table directory's tables")
                .isNull();
        assertThat(DocxNaturalLine.of(withTableLength(lato, "OS/2", 70)))
                .as("an OS/2 table that ends before its Windows metrics, the next table's bytes after it")
                .isNull();
    }

    /** The font with the length its table directory states for one table changed. */
    private static byte[] withTableLength(byte[] font, String tag, int length) {
        byte[] changed = font.clone();
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(changed);
        int tables = Short.toUnsignedInt(buffer.getShort(4));
        for (int index = 0; index < tables; index++) {
            int entry = 12 + index * 16;
            if (tag.equals(new String(changed, entry, 4, java.nio.charset.StandardCharsets.US_ASCII))) {
                buffer.putInt(entry + 12, length);
                return changed;
            }
        }
        throw new AssertionError("no " + tag + " table");
    }

    private static void assertLine(FontName face, double height, double descent) throws IOException {
        DocxNaturalLine line = DocxNaturalLine.of(regularOf(face));

        assertThat(line).as(face.toString()).isNotNull();
        assertThat((line.ascent() + line.descent()) * SIZE).as("%s line", face).isCloseTo(height, within(WORDS_GRID));
        assertThat(line.descent() * SIZE).as("%s descent", face).isCloseTo(descent, within(WORDS_GRID));
    }

    private static byte[] regularOf(FontName face) throws IOException {
        try (InputStream in = DocxFontTable.familiesByName(null).get(face).fontSourceSet().orElseThrow()
                .regular().openStream()) {
            return in.readAllBytes();
        }
    }
}
