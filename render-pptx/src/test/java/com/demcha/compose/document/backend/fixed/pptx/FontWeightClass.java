package com.demcha.compose.document.backend.fixed.pptx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Rewrites a face's {@code OS/2} weight class, for a fixture that needs a given weight.
 *
 * <p>A real face rewritten rather than one built here, so that everything else about the
 * font — the tables both readers require, the names, the outlines — is a shipped face's
 * and only the field under test differs. The field is two bytes at a fixed place in the
 * table, so the rewrite needs no other table touched: nothing in the file is keyed on the
 * weight, and neither reader in front of the embedder validates a table's checksum
 * against its content.</p>
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
