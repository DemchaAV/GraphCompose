package com.demcha.compose.document.backend.semantic.docx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The line an editor sets a face's text in where nothing fixes its height, read from the face's
 * own tables, in ems.
 *
 * <p>Word and LibreOffice agree on it (measured on every face the templates use, 10 and 20pt,
 * Word on Windows and LibreOffice 24.2 on Linux, to 0.15pt): a face whose {@code OS/2} table sets
 * {@code USE_TYPO_METRICS} is set on its typographic ascender and descender with its typographic
 * line gap above them, and any other on its Windows ascent and descent with what {@code hhea}'s
 * line gap asks beyond them above. A line given a least height taller than this keeps the spare
 * above its text: the baseline stands this line's {@link #descent} above the line's foot.</p>
 *
 * @param ascent  how far the line reaches above the baseline, its line gap included, in ems
 * @param descent how far it reaches below, in ems
 * @author Artem Demchyshyn
 */
record DocxNaturalLine(double ascent, double descent) {

    /** {@code fsSelection} bit 7: the typographic metrics are the face's line. */
    private static final int USE_TYPO_METRICS = 0x0080;
    /** The {@code OS/2} version that defines {@code USE_TYPO_METRICS}. */
    private static final int TYPO_METRICS_VERSION = 4;
    /** The {@code OS/2} table's length up to and including {@code usWinDescent}. */
    private static final int OS2_THROUGH_WIN_DESCENT = 78;

    /**
     * The face's line in an editor.
     *
     * @param font the whole font file
     * @return its line, or null where the font carries no readable {@code head}, {@code hhea} or
     *         {@code OS/2} table
     */
    static DocxNaturalLine of(byte[] font) {
        int head = DocxFontEmbedding.tableOffset(font, "head", 20);
        int hhea = DocxFontEmbedding.tableOffset(font, "hhea", 10);
        int os2 = DocxFontEmbedding.tableOffset(font, "OS/2", OS2_THROUGH_WIN_DESCENT);
        if (head < 0 || hhea < 0 || os2 < 0) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(font).order(ByteOrder.BIG_ENDIAN);
        double unitsPerEm = Short.toUnsignedInt(buffer.getShort(head + 18));
        if (!(unitsPerEm > 0)) {
            return null;
        }
        int version = Short.toUnsignedInt(buffer.getShort(os2));
        int selection = Short.toUnsignedInt(buffer.getShort(os2 + 62));
        if (version >= TYPO_METRICS_VERSION && (selection & USE_TYPO_METRICS) != 0) {
            int ascender = buffer.getShort(os2 + 68);
            int descender = buffer.getShort(os2 + 70);
            int lineGap = Math.max(0, buffer.getShort(os2 + 72));
            return new DocxNaturalLine((ascender + lineGap) / unitsPerEm, -descender / unitsPerEm);
        }
        int winAscent = Short.toUnsignedInt(buffer.getShort(os2 + 74));
        int winDescent = Short.toUnsignedInt(buffer.getShort(os2 + 76));
        int ascender = buffer.getShort(hhea + 4);
        int descender = buffer.getShort(hhea + 6);
        int lineGap = buffer.getShort(hhea + 8);
        int beyond = Math.max(0, lineGap - ((winAscent + winDescent) - (ascender - descender)));
        return new DocxNaturalLine((winAscent + beyond) / unitsPerEm, winDescent / unitsPerEm);
    }
}
