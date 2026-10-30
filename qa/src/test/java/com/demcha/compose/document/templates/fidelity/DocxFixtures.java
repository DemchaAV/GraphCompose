package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.document.backend.semantic.docx.DocxExportReport;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the fidelity guards that read a DOCX back need in common: a package of XML parts written
 * by hand, so a reader can be held to finding what it is to judge in a file put together on
 * purpose, and the check that an export was made from its document's layout.
 *
 * <p>A guard's own fixtures stay with the guard — a page reference's fields, a drawing's anchor.
 * Here are the parts of a DOCX itself, which carry nothing of either guard's subject: the
 * namespaces every hand-written part declares, the three part kinds, a paragraph, and the zip.</p>
 */
final class DocxFixtures {

    /** The namespaces a hand-written part declares, enough for the parts written here. */
    static final String NAMESPACES =
            "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" "
            + "xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" "
            + "xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" "
            + "xmlns:wps=\"http://schemas.microsoft.com/office/word/2010/wordprocessingShape\"";

    private DocxFixtures() {
    }

    /** A body part holding the given content. */
    static String body(String content) {
        return "<w:document " + NAMESPACES + "><w:body>" + content + "</w:body></w:document>";
    }

    /** A header part holding the given content. */
    static String header(String content) {
        return "<w:hdr " + NAMESPACES + ">" + content + "</w:hdr>";
    }

    /** A footer part holding the given content. */
    static String footer(String content) {
        return "<w:ftr " + NAMESPACES + ">" + content + "</w:ftr>";
    }

    /** A paragraph holding the given content. */
    static String paragraph(String content) {
        return "<w:p>" + content + "</w:p>";
    }

    /** A package of the given parts, enough for a reader that reads XML parts alone. */
    static byte[] docx(Map<String, String> parts) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> part : new TreeMap<>(parts).entrySet()) {
                zip.putNextEntry(new ZipEntry(part.getKey()));
                zip.write(part.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    /**
     * The export was made from the document's layout: where the layout fails, the export writes
     * the document without it and says so under this subject, and everything the layout would
     * have given — where a line sits, where a drawing is placed from — is the editor's guess
     * instead. A guard reading such a file would be reading the fallback, not the export.
     */
    static void assertLaidOut(String document, DocxExportReport report) {
        assertThat(report.bySubject()).as("%s: exported from its layout", document)
                .doesNotContainKey("measured geometry");
    }
}
