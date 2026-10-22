package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.output.DocumentOutputOptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.BADGE_SEAT;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.BOTTOM_PULL;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.CELL_PADDING;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.COMPOSED_STACK_LAYER;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.DRAWING_OVER_TEXT;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.LAYER_PAST_THE_EDGE;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.LAYER_UNLAID;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.LIST_PICTURE;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.PADDING_BELOW_ZERO;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.PANEL_WIDER_THAN_ITS_CELL;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.PULL_UNRECKONED;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.RIGHT_BORDER;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.RIGHT_END;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.ROW_COLUMNS_UNLAID;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.SEAT_UNREAD;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.SPACE_BELOW_AT_THE_FOOT;
import static com.demcha.compose.document.backend.semantic.docx.DocxNodeFieldLedgerTest.TOP_PULL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the DOCX export does with every part of every value a node's field or an output option
 * holds: a style's colour, a run's alignment, a border's width, a chart's legend, a header's
 * separator. Its parts are a record's components, a final class's fields and an enum's constants,
 * and each takes one of the fates {@link DocxNodeFieldLedgerTest} gives a node's fields: written,
 * reported where the author set it and the file does not carry it, nothing for Word to carry, or a
 * gap, a case the export knows it loses and its report does not name yet, which the entry says.
 *
 * <p>The node ledger stops at a node's fields, and the values in them are where most of what a
 * page draws is said. A part added to one of them later is drawn by the PDF backend at once and,
 * until the DOCX export decides, passes every node entry with no decision of its own. This ledger
 * reads the values from the node classes and the output options, down to the last one they hold,
 * and fails on any value type or part it has no entry for, and on any entry for a part that is
 * gone. An enum's constant is a part: a value added to one is an answer the export has not been
 * asked for.</p>
 *
 * <p>A value used in several places has the fate of all of them: reported where any loses it, a
 * gap where any loses it unnamed, and its note says where. The entries are claims about the
 * export, as the node ledger's are: a written or a reported one proved by its tests, a gap a known
 * defect held here until it is closed.</p>
 */
class DocxValueFieldLedgerTest {

    // A drawing's gradient is not carried, an inline icon's is drawn in one colour.
    private static final String GRADIENT = "a shape's or a path's gradient is not drawn: the drawing keeps what else "
                                           + "it paints, or is dropped; an inline icon's is drawn flat, in its first "
                                           + "colour; a chart's, with its drawing";

    private static final Map<Class<?>, Map<String, DocxNodeFieldLedgerTest.Entry>> VALUES = new LinkedHashMap<>();

