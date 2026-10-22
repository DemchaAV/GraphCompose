package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.AlignNode;
import com.demcha.compose.document.node.BarcodeNode;
import com.demcha.compose.document.node.CanvasLayerNode;
import com.demcha.compose.document.node.ChartNode;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.EllipseNode;
import com.demcha.compose.document.node.ImageNode;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.LineNode;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.node.PageFieldNode;
import com.demcha.compose.document.node.PageReferenceNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.PathNode;
import com.demcha.compose.document.node.PolygonNode;
import com.demcha.compose.document.node.RowNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.ShapeContainerNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.node.TableNode;
import com.demcha.compose.document.output.DocumentOutputOptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the DOCX export does with every field of every node, and with every output option:
 * writes it, reports it when the author set it, or has nothing for Word to carry. A field the
 * export loses in a case its report does not name yet is a gap, and its entry says which case: a
 * known defect, held here so the ledger stays true while it is open. A field the export neither
 * writes nor names, with no gap to say where, is not one this ledger can hold.
 *
 * <p>The export's report promises a caller that what the file does not carry is said, not
 * approximated in silence. That promise is only as good as the fields someone thought about:
 * a field added to a node later is written by the PDF backend at once and, until the DOCX export
 * decides, lost from the Word file without a word. This ledger fails on any node kind or field
 * it has no entry for, so the decision is made when the field is added, not when a reader finds
 * the loss.</p>
 *
 * <p>A node's entries are about the body. A page zone is written as one line of its
 * paragraphs' runs, page fields and tabs, which loses more of a paragraph and a row than the
 * body does: where its parts stand and what its paragraphs lose of their own is reported, once,
 * as the {@code zones} output option's entry.</p>
 *
 * <p>The entries are claims about the export, not proofs of it: a {@code WRITTEN} field is
 * proved by the export's own tests, a {@code REPORTED} one by a report test. A field added later
 * takes a fate in the change that adds it. The values a field holds have their own entries, in
 * {@link DocxValueFieldLedgerTest}, which shares the gaps' notes below.</p>
 */
class DocxNodeFieldLedgerTest {

    enum Fate {
        /** Reaches the Word file, directly or through the geometry the layout resolved. */
        WRITTEN,
        /**
         * Not in the Word file in some or all cases, and named in the export's report there when
         * the author set it; the entry says which, where it is not every case.
         */
        REPORTED,
        /**
         * Nothing for a Word file to carry: a name the report addresses the node by, an
         * identity the layout resolves a wrapper's place by, or a field the page itself does
         * not apply.
         */
        INERT,
        /**
         * Lost in a case the export knows of and its report does not name yet: the note opens with
         * that case, one of the known ones below, then says the field's fate in every other.
         * Closing the gap moves the field to one of the fates above.
         */
        GAP
    }

    /** A field's fate, and a note on it: for a report the cases it names, for another fate why. */
    record Entry(Fate fate, String note) {
    }

    // A text colour's alpha is written, as Word's text fill, and a drawing's in DrawingML; what a
    // cell's shading or a border holds is opaque, so a translucent one is flattened and reported.
    private static final String PANEL_TRANSLUCENCY = "its translucency, as a panel's, flattened against the colour "
                                                     + "under it, a border's against the panel's fill where it has "
                                                     + "one; any other is written";
    // Word draws a border from a quarter of a point to 12pt wide, in eighths of a point.
    private static final String BORDER_WIDTH = "a width that, rounded to the eighth of a point, is below a quarter "
                                               + "of a point or past 12pt, drawn at Word's thinnest or widest";
    // Word draws a panel's borders above and below past its box; what the space round it and its
    // padding do not take stands the content, or the block below, lower, and is reported.
    private static final String PANEL_BORDERS = "its translucency, as a panel's, flattened against the colour "
                                                + "under it, a border's against the panel's fill where it has one; "
                                                + "what of a border above Word draws past the panel's box that "
                                                + "neither the space above it, its padding nor the room over its "
                                                + "first line takes, on the panel; below, what neither the space "
                                                + "below it nor its padding takes, on the block below "
                                                + "(space above); " + BORDER_WIDTH + "; any other is written";
    // A paragraph's, a page reference's or a rule's own edge below zero is taken out of what is
    // round it (standsIntoTheSpaceAbove); Word has no space above below zero.
    private static final String PULLED_UP = "anywhere else, a top below zero out of the space above it and, a "
                                            + "paragraph's, the foot of the one line above and the top of its own "
                                            + "line, as far as they give; what none gives, named (space above)";
    // An edge below zero pulls the next block up; Word sets no block over another.
    private static final String PULL = "below zero below it, the pull the space under it and the next block's own "
                                       + "top edge do not give back, on the next block (space above); any other is "
                                       + "written";
    private static final String RULE_TRANSLUCENCY = "its translucency, as a rule's, flattened against the colour "
                                                    + "under it; drawn, its alpha is written; any other is written";
    // A rule is a paragraph's border, no wider than 12pt (DocxRules): only a hairline is held.
    private static final String RULE_WIDTH = "as a rule, " + BORDER_WIDTH + "; any other is written";
    private static final String CELL_TRANSLUCENCY = "the translucency of a cell's fill, flattened against the colour "
                                                    + "under the cell, and of its rules, against its fill where it has "
                                                    + "one; of its rules, " + BORDER_WIDTH + ", and the room Word "
                                                    + "makes for them in a row where a text cell its padding leaves "
                                                    + "short of it then needs more than the page's height, the row "
                                                    + "taller; any other is written";

