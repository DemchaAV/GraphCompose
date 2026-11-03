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
import org.assertj.core.api.SoftAssertions;
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
 * Every template preset's DOCX, and that of each document built for a shape no preset has
 * ({@link SyntheticDocxCorpus}), set by an editor, stands where its baseline holds it.
 *
 * <p>The engine draws each corpus document to PDF and exports it to DOCX; an editor converts the
 * DOCX to PDF; each document is measured line by line against the page
 * ({@link FidelityMeasurement}) and held to the baseline for this editor and operating system
 * ({@link FidelityBaseline}). A change to the export that moves any document fails here,
 * wherever the document is and whichever way it moved: a move nearer the page is the export
 * doing better and is recorded the same way, by rewriting the baseline in the same commit.</p>
 *
 * <p>It runs only when asked for, with {@code -Dgraphcompose.docxFidelity}, and takes a few
 * minutes; a value it does not know fails. {@code libreoffice} exports, converts with
 * LibreOffice and measures, and fails rather than passing unrun without LibreOffice. Microsoft
 * Word, the editor the export answers to, converts outside the build, around two runs of this
 * test ({@code scripts/docx-visual/word-fidelity.ps1}): {@code export} writes the documents,
 * Word converts them, and {@code word} measures Word's PDFs against {@code word-windows.tsv} —
 * each only when the DOCX Word converted, by the SHA-256 its conversion records, is the one
 * this tree exports ({@link WordConversion}). {@code -Dgraphcompose.docxFidelity.update=true}
 * writes what it measured as the baseline, which is how a change that moves documents records
 * the move; the baseline's diff then shows the reviewer which documents moved, and which way.
 * The documents, their PDFs and the report are left in {@code target/docx-fidelity}.</p>
 *
 * <p>The same run feeds the gate one document changed afterwards in each way a reader would see
 * ({@link DocxTamper}) — a paragraph gone, another font, a page break more, a card's row held to
 * a height its text does not fit, a table replaced by a picture — through the same editor and the
 * same measurement, and fails unless the gate fails on every one. Each is held to this run's own
 * measurement of the document it was changed from, not to the baseline, so what it catches is
 * the change and nothing the baseline holds besides. They are converted apart from the corpus,
 * with a copy of the document left unchanged, which must stand where the corpus's conversion set
 * it: a difference the second conversion makes on its own is then not counted as a change
 * caught. They are written to {@code tampered}, and set by the editor into
 * {@code <editor>-tampered}.</p>
 */
class DocxFidelityCorpusTest {

    private static final String LIBREOFFICE = "libreoffice";
    private static final String WORD = "word";
    private static final String EXPORT = "export";

    /** The corpus document the gate is fed changed: it has a paragraph, a card and a table. */
    static final String TAMPERED_SOURCE = "invoice-modern";

    /** The changes the gate must fail on, each made to {@link #TAMPERED_SOURCE}'s DOCX. */
    static List<DocxTamper> tampers() {
        return List.of(
                DocxTamper.withoutParagraph("Thank you for choosing GraphCompose"),
                DocxTamper.inFont("Courier New"),
                DocxTamper.withPageBreakBefore("Thank you for choosing GraphCompose"),
                DocxTamper.withExactRowHeight("Please include the invoice number", 500),
                DocxTamper.asPicture("Discovery workshop"));
    }

    /** What the changed documents' conversion makes: the unchanged copy first, then each change. */
    static List<DocxTamper> converted() {
        List<DocxTamper> converted = new ArrayList<>();
        converted.add(DocxTamper.unchanged());
        converted.addAll(tampers());
        return converted;
    }

