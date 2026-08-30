package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link NorthlineProposal} — freezes the
 * resolved geometry of both pages, including the natural page-one → page-two
 * break, which the pixel budget alone could quietly absorb shifting.
 *
 * <p>The preset is a fixed two-page composition (each page's bands are
 * composed explicitly), so the canonical fixture IS the pagination
 * contract; there is no overflow fixture to grow.</p>
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a
 * deliberate layout change, and commit the JSON with the change.</p>
 */
class NorthlineProposalLayoutSnapshotTest {

    @Test
    void canonicalProposalMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            NorthlineProposal.create().compose(
                    session, NorthlineProposalFixtures.canonicalProposal());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(2);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "northline_proposal_layout", "proposal");
        }
    }
}