    // Word cuts off what passes a table cell's edge, and draws a nested table's right border
    // outside the table: a panel reaching its cell's edge is narrowed out of its right margin.
    static final String RIGHT_BORDER = "a panel's right border reaching its table cell's edge, its table written "
                                       + "narrower out of its right margin so the border ends half a point short "
                                       + "of the edge, which would cut it off; how much narrower than the page, "
                                       + "and about how far what the margin cannot take in still passes the edge, "
                                       + "named (right border)";

    // A panel's text is given a point of slack past the page's width, out of its right margin.
    static final String RIGHT_END = "a panel's right end, written past the page's by the slack an editor's face is "
                                    + "given where its right margin does not hold it, or not beside a line Word sets "
                                    + "wider than the page, named (right end)";

    // A badge's initials, written as its shape's text, stand off its middle by the page's seat.
    static final String BADGE_SEAT = "in a text badge, its text set off the shape's middle by as much";

    // A row in the flow is written as a panel holding its columns, its paint the panel's.
    private static final String ROW_PAINT = "composed in a table cell, drawn as a shape where it frames no text "
                                            + "and not written where it does; in a page zone's line, in a band of "
                                            + "layers or where a stack's column measures the space round it to its "
                                            + "text, with a padding below zero or a column hanging into it, and "
                                            + "with a margin below zero above or below it or at a side in a cell; "
                                            + "its translucency, as a panel's, and what of a border above or below "
                                            + "Word draws past the panel's box nothing round it takes, and of "
                                            + "a border, " + BORDER_WIDTH + ", as a panel's; any other is "
                                            + "written, as a panel";

    // The gaps: losses the export knows of and its report does not name yet.
    static final String TOP_PULL = "a top below zero: on a picture, a barcode or a block written as a table, a "
                                   + "table, a row or a panel, not written; on another block, what the space above "
                                   + "it and the lines round it cannot give, not written; neither named";
    static final String PULL_UNRECKONED = "a top below zero on the first block of a band's layer or of a row of "
                                          + "columns' later layer, which resumes at the page's distance from what is "
                                          + "above it; on a block opening a page in the flow, a section's first, one "
                                          + "the layout moves to a new page or one after a page break; or on a side "
                                          + "of an overlay's line pair, which takes it out of the space above alone: "
                                          + "not reckoned against the space Word writes there, not named";
    static final String BOTTOM_PULL = "a bottom below zero on a list, a table, a row, a stack, a canvas, a shape "
                                      + "container in the flow or a block with no paint of its own: the next block "
                                      + "not pulled up by it, set that much lower, not named";
    static final String PADDING_BELOW_ZERO = "a table cell's or a panel's padding below zero, and a row's at its "
                                             + "sides, written as none, not named";
    static final String SPACE_BELOW_AT_THE_FOOT = "below a block written as a table that ends a table cell or a "
                                                  + "row's column set at its middle or foot, which Word does not "
                                                  + "count there, the block set that much lower, not named but for "
                                                  + "a shape container";
    static final String CELL_PADDING = "a top or bottom a cell keeps where Word sets it, one opening with a table or "
                                       + "spanning rows, which Word gives the row's other cells too, not named";
    static final String LIST_PICTURE = "in a list's item or marker, placed by the list's first line and the item's "
                                       + "tallest picture rather than its own line, and with no line read not moved "
                                       + "off the baseline, not named";
    static final String SEAT_UNREAD = "where a paragraph's lines are not read, no seat written, not named";
    static final String ROW_COLUMNS_UNLAID = "with no layout, a row's fixed or weighted column beside an auto one, "
                                             + "written with no width, not named";
    static final String DRAWING_OVER_TEXT = "a drawing the page paints over a text or picture layer, drawn behind it, "
                                            + "not named";
    static final String COMPOSED_STACK_LAYER = "of the one layer a stack composed in a table cell writes beside drawn "
                                               + "ones, written at the cell's top left, not named";
    static final String LAYER_PAST_THE_EDGE = "set past its stack's right edge in a table cell the layout places, its "
                                              + "lines held to the edge and broken narrower, not named";
    static final String LAYER_UNLAID = "with no layout, a stack's one layer written where the stack starts, not named";
    static final String PANEL_WIDER_THAN_ITS_CELL = "as a panel in a table cell narrower than it, held to the cell "
                                                    + "where the page draws it past, not named";
    // A row whose children the layout does not place has columns Word sizes to their text.
    private static final String ROW_COLUMNS = "its columns Word's, sized to their text, where the layout does not "
                                              + "show where its children start: composed in a table cell with a "
                                              + "child it does not place, or in a panel or another table in its "
                                              + "cell; with no layout, in the section's note; any other is written";

