package com.demcha.examples.features.structure;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.api.MultiSectionDocument;
import com.demcha.compose.document.backend.fixed.pdf.options.PdfHeaderFooterOptions;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for v1.9 {@code GraphCompose.documents()}: several sections,
 * each a full {@link DocumentSession} with its own page size and chrome,
 * concatenated into one PDF — no external merge. Here a full-bleed landscape
 * cover precedes a portrait, page-numbered body, and the cover's call to action
 * links straight to a chapter in the body.
 *
 * <pre>{@code
 * DocumentSession cover = GraphCompose.document().pageSize(440, 300).margin(of(0)).create();
 * DocumentSession body  = GraphCompose.document().pageSize(300, 440).margin(of(40)).create();
 * body.footer(PdfHeaderFooterOptions.builder().centerText("{page}").build());
 *
 * try (MultiSectionDocument doc = GraphCompose.documents(out).section(cover).section(body).create()) {
 *     doc.buildPdf();
 * }
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class MultiSectionExample {

    private static final DocumentColor COVER_BG = DocumentColor.rgb(28, 39, 64);
    private static final DocumentColor COVER_INK = DocumentColor.rgb(244, 247, 252);
    private static final DocumentColor COVER_ACCENT = DocumentColor.rgb(126, 170, 255);
    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 126, 135);

    private static final String[][] CHAPTERS = {
            {"1. The opening chapter", "ch1"},
            {"2. The middle chapter", "ch2"},
            {"3. The closing chapter", "ch3"},
    };

    private MultiSectionExample() {
    }

    /**
     * Renders a landscape cover and a portrait body into a single PDF via
     * {@code GraphCompose.documents()}.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/structure", "multi-section-document.pdf");

        // Cover: landscape, full-bleed background, no margin.
        DocumentSession cover = GraphCompose.document()
                .pageSize(440, 300)
                .margin(DocumentInsets.of(0))
                .pageBackground(COVER_BG)
                .create();
        cover.pageFlow(page -> page.addSection(s -> s.padding(DocumentInsets.of(46))
                .addParagraph(p -> p.text("The GraphCompose Book")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(30).withColor(COVER_INK)))
                .addParagraph(p -> p.text("One document, two page geometries")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(13).withColor(COVER_INK)))
                .addParagraph(p -> p.text("Open the opening chapter")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(13).withColor(COVER_ACCENT))
                        .linkTo("ch1"))));

        // Body: portrait, margined, page-numbered from its own first page.
        DocumentSession body = GraphCompose.document()
                .pageSize(300, 440)
                .margin(DocumentInsets.of(40))
                .create();
        body.footer(PdfHeaderFooterOptions.builder().centerText("{page} / {pages}").build());
        body.pageFlow(page -> {
            for (int i = 0; i < CHAPTERS.length; i++) {
                String[] chapter = CHAPTERS[i];
                if (i > 0) {
                    page.addPageBreak(b -> b.name("to_" + chapter[1]));
                }
                page.addSection(s -> s.anchor(chapter[1])
                        .addParagraph(p -> p.text(chapter[0])
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(18).withColor(INK)))
                        .addParagraph(p -> p.text("Body pages carry their own margins and footer page numbers, "
                                        + "independent of the borderless cover in front of them.")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(11).withColor(MUTED))));
            }
        });

        try (MultiSectionDocument document = GraphCompose.documents(pdfFile)
                .section(cover)
                .section(body)
                .create()) {
            document.buildPdf();
        }

        return pdfFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
