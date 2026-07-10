package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.dsl.DocumentDsl;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scale smoke test: a ~1000-page document must lay out and render without
 * blowing up on time or memory. It guards against a pagination or measurement
 * regression whose cost is only visible at scale (e.g. an accidental
 * quadratic pass, or unbounded per-page retained state) — the small-document
 * suites would stay green through such a regression while a real document
 * grinds to a halt.
 *
 * <p>Deterministic page count: one paragraph per page, separated by explicit
 * page breaks, so the assertion is a structural bound rather than a
 * metric-sensitive estimate.</p>
 */
class DocumentSessionScaleSmokeTest {

    private static final int PAGES = 1000;

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void thousandPageDocumentLaysOutAndRenders() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(400, 400)
                .margin(DocumentInsets.of(20))
                .create()) {

            DocumentDsl dsl = session.dsl();
            dsl.pageFlow()
                    .name("ScaleFlow")
                    .module("Body", module -> {
                        for (int i = 0; i < PAGES; i++) {
                            module.paragraph("Page " + i + " — the quick brown fox jumps over the lazy dog.");
                            if (i < PAGES - 1) {
                                module.pageBreak("break-" + i);
                            }
                        }
                    })
                    .build();

            LayoutGraph graph = session.layoutGraph();
            // One paragraph per page: the count must reach ~PAGES and must not
            // run away (which would signal a pagination bug at scale).
            assertThat(graph.totalPages())
                    .as("a page-break-per-paragraph document should paginate to ~%d pages", PAGES)
                    .isBetween(PAGES, PAGES + 2);
            assertThat(graph.nodes())
                    .as("every paragraph should be placed")
                    .hasSizeGreaterThanOrEqualTo(PAGES);

            byte[] pdf = session.toPdfBytes();
            assertThat(pdf).as("the large document must render to non-empty PDF bytes").isNotEmpty();
            assertThat(new String(pdf, 0, 4)).as("output must carry the %%PDF magic").isEqualTo("%PDF");
        }
    }
}
