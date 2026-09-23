package com.demcha.compose.document.backend.semantic.docx;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * What a font file says about being embedded, and how Word wants it written.
 *
 * <p>Two things stand between a font on the classpath and a font inside a {@code .docx}.
 * The first is permission: an OpenType face states in its {@code OS/2} table what an
 * embedder may do with it, and a face that may only be previewed and printed is not a
 * face a document can be edited in. The second is the format: Word does not store the
 * font as it came, but with its first bytes scrambled against a key the package carries,
 * so a font part is not a font file anyone can lift out and install.</p>
 *
 * @author Artem Demchyshyn
 */
final class DocxFontEmbedding {

    /** {@code fsType} bit 1: the face may not be embedded at all. */
    private static final int RESTRICTED_LICENSE = 0x0002;
    /** {@code fsType} bit 2: it may be embedded to read and print, but not to edit. */
    private static final int PREVIEW_AND_PRINT = 0x0004;
    /** {@code fsType} bit 3: it may be embedded in a document that is edited. */
    private static final int EDITABLE = 0x0008;

    /** Bytes of a font part that are scrambled — always the first 32, or the whole file. */
    private static final int OBFUSCATED_PREFIX = 32;

    private DocxFontEmbedding() {
    }

    /** What a face's own licence bits allow. */
    enum Permission {
        /** Installable or editable: it may go into a document people will edit. */
        ALLOWED,
        /** Preview-and-print only: readable and printable, but not for an editable file. */
        PREVIEW_ONLY,
        /** Restricted: the face may not be embedded at all. */
        RESTRICTED,
        /** No {@code OS/2} table, or one too short to carry {@code fsType}. */
        UNKNOWN
    }

    /**
     * Reads what {@code font} permits, from its own {@code OS/2} table.
     *
     * <p>{@code fsType} is a bit field with one exception to that: zero means installable
     * embedding, the most permissive value there is. Bits 1 and 2 are mutually exclusive
     * with bit 3 by the specification, and a font that sets none of the three is treated
     * the same as zero — it restricts nothing.</p>
     *
     * @param font the whole font file
     * @return what its licence bits allow
     * @see <a href="https://learn.microsoft.com/en-us/typography/opentype/spec/os2#fstype">
     *      OpenType OS/2 fsType</a>
     */
    static Permission permissionOf(byte[] font) {
        int fsType = readFsType(font);
        if (fsType < 0) {
            return Permission.UNKNOWN;
        }
        if ((fsType & RESTRICTED_LICENSE) != 0) {
            return Permission.RESTRICTED;
        }
        if ((fsType & PREVIEW_AND_PRINT) != 0 && (fsType & EDITABLE) == 0) {
            return Permission.PREVIEW_ONLY;
        }
        return Permission.ALLOWED;
    }

    /**
     * Finds {@code fsType} in the font's table directory.
     *
     * @return the raw {@code fsType}, or -1 when the font carries no readable {@code OS/2}
     */
    private static int readFsType(byte[] font) {
        if (font == null || font.length < 12) {
            return -1;
        }
        ByteBuffer buffer = ByteBuffer.wrap(font).order(ByteOrder.BIG_ENDIAN);
        int numTables = Short.toUnsignedInt(buffer.getShort(4));
        for (int index = 0; index < numTables; index++) {
            int entry = 12 + index * 16;
            if (entry + 16 > font.length) {
                return -1;
            }
            String tag = new String(font, entry, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (!"OS/2".equals(tag)) {
                continue;
            }
            int offset = buffer.getInt(entry + 8);
            // version(2) avgCharWidth(2) weightClass(2) widthClass(2) then fsType.
            if (offset < 0 || offset + 10 > font.length) {
                return -1;
            }
            return Short.toUnsignedInt(buffer.getShort(offset + 8));
        }
        return -1;
    }

    /**
     * A font part: the bytes Word stores, and the key it needs to read them back.
     *
     * @param bytes   the obfuscated font
     * @param fontKey the key, formatted the way {@code w:fontKey} carries it
     */
    record Obfuscated(byte[] bytes, String fontKey) {
    }

    /**
     * Scrambles a font the way a Word package stores one.
     *
     * <p>A font part is the font file with its first 32 bytes — the sfnt header and the
     * start of the table directory — exclusive-ored against a key derived from a GUID the
     * package states beside it. It is not encryption and is not meant to be: it stops a
     * font being lifted out of a document and installed, and nothing more.</p>
     *
     * <p>The key is the GUID's sixteen bytes, read from its hexadecimal form in reverse
     * order, pair by pair, and applied twice over the thirty-two bytes.</p>
     *
     * @param font the font file as it came
     * @return the part's bytes and the key that unscrambles them
     */
    static Obfuscated obfuscate(byte[] font) {
        return obfuscate(font, UUID.randomUUID());
    }

    /**
     * The same, against a stated key — so the derivation can be checked against the one
     * worked example the format publishes rather than only against itself.
     *
     * @param font the font file as it came
     * @param uuid the key to scramble it with
     * @return the part's bytes and the key that unscrambles them
     */
    static Obfuscated obfuscate(byte[] font, UUID uuid) {
        byte[] key = keyOf(uuid);
        byte[] scrambled = font.clone();
        for (int index = 0; index < Math.min(OBFUSCATED_PREFIX, scrambled.length); index++) {
            scrambled[index] ^= key[index % key.length];
        }
        return new Obfuscated(scrambled,
                "{" + uuid.toString().toUpperCase(java.util.Locale.ROOT) + "}");
    }

    /**
     * The sixteen key bytes of a font key: its hexadecimal digits, read in reverse by pair.
     *
     * @param uuid the GUID {@code w:fontKey} states
     * @return the key the first bytes of the font are exclusive-ored against
     */
    static byte[] keyOf(UUID uuid) {
        // The GUID's digits read backwards by pair are its sixteen bytes read backwards: the
        // low half's least significant byte first. Taken from the bits rather than from the
        // printed form, so there is no text to parse.
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        byte[] key = new byte[16];
        for (int index = 0; index < 8; index++) {
            key[index] = (byte) (least >>> (8 * index));
            key[8 + index] = (byte) (most >>> (8 * index));
        }
        return key;
    }
}
