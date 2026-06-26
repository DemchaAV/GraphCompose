package com.demcha.examples.features.navigation;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for v1.9 container {@code bookmark(...)}: a section (or any
 * container flow) becomes a PDF outline target. Each bookmarked section adds an
 * entry to the reader's bookmark panel pointing at the section's start page — a
 * navigable outline for a structured document, with no manual coordinates. The
 * outline is a viewer-panel feature; the page itself shows the report content.
 *
 * <pre>{@code
 * flow.addSection(s -> s.bookmark(new DocumentBookmarkOptions("2. Methodology"))
 *     .addParagraph(heading)
 *     .addParagraph(body));
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class ContainerBookmarkExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 126, 135);

    private static final String[][] CHAPTERS = {
            {"1. Introduction", "Why this report exists and what it covers."},
            {"2. Methodology", "How the data was gathered and analysed."},
            {"3. Results", "What the analysis found, in brief."},
            {"4. Conclusion", "What it means and what to do next."},
    };

    private ContainerBookmarkExample() {
    }

    /**
     * Renders a short report whose sections are each a PDF outline entry.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/navigation", "container-bookmark.pdf");

        DocumentTextStyle heading = DocumentTextStyle.DEFAULT.withSize(13).withColor(INK);
        DocumentTextStyle body = DocumentTextStyle.DEFAULT.withSize(10).withColor(MUTED);

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(360, 320)
                .margin(DocumentInsets.of(34))
                .create()) {
            document.pageFlow(page -> {
                page.addParagraph(p -> p.text("Quarterly Report")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(18).withColor(INK)));
                page.addParagraph(p -> p.text("each section is a bookmark — open the reader's outline panel")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(9).withColor(MUTED))
                        .padding(DocumentInsets.bottom(10)));

                for (String[] chapter : CHAPTERS) {
                    chapterSection(page, heading, body, chapter[0], chapter[1]);
                }
            });

            document.buildPdf();
        }

        return pdfFile;
    }

    private static void chapterSection(PageFlowBuilder page,
                                       DocumentTextStyle heading,
                                       DocumentTextStyle body,
                                       String title,
                                       String summary) {
        page.addSection(s -> s.bookmark(new DocumentBookmarkOptions(title))
                .spacing(2)
                .addParagraph(p -> p.text(title).textStyle(heading))
                .addParagraph(p -> p.text(summary).textStyle(body)));
        page.addSpacer(s -> s.height(8));
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
