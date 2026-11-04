package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHeight;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.w3c.dom.Node;

import java.awt.Color;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a timeline's rail as the left border of the cells it runs along, so that it stretches with
 * the entries when they are edited.
 *
 * <p>A timeline whose entries are each a row alone is written as a table of one row per entry, the
 * entries standing one under the other with an empty paragraph of the gap's height between them. Its
 * rail, drawn as a filled bar of the length the layout gives it, keeps that length in the editor: an
 * entry that grows leaves the entries after it running on past the bar's foot. Here the entries'
 * tables become one table, each gap a row of its own of the gap's exact height, and the column the rail
 * runs down is cut where the rail stands, the cell to the right of the cut carrying the rail as its left
 * border in every row. A rail beside the markers, in the gutter left of the entries, gets a column of its
 * own there. Where the rail starts or ends at a marker rather than at the entry's edge, that entry's row is
 * cut at the rail's end into a row of that exact height and the rest, the other cells merged across both,
 * and only the part along the rail is bordered.</p>
 *
 * <p>Where the column is cut is read off where the drawn rail was placed in its cell, not off the layout's
 * row: the export indents some timelines other than the layout places the row, and the drawn rail stands
 * where the cells are written. Where a row is cut is the layout's distance from the row's top to the rail's
 * end, plus what the export lifted the row into the gap above it.</p>
 *
 * <p>Each gap is a row and not a margin of the row above: of two cells merged down two rows, Word reads
 * a cell's bottom margin off the lower one and LibreOffice off the upper one, so a margin on either put the
 * gap below the first entry wrong in the other editor. The upper row of a cut entry keeps with the lower one:
 * a page breaking between them left the entry's marker and text at the foot of one page in Word, the rail
 * starting above nothing on the next, and in LibreOffice started the rail above the marker.</p>
 *
 * <p>The tables are taken as written, at the end of the export: the rewrite is done on the document's
 * XML once nothing else will read the tables it removes. Anything other than what is described here —
 * a table of other than one row, a cell spanning or merged, anything between two entries but empty
 * paragraphs of an exact height, tables of different grids or properties, a part of the rail not written
 * as a drawing, a drawn rail placed from no cell of the first entry, a rail beside the markers of a table
 * nested in a cell (which stays in its cell whatever its indent), a rail ending on another page than its
 * entry, a see-through rail (a border has no translucency), text in the column the rail cuts, one entry
 * the rail both starts and ends inside — leaves the timeline as it is, its rail drawn.</p>
 *
 * <p>An entry whose row's paragraphs all keep with the next, and a gap whose paragraph did, keep every
 * row they become with the next, so a timeline kept together stays so.</p>
 */
final class DocxTimelineRails {

    /** How far apart two edges may stand and still be one: the layout's sums round. */
    private static final double EDGE = 0.05;
    private static final double TWIPS_PER_POINT = 20.0;
    /** The WordprocessingML namespace, which the elements read off the document's DOM are in. */
    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    private DocxTimelineRails() {
    }

    /**
     * A timeline to write so, gathered while its section is written.
     *
     * @param tables      the tables its entries were written as, in order
     * @param entries     where those entries' rows stand as written: where the layout put them, a row's
     *                    top raised by what the export lifted it into the gap above
     * @param railTop     the rail's top, measured up from the page's foot, on {@code topPage}
     * @param topPage     the page the rail starts on
     * @param railBottom  the rail's foot, measured the same way, on {@code bottomPage}
     * @param bottomPage  the page the rail ends on
     * @param colour      the rail's colour
     * @param width       the rail's width, in points
     * @param bars        the drawings the rail was written as, one a page
     */
    record Plan(List<XWPFTable> tables, List<DocxLayoutMetrics.RailedEntry> entries,
                double railTop, int topPage, double railBottom, int bottomPage, Color colour, double width,
                List<CTDrawing> bars) {
    }

