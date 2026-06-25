package com.demcha.examples.features.shapes;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for v1.9 line caps: {@code LineBuilder.lineCap(...)} brings
 * the round / square end-caps {@code PathBuilder} already had to plain lines.
 * The headline use is a dotted line — a {@code ROUND} cap on a near-zero dash
 * draws round dots, the classic table-of-contents leader / separator.
 *
 * <pre>{@code
 * flow.addLine(l -> l.horizontal(w).stroke(stroke)
 *     .dashed(0.1, 4).lineCap(DocumentLineCap.ROUND));   // round dots
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class LineCapExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(90, 96, 105);
    private static final double RULE = 320;

    private LineCapExample() {
    }

    /**
     * Renders the line-cap sheet: solid, dashed, and dotted rules, plus a
     * thick round-capped vs butt-capped comparison.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/shapes", "line-cap.pdf");

        DocumentTextStyle caption = DocumentTextStyle.DEFAULT.withSize(10).withColor(MUTED);
        DocumentStroke thin = DocumentStroke.of(INK, 1.4);
        DocumentStroke thick = DocumentStroke.of(INK, 8);

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(400, 320)
                .margin(DocumentInsets.of(36))
                .create()) {
            document.pageFlow(page -> {
                page.addParagraph(p -> p
                        .text("Line caps & dotted lines")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(18)));

                page.addParagraph(p -> p.text("solid (default BUTT)").textStyle(caption)
                        .padding(DocumentInsets.top(10)));
                page.addLine(l -> l.horizontal(RULE).stroke(thin));

                page.addParagraph(p -> p.text("dashed(4, 3)").textStyle(caption)
                        .padding(DocumentInsets.top(8)));
                page.addLine(l -> l.horizontal(RULE).stroke(thin).dashed(4, 3));

                page.addParagraph(p -> p.text("dashed(0.1, 4).lineCap(ROUND) — dotted leader")
                        .textStyle(caption).padding(DocumentInsets.top(8)));
                page.addLine(l -> l.horizontal(RULE).stroke(thin)
                        .dashed(0.1, 4).lineCap(DocumentLineCap.ROUND));

                page.addParagraph(p -> p.text("thick stroke — BUTT vs lineCap(ROUND)")
                        .textStyle(caption).padding(DocumentInsets.top(12)));
                page.addLine(l -> l.horizontal(120).stroke(thick));
                page.addLine(l -> l.horizontal(120).stroke(thick)
                        .lineCap(DocumentLineCap.ROUND).padding(DocumentInsets.top(6)));
            });

            document.buildPdf();
        }

        return pdfFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
