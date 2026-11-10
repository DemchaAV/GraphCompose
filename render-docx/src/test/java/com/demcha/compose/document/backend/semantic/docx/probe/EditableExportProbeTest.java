package com.demcha.compose.document.backend.semantic.docx.probe;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.SemanticBackend;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.layout.DocumentGraph;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders the probe corpus to PDF and DOCX and records what each export actually
 * contains.
 *
 * <p>This is a measuring harness, not a parity gate. It asserts only what is settled
 * today — that both renders are produced, that the reference PDF paginates as the
 * corpus was designed to, and that the DOCX is a readable OOXML package whose body is
 * made of real paragraphs and a real table. Everything the editable-export work still
 * has to decide is written to a manifest as an observation, so a later run can be
 * compared against this one instead of against a recollection.</p>
 *
 * <p>The counts are deliberately the ones that decide how a Word document behaves
 * rather than how it looks on open. A missing styles part means every run carries
 * direct formatting and a global restyle does nothing; an absent numbering part means
 * the list markers are characters in the text; an embedded font count of zero means a
 * family the reader does not have will be substituted. None of that shows up in a
 * screenshot, and all of it decides whether the file is editable.</p>
 *
 * <p>Artifacts land under {@code target/docx-probe}. They are inputs to the editor
 * protocol in {@code scripts/docx-visual}, which no build can perform for itself.</p>
 *
 * @author Artem Demchyshyn
 */
class EditableExportProbeTest {

    private static final Path OUTPUT = Path.of("target", "docx-probe");
    private static final Pattern ASCII_FONT = Pattern.compile("w:ascii=\"([^\"]+)\"");

    @Test
    void corpusShouldRenderToBothBackendsAndRecordWhatTheDocxContains() throws Exception {
        Files.createDirectories(OUTPUT);

        List<String> entries = new ArrayList<>();
        entries.add(probe("mixed-two-pager", 2, EditableExportFixtures::mixedTwoPager));
        entries.add(probe("boundary-cases", 1, EditableExportFixtures::boundaryCases));

        Files.writeString(OUTPUT.resolve("manifest.json"),
                "{\n  \"fixtures\": [\n" + String.join(",\n", entries) + "\n  ],\n"
                        + "  \"editorProtocol\": {\n"
                        + "    \"wordDesktop\": \"see word/conversion.json and edit/edit-protocol.json\",\n"
                        + "    \"libreOfficeWriter\": \"NOT_RUN\"\n"
                        + "  },\n"
                        + "  \"tolerances\": \"NOT_MEASURED\"\n}\n",
                StandardCharsets.UTF_8);
    }

    /**
     * Renders one fixture both ways, writes the artifacts and returns its manifest entry.
     */
    private String probe(String id, int expectedPdfPages, Function<Path, DocumentSession> fixture)
            throws Exception {
        Path pdfFile = OUTPUT.resolve(id + ".pdf");
        Path docxFile = OUTPUT.resolve(id + ".docx");

        byte[] docx;
        byte[] prototype;
        EditableExportPrototype.Applied[] applied = new EditableExportPrototype.Applied[1];
        try (DocumentSession session = fixture.apply(pdfFile)) {
            session.buildPdf();
            docx = session.export(new DocxSemanticBackend(), docxFile);
            // The same document, not a re-authored one: the prototype reads the tree the
            // export just walked, so a difference between the two files can only come
            // from the constructs the prototype adds. Null output: this writes nothing.
            DocumentGraph graph = session.export(new GraphCapture(), null);
            prototype = EditableExportPrototype.augment(docx, graph, applied);
        }
        Path prototypeFile = OUTPUT.resolve(id + "-prototype.docx");
        Files.write(prototypeFile, prototype);
        DocxShape prototypeShape = shapeOf(prototype);

        int pdfPages;
        try (PDDocument pdf = Loader.loadPDF(pdfFile.toFile())) {
            pdfPages = pdf.getNumberOfPages();
        }
        // The corpus is designed around a known pagination; a change here means the
        // fixture moved, and every measurement taken against the old one is stale.
        assertThat(pdfPages)
                .as("reference PDF pagination for %s", id)
                .isEqualTo(expectedPdfPages);

        DocxShape shape = shapeOf(docx);
        assertThat(shape.paragraphs())
                .as("%s must export real Word paragraphs", id)
                .isPositive();
        // Nothing in this corpus is a floating box today, and a body turned into boxes
        // is the specific regression the editing contract forbids.
        assertThat(shape.textBoxes())
                .as("%s: body content must flow, not sit in text boxes", id)
                .isZero();

        // Not the package hash: a DOCX carries creation timestamps and a PDF carries a
        // time-seeded /ID, so both change on every run and comparing them would only
        // ever say "this is a different run". document.xml has no such field, so its
        // hash does answer the question a reader actually has — did the body change.
        return """
                    {
                      "id": "%s",
                      "pdf": { "file": "%s.pdf", "pages": %d, "bytes": %d },
                      "docx": { "file": "%s.docx", "documentXmlSha256": "%s",
                                "bodyParagraphs": %d, "tables": %d, "tableRows": %d,
                                "pictures": %d, "textBoxes": %d, "footerParts": %d,
                                "hasStylesPart": %b, "hasNumberingPart": %b,
                                "numberedParagraphs": %d, "paragraphShading": %d,
                                "paragraphBorders": %d, "embeddedFontFaces": %d,
                                "declaredFonts": [%s] },
                      "prototype": { "file": "%s-prototype.docx",
                                "hasStylesPart": %b, "hasNumberingPart": %b,
                                "numberedParagraphs": %d, "paragraphShading": %d,
                                "paragraphBorders": %d, "textBoxes": %d,
                                "runsFreedToFollowTheStyle": %d,
                                "paragraphsNumbered": %d, "paragraphsShaded": %d },
                      "initialFidelity": "see fidelity.json",
                      "editability": "see edit/edit-protocol.json"
                    }"""
                .formatted(id,
                        id, pdfPages, Files.size(pdfFile),
                        id, sha256(documentXml(docx)),
                        shape.paragraphs(), shape.tables(), shape.tableRows(),
                        shape.pictures(), shape.textBoxes(), shape.footerParts(),
                        shape.hasStylesPart(), shape.hasNumberingPart(),
                        shape.numberedParagraphs(), shape.paragraphShading(),
                        shape.paragraphBorders(), shape.embeddedFontFaces(),
                        quoted(shape.declaredFonts()),
                        id, prototypeShape.hasStylesPart(), prototypeShape.hasNumberingPart(),
                        prototypeShape.numberedParagraphs(), prototypeShape.paragraphShading(),
                        prototypeShape.paragraphBorders(), prototypeShape.textBoxes(),
                        applied[0].styledRuns(), applied[0].numberedParagraphs(),
                        applied[0].shadedParagraphs())
                .indent(2).stripTrailing();
    }

