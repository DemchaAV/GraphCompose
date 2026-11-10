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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every template preset's DOCX, and that of each document built for a shape no preset has
 * ({@link SyntheticDocxCorpus}, {@link KeptGroupsAtThePageFoot}), set by an editor, stands where
 * its baseline holds it.
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
 * <p>The same run feeds the gate corpus documents changed afterwards in each way a reader would
 * see ({@link DocxTamper}) — a paragraph gone, another font, on a document of short lines among
 * them, a page break more, a card's row held to a height its text does not fit, the last card's
 * among them, a table replaced by a picture —
 * through the same editor and the same measurement, and fails unless the gate fails on every one.
 * Each is held to this run's own measurement of the document it was changed from, not to the
 * baseline, so what it catches is the change and nothing the baseline holds besides. They are
 * converted apart from the corpus, each document with a copy of it left unchanged, which must
 * stand where the corpus's conversion set it: a difference the second conversion makes on its
 * own is then not counted as a change caught. They are written to {@code tampered}, and set by
 * the editor into {@code <editor>-tampered}.</p>
 */
class DocxFidelityCorpusTest {

    private static final String LIBREOFFICE = "libreoffice";
    private static final String WORD = "word";
    private static final String EXPORT = "export";

    /** The corpus document the gate is fed changed most ways: it has a paragraph, a card and a table. */
    static final String TAMPERED_SOURCE = "invoice-modern";

    /**
     * The corpus document whose last card is held to a height: nothing that holds text follows it
     * in the body — the sign-off under it on the page is a drawing placed from a paragraph above
     * it — so nothing moves when it shrinks, and the gate must fail on the card's own lines.
     */
    static final String LAST_CARD_SOURCE = "invoice-luma_studio";

    /**
     * The corpus document set in another face whose lines change only in width: none of them is
     * long enough to break at other words, and Times New Roman sets them on the page's baselines.
     * Set so in Word 16.0.20527, LibreOffice 26.8 on Windows and CI's LibreOffice 24.2 on Linux,
     * not one of its 114 lines was lost or moved more than half a point down or across, and every
     * one ran wider or narrower by more than that — by up to 16 points.
     */
    static final String SHORT_LINES_SOURCE = "synthetic-card_over_pages";

    /** A change the gate must fail on, and the corpus document whose DOCX it is made to. */
    record Change(String source, DocxTamper tamper) {

        /** The changed DOCX's file stem: its document's, and the change's. */
        String stem() {
            return source + "--" + tamper.name();
        }
    }

    /** The changes the gate must fail on. */
    static List<Change> tampers() {
        return List.of(
                new Change(TAMPERED_SOURCE, DocxTamper.withoutParagraph("Thank you for choosing GraphCompose")),
                new Change(TAMPERED_SOURCE, DocxTamper.inFont("Courier New")),
                new Change(TAMPERED_SOURCE, DocxTamper.withPageBreakBefore("Thank you for choosing GraphCompose")),
                new Change(TAMPERED_SOURCE, DocxTamper.withExactRowHeight("Please include the invoice number", 500)),
                new Change(TAMPERED_SOURCE, DocxTamper.asPicture("Discovery workshop")),
                new Change(LAST_CARD_SOURCE, DocxTamper.withExactRowHeight("Thank you for your business", 450)),
                new Change(SHORT_LINES_SOURCE,
                        DocxTamper.inFont("Times New Roman").costing(DocxTamper.Cost.WIDTH)));
    }

    /** The documents the changes are made to, in the order they are first named. */
    static List<String> sources() {
        return tampers().stream().map(Change::source).distinct().toList();
    }

