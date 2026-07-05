package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.MultiSectionDocument;
import com.demcha.compose.document.output.DocumentViewerPreferences;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPageLayout;
import com.demcha.compose.document.style.DocumentPageMode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PageLayout;
import org.apache.pdfbox.pdmodel.PageMode;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code chrome().viewerPreferences(...)} writes page mode, page layout, and the
 * window-chrome flags to the PDF document catalog. Asserted against the catalog
 * dictionary (PDFBox), not pixels — viewer preferences are advisory and many
 * readers honour only a subset.
 */
class ViewerPreferencesTest {

    @TempDir
    Path tempDir;

    private PDDocument render(String name, Consumer<DocumentSession> chrome) throws Exception {
        Path out = tempDir.resolve(name + ".pdf");
        try (DocumentSession session = GraphCompose.document(out)
                .pageSize(240, 200).margin(DocumentInsets.of(24)).create()) {
            chrome.accept(session);
            session.pageFlow(page -> page.addParagraph("content"));
            session.buildPdf();
        }
        return Loader.loadPDF(out.toFile());
    }

    @Test
    void useOutlinesPageModeIsWrittenToTheCatalog() throws Exception {
        try (PDDocument doc = render("outline",
                s -> s.chrome().viewerPreferences(DocumentViewerPreferences.openOutline()))) {
            assertThat(doc.getDocumentCatalog().getPageMode()).isEqualTo(PageMode.USE_OUTLINES);
        }
    }

    @Test
    void pageLayoutAndWindowFlagsAreWrittenToTheCatalog() throws Exception {
        try (PDDocument doc = render("layout", s -> s.chrome().viewerPreferences(
                DocumentViewerPreferences.builder()
                        .pageLayout(DocumentPageLayout.TWO_COLUMN_LEFT)
                        .displayDocTitle(true)
                        .hideToolbar(true)
                        .fitWindow(true)
                        .build()))) {
            PDDocumentCatalog catalog = doc.getDocumentCatalog();
            assertThat(catalog.getPageLayout()).isEqualTo(PageLayout.TWO_COLUMN_LEFT);
            PDViewerPreferences prefs = catalog.getViewerPreferences();
            assertThat(prefs).isNotNull();
            assertThat(prefs.displayDocTitle()).isTrue();
            assertThat(prefs.hideToolbar()).isTrue();
            assertThat(prefs.fitWindow()).isTrue();
            // unset flags stay at the reader default (false)
            assertThat(prefs.centerWindow()).isFalse();
        }
    }

    @Test
    void noViewerPreferencesLeavesTheCatalogUntouched() throws Exception {
        try (PDDocument doc = render("none", s -> { })) {
            // A document that sets no viewer preferences gets no /ViewerPreferences
            // dictionary and the default page mode — unchanged from before the feature.
            assertThat(doc.getDocumentCatalog().getViewerPreferences()).isNull();
            assertThat(doc.getDocumentCatalog().getPageMode()).isEqualTo(PageMode.USE_NONE);
        }
    }

    @Test
    void hideMenubarAndCenterWindowFlagsRoundTrip() throws Exception {
        try (PDDocument doc = render("morewindowflags", s -> s.chrome().viewerPreferences(
                DocumentViewerPreferences.builder()
                        .hideMenubar(true)
                        .centerWindow(true)
                        .build()))) {
            PDViewerPreferences prefs = doc.getDocumentCatalog().getViewerPreferences();
            assertThat(prefs).isNotNull();
            assertThat(prefs.hideMenubar()).isTrue();
            assertThat(prefs.centerWindow()).isTrue();
        }
    }

    @Test
    void anExplicitlyFalseFlagStillWritesTheDictionary() throws Exception {
        // A flag set to false is an explicit choice, not "unset", so the dictionary
        // is written (the value would read false either way; the dict's presence is
        // what proves the flag was honoured rather than skipped).
        try (PDDocument doc = render("falseflag", s -> s.chrome().viewerPreferences(
                DocumentViewerPreferences.builder().hideToolbar(false).build()))) {
            PDViewerPreferences prefs = doc.getDocumentCatalog().getViewerPreferences();
            assertThat(prefs).isNotNull();
            assertThat(prefs.hideToolbar()).isFalse();
        }
    }

    @Test
    void multiSectionTakesViewerPreferencesFromTheFirstSectionThatSetsThem() throws Exception {
        Path out = tempDir.resolve("multi.pdf");
        DocumentSession cover = GraphCompose.document().pageSize(240, 200).margin(DocumentInsets.of(24)).create();
        cover.chrome().viewerPreferences(DocumentViewerPreferences.openOutline());
        cover.pageFlow(page -> page.addParagraph("cover"));

        DocumentSession body = GraphCompose.document().pageSize(240, 200).margin(DocumentInsets.of(24)).create();
        // A different page mode on a later section must lose to the first.
        body.chrome().viewerPreferences(DocumentViewerPreferences.builder()
                .pageMode(DocumentPageMode.USE_THUMBNAILS).build());
        body.pageFlow(page -> page.addParagraph("body"));

        try (MultiSectionDocument doc = GraphCompose.documents(out).section(cover).section(body).create()) {
            doc.buildPdf();
        }

        try (PDDocument pdf = Loader.loadPDF(out.toFile())) {
            assertThat(pdf.getDocumentCatalog().getPageMode()).isEqualTo(PageMode.USE_OUTLINES);
        }
    }

    @Test
    void pageModeWithoutWindowFlagsWritesNoViewerPreferencesDictionary() throws Exception {
        try (PDDocument doc = render("modeonly",
                s -> s.chrome().viewerPreferences(DocumentViewerPreferences.openOutline()))) {
            // Page mode lives directly on the catalog; with no window flags there is
            // no /ViewerPreferences dict to write.
            assertThat(doc.getDocumentCatalog().getPageMode()).isEqualTo(PageMode.USE_OUTLINES);
            assertThat(doc.getDocumentCatalog().getViewerPreferences()).isNull();
        }
    }
}
