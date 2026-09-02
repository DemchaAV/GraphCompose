package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link NavySidebar} - freezes the resolved
 * geometry of the canonical one-page CV, which is what catches a shift the
 * pixel budget is too coarse to see.
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a
 * deliberate layout change, and commit the JSON with the change.</p>
 */
class NavySidebarLayoutSnapshotTest {

    @Test
    void canonicalCvMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            NavySidebar.create().compose(session, NavySidebarFixtures.canonicalCv());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "navy_sidebar_layout", "cv-v2");
        }
    }
}