    /** The known cases: a gap's note opens with one of them, or the ledger fails. */
    private static final List<String> GAPS = List.of(TOP_PULL, BOTTOM_PULL, PADDING_BELOW_ZERO, SPACE_BELOW_AT_THE_FOOT,
            PULL_UNRECKONED, CELL_PADDING, LIST_PICTURE, SEAT_UNREAD, ROW_COLUMNS_UNLAID, DRAWING_OVER_TEXT,
            COMPOSED_STACK_LAYER, LAYER_PAST_THE_EDGE, LAYER_UNLAID, PANEL_WIDER_THAN_ITS_CELL);

    private static final Map<Class<?>, Map<String, Entry>> NODES = new LinkedHashMap<>();
    private static final Map<String, Entry> OUTPUT_OPTIONS = fields(
            "metadata:WRITTEN",
            "watermark:REPORTED",
            "protection:REPORTED",
            "viewerPreferences:REPORTED",
            "headersAndFooters:REPORTED:a band's translucent separator, flattened against white; of a separator, "
            + BORDER_WIDTH + ", one of no width at Word's thinnest, and one below zero or not a number, not "
            + "written; a band too shallow for its line, whose text and separator Word stands further in from "
            + "the edge; page numbers in letters from 28 on, which Word letters otherwise; slots that overlap "
            + "where the page sets them, and a slot wider than the line; any other is written, or reported "
            + "where Word's parts cannot hold it",
            // The node entries below are the body's. A zone is written as one line of its
            // paragraphs' runs, page fields, pictures and tabs, and has its own entry here.
            "zones:REPORTED:where its parts stand across the line, a part the page sets lower than the line "
            + "holds below Word's baseline or past the page's edge, what its paragraphs and pictures lose of "
            + "their own — an anchor, an outline entry, a picture's transform, a picture's place among a "
            + "paragraph's runs — what else a zone holds, lines reaching past the page margin, a line the "
            + "layout does not measure, and a zone built as nothing for no page in particular; its line's "
            + "height, its lines, its tallest part's baseline, each other one-line part's own, raised or "
            + "lowered to it, and the room that part holds above and below its text are written, as exact "
            + "lines placed from the edge, in a frame at its height where a measured zone shares its kind "
            + "with another page zone; its pictures are written in the line at the size the page draws them");

