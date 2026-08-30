package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link EditorialProposal} — freezes the
 * resolved geometry of both pages, including the natural page-one → page-two
 * break the masthead's {@code keepWithNext} guarantees.
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a
 * deliberate layout change, and commit the JSON with the change.</p>
 */
class EditorialProposalLayoutSnapshotTest {

    @Test
    void canonicalProposalMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            EditorialProposal.create().compose(
                    session, EditorialProposalFixtures.canonicalProposal());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(2);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "editorial_proposal_layout", "proposal");
        }
    }
}
