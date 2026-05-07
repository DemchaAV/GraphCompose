package com.demcha.examples;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import com.demcha.examples.support.ExampleOutputPaths;

import java.nio.file.Path;

/**
 * Runnable showcase for the v1.6 Phase A nested-list ergonomics:
 * {@code ListBuilder.addItem(label, Consumer)}, {@code markerFor(depth)}
 * overrides, mixed flat / nested authoring, and the built-in marker
 * cascade ({@code •} → {@code ◦} → {@code ▪} → {@code ·}). Each section
 * of the generated PDF demonstrates one piece of the new surface.
 *
 * @author Artem Demchyshyn
 */
public final class NestedListExample {

    private static final DocumentColor INK = DocumentColor.rgb(34, 38, 50);
    private static final DocumentColor MUTED = DocumentColor.rgb(102, 106, 118);
    private static final DocumentColor RULE = DocumentColor.rgb(180, 188, 200);

    private NestedListExample() {
    }

    public static Path generate() throws Exception {
        Path outputFile = ExampleOutputPaths.prepare("nested-list-showcase.pdf");

        DocumentTextStyle title = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(20)
                .color(INK)
                .decoration(DocumentTextDecoration.BOLD)
                .build();
        DocumentTextStyle sectionHeading = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_BOLD)
                .size(13)
                .color(INK)
                .decoration(DocumentTextDecoration.BOLD)
                .build();
        DocumentTextStyle caption = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA_OBLIQUE)
                .size(10)
                .color(MUTED)
                .build();
        DocumentTextStyle body = DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(11)
                .color(INK)
                .build();

        try (DocumentSession document = GraphCompose.document(outputFile)
                .pageSize(DocumentPageSize.A4)
                .margin(36, 36, 36, 36)
                .create()) {

            document.pageFlow()
                    .name("NestedListShowcase")
                    .spacing(8)
                    .addParagraph("v1.6 Phase A — Nested list ergonomics", title)
                    .addParagraph(
                            "ListBuilder.addItem(label, body) appends a list item with a builder "
                                    + "callback that scopes children. Per-depth markers, source-order "
                                    + "preservation across mixed flat / nested entries, and a built-in "
                                    + "marker cascade ship out of the box.", caption)

                    // 1) Three-level nesting with the default marker cascade.
                    .addParagraph("1. Default depth cascade (3 levels)", sectionHeading)
                    .addList(list -> list
                            .name("DefaultCascade")
                            .textStyle(body)
                            .itemSpacing(2)
                            .markerFor(1, ListMarker.dash())
                            .markerFor(2, ListMarker.custom("*"))
                            .addItem("Engineering Roadmap", q1 -> q1
                                    .addItem("Document Engine", phaseA -> phaseA
                                            .addItem("Nested lists landed in v1.6")
                                            .addItem("Composed table cells landed in v1.6")
                                            .addItem("Templates v2 with visual parity gate"))
                                    .addItem("Backend SPI", phaseB -> phaseB
                                            .addItem("PdfFragmentRenderHandler is now public")
                                            .addItem("DOCX semantic backend skeleton")))
                            .addItem("Documentation",
                                    docs -> docs
                                            .addItem("Migration guide v1.5 to v1.6")
                                            .addItem("ADRs 0011-0013 published")))

                    // 2) markerFor() per-depth override + per-item marker.
                    .addParagraph("2. markerFor(depth) overrides + per-item marker", sectionHeading)
                    .addParagraph(
                            "markerFor(0) wins over the cascade default at depth 0, "
                                    + "markerFor(1) wins at depth 1, and a per-item marker "
                                    + "wins over both.", caption)
                    .addList(list -> list
                            .name("PerDepthMarkers")
                            .textStyle(body)
                            .itemSpacing(2)
                            .markerFor(0, ListMarker.dash())
                            .markerFor(1, ListMarker.custom(">"))
                            .addItem("Templates v2 (Phase A landed)", a -> a
                                    .addItem("CV presets — 14 with V1 visual parity")
                                    .addItem("Cover-letter pair presets — 14 paired"))
                            .addItem("Layout primitives", a -> a
                                    .addItem("Nested lists (this example)")
                                    .addItem("Composed table cells (Phase B)")))

                    // 3) Mixed flat + nested authoring with source-order preserved.
                    .addParagraph("3. Mixed flat and nested authoring (source order preserved)", sectionHeading)
                    .addParagraph(
                            "Flat addItem(String) and addItem(label, body) calls interleave; "
                                    + "the resulting list keeps depth-0 entries in the order they "
                                    + "were added.", caption)
                    .addList(list -> list
                            .name("MixedAuthoring")
                            .textStyle(body)
                            .itemSpacing(2)
                            .markerFor(1, ListMarker.dash())
                            .addItem("Reviewed PR: nested list ergonomics")
                            .addItem("Reviewed PR with nested children", review -> review
                                    .addItem("Followed up on inline-run wrap")
                                    .addItem("Ran mvnw verify locally"))
                            .addItem("Closed bug: marker double-space rendering")
                            .addItem("Triaged backlog", triage -> triage
                                    .addItem("Phase E.4 deferred to v1.7")
                                    .addItem("CanvasLayerNode parked")))

                    // 4) Deep nesting (depth 4+) falls back to the · cascade.
                    .addParagraph("4. Depth 4+ falls back to mid-dot", sectionHeading)
                    .addList(list -> list
                            .name("DeepNesting")
                            .textStyle(body)
                            .itemSpacing(2)
                            .markerFor(1, ListMarker.dash())
                            .markerFor(2, ListMarker.custom("*"))
                            .markerFor(3, ListMarker.custom(">"))
                            .markerFor(4, ListMarker.custom("+"))
                            .addItem("Plan", l0 -> l0
                                    .addItem("Phase", l1 -> l1
                                            .addItem("Subphase", l2 -> l2
                                                    .addItem("Step", l3 -> l3
                                                            .addItem("Sub-step (depth 4)")
                                                            .addItem("Another sub-step at depth 4"))))))
                    .build();
            document.buildPdf();
        }
        return outputFile;
    }

    public static void main(String[] args) throws Exception {
        Path output = generate();
        System.out.println("Generated: " + output);
    }
}
