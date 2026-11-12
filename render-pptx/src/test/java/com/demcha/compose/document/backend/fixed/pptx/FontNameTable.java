package com.demcha.compose.document.backend.fixed.pptx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Rewrites one name of a real font, so a test can vary a name's length and nothing else.
 *
 * <p>Which name matters here. The container the PPTX embedder builds carries four, and the
 * readers in front of it do not read the same ones: the viewer metrics' reader rejects an
 * over-long family or full name and never looks at the subfamily, while the presentation
 * reads all four back. A test that varied the family name alone would therefore measure
 * the two readers agreeing rather than the gap between them, so this takes the name ID as
 * an argument.</p>
 *
 * <p>The table is rebuilt record for record — the Mac platform entries, the full name, the
 * PostScript name all carried over — and only the string of each record with the given name
 * ID is replaced, then appended at the end of the file with the table directory's entry
 * re-pointed, which is how a longer name can be written without moving every other table.
 * The table directory's checksum and {@code head}'s {@code checkSumAdjustment} are left as
 * they were: nothing in either reader's path verifies them, so what differs for the
 * readers is the name's length alone.</p>
 */
final class FontNameTable {

    private static final int NAME_TAG = 0x6e616d65;

    private FontNameTable() {
    }

    /**
     * The same face with every record of {@code nameId} carrying {@code value}.
     *
     * @param font   a face with a format 0 name table
     * @param nameId the name ID to rewrite — 1 is the family, 2 the subfamily, 4 the full
     *               name
     * @param value  the replacement string. A platform 1 record stores a byte per
     *               character, so anything outside Latin-1 becomes {@code ?} there — fine
     *               for a length fixture, a trap for a test about the characters
     * @return the rewritten face
     */
    static byte[] with(byte[] font, int nameId, String value) {
        ByteBuffer sfnt = ByteBuffer.wrap(font).order(ByteOrder.BIG_ENDIAN);
        int directoryEntry = -1;
        for (int index = 0, tables = Short.toUnsignedInt(sfnt.getShort(4));
                index < tables; index++) {
            if (sfnt.getInt(12 + index * 16) == NAME_TAG) {
                directoryEntry = 12 + index * 16;
                break;
            }
        }
        if (directoryEntry < 0) {
            throw new AssertionError("the face has no name table");
        }
        int table = sfnt.getInt(directoryEntry + 8);
        if (Short.toUnsignedInt(sfnt.getShort(table)) != 0) {
            throw new AssertionError("not a format 0 name table: rebuilding it as one would "
                    + "drop the language-tag records, and the face would then differ from "
                    + "the original in more than the name");
        }
        int storage = table + Short.toUnsignedInt(sfnt.getShort(table + 4));

        List<int[]> records = new ArrayList<>();
        List<byte[]> strings = new ArrayList<>();
        for (int index = 0, count = Short.toUnsignedInt(sfnt.getShort(table + 2));
                index < count; index++) {
            int at = table + 6 + index * 12;
            int platform = Short.toUnsignedInt(sfnt.getShort(at));
            int id = Short.toUnsignedInt(sfnt.getShort(at + 6));
            int offset = storage + Short.toUnsignedInt(sfnt.getShort(at + 10));
            records.add(new int[] {platform, Short.toUnsignedInt(sfnt.getShort(at + 2)),
                    Short.toUnsignedInt(sfnt.getShort(at + 4)), id});
            strings.add(id == nameId
                    // Platform 1 stores a name a byte per character, platform 3 in UTF-16.
                    ? value.getBytes(platform == 1
                            ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_16BE)
                    : Arrays.copyOfRange(font, offset,
                            offset + Short.toUnsignedInt(sfnt.getShort(at + 8))));
        }

        int stringBytes = strings.stream().mapToInt(string -> string.length).sum();
        if (stringBytes > 0xFFFF) {
            // A record's offset into the string storage is sixteen bits wide, so past this
            // the table would be silently wrong rather than longer.
            throw new AssertionError("the rewritten names need " + stringBytes
                    + " bytes of storage, past the 65,535 a record's offset can address");
        }
        ByteBuffer rebuilt = ByteBuffer
                .allocate(6 + 12 * records.size() + stringBytes).order(ByteOrder.BIG_ENDIAN);
        rebuilt.putShort((short) 0); // format 0
        rebuilt.putShort((short) records.size());
        rebuilt.putShort((short) (6 + 12 * records.size()));
        for (int index = 0, offset = 0; index < records.size(); index++) {
            int[] record = records.get(index);
            rebuilt.putShort((short) record[0]);
            rebuilt.putShort((short) record[1]);
            rebuilt.putShort((short) record[2]);
            rebuilt.putShort((short) record[3]);
            rebuilt.putShort((short) strings.get(index).length);
            rebuilt.putShort((short) offset);
            offset += strings.get(index).length;
        }
        strings.forEach(rebuilt::put);

        int appendAt = (font.length + 3) & ~3; // tables start on a four-byte boundary
        byte[] patched = Arrays.copyOf(font, appendAt + rebuilt.capacity());
        System.arraycopy(rebuilt.array(), 0, patched, appendAt, rebuilt.capacity());
        ByteBuffer repointed = ByteBuffer.wrap(patched).order(ByteOrder.BIG_ENDIAN);
        repointed.putInt(directoryEntry + 8, appendAt);
        repointed.putInt(directoryEntry + 12, rebuilt.capacity());
        return patched;
    }
}
