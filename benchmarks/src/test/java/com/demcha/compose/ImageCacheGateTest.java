package com.demcha.compose;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic regression gate for {@code PdfImageCache} dedup, driving
 * {@link ImageCacheOperatorProbe}'s render + count helpers.
 *
 * <p>The cache keys embedded image XObjects by content fingerprint, so the same
 * image placed many times must embed once (referenced by many draws) while
 * distinct images embed once each. Counting the embedded XObjects in the output
 * PDF makes that structural invariant a build-failing assertion — a regression
 * that re-embeds the same image per placement (PDF bloat) breaks this test
 * rather than silently passing CI.</p>
 */
class ImageCacheGateTest {

    @Test
    void sameImageEmbedsOnceRegardlessOfPlacements() throws Exception {
        int placements = 30;

        ImageCacheOperatorProbe.EmbedCounts counts =
                ImageCacheOperatorProbe.countPdf(ImageCacheOperatorProbe.renderSameImage(placements));

        assertThat(counts.embeds())
                .as("the same image placed %d times must embed exactly one XObject", placements)
                .isEqualTo(1);
        assertThat(counts.draws())
                .as("each placement must still draw the cached image")
                .isGreaterThanOrEqualTo(placements);
    }

    @Test
    void distinctImagesEachEmbedOnce() throws Exception {
        int distinct = 8;

        ImageCacheOperatorProbe.EmbedCounts counts =
                ImageCacheOperatorProbe.countPdf(ImageCacheOperatorProbe.renderDistinctImages(distinct));

        assertThat(counts.embeds())
                .as("%d distinct images must embed %d XObjects (no over-dedup)", distinct, distinct)
                .isEqualTo(distinct);
        assertThat(counts.draws())
                .as("each distinct image must be drawn")
                .isGreaterThanOrEqualTo(distinct);
    }
}
