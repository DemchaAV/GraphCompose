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
 * Structural layout-snapshot gate for the layered proposal presets —
 * the sibling of {@link ProposalV2VisualParityTest}.
 *
 * <p>Each preset composes the canonical {@link ProposalDocumentSpec}
 * from {@link ProposalPresetFixtures} on A4 at the preset's
 * {@code RECOMMENDED_MARGIN}, and the resulting post-layout node tree
 * is compared against a checked-in baseline JSON under
 * {@code src/test/resources/layout-snapshots/canonical-templates/proposal/}.
 * This is the multi-page member of the canonical set, so it also pins
 * where the engine breaks the proposal across pages.</p>
 *
 * <p><strong>Re-blessing baselines</strong> — after a deliberate
 * layout change, re-run with
 * {@code -Dgraphcompose.updateSnapshots=true} and commit the updated
 * JSON as part of the same change.</p>
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
            template.compose(document, ProposalPresetFixtures.canonicalProposal());
            TemplateTestSupport.assertCanonicalSnapshot(document, slug, "proposal");
        }
    }

    private static Stream<Arguments> presets() {
        return ProposalPresetFixtures.presets();
    }
}
