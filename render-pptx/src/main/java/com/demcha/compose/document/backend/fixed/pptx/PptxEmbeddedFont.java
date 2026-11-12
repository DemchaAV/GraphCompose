package com.demcha.compose.document.backend.fixed.pptx;

import org.apache.fontbox.ttf.FontHeaders;
import org.apache.fontbox.ttf.OS2WindowsMetricsTable;
import org.apache.fontbox.ttf.OTFParser;
import org.apache.pdfbox.io.RandomAccessReadBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Builds the uncompressed EOT container required by PowerPoint font parts. */
final class PptxEmbeddedFont {

    private static final int EOT_VERSION_2_1 = 0x00020001;
    private static final int EOT_MAGIC = 0x504C;
    private static final int EOT_FIXED_HEADER_SIZE = 100;
    private static final int DEFAULT_CHARSET = 1;
    private static final int ITALIC_SELECTION_BIT = 0x0001;
    private static final int MAC_STYLE_ITALIC_BIT = 0x0002;
    private static final int RESTRICTED_LICENSE_EMBEDDING = 0x0002;
    private static final int BITMAP_EMBEDDING_ONLY = 0x0200;
    /**
     * The most of a container's header, in bytes, a presentation will read.
     *
     * <p>PowerPoint's font part begins with a header carrying four names — the family, the
     * style, a version and a full name joined from the family and the style — and the
     * presentation reads them back through two windows of a thousand bytes each, the
     * tighter of which this is. Taking the face reads <em>each</em> name through
     * {@code IOUtils.safelyAllocate(nameSize, 1000)}; reading the written part back later
     * takes the first thousand bytes of it and parses the header out of those, so the
     * fixed 96 bytes and all four names together have to fit. A header past the first
     * limit costs the render — an unchecked {@code RecordFormatException} from inside
     * {@code XSLFFontInfo.addFacet}, past the point where a refusal still costs only the
     * font — and one past the second writes a part that reads back as
     * {@code Buffer overrun, having 1000 bytes in the stream}. Both are measured here,
     * where a refusal is the checked one {@code readPrograms} already degrades a family on,
     * against the stricter of the two.</p>
     *
     * <p>It has to be measured here rather than left to the readers in front of this one.
     * {@code java.awt.Font.createFont}, which the viewer metrics are read through, rejects
     * an over-long family or full <em>name record</em> but never reads the style record at
     * all, and the full name is the family and the style joined. A face whose subfamily
     * name is long therefore passes every read before the writer: Lato-Regular rewritten
     * with a 496-character subfamily makes a 1,002-byte full name, and cost the whole
     * deck.</p>
     *
     * <p>A header moves in steps of four bytes, since the family is written twice — alone
     * and inside the full name — so no face can express a header of exactly a thousand:
     * 998 is the largest one there is, and 1,002 the smallest that is too big. Any limit
     * from 998 to 1,001 is the same limit as far as a font is concerned.</p>
     *
     * <p>The thousand is POI's default, which a host can lower with
     * {@code IOUtils.setByteArrayMaxOverride}, so this is a floor rather than a
     * guarantee.</p>
     */
    private static final int MAX_READABLE_HEADER_BYTES = 1000;
    /**
     * What the presentation's reader consumes before the names: 80 bytes of fixed fields,
     * then a 2-byte padding and a 2-byte length for each of the four.
     */
    private static final int READER_FIXED_HEADER_BYTES = 96;

    private PptxEmbeddedFont() {
    }

    static byte[] wrap(InputStream input) throws IOException {
        byte[] fontData = input.readAllBytes();
        Metadata metadata = metadata(fontData);
        validateEmbeddingRights(metadata.fsType());

        byte[] family = utf16(metadata.familyName());
        byte[] style = utf16(metadata.styleName());
        byte[] version = utf16(metadata.versionName());
        byte[] full = utf16(metadata.fullName());
        validateReadableHeader(family, style, version, full);
        int eotSize = Math.addExact(
                Math.addExact(EOT_FIXED_HEADER_SIZE, fontData.length),
                Math.addExact(Math.addExact(family.length, style.length),
                        Math.addExact(version.length, full.length)));

        ByteBuffer eot = ByteBuffer.allocate(eotSize).order(ByteOrder.LITTLE_ENDIAN);
        eot.putInt(eotSize);
        eot.putInt(fontData.length);
        eot.putInt(EOT_VERSION_2_1);
        eot.putInt(0); // raw, uncompressed OpenType data
        eot.put(metadata.panose());
        eot.put((byte) DEFAULT_CHARSET);
        eot.put((byte) (metadata.italic() ? 1 : 0));
        eot.putInt(metadata.weight());
        eot.putShort((short) metadata.fsType());
        eot.putShort((short) EOT_MAGIC);
        eot.putInt((int) metadata.unicodeRange1());
        eot.putInt((int) metadata.unicodeRange2());
        eot.putInt((int) metadata.unicodeRange3());
        eot.putInt((int) metadata.unicodeRange4());
        eot.putInt((int) metadata.codePageRange1());
        eot.putInt((int) metadata.codePageRange2());
        eot.putInt((int) metadata.checkSumAdjustment());
        eot.putInt(0);
        eot.putInt(0);
        eot.putInt(0);
        eot.putInt(0);
        eot.putShort((short) 0); // padding before the family name
        putName(eot, family);
        putName(eot, style);
        putName(eot, version);
        putName(eot, full);
        eot.putShort((short) 0); // empty root string (EOT 0x00020001)
        eot.put(fontData);
        return eot.array();
    }

