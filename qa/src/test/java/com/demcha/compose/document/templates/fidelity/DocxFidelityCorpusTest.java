package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.templates.coverletter.presets.CoverLetterDocxCorpus;
import com.demcha.compose.document.templates.cv.presets.CvDocxCorpus;
import com.demcha.compose.document.templates.invoice.presets.InvoiceDocxCorpus;
import com.demcha.compose.document.templates.proposal.presets.ProposalDocxCorpus;
import com.demcha.compose.document.templates.receipt.presets.ReceiptDocxCorpus;
import com.demcha.compose.document.templates.rota.presets.RotaDocxCorpus;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every template preset's DOCX, set by an editor, stands no further from the engine's PDF than
 * its baseline holds it.
 *
 * <p>The engine draws each corpus document to PDF and exports it to DOCX; LibreOffice converts
 * the DOCX to PDF; each document is measured line by line against the page
 * ({@link FidelityMeasurement}) and held to the baseline for this editor and operating system
 * ({@link FidelityBaseline}). A change to the export that sets any document further from the
 * page fails here, wherever the document is.</p>
 *
 * <p>It runs only when asked for, with {@code -Dgraphcompose.docxFidelity=libreoffice}: it
 * needs LibreOffice and takes a few minutes. Asked for and without LibreOffice, it fails rather
 * than passing unrun. {@code -Dgraphcompose.docxFidelity.update=true} writes what it measured as
 * the baseline, for a change that moves documents nearer the page; the baseline's diff then
 * shows the reviewer which documents moved. The documents, their PDFs and the report are left
 * in {@code target/docx-fidelity}.</p>
 */
class DocxFidelityCorpusTest {

    private static final String EDITOR = "libreoffice";

    @Test
    void everyDocumentStandsNoFurtherFromThePageThanItsBaseline() throws Exception {
        Assumptions.assumeTrue(EDITOR.equals(System.getProperty("graphcompose.docxFidelity")),
                "NOT_RUN: the DOCX fidelity corpus runs with -Dgraphcompose.docxFidelity=libreoffice");
        LibreOfficeConverter converter = LibreOfficeConverter.find().orElseThrow(() -> new IllegalStateException(
                "the DOCX fidelity corpus was asked for, and no LibreOffice was found: set -Dgraphcompose.soffice"));

        Path work = Path.of("target", "docx-fidelity").toAbsolutePath();
        Path engine = Files.createDirectories(work.resolve("engine"));
        Path editor = work.resolve(EDITOR);
        List<DocxCorpusDocument> corpus = corpus();
        List<Path> docx = new ArrayList<>();
        for (DocxCorpusDocument document : corpus) {
            docx.add(export(document, engine));
        }
        converter.convert(docx, editor, work);

        List<FidelityMeasurement> measurements = new ArrayList<>();
        for (DocxCorpusDocument document : corpus) {
            Path converted = editor.resolve(document.stem() + ".pdf");
            assertThat(converted).as("LibreOffice's PDF of %s", document.stem()).exists();
            measurements.add(FidelityMeasurement.of(document.stem(),
                    engine.resolve(document.stem() + ".pdf"), converted));
        }
        String note = EDITOR + " on " + LibreOfficeConverter.platform();
        FidelityBaseline.write(work.resolve("measured-" + EDITOR + ".tsv"), note, measurements);

        Path baseline = Path.of("src", "test", "resources", "docx-fidelity",
                EDITOR + "-" + LibreOfficeConverter.platform() + ".tsv");
        if (Boolean.getBoolean("graphcompose.docxFidelity.update")) {
            FidelityBaseline.write(baseline, note, measurements);
            return;
        }
        assertThat(FidelityBaseline.read(baseline).regressions(measurements))
                .as("documents set further from the page than %s holds them (measured: %s)",
                        baseline, work.resolve("measured-" + EDITOR + ".tsv"))
                .isEmpty();
    }

    @Test
    void everyCorpusDocumentHasAStemOfItsOwn() {
        Set<String> stems = new HashSet<>();
        for (DocxCorpusDocument document : corpus()) {
            assertThat(stems.add(document.stem())).as("%s named once", document.stem()).isTrue();
        }
        assertThat(stems).hasSizeGreaterThanOrEqualTo(60);
    }

    /** Every family's presets. */
    static List<DocxCorpusDocument> corpus() {
        List<DocxCorpusDocument> corpus = new ArrayList<>();
        corpus.addAll(CvDocxCorpus.documents());
        corpus.addAll(CoverLetterDocxCorpus.documents());
        corpus.addAll(InvoiceDocxCorpus.documents());
        corpus.addAll(ProposalDocxCorpus.documents());
        corpus.addAll(ReceiptDocxCorpus.documents());
        corpus.addAll(RotaDocxCorpus.documents());
        return corpus;
    }

    /** Writes a document's PDF and its DOCX; returns the DOCX. */
    private static Path export(DocxCorpusDocument document, Path dir) throws Exception {
        var builder = GraphCompose.document();
        if (document.margin() >= 0) {
            float margin = (float) document.margin();
            builder.pageSize(DocumentPageSize.A4).margin(margin, margin, margin, margin);
        }
        try (DocumentSession session = builder.create()) {
            document.compose().accept(session);
            Files.write(dir.resolve(document.stem() + ".pdf"), session.toPdfBytes());
            Path docx = dir.resolve(document.stem() + ".docx");
            Files.write(docx, session.export(DocxSemanticBackend.builder().deterministic(true).build()));
            return docx;
        }
    }
}
