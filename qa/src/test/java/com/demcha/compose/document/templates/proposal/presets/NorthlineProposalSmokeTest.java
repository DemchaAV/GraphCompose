package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.proposal.ProposalPhaseGrid;
import com.demcha.compose.document.templates.data.proposal.ProposalSummaryBlock;
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
 * Smoke test for the structured proposal pipeline through
 * {@link NorthlineProposal} — proves the preset renders a
 * {@link StructuredProposalDocumentSpec} end-to-end, that the packaged
 * icon set resolves from the templates artifact (a full render loads
 * every icon token the canonical fixture names), that an empty document
 * renders through the section guards, and that the phase-grid header
 * contract fails with a named count rather than an index error.
 */
class NorthlineProposalSmokeTest {

    private static void render(StructuredProposalDocumentSpec spec) throws Exception {
        // The preset owns its page geometry (size, margins, background),
        // so the session starts unconfigured.
        try (DocumentSession session = GraphCompose.document().create()) {
            NorthlineProposal.create().compose(session, spec);
            assertThat(session.roots()).isNotEmpty();
            // Drive layout + render — the icon loading, the zero-row table
            // guards, and the chrome only exist at render time.
            assertThat(session.toPdfBytes()).isNotEmpty();
        }
    }

    @Test
    void exposesStableIdentity() {
        DocumentTemplate<StructuredProposalDocumentSpec> template = NorthlineProposal.create();
        assertThat(template.id()).isEqualTo(NorthlineProposal.ID);
        assertThat(template.displayName()).isEqualTo(NorthlineProposal.DISPLAY_NAME);
    }

    @Test
    void rendersCanonicalProposalWithPackagedIcons() throws Exception {
        render(NorthlineProposalFixtures.canonicalProposal());
    }

    @Test
    void rendersEmptyProposal() throws Exception {
        // Exercises the guards: no goal cells (the flattened column spec
        // cannot describe zero cells), no phase grid, no signature row —
        // and the investment table still carries its header + total band.
        render(StructuredProposalDocumentSpec.from(
                StructuredProposalData.builder().build()));
    }

    @Test
    void canonicalRenderCarriesTheTableText() throws Exception {
        // The phase grid and the investment table are single leaf nodes in
        // the layout snapshot (the recorded composed-cell blind spot), so
        // their content is asserted on the rendered text layer instead.
        byte[] pdfBytes;
        try (DocumentSession session = GraphCompose.document().create()) {
            NorthlineProposal.create().compose(
                    session, NorthlineProposalFixtures.canonicalProposal());
            pdfBytes = session.toPdfBytes();
        }
        String text;
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            text = new PDFTextStripper().getText(document);
        }
        // Composed cells wrap, so the assertions stay within one line.
        assertThat(text)
                .contains("Discovery report")
                .contains("Brand Identity Refinement")
                .contains("2 weeks")
                .contains("TOTAL INVESTMENT")
                .contains("£19,250");
    }

    @Test
    void rejectsUnknownIconTokenByName() {
        StructuredProposalDocumentSpec spec = StructuredProposalDocumentSpec.from(
                StructuredProposalData.builder()
                        .executiveSummary(new ProposalSummaryBlock("SUMMARY",
                                "no-such-icon", List.of("Body.")))
                        .build());
        try (DocumentSession session = GraphCompose.document().create()) {
            assertThatThrownBy(() -> NorthlineProposal.create().compose(session, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no-such-icon");
        }
    }

    @Test
    void headerCountIsNotValidatedWhenTheGridNeverRenders() throws Exception {
        // A wrong header count with zero phases constrains nothing — the
        // grid is skipped before the validation.
        render(StructuredProposalDocumentSpec.from(StructuredProposalData.builder()
                .timeline(new ProposalPhaseGrid("TIMELINE", "badge-timeline",
                        List.of("PHASE", "FOCUS", "DURATION"), List.of()))
                .build()));
    }

    @Test
    void phaseGridRejectsHeaderCountMismatchByName() {
        StructuredProposalDocumentSpec spec = StructuredProposalDocumentSpec.from(
                StructuredProposalData.builder()
                        .timeline(new ProposalPhaseGrid("TIMELINE", "badge-timeline",
                                List.of("PHASE", "FOCUS", "DURATION"),
                                List.of(new ProposalPhaseGrid.Phase("01", "Discover",
                                        "Research", "1 week", "Report"))))
                        .build());
        try (DocumentSession session = GraphCompose.document().create()) {
            assertThatThrownBy(() -> NorthlineProposal.create().compose(session, spec))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("one header per column");
        }
    }
}
