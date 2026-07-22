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

    private static byte[] utf16(String value) throws IOException {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_16LE);
        if (encoded.length > 0xFFFF) {
            throw new IOException("PPTX embedded-font metadata exceeds the EOT name limit");
        }
        return encoded;
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
