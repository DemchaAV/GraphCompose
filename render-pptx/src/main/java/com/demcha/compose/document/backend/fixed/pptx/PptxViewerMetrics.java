package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.font.FontFamilyDefinition;
import org.apache.fontbox.ttf.FontHeaders;
import org.apache.fontbox.ttf.OS2WindowsMetricsTable;
import org.apache.fontbox.ttf.OTFParser;
import org.apache.pdfbox.io.RandomAccessReadBuffer;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Where PowerPoint puts a top-anchored frame's first baseline, so the backend can place each
 * frame for its text to stand on the line the engine measured.
 *
 * <p>Measured in PowerPoint 16 (its PDF export, and its own raster of the slide, which agree
 * to the pixel), across the 35 bundled families, Arial, Times New Roman and Courier New, and
 * faces that differ from one another in one font field at a time: the first baseline stands
 * {@link #seatRatio} em below the frame top, a function of one ratio of the face's vertical
 * metrics and nothing else. The ratio is read from the OS/2 typographic ascender, descender
 * and line gap when the face sets {@code USE_TYPO_METRICS}, from the Windows ascent and
 * descent when it does not; neither the {@code hhea} values nor the glyph bounds move it, and
 * neither does how tall the metrics are, only how they divide. Single spacing is a fixed
 * 1.2 em in the same measurement, whatever the face. That rule seats every measured line
 * within 0.11pt from 10pt to 80pt, where the program's own ascent, which this read before,
 * was up to a third of the em off.</p>
 *
 * <p>LibreOffice Impress (26.8 on Windows) seats the first baseline a whole em below the
 * frame top for every face, so it draws a deck's text where neither rule puts it.</p>
 */
final class PptxViewerMetrics {

    private static final FontRenderContext FONT_RENDER_CONTEXT =
            new FontRenderContext(null, true, true);
    private static final float FONT_METRICS_SIZE = 1000.0f;

    /** PowerPoint's single line spacing, in em, whatever the face. */
    private static final double LINE_PITCH = 1.2;

    /**
     * The least share of a face's height PowerPoint gives the part above the baseline; the
     * part below gets at least the rest. Fitted to the measurements: 0.7811, the worst line
     * 0.10pt off; 25/32 puts it at 0.11pt.
     */
    private static final double ASCENT_SHARE = 25.0 / 32.0;

    /** The {@code fsSelection} bit that tells a reader to take the OS/2 typographic metrics. */
    private static final int USE_TYPO_METRICS = 1 << 7;

    /** Arial's Windows ascent and descent, units of 2048: what a Helvetica run is drawn with. */
    private static final double ARIAL_SEAT = seatRatio(1854, 434);
    /** Times New Roman's, for a Times run. */
    private static final double TIMES_NEW_ROMAN_SEAT = seatRatio(1825, 443);
    /** Courier New's, for a Courier run. */
    private static final double COURIER_NEW_SEAT = seatRatio(1705, 615);

    private PptxViewerMetrics() {
    }

    record ViewerFontMetrics(double regularSeat,
                             double boldSeat,
                             double italicSeat,
                             double boldItalicSeat) {
        double seat(boolean bold, boolean italic) {
            if (bold && italic) {
                return boldItalicSeat;
            }
            if (bold) {
                return boldSeat;
            }
            if (italic) {
                return italicSeat;
            }
            return regularSeat;
        }
    }

    /**
     * How far below a top-anchored frame's top PowerPoint stands its first baseline, in em,
     * for a face whose metrics divide into this ascent and descent.
     *
     * <p>PowerPoint raises the share of the face's height above the baseline to at least
     * {@link #ASCENT_SHARE}, or the share below it to at least the rest, whichever the face
     * falls short of, and gives the first line's 1.2 em the same proportion. A face whose
     * ascent is exactly that share of its height has its baseline at 1.2 × 25/32 em.</p>
     *
     * @param ascent  the ascent, with the typographic line gap added when that is what is read,
     *                in any unit
     * @param descent the descent as a positive distance, in the same unit
     * @return the distance from the frame's top to the first baseline, in em
     */
    static double seatRatio(double ascent, double descent) {
        double above = Math.max(0, ascent);
        double below = Math.abs(descent);
        double height = above + below;
        if (!(height > 0)) {
            return LINE_PITCH * ASCENT_SHARE;
        }
        double seatedAbove = Math.max(above, ASCENT_SHARE * height);
        double seatedBelow = Math.max(below, (1 - ASCENT_SHARE) * height);
        return LINE_PITCH * seatedAbove / (seatedAbove + seatedBelow);
    }

    /**
     * Resolves where the first baseline stands below the frame top for one style, in points —
     * the single seat formula shared by the render environment and the geometry test mirror,
     * so the two cannot drift line-by-line.
     *
     * <p>The face is <em>given</em> rather than read off the style here, because which face a run
     * asks for is not the style's alone to say: it is the span's decoration plus what the family's
     * registration leaves for its name to add, which only the environment knows
     * ({@code PptxRenderEnvironment.runIsBold}). Passing it keeps this the one seat formula while
     * the answer is worked out once, where the faces of the family are known; a caller that cannot
     * know — the geometry mirror in the tests — passes the flags it expects on the run it is
     * checking, which for a fixture whose name carries no style suffix is what production
     * writes.</p>
     *
     * <p>Without the family's own metrics — a bundled family, which the deck carries only after
     * the slides are drawn, or one the deck does not carry — a standard-14 name is seated from
     * the metric-compatible face PowerPoint draws it with, and any other from the program the
     * page measured it with, or from the PDF descriptor's ascent and descent where that is not
     * a TrueType program or its OS/2 table gives the face no height. That route seats the face
     * the page measured the style with, read off the style's decoration, and not the flags:
     * without the family's own faces there is no other face to seat.</p>
     *
     * @param style  the span's style
     * @param font   the PDF font the engine measured the span with
     * @param custom the family's own viewer metrics, or {@code null} when the deck does not
     *               carry the family
     * @param bold   whether the run is written bold
     * @param italic whether the run is written italic
     * @return the distance from the frame top to the first baseline, in points
     */
    static double seatPoints(com.demcha.compose.engine.components.content.text.TextStyle style,
                             com.demcha.compose.engine.render.pdf.PdfFont font,
                             ViewerFontMetrics custom,
                             boolean bold,
                             boolean italic) {
        double ratio = custom != null ? custom.seat(bold, italic) : uncarriedSeat(style, font);
        return ratio * style.size();
    }

    private static double uncarriedSeat(com.demcha.compose.engine.components.content.text.TextStyle style,
                                        com.demcha.compose.engine.render.pdf.PdfFont font) {
        String replacement = com.demcha.compose.document.backend.fixed.pptx.handlers.PptxFontMapping
                .standardReplacementFor(style.fontName());
        if ("Arial".equals(replacement)) {
            return ARIAL_SEAT;
        }
        if ("Times New Roman".equals(replacement)) {
            return TIMES_NEW_ROMAN_SEAT;
        }
        if ("Courier New".equals(replacement)) {
            return COURIER_NEW_SEAT;
        }
        OS2WindowsMetricsTable os2 = measuredOs2(font, style);
        double fromProgram = os2 == null ? Double.NaN : os2Seat(os2);
        if (!Double.isNaN(fromProgram)) {
            return fromProgram;
        }
        com.demcha.compose.engine.render.pdf.PdfFont.VerticalMetrics descriptor =
                font.verticalMetrics(style);
        return seatRatio(descriptor.ascent(), descriptor.descent());
    }

    /**
     * The OS/2 table of the TrueType program the page measured this style with, or
     * {@code null} where the page font is not one or the table will not read. A bundled family
     * reaches here: the deck carries it only after the last slide is drawn, so it has no
     * metrics of its own read for it, and the program it carries is this one.
     */
    private static OS2WindowsMetricsTable measuredOs2(
            com.demcha.compose.engine.render.pdf.PdfFont font,
            com.demcha.compose.engine.components.content.text.TextStyle style) {
        try {
            org.apache.pdfbox.pdmodel.font.PDFont face = font.fontType(style.decoration());
            org.apache.fontbox.ttf.TrueTypeFont program = null;
            if (face instanceof org.apache.pdfbox.pdmodel.font.PDType0Font composite
                    && composite.getDescendantFont()
                    instanceof org.apache.pdfbox.pdmodel.font.PDCIDFontType2 descendant) {
                program = descendant.getTrueTypeFont();
            } else if (face instanceof org.apache.pdfbox.pdmodel.font.PDTrueTypeFont simple) {
                program = simple.getTrueTypeFont();
            }
            return program == null ? null : program.getOS2Windows();
        } catch (IOException | RuntimeException unreadable) {
            return null;
        }
    }

    /**
     * The seat by the OS/2 fields PowerPoint reads, or {@code NaN} where they give the face no
     * height — an early table too short to hold them reads back as zeros — so the caller falls
     * back to the metrics it has instead.
     *
     * <p>{@code USE_TYPO_METRICS} is taken whatever the table's version, although the bit is
     * reserved below version 4. No such table was measured, and none of the repository's fonts
     * sets the bit below version 4.</p>
     */
    static double os2Seat(OS2WindowsMetricsTable os2) {
        double ascent;
        double descent;
        if ((os2.getFsSelection() & USE_TYPO_METRICS) != 0) {
            ascent = os2.getTypoAscender() + os2.getTypoLineGap();
            descent = os2.getTypoDescender();
        } else {
            ascent = os2.getWinAscent();
            descent = os2.getWinDescent();
        }
        if (!(Math.max(0, ascent) + Math.abs(descent) > 0)) {
            return Double.NaN;
        }
        return seatRatio(ascent, descent);
    }

    /**
     * A face this reader cannot read, named so the caller can report it.
     *
     * <p>An {@link IOException}, like the refusals {@link PptxEmbeddedFont#wrap}
     * raises, because it means the same thing to the caller: a face whose program
     * could not be read, which costs the deck the font rather than the render.</p>
     */
    static final class UnreadableFaceException extends IOException {

        private static final long serialVersionUID = 1L;

        private final String source;

        private UnreadableFaceException(String family, String source, Throwable cause) {
            super("Unable to read PPTX font metrics for " + family + " from " + source, cause);
            this.source = source;
        }

        /** The face's source, as it describes itself. */
        String source() {
            return source;
        }
    }

    /**
     * Reads where PowerPoint seats the first baseline for every face of a family.
     *
     * <p>The seat itself comes from the face's OS/2 table, read through fontbox's header
     * parse ({@link #os2Seat}), or from the program's own ascent and descent as Java reads
     * them where that table is missing or gives the face no height. What decides whether the face can be read at all is an
     * independent read of the same font programs the embedder takes, through
     * {@link Font#createFont} rather than fontbox, and the two do not accept the same
     * files. They disagree both ways. fontbox's header parse skips a table whose
     * directory entry runs past the end of the file and reads on, while
     * {@code createFont} rejects the font outright for such an entry — a font truncated
     * below its last table used to embed on the first read and then have no readable
     * metrics, which cost the whole deck. The other way round, {@code createFont} reads
     * no name record over 256 bytes while fontbox has no opinion about a name's length,
     * so a face whose family name runs from 129 characters to the limit
     * {@link PptxEmbeddedFont} enforces is embeddable and has no readable metrics. Either
     * way this throws a checked refusal the caller degrades the family on, and the caller
     * reads the metrics <em>before</em> handing any program to the presentation.</p>
     *
     * <p>What neither disagreement covers is the name a record does not carry: the
     * container's full name is joined from the family and the style, and
     * {@code createFont} never reads the style record at all. That one is
     * {@link PptxEmbeddedFont}'s to catch.</p>
     *
     * <p>Each program is read once, however many slots it fills: a family registered from one
     * file repeats that file in all four slots. Keyed by
     * {@link FontFamilyDefinition.FontBinarySource#description()}, as the program reading beside
     * this one keys its programs, so the two reads agree on which slots share a program.</p>
     *
     * @throws UnreadableFaceException naming the first face that cannot be read
     */
    static ViewerFontMetrics load(FontFamilyDefinition family,
                                  FontFamilyDefinition.FontSourceSet sources)
            throws UnreadableFaceException {
        Map<String, Double> read = new HashMap<>(4);
        return new ViewerFontMetrics(
                readSeat(family, sources.regular(), read),
                readSeat(family, sources.bold(), read),
                readSeat(family, sources.italic(), read),
                readSeat(family, sources.boldItalic(), read));
    }

    private static double readSeat(FontFamilyDefinition family,
                                    FontFamilyDefinition.FontBinarySource source,
                                    Map<String, Double> read)
            throws UnreadableFaceException {
        // Named inside the same try, so that asking the source what it is cannot throw out
        // of the failure path itself.
        String description = "<unnamed>";
        try {
            description = source.description();
            Double known = read.get(description);
            if (known != null) {
                return known;
            }
            byte[] program;
            try (InputStream stream = source.openStream()) {
                program = stream.readAllBytes();
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(program))
                    .deriveFont(FONT_METRICS_SIZE);
            OS2WindowsMetricsTable os2 = os2Of(program);
            double ratio = os2 == null ? Double.NaN : os2Seat(os2);
            if (Double.isNaN(ratio)) {
                // No OS/2 table to read, none fontbox will hand over, or one whose fields give
                // the face no height: what is left is the division of the program's own ascent
                // and descent, as Java reads them.
                LineMetrics metrics = font.getLineMetrics("Hg", FONT_RENDER_CONTEXT);
                ratio = seatRatio(metrics.getAscent(), metrics.getDescent());
            }
            read.put(description, ratio);
            return ratio;
        } catch (IOException | FontFormatException | RuntimeException exception) {
            // Unchecked as well as checked, deliberately and without a case of its own:
            // a refusal from this reader is to cost the deck the font, and the one thing it
            // must not cost is the export. The only refusal seen from it is
            // FontFormatException — 1500 damaged copies of a bundled face raised nothing
            // else — so no test reaches the other arms; they are here because the failure
            // this catch is closing is "the export is lost", not "the font is unusual".
            throw new UnreadableFaceException(family.name().name(), description, exception);
        }
    }

    /**
     * The face's OS/2 table, or {@code null} when it has none or fontbox's header parse will
     * not give it. Not a refusal: whether the face is readable is {@link Font#createFont}'s to
     * say, as it was before this table was read, and the embedder has parsed the same headers
     * already on its way to this read.
     */
    private static OS2WindowsMetricsTable os2Of(byte[] program) {
        try (RandomAccessReadBuffer input = new RandomAccessReadBuffer(program)) {
            FontHeaders headers = new OTFParser().parseTableHeaders(input);
            return headers.getError() != null ? null : headers.getOS2Windows();
        } catch (IOException | RuntimeException unreadable) {
            return null;
        }
    }
}
