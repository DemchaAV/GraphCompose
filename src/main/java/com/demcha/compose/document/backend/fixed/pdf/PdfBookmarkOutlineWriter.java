package com.demcha.compose.document.backend.fixed.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Internal helper that builds a PDF outline from canonical semantic fragment
 * bookmark metadata.
 */
final class PdfBookmarkOutlineWriter {
    private PdfBookmarkOutlineWriter() {
    }

    static void apply(PDDocument document, List<PdfRenderEnvironment.BookmarkRecord> bookmarks) {
        if (bookmarks == null || bookmarks.isEmpty()) {
            return;
        }

        List<PdfRenderEnvironment.BookmarkRecord> ordered = bookmarks.stream()
                .sorted(Comparator
                        .comparingInt(PdfRenderEnvironment.BookmarkRecord::pageIndex)
                        .thenComparing(PdfRenderEnvironment.BookmarkRecord::y, Comparator.reverseOrder()))
                .toList();

        PDDocumentOutline outline = new PDDocumentOutline();
        Deque<PDOutlineItem> parentStack = new ArrayDeque<>();
        Deque<Integer> levelStack = new ArrayDeque<>();

        for (PdfRenderEnvironment.BookmarkRecord bookmark : ordered) {
            PDOutlineItem item = new PDOutlineItem();
            item.setTitle(bookmark.title());

            if (bookmark.pageIndex() >= 0 && bookmark.pageIndex() < document.getNumberOfPages()) {
                PDPage page = document.getPage(bookmark.pageIndex());
                PDPageXYZDestination destination = new PDPageXYZDestination();
                destination.setPage(page);
                destination.setTop((int) bookmark.y());
                destination.setLeft(0);
                item.setDestination(destination);
            }

            while (!levelStack.isEmpty() && levelStack.peek() >= bookmark.level()) {
                parentStack.pop();
                levelStack.pop();
            }

            if (parentStack.isEmpty()) {
                outline.addLast(item);
            } else {
                parentStack.peek().addLast(item);
            }

            parentStack.push(item);
            levelStack.push(bookmark.level());
        }

        outline.openNode();
        document.getDocumentCatalog().setDocumentOutline(outline);
    }
}