    static {
        node(AlignNode.class, "name:INERT", "child:WRITTEN",
                "align:REPORTED:composed in a table cell where the layout does not say where its child stands, "
                + "written from the cell's left; with no layout, in the section's note; any other is written",
                "margin:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; any other is written");
        node(BarcodeNode.class, "name:INERT", "barcodeOptions:WRITTEN", "width:WRITTEN", "height:WRITTEN",
                "linkTarget:REPORTED", "bookmarkOptions:REPORTED",
                "padding:GAP:" + TOP_PULL + "; its left side; its right moves nothing in a paragraph set from the "
                + "left",
                "margin:GAP:" + TOP_PULL + "; its left side; its right moves nothing in a paragraph set from the "
                + "left",
                "transform:REPORTED", "anchor:WRITTEN");
        node(CanvasLayerNode.class, "name:INERT",
                "width:REPORTED:the width its text wraps at",
                "height:REPORTED:where what it writes, one block under another, runs past it, where a drawing "
                + "in what it writes takes no room in Word, and where it is not measured, composed in a table "
                + "cell or with no layout; where it only draws, before a band's layer's first block, inside "
                + "drawings held whole, as a layer laid over another or in a block a canvas round it counts "
                + "as taking no room, the room is theirs; any other is written, as the room under what it "
                + "writes, or as its whole box where it only draws",
                "placements:REPORTED:where what it writes stands; its drawings stand where it places them",
                "clipPolicy:INERT:the page clips no canvas",
                "padding:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; any other is written",
                "margin:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; any other is written");
        node(ChartNode.class, "name:INERT", "spec:REPORTED", "style:REPORTED:in the chart's note",
                "margin:REPORTED:in the chart's note; above and below; below a block a band or a column measures from, written",
                "padding:REPORTED:in the chart's note; above and below; below a block a band or a column measures from, written");
        node(ContainerNode.class, "name:INERT", "children:WRITTEN", "spacing:WRITTEN",
                "padding:GAP:" + PADDING_BELOW_ZERO + "; " + TOP_PULL + "; " + BOTTOM_PULL + "; any other is written",
                "margin:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; " + SPACE_BELOW_AT_THE_FOOT + "; " + PULL,
                "fillColor:REPORTED:" + RIGHT_END + "; " + PANEL_TRANSLUCENCY,
                "stroke:REPORTED:" + RIGHT_BORDER + "; " + RIGHT_END + "; " + PANEL_BORDERS,
                "cornerRadius:REPORTED", "borders:REPORTED:" + RIGHT_BORDER + "; " + RIGHT_END + "; " + PANEL_BORDERS,
                "anchor:REPORTED:as a layer stack's column, or where it writes no paragraph, as of drawings alone, "
                + "which has no bookmark; otherwise, in the flow, it is bookmarked",
                "bookmarkOptions:REPORTED", "flowWidth:REPORTED:of an unpainted one, a panel in a table cell and a layer stack's column");
        node(EllipseNode.class, "name:INERT", "width:WRITTEN", "height:WRITTEN", "fillColor:WRITTEN",
                "stroke:WRITTEN", "linkTarget:REPORTED", "bookmarkOptions:REPORTED",
                "padding:WRITTEN", "margin:WRITTEN", "transform:REPORTED",
                "anchor:REPORTED");
        node(ImageNode.class, "name:INERT", "imageData:WRITTEN", "width:WRITTEN", "height:WRITTEN",
                "scale:WRITTEN", "fitMode:WRITTEN", "linkTarget:REPORTED",
                "bookmarkOptions:REPORTED",
                "padding:GAP:" + TOP_PULL + "; its left side; its right moves nothing in a paragraph set from the "
                + "left; drawn beside its text or over its badge, all of it",
                "margin:GAP:" + TOP_PULL + "; its left side; its right moves nothing in a paragraph set from the "
                + "left",
                "transform:REPORTED",
                "anchor:WRITTEN");
        node(LayerStackNode.class, "name:INERT",
                "layers:GAP:" + DRAWING_OVER_TEXT + "; " + COMPOSED_STACK_LAYER + "; " + LAYER_PAST_THE_EDGE + "; "
                + LAYER_UNLAID + "; one the page starts above where the layers before it end, which is written under "
                + "them; composed in a table cell or with no layout, more than one of them written, not measured; "
                + "their drawings stack as the page paints them, by their z-index, among those behind the text "
                + "and among those in front of it; the one of a stack of one layer its offset moves down or up, "
                + "or across in a table cell, or at all with no place of its own in the layout, and one set left "
                + "of the stack in a table cell further than the indents round it give",
                "padding:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; " + SPACE_BELOW_AT_THE_FOOT
                + "; any other is written",
                "margin:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; " + SPACE_BELOW_AT_THE_FOOT
                + "; any other is written",
                "clipToBounds:REPORTED:where it cuts what its layers paint; composed in a table cell, on its table");
        node(LineNode.class, "name:INERT", "width:WRITTEN", "height:WRITTEN", "startX:WRITTEN", "startY:WRITTEN",
                "endX:WRITTEN", "endY:WRITTEN",
                "stroke:REPORTED:its translucency, as a rule's, flattened against the colour under it, and as a "
                + "rule, " + BORDER_WIDTH + "; drawn, its alpha is written; any other is written",
                "linkTarget:REPORTED",
                "bookmarkOptions:REPORTED", "padding:GAP:" + PULL_UNRECKONED + "; as a rule, " + PULLED_UP + "; any other is written",
                "margin:GAP:" + PULL_UNRECKONED + "; as a rule, " + PULLED_UP + "; any other is written", "transform:REPORTED",
                "dashPattern:REPORTED",
                "anchor:REPORTED:drawn; a rule in the flow is bookmarked", "lineCap:REPORTED:a rule's other than butt, a border ending flat; drawn, it is written",
                "fillWidth:WRITTEN", "keepWithNext:REPORTED:of a line drawn in the flow the page keeps blocks together in");
        node(ListNode.class, "name:INERT",
                "items:GAP:" + LIST_PICTURE + "; a blank one a hangingIndent list draws as its marker alone, the "
                + "marks of one "
                + "the page reads as markdown, and a chip's translucent fill, flattened against the colour under "
                + "it; any other is written",
                "nestedItems:GAP:" + LIST_PICTURE + "; the marks of one the page reads as markdown, and a chip's "
                + "translucent fill, "
                + "flattened against the colour under it; any other is written",
                "marker:GAP:" + LIST_PICTURE + "; any other is written",
                "textStyle:WRITTEN", "align:REPORTED",
                "lineSpacing:REPORTED:where the layout's items are not its own and one wraps, an item run "
                + "onto the next page among them; composed in a table cell, its wrapping not measured; "
                + "with no layout, in the section's note",
                "itemSpacing:WRITTEN",
                "continuationIndent:REPORTED:where an item wraps, or its lines are not read, in a markerless "
                + "list or a tree of items without hangingIndent, the only lists the page sets it in",
                "normalizeMarkers:WRITTEN", "padding:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; any other is written",
                "margin:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; any other is written",
                "hangingIndent:REPORTED:where an item stands at a stated column, a space past its marker or "
                + "two spaces a level in",
                "markerGap:REPORTED:where an item stands at a stated column, a space past its marker or "
                + "two spaces a level in");
        node(PageBreakNode.class, "name:INERT", "margin:INERT");
        node(PageFieldNode.class, "name:INERT", "kind:WRITTEN", "textStyle:WRITTEN",
                "align:INERT:the page sets a field in a box a point wider than its number, which its "
                + "alignment moves it no further within",
                "padding:REPORTED:at its sides, and above and below beside other parts, where they stand the "
                + "field off the place Word sets it on the zone's line; a lone or tallest field's above and "
                + "below are written, where its line stands",
                "margin:REPORTED:at its sides, and above and below beside other parts, where they stand the "
                + "field off the place Word sets it on the zone's line; a lone or tallest field's above and "
                + "below are written, where its line stands");
        node(PageReferenceNode.class, "name:INERT",
                "anchor:REPORTED:where the anchor has no bookmark, its number is written as text",
                "textStyle:WRITTEN", "align:WRITTEN", "placeholderText:WRITTEN",
                "padding:GAP:" + PULL_UNRECKONED + "; " + PULLED_UP + "; the sides its alignment sets it from",
                "margin:GAP:" + PULL_UNRECKONED + "; " + PULLED_UP + "; the sides its alignment sets it from");
        node(ParagraphNode.class, "name:INERT",
                "text:REPORTED:where the page reads it as markdown, written as the page sets it where its lines "
                + "hold the pieces read so, a heading written taller than its line named; its marks written as letters "
                + "where the lines hold other letters or none, and not measured where they are not read and the "
                + "page's parser drops a mark; any other is written",
                "inlineRuns:REPORTED:a chip's translucent fill, flattened against the colour under it, with what "
                + "else of its shape Word cannot hold; a run's translucent colour is written as Word's text fill",
                "textStyle:WRITTEN", "align:WRITTEN", "lineSpacing:WRITTEN",
                "bulletOffset:REPORTED:its letters before the first line; over the flow, as a side of an "
                + "overlay's pair or as a badge's text, the room it sets lines in by where that moves one",
                "indentStrategy:REPORTED:as its bulletOffset's: a prefix's letters before the first line, and over "
                + "the flow the room it sets lines in by where that moves one; any other is written",
                "linkTarget:WRITTEN",
                "bookmarkOptions:REPORTED:a title that is not the text Word lists it by, a pair's whole line for "
                + "a side of one; a level past Word's ninth that shares it with another; the right side's of a "
                + "pair whose left holds the level",
                "padding:GAP:" + PULL_UNRECKONED + "; " + PULLED_UP + "; " + PULL,
                "margin:GAP:" + PULL_UNRECKONED + "; " + PULLED_UP + "; " + PULL,
                "autoSize:REPORTED:where the layout does not tell the size the page fits the text in the "
                + "paragraph's style to — no lines read, lines in sizes that do not say which is the "
                + "paragraph's, or one paragraph added at more than one place — the text written at its "
                + "style's, not measured; any other is written, at the size the page fits it to",
                "verticalAlign:GAP:" + SEAT_UNREAD + "; its first text line's seat for all its lines, a line in "
                + "another size or face named from half a point; " + BADGE_SEAT + "; any other is written",
                "anchor:WRITTEN", "direction:WRITTEN");
        node(PathNode.class, "name:INERT", "width:WRITTEN", "height:WRITTEN", "segments:WRITTEN",
                "fillColor:WRITTEN", "fillPaint:REPORTED", "stroke:WRITTEN", "strokePaint:REPORTED",
                "padding:WRITTEN", "margin:WRITTEN", "dashPattern:REPORTED", "lineCap:WRITTEN", "lineJoin:WRITTEN");
        node(PolygonNode.class, "name:INERT", "width:WRITTEN", "height:WRITTEN", "points:WRITTEN",
                "fillColor:WRITTEN", "stroke:WRITTEN", "padding:WRITTEN", "margin:WRITTEN");
        node(RowNode.class, "name:INERT", "children:WRITTEN", "weights:WRITTEN", "gap:WRITTEN",
                "padding:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; " + SPACE_BELOW_AT_THE_FOOT + "; "
                + PADDING_BELOW_ZERO + "; any other is written",
                "margin:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; " + SPACE_BELOW_AT_THE_FOOT
                + "; any other is written",
                "fillColor:REPORTED:" + RIGHT_END + "; " + ROW_PAINT,
                "stroke:REPORTED:" + RIGHT_BORDER + "; " + RIGHT_END + "; " + ROW_PAINT,
                "cornerRadius:REPORTED:a panel's corners, which a Word table cell does not round; with no paint "
                + "it paints nothing",
                "borders:REPORTED:" + RIGHT_BORDER + "; " + RIGHT_END + "; " + ROW_PAINT,
                "columns:GAP:" + ROW_COLUMNS_UNLAID + "; " + ROW_COLUMNS, "verticalAlign:WRITTEN",
                "arrangement:REPORTED:" + ROW_COLUMNS);
        // The wrappers the layout adds round a timeline's parts, public records the export writes
        // through to their child.
        node(com.demcha.compose.document.layout.LayoutAnchorNode.class, "name:INERT", "id:INERT", "child:WRITTEN");
        node(com.demcha.compose.document.layout.HorizontalBandsNode.class, "name:INERT", "key:INERT",
                "child:WRITTEN");
        node(com.demcha.compose.document.layout.HorizontalBandContentNode.class, "name:INERT", "key:INERT",
                "slot:WRITTEN", "child:WRITTEN");
        node(SectionNode.class, "name:INERT", "children:WRITTEN", "spacing:WRITTEN",
                "padding:GAP:" + PADDING_BELOW_ZERO + "; " + TOP_PULL + "; " + BOTTOM_PULL + "; any other is written",
                "margin:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; " + SPACE_BELOW_AT_THE_FOOT + "; " + PULL,
                "fillColor:REPORTED:" + RIGHT_END + "; " + PANEL_TRANSLUCENCY,
                "stroke:REPORTED:" + RIGHT_BORDER + "; " + RIGHT_END + "; " + PANEL_BORDERS,
                "cornerRadius:REPORTED", "borders:REPORTED:" + RIGHT_BORDER + "; " + RIGHT_END + "; " + PANEL_BORDERS,
                "keepTogether:WRITTEN",
                "anchor:REPORTED:as a layer stack's column, or where it writes no paragraph, as of drawings alone, "
                + "which has no bookmark; otherwise, in the flow, it is bookmarked",
                "bleed:REPORTED:of a panel the page bleeds, in the flow it pages",
                "bookmarkOptions:REPORTED", "keepWithNext:WRITTEN",
                "flowWidth:REPORTED:of an unpainted one, a panel in a table cell and a layer stack's column");
        node(ShapeContainerNode.class, "name:INERT",
                "outline:GAP:" + PANEL_WIDER_THAN_ITS_CELL + "; drawn as a shape, corners whose radii differ once "
                + "held to half its shorter side "
                + "at the largest of them; a box "
                + "written as a panel in a table cell, its corners square; one no shape draws, or composed in a "
                + "table cell with nowhere to draw it, dropped",
                "layers:GAP:" + DRAWING_OVER_TEXT + "; written inline one after another in source order where it "
                + "writes them in the "
                + "flow; drawn where the page puts them, unreported, where they are all drawing, stacked as the "
                + "page paints them among those behind the text and among those in front of it; a panel's in its "
                + "cell, at its first written layer's height, a paragraph held across to its box, and named where "
                + "one follows the first, is moved down or up, is set past the shape's side, or is set across by "
                + "a box the layout does not give; one set left of its content in a table cell further than the "
                + "indents round it give",
                "clipPolicy:REPORTED:where it cuts what its layers paint; composed in a table cell, on its table",
                "fillColor:REPORTED:" + RIGHT_END + "; its translucency, as a panel's, flattened against the colour "
                + "under it; drawn, its alpha is written; any other is written",
                "stroke:REPORTED:" + RIGHT_BORDER + "; " + RIGHT_END + "; its translucency, as a panel's border, "
                + "flattened against the "
                + "panel's fill or "
                + "the colour under it, and as a panel's, " + BORDER_WIDTH + "; drawn, its alpha and its width "
                + "are written; any other is written",
                "padding:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; written as a panel's in a table cell, round its "
                + "outline as a margin "
                + "is, and below it, where its table ends its cell and the cell sets what it holds at its middle or "
                + "foot, as far as Word does not count it there; any other is written",
                "margin:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; written as a panel's in a table cell, below it, "
                + "where its table ends "
                + "its cell "
                + "and the cell sets what it holds at its middle or foot, as far as Word does not count it "
                + "there; any other is written",
                "transform:REPORTED");
        node(ShapeNode.class, "name:INERT", "width:WRITTEN", "height:REPORTED:" + RULE_WIDTH,
                "fillColor:REPORTED:" + RULE_TRANSLUCENCY,
                "stroke:WRITTEN", "cornerRadius:REPORTED:corners unequal once held to half its shorter side, drawn at the largest radius",
                "linkTarget:REPORTED", "bookmarkOptions:REPORTED",
                "padding:GAP:" + TOP_PULL + "; any other is written",
                "margin:GAP:" + TOP_PULL + "; any other is written", "transform:REPORTED", "fillPaint:REPORTED",
                "anchor:REPORTED:drawn; a rule in the flow is bookmarked");
        node(SpacerNode.class, "name:INERT", "width:WRITTEN", "height:WRITTEN",
                "padding:REPORTED:above and below; below a block a band or a column measures from, written",
                "margin:REPORTED:above and below; below a block a band or a column measures from, written",
                "grow:WRITTEN");
        node(TableNode.class, "name:INERT",
                "columns:REPORTED:where one is auto and the layout does not show their widths — composed in a "
                + "table cell, or with a column edge no row shows — the editor's, a fixed one beside it with "
                + "them; with no layout, in the section's note; any other is written",
                "rows:GAP:" + CELL_PADDING + "; " + PADDING_BELOW_ZERO + "; " + CELL_TRANSLUCENCY,
                "defaultCellStyle:GAP:" + CELL_PADDING + "; " + PADDING_BELOW_ZERO + "; " + CELL_TRANSLUCENCY,
                "rowStyles:GAP:" + CELL_PADDING + "; " + PADDING_BELOW_ZERO + "; " + CELL_TRANSLUCENCY,
                "columnStyles:GAP:" + CELL_PADDING + "; " + PADDING_BELOW_ZERO + "; " + CELL_TRANSLUCENCY,
                "width:WRITTEN", "linkTarget:REPORTED",
                "bookmarkOptions:REPORTED",
                "padding:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; " + SPACE_BELOW_AT_THE_FOOT
                + "; any other is written",
                "margin:GAP:" + TOP_PULL + "; " + BOTTOM_PULL + "; " + SPACE_BELOW_AT_THE_FOOT
                + "; any other is written",
                "repeatedHeaderRowCount:WRITTEN",
                "anchor:WRITTEN");
    }

