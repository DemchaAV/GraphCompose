package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.probe.EditableExportFixtures;
import com.demcha.compose.document.chart.ChartData;
import com.demcha.compose.document.chart.ChartSpec;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One export's state stays in that export.
 *
 * <p>The backend keeps what an export is in the middle of — the spacing still owed, the
 * bookmark names handed out, the list definitions written, the kinds already warned about —
 * in its own fields. A backend used for a second document has to start that document as a
 * fresh one would, and sessions exporting at the same moment must not see each other at all.
 * Deterministic output is what makes both checkable to the byte: the same document has to be
 * the same file however many exports came before it and however many run beside it.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxExportIsolationTest {

    @Test
    void aBackendUsedBeforeExportsTheSameBytesAsAFreshOne() throws Exception {
        byte[] freshCorpus = exportCorpus(deterministic());
        byte[] freshBusy = exportBusyDocument(deterministic());

        // Everything the busy document does leaves state behind if it is not reset: an
        // anchor claims a bookmark name, a list writes a numbering definition, a chart and a
        // dropped line are warned about once. Each document is exported again afterwards,
        // because state only shows in a document that uses it — the corpus has no bookmark,
        // so a leaked name would never reach its bytes.
        DocxSemanticBackend reused = deterministic();
        exportCorpus(reused);
        exportBusyDocument(reused);
        byte[] busyAgain = exportBusyDocument(reused);
        byte[] corpusAgain = exportCorpus(reused);

        assertThat(busyAgain).isEqualTo(freshBusy);
        assertThat(corpusAgain).isEqualTo(freshCorpus);
    }

    @Test
    void anExportThatFailsHalfwayLeavesNothingForTheNextOne() throws Exception {
        // A service that catches the failure and exports the next document on the same
        // backend: the half-written one stopped with spacing owed, a paragraph remembered
        // and a panel's paint pushed.
        byte[] fresh = exportCorpus(deterministic());

        DocxSemanticBackend reused = deterministic();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> exportFailingDocument(reused))
                .isInstanceOf(Exception.class);
        byte[] after = exportCorpus(reused);

        assertThat(after).isEqualTo(fresh);
    }

    @Test
    void theReportOfAReusedBackendIsTheReportOfAFreshOne() throws Exception {
        AtomicReference<DocxExportReport> freshReport = new AtomicReference<>();
        exportBusyDocument(DocxSemanticBackend.builder().reportSink(freshReport::set).build());

        List<DocxExportReport> reports = new ArrayList<>();
        DocxSemanticBackend reused = DocxSemanticBackend.builder().reportSink(reports::add).build();
        exportBusyDocument(reused);
        exportBusyDocument(reused);

        assertThat(freshReport.get().notes()).isNotEmpty();
        assertThat(reports).hasSize(2);
        assertThat(reports.get(1).notes())
                .as("the second export reports what it lost, not what the first one did")
                .isEqualTo(freshReport.get().notes());
    }

    @Test
    void sessionsExportingAtTheSameMomentDoNotSeeEachOther() throws Exception {
        byte[] alone = exportCorpus(deterministic());

        int sessions = 8;
        ExecutorService pool = Executors.newFixedThreadPool(sessions);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<byte[]>> results = new ArrayList<>();
            for (int index = 0; index < sessions; index++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return exportCorpus(deterministic());
                }));
            }
            start.countDown();
            for (Future<byte[]> result : results) {
                assertThat(result.get(2, TimeUnit.MINUTES))
                        .as("an export that ran beside seven others")
                        .isEqualTo(alone);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static DocxSemanticBackend deterministic() {
        return DocxSemanticBackend.builder().deterministic(true).build();
    }

    private static byte[] exportCorpus(DocxSemanticBackend backend) throws Exception {
        try (DocumentSession session = EditableExportFixtures.mixedTwoPager(Path.of("target", "isolation.pdf"))) {
            return session.export(backend);
        }
    }

    private static void exportFailingDocument(DocxSemanticBackend backend) throws Exception {
        // Bytes no image reader accepts: the image writer does not catch the failure, so the
        // export stops inside a card, with the paragraph above still owed the space below it
        // and an inner section's top edge still waiting for a paragraph that never came.
        byte[] notAnImage = {1, 2, 3, 4, 5, 6, 7, 8};
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page
                    .addSection("Card", card -> card
                            .fillColor(DocumentColor.rgb(238, 243, 249))
                            .addParagraph(p -> p.text("Before").margin(DocumentInsets.bottom(12)))
                            .addSection("Inner", inner -> inner
                                    .padding(DocumentInsets.top(10))
                                    .addImage(image -> image
                                            .source(com.demcha.compose.document.image.DocumentImageData.fromBytes(notAnImage))
                                            .width(40)
                                            .height(20)))));
            session.export(backend);
        }
    }

    private static byte[] exportBusyDocument(DocxSemanticBackend backend) throws Exception {
        ChartData data = ChartData.builder()
                .categories("Q1", "Q2")
                .series("2025", 12.4, 15.1)
                .build();
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 600)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow(page -> page
                    .addParagraph(p -> p.text("Chapter").bookmark(new DocumentBookmarkOptions("Chapter", 0)))
                    .addParagraph(p -> p.text("Terms").anchor("terms"))
                    .addList(list -> list.bullet().items("One", "Two"))
                    .addSection("Card", card -> card
                            .fillColor(DocumentColor.rgb(238, 243, 249))
                            .cornerRadius(DocumentCornerRadius.of(8))
                            .addParagraph(p -> p.text("Inside")))
                    .addLine(line -> line.name("Divider").thickness(1))
                    .chart(ChartSpec.bar().data(data).build()));
            return session.export(backend);
        }
    }
}
