package com.demcha.examples.features.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for v1.9 {@code RowBuilder.verticalAlign(...)}: cross-axis
 * placement of a row's children within the band set by the tallest child. A
 * short {@code / month} label seated beside a large price moves from the top to
 * the middle to the bottom of the band as the alignment changes — no manual
 * coordinates.
 *
 * <pre>{@code
 * flow.addRow(r -> r.verticalAlign(RowVerticalAlign.BOTTOM)
 *     .addParagraph(bigPrice)     // tallest child -> sets the band height
 *     .addParagraph(smallLabel)); // seated on the band bottom
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class RowVerticalAlignExample {

    private static final DocumentColor INK = DocumentColor.rgb(24, 28, 38);
    private static final DocumentColor MUTED = DocumentColor.rgb(120, 126, 135);
    private static final DocumentColor BAND = DocumentColor.rgb(238, 241, 246);

    private RowVerticalAlignExample() {
    }

    /**
     * Renders the same price row at {@code TOP}, {@code CENTER}, and {@code BOTTOM}
     * vertical alignment.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/layout", "row-vertical-align.pdf");

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(380, 300)
                .margin(DocumentInsets.of(34))
                .create()) {
            document.pageFlow(page -> {
                page.addParagraph(p -> p.text("Row verticalAlign")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(18).withColor(INK)));
                page.addParagraph(p -> p.text("the short label is seated against the band set by the price")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(9).withColor(MUTED))
                        .padding(DocumentInsets.bottom(10)));

                priceRow(page, RowVerticalAlign.TOP);
                priceRow(page, RowVerticalAlign.CENTER);
                priceRow(page, RowVerticalAlign.BOTTOM);
            });

            document.buildPdf();
        }

        return pdfFile;
    }

    private static void priceRow(PageFlowBuilder page, RowVerticalAlign align) {
        page.addRow(r -> r.verticalAlign(align)
                .fillColor(BAND)
                .cornerRadius(8)
                .padding(DocumentInsets.of(12))
                .gap(10)
                .addParagraph(p -> p.text("$49")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(30).withColor(INK)))
                .addParagraph(p -> p.text("/ month  ·  verticalAlign(" + align + ")")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(11).withColor(MUTED))));
        page.addSpacer(s -> s.height(10));
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
