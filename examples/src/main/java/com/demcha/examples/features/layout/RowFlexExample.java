package com.demcha.examples.features.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.RowArrangement;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for v1.9 row flex layout: {@code pushRight()} / {@code
 * flexSpacer()} springs absorb the row's leftover width, and {@code
 * arrangement(...)} justifies content-sized children — the {@code justify-content}
 * analogue for a horizontal row, no manual coordinates.
 *
 * <pre>{@code
 * flow.addRow(r -> r.addParagraph(title).pushRight().addParagraph(status)); // title left, status right
 * flow.addRow(r -> r.arrangement(RowArrangement.SPACE_BETWEEN)
 *     .addParagraph(a).addParagraph(b).addParagraph(c));                    // spread across the row
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class RowFlexExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 126, 135);
    private static final DocumentColor GREEN = DocumentColor.rgb(22, 128, 70);
    private static final DocumentColor BAND = DocumentColor.rgb(238, 241, 246);

    private RowFlexExample() {
    }

    /**
     * Renders a {@code pushRight()} header, a {@code SPACE_BETWEEN} nav bar, and a
     * {@code CENTER} footer.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/layout", "row-flex.pdf");

        DocumentTextStyle label = DocumentTextStyle.DEFAULT.withSize(12).withColor(INK);

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(400, 280)
                .margin(DocumentInsets.of(34))
                .create()) {
            document.pageFlow(page -> {
                heading(page, "Row flex & arrangement");

                // pushRight(): title on the left, status flush to the right edge.
                band(page, r -> r
                        .addParagraph(p -> p.text("Invoice #1042").textStyle(label))
                        .pushRight()
                        .addParagraph(p -> p.text("PAID")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(12).withColor(GREEN))));

                // SPACE_BETWEEN: a nav bar spread edge-to-edge.
                band(page, r -> r.arrangement(RowArrangement.SPACE_BETWEEN)
                        .addParagraph(p -> p.text("Overview").textStyle(label))
                        .addParagraph(p -> p.text("Details").textStyle(label))
                        .addParagraph(p -> p.text("History").textStyle(label))
                        .addParagraph(p -> p.text("Settings").textStyle(label)));

                // CENTER: a centred footer note.
                band(page, r -> r.arrangement(RowArrangement.CENTER)
                        .addParagraph(p -> p.text("· thank you for your business ·")
                                .textStyle(DocumentTextStyle.DEFAULT.withSize(11).withColor(MUTED))));
            });

            document.buildPdf();
        }

        return pdfFile;
    }

    private static void heading(PageFlowBuilder page, String text) {
        page.addParagraph(p -> p.text(text).textStyle(DocumentTextStyle.DEFAULT.withSize(18).withColor(INK)));
        page.addParagraph(p -> p.text("pushRight() / flexSpacer() and arrangement(...) on a row")
                .textStyle(DocumentTextStyle.DEFAULT.withSize(9).withColor(MUTED))
                .padding(DocumentInsets.bottom(10)));
    }

    private static void band(PageFlowBuilder page, java.util.function.Consumer<com.demcha.compose.document.dsl.RowBuilder> spec) {
        page.addRow(r -> {
            r.fillColor(BAND).cornerRadius(8).padding(DocumentInsets.of(12));
            spec.accept(r);
        });
        page.addSpacer(s -> s.height(10));
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
