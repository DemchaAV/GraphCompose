package com.demcha.compose.document.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.exceptions.DocumentRenderingException;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

class DocumentSessionRenderingExceptionTest {

    @Test
    void writePdfWrapsCheckedIoExceptionAsDocumentRenderingException() {
        try (DocumentSession session = GraphCompose.document().create()) {
            session.compose(dsl -> dsl.pageFlow(flow -> flow.addText("body")));

            OutputStream failing = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    throw new IOException("simulated IO failure");
                }

                @Override
                public void write(byte[] buf, int off, int len) throws IOException {
                    throw new IOException("simulated IO failure");
                }
            };

            assertThatThrownBy(() -> session.writePdf(failing))
                    .isInstanceOf(DocumentRenderingException.class)
                    .hasMessageContaining("Failed to write PDF to stream")
                    .hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    void buildPdfWithoutDefaultPathThrowsIllegalStateNotRenderingException() {
        try (DocumentSession session = GraphCompose.document().create()) {
            session.compose(dsl -> dsl.pageFlow(flow -> flow.addText("body")));

            assertThatThrownBy(session::buildPdf)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No default output file");
        }
    }

    @Test
    void documentRenderingExceptionPreservesMessageAndCause() {
        IOException cause = new IOException("underlying boom");
        DocumentRenderingException ex = new DocumentRenderingException("wrapper message", cause);

        assertThat(ex).hasMessage("wrapper message");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