    static {
        value(com.demcha.compose.document.node.HorizontalAlign.class,
                "CENTER:REPORTED:composed in a table cell where the layout does not say where its child stands, "
                + "written from the cell's left; with no layout, in the section's note; any other is written, as "
                + "the indents that hold the child where the layout places it",
                "LEFT:WRITTEN",
                "RIGHT:REPORTED:composed in a table cell where the layout does not say where its child stands, "
                + "written from the cell's left; with no layout, in the section's note; any other is written, as "
                + "the indents that hold the child where the layout places it");
        value(com.demcha.compose.document.node.DocumentBookmarkOptions.class,
                "level:REPORTED:its paragraph's heading level, past Word's ninth at the ninth, named where a level "
                + "the page nests apart shares it; not written, named, for a line pair's right side whose left holds "
                + "one, in a page zone and on any node but a paragraph",
                "title:REPORTED:Word lists a heading by its paragraph's text: a title that is not that text, and "
                + "one side's of a line pair, which lists the whole line, named; not written where its level is not");
        value(com.demcha.compose.document.node.ListMarker.class,
                "runs:WRITTEN:each run written as in a paragraph, before its item; what a chip or a picture among "
                + "them loses is the run's own",
                "value:WRITTEN:as a Word list level's text where every item at its depth shares it and it holds no "
                + "percent sign before a digit, otherwise as letters before the item; a marker of runs is written by "
                + "its runs, this being their plain reading");
        value(com.demcha.compose.document.style.DocumentTextStyle.class,
                "color:WRITTEN:its alpha as Word's text fill",
                "decoration:WRITTEN",
                "fontName:REPORTED:a face that may not travel or cannot be read, named as an embedded font; any "
                + "other is written, a face alias as its family, the weight its decoration's",
                "letterSpacing:WRITTEN:as Word's character spacing",
                "size:WRITTEN:to Word's half point; an auto-sized paragraph's text at the size the layout fits it "
                + "to, where its lines tell it, as its autoSize says");
        value(com.demcha.compose.document.node.TextAlign.class,
                "CENTER:REPORTED:a list's items, written flush left, named; in a page zone, where its part stands "
                + "is the zone's note; a page field's the page does not apply; any other is written as Word's "
                + "alignment, turned to the flow in a right-to-left paragraph",
                "LEFT:WRITTEN:turned to the flow in a right-to-left paragraph",
                "RIGHT:REPORTED:a list's items, written flush left, named; in a page zone, where its part stands "
                + "is the zone's note; a page field's the page does not apply; any other is written as Word's "
                + "alignment, turned to the flow in a right-to-left paragraph");
        value(com.demcha.compose.document.node.PageFieldKind.class,
                "NUMBER:WRITTEN:as Word's page field on a page zone's line, each section counting from 1 as on the "
                + "page; a field in the body is not written, which its node's note names",
                "TOTAL:WRITTEN:as Word's count of the pages, a section's in a document of several, reading the "
                + "layout's count until it updates; a field in the body is not written, which its node's note names");
        value(com.demcha.compose.document.style.DocumentTextIndent.class,
                "ALL_LINES:REPORTED:a blank prefix as every line's indent, one with letters as the spaces covering "
                + "it after the first line, its letters named; in a text box, line pair or badge, its room named "
                + "where it moves a line; in a page zone, the zone's note",
                "FIRST_LINE:REPORTED:a blank prefix written as the first line's indent; one with letters, not "
                + "written, named; in a text box, line pair or badge, its room named where it moves a line; in a "
                + "page zone, the zone's note",
                "FROM_SECOND_LINE:REPORTED:written as the indent of the lines after the first, a prefix with letters "
                + "as the spaces covering it; in a text box, line pair or badge, its room named where it moves a "
                + "line; in a page zone, the zone's note",
                "NONE:WRITTEN:the page sets no prefix, and no indent is written for one");
        value(com.demcha.compose.document.style.DocumentTextAutoSize.class,
                "maxSize:REPORTED:read through the size the layout fits the text to, off its lines; where they do "
                + "not tell it, unread, in sizes that do not say which is the paragraph's, or one place's of several, "
                + "the text is written at its style's size, named",
                "minSize:REPORTED:as maxSize: through the fitted size where the lines tell it, the style's size "
                + "named where not",
                "step:REPORTED:as maxSize: through the fitted size where the lines tell it, the style's size named "
                + "where not");
        value(com.demcha.compose.document.node.TextVerticalAlign.class,
                "BOTTOM:GAP:" + SEAT_UNREAD
                + "; written as its text's position in its lines, the first text line's seat for all "
                + "of them; a line in another size or face, seated as the first, named from half a point; in a page "
                + "zone, as its part of the zone's line; " + BADGE_SEAT,
                "CENTER:GAP:" + SEAT_UNREAD
                + "; written as its text's position in its lines, the first text line's seat for all "
                + "of them; a line in another size or face, seated as the first, named from half a point; in a page "
                + "zone, as its part of the zone's line; " + BADGE_SEAT,
                "DEFAULT:WRITTEN:on the baseline, with no seat to write",
                "TOP:GAP:" + SEAT_UNREAD
                + "; written as its text's position in its lines, the first text line's seat for all of "
                + "them; a line in another size or face, seated as the first, named from half a point; in a page "
                + "zone, as its part of the zone's line; " + BADGE_SEAT);
        value(com.demcha.compose.document.node.TextDirection.class,
                "AUTO:REPORTED:resolved as the page resolves it and written as the direction it resolves to; "
                + "resolved right to left on a page zone's paragraph, named as written left to right",
                "LTR:WRITTEN",
                "RTL:REPORTED:on a page zone's paragraph, written left to right, named; any other is written as "
                + "Word's right-to-left paragraph and runs");
        value(com.demcha.compose.document.node.ExternalLinkTarget.class,
                "options:WRITTEN:as an external link from the part the text is in, a header's or footer's included");
        value(com.demcha.compose.document.node.InternalLinkTarget.class,
                "anchor:REPORTED:written as a link to its bookmark; where the node declaring the anchor writes "
                + "none, drawn, in a page zone, a stack's column or a block writing no paragraph, named on that "
                + "node; an anchor several nodes declare or a repeated header row holds, named");
        value(com.demcha.compose.document.node.ListItem.class,
                "children:WRITTEN:each a depth further in, as the next level of a Word list or two spaces more; "
                + "where an item stands against the page's column is the list's note",
                "label:REPORTED:where the page reads it as markdown, its marks named where they are written as "
                + "letters; an item of runs is written by its runs, this being their plain reading; any other is "
                + "written",
                "marker:WRITTEN:its own, or its depth's default as on the page; a Word list's level where every "
                + "item at its depth shares it, otherwise its letters or runs before the item",
                "runs:WRITTEN:each run written as in a paragraph; what a chip or a picture among them loses is the "
                + "run's own");
        value(com.demcha.compose.font.FontName.class,
                "name:REPORTED:resolved to its family as the layout resolves it and written as that family's Word "
                + "name; a face of it that may not travel or cannot be read, named as an embedded font",
                "normalizedName:INERT:the name without case, the identity its family is found by; nothing Word reads");
        value(com.demcha.compose.document.style.DocumentTextDecoration.class,
                "BOLD:WRITTEN",
                "BOLD_ITALIC:WRITTEN",
                "DEFAULT:WRITTEN:no face of its own, and the Normal style sets none",
                "ITALIC:WRITTEN",
                "STRIKETHROUGH:WRITTEN",
                "UNDERLINE:WRITTEN:as Word's single underline");
        value(com.demcha.compose.document.style.DocumentLetterSpacing.class,
                "type:WRITTEN:through the points it resolves the value to",
                "value:WRITTEN:in twentieths of a point; one past what Word can hold fails the export rather than "
                + "wrap");
        value(com.demcha.compose.document.style.DocumentLetterSpacing.Type.class,
                "FONT_SIZE:WRITTEN:against the size the run is written at, an auto-sized paragraph's fitted size",
                "POINTS:WRITTEN");
        value(com.demcha.compose.document.node.DocumentLinkOptions.class,
                "uri:WRITTEN:as the link's target");
        value(com.demcha.compose.document.node.InlineTextRun.class,
                "linkTarget:WRITTEN:its own link before its paragraph's",
                "text:WRITTEN:each line break as Word's; characters the page draws nothing for left out",
                "textStyle:WRITTEN:its own, else its paragraph's or list's, as the page falls back");
        value(com.demcha.compose.document.node.InlineImageRun.class,
                "alignment:GAP:" + LIST_PICTURE
                + "; written as the picture's raise off the baseline, by the first line's measure; "
                + "where its paragraph's lines are not read, set other than on the baseline, and on a later line of "
                + "another height, named; in a page zone, named; any other is written",
                "baselineOffset:GAP:" + LIST_PICTURE
                + "; added to that raise; where its paragraph's lines are not read, named from "
                + "half a point or with its alignment; in a page zone, named; any other is written",
                "height:WRITTEN",
                "imageData:REPORTED:empty data, nothing to write, named; any other is written as given, typed by "
                + "its signature",
                "linkTarget:WRITTEN:its own link before its paragraph's",
                "width:WRITTEN");
        value(com.demcha.compose.document.node.InlineShapeRun.class,
                "alignment:GAP:" + LIST_PICTURE
                + "; as an inline image's, a raised picture carrying its rise as transparent rows; "
                + "where its paragraph's lines are not read, set other than on the baseline, and on a later line of "
                + "another height, named; in a page zone, named; any other is written",
                "baselineOffset:GAP:" + LIST_PICTURE
                + "; as an inline image's: where its paragraph's lines are not read, named from "
                + "half a point or with its alignment; in a page zone, named; any other is written",
                "layers:REPORTED:drawn as one picture of its layers, named; where no text beside it gives back how "
                + "much wider than its box the picture is, how far that moves its line, named from half a point",
                "linkTarget:WRITTEN:its own link before its paragraph's");
        value(com.demcha.compose.document.node.InlineSvgRun.class,
                "alignment:GAP:" + LIST_PICTURE
                + "; as an inline image's, a raised picture carrying its rise as transparent rows; "
                + "where its paragraph's lines are not read, set other than on the baseline, and on a later line of "
                + "another height, named; in a page zone, named; any other is written",
                "baselineOffset:GAP:" + LIST_PICTURE
                + "; as an inline image's: where its paragraph's lines are not read, named from "
                + "half a point or with its alignment; in a page zone, named; any other is written",
                "height:WRITTEN:the picture's, a raised one grown by its rise",
                "icon:REPORTED:drawn as a picture, named, the text it stands for its description; a gradient, "
                + "drawn flat in its first colour, named",
                "linkTarget:WRITTEN:its own link before its paragraph's",
                "width:WRITTEN");
        value(com.demcha.compose.document.node.InlineHighlightRun.class,
                "background:REPORTED:as the run's shading; what Word's shading does not hold of it, named on the chip",
                "linkTarget:WRITTEN:its own link before its paragraph's",
                "text:WRITTEN:its line breaks as spaces, as the page keeps a chip on one line",
                "textStyle:WRITTEN:its own, else its paragraph's or list's");
        value(com.demcha.compose.document.node.InlineImageAlignment.class,
                "BASELINE:GAP:" + LIST_PICTURE
                + "; its foot on the baseline, where Word stands a picture; where its paragraph's lines "
                + "are not read, a drawn shape's picture, which reaches below its box, stands that much high, named "
                + "from half a point; any other is written",
                "CENTER:GAP:" + LIST_PICTURE
                + "; written as a raise off the baseline, by the first line's measure; where its "
                + "paragraph's lines are not read, standing on the baseline, and on a later line of another height, "
                + "named; in a page zone, named; any other is written",
                "TEXT_BOTTOM:GAP:" + LIST_PICTURE
                + "; written as a raise off the baseline, by the first line's measure; where its "
                + "paragraph's lines are not read, standing on the baseline, and on a later line of another height, "
                + "named; in a page zone, named; any other is written",
                "TEXT_TOP:GAP:" + LIST_PICTURE
                + "; written as a raise off the baseline, by the first line's measure; where its "
                + "paragraph's lines are not read, standing on the baseline, and on a later line of another height, "
                + "named; in a page zone, named; any other is written");
        value(com.demcha.compose.document.style.InlineBackground.class,
                "cornerRadius:REPORTED:above zero, named, a run's shading being square",
                "fill:REPORTED:as the run's shading; a translucent one, flattened against what Word paints under it, "
                + "named; any other is written",
                "padding:REPORTED:beside its letters, as space after a letter where one can take it; where none can "
                + "(opening its line, after a picture, by an emoji, right to left), below zero, and above and below "
                + "its letters, named");
        value(com.demcha.compose.document.style.DocumentInsets.class,
                "bottom:GAP:" + BOTTOM_PULL + "; " + SPACE_BELOW_AT_THE_FOOT + "; " + PADDING_BELOW_ZERO
                + "; below zero, the pull nothing under it gives back, on the next block (space above); "
                + "a chart's or a spacer's no band or column measures from; a page field's beside other parts; a "
                + "picture's padding drawn beside its text or over its badge; a chip's; a shape container's at its "
                + "table cell's middle or foot; any other is written",
                "left:GAP:" + PADDING_BELOW_ZERO
                + "; a picture's, a barcode's and a chart's, and a page reference's on the side its "
                + "alignment sets it from; a page field's at its sides; a picture's padding drawn beside its text or "
                + "over its badge; a chip's its shading does not hold; any other is written",
                "right:GAP:" + PADDING_BELOW_ZERO
                + "; a page reference's on the side its alignment sets it from; a page field's at its "
                + "sides; a picture's padding drawn beside its text or over its badge; a chip's its shading does not "
                + "hold; any other is written",
                "top:GAP:" + TOP_PULL + "; " + PULL_UNRECKONED + "; a paragraph's, a rule's or a page reference's "
                + "otherwise named (space above); " + PADDING_BELOW_ZERO
                + "; a chart's or a spacer's; a page field's beside other parts; a picture's padding drawn "
                + "beside its text or over its badge; a chip's, above its letters; any other is written");
        value(com.demcha.compose.document.style.DocumentTransform.class,
                "rotationDegrees:REPORTED", "scaleX:REPORTED", "scaleY:REPORTED");
        value(com.demcha.compose.document.style.ClipPolicy.class,
                "CLIP_BOUNDS:REPORTED:where it cuts what its layers paint past its box, which is written whole; "
                + "composed in a table cell, on its table; with no layout, wherever it is set; a canvas's, which the "
                + "page does not apply, cuts nothing",
                "CLIP_PATH:REPORTED:where it cuts what its layers paint past its outline, which is written whole, a "
                + "picture filling the ellipse that clips it cropped to it in the file; composed in a table cell, on "
                + "its table; with no layout, wherever it is set; a canvas's cuts nothing",
                "OVERFLOW_VISIBLE:INERT:it cuts nothing, on the page or in the Word file");
        value(com.demcha.compose.document.style.DocumentColor.class,
                "color:REPORTED:its translucency where Word holds an opaque colour — a panel's or a table cell's "
                + "fill or border, a rule, a chip, a page zone's separator — flattened against the colour under it; "
                + "a chart's or a watermark's, with them; any other is written, alpha included");
        value(com.demcha.compose.document.style.DocumentStroke.class,
                "color:REPORTED:its translucency as a Word border — a panel's, a table cell's, a rule's — flattened "
                + "against the panel's fill or the colour under it; a chart's, with its drawing; drawn, its alpha is "
                + "written; any other is written",
                "width:REPORTED:" + RIGHT_BORDER
                + "; as a Word border, a width that, rounded to the eighth of a point, is below a quarter "
                + "of a point or past 12pt, drawn at Word's thinnest or widest, and the room Word makes for a panel's "
                + "or a cell's border past the page's; a chart's, with its drawing; any other is written");
        value(com.demcha.compose.document.style.DocumentCornerRadius.class,
                "bottomLeft:REPORTED:a panel's, square, as a Word table cell is; drawn, corners that differ once each "
                + "is held to half the shorter side, at the largest of them; a chart's, with its drawing; any other "
                + "is written",
                "bottomRight:REPORTED:a panel's, square, as a Word table cell is; drawn, corners that differ once each "
                + "is held to half the shorter side, at the largest of them; a chart's, with its drawing; any other "
                + "is written",
                "topLeft:REPORTED:a panel's, square, as a Word table cell is; drawn, corners that differ once each "
                + "is held to half the shorter side, at the largest of them; a chart's, with its drawing; any other "
                + "is written",
                "topRight:REPORTED:a panel's, square, as a Word table cell is; drawn, corners that differ once each "
                + "is held to half the shorter side, at the largest of them; a chart's, with its drawing; any other "
                + "is written");
        value(com.demcha.compose.document.style.DocumentBorders.class,
                "bottom:REPORTED:" + RIGHT_END
                + "; as a panel's: its translucency, flattened against the panel's fill or the colour "
                + "under it; a width outside Word's quarter of a point to 12pt; its reach past the panel's box the "
                + "space below it and its padding do not take, on the block below; a row's where its paint is not "
                + "written; any other is written",
                "left:REPORTED:as a panel's: its translucency, flattened against the panel's fill or the colour "
                + "under it, and a width outside Word's quarter of a point to 12pt; a row's where its paint is not "
                + "written; any other is written",
                "right:REPORTED:" + RIGHT_BORDER + "; " + RIGHT_END
                + "; as a panel's: its translucency, flattened against the panel's fill or the colour "
                + "under it, and a width outside Word's quarter of a point to 12pt; a row's where its paint is not "
                + "written; any other is written",
                "top:REPORTED:" + RIGHT_END
                + "; as a panel's: its translucency, flattened against the panel's fill or the colour "
                + "under it; a width outside Word's quarter of a point to 12pt; its reach past the panel's box the "
                + "space above it, its padding and the room over its first line do not take, on the panel; a row's "
                + "where its paint is not written; any other is written");
        value(com.demcha.compose.document.style.DocumentFlowWidth.class,
                "points:REPORTED:of an unpainted block, a panel the layout gives no width — composed in a table "
                + "cell, or with no layout — and a layer stack's column, where it holds text that wraps, which runs "
                + "the width of its column; any other is written");
        value(com.demcha.compose.document.style.DocumentDashPattern.class,
                "segments:REPORTED:drawn, as a line's or a path's stroke, drawn solid; as a rule, Word's own dash or "
                + "dots, not the pattern's lengths; in a table cell, on its table; an inline icon's is written in its "
                + "picture");
        value(com.demcha.compose.document.style.DocumentLineCap.class,
                "BUTT:WRITTEN",
                "ROUND:REPORTED:a rule's, a border ending flat; drawn, and in an inline icon's picture, it is written",
                "SQUARE:REPORTED:a rule's, a border ending flat; drawn, and in an inline icon's picture, it is "
                + "written");
        value(com.demcha.compose.document.style.DocumentLineJoin.class,
                "BEVEL:WRITTEN", "MITER:WRITTEN", "ROUND:WRITTEN");
        value(com.demcha.compose.document.style.DocumentBleed.class,
                "edges:REPORTED:of a panel in the flow the page pages, whose fill and borders stop at its box; "
                + "elsewhere the page bleeds nothing");
        value(com.demcha.compose.document.style.DocumentEdge.class,
                "BOTTOM:REPORTED:as an edge a panel in the flow the page pages bleeds to: its fill and borders stop "
                + "at its box",
                "LEFT:REPORTED:as an edge a panel in the flow the page pages bleeds to: its fill and borders stop "
                + "at its box",
                "RIGHT:REPORTED:as an edge a panel in the flow the page pages bleeds to: its fill and borders stop "
                + "at its box",
                "TOP:REPORTED:as an edge a panel in the flow the page pages bleeds to: its fill and borders stop "
                + "at its box");
        value(com.demcha.compose.document.style.DocumentPaint.Solid.class,
                "color:REPORTED:its translucency as a shape's fill written as a rule, flattened against the colour "
                + "under it; a chart's, with its drawing; any other is written, alpha included");
        value(com.demcha.compose.document.style.DocumentPaint.Linear.class,
                "angleDegrees:REPORTED:" + GRADIENT,
                "stops:REPORTED:" + GRADIENT);
        value(com.demcha.compose.document.style.DocumentPaint.Radial.class,
                "cx:REPORTED:" + GRADIENT,
                "cy:REPORTED:" + GRADIENT,
                "stops:REPORTED:" + GRADIENT);
        value(com.demcha.compose.document.style.DocumentPaint.LinearAxis.class,
                "stops:REPORTED:" + GRADIENT,
                "x0:REPORTED:" + GRADIENT,
                "x1:REPORTED:" + GRADIENT,
                "y0:REPORTED:" + GRADIENT,
                "y1:REPORTED:" + GRADIENT);
        value(com.demcha.compose.document.style.DocumentPaint.RadialCircle.class,
                "cx:REPORTED:" + GRADIENT,
                "cy:REPORTED:" + GRADIENT,
                "r:REPORTED:" + GRADIENT,
                "stops:REPORTED:" + GRADIENT);
        value(com.demcha.compose.document.style.DocumentPaint.Stop.class,
                "color:REPORTED:as its gradient's: none is drawn on a shape or a path; in an inline icon the first "
                + "stop's is drawn flat and the others are not; a chart's, with its drawing",
                "offset:REPORTED:" + GRADIENT);
        value(com.demcha.compose.document.node.RowVerticalAlign.class,
                "BOTTOM:WRITTEN", "CENTER:WRITTEN",
                "TOP:WRITTEN:Word's cell default, so nothing is written for it");
        value(com.demcha.compose.document.node.RowArrangement.class,
                "CENTER:REPORTED:where the layout does not show where the row's children start — composed in a "
                + "table cell with a child its lines do not place, or inside a panel or another table in its cell — "
                + "its columns are Word's, sized to their text; with no layout, in the section's note; any other is "
                + "written",
                "END:REPORTED:where the layout does not show where the row's children start — composed in a "
                + "table cell with a child its lines do not place, or inside a panel or another table in its cell — "
                + "its columns are Word's, sized to their text; with no layout, in the section's note; any other is "
                + "written",
                "SPACE_AROUND:REPORTED:where the layout does not show where the row's children start — composed in a "
                + "table cell with a child its lines do not place, or inside a panel or another table in its cell — "
                + "its columns are Word's, sized to their text; with no layout, in the section's note; any other is "
                + "written",
                "SPACE_BETWEEN:REPORTED:where the layout does not show where the row's children start — composed in a "
                + "table cell with a child its lines do not place, or inside a panel or another table in its cell — "
                + "its columns are Word's, sized to their text; with no layout, in the section's note; any other is "
                + "written",
                "SPACE_EVENLY:REPORTED:where the layout does not show where the row's children start — composed in a "
                + "table cell with a child its lines do not place, or inside a panel or another table in its cell — "
                + "its columns are Word's, sized to their text; with no layout, in the section's note; any other is "
                + "written",
                "START:WRITTEN");
        value(com.demcha.compose.document.style.DocumentRowColumn.class,
                "type:REPORTED:an auto column, with those beside it, where the layout does not show where the row's "
                + "children start, Word's, sized to their text; with no layout, in the section's note; any other is "
                + "written",
                "value:GAP:" + ROW_COLUMNS_UNLAID
                + "; a fixed width or a weight beside an auto column, where the layout does not show where the "
                + "row's children start, Word's with the rest, named; in a row its arrangement or a growing spacer "
                + "divides, the page reads none of them; any other is written");
        value(com.demcha.compose.document.style.DocumentRowColumn.Type.class,
                "AUTO:REPORTED:where the layout does not show where the row's children start — composed in a table "
                + "cell with a child its lines do not place, or inside a panel or another table in its cell — Word's, "
                + "sized to its text, with the columns beside it; with no layout, in the section's note; any other is "
                + "written",
                "FIXED:GAP:" + ROW_COLUMNS_UNLAID
                + "; beside an auto column, where the layout does not show where the row's children start, "
                + "Word's with the rest, named; any other is written",
                "WEIGHT:GAP:" + ROW_COLUMNS_UNLAID
                + "; beside an auto column, where the layout does not show where the row's children start, "
                + "Word's with the rest, named; any other is written");
        value(com.demcha.compose.document.layout.LayoutAnchorId.class,
                "groupKey:INERT:an identity the layout resolves a wrapper's place by, which the export does not read",
                "index:INERT:an identity the layout resolves a wrapper's place by, which the export does not read",
                "kind:INERT:an identity the layout resolves a wrapper's place by, which the export does not read");
        value(com.demcha.compose.document.node.DocumentBarcodeOptions.class,
                "content:WRITTEN:encoded in the symbol's picture, and as its description",
                "type:WRITTEN:as the symbology the picture's matrix is encoded in",
                "foreground:WRITTEN:as the picture's dark cells, alpha included",
                "background:WRITTEN:as the picture's light cells, alpha included",
                "quietZoneMargin:WRITTEN:as the quiet zone round the picture's matrix");
        value(com.demcha.compose.document.node.DocumentBarcodeType.class,
                "QR_CODE:WRITTEN", "CODE_128:WRITTEN", "CODE_39:WRITTEN", "EAN_13:WRITTEN", "EAN_8:WRITTEN",
                "UPC_A:WRITTEN", "PDF_417:WRITTEN", "DATA_MATRIX:WRITTEN");
        value(com.demcha.compose.document.image.DocumentImageData.class,
                "bytes:WRITTEN:embedded as the picture",
                "path:WRITTEN:read when the document is exported, its bytes embedded as the picture; a file that "
                + "does not read fails the export");
        value(com.demcha.compose.document.image.DocumentImageFitMode.class,
                "STRETCH:WRITTEN:at its box's size",
                "CONTAIN:WRITTEN:at the size it fits its box at, the room it leaves held round it as the page "
                + "centres it",
                "COVER:WRITTEN:at its box's size, cropped to the box about its middle");
        value(com.demcha.compose.document.node.CanvasChild.class,
                "node:WRITTEN:as its own node's entries say",
                "x:REPORTED:of a block the canvas writes, set in from its left edge, which is written at that "
                + "edge, one block after another; a drawing is drawn where it is placed",
                "y:REPORTED:of a block the canvas writes, set elsewhere than at the foot of the one before, which "
                + "is written there; a drawing is drawn where it is placed");
        value(com.demcha.compose.document.node.LayerStackNode.Layer.class,
                "node:WRITTEN:as its own node's entries say",
                "align:GAP:" + COMPOSED_STACK_LAYER
                + "; with no layout, set in from the left, written from the left; among several layers "
                + "written with no place of their own, not measured; in a shape's panel in a table cell, at "
                + "another height than its first written layer, or across by a box the layout does not give; any "
                + "other is written, where the layout places it",
                "offsetX:GAP:" + COMPOSED_STACK_LAYER + "; " + LAYER_PAST_THE_EDGE
                + "; of a stack's one layer, in a table cell or with no place of its own in the "
                + "layout; left of its stack in a table cell further than the indents round it give; in a shape's "
                + "panel in a table cell, past its right edge or by a box the layout does not give; with no "
                + "layout; any other is written",
                "offsetY:GAP:" + COMPOSED_STACK_LAYER + "; " + LAYER_UNLAID
                + "; moving a stack's one layer down or up where no band or column sets the space "
                + "above it, or with no place of its own in the layout; one the page starts above where the "
                + "layers before it end, written under them; in a shape's panel in a table cell; among several "
                + "layers written with no place of their own, not measured; any other is written",
                "zIndex:GAP:" + DRAWING_OVER_TEXT
                + "; as the order Word stacks the drawings in, among those behind the text and among "
                + "those in front of it");
        value(com.demcha.compose.document.node.LayerAlign.class,
                "TOP_LEFT:REPORTED:on a layer after the first a shape's panel in a table cell writes, where that "
                + "one is set lower: the cell sets all it holds at the first one's height; any other is written, "
                + "where the layout places it",
                "TOP_CENTER:GAP:" + COMPOSED_STACK_LAYER
                + "; across, with no layout, written from the left, and in a shape's panel in a "
                + "table cell where the layout gives no box to set it across by, from the shape's left; there, on "
                + "a layer after the first written, where that one is set lower, at its height; any other is "
                + "written",
                "TOP_RIGHT:GAP:" + COMPOSED_STACK_LAYER
                + "; across, with no layout, written from the left, and in a shape's panel in a "
                + "table cell where the layout gives no box to set it across by, from the shape's left; there, on "
                + "a layer after the first written, where that one is set lower, at its height; any other is "
                + "written",
                "CENTER_LEFT:GAP:" + COMPOSED_STACK_LAYER
                + "; on a layer after the first a shape's panel in a table cell writes, where "
                + "that one is set at another height: the cell sets all it holds at the first one's height; any "
                + "other is written, where the layout places it",
                "CENTER:GAP:" + COMPOSED_STACK_LAYER
                + "; across, with no layout, written from the left, and in a shape's panel in a table "
                + "cell where the layout gives no box to set it across by, from the shape's left; there, on a layer "
                + "after the first written, where that one is set at another height, at its height; any other is "
                + "written",
                "CENTER_RIGHT:GAP:" + COMPOSED_STACK_LAYER
                + "; across, with no layout, written from the left, and in a shape's panel in a "
                + "table cell where the layout gives no box to set it across by, from the shape's left; there, on "
                + "a layer after the first written, where that one is set at another height, at its height; any "
                + "other is written",
                "BOTTOM_LEFT:GAP:" + COMPOSED_STACK_LAYER
                + "; on a layer after the first a shape's panel in a table cell writes, where "
                + "that one is set higher: the cell sets all it holds at the first one's height; any other is "
                + "written, where the layout places it",
                "BOTTOM_CENTER:GAP:" + COMPOSED_STACK_LAYER
                + "; across, with no layout, written from the left, and in a shape's panel in a "
                + "table cell where the layout gives no box to set it across by, from the shape's left; there, on "
                + "a layer after the first written, where that one is set higher, at its height; any other is "
                + "written",
                "BOTTOM_RIGHT:GAP:" + COMPOSED_STACK_LAYER
                + "; across, with no layout, written from the left, and in a shape's panel in a "
                + "table cell where the layout gives no box to set it across by, from the shape's left; there, on "
                + "a layer after the first written, where that one is set higher, at its height; any other is "
                + "written");
        value(com.demcha.compose.document.style.ShapePoint.class,
                "x:WRITTEN:as its share of the drawn shape's width, in its custom geometry or a picture's path",
                "y:WRITTEN:as its share of the drawn shape's height, up from its foot, in its custom geometry or a "
                + "picture's path");
        value(com.demcha.compose.document.style.ShapeOutline.Rectangle.class,
                "width:GAP:" + PANEL_WIDER_THAN_ITS_CELL
                + "; as the drawn outline's, a panel's in a table cell, or an inline shape's picture's",
                "height:WRITTEN:as the drawn outline's, a panel's in a table cell, or an inline shape's picture's");
        value(com.demcha.compose.document.style.ShapeOutline.RoundedRectangle.class,
                "width:GAP:" + PANEL_WIDER_THAN_ITS_CELL
                + "; as the drawn outline's, a panel's in a table cell, or an inline shape's picture's",
                "height:WRITTEN:as the drawn outline's, a panel's in a table cell, or an inline shape's picture's",
                "cornerRadius:REPORTED:as a panel in a table cell, its corners square; any other is written, held "
                + "to half the shorter side as the page holds it");
        value(com.demcha.compose.document.style.ShapeOutline.RoundedRectanglePerCorner.class,
                "width:GAP:" + PANEL_WIDER_THAN_ITS_CELL
                + "; as the drawn outline's, a panel's in a table cell, or an inline shape's picture's",
                "height:WRITTEN:as the drawn outline's, a panel's in a table cell, or an inline shape's picture's",
                "corners:REPORTED:drawn as a shape, radii that differ once held to half its shorter side, drawn at "
                + "the largest of them; as a panel in a table cell, square; any other is written, an inline "
                + "shape's picture rounding each as the page does");
        value(com.demcha.compose.document.style.ShapeOutline.Ellipse.class,
                "width:WRITTEN:as the drawn ellipse's, or an inline shape's picture's",
                "height:WRITTEN:as the drawn ellipse's, or an inline shape's picture's");
        value(com.demcha.compose.document.style.ShapeOutline.Polygon.class,
                "width:WRITTEN:as the drawn outline's, or an inline shape's picture's",
                "height:WRITTEN:as the drawn outline's, or an inline shape's picture's",
                "points:WRITTEN:as the drawn outline's custom geometry, or an inline shape's picture");
        value(com.demcha.compose.document.style.ShapeOutline.Path.class,
                "width:WRITTEN:as the drawn outline's, or an inline shape's picture's",
                "height:WRITTEN:as the drawn outline's, or an inline shape's picture's",
                "segments:WRITTEN:as the drawn outline's custom geometry, or an inline shape's picture");
        value(com.demcha.compose.document.style.DocumentPathSegment.MoveTo.class,
                "x:WRITTEN:as a move in the drawn custom geometry, or a picture's path",
                "y:WRITTEN:as a move in the drawn custom geometry, or a picture's path");
        value(com.demcha.compose.document.style.DocumentPathSegment.LineTo.class,
                "x:WRITTEN:as a line in the drawn custom geometry, or a picture's path",
                "y:WRITTEN:as a line in the drawn custom geometry, or a picture's path");
        value(com.demcha.compose.document.style.DocumentPathSegment.CubicTo.class,
                "control1X:WRITTEN:as a curve in the drawn custom geometry, or a picture's path",
                "control1Y:WRITTEN:as a curve in the drawn custom geometry, or a picture's path",
                "control2X:WRITTEN:as a curve in the drawn custom geometry, or a picture's path",
                "control2Y:WRITTEN:as a curve in the drawn custom geometry, or a picture's path",
                "x:WRITTEN:as a curve in the drawn custom geometry, or a picture's path",
                "y:WRITTEN:as a curve in the drawn custom geometry, or a picture's path");
        value(com.demcha.compose.document.style.DocumentPathSegment.Close.class);
        value(com.demcha.compose.document.svg.SvgIcon.class,
                "layers:REPORTED:drawn into the icon's picture, a gradient flat in its first colour, which the "
                + "icon's note names; any other is written",
                "sourceWidth:WRITTEN:as the scale its layers' stroke widths and dash lengths are drawn at",
                "sourceHeight:INERT:the page draws an icon to its run's height, which a builder may size from "
                + "this; the export reads the run's",
                "text:REPORTED:written as the picture's description, not a character in the line, as the icon's "
                + "note says");
        value(com.demcha.compose.document.svg.SvgIcon.Layer.class,
                "geometry:WRITTEN:drawn into the icon's picture",
                "fill:WRITTEN:drawn into the icon's picture, alpha included",
                "fillPaint:REPORTED:a gradient, drawn flat in its first colour, which the icon's note names; a "
                + "solid one is written",
                "stroke:WRITTEN:drawn into the icon's picture, its width scaled to the icon",
                "strokePaint:REPORTED:a gradient, drawn flat in its first colour, which the icon's note names; a "
                + "solid one is written",
                "lineCap:WRITTEN:drawn into the icon's picture",
                "lineJoin:WRITTEN:drawn into the icon's picture",
                "dashArray:WRITTEN:drawn into the icon's picture, its lengths scaled to the icon",
                "clip:WRITTEN:the layer is clipped to it in the icon's picture");
        value(com.demcha.compose.document.svg.SvgPath.class,
                "segments:WRITTEN:drawn into an icon's picture, as a layer's outline or its clip",
                "sourceWidth:INERT:the size of the art the path was read from: a layer is drawn to its icon's box, "
                + "and neither the page nor the export reads it",
                "sourceHeight:INERT:the size of the art the path was read from: a layer is drawn to its icon's "
                + "box, and neither the page nor the export reads it");
        value(com.demcha.compose.document.node.ShapeLayer.class,
                "outline:WRITTEN:drawn into the inline shape's picture, centred in its run's box",
                "fill:WRITTEN:drawn into the inline shape's picture, alpha included",
                "stroke:WRITTEN:drawn into the inline shape's picture at its width, mitred as on the page");
        value(com.demcha.compose.document.chart.ChartStyle.class,
                "palette:REPORTED:in the chart's note",
                "seriesPaintOverrides:REPORTED:in the chart's note",
                "lineWidth:REPORTED:in the chart's note",
                "barCornerRadius:REPORTED:in the chart's note",
                "barWidthRatio:REPORTED:in the chart's note",
                "grid:REPORTED:in the chart's note",
                "pointMarker:REPORTED:in the chart's note",
                "valueLabelOffset:REPORTED:in the chart's note",
                "axisTextStyle:REPORTED:in the chart's note",
                "legendTextStyle:REPORTED:in the chart's note",
                "valueLabelTextStyle:REPORTED:in the chart's note",
                "valueLabelHalo:REPORTED:in the chart's note",
                "areaOpacity:REPORTED:in the chart's note",
                "sliceStroke:REPORTED:in the chart's note",
                "sliceGapDegrees:REPORTED:in the chart's note",
                "donutCenterTextStyle:REPORTED:in the chart's note");
        value(com.demcha.compose.document.chart.ChartStyle.GridStyle.class,
                "horizontal:REPORTED:in the chart's note",
                "vertical:REPORTED:in the chart's note");
        value(com.demcha.compose.document.chart.ChartSpec.Bar.class,
                "data:WRITTEN:as the chart's data table, a row a category and a column a series",
                "horizontal:REPORTED:in the chart's note",
                "grouping:REPORTED:in the chart's note; each series' values are written either way",
                "valueAxis:REPORTED:its format is written, as the data table's value format; its bounds, grid lines "
                + "and tick labels, in the chart's note",
                "legend:REPORTED:in the chart's note; the series' names are written as the data table's header",
                "valueLabels:REPORTED:in the chart's note; the values are written in the data table",
                "size:REPORTED:in the chart's note; the data table is as tall as its rows",
                "showCategoryLabels:REPORTED:in the chart's note; the categories are written in the data table "
                + "either way");
        value(com.demcha.compose.document.chart.ChartSpec.Line.class,
                "data:WRITTEN:as the chart's data table, a row a category and a column a series",
                "interpolation:REPORTED:in the chart's note",
                "area:REPORTED:in the chart's note",
                "valueAxis:REPORTED:its format is written, as the data table's value format; its bounds, grid lines "
                + "and tick labels, in the chart's note",
                "legend:REPORTED:in the chart's note; the series' names are written as the data table's header",
                "valueLabels:REPORTED:in the chart's note; the values are written in the data table",
                "size:REPORTED:in the chart's note; the data table is as tall as its rows",
                "showCategoryLabels:REPORTED:in the chart's note; the categories are written in the data table "
                + "either way");
        value(com.demcha.compose.document.chart.ChartSpec.Pie.class,
                "data:WRITTEN:as the chart's data table, a row a category",
                "donutRatio:REPORTED:in the chart's note",
                "startAngleDegrees:REPORTED:in the chart's note",
                "clockwise:REPORTED:in the chart's note",
                "sliceLabels:REPORTED:in the chart's note; the values and categories are written in the data "
                + "table, the shares not",
                "valueFormat:WRITTEN:as the data table's value format",
                "percentFormat:REPORTED:in the chart's note; the shares it formats are only drawn",
                "centerText:REPORTED:in the chart's note; the text in the donut's hole is only drawn, and is not "
                + "in the data table",
                "legend:REPORTED:in the chart's note; the categories it lists are written in the data table",
                "size:REPORTED:in the chart's note; the data table is as tall as its rows");
        value(com.demcha.compose.document.chart.PointMarker.class,
                "width:REPORTED:in the chart's note",
                "height:REPORTED:in the chart's note",
                "fill:REPORTED:in the chart's note",
                "stroke:REPORTED:in the chart's note");
        value(com.demcha.compose.document.table.DocumentTableStyle.class,
                "padding:GAP:" + CELL_PADDING + "; " + PADDING_BELOW_ZERO
                + "; a text cell's thinner than its rules' share, which Word makes room for, where its "
                + "row then stands taller than the page's, named with the rules; any other is written, less "
                + "that share",
                "fillColor:REPORTED:its translucency, flattened against the colour under the cell; any other is "
                + "written",
                "stroke:REPORTED:its translucency, flattened against the cell's fill or the colour under it; a width "
                + "that rounds below a quarter of a point or past 12pt, drawn at Word's thinnest or widest; the "
                + "room Word makes for it past a text cell's thinner padding, where its row stands taller; any "
                + "other is written, one of no width as no border",
                "textStyle:WRITTEN:a text cell's; the page sets no cell's face on what a composed cell holds",
                "textAnchor:WRITTEN:its height in every cell, its side in a text cell; the page sets what a "
                + "composed cell holds by its height alone",
                "lineSpacing:WRITTEN:a text cell's, as the space after each of its lines but the last; the page "
                + "does not apply it to what a composed cell holds",
                "direction:WRITTEN:a text cell's; the page does not apply it to what a composed cell holds");
        value(com.demcha.compose.document.table.DocumentTableColumn.class,
                "type:REPORTED:where a table has an auto one whose width the layout does not show — composed in a "
                + "table cell, or with a column edge no row shows — named, its columns the editor's; with no "
                + "layout, in the section's note; any other is written",
                "fixedWidth:REPORTED:beside an auto column, where the layout does not show the widths, not "
                + "written, named with the table; with no layout, in the section's note; any other is written");
        value(com.demcha.compose.document.table.DocumentTableColumn.Type.class,
                "AUTO:REPORTED:where the layout does not show its width — composed in a table cell, or with a "
                + "column edge no row shows — the editor's, named; with no layout, in the section's note; any "
                + "other is written, as the layout measured it",
                "FIXED:REPORTED:beside an auto column, where the layout does not show the widths, its width not "
                + "written, named with the table; with no layout, in the section's note; any other is written");
        value(com.demcha.compose.document.table.DocumentTableTextAnchor.class,
                "DEFAULT:WRITTEN:at the cell's foot and its left, where the page sets it",
                "CENTER_LEFT:WRITTEN", "CENTER:WRITTEN", "CENTER_RIGHT:WRITTEN",
                "TOP_LEFT:WRITTEN", "TOP_RIGHT:WRITTEN", "BOTTOM_LEFT:WRITTEN", "BOTTOM_RIGHT:WRITTEN");
        value(com.demcha.compose.document.chart.ChartData.class,
                "categories:WRITTEN:as the first column of the chart's data table",
                "series:WRITTEN:as the chart's data table's other columns, one a series");
        value(com.demcha.compose.document.chart.ChartData.Series.class,
                "name:WRITTEN:as its column's header in the chart's data table",
                "values:WRITTEN:in the chart's data table, in its value format, a gap as an empty cell");
        value(com.demcha.compose.document.chart.BarGrouping.class,
                "GROUPED:REPORTED:in the chart's note; each series' values are written",
                "STACKED:REPORTED:in the chart's note; each series' values are written, not a stack's total");
        value(com.demcha.compose.document.chart.AxisSpec.class,
                "baselineAtZero:REPORTED:in the chart's note",
                "min:REPORTED:in the chart's note",
                "max:REPORTED:in the chart's note",
                "format:WRITTEN:as the chart's data table's value format",
                "showGridLines:REPORTED:in the chart's note",
                "showTickLabels:REPORTED:in the chart's note");
        value(com.demcha.compose.document.chart.LegendPosition.class,
                "NONE:INERT:the page draws no legend; the names one lists are written in the data table all "
                + "the same",
                "BOTTOM:REPORTED:in the chart's note; the names it lists are written in the data table",
                "RIGHT:REPORTED:in the chart's note; the names it lists are written in the data table",
                "TOP:REPORTED:in the chart's note; the names it lists are written in the data table");
        value(com.demcha.compose.document.chart.ValueLabelMode.class,
                "NONE:INERT:the page draws no value labels",
                "OUTSIDE:REPORTED:in the chart's note; the values are written in the data table",
                "INSIDE:INERT:the page lays out no chart that asks for it");
        value(com.demcha.compose.document.chart.LineInterpolation.class,
                "LINEAR:REPORTED:in the chart's note",
                "SMOOTH:REPORTED:in the chart's note",
                "MONOTONE:REPORTED:in the chart's note");
        value(com.demcha.compose.document.chart.SliceLabelMode.class,
                "NONE:INERT:the page draws no slice labels",
                "VALUE:REPORTED:in the chart's note; the values are written in the data table",
                "PERCENT:REPORTED:in the chart's note; the shares are not written, only the values",
                "CATEGORY:REPORTED:in the chart's note; the categories are written in the data table",
                "CATEGORY_PERCENT:REPORTED:in the chart's note; the categories are written in the data table, "
                + "the shares not");
        value(com.demcha.compose.document.chart.NumberFormatSpec.class,
                "pattern:REPORTED:as a pie's percent format, in the chart's note; as a chart's value format, an "
                + "axis's or a pie's, written in its data table's values",
                "locale:REPORTED:as a pie's percent format, in the chart's note; as a chart's value format, an "
                + "axis's or a pie's, written in its data table's values",
                "prefix:REPORTED:as a pie's percent format, in the chart's note; as a chart's value format, an "
                + "axis's or a pie's, written in its data table's values",
                "suffix:REPORTED:as a pie's percent format, in the chart's note; as a chart's value format, an "
                + "axis's or a pie's, written in its data table's values",
                "scaleDivisor:REPORTED:as a pie's percent format, in the chart's note; as a chart's value format, "
                + "an axis's or a pie's, written in its data table's values");
        value(com.demcha.compose.document.table.DocumentTableCell.class,
                "lines:WRITTEN:a text cell's, a line each; a composed cell's are read neither by the page nor by "
                + "the export",
                "style:GAP:" + CELL_PADDING + "; " + PADDING_BELOW_ZERO
                + "; as the cell style's parts say: the translucency of its fill and its rules, a rule's "
                + "width Word does not draw, and the room Word makes for its rules past a text cell's thinner "
                + "padding; any other is written",
                "colSpan:WRITTEN",
                "rowSpan:WRITTEN",
                "content:WRITTEN:as its own node's entries say");
        value(com.demcha.compose.document.chart.ChartSize.Fixed.class,
                "points:REPORTED:in the chart's note; the data table is as tall as its rows");
        value(com.demcha.compose.document.chart.ChartSize.AspectRatio.class,
                "w:REPORTED:in the chart's note; the data table is as tall as its rows",
                "h:REPORTED:in the chart's note; the data table is as tall as its rows");
        value(com.demcha.compose.document.output.DocumentMetadata.class,
                "author:WRITTEN:as the file's creator",
                "creator:WRITTEN:as the application the file names as the one that made it",
                "keywords:WRITTEN",
                "producer:REPORTED:one of the author's own beside a creator other than it, which a Word file has "
                + "no field for; with no creator, or the same as it, written as the application; the one every "
                + "metadata starts with is not the author's, and not named",
                "subject:WRITTEN",
                "title:WRITTEN");
        value(com.demcha.compose.document.output.DocumentWatermark.class,
                "color:REPORTED", "fontSize:REPORTED", "imageBytes:REPORTED", "imagePath:REPORTED",
                "layer:REPORTED", "opacity:REPORTED", "position:REPORTED", "rotation:REPORTED", "text:REPORTED");
        value(com.demcha.compose.document.output.DocumentProtection.class,
                "canAssemble:REPORTED", "canCopyContent:REPORTED", "canExtractForAccessibility:REPORTED",
                "canFillForms:REPORTED", "canModify:REPORTED", "canPrint:REPORTED", "canPrintHighQuality:REPORTED",
                "keyLength:REPORTED", "ownerPassword:REPORTED", "userPassword:REPORTED");
        value(com.demcha.compose.document.output.DocumentViewerPreferences.class,
                "centerWindow:REPORTED", "displayDocTitle:REPORTED", "fitWindow:REPORTED", "hideMenubar:REPORTED",
                "hideToolbar:REPORTED", "pageLayout:REPORTED", "pageMode:REPORTED");
        value(com.demcha.compose.document.output.DocumentWatermarkLayer.class,
                "ABOVE_CONTENT:REPORTED", "BEHIND_CONTENT:REPORTED");
        value(com.demcha.compose.document.output.DocumentWatermarkPosition.class,
                "BOTTOM_LEFT:REPORTED", "BOTTOM_RIGHT:REPORTED", "CENTER:REPORTED", "TILE:REPORTED",
                "TOP_LEFT:REPORTED", "TOP_RIGHT:REPORTED");
        value(com.demcha.compose.document.style.DocumentPageMode.class,
                "FULL_SCREEN:REPORTED", "USE_ATTACHMENTS:REPORTED", "USE_NONE:REPORTED", "USE_OUTLINES:REPORTED",
                "USE_THUMBNAILS:REPORTED");
        value(com.demcha.compose.document.style.DocumentPageLayout.class,
                "ONE_COLUMN:REPORTED", "SINGLE_PAGE:REPORTED", "TWO_COLUMN_LEFT:REPORTED", "TWO_COLUMN_RIGHT:REPORTED",
                "TWO_PAGE_LEFT:REPORTED", "TWO_PAGE_RIGHT:REPORTED");
        value(com.demcha.compose.document.output.DocumentHeaderFooter.class,
                "centerText:REPORTED:where it and another slot overlap where the page sets them, or it is wider "
                + "than the line, which Word sets apart or wraps; any other is written, its page tokens as "
                + "Word's page fields",
                "fontName:REPORTED:a face that may not travel or cannot be read, named as an embedded font; any "
                + "other is written",
                "fontSize:WRITTEN",
                "height:REPORTED:a band too shallow for its line, whose text and separator Word stands further in "
                + "from the edge than the page; any other is written, as where its line and separator stand "
                + "from the edge",
                "leftText:REPORTED:where it and another slot overlap where the page sets them, or it is wider "
                + "than the line, which Word sets apart or wraps; any other is written, its page tokens as "
                + "Word's page fields",
                "numbering:REPORTED:a count from past page 2, a start other than the page it counts from, and page "
                + "numbers in letters from 28 on; any other is written",
                "reserveSpace:WRITTEN:as the page margin the session widens to the band's height",
                "rightText:REPORTED:where it and another slot overlap where the page sets them, or it is wider "
                + "than the line, which Word sets apart or wraps; any other is written, its page tokens as "
                + "Word's page fields",
                "separatorColor:REPORTED:translucent, flattened against white; any other is written, a wholly "
                + "transparent one as no separator",
                "separatorThickness:REPORTED:a width that, rounded to the eighth of a point, is below a quarter of "
                + "a point or past 12pt, drawn at Word's thinnest or widest; one of no width, at Word's thinnest; "
                + "one below zero or not a number, not written; any other is written",
                "showSeparator:REPORTED:in a band too shallow for its line, ruled further in from the edge than the "
                + "page strokes it; any other is written, as its line's border",
                "textColor:WRITTEN:its alpha as Word's text fill",
                "zone:WRITTEN");
        value(com.demcha.compose.document.output.DocumentPageZone.class,
                "appliesTo:REPORTED:one answering otherwise for two pages of a kind Word has a part for — the "
                + "first, even, odd — or refusing a page, written on every page; one telling even from odd, a "
                + "blank page Word adds before a later section; any other is written",
                "content:REPORTED:what a header or footer cannot hold of it, as the zones entry names; built "
                + "otherwise for the last page than for those between, written there as for them; built as "
                + "nothing where the layout draws it, not written; any other is written",
                "height:REPORTED:where the layout shows none of the zone's text or pictures, or shows them built "
                + "otherwise, its line Word's own, not measured; any other is written, through where the layout "
                + "sets its content, and as the page margin where it reserves its height",
                "padding:REPORTED:at its sides, where it stands a part off the place Word sets it on the zone's "
                + "line, and where the line is not measured; any other is written, where its line stands",
                "reserveSpace:WRITTEN:as the page margin the session widens to the zone's height",
                "zone:WRITTEN");
        value(com.demcha.compose.document.output.DocumentHeaderFooterZone.class,
                "FOOTER:WRITTEN", "HEADER:WRITTEN");
        value(com.demcha.compose.document.output.DocumentPageNumbering.class,
                "countFrom:REPORTED:past page 2, which Word has no header or footer of its own for, written on "
                + "every page but the first; any other is written, from page 2 as Word's pages after the first",
                "showOnFirstPage:WRITTEN:off, as a first page whose header or footer holds nothing of the band",
                "startAt:REPORTED:other than the page it counts from, Word's page fields numbering the pages from 1 "
                + "and the total off by as much; any other is written",
                "style:REPORTED:in letters, a page number or total of 28 or more, which Word letters by repeating "
                + "one letter; any other is written, as its page fields' format");
        value(com.demcha.compose.document.output.DocumentPageNumberStyle.class,
                "DECIMAL:WRITTEN",
                "LOWER_ALPHA:REPORTED:a page number or total of 28 or more, which Word letters by repeating one "
                + "letter, 28 reading bb where the page reads ab; any other is written",
                "LOWER_ROMAN:WRITTEN",
                "UPPER_ALPHA:REPORTED:a page number or total of 28 or more, which Word letters by repeating one "
                + "letter, 28 reading BB where the page reads AB; any other is written",
                "UPPER_ROMAN:WRITTEN");
        value(com.demcha.compose.document.output.PageContext.class,
                "number:WRITTEN:asked of a zone's predicate as the page asks it, on the layout's pages and past "
                + "them on a sample of six; read by a zone's content, it is refused, stopping the export, as on "
                + "any semantic export: the content places a page field",
                "paginated:WRITTEN:true where a zone's predicate is asked, as the page asks it; false for the "
                + "content written, as any semantic export builds it, so content built otherwise for one is "
                + "written as built for it",
                "total:WRITTEN:asked of a zone's predicate as the page asks it, the layout's page count, six for "
                + "the kinds of page a shorter layout lacks; read by a zone's content, it is refused, stopping "
                + "the export, as on any semantic export: the content places a page total");
    }

