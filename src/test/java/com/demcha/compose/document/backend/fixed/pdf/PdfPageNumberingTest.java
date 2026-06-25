package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentPageNumberStyle;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof that {@link DocumentPageNumbering} on a public
 * {@code chrome().footer(...)} reaches the rendered PDF: an offset/restart roman
 * footer and a suppress-on-first-page footer, read back per page with
 * {@link PDFTextStripper}.
 */
class PdfPageNumberingTest {

    @TempDir
    Path tempDir;

    private Path renderFourPages(String name, DocumentPageNumbering numbering) throws Exception {
        Path out = tempDir.resolve(name + ".pdf");
        try (DocumentSession session = GraphCompose.document(out)
                .pageSize(220, 200)
                .margin(DocumentInsets.of(24))
                .create()) {
            session.chrome().footer(DocumentHeaderFooter.builder()
                    .centerText("{page} / {pages}")
                    .numbering(numbering)
                    .build());
            session.pageFlow(page -> {
                page.addParagraph("Alpha");
                page.addPageBreak(b -> b.name("b1"));
                page.addParagraph("Beta");
                page.addPageBreak(b -> b.name("b2"));
                page.addParagraph("Gamma");
                page.addPageBreak(b -> b.name("b3"));
                page.addParagraph("Delta");
            });
            session.buildPdf();
        }
        return out;
    }

    private static String pageText(PDDocument doc, int oneBasedPage) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(oneBasedPage);
        stripper.setEndPage(oneBasedPage);
        return stripper.getText(doc);
    }

    @Test
    void romanFrontMatterOffsetReachesTheRenderedFooter() throws Exception {
        // countFrom=2: physical page 1 is uncounted (no footer); the body restarts at i.
        Path pdf = renderFourPages("roman", DocumentPageNumbering.builder()
                .style(DocumentPageNumberStyle.LOWER_ROMAN)
                .countFrom(2)
                .startAt(1)
                .build());

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(doc.getNumberOfPages()).isEqualTo(4);
            assertThat(pageText(doc, 1)).doesNotContain("/");        // uncounted: no footer number
            assertThat(pageText(doc, 2)).contains("i / iii");        // counted total = 1+(4-2) = iii
            assertThat(pageText(doc, 3)).contains("ii / iii");
            assertThat(pageText(doc, 4)).contains("iii / iii");
        }
    }

    @Test
    void showOnFirstPageFalseSuppressesOnlyTheFirstNumber() throws Exception {
        Path pdf = renderFourPages("suppress", DocumentPageNumbering.builder()
                .showOnFirstPage(false)
                .build());

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertThat(pageText(doc, 1)).doesNotContain("/");        // first-page number hidden
            assertThat(pageText(doc, 2)).contains("2 / 4");          // decimal, no offset
            assertThat(pageText(doc, 4)).contains("4 / 4");
        }
    }
}