    /**
     * Writes a timeline's rail as the border of its cells, and removes the drawing it was written as.
     *
     * @param plan the timeline
     * @return true where it was written so; false where the timeline was left as it was, its rail drawn
     */
    static boolean apply(Plan plan) {
        List<CTTbl> tables = new ArrayList<>();
        for (XWPFTable table : plan.tables()) {
            tables.add(table.getCTTbl());
        }
        List<Gap> gaps = gapsBetween(tables);
        if (gaps == null || plan.bars().isEmpty() || plan.bars().stream().anyMatch(bar -> bar == null
                || bar.getDomNode().getParentNode() == null)) {
            return false;
        }
        // A border has no translucency: a rail drawn see-through stays drawn, as its note says.
        if (plan.colour().getAlpha() < 255) {
            return false;
        }
        int[] grid = gridOf(tables.get(0));
        String properties = propertiesOf(tables.get(0));
        if (java.util.Arrays.stream(grid).anyMatch(width -> width <= 0)) {
            return false;
        }
        for (CTTbl table : tables) {
            if (table.sizeOfTrArray() != 1 || !java.util.Arrays.equals(gridOf(table), grid)
                || !properties.equals(propertiesOf(table)) || !plainCells(table.getTrArray(0), grid.length)) {
                return false;
            }
        }
        DocxLayoutMetrics.RailedEntry first = plan.entries().get(0);
        DocxLayoutMetrics.RailedEntry last = plan.entries().get(plan.entries().size() - 1);
        // Where the rail starts below its first entry's top, and ends above its last entry's foot.
        double above = first.top() - plan.railTop();
        double below = plan.railBottom() - last.bottom();
        if (plan.topPage() != first.page() || plan.bottomPage() != last.page()
            || above < -EDGE || below < -EDGE
            || above > EDGE && below > EDGE && plan.entries().size() == 1) {
            return false;
        }

        // Where the column is cut is read off the drawn rail's place in its cell: the layout's row
        // can stand elsewhere than the table is written (TerracottaRail's education rail, read off the
        // layout, stood 2.8pt right). A rail placed from no cell of its first entry is left drawn.
        Integer offset = offsetOfTheBar(plan.bars().get(0), tables.get(0), plan.width());
        if (offset == null) {
            return false;
        }
        int column;
        int cut;
        if (offset < 0) {
            // A table nested in a cell stays inside that cell whatever its indent, so the column the
            // rail would need left of the entries would push them right instead.
            Node parent = tables.get(0).getDomNode().getParentNode();
            if (parent == null || "tc".equals(parent.getLocalName())) {
                return false;
            }
            column = -1;
            cut = -offset;
        } else {
            column = 0;
            int start = 0;
            while (column < grid.length && start + grid[column] <= offset) {
                start += grid[column];
                column++;
            }
            if (column == grid.length) {
                return false;
            }
            cut = offset - start;
            // The cell the rail stands on, in every entry: cut at its left edge it carries the border
            // itself and must hold nothing; cut inside, it keeps its content in the part left of the
            // rail, which a line of text would not keep where the page sets it.
            for (CTTbl table : tables) {
                CTTc cell = table.getTrArray(0).getTcArray(column);
                if (cut == 0 ? !empty(cell, plan.bars()) : holdsText(cell)) {
                    return false;
                }
            }
        }

        // Nothing is changed before this point. The bars go first: the rows are copied with them.
        for (CTDrawing bar : plan.bars()) {
            remove(bar);
        }
        List<CTRow> rows = new ArrayList<>();
        String colour = String.format("%02X%02X%02X", plan.colour().getRed(), plan.colour().getGreen(),
                plan.colour().getBlue());
        BigInteger eighths = BigInteger.valueOf(Math.max(2, Math.min(96, Math.round(plan.width() * 8))));
        for (int index = 0; index < tables.size(); index++) {
            CTRow row = (CTRow) tables.get(index).getTrArray(0).copy();
            // An entry kept with what follows — every paragraph of its row kept with the next, as a
            // timeline kept together keeps all but its last — keeps every row it becomes so.
            boolean kept = everyParagraphKeptWithTheNext(row);
            int from = rows.size();
            // An entry's row is one the page never breaks inside, as the row the layout placed it.
            // Its own table, of one row kept with the next, was let break for LibreOffice's sake
            // (letTheOneRowTablesKeptWithTheNextBreak); as a row among others it may not.
            CTTrPr whole = row.isSetTrPr() ? row.getTrPr() : row.addNewTrPr();
            if (whole.sizeOfCantSplitArray() == 0) {
                whole.addNewCantSplit();
            }
            int rail = cutAtTheRail(row, column, cut);
            boolean isFirst = index == 0;
            boolean isLast = index == tables.size() - 1;
            if (isFirst && above > EDGE) {
                rows.addAll(cutAt(row, rail, twips(above), false, colour, eighths));
            } else if (isLast && below > EDGE) {
                rows.addAll(cutAt(row, rail, twips(last.top() - plan.railBottom()), true, colour, eighths));
            } else {
                border(row.getTcArray(rail), colour, eighths);
                rows.add(row);
            }
            if (kept) {
                for (CTRow written : rows.subList(from, rows.size())) {
                    keepEveryParagraphWithTheNext(written);
                }
            }
            if (!isLast) {
                CTRow gap = gapRow(row, rail, gaps.get(index).height(), colour, eighths);
                // The gap paragraph it stands for kept with the next entry; so does the row.
                if (gaps.get(index).kept()) {
                    keepEveryParagraphWithTheNext(gap);
                }
                rows.add(gap);
            }
        }

        CTTbl table = tables.get(0);
        if (column < 0) {
            widenToTheLeft(table, cut);
        }
        setGrid(table, newGrid(grid, column, cut));
        table.removeTr(0);
        for (CTRow row : rows) {
            table.addNewTr().set(row);
        }
        for (int index = 1; index < tables.size(); index++) {
            removeBetween(tables.get(index - 1).getDomNode(), tables.get(index).getDomNode());
        }
        for (int index = 1; index < tables.size(); index++) {
            remove(tables.get(index));
        }
        return true;
    }

