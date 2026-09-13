package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Structural layout-snapshot gate for the {@link ProposalDocumentSpec}
 * proposal presets — the sibling of {@link ProposalV2VisualParityTest}, on its
 * {@link ProposalV2VisualParityTest#canonicalProposal()}.
 *
 * <p>The structured proposal presets ({@code EditorialProposal},
 * {@code IndigoProposal}, {@code NorthlineProposal}) take a different spec and
 * carry a snapshot suite each, so they are not listed here.</p>
 *
 * <p>Each preset composes the canonical proposal on A4 at its
 * {@code RECOMMENDED_MARGIN}, exactly as the pixel gate does, and the laid-out
 * node tree is compared against
 * {@code layout-snapshots/canonical-templates/proposal/<slug>_layout.json}.
 * The comparison is exact: node identity, nesting, page ownership and
 * geometry.</p>
 *
 * <p>After a deliberate layout change, re-run with
 * {@code -Dgraphcompose.updateSnapshots=true} and commit the JSON with the
 * change. A mismatch writes the actual snapshot under
 * {@code target/visual-tests/layout-snapshots/} for diffing.</p>
 */
class ProposalPresetLayoutSnapshotTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void matchesLayoutSnapshot(String slug,
                               double margin,
                               Supplier<DocumentTemplate<ProposalDocumentSpec>> factory)
            throws Exception {
        DocumentTemplate<ProposalDocumentSpec> template = factory.get();
        float m = (float) margin;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, ProposalV2VisualParityTest.canonicalProposal());
            TemplateTestSupport.assertCanonicalSnapshot(document, slug + "_layout", "proposal");
        }
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of("modern_proposal",
                        ModernProposal.RECOMMENDED_MARGIN,
                        (Supplier<DocumentTemplate<ProposalDocumentSpec>>) ModernProposal::create));
    }
}
