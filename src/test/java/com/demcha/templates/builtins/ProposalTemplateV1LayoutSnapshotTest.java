package com.demcha.templates.builtins;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.layout_core.core.PdfComposer;
import com.demcha.mock.ProposalDataFixtures;
import com.demcha.templates.data.ProposalData;
import com.demcha.compose.testing.layout.LayoutSnapshotAssertions;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class ProposalTemplateV1LayoutSnapshotTest {

    private final ProposalTemplateV1 template = new ProposalTemplateV1();
    private final ProposalData data = ProposalDataFixtures.longProposal();

    @Test
    void shouldMatchLongProposalLayoutSnapshot() throws Exception {
        try (PdfComposer composer = GraphCompose.pdf()
                .pageSize(PDRectangle.A4)
                .margin(22, 22, 22, 22)
                .markdown(true)
                .create()) {
            template.compose(composer, data);
            LayoutSnapshotAssertions.assertMatches(composer, "templates/proposal/proposal_long_layout");
        }
    }
}