    /**
     * What the exported package is made of, read back from the file rather than from the
     * exporter's own view of what it wrote.
     *
     * @param paragraphs top-level body paragraphs
     * @param tables top-level body tables
     * @param tableRows rows across those tables
     * @param pictures embedded pictures across all body paragraphs
     * @param textBoxes {@code w:txbxContent} elements — above zero means some body
     *                  content is a floating box rather than flowing text
     * @param footerParts real {@code w:ftr} parts in the package
     * @param hasStylesPart whether a styles part exists at all; without one every run
     *                      carries direct formatting and a global restyle is inert
     * @param hasNumberingPart whether a numbering part exists; without one a list is
     *                         marker characters in ordinary paragraphs
     * @param numberedParagraphs paragraphs carrying {@code w:numPr}
     * @param paragraphShading {@code w:shd} elements — the only fill Word paragraphs and
     *                         table cells can carry
     * @param paragraphBorders {@code w:pBdr} elements — a panel outline, if there is one
     * @param embeddedFontFaces {@code w:embedRegular} and friends; zero means a family
     *                          the reader lacks will be substituted
     * @param declaredFonts distinct {@code w:ascii} families named in the body
     */
    private record DocxShape(int paragraphs, int tables, int tableRows, int pictures,
                             int textBoxes, int footerParts, boolean hasStylesPart,
                             boolean hasNumberingPart, int numberedParagraphs,
                             int paragraphShading, int paragraphBorders,
                             int embeddedFontFaces, Set<String> declaredFonts) {
    }

    private static DocxShape shapeOf(byte[] docx) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            int rows = document.getTables().stream().mapToInt(t -> t.getRows().size()).sum();
            int pictures = document.getParagraphs().stream()
                    .flatMap(p -> p.getRuns().stream())
                    .mapToInt(r -> r.getEmbeddedPictures().size())
                    .sum();
            // Counted on the raw XML: POI has no accessor for several of these, and an
            // absence is exactly what the editing contract turns on.
            String xml = document.getDocument().toString();
            Set<String> fonts = new TreeSet<>();
            Matcher matcher = ASCII_FONT.matcher(xml);
            while (matcher.find()) {
                fonts.add(matcher.group(1));
            }
            return new DocxShape(document.getParagraphs().size(), document.getTables().size(),
                    rows, pictures,
                    count(xml, "txbxContent"),
                    document.getFooterList().size(),
                    document.getStyles() != null,
                    document.getNumbering() != null,
                    count(xml, "<w:numPr"),
                    count(xml, "<w:shd"),
                    count(xml, "<w:pBdr"),
                    count(xml, "embedRegular") + count(xml, "embedBold")
                            + count(xml, "embedItalic") + count(xml, "embedBoldItalic"),
                    fonts);
        }
    }

    private static int count(String haystack, String needle) {
        int count = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }

    /**
     * Hands back the very tree the export was given.
     *
     * <p>There is no public way to ask a session for its node tree, and the prototype has
     * to read the same one the exporter walked — re-authoring the fixture would compare
     * two documents rather than two exports of one.</p>
     */
    private static final class GraphCapture implements SemanticBackend<DocumentGraph> {
        @Override
        public String name() {
            return "graph-capture";
        }

        @Override
        public DocumentGraph export(DocumentGraph graph, SemanticExportContext context) {
            return graph;
        }
    }

    private static String quoted(Set<String> values) {
        return values.stream().map(v -> "\"" + v + "\"").reduce((a, b) -> a + ", " + b).orElse("");
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    /** Reads {@code word/document.xml} out of the package — the body, without the timestamps. */
    private static byte[] documentXml(byte[] docx) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if ("word/document.xml".equals(entry.getName())) {
                    return zip.readAllBytes();
                }
            }
        }
        throw new IOException("the export contains no word/document.xml");
    }
}
