package com.demcha.examples.features.chrome;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentViewerPreferences;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPageMode;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for v1.9 {@code chrome().viewerPreferences(...)}: how a PDF
 * reader presents the document when it opens. Here the page mode is
 * {@code USE_OUTLINES} so the reader opens with the bookmark panel showing — it
 * pairs with {@code bookmark(...)} on the sections — and {@code displayDocTitle}
 * shows the document title in the window title bar. Viewer preferences are
 * advisory; readers honour the ones they support.
 *
 * <pre>{@code
 * document.chrome().viewerPreferences(DocumentViewerPreferences.builder()
 *     .pageMode(DocumentPageMode.USE_OUTLINES)  // open the bookmark panel
 *     .displayDocTitle(true)
 *     .build());
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class ViewerPreferencesExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 126, 135);

    private static final String[][] CHAPTERS = {
            {"Habitat", "Where the species lives."},
            {"Diet", "What it eats through the seasons."},
            {"Behaviour", "How it moves, nests, and migrates."},
    };

    private ViewerPreferencesExample() {
    }

    /**
     * Renders a short guide that opens with the bookmark panel showing.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/chrome", "viewer-preferences.pdf");

        DocumentTextStyle heading = DocumentTextStyle.DEFAULT.withSize(13).withColor(INK);
        DocumentTextStyle body = DocumentTextStyle.DEFAULT.withSize(10).withColor(MUTED);

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(360, 320)
                .margin(DocumentInsets.of(34))
                .create()) {
            document.metadata(DocumentMetadata.builder().title("Field Guide").build());
            document.chrome().viewerPreferences(DocumentViewerPreferences.builder()
                    .pageMode(DocumentPageMode.USE_OUTLINES)
                    .displayDocTitle(true)
                    .build());

            document.pageFlow(page -> {
                page.addParagraph(p -> p.text("Field Guide")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(18).withColor(INK)));
                page.addParagraph(p -> p.text("opens with the bookmark panel — viewerPreferences(USE_OUTLINES)")
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
