package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.SemanticBackendProviders;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reaching the Word export the way a caller reaches a PDF render.
 *
 * <p>Until now the only way in was to construct {@code DocxSemanticBackend}, which means
 * importing this artifact in the code that builds the document and carrying it wherever the
 * document is built. A render backend has not needed that since 2.0 — the artifact on the
 * classpath is enough — and these pin that the same now holds here.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxSessionExportTest {

    @Test
    void theProviderIsFoundOnTheClasspath() {
        assertThat(SemanticBackendProviders.forFormat("docx"))
                .isInstanceOf(DocxBackendProvider.class);
        assertThat(SemanticBackendProviders.forFormat("DOCX"))
                .as("the format is a key, not a spelling")
                .isInstanceOf(DocxBackendProvider.class);
    }

    @Test
    void aProviderCreatesAFreshBackendPerExport() {
        // A semantic backend holds the state of the export it is running, so two exports
        // sharing one instance would write into each other.
        DocxBackendProvider provider = new DocxBackendProvider();

        assertThat(provider.create()).isNotSameAs(provider.create());
    }

    @Test
    void bytesFileAndStreamAllProduceTheSameDocument() throws Exception {
        byte[] fromBytes;
        byte[] fromStream;
        byte[] fromFile;
        Path file = Files.createTempFile("session-export", ".docx");
        try (DocumentSession session = session(page -> page.addParagraph(p -> p.text("Exported")))) {
            fromBytes = session.toDocxBytes();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            session.writeDocx(stream);
            fromStream = stream.toByteArray();
            session.buildDocx(file);
            fromFile = Files.readAllBytes(file);
        } finally {
            Files.deleteIfExists(file);
        }

        // Not compared byte for byte: a .docx carries creation timestamps, so three
        // exports of one document differ in the package while holding the same document.
        for (byte[] export : new byte[][] {fromBytes, fromStream, fromFile}) {
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(export))) {
                assertThat(document.getParagraphs())
                        .extracting(p -> p.getText())
                        .contains("Exported");
            }
        }
    }

    @Test
    void theStreamIsLeftOpenForItsOwner() throws Exception {
        // The caller owns the stream: an HTTP response or an upload is not ours to close.
        CloseCountingStream stream = new CloseCountingStream();
        try (DocumentSession session = session(page -> page.addParagraph(p -> p.text("Body")))) {
            session.writeDocx(stream);
        }

        assertThat(stream.closes).isZero();
        assertThat(stream.size()).isPositive();
    }

    @Test
    void aFailedExportLeavesTheOldFileAlone() throws Exception {
        // Written atomically, like buildPdf: a document that fails half-way must not
        // replace a good file with an unreadable one.
        Path file = Files.createTempFile("kept", ".docx");
        Files.writeString(file, "the previous export");
        try (DocumentSession session = session(page -> page.addParagraph(p -> p.text("Body")))) {
            session.close();
            assertThatThrownBy(() -> session.buildDocx(file)).isInstanceOf(IllegalStateException.class);
            assertThat(Files.readString(file)).isEqualTo("the previous export");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void anUnknownFormatNamesWhatToAdd() {
        assertThatThrownBy(() -> SemanticBackendProviders.forFormat("odt"))
                .hasMessageContaining("odt")
                .hasMessageContaining("classpath");
    }

    private static DocumentSession session(Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> content) {
        DocumentSession session = GraphCompose.document()
                .pageSize(400, 600)
                .margin(DocumentInsets.of(20))
                .create();
        session.pageFlow(content::accept);
        return session;
    }

    /** Counts closes so the contract can be asserted rather than assumed. */
    private static final class CloseCountingStream extends OutputStream {

        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
        private int closes;

        @Override
        public void write(int b) {
            delegate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            delegate.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            closes++;
            super.close();
        }

        int size() {
            return delegate.size();
        }
    }
}