    @Test
    void everyDocumentStandsWhereItsBaselineHoldsIt() throws Exception {
        String mode = System.getProperty("graphcompose.docxFidelity", "");
        Assumptions.assumeTrue(!mode.isEmpty(), "NOT_RUN: the DOCX fidelity corpus runs with "
                + "-Dgraphcompose.docxFidelity=libreoffice, or with Word through scripts/docx-visual/word-fidelity.ps1");
        assertThat(mode).as("-Dgraphcompose.docxFidelity").isIn(LIBREOFFICE, WORD, EXPORT);
        Path work = Path.of("target", "docx-fidelity").toAbsolutePath();
        Path engine = work.resolve("engine");
        List<DocxCorpusDocument> corpus = corpus();
        String note;
        Path editor = work.resolve(mode);
        Path tampered = work.resolve("tampered");
        Path editorTampered = work.resolve(mode + "-tampered");
        byte[] sourceDocx = null;
        LibreOfficeConverter converter = null;
        if (mode.equals(WORD)) {
            // Word converts outside this run (see word-fidelity.ps1), so its PDFs are measured
            // only when the DOCX it converted is, to the byte, the one this tree exports.
            assertThat(LibreOfficeConverter.platform()).as("Word is measured on Windows").isEqualTo("windows");
            WordConversion conversion = WordConversion.read(editor);
            Files.createDirectories(engine);
            List<String> stale = new ArrayList<>();
            for (DocxCorpusDocument document : corpus) {
                byte[] docx = exportTo(document, engine);
                if (!conversion.convertedFrom(document.stem() + ".docx", docx)) {
                    stale.add(document.stem());
                }
                if (document.stem().equals(TAMPERED_SOURCE)) {
                    sourceDocx = docx;
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
            sourceDocx = Files.readAllBytes(engine.resolve(TAMPERED_SOURCE + ".docx"));
            if (mode.equals(EXPORT)) {
                // Written after the corpus, so a change that cannot be made leaves the corpus for
                // Word to convert; the word run then reports the failure beside the corpus's result.
                try {
                    writeTampered(sourceDocx, tampered);
                } catch (java.io.IOException | RuntimeException unmade) {
                    unmade.printStackTrace();
                }
                return;
            }
            converter = LibreOfficeConverter.find().orElseThrow(() -> new IllegalStateException(
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
        Path baseline = Path.of("src", "test", "resources", "docx-fidelity",
                mode + "-" + LibreOfficeConverter.platform() + ".tsv");
        // Written with the baseline's notes, so that copying this file over the baseline — what a
        // run on a machine the baseline cannot be shot on, CI's, is for — keeps them.
        FidelityBaseline.write(work.resolve("measured-" + mode + ".tsv"), note, measurements, baseline);
        FidelityAcceptance.write(work.resolve("measured-" + mode + "-acceptance.md"), note, measurements);
        FidelityMeasurement source = measurements.stream()
                .filter(measured -> measured.stem().equals(TAMPERED_SOURCE)).findFirst().orElseThrow();
        List<String> divergences = FidelityBaseline.read(baseline).divergences(measurements);
        if (Boolean.getBoolean("graphcompose.docxFidelity.update")) {
            // Written all the same: what it lets through is the baseline's diff, shown here first.
            divergences.forEach(divergence -> System.err.println("baseline rewritten over: " + divergence));
            FidelityBaseline.write(baseline, note, measurements);
        }
        // The changed documents are made, converted and measured only now, once the corpus's
        // measurements, its report and any rewritten baseline are written: whatever goes wrong
        // with them is reported beside the baseline's divergences, never in their place.
        List<String> changes = changesLetThrough(mode, work, engine, source, sourceDocx, converter);
        if (Boolean.getBoolean("graphcompose.docxFidelity.update")) {
            assertThat(changes).as(LET_THROUGH, editorTampered).isEmpty();
            return;
        }
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(divergences)
                    .as("documents standing apart from %s, whichever way they moved: a change that moves "
                        + "them — nearer the page included — rewrites the baseline in the same commit, with "
                        + "-Dgraphcompose.docxFidelity.update=true, so its diff shows what moved (measured: %s)",
                            baseline, work.resolve("measured-" + mode + ".tsv"))
                    .isEmpty();
            softly.assertThat(changes).as(LET_THROUGH, editorTampered).isEmpty();
        });
    }

    private static final String LET_THROUGH = "changes to " + TAMPERED_SOURCE + "'s DOCX the gate did not fail on, "
                                              + "or failed on short of what the change costs (the editor's PDFs: %s)";

    /** A changed DOCX's file stem: its document's, and the change's. */
    static String tamperedStem(DocxTamper tamper) {
        return TAMPERED_SOURCE + "--" + tamper.name();
    }

    /**
     * Each change the gate let through, the changed documents made and converted here for
     * LibreOffice, or checked to be the ones Word converted; anything that stops them being made,
     * converted or measured is one more entry, not an exception, so the corpus's own result is
     * reported with it.
     */
    static List<String> changesLetThrough(String mode, Path work, Path engine, FidelityMeasurement source,
                                                  byte[] sourceDocx, LibreOfficeConverter converter) {
        Path editorTampered = work.resolve(mode + "-tampered");
        try {
            if (mode.equals(WORD)) {
                List<String> stale = staleTampered(WordConversion.read(editorTampered), sourceDocx);
                if (!stale.isEmpty()) {
                    return List.of("changed documents Word converted from other DOCX than this tree makes: " + stale
                                   + ": run scripts/docx-visual/word-fidelity.ps1, which exports and converts first");
                }
            } else {
                List<Path> changed = writeTampered(sourceDocx, work.resolve("tampered"));
                deleteTree(editorTampered);
                converter.convert(changed, editorTampered, work, "libreoffice-tampered.log");
            }
            return letThrough(source, engine.resolve(TAMPERED_SOURCE + ".pdf"), editorTampered);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return List.of("the changed documents' conversion was interrupted");
        } catch (java.io.IOException | RuntimeException failed) {
            java.io.StringWriter trace = new java.io.StringWriter();
            failed.printStackTrace(new java.io.PrintWriter(trace));
            return List.of("the changed documents could not be made, converted or measured: " + trace);
        }
    }

    /** Writes the source DOCX, unchanged and changed each way, into {@code dir}, emptied first. */
    private static List<Path> writeTampered(byte[] docx, Path dir) throws java.io.IOException {
        deleteTree(dir);
        Files.createDirectories(dir);
        List<Path> written = new ArrayList<>();
        for (DocxTamper tamper : converted()) {
            Path file = dir.resolve(tamperedStem(tamper) + ".docx");
            Files.write(file, tamper.applyTo(docx));
            written.add(file);
        }
        return written;
    }

    /** The changed documents Word converted from other DOCX than this tree's source changes into. */
    static List<String> staleTampered(WordConversion conversion, byte[] sourceDocx) throws java.io.IOException {
        List<String> stale = new ArrayList<>();
        for (DocxTamper tamper : converted()) {
            if (!conversion.convertedFrom(tamperedStem(tamper) + ".docx", tamper.applyTo(sourceDocx))) {
                stale.add(tamperedStem(tamper));
            }
        }
        return stale;
    }

    /**
     * Each change the gate let through. Held to this run's measurement of the document it was
     * changed from, the unchanged copy converted with them must stand where that measurement
     * holds it, and each changed document must stand apart from it — and, where the change is
     * one that costs lines or a page, cost them.
     */
    static List<String> letThrough(FidelityMeasurement source, Path enginePdf, Path editorPdfs)
            throws java.io.IOException {
        FidelityBaseline held = FidelityBaseline.of(List.of(source));
        List<String> through = new ArrayList<>();
        for (DocxTamper tamper : converted()) {
            Path pdf = editorPdfs.resolve(tamperedStem(tamper) + ".pdf");
            if (!Files.exists(pdf)) {
                // A DOCX the editor cannot open is a change that was never made, not one caught.
                through.add(tamper.name() + ": the editor wrote no PDF of it");
                continue;
            }
            FidelityMeasurement changed = FidelityMeasurement.of(source.stem(), enginePdf, pdf);
            List<String> apart = held.divergences(List.of(changed));
            String why = tamper.name().equals(DocxTamper.UNCHANGED)
                    ? unchangedApart(apart)
                    : letThrough(tamper, source, changed, apart);
            if (why != null) {
                through.add(why);
            } else if (!apart.isEmpty()) {
                System.out.println("against the unchanged document, the gate fails on " + tamper.name() + ": "
                                   + String.join("; ", apart));
            }
        }
        return through;
    }

    /**
     * Why the unchanged copy cannot vouch for the changed ones' conversion, or {@code null}: it
     * stands apart from the corpus's conversion of the same document, so a difference a change
     * shows may be the second conversion's.
     */
    static String unchangedApart(List<String> apart) {
        return apart.isEmpty() ? null : "unchanged: converted with the changes, the unchanged document stands apart "
                                        + "from the corpus's conversion of it, so what they show is not theirs alone: "
                                        + String.join("; ", apart);
    }

    /** Why the gate let a change through, or {@code null} when it failed on it as it must. */
    static String letThrough(DocxTamper tamper, FidelityMeasurement source, FidelityMeasurement changed,
                             List<String> apart) {
        if (apart.isEmpty()) {
            return tamper.name() + ": the gate let it through";
        }
        boolean costs = switch (tamper.costs()) {
            case ANY -> true;
            case LOST_LINES -> !changed.found().keySet().containsAll(source.found().keySet());
            case PAGES -> changed.editorPages() != source.editorPages();
        };
        return costs ? null : tamper.name() + ": failed, but without " + tamper.costs() + ": " + String.join("; ", apart);
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

    /** Every family's presets, and the documents built for shapes no preset has. */
    static List<DocxCorpusDocument> corpus() {
        List<DocxCorpusDocument> corpus = new ArrayList<>();
        corpus.addAll(CvDocxCorpus.documents());
        corpus.addAll(CoverLetterDocxCorpus.documents());
        corpus.addAll(InvoiceDocxCorpus.documents());
        corpus.addAll(ProposalDocxCorpus.documents());
        corpus.addAll(ReceiptDocxCorpus.documents());
        corpus.addAll(RotaDocxCorpus.documents());
        corpus.addAll(SyntheticDocxCorpus.documents());
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
        try (DocumentSession session = composed(document)) {
            Files.write(dir.resolve(document.stem() + ".pdf"), session.toPdfBytes());
            return session.export(DocxSemanticBackend.builder().deterministic(true).build());
        }
    }

    /** A fresh session holding a corpus document, on its page; the caller closes it. */
    static DocumentSession composed(DocxCorpusDocument document) {
        var builder = GraphCompose.document();
        if (document.margin() >= 0) {
            float margin = (float) document.margin();
            builder.pageSize(DocumentPageSize.A4).margin(margin, margin, margin, margin);
        }
        DocumentSession session = builder.create();
        try {
            document.compose().accept(session);
        } catch (RuntimeException | Error e) {
            session.close();
            throw e;
        }
        return session;
    }
}
