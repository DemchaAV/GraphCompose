package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.probe.EditableExportFixtures;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The same document exports to the same bytes, when asked to.
 *
 * <p>Three things made two exports of one document differ, measured on the probe corpus: the
 * package's created / modified dates, every zip entry's timestamp, and the obfuscation key of
 * each embedded font, which was a random GUID. The first two are the PDF and PPTX backends'
 * problem too, and they answer it with an opt-in {@code deterministic(...)}; this export
 * takes the same contract. The third is DOCX's alone, and needs no option: the key only has
 * to be a GUID, so it is derived from the font.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxDeterminismTest {

    private static final Instant PINNED = Instant.parse("2020-02-02T02:02:02Z");

    @Test
    void twoDeterministicExportsOfOneDocumentAreTheSameBytes() throws Exception {
        byte[] first = exportCorpus(DocxSemanticBackend.builder().deterministic(true).build());
        // Past the zip format's two-second clock, so an unpinned entry time would differ.
        Thread.sleep(2_100);
        byte[] second = exportCorpus(DocxSemanticBackend.builder().deterministic(true).build());

        assertThat(second).isEqualTo(first);
    }

    @Test
    void everyClockInThePackageIsPinnedToTheInstantAsked() throws Exception {
        byte[] docx = exportCorpus(DocxSemanticBackend.builder().deterministic(PINNED).build());

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getProperties().getCoreProperties().getCreated().toInstant()).isEqualTo(PINNED);
            assertThat(document.getProperties().getCoreProperties().getModified().toInstant()).isEqualTo(PINNED);
        }
        LocalDateTime pinnedLocal = LocalDateTime.ofInstant(PINNED, ZoneOffset.UTC);
        assertThat(entryTimes(docx))
                .as("every zip entry, not only the ones POI happens to stamp")
                .isNotEmpty()
                .allSatisfy(time -> assertThat(time).isEqualTo(pinnedLocal));
    }

    @Test
    void anEmbeddedFontsKeyIsTheSameEveryTimeWithoutAnyOption() throws Exception {
        // A random key made every export different bytes and bought nothing: it is not a
        // secret, and Word does not need it to differ between documents.
        Map<String, byte[]> first = parts(exportCorpus(new DocxSemanticBackend()));
        Map<String, byte[]> second = parts(exportCorpus(new DocxSemanticBackend()));

        assertThat(first).containsKey("word/fontTable.xml");
        assertThat(second.get("word/fontTable.xml")).isEqualTo(first.get("word/fontTable.xml"));
        assertThat(second.get("word/fonts/font1.odttf")).isEqualTo(first.get("word/fonts/font1.odttf"));
    }

    @Test
    void withoutTheOptionTheDocumentKeepsItsLiveDates() throws Exception {
        // Deterministic output is opt-in, as it is for PDF and PPTX: a document's creation
        // date is meaningful metadata, and pinning it by default would put a wrong one in
        // every file.
        Instant before = Instant.now().minusSeconds(5);
        byte[] docx = exportCorpus(new DocxSemanticBackend());

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getProperties().getCoreProperties().getCreated().toInstant()).isAfter(before);
        }
    }

    @Test
    void theBuilderStillHandsTheReportOver() throws Exception {
        AtomicReference<DocxExportReport> received = new AtomicReference<>();
        exportCorpus(DocxSemanticBackend.builder().reportSink(received::set).deterministic(true).build());

        assertThat(received.get()).isNotNull();
    }

    private static byte[] exportCorpus(DocxSemanticBackend backend) throws Exception {
        try (DocumentSession session = EditableExportFixtures.mixedTwoPager(Path.of("target", "determinism.pdf"))) {
            return session.export(backend);
        }
    }

    private static List<LocalDateTime> entryTimes(byte[] docx) throws Exception {
        List<LocalDateTime> times = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                times.add(entry.getTimeLocal());
            }
        }
        return times;
    }

    private static Map<String, byte[]> parts(byte[] docx) throws Exception {
        Map<String, byte[]> parts = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                parts.put(entry.getName(), zip.readAllBytes());
            }
        }
        return parts;
    }
}
