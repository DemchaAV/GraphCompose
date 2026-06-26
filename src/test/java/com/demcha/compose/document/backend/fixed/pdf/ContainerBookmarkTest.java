package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code AbstractFlowBuilder.bookmark(...)} makes a container (section / page flow)
 * a PDF outline target — an entry in the bookmark panel pointing at the
 * container's start page, even when the container has no fill or border.
 */
class ContainerBookmarkTest {

    @TempDir
    Path tempDir;

    private static List<PDOutlineItem> outline(PDDocument doc) {
        List<PDOutlineItem> items = new ArrayList<>();
        PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
        if (outline != null) {
            for (PDOutlineItem item : outline.children()) {
                items.add(item);
            }
        }
        return items;
    }

    private static List<String> titles(PDDocument doc) {
        return outline(doc).stream().map(PDOutlineItem::getTitle).toList();
    }

    private DocumentSession session(Path out) {
        return GraphCompose.document(out).pageSize(260, 200).margin(DocumentInsets.of(24)).create();
    }

    @Test
    void bookmarkedSectionsBecomeOutlineEntriesAtTheirPages() throws Exception {
        Path out = tempDir.resolve("bookmarks.pdf");
        try (DocumentSession session = session(out)) {
            session.pageFlow(page -> {
                page.addSection(s -> s.bookmark(new DocumentBookmarkOptions("Chapter 1")).addParagraph("One"));
                page.addPageBreak(b -> b.name("br"));
                page.addSection(s -> s.bookmark(new DocumentBookmarkOptions("Chapter 2")).addParagraph("Two"));
            });
            session.buildPdf();
        }

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(titles(doc)).containsExactly("Chapter 1", "Chapter 2");
            PDOutlineItem chapter2 = outline(doc).get(1);
            // Chapter 2 follows a page break, so its outline destination is page 2 (index 1).
            assertThat(((PDPageDestination) chapter2.getDestination()).retrievePageNumber()).isEqualTo(1);
        }
    }

    @Test
    void anUnstyledSectionStillEmitsItsBookmark() throws Exception {
        // No fill / border, so the decoration fragment is suppressed — the bookmark
        // rides its own marker fragment, not the decoration, so it still appears.
        Path out = tempDir.resolve("plain.pdf");
        try (DocumentSession session = session(out)) {
            session.pageFlow(page -> page.addSection(s -> s
                    .bookmark(new DocumentBookmarkOptions("Plain section")).addParagraph("body")));
            session.buildPdf();
        }

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(titles(doc)).containsExactly("Plain section");
        }
    }

    @Test
    void aBookmarkedMultiPageFlowEmitsItsEntryExactlyOnce() throws Exception {
        // The page flow is a ContainerNode (not a SectionNode) and spans two pages;
        // its bookmark must appear once, at the start page — not per page slice.
        Path out = tempDir.resolve("pageflow.pdf");
        try (DocumentSession session = session(out)) {
            session.pageFlow(page -> {
                page.bookmark(new DocumentBookmarkOptions("Document"));
                page.addSection(s -> s.addParagraph("Front"));
                page.addPageBreak(b -> b.name("br"));
                page.addSection(s -> s.addParagraph("Back"));
            });
            session.buildPdf();
        }

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
            assertThat(titles(doc)).containsExactly("Document");
            assertThat(((PDPageDestination) outline(doc).get(0).getDestination()).retrievePageNumber()).isZero();
        }
    }

    @Test
    void aSectionWithoutABookmarkAddsNoOutlineEntry() throws Exception {
        Path out = tempDir.resolve("none.pdf");
        try (DocumentSession session = session(out)) {
            session.pageFlow(page -> page.addSection(s -> s.addParagraph("body")));
            session.buildPdf();
        }

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            assertThat(titles(doc)).isEmpty();
        }
    }

    @Test
    void backwardCompatibleSectionConstructorDefaultsToNoBookmark() {
        // The pre-bookmark 12-arg SectionNode constructor still resolves; bookmark is null.
        com.demcha.compose.document.node.SectionNode section = new com.demcha.compose.document.node.SectionNode(
                "s", List.of(), 0.0, DocumentInsets.zero(), DocumentInsets.zero(),
                null, null, null, null, false, null, com.demcha.compose.document.style.DocumentBleed.none());
        assertThat(section.bookmarkOptions()).isNull();
    }
}
