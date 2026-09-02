package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link OrangeOps} — freezes the resolved
 * geometry of the canonical one-page CV, which the pixel budget cannot see: a
 * small column or spacing shift stays under the visual-diff budget but changes
 * the snapshot.
 *
 * <p>The preset owns its page geometry, so the session starts unconfigured
 * apart from the display family it does not carry.</p>
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a
 * deliberate layout change, and commit the JSON with the change.</p>
 */
class OrangeOpsLayoutSnapshotTest {

    @Test
    void canonicalCvMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            OrangeOpsTestFont.register(session);
            OrangeOps.create().compose(session, OrangeOpsFixtures.canonicalCv());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "orange_ops_layout", "cv-v2");
        }
    }
}