    /**
     * Where the rail's centre stands across its first entry's table, in twips from the table's left
     * edge, read off the bar it was drawn as: the bar's offset in the cell whose paragraph carries it,
     * and that cell's place in its row, a table nested in a cell adding its indent, out to the entry's
     * table. The bar is placed where the cells are written, so this is where the table stands in the
     * editor even where the export indents it other than the layout places the row.
     *
     * @return the offset, or null where the bar is not placed from a cell of that table
     */
    private static Integer offsetOfTheBar(CTDrawing bar, CTTbl entry, double width) {
        if (bar.sizeOfAnchorArray() != 1) {
            return null;
        }
        org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTPosH across =
                bar.getAnchorArray(0).getPositionH();
        if (across == null || !across.isSetPosOffset()
            || across.getRelativeFrom() != org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing
                    .STRelFromH.COLUMN) {
            return null;
        }
        long twips = Math.round(across.getPosOffset() / 635.0 + width * TWIPS_PER_POINT / 2);
        Node node = bar.getDomNode();
        boolean nested = false;
        while (true) {
            Node cell = enclosing(node, "tc");
            if (cell == null) {
                return null;
            }
            if (nested && leftMarginOf(cell) != 0) {
                // Where a table nested in a cell with a left margin stands is not measured.
                return null;
            }
            nested = true;
            for (Node before = cell.getPreviousSibling(); before != null; before = before.getPreviousSibling()) {
                if (before.getNodeType() == Node.ELEMENT_NODE && "tc".equals(before.getLocalName())) {
                    Node properties = child(before, "tcPr");
                    Node tcW = properties == null ? null : child(properties, "tcW");
                    Long cellWidth = tcW == null ? null : twipsOf(attribute(tcW, "w"));
                    if (cellWidth == null || child(properties, "gridSpan") != null) {
                        return null;
                    }
                    twips += cellWidth;
                }
            }
            Node table = enclosing(cell, "tbl");
            if (table == entry.getDomNode()) {
                return (int) twips;
            }
            Node properties = table == null ? null : child(table, "tblPr");
            Node indent = properties == null ? null : child(properties, "tblInd");
            if (indent != null) {
                Long by = twipsOf(attribute(indent, "w"));
                if (by == null) {
                    return null;
                }
                twips += by;
            }
            node = table;
        }
    }

    /** A cell's left margin in twips, 0 where it sets none; -1 where it sets one in other units. */
    private static long leftMarginOf(Node cell) {
        Node properties = child(cell, "tcPr");
        Node margins = properties == null ? null : child(properties, "tcMar");
        Node left = margins == null ? null : child(margins, "left");
        if (left == null) {
            return 0;
        }
        Long width = twipsOf(attribute(left, "w"));
        return width == null ? -1 : width;
    }

