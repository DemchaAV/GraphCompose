package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageNumberStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A text header or footer — three slots and their page tokens — as a line of a Word header or
 * footer.
 *
 * <p>The PDF paints the band after the page is laid out: its left slot at the left margin, its
 * centre slot in the middle of the margins, its right slot against the right one, with
 * {@code {page}} and {@code {pages}} resolved per page. Word lays out a header or footer for
 * itself, so the band is written once as one paragraph: the slots are runs set by a centre and a
 * right tab stop, and the page tokens are Word's own fields, counted on every page Word makes.
 * This class holds the pieces that do not touch a document: the band's text split into text and
 * tokens, where the paragraph stands, and how its separator is drawn.</p>
 */
final class DocxTextBands {

    /**
     * The height of the band's line against its type size. An exact line leaves the editor no
     * room to grow it, so the paragraph is as tall as a line of text set solid with some air.
     */
    static final double LINE_FACTOR = 1.2;

    /**
     * Where both editors stand the baseline in an exact line, as a share of the line from its
     * top. Measured, it is four fifths of the line whatever the face and size: Spectral, Lato
     * and Arial at 10 to 46pt, in lines 12 to 100pt tall, within 0.1pt of it in Word and on it in
     * LibreOffice; and again on the fifteen faces the templates use and JetBrains Mono at 8 to
     * 36pt, within 0.04pt of it in Word in median, and in the lines of the DOCX fidelity corpus,
     * read off Word's own line tops. A band's line is placed by it here, and a paragraph's text is
     * moved from it to the page's baseline ({@code DocxSemanticBackend#shiftToThePagesBaseline}).
     */
    static final double BASELINE_SHARE = 0.8;

    private static final Pattern TOKEN = Pattern.compile("\\{(page|pages|date)}");

    private DocxTextBands() {
    }

    /** What a piece of a slot's text is. */
    enum Kind {
        TEXT,
        PAGE,
        PAGES,
        DATE
    }

    /**
     * One piece of a slot's text: literal text, or a token.
     *
     * @param kind what it is
     * @param text the literal text, empty for a token
     */
    record Segment(Kind kind, String text) {
    }

    /**
     * A slot's text as literal text and tokens, in order; empty for a slot with no text.
     *
     * @param text the slot's text, possibly {@code null}
     */
    static List<Segment> segments(String text) {
        List<Segment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }
        Matcher matcher = TOKEN.matcher(text);
        int from = 0;
        while (matcher.find()) {
            if (matcher.start() > from) {
                segments.add(new Segment(Kind.TEXT, text.substring(from, matcher.start())));
            }
            Kind kind = switch (matcher.group(1)) {
                case "page" -> Kind.PAGE;
                case "pages" -> Kind.PAGES;
                default -> Kind.DATE;
            };
            segments.add(new Segment(kind, ""));
            from = matcher.end();
        }
        if (from < text.length()) {
            segments.add(new Segment(Kind.TEXT, text.substring(from)));
        }
        return segments;
    }

    /** The band's line height, in points. */
    static double lineHeight(DocumentHeaderFooter band) {
        return band.getFontSize() * LINE_FACTOR;
    }

    /**
     * How far from its page edge the band's paragraph starts, in points: from the top of the
     * page to the top of a header's line, or from the bottom of the page to the bottom of a
     * footer's, which is what Word measures the header and footer distance to.
     *
     * <p>The PDF sets a header's baseline {@code height - fontSize / 2} below the top of the
     * page, and a footer's {@code height - fontSize} above its foot.</p>
     */
    static double distanceFromEdge(DocumentHeaderFooter band) {
        double line = lineHeight(band);
        if (band.getZone() == DocumentHeaderFooterZone.HEADER) {
            return distanceFromEdge(true, band.getHeight() - band.getFontSize() / 2.0, line);
        }
        return distanceFromEdge(false, band.getHeight() - band.getFontSize(), line);
    }

    /**
     * How far from its page edge an exact line stands to set its baseline where the page has
     * it, in points: from the top of the page to the top of a header's line, or from the bottom
     * of the page to the bottom of a footer's. A line that would stand past the edge stops at
     * it, its baseline as far in as that leaves.
     *
     * @param header           whether the line is a header's
     * @param baselineFromEdge how far the page sets the baseline from the edge: down from the
     *                         page's top for a header, up from its foot for a footer
     * @param line             the exact line's height
     */
    static double distanceFromEdge(boolean header, double baselineFromEdge, double line) {
        return Math.max(0, baselineFromEdge - line * (header ? BASELINE_SHARE : 1 - BASELINE_SHARE));
    }

    /**
     * The space between the band's line and its separator, in points: the PDF strokes the
     * separator {@code height} from the page edge, below a header's text and above a footer's.
     */
    static double separatorSpace(DocumentHeaderFooter band) {
        return Math.max(0, band.getHeight() - distanceFromEdge(band) - lineHeight(band));
    }

    /**
     * The switch that numbers a page field in a style, or an empty string for decimal.
     *
     * @param style the band's numbering style
     */
    static String numberFormat(DocumentPageNumberStyle style) {
        if (style == null) {
            return "";
        }
        return switch (style) {
            case LOWER_ROMAN -> " \\* roman";
            case UPPER_ROMAN -> " \\* ROMAN";
            case LOWER_ALPHA -> " \\* alphabetic";
            case UPPER_ALPHA -> " \\* ALPHABETIC";
            case DECIMAL -> "";
        };
    }
}
