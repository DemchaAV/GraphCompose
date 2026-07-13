package com.demcha.smoke;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.exceptions.MissingBackendException;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Scenario 2 — a lean {@code graph-compose-core} consumer with no render
 * backend on the classpath. Asking it to build a PDF must fail with a
 * {@link MissingBackendException} whose message names the artifact to add
 * ({@code graph-compose-render-pdf}), rather than an opaque NPE or ISE. The
 * dependency-tree leanness is asserted separately by the enforcer rule in the
 * pom.
 */
class CoreOnlyMissingBackendTest {

    @Test
    void coreOnlyRenderThrowsMissingBackendNamingRenderPdf() throws Exception {
        Path out = Files.createTempFile("gc-smoke-core-only", ".pdf");
        assertThatThrownBy(() -> {
            try (DocumentSession document = GraphCompose.document(out)
                    .pageSize(DocumentPageSize.A4)
                    .margin(36f, 36f, 36f, 36f)
                    .create()) {
                document.add(document.dsl().paragraph()
                        .text("no backend on the classpath")
                        .build());
                document.buildPdf();
            }
        })
                .isInstanceOf(MissingBackendException.class)
                .hasMessageContaining("graph-compose-render-pdf");
    }
}
