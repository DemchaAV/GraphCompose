package com.demcha.compose.document.dsl;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the opt-in {@code keepTogether()} pagination behavior: a section that
 * does not fit in the remaining page space relocates whole to the next page
 * instead of orphaning its leading children. Default flow behavior is unchanged.
 */
class SectionKeepTogetherTest {

    /** Builds a page with a tall filler then a two-child card; returns the card node. */
    private static PlacedNode cardNode(boolean keepTogether) {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(12)
                    // Filler consumes most of page 1 so the card cannot fit below it.
                    .addSection("Filler", s -> s.addShape(260, 250, DocumentColor.rgb(220, 220, 220)))
                    .addSection("Card", s -> {
                        if (keepTogether) {
                            s.keepTogether();
                        }
                        s.spacing(8)
                                .addParagraph("Heading")
                                .addShape(260, 120, DocumentColor.rgb(20, 80, 95));
                    })
                    .build();

            LayoutGraph graph = document.layoutGraph();
            return graph.nodes().stream()
                    .filter(n -> "Card".equals(n.semanticName()))
                    .findFirst().orElseThrow();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void keepTogetherRelocatesTheWholeCardToTheNextPage() {
        PlacedNode card = cardNode(true);
        // The card sits entirely on page 1 (0-indexed), not straddling.
        assertThat(card.startPage()).isEqualTo(card.endPage());
        assertThat(card.startPage()).isEqualTo(1);
    }

    @Test
    void moduleKeepTogetherRelocatesWhole() {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(12)
                    .addSection("Filler", s -> s.addShape(260, 250, DocumentColor.rgb(220, 220, 220)))
                    .module(m -> m.name("ModCard").title("Module title").keepTogether()
                            .addShape(260, 120, DocumentColor.rgb(20, 80, 95)))
                    .build();

            PlacedNode mod = document.layoutGraph().nodes().stream()
                    .filter(n -> "ModCard".equals(n.semanticName()))
                    .findFirst().orElseThrow();
            assertThat(mod.startPage()).isEqualTo(mod.endPage());
            assertThat(mod.startPage()).isEqualTo(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void timelineKeepTogetherRelocatesWhole() {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            // buildInto is package-private; building into a named section lets the
            // test locate the timeline node while exercising the real DSL path.
            TimelineBuilder timeline = new TimelineBuilder()
                    .keepTogether()
                    .entry(TimelineMarker.dot(6, DocumentColor.rgb(20, 80, 95)),
                            e -> e.title("Role A").body("Did things for a while."))
                    .entry(TimelineMarker.dot(6, DocumentColor.rgb(196, 153, 76)),
                            e -> e.title("Role B").body("Did more things."));
            document.pageFlow().name("Flow").spacing(12)
                    // Leaves ~45pt on page 0 so the two-entry timeline cannot fit there.
                    .addSection("Filler", s -> s.addShape(260, 300, DocumentColor.rgb(220, 220, 220)))
                    .addSection("Timeline", timeline::buildInto)
                    .build();

            PlacedNode tl = document.layoutGraph().nodes().stream()
                    .filter(n -> "Timeline".equals(n.semanticName()))
                    .findFirst().orElseThrow();
            assertThat(tl.startPage()).isEqualTo(tl.endPage());
            assertThat(tl.startPage()).isEqualTo(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void keepTogetherSectionTallerThanAPageStillFlows() {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(300, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").spacing(12)
                    .addSection("Filler", s -> s.addShape(260, 250, DocumentColor.rgb(220, 220, 220)))
                    .addSection("Oversized", s -> {
                        s.keepTogether().spacing(8);
                        // 5 × 100pt + spacing ≈ 532pt — taller than the 360pt
                        // inner page, so relocation cannot help.
                        for (int i = 0; i < 5; i++) {
                            s.addShape(260, 100, DocumentColor.rgb(20, 80, 95));
                        }
                    })
                    .build();

            PlacedNode oversized = document.layoutGraph().nodes().stream()
                    .filter(n -> "Oversized".equals(n.semanticName()))
                    .findFirst().orElseThrow();
            // The keep-together request is ignored for a block taller than a
            // full page: the section starts in the remaining space on page 0
            // (no pointless relocation) and flows across the boundary.
            assertThat(oversized.startPage()).isEqualTo(0);
            assertThat(oversized.endPage()).isGreaterThan(oversized.startPage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void withoutKeepTogetherTheCardStraddlesThePageBoundary() {
        PlacedNode card = cardNode(false);
        // Default behavior: the section flows across the boundary (heading on the
        // first page, shape on the next) — start and end pages differ.
        assertThat(card.endPage()).isGreaterThan(card.startPage());
    }
}
