package com.demcha.smoke;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 1 — the drop-in {@code graph-compose} wrapper renders a PDF out of
 * the box, exactly as a 1.x caller expects after upgrading to 2.0. The wrapper
 * pulls {@code graph-compose-core} + {@code graph-compose-render-pdf}
 * transitively, so a successful render is positive proof both were resolved.
 */
class WrapperRendersPdfTest {

    @Test
    void graphComposeWrapperRendersPdfOutOfTheBox() throws Exception {
        Path out = Files.createTempFile("gc-smoke-wrapper", ".pdf");
        try (DocumentSession document = GraphCompose.document(out)
                .pageSize(DocumentPageSize.A4)
                .margin(36f, 36f, 36f, 36f)
                .create()) {
            document.add(document.dsl().paragraph()
                    .text("graph-compose 2.0.0 renders PDF out of the box.")
                    .build());
            document.buildPdf();
        }

        assertThat(Files.size(out)).isGreaterThan(0L);
        byte[] head = Arrays.copyOf(Files.readAllBytes(out), 5);
        assertThat(new String(head)).isEqualTo("%PDF-");
    }
}
