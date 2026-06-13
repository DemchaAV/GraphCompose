package com.demcha.compose.document.architecture;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPathSegment;
import com.demcha.compose.document.style.ShapeOutline;
import com.demcha.compose.document.style.ShapePoint;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exhaustiveness guard: every {@link ShapeOutline} permit must render through
 * <em>both</em> outline-consuming surfaces — the shape-container clip path and
 * the inline-shape run — without throwing. Each surface dispatches on the
 * outline kind with an {@code instanceof} chain that ends in an
 * {@code IllegalStateException}; this test reflects over
 * {@code getPermittedSubclasses()} so the next permit added to the sealed type
 * cannot silently miss a render branch (the lesson from the
 * {@code ShapeOutline.Path} clipper, where the inline-shape switch was missed).
 */
class ShapeOutlineRenderCoverageTest {

    /** One representative instance per permitted ShapeOutline kind. */
    private static final Map<Class<?>, ShapeOutline> REPRESENTATIVES = Map.of(
            ShapeOutline.Rectangle.class, new ShapeOutline.Rectangle(40, 24),
            ShapeOutline.RoundedRectangle.class, new ShapeOutline.RoundedRectangle(40, 24, 6),
            ShapeOutline.RoundedRectanglePerCorner.class,
            new ShapeOutline.RoundedRectanglePerCorner(40, 24, DocumentCornerRadius.right(6)),
            ShapeOutline.Ellipse.class, new ShapeOutline.Ellipse(40, 24),
            ShapeOutline.Polygon.class, ShapeOutline.diamond(40, 24),
            ShapeOutline.Path.class, ShapeOutline.path(40, 24, List.of(
                    DocumentPathSegment.moveTo(0.5, 1.0),
                    DocumentPathSegment.lineTo(1.0, 0.0),
                    DocumentPathSegment.lineTo(0.0, 0.0),
                    DocumentPathSegment.close())));

    @Test
    void everyPermittedOutlineHasARepresentative() {
        Set<Class<?>> permits = Set.of(ShapeOutline.class.getPermittedSubclasses());
        Set<Class<?>> covered = REPRESENTATIVES.keySet();
        // If this fails, a new ShapeOutline permit was added — give it a
        // representative here AND a render branch in BOTH surfaces below.
        assertThat(covered).containsExactlyInAnyOrderElementsOf(permits);
    }

    @Test
    void everyOutlineClipsInAContainerWithoutThrowing() throws Exception {
        for (ShapeOutline outline : REPRESENTATIVES.values()) {
            try (DocumentSession session = GraphCompose.document()
                    .pageSize(200, 160)
                    .margin(DocumentInsets.of(16))
                    .create()) {
                session.add(new ShapeContainerBuilder()
                        .name("Clip" + outline.getClass().getSimpleName())
                        .outline(outline)
                        .clipPolicy(ClipPolicy.CLIP_PATH)
                        .fillColor(DocumentColor.rgb(20, 80, 95))
                        .center(spacer())
                        .build());
                byte[] pdf = session.toPdfBytes();
                assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII))
                        .as("clip render of " + outline.getClass().getSimpleName())
                        .isEqualTo("%PDF-");
            }
        }
    }

    @Test
    void everyOutlineRendersAsAnInlineShapeWithoutThrowing() throws Exception {
        for (ShapeOutline outline : REPRESENTATIVES.values()) {
            try (DocumentSession session = GraphCompose.document()
                    .pageSize(200, 160)
                    .margin(DocumentInsets.of(16))
                    .create()) {
                session.dsl().pageFlow().name("Flow")
                        .addParagraph(p -> p
                                .text("inline ")
                                .shape(outline, DocumentColor.rgb(196, 30, 58)))
                        .build();
                byte[] pdf = session.toPdfBytes();
                assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII))
                        .as("inline render of " + outline.getClass().getSimpleName())
                        .isEqualTo("%PDF-");
            }
        }
    }

    private static com.demcha.compose.document.node.DocumentNode spacer() {
        return new com.demcha.compose.document.dsl.SpacerBuilder().size(12, 12).build();
    }
}
