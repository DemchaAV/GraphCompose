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
 * Runnable showcase for v1.9 {@code LineBuilder.fill()}: a line stretches to the
 * width available where it is placed — the content width at flow level, or its
 * slot inside a row — instead of an authored fixed width. Paired with a dotted
 * stroke it is the flex leader behind a table-of-contents row, drawn without
 * measuring the gap by hand.
 *
 * <pre>{@code
 * flow.addLine(l -> l.fill().stroke(s).dashed(0.1, 4).lineCap(ROUND));  // full-width dots
 * flow.addRow(r -> r.weights(4, 1)
 *     .addLine(l -> l.fill().stroke(s).dashed(0.1, 4).lineCap(ROUND))   // leader fills its column
 *     .addParagraph("p. 12"));
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class LineFillExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(90, 96, 105);

    private LineFillExample() {
    }

    /**
     * Renders the fill sheet: a full-width rule, a full-width dotted leader, and
     * a weighted leader-to-number row that previews a table-of-contents line.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/shapes", "line-fill.pdf");

        DocumentTextStyle caption = DocumentTextStyle.DEFAULT.withSize(10).withColor(MUTED);
        DocumentStroke rule = DocumentStroke.of(INK, 1.2);
        DocumentStroke dots = DocumentStroke.of(MUTED, 1.4);

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(400, 260)
                .margin(DocumentInsets.of(36))
                .create()) {
            document.pageFlow(page -> {
                page.addParagraph(p -> p
                        .text("Fill lines & dot leaders")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(18)));

                page.addParagraph(p -> p.text("line().fill() — rule spans the content width")
                        .textStyle(caption).padding(DocumentInsets.top(12)));
                page.addLine(l -> l.fill().height(2).stroke(rule));

                page.addParagraph(p -> p.text("line().fill().dashed(0.1, 4).lineCap(ROUND) — dotted leader")
                        .textStyle(caption).padding(DocumentInsets.top(12)));
                page.addLine(l -> l.fill().height(2).stroke(dots)
                        .dashed(0.1, 4).lineCap(DocumentLineCap.ROUND));

                page.addParagraph(p -> p.text("weighted row: leader fills the gap up to the page number")
                        .textStyle(caption).padding(DocumentInsets.top(12)));
                page.addRow(r -> r.gap(6).weights(5, 1)
                        .addLine(l -> l.fill().height(8).stroke(dots)
                                .dashed(0.1, 4).lineCap(DocumentLineCap.ROUND))
                        .addParagraph(p -> p.text("p. 12").textStyle(caption)));
            });

            document.buildPdf();
        }

        return pdfFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
