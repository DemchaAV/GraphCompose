package com.demcha.examples.features.navigation;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLeader;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for v1.9 {@code addTableOfContents(...)}: a native, clickable
 * table of contents. Each {@code entry(label, anchor)} becomes a row whose label
 * links to the chapter, a dotted leader fills the gap, and the page number is
 * resolved automatically from the laid-out document — no manual two-pass.
 *
 * <pre>{@code
 * flow.addTableOfContents(toc -> toc.title("Contents")
 *     .leader(DocumentLeader.DOTS)
 *     .entry("Introduction", "intro")
 *     .entry("Appendix", "appendix"));
 * // ... chapters declared with .anchor("intro"), .anchor("appendix"), ...
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class TocExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 126, 135);

    private static final String[][] CHAPTERS = {
            {"Introduction", "intro"},
            {"Getting started", "start"},
            {"A longer chapter title that runs on", "longer"},
            {"Configuration reference", "config"},
            {"Appendix", "appendix"},
    };

    private TocExample() {
    }

    /**
     * Renders a table-of-contents page followed by one page per chapter, with the
     * contents page numbers resolved automatically.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/navigation", "table-of-contents.pdf");

        DocumentTextStyle title = DocumentTextStyle.DEFAULT.withSize(20).withColor(INK);
        DocumentTextStyle entry = DocumentTextStyle.DEFAULT.withSize(11).withColor(INK);
        DocumentTextStyle number = DocumentTextStyle.DEFAULT.withSize(11).withColor(MUTED);
        DocumentTextStyle chapterHeading = DocumentTextStyle.DEFAULT.withSize(18).withColor(INK);

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(360, 280)
                .margin(DocumentInsets.of(34))
                .create()) {
            document.pageFlow(page -> {
                page.addTableOfContents(toc -> {
                    toc.title("Contents").titleStyle(title)
                            .leader(DocumentLeader.DOTS)
                            .entryStyle(entry)
                            .pageNumberStyle(number);
                    for (String[] chapter : CHAPTERS) {
                        toc.entry(chapter[0], chapter[1]);
                    }
                });

                for (String[] chapter : CHAPTERS) {
                    page.addPageBreak(b -> b.name("to_" + chapter[1]));
                    page.addSection(s -> s.anchor(chapter[1])
                            .addParagraph(p -> p.text(chapter[0]).textStyle(chapterHeading)));
                }
            });
            document.buildPdf();
        }

        return pdfFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
