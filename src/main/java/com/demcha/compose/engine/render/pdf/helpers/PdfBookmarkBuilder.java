package com.demcha.compose.engine.render.pdf.helpers;

import com.demcha.compose.engine.components.content.bookmark.BookmarkEntry;
import com.demcha.compose.engine.components.core.Entity;
import com.demcha.compose.engine.components.layout.coordinator.Placement;
import com.demcha.compose.engine.core.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * PDF-specific helper that builds a bookmark (outline) tree from
 * {@link BookmarkEntry} components attached to entities.
 *
 * <p>Call this after layout and rendering have resolved placements and pages.</p>
 *
 * @author Artem Demchyshyn
 */
@Slf4j
public final class PdfBookmarkBuilder {

    private PdfBookmarkBuilder() {
        // static helper
    }

    /**
     * Scans all entities for {@link BookmarkEntry} components and builds a
     * {@link PDDocumentOutline} tree in the document catalog.
     *
     * @param doc           the PDF document
     * @param entityManager the entity manager with all resolved entities
     */
    public static void buildOutline(PDDocument doc, EntityManager entityManager) {
        List<BookmarkRecord> records = collectBookmarks(entityManager);
        if (records.isEmpty()) return;

        PDDocumentOutline outline = new PDDocumentOutline();

        Deque<PDOutlineItem> parentStack = new ArrayDeque<>();
        Deque<Integer> levelStack = new ArrayDeque<>();

        for (BookmarkRecord record : records) {
            PDOutlineItem item = new PDOutlineItem();
            item.setTitle(record.title());

            // Create destination
            PDPageXYZDestination dest = new PDPageXYZDestination();
            if (record.pageIndex() >= 0 && record.pageIndex() < doc.getNumberOfPages()) {
                PDPage page = doc.getPage(record.pageIndex());
                dest.setPage(page);
                dest.setTop((int) record.y());
                dest.setLeft(0);
                item.setDestination(dest);
            }

            // Determine parent based on level
            while (!levelStack.isEmpty() && levelStack.peek() >= record.level()) {
                parentStack.pop();
                levelStack.pop();
            }

            if (parentStack.isEmpty()) {
                outline.addLast(item);
            } else {
                parentStack.peek().addLast(item);
            }

            parentStack.push(item);
            levelStack.push(record.level());
        }

        outline.openNode();
        doc.getDocumentCatalog().setDocumentOutline(outline);
    }

    private static List<BookmarkRecord> collectBookmarks(EntityManager entityManager) {
        List<BookmarkRecord> records = new ArrayList<>();

        entityManager.getEntities().forEach((uuid, entity) -> {
            entity.getComponent(BookmarkEntry.class).ifPresent(bookmark -> {
                Placement placement = entity.getComponent(Placement.class).orElse(null);
                if (placement != null) {
                    records.add(new BookmarkRecord(
                            bookmark.getTitle(),
                            bookmark.getLevel(),
                            placement.startPage(),
                            placement.y() + placement.height()
                    ));
                }
            });
        });

        // Sort by page index ascending, then by Y descending.
        // In GraphCompose's coordinate system, Y decreases as content flows
        // down the page. Higher Y = higher on the page = earlier in document.
        records.sort((a, b) -> {
            int pageCmp = Integer.compare(a.pageIndex(), b.pageIndex());
            if (pageCmp != 0) return pageCmp;
            return Double.compare(b.y(), a.y()); // descending Y = top first
        });

        return records;
    }

    private record BookmarkRecord(String title, int level, int pageIndex, double y) {
    }
}
