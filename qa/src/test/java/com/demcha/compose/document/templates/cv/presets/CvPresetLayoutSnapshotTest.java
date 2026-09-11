package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Structural layout-snapshot gate for the layered CV presets — the
 * sibling of {@link CvV2VisualParityTest}.
 *
 * <p>Each preset composes the canonical {@link CvDocument} from
 * {@link CvPresetFixtures} on full A4 at the preset's
 * {@code RECOMMENDED_MARGIN}, and the resulting post-layout node tree
 * is compared against a checked-in baseline JSON under
 * {@code src/test/resources/layout-snapshots/canonical-templates/cv-v2/}.
 * The comparison is exact, so it pins node identity, parent/child
 * nesting, page assignment, and computed geometry — the drift the
 * loose pixel budget cannot see.</p>
 *
 * <p><strong>Re-blessing baselines</strong> — after a deliberate
 * layout change, re-run with
 * {@code -Dgraphcompose.updateSnapshots=true} and commit the updated
 * JSON as part of the same change. On mismatch the actual snapshot is
 * written under {@code target/visual-tests/layout-snapshots/} so it
 * can be diffed against the baseline.</p>
 */
class CvPresetLayoutSnapshotTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("presets")
    void matchesLayoutSnapshot(String slug,
                               double margin,
                               Supplier<DocumentTemplate<CvDocument>> factory)
            throws Exception {
        DocumentTemplate<CvDocument> template = factory.get();
        float m = (float) margin;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(m, m, m, m)
                .create()) {
            template.compose(document, CvPresetFixtures.canonicalDocument());
            TemplateTestSupport.assertCanonicalSnapshot(document, slug, "cv-v2");
        }
    }

    private static Stream<Arguments> presets() {
        return CvPresetFixtures.presets();
    }
}
