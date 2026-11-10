package com.demcha.smoke;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 8 — {@code graph-compose-core} + {@code graph-compose-render-docx}
 * resolved from Maven Central produce a real Word document, along both routes a
 * consumer takes.
 *
 * <p>The session's own calls — {@link DocumentSession#toDocxBytes()},
 * {@link DocumentSession#writeDocx(java.io.OutputStream)} and
 * {@link DocumentSession#buildDocx(Path)} — name no backend: they find the one
 * render-docx registers through the {@link java.util.ServiceLoader}. They are the
 * route the render-docx README leads with, and they work only if the published jar
 * carries its service registration. These calls are Experimental ({@code @Beta}) in
 * 2.5.</p>
 *
 * <p>{@code export(new DocxSemanticBackend())} is the Stable route, and proves the
 * exporter type is on the compile classpath of a consumer who installed only these
 * two coordinates.</p>
 *
 * <p>Neither route has the PDF backend declared here: opening a session needs the
 * font metrics render-pdf publishes, and render-docx brings it at compile scope. A
 * session that opens is the proof.</p>
 *
 * <p>Inspected with {@link ZipInputStream} rather than Apache POI, so the
 * scenario asserts against the published artifacts alone.</p>
 */
class CoreRenderDocxTest {

    private static final String TEXT =
            "graph-compose-core + graph-compose-render-docx exports semantic Word content.";

    @Test
    void toDocxBytesFindsTheBackendAndProducesAWordDocument() throws Exception {
        byte[] docx;
        try (DocumentSession document = newDocument()) {
            docx = document.toDocxBytes();
        }

        assertIsAWordDocumentCarryingTheText(docx);
    }

    @Test
    void writeDocxFindsTheBackendAndStreamsAWordDocument() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DocumentSession document = newDocument()) {
            document.writeDocx(out);
        }

        assertIsAWordDocumentCarryingTheText(out.toByteArray());
    }

    @Test
    void buildDocxFindsTheBackendAndWritesAWordDocument(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("smoke.docx");
        try (DocumentSession document = newDocument()) {
            document.buildDocx(file);
        }

        assertThat(file).isRegularFile();
        assertIsAWordDocumentCarryingTheText(Files.readAllBytes(file));
    }

    @Test
    void theNamedStableBackendProducesAWordDocument() throws Exception {
        byte[] docx;
        try (DocumentSession document = newDocument()) {
            docx = document.export(new DocxSemanticBackend());
        }

        assertIsAWordDocumentCarryingTheText(docx);
    }

    private static DocumentSession newDocument() {
        DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(36f, 36f, 36f, 36f)
                .create();
        document.add(document.dsl().paragraph().text(TEXT).build());
        return document;
    }

    private static void assertIsAWordDocumentCarryingTheText(byte[] docx) throws Exception {
        assertThat(docx).isNotEmpty();
        assertThat(new String(docx, 0, 2, StandardCharsets.US_ASCII))
                .describedAs("a .docx is an OPC package, so it must start with the ZIP signature")
                .isEqualTo("PK");

        Map<String, byte[]> parts = unzip(docx);
        assertThat(parts).containsKey("[Content_Types].xml");
        assertThat(parts)
                .describedAs("the main document part must be present")
                .containsKey("word/document.xml");

        String body = new String(parts.get("word/document.xml"), StandardCharsets.UTF_8);
        assertThat(body)
                .describedAs("the paragraph must land as a Word text run")
                .contains("<w:t")
                .contains("graph-compose-render-docx");
    }

    private static Map<String, byte[]> unzip(byte[] archive) throws Exception {
        Map<String, byte[]> parts = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                zip.transferTo(bytes);
                parts.put(entry.getName(), bytes.toByteArray());
            }
        }
        return parts;
    }
}