    /**
     * What the changed documents' conversion makes: for each document changed, a copy left
     * unchanged first, then each change made to it.
     */
    static List<Change> converted() {
        List<Change> converted = new ArrayList<>();
        for (String source : sources()) {
            converted.add(new Change(source, DocxTamper.unchanged()));
            tampers().stream().filter(change -> change.source().equals(source)).forEach(converted::add);
        }
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
        Map<String, byte[]> sourceDocx = new LinkedHashMap<>();
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
                if (sources().contains(document.stem())) {
                    sourceDocx.put(document.stem(), docx);
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
            for (String source : sources()) {
                sourceDocx.put(source, Files.readAllBytes(engine.resolve(source + ".docx")));
            }
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
        Map<String, FidelityMeasurement> source = new LinkedHashMap<>();
        for (FidelityMeasurement measured : measurements) {
            if (sources().contains(measured.stem())) {
                source.put(measured.stem(), measured);
            }
        }
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

    private static final String LET_THROUGH = "changes to the corpus's DOCX the gate did not fail on, or failed on short "
                                              + "of what the change costs (the editor's PDFs: %s)";

    /**
     * Each change the gate let through, the changed documents made and converted here for
     * LibreOffice, or checked to be the ones Word converted; anything that stops them being made,
     * converted or measured is one more entry, not an exception, so the corpus's own result is
     * reported with it.
     *
     * @param source     this run's measurement of each document changed, by its stem
     * @param sourceDocx each document changed, as this tree exports it, by its stem
     */
    static List<String> changesLetThrough(String mode, Path work, Path engine, Map<String, FidelityMeasurement> source,
                                          Map<String, byte[]> sourceDocx, LibreOfficeConverter converter) {
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
            List<String> through = new ArrayList<>();
            for (String stem : sources()) {
                FidelityMeasurement measured = source.get(stem);
                if (measured == null) {
                    throw new IllegalStateException("the run measured no " + stem + " to hold its changes to");
                }
                through.addAll(letThrough(measured, engine.resolve(stem + ".pdf"), editorTampered));
            }
            return through;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return List.of("the changed documents' conversion was interrupted");
        } catch (java.io.IOException | RuntimeException failed) {
            java.io.StringWriter trace = new java.io.StringWriter();
            failed.printStackTrace(new java.io.PrintWriter(trace));
            return List.of("the changed documents could not be made, converted or measured: " + trace);
        }
    }

    /** Writes each document changed, unchanged and changed each way, into {@code dir}, emptied first. */
    private static List<Path> writeTampered(Map<String, byte[]> docx, Path dir) throws java.io.IOException {
        deleteTree(dir);
        Files.createDirectories(dir);
        List<Path> written = new ArrayList<>();
        for (Change change : converted()) {
            Path file = dir.resolve(change.stem() + ".docx");
            Files.write(file, change.tamper().applyTo(sourceOf(docx, change)));
            written.add(file);
        }
        return written;
    }

    /** The changed documents Word converted from other DOCX than this tree's documents change into. */
    static List<String> staleTampered(WordConversion conversion, Map<String, byte[]> sourceDocx)
            throws java.io.IOException {
        List<String> stale = new ArrayList<>();
        for (Change change : converted()) {
            if (!conversion.convertedFrom(change.stem() + ".docx", change.tamper().applyTo(sourceOf(sourceDocx, change)))) {
                stale.add(change.stem());
            }
        }
        return stale;
    }

    private static byte[] sourceOf(Map<String, byte[]> docx, Change change) {
        byte[] source = docx.get(change.source());
        if (source == null) {
            throw new IllegalStateException("the run exported no " + change.source() + " to change");
        }
        return source;
    }

    /**
     * Each change made to {@code source}'s document that the gate let through. Held to this run's
     * measurement of that document, the unchanged copy converted with them must stand where that
     * measurement holds it, and each changed document must stand apart from it — and, where the
     * change is one that costs lines or a page, cost them.
     */
    static List<String> letThrough(FidelityMeasurement source, Path enginePdf, Path editorPdfs)
            throws java.io.IOException {
        FidelityBaseline held = FidelityBaseline.of(List.of(source));
        List<String> through = new ArrayList<>();
        for (Change change : converted()) {
            if (!change.source().equals(source.stem())) {
                continue;
            }
            Path pdf = editorPdfs.resolve(change.stem() + ".pdf");
            if (!Files.exists(pdf)) {
                // A DOCX the editor cannot open is a change that was never made, not one caught.
                through.add(change.stem() + ": the editor wrote no PDF of it");
                continue;
            }
            FidelityMeasurement changed = FidelityMeasurement.of(source.stem(), enginePdf, pdf);
            List<String> apart = held.divergences(List.of(changed));
            String why = change.tamper().name().equals(DocxTamper.UNCHANGED)
                    ? unchangedApart(change, apart)
                    : letThrough(change, source, changed, apart);
            if (why != null) {
                through.add(why);
            } else if (!apart.isEmpty()) {
                System.out.println("against the unchanged document, the gate fails on " + change.stem() + ": "
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
    static String unchangedApart(Change unchanged, List<String> apart) {
        return apart.isEmpty() ? null : unchanged.stem() + ": converted with the changes, the unchanged document "
                                        + "stands apart from the corpus's conversion of it, so what they show is not "
                                        + "theirs alone: " + String.join("; ", apart);
    }

    /** Why the gate let a change through, or {@code null} when it failed on it as it must. */
    static String letThrough(Change change, FidelityMeasurement source, FidelityMeasurement changed,
                             List<String> apart) {
        if (apart.isEmpty()) {
            return change.stem() + ": the gate let it through";
        }
        DocxTamper.Cost cost = change.tamper().costs();
        boolean costs = switch (cost) {
            case ANY -> true;
            case LOST_LINES -> !changed.found().keySet().containsAll(source.found().keySet());
            case PAGES -> changed.editorPages() != source.editorPages();
            case WIDTH -> source.found().entrySet().stream().anyMatch(line -> {
                FidelityMeasurement.Found there = changed.found().get(line.getKey());
                return there != null && FidelityBaseline.moved(line.getValue().wider(), there.wider());
            });
        };
        return costs ? null : change.stem() + ": failed, but without " + cost + ": " + String.join("; ", apart);
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
        corpus.addAll(KeptGroupsAtThePageFoot.documents());
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
