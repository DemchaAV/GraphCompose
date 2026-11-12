package com.demcha.compose.document.backend.fixed.pptx;

import org.apache.fontbox.ttf.FontHeaders;
import org.apache.fontbox.ttf.OS2WindowsMetricsTable;
import org.apache.fontbox.ttf.OTFParser;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.poi.common.usermodel.fonts.FontHeader;

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
     * <p>This is the one account of the limit; everything else that needs it points here.
     * PowerPoint's font part begins with a header carrying four names — the family, the
     * style, a version and a full name joined from the family and the style — and the
     * presentation reads that header twice, each time out of a thousand bytes, under
     * different rules. Taking the face reads <em>each name</em> through
     * {@code IOUtils.safelyAllocate(nameSize, 1000)}, and a longer one costs the render: an
     * unchecked {@code RecordFormatException} from inside {@code XSLFFontInfo.addFacet},
     * past the point where a refusal still costs only the font. Reading the written part
     * back later takes the first thousand bytes of the part and parses the whole header out
     * of those, so {@link #READER_FIXED_HEADER_BYTES} and all four names together have to
     * fit, or the part comes back as {@code Buffer overrun, having 1000 bytes in the
     * stream}. The second rule is the stricter — it bounds the four together where the
     * first bounds them one at a time — so it is the one measured here, where a refusal is
     * the checked one {@code readPrograms} already degrades a family on.</p>
     *
     * <p>It has to be measured here rather than left to the readers in front of this one.
     * {@code java.awt.Font.createFont}, which the viewer metrics are read through, rejects
     * an over-long family or full <em>name record</em> but never reads the style record at
     * all, and the full name is the family and the style joined. A face whose subfamily
     * name is long therefore passes every read before the writer: Lato-Regular rewritten
     * with a 496-character subfamily makes a 1,002-byte full name, and cost the whole
     * deck.</p>
     *
     * <p>A header moves in steps of four bytes, because the family is written twice — alone
     * and inside the full name — while the version name is always empty. So no face can
     * express a header of exactly a thousand: 998 is the largest one there is, and 1,002
     * the smallest that is too big, and any limit from 998 to 1,001 is the same limit as
     * far as a font is concerned. The heaviest of the 130 bundled faces is 250 bytes, so
     * this bears only on a face a caller registers.</p>
     *
     * <p>Two things it does not promise. The thousand is POI's default, which a host can
     * lower with {@code IOUtils.setByteArrayMaxOverride}, so this is a floor rather than a
     * guarantee. And the read of the part wants a thousand bytes to be <em>there</em>: a
     * container shorter than that comes back as an {@code EOFException} instead, which a
     * font of a few hundred bytes would reach and nothing a caller would ship does.</p>
     */
    private static final int MAX_READABLE_HEADER_BYTES = 1000;
    /**
     * What the presentation's reader consumes before the names: 80 bytes of fixed fields,
     * then a 2-byte padding and a 2-byte length for each of the four.
     *
     * <p>Four less than {@link #EOT_FIXED_HEADER_SIZE}, which is what this container
     * <em>writes</em>: the reader stops after the full name and never takes its trailing
     * padding or the root string's length. Derived from it rather than written out, so a
     * change to the layout cannot move one without the other.</p>
     */
    private static final int READER_FIXED_HEADER_BYTES = EOT_FIXED_HEADER_SIZE - 4;

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

    /**
     * How a presentation will seat a container this class built.
     *
     * <p>{@code slot} is the element the font list declares the face under —
     * {@code regular}, {@code bold}, {@code italic} or {@code boldItalic} — and the other
     * two are the fields that decide it, carried so a refusal can say what made two faces
     * collide rather than only that they did.</p>
     *
     * @param slot   the font list element the face would be declared under
     * @param weight the container's weight, as the presentation reads it
     * @param italic the container's italic flag, as the presentation reads it
     */
    record Style(String slot, int weight, boolean italic) {
    }

    /**
     * Reads back the style a presentation will seat this container in.
     *
     * <p>Read through POI's own {@link FontHeader}, the reader
     * {@code XSLFFontInfo.addFacet} decides the slot with, rather than from the fields
     * this class put in the buffer: the slot is that reader's answer, and a second
     * derivation of it here could drift from it without either side being wrong on its own
     * terms. The four-way split below is the same one {@code addFacet} switches on, where
     * {@link FontHeader#isBold()} is {@code getWeight() > 400} written as a predicate.</p>
     *
     * <p>There are four slots and no more, and a presentation holds one face in each: a
     * second face seated in a slot overwrites the part the first one wrote, keeping the
     * declaration the first made. So the slot is not a label but a capacity, which is why
     * the caller treats two faces reading back the same one as a refusal.</p>
     *
     * <p>A {@code String} naming the font-list element rather than the caller's own
     * four-valued {@code Face}, and not a duplicate of it: {@code Face} is the slot a
     * family <em>registers</em> a face under, this is the element the presentation
     * <em>declares</em> it under, and the whole of what is guarded here is that the two
     * need not agree. Spelling it as the element keeps the refusal's wording and the
     * written {@code p:} tag the same string.</p>
     *
     * @param container a container {@link #wrap} built
     * @return the slot it would be seated in, and the two fields that decide it
     */
    static Style styleOf(byte[] container) {
        FontHeader header = new FontHeader();
        header.init(container, 0, container.length);
        boolean bold = header.isBold();
        boolean italic = header.isItalic();
        String slot = bold && italic ? "boldItalic"
                : bold ? "bold" : italic ? "italic" : "regular";
        return new Style(slot, header.getWeight(), italic);
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
     * <p>The names are named one by one in the message, not only totalled, because the one
     * that overflows is usually the full name — which no record of the font carries, being
     * joined here from the other two — so a total on its own matches nothing a caller can
     * look for in their font. The version is left out of the message, not out of the sum:
     * this container always writes it empty.</p>
     *
     * @see #MAX_READABLE_HEADER_BYTES for what the limit is and what it does not promise
     */
    private static void validateReadableHeader(byte[] family, byte[] style, byte[] version,
                                               byte[] full) throws IOException {
        int header = READER_FIXED_HEADER_BYTES
                + family.length + style.length + version.length + full.length;
        if (header > MAX_READABLE_HEADER_BYTES) {
            throw new IOException("PPTX embedded-font header is " + header
                    + " bytes, past the " + MAX_READABLE_HEADER_BYTES
                    + " a presentation reads: family " + family.length + ", style "
                    + style.length + ", full name " + full.length
                    + " (the full name is the family and the style joined)");
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
