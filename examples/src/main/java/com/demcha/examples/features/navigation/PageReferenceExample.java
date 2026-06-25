package com.demcha.examples.features.navigation;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for v1.9 page references: {@code DocumentSession.pageIndex()}
 * resolves a declared {@code anchor(...)} to its final page, so a document can
 * print a real "see page N" cross-reference.
 *
 * <p>It is a two-pass workflow — exactly what {@code pageIndex()} exists for. A
 * throwaway first pass lays out the document and reads the anchor's page; the
 * second pass renders the same document with the resolved number substituted.</p>
 *
 * <pre>{@code
 * int page = probe.pageIndex().pageNumberOf("appendix").orElse(0);
 * // ... render again, printing "see page " + page
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class PageReferenceExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(96, 102, 112);

    private PageReferenceExample() {
    }

    /**
     * Renders a two-page note whose first page cross-references the appendix by
     * its resolved page number.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/navigation", "page-reference.pdf");

        // Pass 1 — throwaway layout to resolve the anchor's page. The appendix is
        // forced onto its own page by the break, so its page is stable regardless
        // of the (still-unresolved) reference text on page 1.
        int appendixPage;
        try (DocumentSession probe = newSession(null)) {
            compose(probe, 0);
            appendixPage = probe.pageIndex().pageNumberOf("appendix").orElse(0);
        }

        // Pass 2 — render with the resolved cross-reference.
        try (DocumentSession document = newSession(pdfFile)) {
            compose(document, appendixPage);
            document.buildPdf();
        }
        return pdfFile;
    }

    private static DocumentSession newSession(Path output) {
        return (output == null ? GraphCompose.document() : GraphCompose.document(output))
                .pageSize(320, 240)
                .margin(DocumentInsets.of(30))
                .create();
    }

    private static void compose(DocumentSession session, int appendixPage) {
        DocumentTextStyle title = DocumentTextStyle.DEFAULT.withSize(18).withColor(INK);
        DocumentTextStyle body = DocumentTextStyle.DEFAULT.withSize(11).withColor(INK);
        DocumentTextStyle ref = DocumentTextStyle.DEFAULT.withSize(11).withColor(MUTED);

        session.pageFlow(page -> {
            page.addParagraph(p -> p.text("Release notes").textStyle(title));
            String reference = appendixPage > 0
                    ? "Full configuration options are listed in the Appendix on page " + appendixPage + "."
                    : "Full configuration options are listed in the Appendix.";
            page.addParagraph(p -> p.text(reference).textStyle(ref).padding(DocumentInsets.top(10)));

            page.addPageBreak(b -> b.name("toAppendix"));

            page.addSection(s -> s.anchor("appendix")
                    .addParagraph(p -> p.text("Appendix").textStyle(title)));
            page.addParagraph(p -> p.text("Configuration keys, defaults, and units.")
                    .textStyle(body).padding(DocumentInsets.top(6)));
        });
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
