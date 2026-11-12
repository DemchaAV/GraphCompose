package com.demcha.compose.document.backend.fixed.pptx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Rewrites a face's {@code OS/2} weight class, for a fixture that needs a given weight.
 *
 * <p>A real face rewritten rather than one built here, so that everything else about the
 * font — the tables both readers require, the names, the outlines — is a shipped face's
 * and only the field under test differs. The field is two bytes at a fixed place in the
 * table, so the rewrite needs no other table touched: nothing the style element is read
 * from is keyed on this field, and neither reader in front of the embedder validates a
 * table's checksum against its content. Other weight-ish fields are left as the face had
 * them — the PANOSE weight digit, which the container copies across verbatim, and
 * {@code head.macStyle}'s bold bit — so a rewritten face carries a weight its PANOSE does
 * not agree with. That is deliberate and harmless here, because the element is read from
 * the container's weight and italic fields and from nothing else.</p>
 *
 * <p>Separate from {@link FontNameTable}, which rebuilds a whole table and has to move
 * everything after it. This one writes in place.</p>
 */
final class FontWeightClass {

    /** {@code 'OS/2'} as the four bytes a table directory entry carries. */
    private static final int OS2_TAG = 0x4F532F32;

    /**
     * Where {@code usWeightClass} sits in {@code OS/2}: after the two-byte table version
     * and the two-byte {@code xAvgCharWidth}.
     */
    private static final int WEIGHT_CLASS_IN_OS2 = 4;

    private FontWeightClass() {
    }

    /** A copy of the face, with {@code usWeightClass} set to {@code weight}. */
    static byte[] with(byte[] font, int weight) {
        byte[] rewritten = font.clone();
        ByteBuffer sfnt = ByteBuffer.wrap(rewritten).order(ByteOrder.BIG_ENDIAN);
        int tables = Short.toUnsignedInt(sfnt.getShort(4));
        for (int index = 0; index < tables; index++) {
            int record = 12 + index * 16;
            if (sfnt.getInt(record) == OS2_TAG) {
                sfnt.putShort(sfnt.getInt(record + 8) + WEIGHT_CLASS_IN_OS2, (short) weight);
                return rewritten;
            }
        }
        throw new AssertionError("the face carries no OS/2 table");
    }
}
