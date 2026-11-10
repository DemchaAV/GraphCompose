package com.demcha.compose.document.backend.fixed.pdf;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Puts text in a page's text layer without painting anything.
 *
 * <p>An inline icon is a drawing, and a drawing has no characters: a colour emoji drawn from
 * SVG paths leaves nothing where it sits for a reader that copies, searches or extracts the page,
 * so a line copied into a messenger arrives without its emoji. {@code ActualText} cannot supply
 * one. It replaces the text of the glyphs it covers, and a drawing covers none; PDFBox, poppler,
 * pdf.js and MuPDF all extract nothing from a {@code Span} with {@code ActualText} around paths.</p>
 *
 * <p>So the text goes in as a glyph of its own, shown over the drawing from a font that paints
 * nothing: a Type 3 font whose glyph procedures are empty, and whose {@code ToUnicode} map states,
 * code by code, the text each glyph stands for &mdash; a whole ZWJ sequence when that is what the
 * icon depicts. The glyph is shown in rendering mode 3, neither filled nor stroked, so a viewer
 * that cannot read the font still paints nothing. An OCR'd scan carries its text layer the same
 * way, as invisible text over the picture; PDFBox, poppler, pdf.js and MuPDF all read this one.</p>
 *
 * <p>One font serves a document, with one code per distinct text; a font has 255 codes, and the
 * text after that starts another. The font's dictionaries grow as codes are handed out, so the
 * file is complete whenever it is saved, however many pages have been drawn.</p>
 */
final class PdfTextLayer {

    /**
     * The longest text one glyph may stand for, in UTF-16 code units. A {@code ToUnicode}
     * destination is limited to 512 bytes; longer text is not written.
     */
    static final int MAX_TEXT_UNITS = 256;

    private final PDDocument document;
    private final Map<String, Glyph> glyphs = new HashMap<>();
    private TextLayerFont font;

    PdfTextLayer(PDDocument document) {
        this.document = document;
    }

    /**
     * Writes {@code text} as one invisible glyph whose advance spans {@code width}.
     *
     * @param stream    page content stream, outside a text object
     * @param text      the text to write; nothing is written when {@code null}, empty or longer
     *                  than {@link #MAX_TEXT_UNITS}
     * @param x         left edge of the glyph in page space
     * @param baselineY baseline the glyph sits on
     * @param width     advance of the glyph in points
     * @param height    font size of the glyph in points
     * @throws IOException if the content stream cannot be written
     */
    void write(PDPageContentStream stream,
               String text,
               float x,
               float baselineY,
               float width,
               float height) throws IOException {
        if (text == null || text.isEmpty() || text.length() > MAX_TEXT_UNITS
                || !(width > 0) || !(height > 0) || Float.isInfinite(width) || Float.isInfinite(height)) {
            return;
        }
        Glyph glyph = glyphs.get(text);
        if (glyph == null) {
            if (font == null || font.isFull()) {
                font = TextLayerFont.create(document);
            }
            glyph = font.add(text);
            glyphs.put(text, glyph);
        }
        // Text state is graphics state and outlives ET, in both directions. Out: without the
        // q/Q every word drawn after the glyph would be invisible too. In: spacing or rise left
        // by the run before (a letter-spaced word keeps its tracking in Tc) would stretch or
        // lift the glyph, so each is set here rather than inherited.
        // Inside a caller's text object PDFBox refuses the q before writing it, so a misplaced
        // call fails with nothing written. Past the q, the text object and the q are closed on
        // every path: a failure half-way must not leave the rest of the page inside them.
        stream.saveGraphicsState();
        try {
            stream.beginText();
            try {
                stream.setFont(glyph.font(), height);
                stream.setRenderingMode(RenderingMode.NEITHER);
                stream.setCharacterSpacing(0);
                stream.setWordSpacing(0);
                stream.setTextRise(0);
                // The glyph is one em wide, and an em is the font size: scale it to the box.
                stream.setHorizontalScaling(100f * width / height);
                stream.newLineAtOffset(x, baselineY);
                stream.showText(glyph.shown());
            } finally {
                stream.endText();
            }
        } finally {
            stream.restoreGraphicsState();
        }
    }

    /** A code of one text-layer font, and the string that shows it. */
    private record Glyph(TextLayerFont font, String shown) {
    }

    /**
     * A Type 3 font of empty glyphs whose codes are handed out one text at a time.
     *
     * <p>PDFBox cannot encode text for a Type 3 font, so {@link #encode(int)} is overridden: a
     * glyph is shown as the private-use character {@code U+E000} plus its code, and encodes to
     * that one byte.</p>
     *
     * <p>Only the dictionary is kept current. What {@code PDType3Font} read from it at
     * construction &mdash; the encoding, the widths, an absent {@code ToUnicode} &mdash; goes
     * stale as codes are added, and nothing on PDFBox 3.0's write path reads it: {@code setFont}
     * and {@code showText} reach only {@code encode} and the embedding flags. A PDFBox upgrade
     * that measured or looked glyphs up while writing would see {@code .notdef} here.</p>
     */
    private static final class TextLayerFont extends PDType3Font {

        private static final int CARRIER = 0xE000;
        private static final int LAST_CODE = 255;
        private static final int GLYPH_WIDTH = 1000;
        /** A CMap block holds at most 100 mappings. */
        private static final int BFCHAR_BLOCK = 100;

