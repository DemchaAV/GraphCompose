package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.exceptions.MissingBackendException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The other half of the {@code MissingBackendException} contract: the one case that
 * really does surface at the output call.
 *
 * <p>On a lean core the session never opens — {@code create()} resolves the
 * font-metrics provider first — so no test on a backend-free classpath can reach a
 * render call. This module is where the case becomes reachable: the PDF backend is
 * present, so measurement resolves and the session opens, while {@code render-pptx}
 * is absent, so asking for a deck fails at {@code buildPptx(...)} and names the
 * artifact to add.</p>
 *
 * <p>The engine-side test asserts on {@code BackendProviders.fixedLayout("pptx")}
 * directly. That pins the resolver, not the path a caller takes to it: a convenience
 * method that stopped routing through the resolver, or started resolving eagerly,
 * would leave that test green. This one goes through the public API.</p>
 *
 * <p>Note the ordering the assertion depends on: a document with no roots fails
 * {@code ensureRenderable()} with an {@code IllegalStateException} <em>before</em>
 * any backend is looked up, so the page content below is not decoration — without it
 * this test would pass for the wrong reason.</p>
 */
class MissingPptxBackendContractTest {

    @Test
    void buildingADeckWithoutThePptxBackendThrowsNamingRenderPptx(@TempDir Path directory) throws Exception {
        Path deck = directory.resolve("deck.pptx");

        try (DocumentSession session = GraphCompose.document()
                .pageSize(200, 200)
                .create()) {
            session.pageFlow(page -> page.module("m", module -> module.paragraph("hi")));

            assertThatThrownBy(() -> session.buildPptx(deck))
                    .isInstanceOf(MissingBackendException.class)
                    .hasMessageContaining("graph-compose-render-pptx");
        }
    }
}
