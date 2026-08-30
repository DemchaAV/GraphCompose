package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.proposal.ProposalAcceptance;
import com.demcha.compose.document.templates.data.proposal.ProposalDeliverables;
import com.demcha.compose.document.templates.data.proposal.ProposalInvestment;
import com.demcha.compose.document.templates.data.proposal.ProposalGoals;
import com.demcha.compose.document.templates.data.proposal.ProposalPhaseGrid;
import com.demcha.compose.document.templates.data.proposal.ProposalScope;
import com.demcha.compose.document.templates.data.proposal.ProposalSummaryBlock;
import com.demcha.compose.document.templates.data.proposal.ProposalTermsBlock;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalData;
import com.demcha.compose.document.templates.data.proposal.StructuredProposalDocumentSpec;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke test for {@link EditorialProposal} — proves the preset renders a
 * {@link StructuredProposalDocumentSpec} end-to-end with its packaged SVG
 * icon set, renders an empty document through its guards, puts the table
 * content on the text layer where the layout snapshot cannot see it, and
 * reports its two data contracts (unknown icon token, phase-grid header
 * count) by name.
 *
 * <p>It also pins what the two proposal presets do and do not share: the
 * document model and the four {@code fact-*} tokens, yes; the badge and
 * goal tokens, no — each preset packages its own set.</p>
 */
class EditorialProposalSmokeTest {

    private static byte[] render(StructuredProposalDocumentSpec spec) throws Exception {
        // The preset owns its page geometry, so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            EditorialProposal.create().compose(session, spec);
            assertThat(session.roots()).isNotEmpty();
            byte[] pdfBytes = session.toPdfBytes();
            assertThat(pdfBytes).isNotEmpty();
            return pdfBytes;
        }
    }

    private static String textOf(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<StructuredProposalDocumentSpec> template = EditorialProposal.create();
        assertThat(template.id()).isEqualTo(EditorialProposal.ID);
        assertThat(template.displayName()).isEqualTo(EditorialProposal.DISPLAY_NAME);
    }

    @Test
    void rendersCanonicalProposalWithPackagedIcons() throws Exception {
        render(EditorialProposalFixtures.canonicalProposal());
    }

    @Test
    void rendersEmptyProposal() throws Exception {
        // Exercises the guards: no goal cells, no phase grid, no signature
        // row — and the blank icon tokens that carry no packaged mark.
        render(StructuredProposalDocumentSpec.from(StructuredProposalData.builder().build()));
    }

    @Test
    void canonicalRenderCarriesTheTableText() throws Exception {
        // The phase grid and the investment table are single leaf nodes in
        // the layout snapshot, so their content is asserted on the text layer.
        String text = textOf(render(EditorialProposalFixtures.canonicalProposal()));
        assertThat(text)
                .contains("Discovery report")
                .contains("Brand Identity Refinement")
                .contains("2 weeks")
                .contains("TOTAL INVESTMENT")
                .contains("£19,250");
    }

    @Test
    void theSameDocumentRendersThroughBothProposalPresets() throws Exception {
        // The point of the structured model: one document, two looks. The
        // fact tokens are named alike in both packaged sets, so they stay in
        // the document; the badge and goal tokens are not, so those are the
        // ones a move has to clear.
        StructuredProposalDocumentSpec spec =
                StructuredProposalDocumentSpec.from(withoutSectionIconTokens(
                        EditorialProposalFixtures.canonicalProposal().proposal()));
        String editorial = textOf(render(spec));
        byte[] northline;
        try (DocumentSession session = GraphCompose.document().create()) {
            NorthlineProposal.create().compose(session, spec);
            northline = session.toPdfBytes();
        }
        assertThat(textOf(northline))
                .contains("Executive Summary")
                .contains("TOTAL INVESTMENT");
        assertThat(editorial).contains("Executive Summary");
    }

    /**
     * The same document with the preset-scoped tokens cleared — every
     * section badge and the goal mark — and the shared {@code fact-*} tokens
     * left in place, since both packaged sets name those alike.
     */
    private static StructuredProposalData withoutSectionIconTokens(
            StructuredProposalData data) {
        return StructuredProposalData.builder()
                .brand(data.brand())
                .title(data.title())
                .meta(data.meta())
                .executiveSummary(new ProposalSummaryBlock(data.executiveSummary().heading(),
                        "", data.executiveSummary().paragraphs()))
                .glance(data.glance())
                .goals(new ProposalGoals(data.goals().heading(), "",
                        data.goals().items().stream()
                                .map(g -> new ProposalGoals.Goal("", g.text()))
                                .toList()))
                .scope(new ProposalScope(data.scope().heading(), "", data.scope().items()))
                .deliverables(new ProposalDeliverables(data.deliverables().heading(), "",
                        data.deliverables().leftColumn(), data.deliverables().rightColumn()))
                .timeline(new ProposalPhaseGrid(data.timeline().heading(), "",
                        data.timeline().columnHeaders(), data.timeline().phases()))
                .investment(new ProposalInvestment(data.investment().heading(), "",
                        data.investment().itemHeader(), data.investment().amountHeader(),
                        data.investment().rows(), data.investment().totalLabel(),
                        data.investment().totalAmount()))
                .terms(new ProposalTermsBlock(data.terms().heading(), "", data.terms().items()))
                .acceptance(new ProposalAcceptance(data.acceptance().heading(), "",
                        data.acceptance().statement(), data.acceptance().fields()))
                .build();
    }

    @Test
    void rejectsUnknownIconTokenByName() {
        StructuredProposalDocumentSpec spec = StructuredProposalDocumentSpec.from(
                StructuredProposalData.builder()
                        .goals(new ProposalGoals("Project Goals", "no-such-icon",
                                List.of(new ProposalGoals.Goal("", "A goal."))))
                        .build());
        try (DocumentSession session = GraphCompose.document().create()) {
            assertThatThrownBy(() -> EditorialProposal.create().compose(session, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no-such-icon");
        }
    }

    @Test
    void phaseGridRejectsHeaderCountMismatchByName() {
        StructuredProposalDocumentSpec spec = StructuredProposalDocumentSpec.from(
                StructuredProposalData.builder()
                        .timeline(new ProposalPhaseGrid("Timeline", "",
                                List.of("PHASE", "FOCUS", "DURATION"),
                                List.of(new ProposalPhaseGrid.Phase("01", "Discover",
                                        "Research", "1 week", "Report"))))
                        .build());
        try (DocumentSession session = GraphCompose.document().create()) {
            assertThatThrownBy(() -> EditorialProposal.create().compose(session, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("one header per column");
        }
    }
}
