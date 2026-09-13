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
 * Structural layout-snapshot gate for the layered CV presets — the sibling of
 * {@link CvV2VisualParityTest}, reading the same {@link CvPresetFixtures}.
 *
 * <p>Each preset composes the canonical {@link CvDocument} on A4 at its
 * {@code RECOMMENDED_MARGIN}, exactly as the pixel gate does, and the laid-out
 * node tree is compared against
 * {@code layout-snapshots/canonical-templates/cv-v2/<slug>_layout.json}. The
 * comparison is exact: node identity, nesting, page ownership and geometry.</p>
 *
 * <p>After a deliberate layout change, re-run with
 * {@code -Dgraphcompose.updateSnapshots=true} and commit the JSON with the
 * change. A mismatch writes the actual snapshot under
 * {@code target/visual-tests/layout-snapshots/} for diffing.</p>
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
            TemplateTestSupport.assertCanonicalSnapshot(document, slug + "_layout", "cv-v2");
        }
    }

    private static Stream<Arguments> presets() {
        return CvPresetFixtures.presets();
    }
}
