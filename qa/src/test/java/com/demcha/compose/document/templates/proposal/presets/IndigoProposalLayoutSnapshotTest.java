package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link IndigoProposal} — freezes the resolved
 * geometry of the one-page sheet, whose blocks are placed to a stated position
 * rather than stacked, so a shift the pixel budget would absorb still shows up
 * here as a moved node.
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a deliberate
 * layout change, and commit the JSON with the change.</p>
 */
class IndigoProposalLayoutSnapshotTest {

    @Test
    void canonicalProposalMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            IndigoProposal.create().compose(
                    session, IndigoProposalFixtures.canonicalProposal());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(
                    session, "indigo_proposal_layout", "proposal");
        }
    }
}