        private final COSDictionary charProcs;
        private final COSArray differences;
        private final COSArray widths;
        private final COSStream emptyGlyph;
        private final COSStream toUnicode;
        private final List<String> texts = new ArrayList<>();

        private TextLayerFont(COSDictionary dictionary, COSStream emptyGlyph, COSStream toUnicode)
                throws IOException {
            super(dictionary);
            this.charProcs = dictionary.getCOSDictionary(COSName.CHAR_PROCS);
            this.differences = dictionary.getCOSDictionary(COSName.ENCODING).getCOSArray(COSName.DIFFERENCES);
            this.widths = dictionary.getCOSArray(COSName.WIDTHS);
            this.emptyGlyph = emptyGlyph;
            this.toUnicode = toUnicode;
        }

        static TextLayerFont create(PDDocument document) throws IOException {
            COSStream emptyGlyph = document.getDocument().createCOSStream();
            try (OutputStream out = emptyGlyph.createOutputStream()) {
                // Advance one em, no marks, no bounding box: d0 with nothing after it.
                out.write((GLYPH_WIDTH + " 0 d0\n").getBytes(StandardCharsets.US_ASCII));
            }
            COSStream toUnicode = document.getDocument().createCOSStream();

            COSDictionary dictionary = new COSDictionary();
            dictionary.setItem(COSName.TYPE, COSName.FONT);
            dictionary.setItem(COSName.SUBTYPE, COSName.TYPE3);
            dictionary.setItem(COSName.FONT_BBOX, numbers(0, 0, GLYPH_WIDTH, GLYPH_WIDTH));
            dictionary.setItem(COSName.FONT_MATRIX, numbers(0.001f, 0, 0, 0.001f, 0, 0));
            dictionary.setItem(COSName.CHAR_PROCS, new COSDictionary());
            COSDictionary encoding = new COSDictionary();
            encoding.setItem(COSName.TYPE, COSName.ENCODING);
            COSArray differences = new COSArray();
            differences.add(COSInteger.ONE);
            encoding.setItem(COSName.DIFFERENCES, differences);
            dictionary.setItem(COSName.ENCODING, encoding);
            dictionary.setInt(COSName.FIRST_CHAR, 1);
            dictionary.setInt(COSName.LAST_CHAR, 1);
            dictionary.setItem(COSName.WIDTHS, new COSArray());
            dictionary.setItem(COSName.RESOURCES, new COSDictionary());
            TextLayerFont font = new TextLayerFont(dictionary, emptyGlyph, toUnicode);
            // Attached only now: PDFont's constructor reads ToUnicode, and this stream is still
            // empty until the first code is added.
            dictionary.setItem(COSName.TO_UNICODE, toUnicode);
            return font;
        }

        boolean isFull() {
            return texts.size() == LAST_CODE;
        }

        /** Gives {@code text} the next code, and states it in every dictionary that lists codes. */
        Glyph add(String text) throws IOException {
            texts.add(text);
            int code = texts.size();
            COSName glyphName = COSName.getPDFName("t" + code);
            charProcs.setItem(glyphName, emptyGlyph);
            differences.add(glyphName);
            widths.add(COSInteger.get(GLYPH_WIDTH));
            getCOSObject().setInt(COSName.LAST_CHAR, code);
            writeToUnicode();
            return new Glyph(this, new String(Character.toChars(CARRIER + code)));
        }

        @Override
        protected byte[] encode(int unicode) {
            int code = unicode - CARRIER;
            if (code < 1 || code > texts.size()) {
                throw new IllegalArgumentException("No text-layer glyph for U+" + Integer.toHexString(unicode));
            }
            return new byte[]{(byte) code};
        }

        private void writeToUnicode() throws IOException {
            StringBuilder cmap = new StringBuilder(128 + texts.size() * 24);
            cmap.append("/CIDInit /ProcSet findresource begin\n")
                    .append("12 dict begin\n")
                    .append("begincmap\n")
                    .append("/CIDSystemInfo << /Registry (Adobe) /Ordering (UCS) /Supplement 0 >> def\n")
                    .append("/CMapName /Adobe-Identity-UCS def\n")
                    .append("/CMapType 2 def\n")
                    .append("1 begincodespacerange\n<00> <FF>\nendcodespacerange\n");
            HexFormat hex = HexFormat.of().withUpperCase();
            for (int start = 0; start < texts.size(); start += BFCHAR_BLOCK) {
                int end = Math.min(texts.size(), start + BFCHAR_BLOCK);
                cmap.append(end - start).append(" beginbfchar\n");
                for (int i = start; i < end; i++) {
                    cmap.append('<').append(hex.toHexDigits((byte) (i + 1))).append("> <")
                            .append(hex.formatHex(texts.get(i).getBytes(StandardCharsets.UTF_16BE)))
                            .append(">\n");
                }
                cmap.append("endbfchar\n");
            }
            cmap.append("endcmap\n")
                    .append("CMapName currentdict /CMap defineresource pop\n")
                    .append("end\n")
                    .append("end\n");
            try (OutputStream out = toUnicode.createOutputStream(COSName.FLATE_DECODE)) {
                out.write(cmap.toString().getBytes(StandardCharsets.US_ASCII));
            }
        }

        private static COSArray numbers(float... values) {
            COSArray array = new COSArray();
            for (float value : values) {
                array.add(value == (int) value ? COSInteger.get((int) value) : new COSFloat(value));
            }
            return array;
        }
    }
}
