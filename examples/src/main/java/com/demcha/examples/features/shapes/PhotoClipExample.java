package com.demcha.examples.features.shapes;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgPath;
import com.demcha.examples.support.ExampleOutputPaths;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Clip one rectangular photo to a free-form silhouette — the v1.8
 * {@code ShapeOutline.Path} clip. Three "cookie-cutters" (a circle, an SVG
 * heart, a star) cut the same photo into three shapes.
 *
 * <p>The construction logic is always the same three moves:</p>
 * <ol>
 *   <li><b>outline</b> — the shape that does the cutting (mandatory);</li>
 *   <li><b>clipPolicy(CLIP_PATH)</b> — "cut children to the silhouette"
 *       (already the container default, written here for clarity);</li>
 *   <li><b>a layer</b> — the photo, sized to the <em>same</em> box and
 *       {@code COVER}-filled so it reaches every edge for the clip to bite.</li>
 * </ol>
 *
 * @author Artem Demchyshyn
 */
public final class PhotoClipExample {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor GOLD = DocumentColor.rgb(196, 153, 76);

    /** Material Icons "favorite" heart (Apache 2.0), viewBox 0 0 24 24. */
    private static final String HEART_D =
            "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3"
            + "c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5"
            + "c0 3.78-3.4 6.86-8.55 11.54L12 21.35z";

    private static final double BOX = 150;

    private PhotoClipExample() {
    }

    /**
     * Renders the sheet: one photo, three silhouettes.
     *
     * @return path to the generated PDF
     * @throws Exception if rendering or resource IO fails
     */
    public static Path generate() throws Exception {
        Path pdf = ExampleOutputPaths.prepare("features/shapes", "photo-clip.pdf");
        DocumentImageData photo = photo();

        try (DocumentSession document = GraphCompose.document(pdf)
                .pageSize(560, 300)
                .margin(DocumentInsets.of(28))
                .create()) {
            document.pageFlow(page -> page
                    .addParagraph(p -> p
                            .text("Photo clipped to a silhouette")
                            .textStyle(DocumentTextStyle.DEFAULT.withSize(18).withColor(INK)))
                    .addParagraph(p -> p
                            .text("One rectangular photo, three cookie-cutters. The image fills "
                                  + "each box (COVER); the outline cuts it to shape, native curves "
                                  + "stay crisp at any zoom.")
                            .textStyle(DocumentTextStyle.DEFAULT.withSize(9.5)
                                    .withColor(DocumentColor.rgb(90, 96, 105)))
                            .padding(DocumentInsets.bottom(12)))
                    .addRow(row -> row.spacing(20).evenWeights()
                            .addSection(s -> s.spacing(6)
                                    .add(circleClip(photo))
                                    .addParagraph("circle(150)"))
                            .addSection(s -> s.spacing(6)
                                    .add(heartClip(photo))
                                    .addParagraph("path(150, 150, SvgPath heart)"))
                            .addSection(s -> s.spacing(6)
                                    .add(starClip(photo))
                                    .addParagraph("star(150, 150)"))));
            document.buildPdf();
        }
        return pdf;
    }

    /** Circle cutter — an ellipse clip, available before v1.8. */
    private static DocumentNode circleClip(DocumentImageData photo) {
        return new ShapeContainerBuilder()
                .name("CirclePhoto")
                .circle(BOX)                          // 1. outline = the cutter (mandatory)
                .clipPolicy(ClipPolicy.CLIP_PATH)     // 2. cut children to the silhouette
                .stroke(DocumentStroke.of(GOLD, 2))   //    (optional) gold rim along the cut
                .center(cover(photo))                 // 3. the photo, COVER-filling the box
                .build();
    }

    /** Heart cutter — the v1.8 free-form path clip (native Béziers). */
    private static DocumentNode heartClip(DocumentImageData photo) {
        return new ShapeContainerBuilder()
                .name("HeartPhoto")
                .path(BOX, BOX, SvgPath.parse(HEART_D, 0, 0, 24, 24))
                .clipPolicy(ClipPolicy.CLIP_PATH)
                .stroke(DocumentStroke.of(GOLD, 2))
                .center(cover(photo))
                .build();
    }

    /** Five-point star cutter. */
    private static DocumentNode starClip(DocumentImageData photo) {
        return new ShapeContainerBuilder()
                .name("StarPhoto")
                .star(BOX, BOX)
                .clipPolicy(ClipPolicy.CLIP_PATH)
                .stroke(DocumentStroke.of(GOLD, 2))
                .center(cover(photo))
                .build();
    }

    /**
     * The photo sized to fill the WHOLE box with {@code COVER}, so it reaches
     * every edge of the silhouette and the clip has something to cut. A smaller
     * size or {@code CONTAIN} would leave gaps inside the shape; {@code STRETCH}
     * would distort the photo.
     */
    private static DocumentNode cover(DocumentImageData photo) {
        return new ImageBuilder()
                .source(photo)
                .size(BOX, BOX)
                .fitMode(DocumentImageFitMode.COVER)
                .build();
    }

    private static DocumentImageData photo() throws Exception {
        try (InputStream in = Objects.requireNonNull(
                PhotoClipExample.class.getResourceAsStream("/engine-hero.jpg"),
                "engine-hero.jpg missing from examples/src/main/resources/")) {
            return DocumentImageData.fromBytes(in.readAllBytes());
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generated: " + generate());
    }
}
