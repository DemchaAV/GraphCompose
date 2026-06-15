package com.demcha.compose;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic regression gate for the F5 render-operator coalescing, driving
 * {@link RenderOperatorProbe#countOperators}.
 *
 * <p>Before F5 the paragraph handler emitted one {@code setFont} (Tf) and one
 * non-stroking-colour op per text-show, so font/colour ops scaled 1:1 with the
 * per-line {@code Tj}/{@code TJ} draws. After F5 they are coalesced, so a single
 * styled paragraph that wraps to many lines emits far fewer Tf/colour ops than
 * draws. Asserting {@code tf < draws} and {@code rg < draws} pins that
 * structural win as a build-failing check — a regression back to per-span font
 * ops (bloated content streams) breaks this test instead of passing CI. The
 * assertion is content-independent: it does not hardcode brittle exact counts.</p>
 */
class RenderOperatorGateTest {

    private static final String LONG_PARAGRAPH =
            ("GraphCompose lays out structured business documents across many pages "
                    + "while keeping header and footer placement stable. ").repeat(30);

    @Test
    void fontAndColourOpsStayCoalescedBelowTextDraws() throws Exception {
        RenderOperatorProbe.OpCounts counts =
                RenderOperatorProbe.countOperators(flow -> flow.addParagraph(LONG_PARAGRAPH));

        assertThat(counts.draws())
                .as("a long paragraph must wrap to many text-show ops")
                .isGreaterThanOrEqualTo(10);
        assertThat(counts.tf())
                .as("setFont ops must be coalesced below the per-line draw count (F5), not 1:1")
                .isLessThan(counts.draws());
        assertThat(counts.rg())
                .as("non-stroking colour ops must be coalesced below the per-line draw count (F5)")
                .isLessThan(counts.draws());
    }
}
