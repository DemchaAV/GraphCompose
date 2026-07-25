package com.demcha.smoke;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 8 — {@code graph-compose-core} + {@code graph-compose-render-docx}
 * resolved from Maven Central produces a real Word document.
 *
 * <p>Unlike the fixed-layout backends, the semantic exporter is named by the
 * caller rather than discovered through the ServiceLoader, so this scenario also
 * proves the type is on the compile classpath of a consumer who installed only
 * these two coordinates.</p>
 *
 * <p>Inspected with {@link ZipInputStream} rather than Apache POI, so the
 * scenario asserts against the published artifacts alone.</p>
 */
class CoreRenderDocxTest {

    @Test
    void coreWithRenderDocxProducesAWordDocument() throws Exception {
        byte[] docx;
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(36f, 36f, 36f, 36f)
                .create()) {
            document.add(document.dsl().paragraph()
                    .text("graph-compose-core + graph-compose-render-docx exports semantic Word content.")
                    .build());
            docx = document.export(new DocxSemanticBackend());
        }

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
