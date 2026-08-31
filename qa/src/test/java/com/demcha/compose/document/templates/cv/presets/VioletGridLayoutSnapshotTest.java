package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link VioletGrid} — freezes the resolved
 * geometry of the canonical one-page CV, which the pixel budget cannot see: a
 * small column or spacing shift stays under the visual-diff budget but changes
 * the snapshot.
 *
 * <p>The preset owns its page geometry, so the session starts unconfigured.</p>
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a
 * deliberate layout change, and commit the JSON with the change.</p>
 */
class VioletGridLayoutSnapshotTest {

    @Test
    void canonicalCvMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            VioletGrid.create().compose(session, VioletGridFixtures.canonicalCv());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "violet_grid_layout", "cv-v2");
        }
    }
}
