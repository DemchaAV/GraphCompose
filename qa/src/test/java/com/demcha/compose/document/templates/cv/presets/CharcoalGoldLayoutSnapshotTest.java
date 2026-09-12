package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link CharcoalGold} - freezes the resolved
 * geometry of the canonical one-page CV.
 *
 * <p>This preset leaves the page to the caller, so the session sets A4 with
 * no margin: both columns run to the paper edge and each carries its own
 * padding.</p>
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a
 * deliberate layout change, and commit the JSON with the change.</p>
 */
class CharcoalGoldLayoutSnapshotTest {

    @Test
    void canonicalCvMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(0f, 0f, 0f, 0f)
                .create()) {
            CharcoalGold.create().compose(session, CharcoalGoldFixtures.canonicalCv());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "charcoal_gold_layout", "cv-v2");
        }
    }
}
