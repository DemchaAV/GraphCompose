package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link SerifHeadline} - freezes the resolved
 * geometry of the canonical one-page CV. It matters more here than on the
 * simpler sheets: almost every distance in this design is computed from a
 * reference grid and a set of type metrics, so a change to one constant moves
 * a great many nodes at once.
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a
 * deliberate layout change, and commit the JSON with the change.</p>
 */
class SerifHeadlineLayoutSnapshotTest {

    @Test
    void canonicalCvMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            SerifHeadline.create().compose(session, SerifHeadlineFixtures.canonicalCv());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "serif_headline_layout", "cv-v2");
        }
    }
}
