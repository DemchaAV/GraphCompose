package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Where each shape {@link DocxDrawings} draws is anchored: in a paragraph of the block it belongs
 * to, or else in a paragraph written on its page.
 *
 * <p>A shape a node draws belongs to that node and to the blocks holding it: an icon laid over a
 * card's corner to the card's stack, a skill's bar to its row, a portrait to its sidebar. It is
 * anchored in the first paragraph the nearest of them writes on its page that can place it, and
 * placed from that paragraph's top ({@link #seat}), so Word moves it with that block wherever an
 * edit above takes it; where it stands gives only its offset, however far from the paragraph's
 * text that is. Placed from the page's edges, it stayed where the page had put it while the text
 * moved away from it. In a table cell, the cell of the paragraph's row that holds the shape
 * across takes it — a timeline's dot stands in a column of its own beside its entry's text —
 * placed from that cell's first paragraph (see {@link Seat#placeHeld}). A shape no node draws —
 * a timeline's rail — or whose holders write no paragraph on its page goes to the paragraph
 * nearest it there. What a table's composed cell draws has no node of its own — such a cell's
 * content is laid out under its table — so the cell found by the box the layout placed it in
 * stands for the block holding it: it goes to the first paragraph of that cell that can place it
 * ({@link #placeInItsCell}). One no cell's box holds, or whose cell's paragraphs cannot place it,
 * goes to the paragraph whose text is nearest, within {@link #REACH}, when the shape rises above
 * its top by its own height at most — in a table cell by a stroke's width at most — or else to the
 * page. The shapes wait for the section's end, since the paragraph carrying a shape may be written
 * after it.</p>
 *
 * <p>A shape no paragraph on its page can carry is positioned from the page's edges, so any
 * paragraph on its page carries it to the right place — but only one on that page: an anchor
 * takes its shape to the page the anchoring paragraph lands on. And a paragraph of the body
 * rather than of a table cell: Word measures a shape anchored in a cell from the cell, not the
 * page. The first body
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
 * <p>Every shape takes its place in the paint order from where the page paints the fragment it
 * is drawn from ({@link #ordered}), not when it is drawn or when it finds its paragraph: Word
 * stacks drawings by those places, a later one over an earlier one, and the export draws a
 * stack's layers, a band's and a table's cells in the order it writes them.</p>
 *
 * <p>The paragraph each shape is anchored in is kept for the whole document
 * ({@link #carrierOfTheFirst}): a block of drawings alone writes no paragraph of its own, and its
 * anchor's bookmark goes in the one holding its first drawing.</p>
 */
final class DocxDrawingAnchors {

    // Shapes waiting for a paragraph on their page, by page, each with its place in the paint order.
    private final Map<Integer, List<Ordered>> pending = new TreeMap<>();
    // What each waiting shape is drawn for, where something is.
    private final Map<Ordered, Owner> owners = new java.util.IdentityHashMap<>();
    // The cell whose box holds each waiting shape a table's composed cell draws, where one does.
    private final Map<Ordered, HoldingCell> heldBy = new java.util.IdentityHashMap<>();
    // The first body paragraph written on each page of the section, by page.
    private final Map<Integer, XWPFParagraph> bodyParagraph = new HashMap<>();
    // The first table-cell paragraph written on each page of the section, by page.
    private final Map<Integer, XWPFParagraph> cellParagraph = new HashMap<>();
    // The paragraph of a page break written at the end of each page of the section, by page.
    private final Map<Integer, XWPFParagraph> breakParagraph = new HashMap<>();
    // The paragraphs written on each page of the section a shape can be anchored beside, by page.
    private final Map<Integer, List<Seat>> seats = new HashMap<>();
    // The paragraphs offered as seats in the section, in the order they were written.
    private final List<Offer> offers = new ArrayList<>();
    // Where Word places from in each paragraph taken as a seat in the section, once asked: a
    // drawing's holders and the paragraphs nearest it try the same paragraphs again.
    private final Map<Seat, Double> tops = new java.util.IdentityHashMap<>();
    // Identifiers unique in the document, shared with the other drawings the export writes.
    private final LongSupplier ids;
    // How tall Word stands a written paragraph's line, and its baseline in it.
    private final Function<CTP, DocxGrownLines.Line> lines;
    // The paths of the node at a path and of the blocks holding it, nearest first.
    private final Function<String, List<String>> holders;
    // The places given each fragment's shapes in the section so far, by the fragment's place in
    // the page's paint order.
    private final Map<Integer, Integer> placesTaken = new HashMap<>();
    // The latest place given: a shape drawn from no fragment the page paints is drawn as the
    // fragment that place is of.
    private int latest = -1;
    // How many shapes have been drawn in the document, which numbers their names.
    private int drawn;
    // The paragraph each shape anchored in the document is anchored in, by its number
    // (Ordered#drawn), with where the page puts it.
    private final Map<Integer, Carried> carried = new HashMap<>();

    /**
     * How many places in the paint order a fragment's shapes take, at most: one drawn as more
     * shapes than that stacks the rest at its last place. A fragment is drawn as five shapes at
     * most ({@link DocxDrawings#of}).
     */
    static final int PLACES_PER_FRAGMENT = 8;

    /**
     * @param ids     identifiers unique in the document
     * @param lines   how tall Word stands a written paragraph's line, and its baseline in it
     *                ({@link DocxGrownLines#lineOf})
     * @param holders the paths of the node at a path and of the blocks holding it, nearest first,
     *                the page's flow left out ({@link DocxLayoutMetrics#holdersOf})
     */
    DocxDrawingAnchors(LongSupplier ids, Function<CTP, DocxGrownLines.Line> lines,
                       Function<String, List<String>> holders) {
        this.ids = ids;
        this.lines = lines;
        this.holders = holders;
    }

    /** Forgets everything, for a new export. */
    void reset() {
        pending.clear();
        owners.clear();
        heldBy.clear();
        bodyParagraph.clear();
        cellParagraph.clear();
        breakParagraph.clear();
        seats.clear();
        offers.clear();
        tops.clear();
        placesTaken.clear();
        latest = -1;
        drawn = 0;
        carried.clear();
    }

    /**
     * How many shapes the document has given a place in the paint order so far: those given
     * one from here on are numbered from it ({@link Ordered#drawn}).
     */
    int drawnSoFar() {
        return drawn;
    }

    /**
     * The paragraph a shape is anchored in, and where the page puts the shape.
     *
     * @param paragraph the paragraph whose run holds the shape's anchor, or {@code null} for a shape
     *                  the section's end found nothing to carry
     * @param page      the shape's page, counted within its section
     * @param top       the shape's top, from the page's top edge
     */
    private record Carried(XWPFParagraph paragraph, int page, double top) {
    }

    /**
     * The paragraph that carries the first of the shapes numbered {@code from} up to
     * {@code to}: the one on the earliest page and highest on it. {@code null} where none of them
     * has been anchored, or where the first is on a page nothing carries ({@link Leftovers}): one
     * carried on a later page is not where they start.
     *
     * <p>Word anchors a shape in a paragraph on its page and keeps it there, whatever an edit
     * does to the text above: the paragraph's page is the shape's.</p>
     *
     * <p>The paragraph is the one the shape was anchored in, as its section's end found it: asked
     * then, before the section's pages are given their margins, it is in the file. A table split
     * for those margins later takes the rows it moves out of the part it read them from.</p>
     *
     * @param from the number of the first shape, as {@link #drawnSoFar} read before they were drawn
     * @param to   the number after the last
     */
    XWPFParagraph carrierOfTheFirst(int from, int to) {
        Carried first = null;
        for (int number = from; number < to; number++) {
            Carried shape = carried.get(number);
            if (shape != null && (first == null || shape.page() < first.page()
                                  || shape.page() == first.page() && shape.top() < first.top())) {
                first = shape;
            }
        }
        return first == null ? null : first.paragraph();
    }

    /** Records the paragraph a shape is anchored in: {@code null} where nothing carries it. */
    private void carry(XWPFParagraph paragraph, Ordered shape) {
        carried.put(shape.drawn(), new Carried(paragraph, shape.shape().page(), shape.shape().top()));
    }

    /**
     * A shape and its place in the paint order: a later one is drawn over an earlier one.
     *
     * @param shape the shape
     * @param order its place, from where the page paints the fragment it is drawn from, whenever
     *              it is anchored
     * @param drawn how many shapes were drawn in the document before it, which numbers its name
     */
    record Ordered(DocxDrawings.Shape shape, int order, int drawn) {
    }

    /**
     * Gives shapes drawn from one fragment their places in the paint order: after every shape of
     * a fragment the page paints before it, and in the order they come among that fragment's.
     *
     * <p>A shape is painted where the page paints it, not in the order it is drawn or finds a
     * paragraph: a disc a dot is laid under, its layers given dot last, hides the dot as on the
     * page; one waiting for its page's first paragraph is not painted over a shape anchored in a
     * cell meanwhile, though painted before it.</p>
     *
     * @param painted where the page paints the fragment they are drawn from, from 0, as the layout
     *                lists its fragments; -1 where it is none the layout holds, and they are drawn
     *                as the fragment the latest place given is of, after its shapes so far
     */
    List<Ordered> ordered(List<DocxDrawings.Shape> shapes, int painted) {
        List<Ordered> ordered = new ArrayList<>(shapes.size());
        int fragment = painted >= 0 ? painted : Math.max(0, latest) / PLACES_PER_FRAGMENT;
        for (DocxDrawings.Shape shape : shapes) {
            int taken = placesTaken.merge(fragment, 1, Integer::sum) - 1;
            int place = fragment * PLACES_PER_FRAGMENT + Math.min(taken, PLACES_PER_FRAGMENT - 1);
            latest = Math.max(latest, place);
            ordered.add(new Ordered(shape, place, drawn++));
        }
        return ordered;
    }

    /**
     * What a drawing is drawn for: the report's note on it, which a drawing the section's end
     * finds nothing to carry is named on instead (see {@link Leftovers}), and the node whose
     * holders carry it.
     *
     * @param subject  the note's subject
     * @param path     the note's path: the node that draws it, whose holders look for the
     *                 paragraph carrying it; a path no node has — a pass of the layout's own —
     *                 leaves it to the paragraph nearest it
     * @param composed whether a table's composed cell draws it, which has no node of its own
     */
    record Owner(String subject, String path, boolean composed) {

        /** What a node draws, or a pass of the layout's own. */
        Owner(String subject, String path) {
            this(subject, path, false);
        }
    }

    /**
     * The cell whose box holds a drawing its table paints for it, where that drawing goes: the
     * cell as written, so that the paragraphs inside it are found by what holds them rather than
     * by a path. A composed cell's content is given no path of its own by the layout, and one node
     * standing in two cells is given one path for both, which would send the second cell's
     * drawings into the first cell's paragraph.
     *
     * <p>A cell set to the middle or the bottom carries a drawing only where the page fills it.
     * Measured in Word 16.0.20527 and LibreOffice 24.2: a cell set to the middle that carries a
     * drawing has its own text set higher — Word by 4pt, LibreOffice by the drawing's height —
     * and LibreOffice sets the content of the cell holding its table from that cell's top as well,
     * 43.7pt up in a cell 100pt tall round one line. Both only where the row leaves the cell
     * something to centre, and a cell the page fills moved no line in either editor. Its
     * paragraphs' tops are the row's only then as well: in a cell with room to centre in, Word
     * centres the drawing along with the paragraph carrying it, so where the drawing stands no
     * longer tells where that paragraph starts.</p>
     *
     * @param cell  the cell as written, which the paragraph carrying the drawing must stand in
     * @param fills whether the page fills the cell's box, so that its own vertical alignment, and
     *              that of a cell inside it holding no text, may be left out of the walk
     *              ({@link Seat#placeHeld})
     */
    record HoldingCell(XWPFTableCell cell, boolean fills) {

        /** Whether a paragraph stands in this cell, at any depth. */
        boolean holds(XWPFParagraph paragraph) {
            for (org.apache.poi.xwpf.usermodel.IBody body = paragraph.getBody(); body != null; ) {
                if (!(body instanceof XWPFTableCell at)) {
                    return false;
                }
                if (at.getCTTc() == cell.getCTTc()) {
                    return true;
                }
                body = at.getTableRow().getTable().getBody();
            }
            return false;
        }

        /** The cell whose vertical alignment the walk may leave out, or {@code null} where none may. */
        CTTc filled() {
            return fills ? cell.getCTTc() : null;
        }
    }

    /**
     * Gives shapes their places in the paint order and keeps them for the section's end, which
     * anchors them (see {@link #endSection}).
     *
     * @param painted where the page paints the fragment they are drawn from ({@link #ordered})
     * @param owner   what they are drawn for, or {@code null} where no note names them
     */
    void queue(List<DocxDrawings.Shape> shapes, int painted, Owner owner) {
        queueOrdered(ordered(shapes, painted), owner, null);
    }

    /**
     * Keeps shapes given their places in the paint order for the section's end, as {@link #queue}
     * does, with the cell holding them.
     *
     * @param held for shapes a table's composed cell draws, the cell whose box holds them;
     *             {@code null} where no cell's box does, or for shapes a node draws. It is kept
     *             against the shapes rather than in the owner, so that the note the owner names
     *             stays one note for the table ({@link Leftovers}).
     */
    void queueOrdered(List<Ordered> shapes, Owner owner, HoldingCell held) {
        // Held until the section ends: the paragraph carrying a shape may be written after it.
        for (Ordered shape : shapes) {
            pending.computeIfAbsent(shape.shape().page(), page -> new ArrayList<>()).add(shape);
            if (owner != null) {
                owners.put(shape, owner);
            }
            if (held != null) {
                heldBy.put(shape, held);
            }
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
     * @param path      the path of the node the text is written for, which the blocks holding a
     *                  drawing are matched against; {@code null} for text no node writes
     * @param paragraph the paragraph holding that text
     * @param left      the text's left edge, from the page's left edge
     * @param right     the text's right edge, from the page's left edge
     * @param top       the top of its first line, from the page's top edge
     * @param baseline  its first line's baseline, from the page's top edge
     */
    void seat(int page, Object owner, String path, XWPFParagraph paragraph, double left, double right,
              double top, double baseline) {
        if (page >= 0) {
            offers.add(new Offer(page, owner, new Seat(paragraph, left, right, top, baseline, path)));
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
     * no other paragraph does; a cell's is kept in case the page has no other.
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
     * Records the paragraph of a page break that ends a page: it stands on that page, and
     * carries its shapes where nothing else written there does. Behind a table that fills its
     * page Word would set it on the next, so a page's cell paragraph comes first.
     *
     * @param page      the page the break ends, counted within the section
     * @param paragraph the break's paragraph
     */
    void breakOn(int page, XWPFParagraph paragraph) {
        if (page >= 0) {
            breakParagraph.putIfAbsent(page, paragraph);
        }
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
     * the first cell paragraph written on the page, or else in the paragraph of the page break
     * ending it ({@link #breakOn}). Those on a page with none of these are returned, and forgotten.
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
        Map<Owner, Integer> queuedFor = new java.util.LinkedHashMap<>();
        Map<Owner, Integer> droppedFor = new java.util.LinkedHashMap<>();
        pending.values().forEach(all -> all.forEach(shape -> {
            Owner owner = owners.get(shape);
            if (owner != null) {
                queuedFor.merge(owner, 1, Integer::sum);
            }
        }));
        takeTheSeats();
        // By the paragraph each is anchored in, which keeps no equality but its own.
        Map<XWPFParagraph, List<Placed>> beside = new java.util.LinkedHashMap<>();
        pending.forEach((page, all) -> {
            List<Ordered> shapes = new ArrayList<>();
            for (Ordered shape : all) {
                Owner owner = owners.get(shape);
                Place place = owner != null && owner.composed()
                        ? placeInItsCell(shape.shape(), heldBy.get(shape))
                        : placeInItsHolder(shape.shape(), owner == null ? List.of() : holders.apply(owner.path()));
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
            } else if (cellParagraph.containsKey(page)) {
                carrier = cellParagraph.get(page);
                inCells.put(page, shapes.size());
            } else {
                carrier = breakParagraph.get(page);
            }
            if (carrier != null) {
                anchor(carrier, shapes);
            } else {
                int unowned = 0;
                for (Ordered shape : shapes) {
                    carry(null, shape);
                    Owner owner = owners.get(shape);
                    if (owner == null) {
                        unowned++;
                    } else {
                        droppedFor.merge(owner, 1, Integer::sum);
                    }
                }
                if (unowned > 0) {
                    dropped.put(page, unowned);
                }
            }
        });
        beside.forEach(this::anchorBeside);
        pending.clear();
        owners.clear();
        heldBy.clear();
        bodyParagraph.clear();
        cellParagraph.clear();
        breakParagraph.clear();
        seats.clear();
        tops.clear();
        // The next section's layout counts its fragments from its own first one.
        placesTaken.clear();
        latest = -1;
        Map<Owner, Boolean> droppedWhole = new java.util.LinkedHashMap<>();
        droppedFor.forEach((owner, count) -> droppedWhole.put(owner, count.equals(queuedFor.get(owner))));
        return new Leftovers(inCells, dropped, droppedWhole);
    }

    /**
     * The shapes a section's end could not anchor in a body paragraph, by page.
     *
     * @param inCells anchored in a table cell's paragraph, which Word may print them clipped to
     * @param dropped anchored nowhere, drawn for nothing a note names
     * @param owners  what the shapes anchored nowhere were drawn for, each with whether all of its
     *                shapes were
     */
    record Leftovers(Map<Integer, Integer> inCells, Map<Integer, Integer> dropped, Map<Owner, Boolean> owners) {
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
                    DocxDrawings.drawingInCell(shape.shape(), ids.getAsLong(), shape.order(), shape.drawn(), origin));
            carry(carrier, shape);
        }
    }

    /**
     * Where a shape a node draws is anchored: in the first paragraph on its page of the nearest
     * of its holders that writes one there it can be placed from, or else in the paragraph
     * nearest it on its page; {@code null} where no paragraph on its page can carry it.
     *
     * <p>Its holders — the node drawing it, then each block holding that node — are what it
     * belongs to: an icon laid over a card's corner is the card's stack's, a skill's bar its
     * row's, a portrait its sidebar's. Anchored in the first paragraph one of them writes on its
     * page, it moves with that block wherever an edit above takes it, however far from the
     * paragraph's text it stands: where it stands gives only its offset. A holder that writes no
     * paragraph there, or only ones that cannot place it, passes it on to the next one up. A shape
     * no holder carries — a timeline's rail, which no node draws, or one whose holders' text is a
     * list or a table — goes to the paragraph nearest it.</p>
     *
     * @param holders the paths of the node drawing it and of the blocks holding it, nearest first
     */
    private Place placeInItsHolder(DocxDrawings.Shape shape, List<String> holders) {
        List<Seat> onItsPage = seats.getOrDefault(shape.page(), List.of());
        for (String holder : holders) {
            for (Seat seat : onItsPage) {
                Place place = seat.isWithin(holder) ? placeHeld(seat, shape) : null;
                if (place != null) {
                    return place;
                }
            }
        }
        return placeBesideTheNearest(shape, onItsPage);
    }

    /**
     * Where a shape a table's composed cell draws is anchored: in the first paragraph on its page
     * of what the cell whose box holds it is composed of that it can be placed from, or else
     * beside the text it stands nearest ({@link #placeBeside}).
     *
     * <p>Such a shape has no node of its own — a composed cell's content is laid out under its
     * table, its drawings among the table's own fragments — so the cell is found by the box the
     * layout placed it in, and that cell stands for the block holding the shape: anchored in a
     * paragraph of it, the shape moves with the cell's row wherever an edit above takes it. A cell
     * whose paragraphs cannot place it — none on the shape's page, or none set from its top, which
     * LibreOffice 24.2 sets a cell carrying a drawing, and the cell holding its table, from — hands
     * it back to the text nearest it, as before.</p>
     *
     * @param cell the cell holding it, or {@code null} where no cell's box holds it
     */
    private Place placeInItsCell(DocxDrawings.Shape shape, HoldingCell cell) {
        if (cell != null) {
            for (Seat seat : seats.getOrDefault(shape.page(), List.of())) {
                Place place = cell.holds(seat.paragraph())
                        ? seat.placeHeld(shape, tops.computeIfAbsent(seat, taken -> taken.paragraphTop(lines)),
                                cell.filled())
                        : null;
                if (place != null) {
                    return place;
                }
            }
        }
        return placeBeside(shape);
    }

    /** Where a paragraph places a shape it carries for a block holding both ({@link Seat#placeHeld}). */
    private Place placeHeld(Seat seat, DocxDrawings.Shape shape) {
        return seat.placeHeld(shape, tops.computeIfAbsent(seat, taken -> taken.paragraphTop(lines)), null);
    }

    /**
     * The paragraph on a shape's page whose text it stands nearest that can place it, however far
     * off; {@code null} where none can.
     */
    private Place placeBesideTheNearest(DocxDrawings.Shape shape, List<Seat> onItsPage) {
        List<Seat> byDistance = new ArrayList<>(onItsPage);
        byDistance.sort(java.util.Comparator.comparingDouble(seat -> distance(shape, seat)));
        for (Seat seat : byDistance) {
            Place place = placeHeld(seat, shape);
            if (place != null) {
                return place;
            }
        }
        return null;
    }

    /** How far a shape stands from a paragraph's text: down from its first line's top, and across past its edges. */
    private static double distance(DocxDrawings.Shape shape, Seat seat) {
        return Math.abs(shape.top() - seat.top())
               + Math.max(0, seat.left() - (shape.x() + shape.width()))
               + Math.max(0, shape.x() - seat.right());
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
            double by = distance(shape, seat);
            if (by <= REACH && by < nearestBy) {
                nearest = seat;
                nearestBy = by;
            }
        }
        // A shape rises above its paragraph's top by its own height at most, as a dot centred on
        // the paragraph's first line does: Word and LibreOffice place it there, out of a cell's
        // top too, and draw it whole. One rising further stands by other text: LibreOffice set an
        // 11pt rule 45pt above its paragraph's top at the head of the next page.
        return nearest == null ? null : nearest.place(shape, lines);
    }

    /**
     * Anchors shapes in the paragraph each was placed from — a paragraph of a block holding it, one
     * of the cell holding a composed cell's, the one nearest it, or the one whose text it stands
     * nearest — placed down from its top, in the order they are drawn: they move with the paragraph
     * wherever an edit takes it.
     */
    private void anchorBeside(XWPFParagraph paragraph, List<Placed> shapes) {
        if (shapes.stream().anyMatch(placed -> placed.place().setItsCell())
            && paragraph.getBody() instanceof XWPFTableCell cell) {
            readItFromItsTop(cell);
        }
        XWPFRun run = paragraph.insertNewRun(0);
        for (Placed placed : shapes) {
            Ordered ordered = placed.ordered();
            double column = placed.place().column();
            // A shape standing less than READABLE_RISE above the paragraph's top is placed at its
            // top instead: Word draws such an offset where it says, but reports it to a reader as
            // one of its position keywords. A shape higher keeps its own offset, which Word reports.
            double rise = placed.place().top() - ordered.shape().top();
            double top = rise > 0 && rise < READABLE_RISE ? ordered.shape().top() : placed.place().top();
            run.getCTR().addNewDrawing().set(Double.isInfinite(column)
                    ? DocxDrawings.drawingInParagraph(ordered.shape(), ids.getAsLong(), ordered.order(), ordered.drawn(), top)
                    : DocxDrawings.drawingInCell(ordered.shape(), ids.getAsLong(), ordered.order(), ordered.drawn(),
                            new DocxDrawings.CellOrigin(column, top)));
            carry(paragraph, ordered);
        }
    }

    /**
     * Reads a cell from its top, which it holds no text to be read any other way by: Word centres
     * a cell set to the middle along with the drawing its paragraph carries, which stood a tile
     * 7.10pt below where the page puts it in a row four lines tall, and 0.30pt from it once the
     * cell was read from its top (measured in Word 16.0.20527 and LibreOffice 24.2; in a row the
     * cell's own content fills, the two are the same place and nothing in the file moves).
     */
    private static void readItFromItsTop(XWPFTableCell cell) {
        CTTcPr properties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        if (properties.isSetVAlign()) {
            properties.getVAlign().setVal(
                    org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc.TOP);
        }
    }

    /**
     * How far from a paragraph's text a shape a table's composed cell draws still stands beside
     * it, across and down added, in points: an icon beside its label in another cell. One further
     * from every paragraph stays where the page puts it. A shape a node draws goes by the blocks
     * holding it, however far off ({@link #placeInItsHolder}).
     */
    private static final double REACH = 48;

    /** A stroke's width either side a shape may reach past its paragraph's top or its cell, in points. */
    private static final double SLACK = 1;

    /**
     * The smallest distance above a paragraph's top a shape may be placed from and still be read
     * back, in points: Word draws a smaller offset where it says, but reports it to a reader as one
     * of its position keywords rather than as a distance, and the editing protocol reads a
     * drawing's place through that. Measured in Word 16.0.20527: offsets of 0.05, 0.13, 0.21, 0.25
     * and 0.29pt above the paragraph came back as -999999, -999997, -999994 and -999993, while
     * 0.30pt and every larger offset — 0.40, 0.50, 0.60, 0.75, 0.97 — came back as itself. A
     * shape nearer the top than this is placed at the top, which costs less than three tenths of a
     * point and is where it practically stands.
     */
    private static final double READABLE_RISE = 0.3;

    /**
     * The furthest a shape a table's composed cell draws rises above its paragraph's top in the
     * body, in points: as far as a dot or an icon's disc centred on a line of text does, as
     * NavySidebar's 22.6pt badge did when every shape was placed by the text nearest it — Word and
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
     * @param path      the path of the node its text is written for; {@code null} for none
     */
    record Seat(XWPFParagraph paragraph, double left, double right, double top, double baseline, String path) {

        /** Whether its text is written for the node at a path, or for a node that node holds. */
        boolean isWithin(String holder) {
            return path != null && (path.equals(holder)
                                    || path.length() > holder.length() && path.startsWith(holder)
                                       && path.charAt(holder.length()) == '/');
        }

        /**
         * Where the page puts the top Word places a paragraph's shapes from: its first line's,
         * less the space written above it. NaN where something else stands above the line — a
         * top border, or space Word reckons in lines, by itself or against the paragraph before.
         *
         * <p>Word's line is not the page's: the export stands Word's baseline where the page's
         * is, and Word stands an exact line's baseline four fifths of the way down it, raised by
         * the runs' position, and a least line's its runs' descent above its foot
         * ({@link DocxGrownLines#lineOf}), which holds no position. A 15pt exact line round 8.6pt
         * text starts 4.6pt above the page's line, and a shape placed from the page's line stood
         * that much high.</p>
         *
         * @param lines how tall Word stands a paragraph's line, and its baseline in it
         */
        double paragraphTop(Function<CTP, DocxGrownLines.Line> lines) {
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
                    DocxGrownLines.Line line = lines.apply(paragraph.getCTP());
                    if (line != null) {
                        lineTop = baseline - (line.baseline() - raised());
                    } else if (spacing.isSetLineRule() && spacing.getLineRule() == STLineSpacingRule.EXACT) {
                        return Double.NaN;
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
         * Where Word places a shape a table's composed cell draws beside this paragraph's text
         * from, or {@code null} where it cannot; a shape a node draws is placed by
         * {@link #placeHeld}. The shape may rise above the paragraph's top as
         * {@code risesNoFurther} allows: by its own height in the body, by a stroke's width in a
         * table cell.
         *
         * <p>A paragraph of the body places it across from the page's edge. One in a table cell
         * places it from the cell's text column; a cell that does not hold the shape across hands
         * it to the cell of the same row that does, from that cell's first paragraph — a
         * timeline's dot stands in a column of its own beside its entry's text, in a cell holding
         * nothing else. None takes it for a line not set from the cell's left, whose indent is
         * written short of the page's, or in a repeated header row, which Word repeats on every
         * page with what is anchored in it.</p>
         */
        Place place(DocxDrawings.Shape shape, Function<CTP, DocxGrownLines.Line> lines) {
            double top = paragraphTop(lines);
            if (!(paragraph.getBody() instanceof XWPFTableCell cell)) {
                return risesNoFurther(shape, top, false) ? new Place(paragraph, Double.POSITIVE_INFINITY, top) : null;
            }
            if (!risesNoFurther(shape, top, true) || cell.getTableRow().isRepeatHeader() || !leftAligned(paragraph)) {
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
            return inTheRow(shape, cell, cellLeft, width, top, null);
        }

        /**
         * Where Word places a shape this paragraph carries for a block holding both, however far
         * from its text the shape stands; {@code null} where it cannot.
         *
         * <p>Measured in Word 16.0.20527 and LibreOffice 24.2: a shape anchored in a table cell's
         * paragraph, placed from the cell's text column and the paragraph's top, stands where both
         * say when it rises above its row or its table, hangs below them, stands out of its cell,
         * is taller than its row, sits in a row of an exact height or in a nested table, in front of
         * the text or behind it — from 36pt above its paragraph to 70pt below in probes, from 155pt
         * above to 620pt below in the fidelity corpus. Neither editor clips it or moves a line for
         * it, and both move it with the row when the text above it is edited. A cell set to the
         * middle or the bottom, or held in one, carries none: LibreOffice 24.2 sets the text of a
         * cell carrying a drawing from the cell's top, whatever drawing it is — 16pt and 32pt high
         * in a row three lines taller than it. A paragraph of the body places a shape across from
         * the page's edge.</p>
         *
         * <p>A cell holding the shape across takes it in this paragraph; one that does not hands it
         * to the cell of its row that does, the shape's own ({@link #inTheRow}), or else keeps it,
         * placed out of its column — unless it holds text, a text box or a badge's letters, which
         * LibreOffice 26.8 pulls back into the column. None takes it in a repeated header row,
         * which Word repeats on every page with what is anchored in it, nor for a line not set from
         * the cell's left, whose text column its text does not tell.</p>
         *
         * @param top    where Word places from in this paragraph ({@link #paragraphTop}); NaN where
         *               something above its line hides it
         * @param filled the cell whose box holds a drawing its table paints, where the page fills
         *               it, whose own vertical alignment is then left out of the walk; a cell
         *               carrying a drawing then has nothing to move, and nor has one holding its
         *               table ({@link HoldingCell}). {@code null} for a drawing a node paints,
         *               whose every cell is held to being set from its top.
         */
        Place placeHeld(DocxDrawings.Shape shape, double top, CTTc filled) {
            if (Double.isNaN(top)) {
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
            // Only the cell that ends up carrying the shape has to be set from its top: this one
            // may simply be where the row's text is measured, and hand the shape to a neighbour.
            Place here = setFromTheTop(cell, filled)
                    ? new Place(paragraph, cellLeft + leftMargin(cell), top,
                            !alignedToTheTop(cell) && !holdsAnyText(cell)) : null;
            if (holdsAcross(shape, cellLeft, width)) {
                return here;
            }
            Place own = inTheRow(shape, cell, cellLeft, width, top, filled);
            // LibreOffice 26.8 pulls a shape holding text back into the column of the cell it is
            // anchored in: a text box placed 120pt left of its column stood at the column's start.
            // A shape holding none it draws out of the column, as Word and LibreOffice 24.2 do both.
            return own != null || shape.text() != null ? own : here;
        }

        /**
         * Whether a cell may carry a drawing: it, and each cell holding its table, sets its content
         * from its top — or the page fills the cell that holds them all ({@code filled}), which
         * leaves that cell nothing to set lower and this one's own paragraph tops where the row
         * puts them, and this cell holds no text of its own. Both are measured in Word 16.0.20527
         * and LibreOffice 24.2 (see {@link HoldingCell}); the second is held to the first because a
         * cell with room to centre in centres the drawing along with the paragraph carrying it, so
         * where the drawing stands no longer tells where that paragraph starts.
         */
        private static boolean setFromTheTop(XWPFTableCell cell, CTTc filled) {
            for (XWPFTableCell at = cell; at != null; ) {
                boolean nothingToSetLower = filled != null && (at.getCTTc() == filled
                                                               || at == cell && !holdsAnyText(at));
                if (!alignedToTheTop(at) && !nothingToSetLower) {
                    return false;
                }
                at = at.getTableRow().getTable().getBody() instanceof XWPFTableCell holding ? holding : null;
            }
            return true;
        }

        /**
         * Whether a cell holds anything an editor could set lower, at any depth: a line of text —
         * a link's among it, which POI leaves out of a paragraph's own text
         * ({@link #firstRunOfText}) — or a picture or a drawing of its own, which an editor moves
         * with the text.
         */
        private static boolean holdsAnyText(XWPFTableCell cell) {
            for (IBodyElement element : cell.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph
                    && (new Seat(paragraph, 0, 0, 0, 0, null).holdsText() || draws(paragraph))) {
                    return true;
                }
                if (element instanceof org.apache.poi.xwpf.usermodel.XWPFTable table
                    && table.getRows().stream().flatMap(row -> row.getTableCells().stream())
                            .anyMatch(Seat::holdsAnyText)) {
                    return true;
                }
            }
            return false;
        }

        /** Whether a paragraph holds a picture or a drawing of its own, a link's among them. */
        private static boolean draws(XWPFParagraph paragraph) {
            for (org.apache.xmlbeans.XmlObject found : paragraph.getCTP().selectPath(
                    "declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' "
                    + "./w:r | ./w:hyperlink/w:r")) {
                if (found instanceof org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR run
                    && (run.sizeOfDrawingArray() > 0 || run.sizeOfPictArray() > 0)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Whether a shape rises above a paragraph's top no further than it may: in the body, by
         * its own height, as a dot centred on the paragraph's first line does, and
         * {@link #FURTHEST_RISE} at most; in a table cell, no further than it is placed at that
         * top ({@link #READABLE_RISE}), so no cell on this path is given a shape to carry above
         * its paragraph. A 20.5pt rule placed 1pt above its paragraph in a cell of a contact row
         * moved that row's text 1.8pt up in the Linux LibreOffice the build checks with, though
         * not in Word or LibreOffice on Windows. NaN for a top not known is no.
         *
         * <p>Only what a table's composed cell draws, and no cell's box holds, is held to this
         * ({@link DocxDrawingAnchors#placeBeside}). That contact row's cells are set to the middle,
         * and LibreOffice 24.2 sets such a cell's text from its top once it carries a drawing,
         * wherever the drawing stands; a cell set from its top moved no line for a drawing 36pt
         * above its paragraph ({@link #placeHeld}).</p>
         */
        private static boolean risesNoFurther(DocxDrawings.Shape shape, double top, boolean inCell) {
            // In a cell, short of the rise: one at it is written as its own offset, which is what
            // this holds a cell clear of.
            return inCell ? shape.top() > top - READABLE_RISE
                    : shape.top() >= top - Math.max(SLACK, Math.min(shape.height(), FURTHEST_RISE));
        }

        /**
         * The place in the cell of this paragraph's row that holds a shape across, from that
         * cell's first paragraph; {@code null} when none does, its top is not known, or a cell
         * crossed on the way has no width in twips — the walk stops there rather than guess.
         *
         * <p>Measured in Word 16.0.20430 and LibreOffice: in a row whose cells set their content
         * from the top, the first paragraph of each starts at the row's top. Both give every cell
         * of a row the row's largest top margin (see {@code DocxSemanticBackend.evenTheRowsMargins}),
         * so a neighbour's first paragraph starts where this one does, whatever margin it states.
         * One with a border above it is not taken: its border stands between its top and its line.</p>
         */
        private Place inTheRow(DocxDrawings.Shape shape, XWPFTableCell cell, double cellLeft, double width,
                               double top, CTTc filled) {
            List<IBodyElement> opening = cell.getBodyElements();
            if (opening.isEmpty() || opening.get(0) != paragraph || !(alignedToTheTop(cell) || filled != null)) {
                return null;
            }
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
                    return openingPlace(cells.get(index), edge, top, filled);
                }
            }
            edge = cellLeft + width;
            for (int index = at + 1; index < cells.size(); index++) {
                double neighbour = widthOf(cells.get(index));
                if (Double.isNaN(neighbour)) {
                    return null;
                }
                if (holdsAcross(shape, edge, neighbour)) {
                    return openingPlace(cells.get(index), edge, top, filled);
                }
                edge += neighbour;
            }
            return null;
        }

        /**
         * The place at the start of a cell whose left edge stands where given; {@code null} when
         * unknown. A cell set to the middle or the bottom takes the shape only for one its table's
         * cell is filled with ({@code filled}) and where it holds no text of its own to be set
         * lower ({@link #setFromTheTop}).
         */
        private static Place openingPlace(XWPFTableCell cell, double cellLeft, double top, CTTc filled) {
            CTTcPr properties = cell.getCTTc().getTcPr();
            List<IBodyElement> opening = cell.getBodyElements();
            if (properties != null && properties.isSetVMerge() || opening.isEmpty()
                || !setFromTheTop(cell, filled)
                || !(opening.get(0) instanceof XWPFParagraph first) || borderedAbove(first)) {
                return null;
            }
            return new Place(first, cellLeft + leftMargin(cell), top, !alignedToTheTop(cell));
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

        /** Whether a paragraph has a border above it. */
        private static boolean borderedAbove(XWPFParagraph paragraph) {
            CTPPr properties = paragraph.getCTP().getPPr();
            return properties != null && properties.isSetPBdr() && properties.getPBdr().isSetTop();
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
     * @param paragraph   the paragraph it is anchored in
     * @param column      where Word's column starts, from the page's left edge; infinite for a
     *                    paragraph of the body, whose shapes are placed from the page's edge
     * @param top         where the paragraph's top stands, from the page's top edge
     * @param setItsCell  whether the paragraph's cell is to be read from its top, which it is
     *                    where it is set to the middle or the bottom and holds no text: Word
     *                    centres such a cell's paragraph along with the drawing it carries, and
     *                    the row's top is where the page measured the paragraph this shape is
     *                    placed from. Measured in Word 16.0.20527: a tile carried by a cell set
     *                    to the middle of a row four lines tall stood 7.10pt below where the page
     *                    puts it, and 0.30pt from it once that cell was read from its top; in a
     *                    row the cell's own content fills, the two are the same place. Nothing
     *                    moves with it, the cell holding no text ({@link Seat#holdsAnyText})
     */
    record Place(XWPFParagraph paragraph, double column, double top, boolean setItsCell) {

        /** A place in a paragraph whose cell is left as it is. */
        Place(XWPFParagraph paragraph, double column, double top) {
            this(paragraph, column, top, false);
        }
    }

    /** A shape and where it is placed from. */
    private record Placed(Ordered ordered, Place place) {
    }

    private void anchor(XWPFParagraph carrier, List<Ordered> shapes) {
        XWPFRun run = carrier.insertNewRun(0);
        for (Ordered shape : shapes) {
            run.getCTR().addNewDrawing().set(DocxDrawings.drawing(shape.shape(), ids.getAsLong(), shape.order(), shape.drawn()));
            carry(carrier, shape);
        }
    }
}
