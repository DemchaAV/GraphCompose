package com.demcha.examples.features.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentBleed;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentEdge;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for v1.9 content bleed: a section's background fill extends
 * to the trimmed physical page edge on the declared sides, while its children
 * stay inside the content margin — a full-bleed masthead band whose title never
 * runs off the page. The intent-revealing replacement for the hand-computed
 * negative-margin idiom.
 *
 * <pre>{@code
 * page.addSection(band -> band
 *     .fillColor(ink)
 *     .bleedToEdge(DocumentEdge.TOP, DocumentEdge.LEFT, DocumentEdge.RIGHT)
 *     .addParagraph("Title"));            // title stays in the safe area
 *
 * page.addSection(rule -> rule
 *     .fillColor(accent)
 *     .bleed(DocumentBleed.of(DocumentEdge.LEFT, DocumentEdge.RIGHT)));
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class BleedExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor ACCENT = DocumentColor.rgb(196, 30, 58);
    private static final DocumentColor MUTED = DocumentColor.rgb(90, 96, 105);

    private BleedExample() {
    }

    /**
     * Renders the bleed sheet: a full-bleed masthead, a body paragraph, and a
     * side-to-side accent band.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/layout", "content-bleed.pdf");

        DocumentTextStyle caption = DocumentTextStyle.DEFAULT.withSize(10).withColor(MUTED);

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(420, 480)
                .margin(DocumentInsets.of(36))
                .create()) {
            document.pageFlow(page -> {
                // Full-bleed masthead: fill reaches the top + both side edges,
                // the heading text stays seated in the content margin.
                page.addSection(band -> band
                        .fillColor(INK)
                        .padding(DocumentInsets.of(16))
                        .bleedToEdge(DocumentEdge.TOP, DocumentEdge.LEFT, DocumentEdge.RIGHT)
                        .addParagraph(p -> p
                                .text("Content bleed")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(22)
                                        .withColor(DocumentColor.rgb(255, 255, 255)))));

                page.addParagraph(p -> p
                        .text("band.bleedToEdge(TOP, LEFT, RIGHT) — the fill runs to the page "
                              + "edge while the title stays inside the content margin, so text "
                              + "never clips. The intent-revealing replacement for a hand-tuned "
                              + "negative margin.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(9.5).withColor(MUTED))
                        .padding(new DocumentInsets(12, 0, 6, 0)));

                page.addParagraph(p -> p
                        .text("rule.bleed(DocumentBleed.of(LEFT, RIGHT))")
                        .textStyle(caption)
                        .padding(DocumentInsets.top(14)));

                // Edge-to-edge accent band: horizontal bleed only.
                page.addSection(rule -> rule
                        .fillColor(ACCENT)
                        .padding(new DocumentInsets(5, 0, 5, 0))
                        .bleed(DocumentBleed.of(DocumentEdge.LEFT, DocumentEdge.RIGHT))
                        .addParagraph(p -> p
                                .text("edge to edge")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(9)
                                        .withColor(DocumentColor.rgb(255, 255, 255)))));

                page.addParagraph(p -> p
                        .text("Without bleed, the same band would stop at the content margin "
                              + "on every side.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(9.5).withColor(MUTED))
                        .padding(DocumentInsets.top(12)));
            });

            document.buildPdf();
        }

        return pdfFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
