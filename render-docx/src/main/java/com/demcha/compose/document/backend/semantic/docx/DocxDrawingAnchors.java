package com.demcha.compose.document.backend.semantic.docx;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Where each shape {@link DocxDrawings} draws is anchored: in a paragraph written on its page.
 *
 * <p>A shape is positioned from the page's edges, so any paragraph on its page carries it to
 * the right place — but only one on that page: an anchor takes its shape to the page the
 * anchoring paragraph lands on. And a paragraph of the body rather than of a table cell: Word
 * prints a shape anchored in a cell clipped to that cell, so a timeline's rings anchored in the
 * sidebar's cell came out cut where the sidebar ends. The first body paragraph written on each
 * page is kept; a shape whose page already has one is anchored there at once, and one whose
 * page has none yet waits for it. The run is put at the front of the paragraph, so a
 * paragraph that runs on to the next page does not take the shape with it.</p>
 *
 * <p>A page laid out entirely in a table — a two-column CV is one row of two cells — has no
 * body paragraph. On the section's first page a paragraph a hairline tall is opened before the
 * table to carry its shapes; on its last, the paragraph closing the section does; on any other,
 * the first paragraph of a cell on that page, clipped as it may be, is still better than none.
 * Pages are counted within a section, as the layout counts them, so a section ends with
 * {@link #endSection}.</p>
 */
final class DocxDrawingAnchors {

    // Shapes waiting for a paragraph on their page, by page.
    private final Map<Integer, List<DocxDrawings.Shape>> pending = new TreeMap<>();
    // The first body paragraph written on each page of the section, by page.
    private final Map<Integer, XWPFParagraph> bodyParagraph = new HashMap<>();
    // The first table-cell paragraph written on each page of the section, by page.
    private final Map<Integer, XWPFParagraph> cellParagraph = new HashMap<>();
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
        order = 0;
    }

    /**
     * Anchors shapes in the body paragraph already written on their page, or keeps them for
     * the first one written there.
     */
    void queue(List<DocxDrawings.Shape> shapes) {
        for (DocxDrawings.Shape shape : shapes) {
            XWPFParagraph carrier = bodyParagraph.get(shape.page());
            if (carrier != null) {
                anchor(carrier, List.of(shape));
            } else {
                pending.computeIfAbsent(shape.page(), page -> new ArrayList<>()).add(shape);
            }
        }
    }

    /**
     * Records a paragraph written on a page. The page's first body paragraph takes the shapes
     * waiting for it; a cell's is kept in case the page has no other.
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
        if (bodyParagraph.putIfAbsent(page, paragraph) != null) {
            return;
        }
        List<DocxDrawings.Shape> waiting = pending.remove(page);
        if (waiting != null && !waiting.isEmpty()) {
            anchor(paragraph, waiting);
        }
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
        pending.forEach((page, shapes) -> {
            XWPFParagraph carrier;
            if (page == 0 && opening != null) {
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
        pending.clear();
        bodyParagraph.clear();
        cellParagraph.clear();
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

    private void anchor(XWPFParagraph carrier, List<DocxDrawings.Shape> shapes) {
        XWPFRun run = carrier.insertNewRun(0);
        for (DocxDrawings.Shape shape : shapes) {
            run.getCTR().addNewDrawing().set(DocxDrawings.drawing(shape, ids.getAsLong(), order++));
        }
    }
}
