package com.demcha.examples.features.shapes;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the v1.8 vector-path primitive: free-form design
 * shapes with native cubic Bézier curves, authored through the
 * {@code addPath(...)} DSL.
 *
 * <pre>{@code
 * flow.addPath(path -> path
 *         .size(320, 60)
 *         .moveTo(0.0, 0.5)
 *         .curveTo(0.25, 1.0, 0.25, 0.0, 0.5, 0.5)
 *         .curveTo(0.75, 1.0, 0.75, 0.0, 1.0, 0.5)
 *         .stroke(DocumentStroke.of(accent, 2.4)));
 * }</pre>
 *
 * <p>Curves render as native PDF {@code curveTo} operators — perfectly
 * smooth at any zoom, no tessellation. Coordinates are normalized to the
 * shape's box ({@code (0,0)} bottom-left, {@code y} up) and Bézier control
 * points may overshoot it, which is what gives the blob its bulge.</p>
 *
 * @author Artem Demchyshyn
 */
public final class VectorPathExample {

    private static final DocumentColor INK = DocumentColor.rgb(20, 60, 120);
    private static final DocumentColor SAND = DocumentColor.rgb(235, 205, 160);
    private static final DocumentColor SAND_EDGE = DocumentColor.rgb(140, 90, 30);
    private static final DocumentColor MOSS = DocumentColor.rgb(208, 226, 213);
    private static final DocumentColor MOSS_EDGE = DocumentColor.rgb(60, 110, 80);

    private VectorPathExample() {
    }

    /**
     * Renders the three-shape sheet: a stroked Bézier wave, a filled blob
     * with overshooting control points, and a mixed line/curve ribbon.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or file IO fails
     */
    public static Path generate() throws Exception {
        Path pdfFile = ExampleOutputPaths.prepare("features/shapes", "vector-path.pdf");

        try (DocumentSession document = GraphCompose.document(pdfFile)
                .pageSize(420, 420)
                .margin(DocumentInsets.of(28))
                .create()) {
            document.pageFlow(page -> page
                    .addParagraph("Stroked Bézier wave — two cubic spans, zero tessellation")
                    .addPath(path -> path
                            .name("Wave")
                            .size(364, 64)
                            .moveTo(0.0, 0.5)
                            .curveTo(0.25, 1.1, 0.25, -0.1, 0.5, 0.5)
                            .curveTo(0.75, 1.1, 0.75, -0.1, 1.0, 0.5)
                            .stroke(DocumentStroke.of(INK, 2.4))
                            .margin(DocumentInsets.bottom(16)))
                    .addParagraph("Filled blob — closed curves, control points overshoot the box")
                    .addPath(path -> path
                            .name("Blob")
                            .size(120, 104)
                            .moveTo(0.5, 1.0)
                            .curveTo(1.12, 0.94, 0.96, 0.08, 0.5, 0.0)
                            .curveTo(0.04, 0.08, -0.12, 0.94, 0.5, 1.0)
                            .closePath()
                            .fillColor(SAND)
                            .stroke(DocumentStroke.of(SAND_EDGE, 1.4))
                            .margin(DocumentInsets.bottom(16)))
                    .addParagraph("Mixed ribbon — lines and curves in one closed, filled subpath")
                    .addPath(path -> path
                            .name("Ribbon")
                            .size(364, 70)
                            .moveTo(0.0, 0.85)
                            .lineTo(0.18, 0.85)
                            .curveTo(0.42, 1.05, 0.58, 0.55, 0.82, 0.75)
                            .lineTo(1.0, 0.75)
                            .lineTo(1.0, 0.15)
                            .curveTo(0.6, -0.05, 0.4, 0.45, 0.0, 0.25)
                            .closePath()
                            .fillColor(MOSS)
                            .stroke(DocumentStroke.of(MOSS_EDGE, 1.2))));

            document.buildPdf();
        }

        return pdfFile;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
