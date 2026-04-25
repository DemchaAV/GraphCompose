package com.demcha.compose.document.api;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.engine.components.style.Margin;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentSessionLifecycleTest {

    @Test
    void closeShouldBeIdempotent() throws Exception {
        DocumentSession session = newSession();
        session.pageFlow(page -> page.module("Summary", m -> m.paragraph("Hello GraphCompose")));

        assertThat(session.isClosed()).isFalse();
        session.close();
        assertThat(session.isClosed()).isTrue();

        // Second close is a no-op and must not raise.
        session.close();
        assertThat(session.isClosed()).isTrue();
    }

    @Test
    void authoringMethodsShouldFailOnClosedSession() throws Exception {
        DocumentSession session = newSession();
        session.close();

        assertThatThrownBy(session::dsl)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");

        assertThatThrownBy(session::pageFlow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");

        assertThatThrownBy(() -> session.compose(dsl -> { /* no-op */ }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");

        assertThatThrownBy(() -> session.add(simpleParagraph()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");

        assertThatThrownBy(session::clear)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");
    }

    @Test
    void layoutAndRenderMethodsShouldFailOnClosedSession() throws Exception {
        DocumentSession session = newSession();
        session.add(simpleParagraph());
        session.close();

        assertThatThrownBy(session::layoutGraph)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");

        assertThatThrownBy(session::layoutSnapshot)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");

        assertThatThrownBy(session::toPdfBytes)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");

        assertThatThrownBy(() -> session.writePdf(new ByteArrayOutputStream()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");

        assertThatThrownBy(session::buildPdf)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");
    }

    @Test
    void configurationMutatorsShouldFailOnClosedSession() throws Exception {
        DocumentSession session = newSession();
        session.close();

        assertThatThrownBy(() -> session.pageSize(PDRectangle.A4))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> session.margin(DocumentInsets.of(8)))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> session.markdown(false))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> session.guideLines(true))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void emptyDocumentRenderShouldThrowDomainSpecificError() throws Exception {
        try (DocumentSession session = newSession()) {
            assertThatThrownBy(session::toPdfBytes)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("empty document")
                    .hasMessageContaining("Add at least one root");

            assertThatThrownBy(() -> session.writePdf(new ByteArrayOutputStream()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("empty document");
        }
    }

    @Test
    void emptyDocumentBuildPdfShouldThrowBeforeOpeningOutputFile() throws Exception {
        java.nio.file.Path outputPath = java.nio.file.Files.createTempFile("graphcompose-empty-", ".pdf");
        java.nio.file.Files.deleteIfExists(outputPath);
        try {
            try (DocumentSession session = GraphCompose.document(outputPath)
                    .pageSize(new PDRectangle(220, 180))
                    .margin(DocumentInsets.of(12))
                    .create()) {

                assertThatThrownBy(session::buildPdf)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("empty document");
            }
            assertThat(java.nio.file.Files.exists(outputPath)).isFalse();
        } finally {
            java.nio.file.Files.deleteIfExists(outputPath);
        }
    }

    @Test
    void clearAndAddAfterRootsRemovesEmptyDocumentError() throws Exception {
        try (DocumentSession session = newSession()) {
            session.add(simpleParagraph());
            byte[] firstPdf = session.toPdfBytes();
            assertThat(firstPdf).isNotEmpty();

            session.clear();
            assertThatThrownBy(session::toPdfBytes)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("empty document");

            session.add(simpleParagraph());
            byte[] secondPdf = session.toPdfBytes();
            assertThat(secondPdf).isNotEmpty();
        }
    }

    @Test
    void tryWithResourcesShouldCloseSessionExactlyOnce() throws Exception {
        DocumentSession reference;
        try (DocumentSession session = newSession()) {
            session.pageFlow(page -> page.module("Summary", m -> m.paragraph("Hello GraphCompose")));
            session.toPdfBytes();
            reference = session;
        }
        assertThat(reference.isClosed()).isTrue();
        // Calling close after try-with-resources is still a no-op.
        reference.close();
        assertThat(reference.isClosed()).isTrue();
    }

    private DocumentSession newSession() {
        return GraphCompose.document()
                .pageSize(new PDRectangle(220, 180))
                .margin(DocumentInsets.of(12))
                .create();
    }

    private ParagraphNode simpleParagraph() {
        return new ParagraphNode(
                "Paragraph",
                "Lifecycle paragraph",
                DocumentTextStyle.DEFAULT,
                TextAlign.LEFT,
                2.0,
                DocumentInsets.of(4),
                DocumentInsets.zero());
    }
}