    @Test
    void everyNodeKindGraphComposeShipsHasAnEntry() throws Exception {
        // Every concrete one, nested or not, public or not: the export counts all of them as its
        // own (DocxSemanticBackend.isBuiltIn), so none may go undecided.
        assertThat(names(nodeClasses()))
                .as("the node classes of %s: a kind with no entry has fields nobody decided "
                    + "the DOCX export's answer for", PACKAGES)
                .containsExactlyInAnyOrderElementsOf(names(NODES.keySet()));
    }

    @Test
    void everyNodeKindGraphComposeShipsIsARecord() throws Exception {
        // The ledger reads a node's fields as its record components; a node class that is not a
        // record would have fields this test cannot see, and so no decision for any of them.
        assertThat(nodeClasses()).as("node classes that are not records")
                .filteredOn(kind -> !kind.isRecord()).isEmpty();
    }

    @Test
    void everyFieldOfEveryNodeHasAnEntry() {
        Map<String, Set<String>> undecided = new LinkedHashMap<>();
        Map<String, Set<String>> stale = new LinkedHashMap<>();
        NODES.forEach((kind, entries) -> {
            Set<String> fields = components(kind);
            Set<String> missing = new TreeSet<>(fields);
            missing.removeAll(entries.keySet());
            Set<String> extra = new TreeSet<>(entries.keySet());
            extra.removeAll(fields);
            if (!missing.isEmpty()) {
                undecided.put(kind.getSimpleName(), missing);
            }
            if (!extra.isEmpty()) {
                stale.put(kind.getSimpleName(), extra);
            }
        });
        assertThat(undecided).as("fields with no entry: decide whether the DOCX export writes them, "
                                 + "reports them, or has nothing to carry, and say which here").isEmpty();
        assertThat(stale).as("entries for fields the node no longer has").isEmpty();
    }