    @Test
    void everyValueTypeANodeOrAnOutputOptionHoldsHasAnEntry() throws Exception {
        assertThat(names(reach().parts().keySet()))
                .as("the value types a node's field or an output option holds: a type with no entry has "
                    + "parts nobody decided the DOCX export's answer for")
                .containsExactlyInAnyOrderElementsOf(names(VALUES.keySet()));
    }

    @Test
    void everyPartOfEveryValueTypeHasAnEntry() throws Exception {
        Map<String, Set<String>> undecided = new TreeMap<>();
        Map<String, Set<String>> stale = new TreeMap<>();
        Map<Class<?>, Set<String>> types = reach().parts();
        VALUES.forEach((type, entries) -> {
            Set<String> parts = types.getOrDefault(type, Set.of());
            Set<String> missing = new TreeSet<>(parts);
            missing.removeAll(entries.keySet());
            Set<String> extra = new TreeSet<>(entries.keySet());
            extra.removeAll(parts);
            if (!missing.isEmpty()) {
                undecided.put(type.getName(), missing);
            }
            if (!extra.isEmpty()) {
                stale.put(type.getName(), extra);
            }
        });
        assertThat(undecided).as("parts with no entry: decide whether the DOCX export writes them, reports "
                                 + "them, or has nothing to carry, and say which here").isEmpty();
        assertThat(stale).as("entries for parts the type no longer has").isEmpty();
    }

