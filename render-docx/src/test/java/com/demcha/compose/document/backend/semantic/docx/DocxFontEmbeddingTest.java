package com.demcha.compose.document.backend.semantic.docx;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two things standing between a font file and a font part: its licence and its format.
 *
 * <p>Both are read from the specification rather than from our own output, so both are
 * checked against it: the key derivation against the one worked example the format
 * publishes, and the licence bits against fonts built here to carry each value.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxFontEmbeddingTest {

    /**
     * The example {@code fontKey} published with the obfuscation algorithm, and the key
     * bytes it must produce — its hexadecimal digits read backwards, pair by pair.
     */
    private static final UUID PUBLISHED_EXAMPLE = UUID.fromString("F9168C5E-CEB2-4FAA-B6BF-329BF39FA1E4");
    private static final int[] PUBLISHED_KEY = {
            0xE4, 0xA1, 0x9F, 0xF3, 0x9B, 0x32, 0xBF, 0xB6,
            0xAA, 0x4F, 0xB2, 0xCE, 0x5E, 0x8C, 0x16, 0xF9};

    @Test
    void theKeyIsTheOneTheFormatPublishes() {
        byte[] key = DocxFontEmbedding.keyOf(PUBLISHED_EXAMPLE);

        assertThat(key).hasSize(16);
        for (int index = 0; index < key.length; index++) {
            assertThat(Byte.toUnsignedInt(key[index]))
                    .as("key byte %d", index)
                    .isEqualTo(PUBLISHED_KEY[index]);
        }
    }

    @Test
    void onlyTheFirstThirtyTwoBytesAreScrambledAndTheyComeBack() {
        byte[] font = new byte[200];
        for (int index = 0; index < font.length; index++) {
            font[index] = (byte) index;
        }

        DocxFontEmbedding.Obfuscated part = DocxFontEmbedding.obfuscate(font, PUBLISHED_EXAMPLE);

        assertThat(part.bytes()).hasSameSizeAs(font);
        assertThat(part.fontKey())
                .as("as w:fontKey carries it — braced and upper case")
                .isEqualTo("{F9168C5E-CEB2-4FAA-B6BF-329BF39FA1E4}");
        // Past the header the font is untouched: a part is a font with a scrambled front,
        // not an encrypted file.
        for (int index = 32; index < font.length; index++) {
            assertThat(part.bytes()[index]).as("byte %d", index).isEqualTo(font[index]);
        }
        // And the front comes back with the same key, which is what Word does to read it.
        byte[] restored = DocxFontEmbedding.obfuscate(part.bytes(), PUBLISHED_EXAMPLE).bytes();
        assertThat(restored).isEqualTo(font);
    }

    @Test
    void aShortFontIsScrambledAsFarAsItGoes() {
        byte[] font = new byte[8];

        byte[] scrambled = DocxFontEmbedding.obfuscate(font, PUBLISHED_EXAMPLE).bytes();

        assertThat(scrambled).hasSize(8).isNotEqualTo(font);
    }

    @Test
    void aFaceSaysWhatMayBeDoneWithIt() {
        // 0 is installable, and bit 3 is editable: both may travel in a document people
        // type into.
        assertThat(DocxFontEmbedding.permissionOf(fontWithFsType(0x0000)))
                .isEqualTo(DocxFontEmbedding.Permission.ALLOWED);
        assertThat(DocxFontEmbedding.permissionOf(fontWithFsType(0x0008)))
                .isEqualTo(DocxFontEmbedding.Permission.ALLOWED);
        // Bit 1 forbids embedding outright.
        assertThat(DocxFontEmbedding.permissionOf(fontWithFsType(0x0002)))
                .isEqualTo(DocxFontEmbedding.Permission.RESTRICTED);
        // Bit 2 alone permits reading and printing, which an edited document cannot rely
        // on — the distinction this whole check exists for.
        assertThat(DocxFontEmbedding.permissionOf(fontWithFsType(0x0004)))
                .isEqualTo(DocxFontEmbedding.Permission.PREVIEW_ONLY);
        // Bits that restrict nothing about embedding — no subsetting, bitmap only — leave
        // the answer alone.
        assertThat(DocxFontEmbedding.permissionOf(fontWithFsType(0x0300)))
                .isEqualTo(DocxFontEmbedding.Permission.ALLOWED);
    }

    @Test
    void aFontWithNoReadableTableSaysSo() {
        assertThat(DocxFontEmbedding.permissionOf(null))
                .isEqualTo(DocxFontEmbedding.Permission.UNKNOWN);
        assertThat(DocxFontEmbedding.permissionOf(new byte[4]))
                .isEqualTo(DocxFontEmbedding.Permission.UNKNOWN);
        // A well-formed directory that simply has no OS/2 table in it.
        assertThat(DocxFontEmbedding.permissionOf(fontWithTable("cmap", 0)))
                .isEqualTo(DocxFontEmbedding.Permission.UNKNOWN);
    }

    /** A font file carrying one {@code OS/2} table, long enough to reach {@code fsType}. */
    private static byte[] fontWithFsType(int fsType) {
        return fontWithTable("OS/2", fsType);
    }

    private static byte[] fontWithTable(String tag, int fsType) {
        int tableOffset = 12 + 16;
        byte[] font = new byte[tableOffset + 96];
        ByteBuffer buffer = ByteBuffer.wrap(font).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(0, 0x00010000);
        buffer.putShort(4, (short) 1);
        System.arraycopy(tag.getBytes(StandardCharsets.US_ASCII), 0, font, 12, 4);
        buffer.putInt(20, tableOffset);
        buffer.putInt(24, 96);
        buffer.putShort(tableOffset + 8, (short) fsType);
        return font;
    }
}
