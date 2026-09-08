package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.PlacedFragment;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.layout.payloads.ShapeFragmentPayload;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentEdge;
import com.demcha.compose.document.style.DocumentFlowWidth;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import com.demcha.compose.testing.layout.LayoutSnapshotJson;
import com.demcha.testing.VisualTestOutputs;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Geometry for {@code fixedWidth(...)} on a vertical flow: the box takes the width
 * it asked for, the height stays whatever the content measures to, and a flow that
 * never asks keeps the layout it always had.
 *
 * <p>The page is 400&times;300 with a 20pt margin, so every scenario runs against a
 * 360pt content column — wide enough that a 240pt request is honoured outright and
 * a 900pt one has something to be clamped to.</p>
 */
class FlowFixedWidthLayoutTest {

    private static final double PAGE_WIDTH = 400;
    private static final double PAGE_HEIGHT = 300;
    private static final double INNER_WIDTH = 360;
    private static final String BODY =
            "A fixed width pins the horizontal axis and leaves the height to the content.";

    // --- the width is the width that was asked for -------------------------

    @Test
    void aFixedWidthSectionIsPlacedAtTheRequestedWidth() {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page.addSection(s -> s
                    .name("Card")
                    .fixedWidth(240)
                    .addParagraph(BODY)));

            assertThat(node(document, "Card").placementWidth()).isCloseTo(240.0, within(0.01));
        }
    }

    @Test
    void aFixedWidthSectionWithOneParagraphMatchesItsLayoutSnapshot() throws Exception {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page
                    .name("FixedWidthFlow")
                    .addSection(s -> s.name("Card").fixedWidth(240).addParagraph(BODY)));

            LayoutSnapshotAssertions.assertMatches(document, "flow-fixed-width/section_fixed_width");
        }
    }

    @Test
    void anUnconstrainedSectionKeepsItsShrinkToFitWidth() {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page.addSection(s -> s
                    .name("Plain")
                    .addParagraph(p -> p.name("Body").text(BODY))));

            PlacedNode plain = node(document, "Plain");
            PlacedNode body = node(document, "Body");

            // The pre-existing measurement, untouched: an unpadded section reports the
            // width of its widest child, capped at the column — it hugs its content
            // rather than filling the column.
            assertThat(plain.placementWidth()).isCloseTo(body.placementWidth(), within(0.01));
            assertThat(plain.placementWidth()).isLessThanOrEqualTo(INNER_WIDTH);
        }
    }

    @Test
    void aFixedWidthSectionPinsItsWidthWhereAnUnconstrainedOneHugsItsContent() {
        PlacedNode fixed = cardWith(s -> s.fixedWidth(240).addParagraph(BODY));
        PlacedNode hugging = cardWith(s -> s.addParagraph(BODY));

        // The whole point of the feature: the fixed box is the width that was asked
        // for even though its content does not fill it.
        assertThat(fixed.placementWidth()).isCloseTo(240.0, within(0.01));
        assertThat(hugging.placementWidth()).isNotCloseTo(240.0, within(0.01));
    }

    // --- height stays content-driven ---------------------------------------

    @Test
    void growingTheContentAddsHeightAndLeavesTheWidthAlone() {
        PlacedNode one = cardWith(s -> s.fixedWidth(240).addParagraph(BODY));
        PlacedNode three = cardWith(s -> s.fixedWidth(240)
                .addParagraph(BODY)
                .addParagraph(BODY)
                .addParagraph(BODY));

        assertThat(three.placementWidth()).isCloseTo(one.placementWidth(), within(0.01));
        assertThat(three.placementHeight()).isGreaterThan(one.placementHeight());
    }

    @Test
    void theFixedWidthNeverIntroducesAFixedHeight() {
        PlacedNode shortCard = cardWith(s -> s.fixedWidth(240).addParagraph("One line."));
        PlacedNode tallCard = cardWith(s -> s.fixedWidth(240).addParagraph(BODY + " " + BODY));

        assertThat(tallCard.placementHeight()).isGreaterThan(shortCard.placementHeight());
    }

    // --- narrowing re-wraps -------------------------------------------------

    @Test
    void narrowingTheFixedWidthReWrapsTheContentIntoMoreLines() {
        PlacedNode wide = cardWith(s -> s.fixedWidth(300).addParagraph(BODY));
        PlacedNode narrow = cardWith(s -> s.fixedWidth(120).addParagraph(BODY));

        assertThat(narrow.placementWidth()).isCloseTo(120.0, within(0.01));
        assertThat(wide.placementWidth()).isCloseTo(300.0, within(0.01));
        // Same words in a third of the width: the only way for them to fit is more
        // lines, which is height the box did not have before.
        assertThat(narrow.placementHeight()).isGreaterThan(wide.placementHeight());
    }

    @Test
    void theChildIsMeasuredInsideTheFixedWidthNotTheParentColumn() {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page.addSection(s -> s
                    .name("Card")
                    .fixedWidth(160)
                    .addSection(inner -> inner.name("Body").addParagraph(BODY))));

            assertThat(node(document, "Body").placementWidth()).isLessThanOrEqualTo(160.0);
        }
    }

    // --- padding sits inside the fixed outer width --------------------------

    @Test
    void paddingStaysInsideTheFixedOuterWidth() {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page.addSection(s -> s
                    .name("Card")
                    .fixedWidth(240)
                    .padding(DocumentInsets.of(20))
                    .addSection(inner -> inner.name("Body").addParagraph(BODY))));

            PlacedNode card = node(document, "Card");
            PlacedNode body = node(document, "Body");

            // The fixed width is the OUTER width: padding eats into it rather than
            // being added on top of it.
            assertThat(card.placementWidth()).isCloseTo(240.0, within(0.01));
            assertThat(body.placementX()).isCloseTo(card.placementX() + 20, within(0.01));
            assertThat(body.placementWidth()).isLessThanOrEqualTo(200.0 + 0.01);
            assertThat(body.placementX() + body.placementWidth())
                    .isLessThanOrEqualTo(card.placementX() + 240.0 + 0.01);
        }
    }

    // --- clamping -----------------------------------------------------------

    @Test
    void aWidthWiderThanTheParentIsClampedToTheParent() {
        PlacedNode clamped = cardWith(s -> s.fixedWidth(900).addParagraph(BODY));

        assertThat(clamped.placementWidth()).isCloseTo(INNER_WIDTH, within(0.01));
    }

    @Test
    void aClampedWidthWrapsItsContentTheSameWayAnUnconstrainedFlowDoes() {
        PlacedNode clamped = cardWith(s -> s.fixedWidth(900).addParagraph(BODY));
        PlacedNode unconstrained = cardWith(s -> s.addParagraph(BODY));

        // Both measure their children inside the same 360pt column, so the content
        // wraps identically and the box is the same height — clamping changes the
        // reported width, never the line breaking underneath it.
        assertThat(clamped.placementHeight()).isCloseTo(unconstrained.placementHeight(), within(0.01));
        assertThat(clamped.placementY()).isCloseTo(unconstrained.placementY(), within(0.01));
        assertThat(clamped.placementWidth()).isCloseTo(INNER_WIDTH, within(0.01));
    }

    @Test
    void aClampedWidthNeverOverflowsTheColumn() {
        PlacedNode clamped = cardWith(s -> s.fixedWidth(900).addParagraph(BODY));

        assertThat(clamped.placementX() + clamped.placementWidth())
                .isLessThanOrEqualTo(20 + INNER_WIDTH + 0.01);
    }

    // --- nesting ------------------------------------------------------------

    @Test
    void nestedFixedWidthFlowsEachHonourTheirOwnWidth() {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page.addSection(outer -> outer
                    .name("Outer")
                    .fixedWidth(300)
                    .addSection(inner -> inner.name("Inner").fixedWidth(160).addParagraph(BODY))));

            assertThat(node(document, "Outer").placementWidth()).isCloseTo(300.0, within(0.01));
            assertThat(node(document, "Inner").placementWidth()).isCloseTo(160.0, within(0.01));
        }
    }

    @Test
    void aNestedFlowIsClampedByItsFixedWidthParentNotByThePage() {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page.addSection(outer -> outer
                    .name("Outer")
                    .fixedWidth(200)
                    .padding(DocumentInsets.of(10))
                    .addSection(inner -> inner.name("Inner").fixedWidth(320).addParagraph(BODY))));

            // The inner box asked for more than the outer box's 180pt inner width and
            // is clamped to it, not to the page's 360pt column.
            assertThat(node(document, "Inner").placementWidth()).isCloseTo(180.0, within(0.01));
        }
    }

    @Test
    void nestedFixedWidthFlowMatchesItsLayoutSnapshot() throws Exception {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page
                    .name("NestedFixedWidthFlow")
                    .addSection(outer -> outer
                            .name("Outer")
                            .fixedWidth(300)
                            .padding(DocumentInsets.of(12))
                            .addSection(inner -> inner
                                    .name("Inner")
                                    .fixedWidth(160)
                                    .addParagraph(BODY))));

            LayoutSnapshotAssertions.assertMatches(document, "flow-fixed-width/nested_fixed_width");
        }
    }

    // --- the painted box and the wrapping width are the same number ---------

    @Test
    void aFixedWidthSectionInARowSlotSeatsItsChildrenInsideThePaintedBox() {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page.addRow(row -> row
                    .name("Band")
                    .addSection(s -> s
                            .name("Card")
                            // A horizontal margin used to make the two bases disagree: the
                            // row prepared this child against the slot less its margin and
                            // then handed placement the whole slot, so the margin came off
                            // twice and the card was placed at 100pt. It comes off once
                            // now, and 120 + 40 fits inside the 180pt slot.
                            .margin(new DocumentInsets(0, 20, 0, 20))
                            .fixedWidth(120)
                            // A centred paragraph reports the full width it was given
                            // rather than shrinking to its longest line, so its placed
                            // width IS the region the box handed its children — which is
                            // the number under test. A left-aligned one would hug its
                            // text and hide the difference.
                            .addParagraph(p -> p.name("Body").text("Short.").align(TextAlign.CENTER)))
                    .addSection(s -> s.name("Filler").addParagraph("f"))));

            PlacedNode card = node(document, "Card");
            PlacedNode body = node(document, "Body");

            // The width the author asked for, not a fraction of it: two 180pt slots,
            // a 40pt horizontal margin, and a card that must still be exactly 120.
            assertThat(card.placementWidth()).isCloseTo(120.0, within(0.01));
            // And the children sit inside the box that is actually painted — text can
            // never wrap wider than its own background.
            assertThat(body.placementWidth()).isLessThanOrEqualTo(card.placementWidth() + 0.01);
            assertThat(body.placementX() + body.placementWidth())
                    .isLessThanOrEqualTo(card.placementX() + card.placementWidth() + 0.01);
        }
    }

    @Test
    void aRowColumnWiderThanItsSlotLessTheMarginIsClampedToWhatIsLeft() {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page.addRow(row -> row
                    .name("Band")
                    .addSection(s -> s
                            .name("Card")
                            // The other side of "as long as that width plus the margin
                            // fits the slot": 160 + 40 does not fit 180, so the clamp
                            // that makes a computed width safe resolves it to the 140
                            // the margin leaves rather than overflowing the row.
                            .margin(new DocumentInsets(0, 20, 0, 20))
                            .fixedWidth(160)
                            .addParagraph(p -> p.name("Body").text("Short.").align(TextAlign.CENTER)))
                    .addSection(s -> s.name("Filler").addParagraph("f"))));

            PlacedNode card = node(document, "Card");
            assertThat(card.placementWidth()).isCloseTo(140.0, within(0.01));
            assertThat(card.placementX() + card.placementWidth())
                    .as("the card stays inside its slot")
                    .isLessThanOrEqualTo(20.0 + INNER_WIDTH / 2 - 20.0 + 0.01);
        }
    }

    @Test
    void aFixedWidthSectionInARowSlotIsPlacedAtTheRequestedWidth() {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page.addRow(row -> row
                    .name("Band")
                    .addSection(s -> s
                            .name("Card")
                            .fixedWidth(120)
                            .addParagraph(p -> p.name("Body").text("Short.").align(TextAlign.CENTER)))
                    .addSection(s -> s.name("Filler").addParagraph("f"))));

            // The row slot is 180pt wide; the card asked for 120 and must get exactly
            // that, not the slot and not some re-derived fraction of it.
            assertThat(node(document, "Card").placementWidth()).isCloseTo(120.0, within(0.01));
            assertThat(node(document, "Body").placementWidth()).isCloseTo(120.0, within(0.01));
        }
    }

    @Test
    void bleedStillReachesThePageEdgeOnAFixedWidthSection() {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page.addSection(s -> s
                    .name("Band")
                    .fixedWidth(120)
                    .fillColor(DocumentColor.rgb(226, 236, 245))
                    .bleedToEdge(DocumentEdge.LEFT, DocumentEdge.RIGHT)
                    .addParagraph("Bled band")));

            PlacedNode band = node(document, "Band");
            PlacedFragment fill = document.layoutGraph().fragments().stream()
                    .filter(f -> f.payload() instanceof ShapeFragmentPayload)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no fill fragment for the bled band"));

            // The documented precedence: the node keeps the width it asked for, while
            // the decoration it opted to bleed still reaches both trimmed page edges.
            assertThat(band.placementWidth()).isCloseTo(120.0, within(0.01));
            assertThat(fill.x()).isCloseTo(0.0, within(0.01));
            assertThat(fill.width()).isCloseTo(PAGE_WIDTH, within(0.01));
        }
    }

    @Test
    void aFixedWidthRootFlowKeepsItsWidthUnderPerPageMargins() {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(20))
                .create()) {
            // Per-page margins send the ROOT walk down the page-column branch, which
            // re-derives each direct child's region from the page it starts on — a
            // different number from the root's own width (280 here, not 200). A
            // fixed-width root owns its width there too, so its children stay inside it.
            document.pageMargins(List.of(PageMarginRule.page(1, DocumentInsets.of(60))));
            document.pageFlow(page -> page
                    .name("NarrowFlow")
                    .fixedWidth(200)
                    .addParagraph(p -> p.name("Body").text("Short.").align(TextAlign.CENTER)));

            PlacedNode flow = node(document, "NarrowFlow");
            PlacedNode body = node(document, "Body");

            assertThat(flow.placementWidth()).isCloseTo(200.0, within(0.01));
            assertThat(body.placementWidth()).isLessThanOrEqualTo(200.0 + 0.01);
        }
    }

    @Test
    void aFixedWidthRootFlowIsCappedByTheColumnOfALaterNarrowerPage() {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(20))
                .create()) {
            // Page 2 onwards has an 80pt margin, so its content column is 240pt wide
            // against page 1's 360pt. The root asks for 340pt: legal on page 1, wider
            // than page 2 can hold. The x of a child starting on page 2 is re-seated to
            // that page's margin, so a width left at page 1's value walks the child off
            // the sheet — 80 + 340 = 420 on a 400pt page.
            document.pageMargins(List.of(PageMarginRule.from(2, DocumentInsets.of(80))));
            document.pageFlow(page -> page
                    .name("WideFlow")
                    .fixedWidth(340)
                    .addSection(a -> a.name("PageOne").addParagraph("First page."))
                    .addPageBreak(br -> br.name("ToPageTwo"))
                    .addSection(b -> b.name("PageTwo")
                            .addParagraph(p -> p.name("Body").text("Short.").align(TextAlign.CENTER))));

            PlacedNode body = node(document, "Body");

            assertThat(body.startPage()).as("the probe must actually land on page 2").isEqualTo(1);
            assertThat(body.placementX() + body.placementWidth())
                    .as("right edge must stay inside page 2's content column")
                    .isLessThanOrEqualTo(PAGE_WIDTH - 80 + 0.01);
            assertThat(body.placementWidth()).isLessThanOrEqualTo(240.0 + 0.01);
        }
    }

    @Test
    void aSubPointFixedWidthFailsNamingTheFixedWidthNotTheParent() {
        try (DocumentSession document = document()) {
            // A width below the engine's floor passes DocumentFlowWidth.of and can only
            // fail at layout. The message has to name the knob the author actually
            // turned, not send them to the parent's padding.
            assertThatThrownBy(() -> {
                document.pageFlow(page -> page.addSection(s -> s
                        .name("Sliver")
                        .fixedWidth(1e-9)
                        .addParagraph(BODY)));
                document.layoutGraph();
            })
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("fixed width");
        }
    }

    // --- the root flow container -------------------------------------------

    @Test
    void aFixedWidthRootFlowNarrowsItselfAndItsChildren() {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page
                    .name("NarrowFlow")
                    .fixedWidth(200)
                    .addSection(s -> s.name("Card").addParagraph(BODY)));

            assertThat(node(document, "NarrowFlow").placementWidth()).isCloseTo(200.0, within(0.01));
            assertThat(node(document, "Card").placementWidth()).isLessThanOrEqualTo(200.0 + 0.01);
        }
    }

    // --- nothing moved for flows that never opted in ------------------------

    /**
     * Proves the compatibility constructor defaults to {@code natural()} and that the
     * default is inert down to the serialized snapshot. It does <em>not</em> by itself
     * prove compatibility with pre-change output — the evidence for that is every
     * committed layout snapshot in the repository still matching after this change.
     */
    @Test
    void aSectionBuiltThroughTheCompatibilityConstructorLaysOutIdenticallyToOneGivenNatural() throws Exception {
        String viaCompatibilityConstructor = snapshotJsonOf(new SectionNode(
                "Card", List.of(paragraph()), 6, DocumentInsets.of(8), DocumentInsets.zero(),
                null, null, null, null, false, null, null, null, false));
        String viaCanonicalConstructor = snapshotJsonOf(new SectionNode(
                "Card", List.of(paragraph()), 6, DocumentInsets.of(8), DocumentInsets.zero(),
                null, null, null, null, false, null, null, null, false, DocumentFlowWidth.natural()));

        // The new record component is inert unless something asks for it: the old
        // constructor and the new one agree down to the serialized snapshot.
        assertThat(viaCanonicalConstructor).isEqualTo(viaCompatibilityConstructor);
    }

    // --- visual artifact ----------------------------------------------------

    @Test
    void fixedWidthFlowRendersToPdf() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow(page -> page
                    .name("FixedWidthShowcase")
                    .spacing(10)
                    .addParagraph("Unconstrained — hugs its content.")
                    // The fills are what make the pinned width visible: the panel is
                    // the box, so a reader can see 240 and 120 rather than infer them
                    // from where the text happens to wrap.
                    .addSection(s -> s
                            .name("Card240")
                            .fixedWidth(240)
                            .softPanel(DocumentColor.rgb(226, 236, 245), 6, 8)
                            .addParagraph("fixedWidth(240), padding inside it. " + BODY))
                    .addSection(s -> s
                            .name("Card120")
                            .fixedWidth(120)
                            .softPanel(DocumentColor.rgb(245, 232, 226), 6, 8)
                            .addParagraph("fixedWidth(120) — same words, more lines.")));

            byte[] pdf = document.toPdfBytes();
            Path output = VisualTestOutputs.preparePdf("flow_fixed_width", "flow-fixed-width");
            Files.write(output, pdf);

            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        }
    }

    // --- helpers ------------------------------------------------------------

    private static DocumentSession document() {
        return GraphCompose.document()
                .pageSize(PAGE_WIDTH, PAGE_HEIGHT)
                .margin(DocumentInsets.of(20))
                .create();
    }

    private static com.demcha.compose.document.node.ParagraphNode paragraph() {
        return new com.demcha.compose.document.dsl.ParagraphBuilder().name("Body").text(BODY).build();
    }

    /** Lays out a single named "Card" section configured by {@code spec} and returns its placed node. */
    private static PlacedNode cardWith(Consumer<com.demcha.compose.document.dsl.SectionBuilder> spec) {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page.addSection(s -> {
                s.name("Card");
                spec.accept(s);
            }));
            return node(document, "Card");
        }
    }

    private static String snapshotJsonOf(SectionNode section) throws java.io.IOException {
        try (DocumentSession document = document()) {
            document.pageFlow(page -> page.name("Flow").add(section));
            return LayoutSnapshotJson.toJson(document.layoutSnapshot());
        }
    }

    private static PlacedNode node(DocumentSession document, String semanticName) {
        return document.layoutGraph().nodes().stream()
                .filter(n -> semanticName.equals(n.semanticName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no node named '" + semanticName + "' in the layout graph"));
    }
}