    /** Whether an on/off element is there and on: no value, or a value other than false, 0 or off. */
    private static boolean on(Node element) {
        if (element == null) {
            return false;
        }
        String value = attribute(element, "val");
        return value == null || !("false".equals(value) || "0".equals(value) || "off".equals(value));
    }

    /** The table's properties as written, to tell the entries' tables apart by. */
    private static String propertiesOf(CTTbl table) {
        return table.getTblPr() == null ? "" : table.getTblPr().xmlText();
    }

    /** Whether a cell holds a run of text, a field or a link — anything a reader reads. */
    private static boolean holdsText(CTTc cell) {
        Node node = cell.getDomNode();
        return hasDescendant(node, "t") || hasDescendant(node, "fldSimple") || hasDescendant(node, "instrText")
               || hasDescendant(node, "hyperlink") || cell.sizeOfTblArray() > 0;
    }

    private static boolean hasDescendant(Node node, String name) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE
                && (name.equals(child.getLocalName()) && W.equals(child.getNamespaceURI())
                    || hasDescendant(child, name))) {
                return true;
            }
        }
        return false;
    }

    /** Whether every paragraph a row's cells hold, directly, keeps with the next. */
    private static boolean everyParagraphKeptWithTheNext(CTRow row) {
        boolean any = false;
        for (CTTc cell : row.getTcArray()) {
            for (CTP paragraph : cell.getPArray()) {
                Node properties = child(paragraph.getDomNode(), "pPr");
                if (properties == null || !on(child(properties, "keepNext"))) {
                    return false;
                }
                any = true;
            }
        }
        return any;
    }

    /** Keeps every paragraph a row's cells hold, directly, with the next. */
    private static void keepEveryParagraphWithTheNext(CTRow row) {
        for (CTTc cell : row.getTcArray()) {
            for (CTP paragraph : cell.getPArray()) {
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr properties =
                        paragraph.isSetPPr() ? paragraph.getPPr() : paragraph.addNewPPr();
                if (!properties.isSetKeepNext()) {
                    properties.addNewKeepNext();
                }
            }
        }
    }

    /** The nearest ancestor element of a WordprocessingML name, or null. */
    private static Node enclosing(Node node, String name) {
        for (Node up = node == null ? null : node.getParentNode(); up != null; up = up.getParentNode()) {
            if (up.getNodeType() == Node.ELEMENT_NODE && name.equals(up.getLocalName())
                && W.equals(up.getNamespaceURI())) {
                return up;
            }
        }
        return null;
    }

    /**
     * A gap between two entries: its height in twips, and whether the paragraphs it was written as all
     * kept with the next entry.
     */
    private record Gap(int height, boolean kept) {
    }

    /**
     * The gaps between consecutive tables: each an empty paragraph of an exact height, or several, and
     * nothing else; null where the tables are not that.
     */
    private static List<Gap> gapsBetween(List<CTTbl> tables) {
        List<Gap> gaps = new ArrayList<>();
        for (int index = 0; index + 1 < tables.size(); index++) {
            Node from = tables.get(index).getDomNode();
            Node to = tables.get(index + 1).getDomNode();
            if (from.getParentNode() == null || from.getParentNode() != to.getParentNode()) {
                return null;
            }
            int gap = 0;
            boolean any = false;
            boolean kept = true;
            for (Node node = from.getNextSibling(); node != to; node = node.getNextSibling()) {
                if (node == null) {
                    return null;
                }
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Integer height = "p".equals(node.getLocalName()) ? exactHeightOfAnEmpty(node) : null;
                if (height == null) {
                    return null;
                }
                gap += height;
                any = true;
                Node properties = child(node, "pPr");
                kept &= properties != null && on(child(properties, "keepNext"));
            }
            if (!any) {
                return null;
            }
            gaps.add(new Gap(gap, kept));
        }
        if (!tables.isEmpty() && tables.get(0).getDomNode().getParentNode() == null) {
            return null;
        }
        return gaps;
    }

    /** The height of a paragraph holding nothing and set one exact line, spacing included; else null. */
    private static Integer exactHeightOfAnEmpty(Node paragraph) {
        for (Node child = paragraph.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && !"pPr".equals(child.getLocalName())) {
                return null;
            }
        }
        Node properties = child(paragraph, "pPr");
        Node spacing = properties == null ? null : child(properties, "spacing");
        if (spacing == null || child(properties, "sectPr") != null || child(properties, "pBdr") != null
            || child(properties, "shd") != null || !"exact".equals(attribute(spacing, "lineRule"))) {
            return null;
        }
        Long line = twipsOf(attribute(spacing, "line"));
        String beforeText = attribute(spacing, "before");
        String afterText = attribute(spacing, "after");
        Long before = beforeText == null ? Long.valueOf(0) : twipsOf(beforeText);
        Long after = afterText == null ? Long.valueOf(0) : twipsOf(afterText);
        if (line == null || before == null || after == null) {
            return null;
        }
        return (int) (line + before + after);
    }

    /** A node's first child element of a WordprocessingML name, or null. */
    private static Node child(Node node, String name) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getLocalName())
                && W.equals(child.getNamespaceURI())) {
                return child;
            }
        }
        return null;
    }

    /** An element's WordprocessingML attribute, or null. */
    private static String attribute(Node element, String name) {
        Node value = element.getAttributes().getNamedItemNS(W, name);
        return value == null ? null : value.getNodeValue();
    }

    private static Long twipsOf(Object value) {
        if (value instanceof BigInteger integer) {
            return integer.longValue();
        }
        try {
            return value == null ? null : Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int[] gridOf(CTTbl table) {
        if (table.getTblGrid() == null) {
            return new int[0];
        }
        int[] widths = new int[table.getTblGrid().sizeOfGridColArray()];
        for (int index = 0; index < widths.length; index++) {
            Long width = twipsOf(table.getTblGrid().getGridColArray(index).getW());
            widths[index] = width == null ? -1 : width.intValue();
        }
        return widths;
    }

    /** Whether a row's cells are one a column, none spanning or merged, each its width in twips. */
    private static boolean plainCells(CTRow row, int columns) {
        if (row.sizeOfTcArray() != columns || columns == 0) {
            return false;
        }
        for (CTTc cell : row.getTcArray()) {
            CTTcPr properties = cell.getTcPr();
            if (properties == null || properties.isSetGridSpan() || properties.isSetVMerge()
                || properties.isSetHMerge() || properties.getTcW() == null
                || properties.getTcW().getType() != STTblWidth.DXA) {
                return false;
            }
            Long width = twipsOf(properties.getTcW().getW());
            if (width == null || width <= 0) {
                return false;
            }
        }
        return true;
    }

    /** Whether a cell holds nothing but empty paragraphs, and runs that carry the rail's own drawings. */
    private static boolean empty(CTTc cell, List<CTDrawing> bars) {
        if (cell.sizeOfTblArray() > 0) {
            return false;
        }
        java.util.Set<Node> drawn = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (CTDrawing bar : bars) {
            drawn.add(bar.getDomNode());
        }
        for (CTP paragraph : cell.getPArray()) {
            if (paragraph.sizeOfHyperlinkArray() > 0 || paragraph.sizeOfFldSimpleArray() > 0) {
                return false;
            }
            for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run : paragraph.getRArray()) {
                for (Node child = run.getDomNode().getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() == Node.ELEMENT_NODE && !"rPr".equals(child.getLocalName())
                        && !drawn.contains(child)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Cuts a row's cell where the rail stands, the part right of the cut a cell of its own, or adds a cell
     * for a rail left of the row.
     *
     * @return the index of the cell whose left edge the rail stands on
     */
    private static int cutAtTheRail(CTRow row, int column, int cut) {
        if (column < 0) {
            CTTc cell = row.insertNewTc(0);
            railCell(cell, cut, 0);
            return 0;
        }
        if (cut == 0) {
            return column;
        }
        CTTc held = row.getTcArray(column);
        CTTcPr properties = held.getTcPr();
        int width = twipsOf(properties.getTcW().getW()).intValue();
        long right = 0;
        if (properties.isSetTcMar() && properties.getTcMar().isSetRight()) {
            Long margin = twipsOf(properties.getTcMar().getRight().getW());
            right = margin == null ? 0 : margin;
            properties.getTcMar().getRight().setW(BigInteger.ZERO);
        }
        properties.getTcW().setW(BigInteger.valueOf(cut));
        railCell(row.insertNewTc(column + 1), width - cut, right);
        return column + 1;
    }

    /** Makes an empty cell of a width, with no left margin and a right one. */
    private static void railCell(CTTc cell, int width, long right) {
        CTTcPr properties = cell.addNewTcPr();
        CTTblWidth tcW = properties.addNewTcW();
        tcW.setType(STTblWidth.DXA);
        tcW.setW(BigInteger.valueOf(width));
        CTTcMar margins = properties.addNewTcMar();
        CTTblWidth left = margins.addNewLeft();
        left.setType(STTblWidth.DXA);
        left.setW(BigInteger.ZERO);
        CTTblWidth rightMargin = margins.addNewRight();
        rightMargin.setType(STTblWidth.DXA);
        rightMargin.setW(BigInteger.valueOf(right));
        emptyParagraph(cell);
    }

    /** An empty paragraph one exact line two twips tall: what holds a cell no taller than its row. */
    private static void emptyParagraph(CTTc cell) {
        CTSpacing spacing = cell.addNewP().addNewPPr().addNewSpacing();
        spacing.setBefore(BigInteger.ZERO);
        spacing.setAfter(BigInteger.ZERO);
        spacing.setLine(BigInteger.valueOf(2));
        spacing.setLineRule(STLineSpacingRule.EXACT);
    }

    private static void border(CTTc cell, String colour, BigInteger eighths) {
        CTTcPr properties = cell.getTcPr();
        CTTcBorders borders = properties.isSetTcBorders() ? properties.getTcBorders() : properties.addNewTcBorders();
        CTBorder left = borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft();
        left.setVal(STBorder.SINGLE);
        left.setSz(eighths);
        left.setSpace(BigInteger.ZERO);
        left.setColor(colour);
    }

    /**
     * Cuts an entry's row where the rail ends inside it: a row of the given exact height, and a row of
     * the rest, every cell but the rail's merged down both.
     *
     * @param upperOnTheRail whether the rail runs along the upper row (it ends here) or the lower one (it
     *                       starts here)
     */
    private static List<CTRow> cutAt(CTRow row, int rail, int height, boolean upperOnTheRail, String colour,
                                     BigInteger eighths) {
        CTRow lower = (CTRow) row.copy();
        for (int index = 0; index < row.sizeOfTcArray(); index++) {
            if (index == rail) {
                continue;
            }
            row.getTcArray(index).getTcPr().addNewVMerge().setVal(STMerge.RESTART);
            CTTc continued = lower.getTcArray(index);
            clearContent(continued);
            continued.getTcPr().addNewVMerge();
            emptyParagraph(continued);
        }
        clearContent(lower.getTcArray(rail));
        emptyParagraph(lower.getTcArray(rail));
        border(upperOnTheRail ? row.getTcArray(rail) : lower.getTcArray(rail), colour, eighths);
        // A height the row was held to — a marker taller than its text, a row lifted into the gap
        // above it — is the two rows' together: the upper one takes the cut, the lower one the rest,
        // held the same way. Left on both, the entry stood taller by the cut and all under it lower.
        CTTrPr properties = row.isSetTrPr() ? row.getTrPr() : row.addNewTrPr();
        CTTrPr lowerProperties = lower.isSetTrPr() ? lower.getTrPr() : lower.addNewTrPr();
        Long held = null;
        STHeightRule.Enum rule = STHeightRule.AT_LEAST;
        if (properties.sizeOfTrHeightArray() > 0) {
            CTHeight was = properties.getTrHeightArray(0);
            held = twipsOf(was.getVal());
            if (was.isSetHRule()) {
                rule = was.getHRule();
            }
        }
        while (properties.sizeOfTrHeightArray() > 0) {
            properties.removeTrHeight(0);
        }
        while (lowerProperties.sizeOfTrHeightArray() > 0) {
            lowerProperties.removeTrHeight(0);
        }
        CTHeight exact = properties.addNewTrHeight();
        exact.setVal(BigInteger.valueOf(Math.max(1, height)));
        exact.setHRule(STHeightRule.EXACT);
        if (held != null && held > height && rule != STHeightRule.AUTO) {
            CTHeight rest = lowerProperties.addNewTrHeight();
            rest.setVal(BigInteger.valueOf(held - height));
            rest.setHRule(rule);
        }
        // The upper row keeps with the lower one: a page breaking between them left the entry's text
        // and marker at the foot of one page and the rail starting above nothing on the next in Word,
        // and in LibreOffice started the rail above the marker (measured, a timeline pushed down past
        // the foot of its page 3pt at a time).
        for (CTTc cell : row.getTcArray()) {
            for (CTP paragraph : cell.getPArray()) {
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr paragraphProperties =
                        paragraph.isSetPPr() ? paragraph.getPPr() : paragraph.addNewPPr();
                if (!paragraphProperties.isSetKeepNext()) {
                    paragraphProperties.addNewKeepNext();
                }
            }
        }
        return List.of(row, lower);
    }

    private static void clearContent(CTTc cell) {
        while (cell.sizeOfPArray() > 0) {
            cell.removeP(0);
        }
        while (cell.sizeOfTblArray() > 0) {
            cell.removeTbl(0);
        }
        // What else a cell can hold — a bookmark, a structured tag — goes with its paragraphs.
        try (XmlCursor cursor = cell.newCursor()) {
            if (cursor.toFirstChild()) {
                List<XmlObject> extra = new ArrayList<>();
                do {
                    XmlObject child = cursor.getObject();
                    if (!(child instanceof CTTcPr)) {
                        extra.add(child);
                    }
                } while (cursor.toNextSibling());
                for (XmlObject child : extra) {
                    remove(child);
                }
            }
        }
    }

    /** A row of the gap's exact height, its cells as wide as the entry's, the rail's bordered. */
    private static CTRow gapRow(CTRow entry, int rail, int height, String colour, BigInteger eighths) {
        CTRow gap = CTRow.Factory.newInstance();
        CTTrPr properties = gap.addNewTrPr();
        properties.addNewCantSplit();
        CTHeight exact = properties.addNewTrHeight();
        exact.setVal(BigInteger.valueOf(Math.max(1, height)));
        exact.setHRule(STHeightRule.EXACT);
        for (int index = 0; index < entry.sizeOfTcArray(); index++) {
            CTTcPr from = entry.getTcArray(index).getTcPr();
            CTTc cell = gap.addNewTc();
            CTTcPr to = cell.addNewTcPr();
            to.addNewTcW().set(from.getTcW());
            if (from.isSetTcMar()) {
                to.addNewTcMar().set(from.getTcMar());
            }
            emptyParagraph(cell);
            if (index == rail) {
                border(cell, colour, eighths);
            }
        }
        return gap;
    }

    private static int[] newGrid(int[] grid, int column, int cut) {
        List<Integer> widths = new ArrayList<>();
        if (column < 0) {
            widths.add(cut);
        }
        for (int index = 0; index < grid.length; index++) {
            if (index == column && cut > 0) {
                widths.add(cut);
                widths.add(grid[index] - cut);
            } else {
                widths.add(grid[index]);
            }
        }
        return widths.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void setGrid(CTTbl table, int[] widths) {
        while (table.getTblGrid().sizeOfGridColArray() > 0) {
            table.getTblGrid().removeGridCol(0);
        }
        for (int width : widths) {
            table.getTblGrid().addNewGridCol().setW(BigInteger.valueOf(width));
        }
    }

    /** Moves a table left by a width and widens it by as much, its cells staying where they stood. */
    private static void widenToTheLeft(CTTbl table, int width) {
        CTTblPr properties = table.getTblPr();
        CTTblWidth indent = properties.isSetTblInd() ? properties.getTblInd() : properties.addNewTblInd();
        Long was = indent.isSetW() ? twipsOf(indent.getW()) : Long.valueOf(0);
        indent.setType(STTblWidth.DXA);
        indent.setW(BigInteger.valueOf((was == null ? 0 : was) - width));
        if (properties.isSetTblW() && properties.getTblW().getType() == STTblWidth.DXA) {
            Long total = twipsOf(properties.getTblW().getW());
            if (total != null) {
                properties.getTblW().setW(BigInteger.valueOf(total + width));
            }
        }
    }

    private static void removeBetween(Node from, Node to) {
        Node node = from.getNextSibling();
        while (node != null && node != to) {
            Node next = node.getNextSibling();
            node.getParentNode().removeChild(node);
            node = next;
        }
    }

    private static void remove(XmlObject object) {
        try (XmlCursor cursor = object.newCursor()) {
            cursor.removeXml();
        }
    }

    private static int twips(double points) {
        return (int) Math.round(points * TWIPS_PER_POINT);
    }
}
