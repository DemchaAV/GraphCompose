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
 * <p>The engine draws each corpus document to PDF and exports it to DOCX; an editor converts the
 * DOCX to PDF; each document is measured line by line against the page
 * ({@link FidelityMeasurement}) and held to the baseline for this editor and operating system
 * ({@link FidelityBaseline}). A change to the export that sets any document further from the
 * page fails here, wherever the document is.</p>
 *
 * <p>It runs only when asked for, with {@code -Dgraphcompose.docxFidelity}, and takes a few
 * minutes; a value it does not know fails. {@code libreoffice} exports, converts with
 * LibreOffice and measures, and fails rather than passing unrun without LibreOffice. Microsoft
 * Word, the editor the export answers to, converts outside the build, around two runs of this
 * test ({@code scripts/docx-visual/word-fidelity.ps1}): {@code export} writes the documents,
 * Word converts them, and {@code word} measures Word's PDFs against {@code word-windows.tsv} —
 * each only when the DOCX Word converted, by the SHA-256 its conversion records, is the one
 * this tree exports ({@link WordConversion}). {@code -Dgraphcompose.docxFidelity.update=true}
 * writes what it measured as the baseline, for a change that moves documents nearer the page;
 * the baseline's diff then shows the reviewer which documents moved. The documents, their PDFs
 * and the report are left in {@code target/docx-fidelity}.</p>
 */
class DocxFidelityCorpusTest {

    private static final String LIBREOFFICE = "libreoffice";
    private static final String WORD = "word";
    private static final String EXPORT = "export";

    @Test
    void everyDocumentStandsNoFurtherFromThePageThanItsBaseline() throws Exception {
        String mode = System.getProperty("graphcompose.docxFidelity", "");
        Assumptions.assumeTrue(!mode.isEmpty(), "NOT_RUN: the DOCX fidelity corpus runs with "
                + "-Dgraphcompose.docxFidelity=libreoffice, or with Word through scripts/docx-visual/word-fidelity.ps1");
        assertThat(mode).as("-Dgraphcompose.docxFidelity").isIn(LIBREOFFICE, WORD, EXPORT);
        Path work = Path.of("target", "docx-fidelity").toAbsolutePath();
        Path engine = work.resolve("engine");
        List<DocxCorpusDocument> corpus = corpus();
        String note;
        Path editor = work.resolve(mode);
        if (mode.equals(WORD)) {
            // Word converts outside this run (see word-fidelity.ps1), so its PDFs are measured
            // only when the DOCX it converted is, to the byte, the one this tree exports.
            assertThat(LibreOfficeConverter.platform()).as("Word is measured on Windows").isEqualTo("windows");
            WordConversion conversion = WordConversion.read(editor);
            Files.createDirectories(engine);
            List<String> stale = new ArrayList<>();
            for (DocxCorpusDocument document : corpus) {
                if (!conversion.convertedFrom(document.stem() + ".docx", exportTo(document, engine))) {
                    stale.add(document.stem());
                }
            }
            assertThat(stale).as("documents Word converted from other DOCX than this tree exports: run "
                                 + "scripts/docx-visual/word-fidelity.ps1, which exports and converts first").isEmpty();
            note = WORD + " " + conversion.version() + " on " + LibreOfficeConverter.platform();
        } else {
            // A document no longer in the corpus leaves no DOCX for an editor to convert.
            deleteTree(engine);
            Files.createDirectories(engine);
            List<Path> docx = new ArrayList<>();
            for (DocxCorpusDocument document : corpus) {
                docx.add(export(document, engine));
            }
            if (mode.equals(EXPORT)) {
                return;
            }
            LibreOfficeConverter converter = LibreOfficeConverter.find().orElseThrow(() -> new IllegalStateException(
                    "the DOCX fidelity corpus was asked for, and no LibreOffice was found: set -Dgraphcompose.soffice"));
            // LibreOffice can exit cleanly when a file fails to load: a PDF left from an earlier
            // run would then be measured in its place.
            deleteTree(editor);
            converter.convert(docx, editor, work);
            note = LIBREOFFICE + " " + converter.build() + " on " + LibreOfficeConverter.platform();
        }

        List<FidelityMeasurement> measurements = new ArrayList<>();
        for (DocxCorpusDocument document : corpus) {
            Path converted = editor.resolve(document.stem() + ".pdf");
            assertThat(converted).as("%s's PDF of %s", mode, document.stem()).exists();
            measurements.add(FidelityMeasurement.of(document.stem(),
                    engine.resolve(document.stem() + ".pdf"), converted));
        }
        FidelityBaseline.write(work.resolve("measured-" + mode + ".tsv"), note, measurements);
        FidelityAcceptance.write(work.resolve("measured-" + mode + "-acceptance.md"), note, measurements);

        Path baseline = Path.of("src", "test", "resources", "docx-fidelity",
                mode + "-" + LibreOfficeConverter.platform() + ".tsv");
        List<String> regressions = FidelityBaseline.read(baseline).regressions(measurements);
        if (Boolean.getBoolean("graphcompose.docxFidelity.update")) {
            // Written all the same: what it lets through is the baseline's diff, shown here first.
            regressions.forEach(regression -> System.err.println("baseline rewritten over: " + regression));
            FidelityBaseline.write(baseline, note, measurements);
            return;
        }
        assertThat(regressions)
                .as("documents set further from the page than %s holds them (measured: %s)",
                        baseline, work.resolve("measured-" + mode + ".tsv"))
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

    private static void deleteTree(Path dir) throws java.io.IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
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
        Path docx = dir.resolve(document.stem() + ".docx");
        Files.write(docx, exportTo(document, dir));
        return docx;
    }

    /** Writes a document's PDF into {@code dir}; returns its DOCX. */
    private static byte[] exportTo(DocxCorpusDocument document, Path dir) throws Exception {
        var builder = GraphCompose.document();
        if (document.margin() >= 0) {
            float margin = (float) document.margin();
            builder.pageSize(DocumentPageSize.A4).margin(margin, margin, margin, margin);
        }
        try (DocumentSession session = builder.create()) {
            document.compose().accept(session);
            Files.write(dir.resolve(document.stem() + ".pdf"), session.toPdfBytes());
            return session.export(DocxSemanticBackend.builder().deterministic(true).build());
        }
    }
}