    private static Metadata metadata(byte[] fontData) throws IOException {
        try (RandomAccessReadBuffer input = new RandomAccessReadBuffer(fontData)) {
            FontHeaders headers = new OTFParser().parseTableHeaders(input);
            if (headers.getError() != null) {
                throw new IOException("Unable to read OpenType headers: " + headers.getError());
            }
            OS2WindowsMetricsTable os2 = headers.getOS2Windows();

            byte[] panose = os2 == null || os2.getPanose() == null
                    ? new byte[10]
                    : normalizedPanose(os2.getPanose());
            int fsSelection = os2 == null ? 0 : os2.getFsSelection();
            int macStyle = headers.getHeaderMacStyle() == null ? 0 : headers.getHeaderMacStyle();
            boolean italic = (fsSelection & ITALIC_SELECTION_BIT) != 0
                    || (macStyle & MAC_STYLE_ITALIC_BIT) != 0;
            String family = valueOr(headers.getFontFamily(), headers.getName());
            String style = valueOr(headers.getFontSubFamily(), "Regular");
            return new Metadata(
                    panose,
                    italic,
                    os2 == null ? 400 : os2.getWeightClass(),
                    os2 == null ? 0 : Short.toUnsignedInt(os2.getFsType()),
                    os2 == null ? 0 : os2.getUnicodeRange1(),
                    os2 == null ? 0 : os2.getUnicodeRange2(),
                    os2 == null ? 0 : os2.getUnicodeRange3(),
                    os2 == null ? 0 : os2.getUnicodeRange4(),
                    os2 == null ? 0 : os2.getCodePageRange1(),
                    os2 == null ? 0 : os2.getCodePageRange2(),
                    checkSumAdjustment(fontData),
                    family,
                    style,
                    "",
                    family + " " + style);
        }
    }

    private static long checkSumAdjustment(byte[] fontData) throws IOException {
        if (fontData.length < 12) {
            throw new IOException("OpenType font data is shorter than its table directory");
        }
        ByteBuffer sfnt = ByteBuffer.wrap(fontData).order(ByteOrder.BIG_ENDIAN);
        int tableCount = Short.toUnsignedInt(sfnt.getShort(4));
        long directoryEnd = 12L + tableCount * 16L;
        if (directoryEnd > fontData.length) {
            throw new IOException("OpenType table directory exceeds the font data");
        }
        for (int index = 0; index < tableCount; index++) {
            int record = 12 + index * 16;
            if (sfnt.getInt(record) != 0x68656164) { // 'head'
                continue;
            }
            long offset = Integer.toUnsignedLong(sfnt.getInt(record + 8));
            if (offset + 12 > fontData.length) {
                throw new IOException("OpenType head table exceeds the font data");
            }
            return Integer.toUnsignedLong(sfnt.getInt((int) offset + 8));
        }
        return 0;
    }

    private static byte[] normalizedPanose(byte[] panose) {
        byte[] normalized = new byte[10];
        System.arraycopy(panose, 0, normalized, 0, Math.min(panose.length, normalized.length));
        return normalized;
    }

    private static byte[] utf16(String value) {
        return value.getBytes(StandardCharsets.UTF_16LE);
    }

    /**
     * Refuses a header the presentation would not read back whole.
     *
     * <p>Refused rather than shortened: what a viewer matches an embedded program to its
     * declared typeface by is not something this backend has measured, so trimming a name
     * the deck would then carry risks a program no viewer uses, where not carrying it is
     * the degradation the embedder already warns about.</p>
     *
     * <p>Each name's own length is named in the message, not only the total, because the
     * one that overflows is usually the full name — which no record of the font carries,
     * being joined here from the other two — so a total on its own matches nothing a
     * caller can look for in their font.</p>
     */
    private static void validateReadableHeader(byte[] family, byte[] style, byte[] version,
                                               byte[] full) throws IOException {
        int header = READER_FIXED_HEADER_BYTES
                + family.length + style.length + version.length + full.length;
        if (header > MAX_READABLE_HEADER_BYTES) {
            throw new IOException("PPTX embedded-font header is " + header
                    + " bytes, past the " + MAX_READABLE_HEADER_BYTES
                    + " a presentation reads: family " + family.length + ", style "
                    + style.length + ", version " + version.length + ", full name "
                    + full.length + " (the full name is the family and the style joined)");
        }
    }

    private static void putName(ByteBuffer target, byte[] encoded) {
        target.putShort((short) encoded.length);
        target.put(encoded);
        target.putShort((short) 0);
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void validateEmbeddingRights(int fsType) throws IOException {
        if ((fsType & RESTRICTED_LICENSE_EMBEDDING) != 0) {
            throw new IOException("Font license flags prohibit embedding in PPTX");
        }
        if ((fsType & BITMAP_EMBEDDING_ONLY) != 0) {
            throw new IOException("Font license flags permit bitmap embedding only");
        }
    }

    private record Metadata(byte[] panose,
                            boolean italic,
                            int weight,
                            int fsType,
                            long unicodeRange1,
                            long unicodeRange2,
                            long unicodeRange3,
                            long unicodeRange4,
                            long codePageRange1,
                            long codePageRange2,
                            long checkSumAdjustment,
                            String familyName,
                            String styleName,
                            String versionName,
                            String fullName) {
    }
}
