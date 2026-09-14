package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link ProfessionalSidebar} — freezes the
 * resolved geometry of the canonical one-page CV, which is what catches a
 * shift the pixel budget is too coarse to see, and of a CV longer than the
 * sheet, which is where the page boundaries the preset picks can move.
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a
 * deliberate layout change, and commit the JSON with the change.</p>
 */
class ProfessionalSidebarLayoutSnapshotTest {

    @Test
    void canonicalCvMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            ProfessionalSidebar.create().compose(
                    session, ProfessionalSidebarFixtures.canonicalCv());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "professional_sidebar_layout", "cv-v2");
        }
    }

    @Test
    void cvLongerThanTheSheetMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            ProfessionalSidebar.create().compose(
                    session, ProfessionalSidebarFixtures.longCv());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(2);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "professional_sidebar_long_layout", "cv-v2");
        }
    }
}
