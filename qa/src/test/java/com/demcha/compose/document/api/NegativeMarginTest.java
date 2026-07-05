package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

/**
 * A negative <em>page</em> margin is rejected (it would make the content area
 * larger than the sheet), while a negative <em>node</em> bottom margin now pulls
 * following content up — symmetric with a negative top margin, which was already
 * honoured. Previously the vertical flow silently dropped it.
 */
class NegativeMarginTest {

    @Test
    void negativePageMarginIsRejectedViaTheBuilder() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> GraphCompose.document().pageSize(300, 300).margin(-2, 0, 0, 0).create())
                .withMessageContaining("bleed");
    }

    @Test
    void negativePageMarginIsRejectedViaTheSessionSetter() {
        try (DocumentSession session = GraphCompose.document().pageSize(300, 300).create()) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> session.margin(new DocumentInsets(0, 0, -5, 0)))
                    .withMessageContaining("non-negative");
        }
    }

    @Test
    void zeroAndPositivePageMarginAreAccepted() {
        GraphCompose.document().pageSize(300, 300).margin(0, 0, 0, 0).create().close();
        GraphCompose.document().pageSize(300, 300).margin(DocumentInsets.of(24)).create().close();
    }

    @Test
    void negativeBottomNodeMarginPullsFollowingContentUp() {
        double baseline = followingNodeY(0);
        double pulled = followingNodeY(-20);

        // y grows upward, so "pulled up" means a larger y; the shift equals the margin.
        assertThat(pulled).isGreaterThan(baseline);
        assertThat(pulled - baseline).isCloseTo(20.0, within(0.5));
    }

    /** Lays out [filler, section A (bottom margin = m), section B] and returns B's placed y. */
    private double followingNodeY(double aBottomMargin) {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(240, 400)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> {
                page.addParagraph("filler so the cursor is well below the page top");
                page.addSection(s -> s.name("A")
                        .margin(new DocumentInsets(0, 0, aBottomMargin, 0))
                        .addParagraph("A"));
                page.addSection(s -> s.name("B").addParagraph("B"));
            });
            PlacedNode b = session.layoutGraph().nodes().stream()
                    .filter(n -> "B".equals(n.semanticName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("section B not found in layout graph"));
            return b.placementY();
        }
    }
}
