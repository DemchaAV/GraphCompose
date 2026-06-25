package com.demcha.examples.features.chrome;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentPageNumberStyle;
import com.demcha.compose.document.output.DocumentPageNumbering;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for v1.9 page numbering: a footer whose {@code {page}} /
 * {@code {pages}} tokens offset, restart, restyle, and suppress-on-first-page via
 * {@code DocumentHeaderFooter.builder().numbering(...)}. Here a cover page is left
 * uncounted ({@code countFrom = 2}) and the body is numbered in lower-roman, so
 * {@code {pages}} reports the counted total rather than the physical page count.
 *
 * <pre>{@code
 * session.chrome().footer(DocumentHeaderFooter.builder()
 *     .centerText("{page} / {pages}")
 *     .numbering(DocumentPageNumbering.builder()
 *         .style(DocumentPageNumberStyle.LOWER_ROMAN)
 *         .countFrom(2)         // physical page 1 (the cover) is uncounted
 *         .build())
 *     .build());
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class PageNumberingExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(96, 102, 112);

    private PageNumberingExample() {
    }

    /**
     * Renders a 4-page booklet: an uncounted cover, then three lower-roman body
     * pages whose footer reads {@code i / iii}, {@code ii / iii}, {@code iii / iii}.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/chrome", "page-numbering.pdf");

        DocumentTextStyle title = DocumentTextStyle.DEFAULT.withSize(22).withColor(INK);
        DocumentTextStyle body = DocumentTextStyle.DEFAULT.withSize(11).withColor(INK);
        DocumentTextStyle caption = DocumentTextStyle.DEFAULT.withSize(9.5).withColor(MUTED);

        try (DocumentSession session = GraphCompose.document(pdfFile)
                .pageSize(300, 230)
                .margin(DocumentInsets.of(30))
                .create()) {

            session.chrome().footer(DocumentHeaderFooter.builder()
                    .centerText("{page} / {pages}")
                    .numbering(DocumentPageNumbering.builder()
                            .style(DocumentPageNumberStyle.LOWER_ROMAN)
                            .countFrom(2)   // the cover (physical page 1) is uncounted
                            .startAt(1)
                            .build())
                    .build());

            session.pageFlow(page -> {
                // Cover — uncounted, no footer number.
                page.addParagraph(p -> p.text("Field Notes").textStyle(title));
                page.addParagraph(p -> p.text("countFrom(2) leaves this cover uncounted; "
                        + "the body restarts at i and {pages} reports the counted total.")
                        .textStyle(caption).padding(DocumentInsets.top(8)));
                page.addPageBreak(b -> b.name("toBody"));

                page.addParagraph(p -> p.text("Chapter I").textStyle(title));
                page.addParagraph(p -> p.text("Footer reads i / iii.").textStyle(body)
                        .padding(DocumentInsets.top(6)));
                page.addPageBreak(b -> b.name("toII"));

                page.addParagraph(p -> p.text("Chapter II").textStyle(title));
                page.addParagraph(p -> p.text("Footer reads ii / iii.").textStyle(body)
                        .padding(DocumentInsets.top(6)));
                page.addPageBreak(b -> b.name("toIII"));

                page.addParagraph(p -> p.text("Chapter III").textStyle(title));
                page.addParagraph(p -> p.text("Footer reads iii / iii.").textStyle(body)
                        .padding(DocumentInsets.top(6)));
            });

            session.buildPdf();
        }

        return pdfFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