    @Test
    void everyOutputOptionHasAnEntry() {
        assertThat(components(DocumentOutputOptions.class))
                .as("the output options a session hands every export")
                .containsExactlyInAnyOrderElementsOf(OUTPUT_OPTIONS.keySet());
    }

    private static void node(Class<? extends DocumentNode> kind, String... specs) {
        NODES.put(kind, fields(specs));
    }

    /** {@code field:FATE}, or {@code field:FATE:a note}, as {@code field:REPORTED:the cases the report names}. */
    static Map<String, Entry> fields(String... specs) {
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (String spec : specs) {
            String[] parts = spec.split(":", 3);
            Entry entry = new Entry(Fate.valueOf(parts[1]), parts.length > 2 ? parts[2] : "");
            if (entry.fate() == Fate.GAP && GAPS.stream().noneMatch(entry.note()::startsWith)) {
                throw new IllegalStateException("a gap whose note opens with no known case, for " + parts[0]);
            }
            if (entries.put(parts[0], entry) != null) {
                throw new IllegalStateException("two entries for " + parts[0]);
            }
        }
        return entries;
    }

    private static Set<String> components(Class<?> record) {
        return Arrays.stream(record.getRecordComponents()).map(RecordComponent::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> names(java.util.Collection<Class<?>> kinds) {
        return kinds.stream().map(Class::getName).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * The packages whose node kinds the export counts as its own: the public node records, and
     * the wrappers the layout adds (see {@code DocxSemanticBackend.isBuiltIn}).
     */
    private static final List<String> PACKAGES = List.of(DocumentNode.class.getPackageName(),
            com.demcha.compose.document.layout.LayoutAnchorNode.class.getPackageName());

    /**
     * The concrete node classes the core module ships in {@link #PACKAGES} — nested ones, and
     * ones not public, included.
     */
    static List<Class<?>> nodeClasses() throws IOException, URISyntaxException, ClassNotFoundException {
        Path source = Path.of(DocumentNode.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<String> classFiles = new ArrayList<>();
        for (String pkg : PACKAGES) {
            String folder = pkg.replace('.', '/') + "/";
            if (Files.isDirectory(source)) {
                try (Stream<Path> files = Files.list(source.resolve(folder))) {
                    files.map(file -> folder + file.getFileName()).forEach(classFiles::add);
                }
            } else {
                try (JarFile jar = new JarFile(source.toFile())) {
                    jar.stream().map(java.util.zip.ZipEntry::getName)
                            .filter(name -> name.startsWith(folder) && name.indexOf('/', folder.length()) < 0)
                            .forEach(classFiles::add);
                }
            }
        }
        List<Class<?>> kinds = new ArrayList<>();
        for (String file : classFiles) {
            if (!file.endsWith(".class") || file.endsWith("package-info.class")) {
                continue;
            }
            // Loaded without initialising: a nested class's binary name keeps its '$'.
            Class<?> type = Class.forName(file.substring(0, file.length() - ".class".length()).replace('/', '.'),
                    false, DocumentNode.class.getClassLoader());
            if (DocumentNode.class.isAssignableFrom(type)
                && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
                kinds.add(type);
            }
        }
        return kinds;
    }
}
