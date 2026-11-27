package com.demcha.compose.document.backend.fixed.pptx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Rewrites a face's {@code OS/2} {@code fsType}, for a fixture that needs given licence bits.
 *
 * <p>A real face rewritten in place, as {@link FontWeightClass} rewrites the weight, so that
 * only the licence bits differ from a shipped face. Neither reader in front of the embedder
 * checks a table's checksum against its content.</p>
 */
final class FontEmbeddingType {

    /** {@code 'OS/2'} as the four bytes a table directory entry carries. */
    private static final int OS2_TAG = 0x4F532F32;

    /**
     * Where {@code fsType} sits in {@code OS/2}: after the version, {@code xAvgCharWidth},
     * {@code usWeightClass} and {@code usWidthClass}, two bytes each.
     */
    private static final int FS_TYPE_IN_OS2 = 8;

    private FontEmbeddingType() {
    }

    /** A copy of the face, with {@code fsType} set to {@code fsType}. */
    static byte[] with(byte[] font, int fsType) {
        byte[] rewritten = font.clone();
        ByteBuffer sfnt = ByteBuffer.wrap(rewritten).order(ByteOrder.BIG_ENDIAN);
        int tables = Short.toUnsignedInt(sfnt.getShort(4));
        for (int index = 0; index < tables; index++) {
            int record = 12 + index * 16;
            if (sfnt.getInt(record) == OS2_TAG) {
                sfnt.putShort(sfnt.getInt(record + 8) + FS_TYPE_IN_OS2, (short) fsType);
                return rewritten;
            }
        }
        throw new AssertionError("the face carries no OS/2 table");
    }

    /** The face's {@code fsType}, as it reads. */
    static int of(byte[] font) {
        ByteBuffer sfnt = ByteBuffer.wrap(font).order(ByteOrder.BIG_ENDIAN);
        int tables = Short.toUnsignedInt(sfnt.getShort(4));
        for (int index = 0; index < tables; index++) {
            int record = 12 + index * 16;
            if (sfnt.getInt(record) == OS2_TAG) {
                return Short.toUnsignedInt(sfnt.getShort(sfnt.getInt(record + 8) + FS_TYPE_IN_OS2));
            }
        }
        throw new AssertionError("the face carries no OS/2 table");
    }
}
