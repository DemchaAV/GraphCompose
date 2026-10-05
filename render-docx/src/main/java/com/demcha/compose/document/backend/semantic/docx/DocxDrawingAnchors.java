package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Where each shape {@link DocxDrawings} draws is anchored: in the paragraph whose text it stands
 * beside, or else in a paragraph written on its page.
 *
 * <p>A shape standing beside a paragraph's text — a timeline's dot by its entry, an icon by its
 * heading, a skill's bar by its label — is anchored in that paragraph and placed down from its
 * top ({@link #seat}): Word moves it with the paragraph, wherever an edit above takes it. Placed
 * from the page's edges, it stayed where the page had put it while the text moved away from it.
 * The paragraph is the one whose text is nearest, within {@link #REACH}, when the shape rises
 * above its top by the shape's own height at most, as a dot centred on its first line does. In a
 * table cell, the cell that holds the shape across takes it — the paragraph's own, or another of
 * its row, as a timeline's dot in a column of its own beside its entry's text — placed from that
 * cell's first paragraph (see {@link Seat#place}). When the nearest paragraph cannot place the
 * shape, it is left to the page rather than given to one further off. The shapes wait for the
 * section's end, since the paragraph a shape stands beside may be written after it.</p>
 *
 * <p>A shape beside no paragraph is positioned from the page's edges, so any paragraph on its
 * page carries it to the right place — but only one on that page: an anchor takes its shape to
 * the page the anchoring paragraph lands on. And a paragraph of the body rather than of a table
 * cell: Word measures a shape anchored in a cell from the cell, not the page. The first body
 * paragraph written on each page carries such shapes. The run is put at the front of the
 * paragraph, so a paragraph that runs on to the next page does not take the shape with it.</p>
 *
 * <p>A page laid out entirely in a table — a two-column CV is one row of two cells — has no
 * body paragraph. On the section's first page a paragraph a hairline tall is opened before the
 * table to carry its shapes; on its last, the paragraph closing the section does; on any other,
 * the first paragraph of a cell on that page, clipped as it may be, is still better than none.
 * Pages are counted within a section, as the layout counts them, so a section ends with
 * {@link #endSection}.</p>
 *
 * <p>A drawing that is all a table cell holds is the exception: {@link #anchorInCell} anchors it
 * in that cell's paragraph, placed from the paragraph and the cell's text column, so it moves
 * with its row; the cell holds it whole, so Word's clipping cuts nothing.</p>
 *
 * <p>Every shape takes its place in the paint order when it is drawn ({@link #ordered}), not
 * when it finds its paragraph.</p>
 */
final class DocxDrawingAnchors {

    // Shapes waiting for a paragraph on their page, by page, each with its place in the paint order.
    private final Map<Integer, List<Ordered>> pending = new TreeMap<>();
    // The first body paragraph written on each page of the section, by page.
    private final Map<Integer, XWPFParagraph> bodyParagraph = new HashMap<>();
    // The first table-cell paragraph written on each page of the section, by page.
    private final Map<Integer, XWPFParagraph> cellParagraph = new HashMap<>();
    // The paragraphs written on each page of the section a shape can be anchored beside, by page.
    private final Map<Integer, List<Seat>> seats = new HashMap<>();
    // The paragraphs offered as seats in the section, in the order they were written.
    private final List<Offer> offers = new ArrayList<>();
    // Identifiers unique in the document, shared with the other drawings the export writes.
    private final LongSupplier ids;
    // The order the shapes are painted in, across the document.
    private int order;

    DocxDrawingAnchors(LongSupplier ids) {
        this.ids = ids;
    }

    /** Forgets everything, for a new export. */
    void reset() {
        pending.clear();
        bodyParagraph.clear();
        cellParagraph.clear();
        seats.clear();
        offers.clear();
        order = 0;
    }

    /**
     * A shape and its place in the paint order: a later one is drawn over an earlier one.
     *
     * @param shape the shape
     * @param order its place, given when it was drawn, whenever it is anchored
     */
    record Ordered(DocxDrawings.Shape shape, int order) {
    }

    /**
     * Gives shapes their places in the paint order, in the order they come, as drawn now.
     *
     * <p>A shape is painted in the order it is drawn, not the order it finds a paragraph: one
     * waiting for its page's first paragraph would otherwise be painted over a shape anchored
     * in a cell meanwhile, though drawn before it.</p>
     */
    List<Ordered> ordered(List<DocxDrawings.Shape> shapes) {
        List<Ordered> ordered = new ArrayList<>(shapes.size());
        for (DocxDrawings.Shape shape : shapes) {
            ordered.add(new Ordered(shape, order++));
        }
        return ordered;
    }

    /**
     * Gives shapes their places in the paint order and keeps them for the section's end, which
     * anchors them (see {@link #endSection}).
     */
    void queue(List<DocxDrawings.Shape> shapes) {
        queueOrdered(ordered(shapes));
    }

    /** Keeps shapes given their places in the paint order for the section's end, as {@link #queue} does. */
    void queueOrdered(List<Ordered> shapes) {
        // Held until the section ends: the paragraph a shape stands beside may be written after it.
        for (Ordered shape : shapes) {
            pending.computeIfAbsent(shape.shape().page(), page -> new ArrayList<>()).add(shape);
        }
    }

    /**
     * Records a paragraph a shape on its page can be anchored beside, and where the page sets
     * its text.
     *
     * <p>Every paragraph written for a piece of text is offered; the first that holds text when
     * the section ends takes the place. A paragraph the layout starts on a new page is written
     * after an empty line holding its top edge there, and that line is not where its text is.</p>
     *
     * @param page      the page, counted within the section
     * @param owner     what the text is written for: one of its paragraphs takes the place
     * @param paragraph the paragraph holding that text
     * @param left      the text's left edge, from the page's left edge
     * @param right     the text's right edge, from the page's left edge
     * @param top       the top of its first line, from the page's top edge
     * @param baseline  its first line's baseline, from the page's top edge
     */
    void seat(int page, Object owner, XWPFParagraph paragraph, double left, double right, double top,
              double baseline) {
        if (page >= 0) {
            offers.add(new Offer(page, owner, new Seat(paragraph, left, right, top, baseline)));
        }
    }

    /** A paragraph offered as a seat, and what its text is written for. */
    private record Offer(int page, Object owner, Seat seat) {
    }

    /** Takes, for each piece of text offered, the first of its paragraphs that holds text. */
    private void takeTheSeats() {
        java.util.Set<Object> taken = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Offer offer : offers) {
            if (offer.seat().holdsText() && taken.add(offer.owner())) {
                seats.computeIfAbsent(offer.page(), ignored -> new ArrayList<>()).add(offer.seat());
            }
        }
        offers.clear();
    }

    /**
     * Records a paragraph written on a page. The page's first body paragraph carries the shapes
     * no paragraph beside them does; a cell's is kept in case the page has no other.
     *
     * @param page      the page, counted within the section
     * @param paragraph a paragraph the layout puts on that page
     * @param inBody    whether it is a paragraph of the body rather than of a table cell
     */
    void paragraphOn(int page, XWPFParagraph paragraph, boolean inBody) {
        if (page < 0) {
            return;
        }
        if (!inBody) {
            cellParagraph.putIfAbsent(page, paragraph);
            return;
        }
        bodyParagraph.putIfAbsent(page, paragraph);
    }

    /**
     * The first body paragraph written on a page of the section, or {@code null}.
     *
     * @param page the page, counted within the section
     */
    XWPFParagraph bodyParagraphOn(int page) {
        return bodyParagraph.get(page);
    }

    /**
     * The first table-cell paragraph written on a page of the section, or {@code null}.
     *
     * @param page the page, counted within the section
     */
    XWPFParagraph cellParagraphOn(int page) {
        return cellParagraph.get(page);
    }

    /**
     * Ends a section, anchoring the shapes no body paragraph on their page carried: on the
     * section's first page in {@code opening}, on its last in {@code closing}, on any other in
     * the first cell paragraph written on the page. Those on a page with none of these are
     * returned, and forgotten.
     *
     * @param lastPage the section's last page
     * @param opening  a hairline paragraph before the section's first table, asked for only when
     *                 needed; {@code null} when the section does not open with a table
     * @param closing  a paragraph at the section's end, asked for only when needed
     * @return how many shapes each page anchored in a cell, and each page nothing carried, had
     */
    Leftovers endSection(int lastPage, Supplier<XWPFParagraph> opening, Supplier<XWPFParagraph> closing) {
        Map<Integer, Integer> inCells = new TreeMap<>();
        Map<Integer, Integer> dropped = new TreeMap<>();
        takeTheSeats();
        // By the paragraph each is anchored in, which keeps no equality but its own.
        Map<XWPFParagraph, List<Placed>> beside = new java.util.LinkedHashMap<>();
        pending.forEach((page, all) -> {
            List<Ordered> shapes = new ArrayList<>();
            for (Ordered shape : all) {
                Place place = placeBeside(shape.shape());
                if (place != null) {
                    beside.computeIfAbsent(place.paragraph(), ignored -> new ArrayList<>()).add(new Placed(shape, place));
                } else {
                    shapes.add(shape);
                }
            }
            if (shapes.isEmpty()) {
                return;
            }
            XWPFParagraph carrier;
            if (bodyParagraph.containsKey(page)) {
                carrier = bodyParagraph.get(page);
            } else if (page == 0 && opening != null) {
                carrier = opening.get();
            } else if (page == lastPage) {
                carrier = closing.get();
            } else {
                carrier = cellParagraph.get(page);
                if (carrier != null) {
                    inCells.put(page, shapes.size());
                }
            }
            if (carrier != null) {
                anchor(carrier, shapes);
            } else {
                dropped.put(page, shapes.size());
            }
        });
        beside.forEach(this::anchorBeside);
        pending.clear();
        bodyParagraph.clear();
        cellParagraph.clear();
        seats.clear();
        return new Leftovers(inCells, dropped);
    }

    /**
     * The shapes a section's end could not anchor in a body paragraph, by page.
     *
     * @param inCells anchored in a table cell's paragraph, which Word may print them clipped to
     * @param dropped anchored nowhere
     */
    record Leftovers(Map<Integer, Integer> inCells, Map<Integer, Integer> dropped) {
    }

    /**
     * Anchors shapes in the paragraph of the table cell that holds them, placed from its text
     * column and the paragraph's top rather than from the page's edges: they move with the row
     * wherever Word sets it. The caller hands it only shapes the cell holds whole, as Word clips
     * a shape anchored in a cell to the cell.
     *
     * @param carrier the cell's paragraph
     * @param shapes  the shapes, measured from the page's edges
     * @param origin  where the page puts the cell's text column and the paragraph's top
     */
    void anchorInCell(XWPFParagraph carrier, List<Ordered> shapes, DocxDrawings.CellOrigin origin) {
        XWPFRun run = carrier.insertNewRun(0);
        for (Ordered shape : shapes) {
            run.getCTR().addNewDrawing().set(
                    DocxDrawings.drawingInCell(shape.shape(), ids.getAsLong(), shape.order(), origin));
        }
    }

    /**
     * The paragraph whose text a shape stands nearest, within reach, when it can hold the shape
     * placed down from its top; {@code null} when there is none, or it cannot.
     *
     * <p>A paragraph further off is never taken instead: it is not the text the shape stands
     * beside. An icon centred on the line of a note under a totals table stands a little above
     * the note's top, and the table's last cell, 44pt up, took it in; the icon hung below that
     * cell's row, and the Linux LibreOffice the build checks with set the row's 'Total due' 2pt
     * and 4pt up in two invoices.</p>
     */
    private Place placeBeside(DocxDrawings.Shape shape) {
        Seat nearest = null;
        double nearestBy = Double.POSITIVE_INFINITY;
        for (Seat seat : seats.getOrDefault(shape.page(), List.of())) {
            double by = Math.abs(shape.top() - seat.top())
                        + Math.max(0, seat.left() - (shape.x() + shape.width()))
                        + Math.max(0, shape.x() - seat.right());
            if (by <= REACH && by < nearestBy) {
                nearest = seat;
                nearestBy = by;
            }
        }
        // A shape rises above its paragraph's top by its own height at most, as a dot centred on
        // the paragraph's first line does: Word and LibreOffice place it there, out of a cell's
        // top too, and draw it whole. One rising further stands by other text: LibreOffice set an
        // 11pt rule 45pt above its paragraph's top at the head of the next page.
        return nearest == null ? null : nearest.place(shape);
    }

    /**
     * Anchors shapes in the paragraph whose text they stand nearest, placed down from its top,
     * in the order they are drawn: they move with the paragraph wherever an edit takes it.
     */
    private void anchorBeside(XWPFParagraph paragraph, List<Placed> shapes) {
        XWPFRun run = paragraph.insertNewRun(0);
        for (Placed placed : shapes) {
            Ordered ordered = placed.ordered();
            double column = placed.place().column();
            // A shape less than a stroke's width above the paragraph's top is placed at its top:
            // Word 16.0.20430 draws an offset of -0.13pt where it says, but reports it to a reader
            // as a Top of -999997. A shape higher still keeps its own offset, which Word reports.
            double rise = placed.place().top() - ordered.shape().top();
            double top = rise > 0 && rise <= SLACK ? ordered.shape().top() : placed.place().top();
            run.getCTR().addNewDrawing().set(Double.isInfinite(column)
                    ? DocxDrawings.drawingInParagraph(ordered.shape(), ids.getAsLong(), ordered.order(), top)
                    : DocxDrawings.drawingInCell(ordered.shape(), ids.getAsLong(), ordered.order(),
                            new DocxDrawings.CellOrigin(column, top)));
        }
    }

    /**
     * How far from a paragraph's text a shape still stands beside it, across and down added, in
     * points: a timeline's dot beside its entry, a rail from its first entry, an icon beside its
     * heading. A shape further from every paragraph stays where the page puts it.
     */
    private static final double REACH = 48;

    /** A stroke's width either side a shape may reach past its paragraph's top or its cell, in points. */
    private static final double SLACK = 1;

    /**
     * The furthest a shape rises above its paragraph's top, in points: as far as a dot or an
     * icon's disc centred on a line — the corpus's largest disc is 22.6pt — where Word and
     * LibreOffice were measured placing 6pt and 10pt dots up to 8pt above their paragraphs. A
     * taller shape — a rail, an accent bar — rising further stands by other text.
     */
    private static final double FURTHEST_RISE = 24;

    /** Word's room for a cell's text either side, where neither the cell nor its table states one. */
    private static final double DEFAULT_CELL_MARGIN = 5.4;

    /**
     * A paragraph a shape can be anchored beside, and where the page sets its text.
     *
     * @param paragraph the paragraph
     * @param left      its text's left edge, from the page's left edge
     * @param right     its text's right edge, from the page's left edge
     * @param top       its first line's top, from the page's top edge
     * @param baseline  its first line's baseline, from the page's top edge
     */
    record Seat(XWPFParagraph paragraph, double left, double right, double top, double baseline) {

        /**
         * Where the page puts the top Word places a paragraph's shapes from: its first line's,
         * less the space written above it. NaN where something else stands above the line — a
         * top border, or space Word reckons in lines, by itself or against the paragraph before.
         *
         * <p>Word's line is not the page's: the export stands Word's baseline where the page's
         * is, and Word stands an exact line's baseline four fifths of the way down it, raised by
         * the runs' position. A 15pt exact line round 8.6pt text starts 4.6pt above the page's
         * line, and a shape placed from the page's line stood that much high.</p>
         */
        double paragraphTop() {
            CTPPr properties = paragraph.getCTP().getPPr();
            double before = 0;
            double lineTop = top;
            if (properties != null) {
                if (properties.isSetPBdr() && properties.getPBdr().isSetTop()
                    || properties.isSetContextualSpacing()
                       && org.apache.poi.ooxml.util.POIXMLUnits.parseOnOff(properties.getContextualSpacing())) {
                    return Double.NaN;
                }
                if (properties.isSetSpacing()) {
                    CTSpacing spacing = properties.getSpacing();
                    if (spacing.isSetBeforeLines() || spacing.isSetBeforeAutospacing()) {
                        return Double.NaN;
                    }
                    if (spacing.isSetBefore()) {
                        if (!(spacing.getBefore() instanceof Number twips)) {
                            return Double.NaN;
                        }
                        before = twips.doubleValue() / 20;
                    }
                    if (spacing.isSetLineRule() && spacing.getLineRule() == STLineSpacingRule.EXACT) {
                        if (!(spacing.getLine() instanceof Number twips)) {
                            return Double.NaN;
                        }
                        lineTop = baseline - (DocxTextBands.BASELINE_SHARE * twips.doubleValue() / 20 - raised());
                    }
                }
            }
            return lineTop - before;
        }

        /** How far the paragraph's first run of text is raised, in points. */
        private double raised() {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run = firstRunOfText();
            CTRPr properties = run == null ? null : run.getRPr();
            return properties != null && properties.sizeOfPositionArray() > 0
                   && properties.getPositionArray(0).getVal() instanceof Number halfPoints
                    ? halfPoints.doubleValue() / 2 : 0;
        }

        /** Whether the paragraph holds any text. */
        boolean holdsText() {
            return firstRunOfText() != null;
        }

        /**
         * The paragraph's first run holding text, a link's among them: POI leaves an internal
         * link's runs out of a paragraph's runs and its text.
         */
        private org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR firstRunOfText() {
            for (org.apache.xmlbeans.XmlObject found : paragraph.getCTP().selectPath(
                    "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' "
                    + "./w:r | ./w:hyperlink/w:r")) {
                if (found instanceof org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run
                    && run.getTList().stream().anyMatch(text -> !text.getStringValue().isBlank())) {
                    return run;
                }
            }
            return null;
        }

        /**
         * Where Word places a shape beside this paragraph's text from, or {@code null} where it
         * cannot. The shape may rise above the paragraph's top by its own height at most.
         *
         * <p>A paragraph of the body places it across from the page's edge. One in a table cell
         * places it from the cell's text column; a cell that does not hold the shape across hands
         * it to the cell of the same row that does, from that cell's first paragraph — a
         * timeline's dot stands in a column of its own beside its entry's text, in a cell holding
         * nothing else. None takes it for a line not set from the cell's left, whose indent is
         * written short of the page's, or in a repeated header row, which Word repeats on every
         * page with what is anchored in it.</p>
         */
        Place place(DocxDrawings.Shape shape) {
            double top = paragraphTop();
            if (!risesNoFurther(shape, top)) {
                return null;
            }
            if (!(paragraph.getBody() instanceof XWPFTableCell cell)) {
                return new Place(paragraph, Double.POSITIVE_INFINITY, top);
            }
            if (cell.getTableRow().isRepeatHeader() || !leftAligned(paragraph)) {
                return null;
            }
            double width = widthOf(cell);
            double cellLeft = textLeft() - leftMargin(cell);
            if (Double.isNaN(width) || Double.isNaN(cellLeft)) {
                return null;
            }
            if (holdsAcross(shape, cellLeft, width)) {
                return new Place(paragraph, cellLeft + leftMargin(cell), top);
            }
            Place inTheRow = inTheRow(shape, cell, cellLeft, width, top);
            return inTheRow != null && risesNoFurther(shape, inTheRow.top()) ? inTheRow : null;
        }

        /**
         * Whether a shape rises above a paragraph's top by no more than its own height, as a dot
         * centred on the paragraph's first line does, nor more than {@link #FURTHEST_RISE}. NaN
         * for a top not known is no.
         */
        private static boolean risesNoFurther(DocxDrawings.Shape shape, double top) {
            return shape.top() >= top - Math.max(SLACK, Math.min(shape.height(), FURTHEST_RISE));
        }

        /**
         * The place in the cell of this paragraph's row that holds a shape across, from that
         * cell's first paragraph; {@code null} when none does or its top is not known.
         *
         * <p>Measured in Word 16.0.20430 and LibreOffice: in a row whose cells set their content
         * from the top, the first paragraph of each starts at the row's top, less its cell's top
         * margin; so a shape is placed from a neighbour's first paragraph as from this one's.</p>
         */
        private Place inTheRow(DocxDrawings.Shape shape, XWPFTableCell cell, double cellLeft, double width,
                               double top) {
            List<IBodyElement> opening = cell.getBodyElements();
            if (opening.isEmpty() || opening.get(0) != paragraph || !alignedToTheTop(cell)) {
                return null;
            }
            double rowTop = top - topMargin(cell);
            List<XWPFTableCell> cells = cell.getTableRow().getTableCells();
            int at = -1;
            for (int index = 0; index < cells.size(); index++) {
                if (cells.get(index).getCTTc() == cell.getCTTc()) {
                    at = index;
                }
            }
            if (at < 0) {
                return null;
            }
            double edge = cellLeft;
            for (int index = at - 1; index >= 0; index--) {
                double neighbour = widthOf(cells.get(index));
                if (Double.isNaN(neighbour)) {
                    return null;
                }
                edge -= neighbour;
                if (holdsAcross(shape, edge, neighbour)) {
                    return openingPlace(cells.get(index), edge, rowTop);
                }
            }
            edge = cellLeft + width;
            for (int index = at + 1; index < cells.size(); index++) {
                double neighbour = widthOf(cells.get(index));
                if (Double.isNaN(neighbour)) {
                    return null;
                }
                if (holdsAcross(shape, edge, neighbour)) {
                    return openingPlace(cells.get(index), edge, rowTop);
                }
                edge += neighbour;
            }
            return null;
        }

        /** The place at the start of a cell whose left edge stands where given; {@code null} when unknown. */
        private static Place openingPlace(XWPFTableCell cell, double cellLeft, double rowTop) {
            CTTcPr properties = cell.getCTTc().getTcPr();
            List<IBodyElement> opening = cell.getBodyElements();
            if (properties != null && properties.isSetVMerge() || !alignedToTheTop(cell) || opening.isEmpty()
                || !(opening.get(0) instanceof XWPFParagraph first)) {
                return null;
            }
            return new Place(first, cellLeft + leftMargin(cell), rowTop + topMargin(cell));
        }

        /** Where the paragraph's text's left edge stands without its indent; NaN when not known. */
        private double textLeft() {
            CTPPr properties = paragraph.getCTP().getPPr();
            if (properties != null && properties.isSetInd() && properties.getInd().isSetLeft()) {
                return properties.getInd().getLeft() instanceof Number twips ? left - twips.doubleValue() / 20
                        : Double.NaN;
            }
            return left;
        }

        private static boolean holdsAcross(DocxDrawings.Shape shape, double cellLeft, double width) {
            return shape.x() >= cellLeft - SLACK && shape.x() + shape.width() <= cellLeft + width + SLACK;
        }

        /** A cell's width as written, in points; NaN where it is not written in twips. */
        private static double widthOf(XWPFTableCell cell) {
            CTTcPr properties = cell.getCTTc().getTcPr();
            return properties != null && properties.isSetTcW() && properties.getTcW().getType() == STTblWidth.DXA
                   && properties.getTcW().getW() instanceof Number width ? width.doubleValue() / 20 : Double.NaN;
        }

        /** Whether a cell sets its content from its top. */
        private static boolean alignedToTheTop(XWPFTableCell cell) {
            CTTcPr properties = cell.getCTTc().getTcPr();
            return properties == null || !properties.isSetVAlign()
                   || properties.getVAlign().getVal() == org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc.TOP;
        }

        /** A cell's top margin as written, in points: Word's own is none. */
        private static double topMargin(XWPFTableCell cell) {
            CTTcPr cellProperties = cell.getCTTc().getTcPr();
            if (cellProperties != null && cellProperties.isSetTcMar() && cellProperties.getTcMar().isSetTop()) {
                return points(cellProperties.getTcMar().getTop(), 0);
            }
            CTTblPr table = cell.getTableRow().getTable().getCTTbl().getTblPr();
            if (table != null && table.isSetTblCellMar() && table.getTblCellMar().isSetTop()) {
                return points(table.getTblCellMar().getTop(), 0);
            }
            return 0;
        }

        /** Whether a paragraph's lines start at its left indent: not centred, set right or right to left. */
        private static boolean leftAligned(XWPFParagraph paragraph) {
            CTPPr properties = paragraph.getCTP().getPPr();
            if (properties == null) {
                return true;
            }
            if (properties.isSetBidi() && org.apache.poi.ooxml.util.POIXMLUnits.parseOnOff(properties.getBidi())) {
                return false;
            }
            if (!properties.isSetJc()) {
                return true;
            }
            STJc.Enum alignment = properties.getJc().getVal();
            return alignment != STJc.CENTER && alignment != STJc.RIGHT && alignment != STJc.END;
        }

        private static double leftMargin(XWPFTableCell cell) {
            CTTcPr cellProperties = cell.getCTTc().getTcPr();
            if (cellProperties != null && cellProperties.isSetTcMar() && cellProperties.getTcMar().isSetLeft()) {
                return points(cellProperties.getTcMar().getLeft(), DEFAULT_CELL_MARGIN);
            }
            CTTblPr table = cell.getTableRow().getTable().getCTTbl().getTblPr();
            if (table != null && table.isSetTblCellMar() && table.getTblCellMar().isSetLeft()) {
                return points(table.getTblCellMar().getLeft(), DEFAULT_CELL_MARGIN);
            }
            return DEFAULT_CELL_MARGIN;
        }

        private static double points(CTTblWidth width, double otherwise) {
            return width.getW() instanceof Number twips ? twips.doubleValue() / 20 : otherwise;
        }
    }

    /**
     * Where a shape is anchored and placed from.
     *
     * @param paragraph the paragraph it is anchored in
     * @param column    where Word's column starts, from the page's left edge; infinite for a
     *                  paragraph of the body, whose shapes are placed from the page's edge
     * @param top       where the paragraph's top stands, from the page's top edge
     */
    record Place(XWPFParagraph paragraph, double column, double top) {
    }

    /** A shape and where it is placed from. */
    private record Placed(Ordered ordered, Place place) {
    }

    private void anchor(XWPFParagraph carrier, List<Ordered> shapes) {
        XWPFRun run = carrier.insertNewRun(0);
        for (Ordered shape : shapes) {
            run.getCTR().addNewDrawing().set(DocxDrawings.drawing(shape.shape(), ids.getAsLong(), shape.order()));
        }
    }
}
