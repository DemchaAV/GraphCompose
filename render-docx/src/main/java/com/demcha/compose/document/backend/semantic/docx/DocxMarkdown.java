package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphSpan;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.node.ListItem;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.engine.components.content.text.TextDataBody;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.engine.text.TextControlSanitizer;
import com.demcha.compose.engine.text.markdown.MarkDownParser;
import com.demcha.compose.font.FontName;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A paragraph's or a list item's text as the page sets it where its session reads markdown: the
 * pieces its marks style, each in its face and size, the marks the parser reads dropped.
 *
 * <p>The page reads the text line by line, each through {@link MarkDownParser}, a line opening
 * with {@code -}, {@code *} or {@code +} and a space keeping that marker and the space in the
 * paragraph's style ({@code ParagraphWrapping.tokenizeMarkdownLine}). The parser sets every piece
 * it reads in a face of its own — bold, italic, both or neither, whatever the paragraph's style
 * — and a heading's at a multiple of the size. The pieces are read here the same way
 * ({@link #read}), and are the text the page sets only where its laid-out lines hold the same
 * letters in the same faces, families, colours, tracking and sizes ({@link #laidOutIn}): a session that
 * reads no markdown lays the marks out, and the pieces hold none.</p>
 */
final class DocxMarkdown {

    /**
     * A style the parser is handed to read a line in: at a size of one, so the size it sets a
     * piece in is the multiple of the paragraph's.
     */
    private static final TextStyle UNIT = new TextStyle(FontName.HELVETICA, 1, TextDecoration.DEFAULT, Color.BLACK);

    /** How far apart two sizes may be and still be the same, in points. */
    private static final double SIZE_CLEARANCE = 0.01;

    /**
     * The indent a list with no {@code hangingIndent} sets an item of a tree of items in, a level
     * at a time: two no-break spaces ({@code TextFlowSupport}, where it flattens the tree into
     * labels), which the parser reads as letters.
     */
    private static final String NESTED_ITEM_INDENT = Character.toString(0x00A0).repeat(2);

    private DocxMarkdown() {
    }

    /**
     * Whether text holds a mark the page reads markdown on: emphasis or code
     * ({@code ParagraphWrapping.containsMarkdownSyntax}).
     */
    static boolean holdsAMark(String text) {
        return text != null && (text.indexOf('*') >= 0 || text.indexOf('_') >= 0 || text.indexOf('`') >= 0);
    }

    /**
     * Whether the page may read a paragraph as markdown, where its session asks it to: plain text,
     * with no runs, holding a mark it reads markdown on ({@link #holdsAMark}). Any other paragraph
     * is laid out with every mark it holds.
     */
    static boolean mayRead(ParagraphNode node) {
        return (node.inlineRuns() == null || node.inlineRuns().isEmpty()) && holdsAMark(node.text());
    }

    /**
     * A list item's text as the page lays it out, and reads it where its session reads markdown.
     *
     * @param text   the text the page lays the item out in and reads
     * @param lead   the characters it opens with that the file writes apart from the item's own
     *               text: the indent and marker a list of a tree of items with no
     *               {@code hangingIndent} lays out in its text; empty for none
     * @param prefix the prefix the page sets before its first line, apart from its text: a flat
     *               list's marker, where it has no {@code hangingIndent}; empty for none
     */
    record ItemReading(String text, String lead, String prefix) {
    }

    /**
     * A flat list's item as the page reads it: its text, a marker typed before it taken off, after
     * the list's marker where the page sets that before its first line.
     *
     * @param normalized the item's text, a typed marker taken off
     */
    static ItemReading flatItem(ListNode list, String normalized) {
        return new ItemReading(normalized, "",
                !list.hangingIndent() && list.marker().isVisible() ? list.marker().prefix() : "");
    }

    /**
     * An item of a tree of items as the page reads it: with {@code hangingIndent}, its label, a
     * marker typed before it taken off; without it, its label after its depth's indent and its
     * marker, as the page flattens the tree into labels, nothing taken off.
     *
     * @param marker the marker the item takes, its own or its depth's
     * @return its reading, {@code null} for an item of runs, which the page never reads
     */
    static ItemReading nestedItem(ListNode list, ListItem item, int depth, ListMarker marker) {
        if (item.isRich()) {
            return null;
        }
        if (list.hangingIndent()) {
            return new ItemReading(ListMarker.normalizeItemText(item.label(), list.normalizeMarkers()), "", "");
        }
        String lead = NESTED_ITEM_INDENT.repeat(depth) + (marker.isVisible() ? marker.prefix() : "");
        return new ItemReading(lead + item.label(), lead, "");
    }

    /**
     * Every item of plain text a list writes, as the page reads it: its flat items, then its tree
     * of items depth first, each taking its own marker or its depth's.
     */
    static List<ItemReading> items(ListNode list) {
        List<ItemReading> readings = new ArrayList<>();
        for (String item : list.items()) {
            String normalized = ListMarker.normalizeItemText(item, list.normalizeMarkers());
            if (!normalized.isBlank()) {
                readings.add(flatItem(list, normalized));
            }
        }
        addItems(list, list.nestedItems(), 0, readings);
        return List.copyOf(readings);
    }

    private static void addItems(ListNode list, List<ListItem> items, int depth, List<ItemReading> into) {
        for (ListItem item : items) {
            ItemReading reading = nestedItem(list, item,
                    depth, item.marker() != null ? item.marker() : ListMarker.defaultForDepth(depth));
            if (reading != null) {
                into.add(reading);
            }
            addItems(list, item.children(), depth + 1, into);
        }
    }

    /**
     * One piece of text the page sets in one style.
     *
     * @param text  its text, a {@code "\n"} where the page starts a line
     * @param style its style
     */
    record Piece(String text, DocumentTextStyle style) {
    }

    /**
     * Reads text as the page does where its session reads markdown.
     *
     * @param text  the paragraph's text
     * @param style the paragraph's style
     * @return its pieces in order, those of one style side by side joined, a {@code "\n"} in the
     *         paragraph's style between its lines; none where the parser keeps no text, as of a
     *         line of marks alone
     */
    static List<Piece> read(String text, DocumentTextStyle style) {
        List<Piece> pieces = new ArrayList<>();
        MarkDownParser parser = new MarkDownParser();
        String[] lines = (text == null ? "" : text).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                add(pieces, "\n", style);
            }
            readLine(TextControlSanitizer.removeExceptFormattingControls(lines[index]), style, parser, pieces);
        }
        return List.copyOf(pieces);
    }

    private static void readLine(String line, DocumentTextStyle style, MarkDownParser parser, List<Piece> into) {
        if (line.isBlank()) {
            return;
        }
        int first = 0;
        while (first < line.length() && Character.isWhitespace(line.charAt(first))) {
            first++;
        }
        String rest = line;
        if (first + 1 < line.length()) {
            char marker = line.charAt(first);
            if ((marker == '-' || marker == '*' || marker == '+') && Character.isWhitespace(line.charAt(first + 1))) {
                add(into, line.substring(0, first) + marker + " ", style);
                rest = line.substring(first + 2);
            }
        }
        for (TextDataBody body : parser.getBody(rest, UNIT)) {
            add(into, body.text(), styled(style, body.textStyle()));
        }
    }

    /** The paragraph's style in the face the parser set a piece in, at the multiple of its size. */
    private static DocumentTextStyle styled(DocumentTextStyle style, TextStyle parsed) {
        double scale = parsed.size();
        DocumentLetterSpacing spacing = style.letterSpacing();
        // The page carries the paragraph's tracking, in points, to a heading's larger size.
        if (scale != 1 && spacing != null && !spacing.isNone()) {
            spacing = DocumentLetterSpacing.points(spacing.resolve(style.size()));
        }
        return new DocumentTextStyle(style.fontName(), style.size() * scale, documentFace(parsed.decoration()),
                style.color(), spacing);
    }

    /** A face the page names, as the document names it: the two share their names. */
    private static DocumentTextDecoration documentFace(TextDecoration decoration) {
        return decoration == null ? DocumentTextDecoration.DEFAULT : DocumentTextDecoration.valueOf(decoration.name());
    }

    /** A face the document names, as the page names it. */
    private static TextDecoration pageFace(DocumentTextDecoration decoration) {
        return decoration == null ? TextDecoration.DEFAULT : TextDecoration.valueOf(decoration.name());
    }

    private static void add(List<Piece> pieces, String text, DocumentTextStyle style) {
        if (text.isEmpty()) {
            return;
        }
        int last = pieces.size() - 1;
        if (last >= 0 && pieces.get(last).style().equals(style)) {
            pieces.set(last, new Piece(pieces.get(last).text() + text, style));
        } else {
            pieces.add(new Piece(text, style));
        }
    }

    /**
     * Pieces read off text that opens with a lead the file writes apart from them, split at the
     * lead's end.
     *
     * @param leadStyle the style the page sets the lead in, {@code null} where there is no lead
     * @param after     the pieces after the lead
     */
    record Split(DocumentTextStyle leadStyle, List<Piece> after) {
    }

    /**
     * Splits pieces read off text that opens with a lead — a nested item's indent and marker, which
     * a list with no {@code hangingIndent} lays out in the item's text — at the lead's end.
     *
     * @param pieces the pieces read off the text
     * @param lead   the characters the text opens with, empty for none
     * @return the split, or {@code null} where the pieces do not open with the lead's characters,
     *         every one in one style, or hold nothing after it
     */
    static Split split(List<Piece> pieces, String lead) {
        if (lead.isEmpty()) {
            return new Split(null, pieces);
        }
        DocumentTextStyle style = null;
        List<Piece> after = new ArrayList<>();
        int taken = 0;
        int index = 0;
        for (; index < pieces.size() && taken < lead.length(); index++) {
            Piece piece = pieces.get(index);
            if (style != null && !piece.style().equals(style)) {
                return null;
            }
            style = piece.style();
            String left = lead.substring(taken);
            if (piece.text().length() <= left.length()) {
                if (!left.startsWith(piece.text())) {
                    return null;
                }
                taken += piece.text().length();
            } else {
                if (!piece.text().startsWith(left)) {
                    return null;
                }
                after.add(new Piece(piece.text().substring(left.length()), piece.style()));
                taken = lead.length();
            }
        }
        if (taken < lead.length()) {
            return null;
        }
        after.addAll(pieces.subList(index, pieces.size()));
        return after.isEmpty() ? null : new Split(style, List.copyOf(after));
    }

    /** The pieces' text, as Word's paragraph holds it. */
    static String text(List<Piece> pieces) {
        StringBuilder text = new StringBuilder();
        for (Piece piece : pieces) {
            text.append(piece.text());
        }
        return text.toString();
    }

    /**
     * Whether the page laid the pieces out in its lines: the lines' letters, less a prefix's
     * leading them, are the pieces' letters, each in the same face, family, colour and tracking,
     * and at the same size, to {@link #SIZE_CLEARANCE} — or, where the page fits the text to a
     * size of its own, at sizes in the same proportion, its tracking, resolved at that size, not
     * compared. Pieces of no letter
     * — text the parser reads into nothing, which the page sets as nothing — are not taken for the
     * page's: written, the paragraph would be blank, and a blank paragraph is what the export
     * writes elsewhere for no line at all. White space is not compared: the page drops it where it
     * breaks a line.
     *
     * @param pieces the pieces read off the text
     * @param lines  the lines the page laid the text out in; none never holds the pieces
     * @param prefix the prefix the page sets before the first line, empty where it sets none
     * @param fitted whether the page fits the text to a size of its own, an auto-sized paragraph's
     */
    static boolean laidOutIn(List<Piece> pieces, List<ParagraphLine> lines, String prefix, boolean fitted) {
        return !Double.isNaN(laidOutAt(pieces, lines, prefix, fitted));
    }

    /**
     * How many times the size it was read at the page sets text read into pieces, where it fits
     * the text to a size of its own: the share its first letter is set at, where the page laid the
     * pieces out at sizes in that proportion ({@link #laidOutIn}); {@code NaN} where it did not.
     * Pieces read at the paragraph's style's size and taken by this share are the size the page
     * fits the paragraph's text to, a heading's a multiple of it.
     *
     * @param pieces the pieces read off the text
     * @param lines  the lines the page laid the text out in
     * @param prefix the prefix the page sets before the first line, empty where it sets none
     */
    static double scaleIn(List<Piece> pieces, List<ParagraphLine> lines, String prefix) {
        return laidOutAt(pieces, lines, prefix, true);
    }

    /** The share {@link #laidOutIn} finds the pieces set at, 1 unless fitted; {@code NaN} where it finds them not. */
    private static double laidOutAt(List<Piece> pieces, List<ParagraphLine> lines, String prefix, boolean fitted) {
        if (lines.isEmpty()) {
            return Double.NaN;
        }
        List<Letter> written = new ArrayList<>();
        for (Piece piece : pieces) {
            DocumentTextStyle style = piece.style();
            double tracking = style.letterSpacing() == null ? 0 : style.letterSpacing().resolve(style.size());
            lettersOf(piece.text(), pageFace(style.decoration()), style.size(), tracking, style.fontName(),
                    style.color() == null ? null : style.color().color(), written);
        }
        List<Letter> laid = new ArrayList<>();
        for (ParagraphLine line : lines) {
            for (ParagraphSpan span : line.spans()) {
                if (!(span instanceof ParagraphTextSpan text) || text.textStyle() == null) {
                    return Double.NaN;
                }
                TextStyle style = text.textStyle();
                lettersOf(text.text(), style.decoration(), style.size(), style.letterSpacing(), style.fontName(),
                        style.color(), laid);
            }
        }
        List<Letter> leading = new ArrayList<>();
        lettersOf(prefix, null, 0, 0, null, null, leading);
        if (written.isEmpty() || laid.size() != leading.size() + written.size()) {
            return Double.NaN;
        }
        for (int index = 0; index < leading.size(); index++) {
            if (laid.get(index).codePoint() != leading.get(index).codePoint()) {
                return Double.NaN;
            }
        }
        double ratio = fitted ? laid.get(leading.size()).size() / written.get(0).size() : 1;
        for (int index = 0; index < written.size(); index++) {
            Letter page = laid.get(leading.size() + index);
            Letter file = written.get(index);
            if (page.codePoint() != file.codePoint() || page.face() != file.face()
                || !Objects.equals(page.family(), file.family()) || page.argb() != file.argb()
                || Math.abs(page.size() - file.size() * ratio) > SIZE_CLEARANCE
                || !fitted && Math.abs(page.tracking() - file.tracking()) > SIZE_CLEARANCE) {
                return Double.NaN;
            }
        }
        return ratio;
    }

    /** One letter as the page or the file sets it: its tracking in points, its colour as packed ARGB. */
    private record Letter(int codePoint, TextDecoration face, double size, double tracking, FontName family, int argb) {
    }

    private static void lettersOf(String text, TextDecoration face, double size, double tracking, FontName family,
                                  Color color, List<Letter> into) {
        if (text == null) {
            return;
        }
        int argb = color == null ? 0 : color.getRGB();
        text.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .forEach(codePoint -> into.add(new Letter(codePoint, face, size, tracking, family, argb)));
    }
}
