package com.demcha.examples.features.layout;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for v1.8 block-level horizontal alignment: a fixed-size
 * node (an SVG icon, a vector path, …) placed LEFT / CENTER / RIGHT within the
 * page content width with one call — the {@code margin: auto} the flow does not
 * give fixed nodes on its own.
 *
 * <pre>{@code
 * flow.addSvgIcon(icon, 48, HorizontalAlign.CENTER);
 * flow.addAligned(HorizontalAlign.RIGHT, anyFixedNode);
 * }</pre>
 *
 * @author Artem Demchyshyn
 */
public final class BlockAlignExample {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor TEAL = DocumentColor.rgb(20, 80, 95);

    /** Inline two-tone badge so the example needs no icon resource. */
    private static final String BADGE_SVG = """
            <svg viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="11" fill="#fde9e3"/>
              <path fill="#c41e3a" d="M12 2 L22 12 L12 22 L2 12 Z"/>
            </svg>
            """;

    private BlockAlignExample() {
    }

    /**
     * Renders the alignment sheet: one icon and one path, each shown
     * left / centre / right aligned.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/layout", "block-align.pdf");

        SvgIcon icon = SvgIcon.parse(BADGE_SVG);
        DocumentTextStyle caption = DocumentTextStyle.DEFAULT.withSize(10).withColor(INK);

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(420, 400)
                .margin(DocumentInsets.of(28))
                .create()) {
            document.pageFlow(page -> {
                page.addParagraph(p -> p
                        .text("Block alignment")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(20)));
                page.addParagraph(p -> p
                        .text("A fixed-size node left-aligns in the flow by default. "
                              + "addSvgIcon(icon, w, align) / addAligned(align, node) seats it "
                              + "left, centre, or right across the content width — no manual maths.")
                        .textStyle(DocumentTextStyle.DEFAULT.withSize(9.5)
                                .withColor(DocumentColor.rgb(90, 96, 105)))
                        .padding(DocumentInsets.bottom(8)));

                for (HorizontalAlign align : HorizontalAlign.values()) {
                    page.addParagraph(p -> p
                            .text("addSvgIcon(icon, 44, HorizontalAlign." + align + ")")
                            .textStyle(caption)
                            .padding(DocumentInsets.top(6)));
                    page.addSvgIcon(icon, 44, align);
                }

                page.addParagraph(p -> p
                        .text("addAligned(CENTER, anyNode) — works for any fixed node, e.g. a path")
                        .textStyle(caption)
                        .padding(DocumentInsets.top(10)));
                page.addAligned(HorizontalAlign.CENTER, chevron());
            });

            document.buildPdf();
        }

        return pdfFile;
    }

    /** A small stroked chevron path to show alignment is not icon-specific. */
    private static com.demcha.compose.document.node.DocumentNode chevron() {
        return new com.demcha.compose.document.dsl.PathBuilder()
                .name("Chevron")
                .size(60, 30)
                .moveTo(0.0, 1.0).lineTo(0.5, 0.0).lineTo(1.0, 1.0)
                .stroke(DocumentStroke.of(TEAL, 4))
                .lineCap(com.demcha.compose.document.style.DocumentLineCap.ROUND)
                .lineJoin(com.demcha.compose.document.style.DocumentLineJoin.ROUND)
                .build();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
