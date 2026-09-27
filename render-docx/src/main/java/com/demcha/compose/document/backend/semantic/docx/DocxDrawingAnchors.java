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
 * anchoring paragraph lands on. The first paragraph written on each page is kept; a shape
 * whose page already has one is anchored there at once, and one whose page has none yet waits
 * for it. The run is put at the front of the paragraph, so a paragraph that runs on to the
 * next page does not take the shape with it.</p>
 *
 * <p>Pages are counted within a section, as the layout counts them, so a section ends with
 * {@link #endSection}.</p>
 */
final class DocxDrawingAnchors {

    // Shapes waiting for a paragraph on their page, by page.
    private final Map<Integer, List<DocxDrawings.Shape>> pending = new TreeMap<>();
    // The first paragraph written on each page of the section, by page.
    private final Map<Integer, XWPFParagraph> paragraphOnPage = new HashMap<>();
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
        paragraphOnPage.clear();
        order = 0;
    }

    /**
     * Anchors shapes in the paragraph already written on their page, or keeps them for the
     * first one written there.
     */
    void queue(List<DocxDrawings.Shape> shapes) {
        for (DocxDrawings.Shape shape : shapes) {
            XWPFParagraph carrier = paragraphOnPage.get(shape.page());
            if (carrier != null) {
                anchor(carrier, List.of(shape));
            } else {
                pending.computeIfAbsent(shape.page(), page -> new ArrayList<>()).add(shape);
            }
        }
    }

    /**
     * Records a paragraph written on a page, and anchors in the page's first one the shapes
     * waiting for it.
     *
     * @param page      the page, counted within the section
     * @param paragraph a paragraph the layout puts on that page
     */
    void paragraphOn(int page, XWPFParagraph paragraph) {
        if (page < 0 || paragraphOnPage.putIfAbsent(page, paragraph) != null) {
            return;
        }
        List<DocxDrawings.Shape> waiting = pending.remove(page);
        if (waiting != null && !waiting.isEmpty()) {
            anchor(paragraph, waiting);
        }
    }

    /**
     * Ends a section: the shapes waiting on its last page go into the carrier, a paragraph
     * added at the end still being on that page; those on an earlier page no paragraph was
     * written on are returned, and forgotten.
     *
     * @param lastPage the section's last page
     * @param carrier  a paragraph at the section's end, asked for only when one is needed
     * @return how many shapes each page nothing carried had, by page
     */
    Map<Integer, Integer> endSection(int lastPage, Supplier<XWPFParagraph> carrier) {
        List<DocxDrawings.Shape> onTheLastPage = pending.remove(lastPage);
        if (onTheLastPage != null && !onTheLastPage.isEmpty()) {
            anchor(carrier.get(), onTheLastPage);
        }
        Map<Integer, Integer> dropped = new TreeMap<>();
        pending.forEach((page, shapes) -> dropped.put(page, shapes.size()));
        pending.clear();
        paragraphOnPage.clear();
        return dropped;
    }

    private void anchor(XWPFParagraph carrier, List<DocxDrawings.Shape> shapes) {
        XWPFRun run = carrier.insertNewRun(0);
        for (DocxDrawings.Shape shape : shapes) {
            run.getCTR().addNewDrawing().set(DocxDrawings.drawing(shape, ids.getAsLong(), order++));
        }
    }
}