    @Test
    void noValueTypeIsOpenToKindsTheLedgerCannotList() throws Exception {
        // An interface that is not sealed, or a class that is not final, may be implemented or
        // extended anywhere, with parts of its own no entry here could name.
        assertThat(reach().open()).as("value types any class may implement or extend").isEmpty();
    }

    @Test
    void everyNodeAValueHoldsIsOneTheNodeLedgerLists() throws Exception {
        // A node held in another package would be in neither ledger.
        Set<String> listed = names(DocxNodeFieldLedgerTest.nodeClasses());
        listed.add(DocumentNode.class.getName());
        assertThat(names(reach().nodes())).as("node types a field holds that the node ledger does not list")
                .contains(DocumentNode.class.getName())
                .isSubsetOf(listed);
    }

    private static void value(Class<?> type, String... specs) {
        if (VALUES.put(type, DocxNodeFieldLedgerTest.fields(specs)) != null) {
            throw new IllegalStateException("two entries for " + type.getName());
        }
    }

    private static Set<String> names(java.util.Collection<Class<?>> types) {
        return types.stream().map(Class::getName).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * What a node's fields and the output options hold, down to the last value.
     *
     * @param parts every GraphCompose value type held, with its parts: a record's components, a
     *              final class's instance fields and its superclasses', an enum's constants
     * @param open  the types held that any class may implement or extend
     * @param nodes the node types held, which are the node ledger's
     */
    record Reach(Map<Class<?>, Set<String>> parts, Set<String> open, Set<Class<?>> nodes) {
    }

    /** Walks the types a node's fields and the output options hold. A sealed type is read as the kinds it permits. */
    static Reach reach() throws Exception {
        Deque<Type> queue = new ArrayDeque<>();
        for (Class<?> kind : DocxNodeFieldLedgerTest.nodeClasses()) {
            for (RecordComponent component : kind.getRecordComponents()) {
                queue.add(component.getGenericType());
            }
        }
        for (RecordComponent component : DocumentOutputOptions.class.getRecordComponents()) {
            queue.add(component.getGenericType());
        }
        Map<Class<?>, Set<String>> parts = new LinkedHashMap<>();
        Set<String> open = new TreeSet<>();
        Set<Class<?>> nodes = new LinkedHashSet<>();
        // A type read once: a bound that names its own variable would be read for ever.
        Set<Type> seen = new HashSet<>();
        while (!queue.isEmpty()) {
            Type type = queue.poll();
            if (!seen.add(type)) {
                continue;
            }
            if (type instanceof ParameterizedType parameterized) {
                queue.add(parameterized.getRawType());
                queue.addAll(Arrays.asList(parameterized.getActualTypeArguments()));
                continue;
            }
            if (type instanceof WildcardType wildcard) {
                queue.addAll(Arrays.asList(wildcard.getUpperBounds()));
                queue.addAll(Arrays.asList(wildcard.getLowerBounds()));
                continue;
            }
            if (type instanceof GenericArrayType array) {
                queue.add(array.getGenericComponentType());
                continue;
            }
            if (type instanceof TypeVariable<?> variable) {
                queue.addAll(Arrays.asList(variable.getBounds()));
                continue;
            }
            if (!(type instanceof Class<?> raw)) {
                continue;
            }
            if (raw.isArray()) {
                queue.add(raw.getComponentType());
                continue;
            }
            if (!raw.getName().startsWith("com.demcha.compose.")) {
                continue;
            }
            if (DocumentNode.class.isAssignableFrom(raw)) {
                nodes.add(raw);
                continue;
            }
            Set<String> own = new TreeSet<>();
            if (raw.isEnum()) {
                Arrays.stream(raw.getEnumConstants()).map(constant -> ((Enum<?>) constant).name()).forEach(own::add);
            } else if (raw.isRecord()) {
                for (RecordComponent component : raw.getRecordComponents()) {
                    own.add(component.getName());
                    queue.add(component.getGenericType());
                }
            } else if (raw.isSealed()) {
                queue.addAll(Arrays.asList(raw.getPermittedSubclasses()));
                continue;
            } else if (raw.isInterface() || !Modifier.isFinal(raw.getModifiers())) {
                open.add(raw.getName());
                continue;
            } else {
                // A final class's state is its own fields and those it inherits.
                for (Class<?> level = raw; level != null && level != Object.class; level = level.getSuperclass()) {
                    for (Field field : level.getDeclaredFields()) {
                        if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                            own.add(field.getName());
                            queue.add(field.getGenericType());
                        }
                    }
                }
            }
            parts.put(raw, own);
        }
        return new Reach(parts, open, nodes);
    }
}
