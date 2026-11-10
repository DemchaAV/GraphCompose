package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Each change the corpus run feeds the fidelity gate is made, to the document it names, as it
 * says — and a change that finds nothing to change refuses rather than changing nothing, since
 * the gate would then pass for want of anything to catch. What the gate does with the changed
 * documents is {@link DocxFidelityCorpusTest}'s, through a real editor; the decision it reads
 * them by is held here too, on measurements written to differ in just that way.
 */
class DocxTamperTest {

    private static final String THANKS = "Thank you for choosing GraphCompose";

    private static final String NOTES = "Thank you for your business";

    /** {@link DocxFidelityCorpusTest#TAMPERED_SOURCE}'s DOCX. */
    private static byte[] source;

    /** Each document the run changes, as the run exports it, by its stem. */
    private static final Map<String, byte[]> SOURCES = new LinkedHashMap<>();

    @BeforeAll
    static void exportTheSources() throws Exception {
        for (String stem : DocxFidelityCorpusTest.sources()) {
            DocxCorpusDocument document = DocxFidelityCorpusTest.corpus().stream()
                    .filter(each -> each.stem().equals(stem)).findFirst().orElseThrow();
            try (DocumentSession session = DocxFidelityCorpusTest.composed(document)) {
                SOURCES.put(stem, session.export(DocxSemanticBackend.builder().deterministic(true).build()));
            }
        }
        source = SOURCES.get(DocxFidelityCorpusTest.TAMPERED_SOURCE);
    }

    @Test
    void everyChangeTheRunMakesStillOpensAsXmlAndIsMadeTheSameEachTime() throws Exception {
        assertThat(DocxFidelityCorpusTest.tampers()).extracting(DocxFidelityCorpusTest.Change::stem).containsExactly(
                "invoice-modern--dropped-paragraph", "invoice-modern--changed-font", "invoice-modern--extra-page-break",
                "invoice-modern--exact-card-height", "invoice-modern--table-as-picture",
                "invoice-luma_studio--exact-card-height", "synthetic-card_over_pages--changed-font");
        for (DocxFidelityCorpusTest.Change change : DocxFidelityCorpusTest.tampers()) {
            DocxTamper tamper = change.tamper();
            byte[] docx = SOURCES.get(change.source());
            byte[] changed = tamper.applyTo(docx);
            assertThat(changed).as(change.stem()).isNotEqualTo(docx);
            assertThat(tamper.applyTo(docx)).as("%s, made again", change.stem()).isEqualTo(changed);
            Map<String, byte[]> parts = parts(changed);
            for (Map.Entry<String, byte[]> part : parts.entrySet()) {
                if (part.getKey().endsWith(".xml") || part.getKey().endsWith(".rels")) {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    factory.newDocumentBuilder().parse(new ByteArrayInputStream(part.getValue()));
                }
            }
            assertThat(parts.keySet()).as(change.stem()).containsAll(parts(docx).keySet());
        }
    }

    @Test
    void eachDocumentChangedIsConvertedUnchangedFirstThenWithEachOfItsChanges() {
        assertThat(DocxFidelityCorpusTest.sources())
                .containsExactly(DocxFidelityCorpusTest.TAMPERED_SOURCE, DocxFidelityCorpusTest.LAST_CARD_SOURCE,
                        DocxFidelityCorpusTest.SHORT_LINES_SOURCE);
        assertThat(DocxFidelityCorpusTest.converted()).extracting(DocxFidelityCorpusTest.Change::stem).containsExactly(
                "invoice-modern--unchanged", "invoice-modern--dropped-paragraph", "invoice-modern--changed-font",
                "invoice-modern--extra-page-break", "invoice-modern--exact-card-height",
                "invoice-modern--table-as-picture",
                "invoice-luma_studio--unchanged", "invoice-luma_studio--exact-card-height",
                "synthetic-card_over_pages--unchanged", "synthetic-card_over_pages--changed-font");
    }

    @Test
    void theShortLinesAreSetInAnotherFaceAndMustChangeWidth() throws Exception {
        DocxTamper face = DocxFidelityCorpusTest.tampers().stream()
                .filter(change -> change.source().equals(DocxFidelityCorpusTest.SHORT_LINES_SOURCE))
                .map(DocxFidelityCorpusTest.Change::tamper).findFirst().orElseThrow();
        Map<String, byte[]> before = parts(SOURCES.get(DocxFidelityCorpusTest.SHORT_LINES_SOURCE));
        Map<String, byte[]> after = parts(face.applyTo(SOURCES.get(DocxFidelityCorpusTest.SHORT_LINES_SOURCE)));

        assertThat(fonts(before)).isNotEmpty().doesNotContain("Times New Roman");
        assertThat(fonts(after)).containsOnly("Times New Roman");
        assertThat(face.costs()).isEqualTo(DocxTamper.Cost.WIDTH);
    }

    @Test
    void nothingWithTextFollowsTheLastCardInTheBodyAndOnlyItsOwnRowIsHeld() throws Exception {
        byte[] luma = SOURCES.get(DocxFidelityCorpusTest.LAST_CARD_SOURCE);
        String before = body(luma);
        DocxTamper held = DocxFidelityCorpusTest.tampers().stream()
                .filter(change -> change.source().equals(DocxFidelityCorpusTest.LAST_CARD_SOURCE))
                .map(DocxFidelityCorpusTest.Change::tamper).findFirst().orElseThrow();
        String after = body(held.applyTo(luma));

        // The case the change is there for: nothing that holds text follows the card in the body,
        // so a card that shrinks moves no line below it, and only its own lines can fail the gate.
        List<int[]> children = DocxTamper.children(before, DocxTamper.body(before), Set.of("w:p", "w:tbl"));
        int card = -1;
        for (int i = 0; i < children.size(); i++) {
            if (DocxTamper.letters(before.substring(children.get(i)[0], children.get(i)[1])).contains(NOTES)) {
                card = i;
            }
        }
        assertThat(card).as("the body element holding the notes").isNotNegative();
        assertThat(before.substring(children.get(card)[0], children.get(card)[0] + 7)).isEqualTo("<w:tbl>");
        assertThat(children.subList(card + 1, children.size()))
                .as("what follows the card in the body: one paragraph, empty").singleElement()
                .satisfies(span -> assertThat(before.substring(span[0], span[1])).startsWith("<w:p")
                        .satisfies(paragraph -> assertThat(DocxTamper.letters(paragraph)).isEmpty()));
        assertThat(held.costs()).isEqualTo(DocxTamper.Cost.LOST_LINES);
        // The card's own row is held, and nothing outside it changes.
        int[] table = children.get(card);
        int[] inside = {before.indexOf('>', table[0]) + 1, before.lastIndexOf("</", table[1] - 1)};
        List<int[]> rows = DocxTamper.children(before, inside, Set.of("w:tr")).stream()
                .filter(span -> DocxTamper.letters(before.substring(span[0], span[1])).contains(NOTES)).toList();
        assertThat(rows).as("the card's own rows holding the notes").hasSize(1);
        String head = before.substring(0, rows.get(0)[0]);
        String tail = before.substring(rows.get(0)[1]);
        assertThat(after).startsWith(head).endsWith(tail);
        String row = after.substring(head.length(), after.length() - tail.length());
        assertThat(row.substring(0, row.indexOf("<w:tc")))
                .as("the row's own properties").containsOnlyOnce("<w:trHeight")
                .contains("<w:trHeight w:val=\"450\" w:hRule=\"exact\"/>");
        assertThat(DocxTamper.letters(row)).isEqualTo(DocxTamper.letters(before.substring(rows.get(0)[0], rows.get(0)[1])));
    }

    @Test
    void theParagraphIsTakenOutAndNothingElse() throws Exception {
        String before = body(source);
        String after = body(DocxTamper.withoutParagraph(THANKS).applyTo(source));

        assertThat(DocxTamper.letters(before)).contains(THANKS);
        assertThat(DocxTamper.letters(after)).doesNotContain(THANKS);
        assertThat(paragraphs(after)).isEqualTo(paragraphs(before) - 1);
        assertThat(DocxTamper.letters(after).length()).isEqualTo(
                DocxTamper.letters(before).length() - letters(paragraphHolding(before, THANKS)).length());
    }

    @Test
    void everyFontTheDocumentNamesIsTheNewOne() throws Exception {
        Map<String, byte[]> before = parts(source);
        Map<String, byte[]> after = parts(DocxTamper.inFont("Courier New").applyTo(source));

        assertThat(fonts(before)).isNotEmpty().doesNotContain("Courier New");
        assertThat(fonts(after)).containsOnly("Courier New");
        assertThat(rFonts(after)).as("as many w:rFonts as there were, every one in place").isEqualTo(rFonts(before));
        assertThat(new String(before.get("word/numbering.xml"), StandardCharsets.UTF_8))
                .as("the list levels name a font of their own").contains("<w:rFonts");
    }

    @Test
    void aThemeFontIsTakenOutAndALanguageIsNotAFont() throws Exception {
        String styles = "<w:styles " + DocxFixtures.NAMESPACES + "><w:docDefaults><w:rPrDefault><w:rPr>"
                        + "<w:rFonts w:asciiTheme=\"minorHAnsi\" w:hAnsiTheme=\"minorHAnsi\" w:ascii=\"Calibri\"/>"
                        + "<w:lang w:val=\"en-GB\" w:eastAsia=\"zh-CN\"/></w:rPr></w:rPrDefault></w:docDefaults>"
                        + "<w:style w:styleId=\"Code\"><w:rPr><w:rFonts w:ascii=\"Consolas\"></w:rFonts></w:rPr></w:style>"
                        + "</w:styles>";
        byte[] docx = DocxFixtures.docx(Map.of("word/document.xml", DocxFixtures.body(""), "word/styles.xml", styles));

        String after = new String(parts(DocxTamper.inFont("Courier New").applyTo(docx)).get("word/styles.xml"),
                StandardCharsets.UTF_8);

        assertThat(after).doesNotContain("Theme").doesNotContain("Calibri").doesNotContain("Consolas")
                .contains("<w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\" w:eastAsia=\"Courier New\" "
                          + "w:cs=\"Courier New\"/>")
                .contains("<w:lang w:val=\"en-GB\" w:eastAsia=\"zh-CN\"/>");
    }

    @Test
    void theUnchangedCopyIsHeldAsTheControlAndTheChangesAsChanges(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        java.nio.file.Path engine = dir.resolve("engine.pdf");
        DocxCorpusDocument document = DocxFidelityCorpusTest.corpus().stream()
                .filter(each -> each.stem().equals(DocxFidelityCorpusTest.TAMPERED_SOURCE)).findFirst().orElseThrow();
        try (DocumentSession session = DocxFidelityCorpusTest.composed(document)) {
            java.nio.file.Files.write(engine, session.toPdfBytes());
        }
        FidelityMeasurement page = FidelityMeasurement.of(DocxFidelityCorpusTest.TAMPERED_SOURCE, engine, engine);
        // Every copy set exactly as the page: the unchanged one passes, and no change is caught.
        for (DocxFidelityCorpusTest.Change change : DocxFidelityCorpusTest.converted()) {
            java.nio.file.Files.copy(engine, dir.resolve(change.stem() + ".pdf"));
        }

        // Only the changes made to this document are held to it.
        assertThat(DocxFidelityCorpusTest.letThrough(page, engine, dir))
                .containsExactly("invoice-modern--dropped-paragraph: the gate let it through",
                        "invoice-modern--changed-font: the gate let it through",
                        "invoice-modern--extra-page-break: the gate let it through",
                        "invoice-modern--exact-card-height: the gate let it through",
                        "invoice-modern--table-as-picture: the gate let it through");

        // The unchanged copy set as another document: it vouches for nothing.
        try (DocumentSession other = DocxFidelityCorpusTest.composed(DocxFidelityCorpusTest.corpus().get(0))) {
            java.nio.file.Files.write(dir.resolve(DocxFidelityCorpusTest.TAMPERED_SOURCE + "--unchanged.pdf"),
                    other.toPdfBytes());
        }
        assertThat(DocxFidelityCorpusTest.letThrough(page, engine, dir))
                .first().asString().startsWith("invoice-modern--unchanged: converted with the changes");
    }

    @Test
    void everyDocumentChangedIsHeldToItsOwnMeasurement(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        java.nio.file.Path engine = java.nio.file.Files.createDirectories(dir.resolve("engine"));
        java.nio.file.Path set = java.nio.file.Files.createDirectories(dir.resolve("word-tampered"));
        Map<String, FidelityMeasurement> measured = new LinkedHashMap<>();
        for (String stem : DocxFidelityCorpusTest.sources()) {
            DocxCorpusDocument document = DocxFidelityCorpusTest.corpus().stream()
                    .filter(each -> each.stem().equals(stem)).findFirst().orElseThrow();
            java.nio.file.Path pdf = engine.resolve(stem + ".pdf");
            try (DocumentSession session = DocxFidelityCorpusTest.composed(document)) {
                java.nio.file.Files.write(pdf, session.toPdfBytes());
            }
            measured.put(stem, FidelityMeasurement.of(stem, pdf, pdf));
        }
        // Word set every copy exactly as its own document's page: each unchanged copy passes, and
        // every change, of each document, is let through, once.
        StringBuilder results = new StringBuilder();
        for (DocxFidelityCorpusTest.Change change : DocxFidelityCorpusTest.converted()) {
            java.nio.file.Files.copy(engine.resolve(change.source() + ".pdf"), set.resolve(change.stem() + ".pdf"));
            results.append(results.isEmpty() ? "" : ",").append("{\"source\":\"").append(change.stem())
                    .append(".docx\",\"sha256\":\"")
                    .append(WordConversion.sha256(change.tamper().applyTo(SOURCES.get(change.source())))).append("\"}");
        }
        java.nio.file.Files.writeString(set.resolve("conversion.json"),
                "{\"version\":\"16.0\",\"results\":[" + results + "]}");

        assertThat(DocxFidelityCorpusTest.changesLetThrough("word", dir, engine, measured, SOURCES, null))
                .containsExactlyElementsOf(DocxFidelityCorpusTest.tampers().stream()
                        .map(change -> change.stem() + ": the gate let it through").toList());

        // A document the run did not measure cannot have its changes held to anything.
        assertThat(DocxFidelityCorpusTest.changesLetThrough("word", dir, engine,
                Map.of(DocxFidelityCorpusTest.TAMPERED_SOURCE, measured.get(DocxFidelityCorpusTest.TAMPERED_SOURCE)),
                SOURCES, null))
                .singleElement().asString().contains("the run measured no invoice-luma_studio to hold its changes to");
    }

    @Test
    void everyPartKeepsTheTimeTheExportWroteIt() throws Exception {
        for (DocxFidelityCorpusTest.Change change : DocxFidelityCorpusTest.converted()) {
            Map<String, java.time.LocalDateTime> written = times(SOURCES.get(change.source()));
            Map<String, java.time.LocalDateTime> kept = times(change.tamper().applyTo(SOURCES.get(change.source())));
            written.forEach((name, time) -> assertThat(kept.get(name)).as("%s: %s", change.stem(), name).isEqualTo(time));
            kept.forEach((name, time) -> assertThat(time).as("%s: %s", change.stem(), name)
                    .isEqualTo(written.getOrDefault(name, written.get("word/document.xml"))));
        }
    }

    @Test
    void aPictureThatCannotBeRelatedOrDeclaredRefuses() throws Exception {
        String table = "<w:tbl><w:tr><w:tc>" + DocxFixtures.paragraph("<w:r><w:t>Line item</w:t></w:r>")
                       + "</w:tc></w:tr></w:tbl>";
        Map<String, String> parts = new LinkedHashMap<>(Map.of("word/document.xml", DocxFixtures.body(table),
                "word/_rels/document.xml.rels", "<Relationships>",
                "[Content_Types].xml", "<Types></Types>"));

        assertThatThrownBy(() -> DocxTamper.asPicture("Line item").applyTo(DocxFixtures.docx(parts)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("relationships do not close");

        parts.put("word/_rels/document.xml.rels", "<Relationships></Relationships>");
        parts.put("[Content_Types].xml", "<Types>");
        assertThatThrownBy(() -> DocxTamper.asPicture("Line item").applyTo(DocxFixtures.docx(parts)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("content types do not close");
    }

    @Test
    void changedDocumentsThatCannotBeMadeAreAnEntryNotAnException(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        FidelityMeasurement page = measured(1, Map.of("0:a", line(0.1)));

        // No source DOCX to change: making them fails, and the corpus's result must still be reported.
        Map<String, FidelityMeasurement> measured = Map.of(DocxFidelityCorpusTest.TAMPERED_SOURCE, page,
                DocxFidelityCorpusTest.LAST_CARD_SOURCE, page);
        assertThat(DocxFidelityCorpusTest.changesLetThrough("libreoffice", dir, dir, measured, Map.of(), null))
                .singleElement().asString().startsWith("the changed documents could not be made, converted or measured")
                .contains("the run exported no invoice-modern to change");
        // No record of Word's conversion of them.
        assertThat(DocxFidelityCorpusTest.changesLetThrough("word", dir, dir, measured, SOURCES, null))
                .singleElement().asString().contains("no Word conversion recorded");
    }

    @Test
    void aRowAlreadyHeldSoRefuses() throws Exception {
        byte[] held = DocxTamper.withExactRowHeight("Please include the invoice number", 500).applyTo(source);

        assertThatThrownBy(() -> DocxTamper.withExactRowHeight("Please include the invoice number", 500).applyTo(held))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("already held to exactly 500 twips");
    }

    @Test
    void theUnchangedCopyHoldsTheSameParts() throws Exception {
        Map<String, byte[]> before = parts(source);
        Map<String, byte[]> after = parts(DocxTamper.unchanged().applyTo(source));

        assertThat(after.keySet()).containsExactlyElementsOf(before.keySet());
        before.forEach((name, bytes) -> assertThat(after.get(name)).as(name).isEqualTo(bytes));
    }

    @Test
    void thePictureStandingInForTheTableIsOneWhitePixel() throws Exception {
        java.awt.image.BufferedImage pixel = javax.imageio.ImageIO.read(new ByteArrayInputStream(DocxTamper.PIXEL));

        assertThat(pixel.getWidth()).isEqualTo(1);
        assertThat(pixel.getHeight()).isEqualTo(1);
        assertThat(pixel.getRGB(0, 0) & 0xFFFFFF).isEqualTo(0xFFFFFF);
    }

    @Test
    void aChangeTheEditorWroteNoPdfOfIsLetThrough(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        FidelityMeasurement page = measured(1, Map.of("0:a", line(0.1)));

        assertThat(DocxFidelityCorpusTest.letThrough(page, dir.resolve("engine.pdf"), dir))
                .hasSize(6)
                .allMatch(why -> why.startsWith("invoice-modern--") && why.endsWith(": the editor wrote no PDF of it"));
    }

    @Test
    void theUnchangedCopyStandingApartVouchesForNothing() {
        DocxFidelityCorpusTest.Change unchanged = DocxFidelityCorpusTest.converted().get(0);
        assertThat(DocxFidelityCorpusTest.unchangedApart(unchanged, List.of())).isNull();
        assertThat(DocxFidelityCorpusTest.unchangedApart(unchanged, List.of("invoice-modern: 1 line moved")))
                .startsWith("invoice-modern--unchanged: converted with the changes")
                .endsWith("invoice-modern: 1 line moved");
    }

    @Test
    void aWordConversionOfOtherChangedDocumentsIsStale() throws Exception {
        Map<String, String> converted = new LinkedHashMap<>();
        for (DocxFidelityCorpusTest.Change change : DocxFidelityCorpusTest.converted()) {
            converted.put(change.stem() + ".docx",
                    WordConversion.sha256(change.tamper().applyTo(SOURCES.get(change.source()))));
        }
        assertThat(DocxFidelityCorpusTest.staleTampered(new WordConversion("16.0", converted), SOURCES)).isEmpty();

        converted.put("invoice-modern--changed-font.docx", WordConversion.sha256(source));
        converted.remove("invoice-modern--unchanged.docx");
        // The last card's change, made to invoice-modern rather than to its own document.
        converted.put("invoice-luma_studio--exact-card-height.docx", WordConversion.sha256(
                DocxTamper.withExactRowHeight("Please include the invoice number", 450).applyTo(source)));
        assertThat(DocxFidelityCorpusTest.staleTampered(new WordConversion("16.0", converted), SOURCES))
                .containsExactly("invoice-modern--unchanged", "invoice-modern--changed-font",
                        "invoice-luma_studio--exact-card-height");
    }

    @Test
    void thePageBreakOpensTheParagraph() throws Exception {
        String after = body(DocxTamper.withPageBreakBefore(THANKS).applyTo(source));

        assertThat(body(source)).doesNotContain("w:type=\"page\"");
        String paragraph = paragraphHolding(after, THANKS);
        assertThat(paragraph.indexOf("<w:br w:type=\"page\"/>")).isPositive()
                .isLessThan(paragraph.indexOf("<w:t"));
        assertThat(after.split("w:type=\"page\"", -1)).hasSize(2);
    }

    @Test
    void theCardsOwnRowIsHeldExactlyAndItsTablesRowsAreNot() throws Exception {
        String before = body(source);
        String after = body(DocxTamper.withExactRowHeight("Please include the invoice number", 500).applyTo(source));

        String card = DocxTamper.children(after, DocxTamper.body(after), Set.of("w:tbl")).stream()
                .map(span -> after.substring(span[0], span[1]))
                .filter(table -> DocxTamper.letters(table).contains("Please include the invoice number"))
                .findFirst().orElseThrow();
        String row = card.substring(card.indexOf("<w:tr>"));
        assertThat(row.substring(0, row.indexOf("</w:trPr>")))
                .as("the card's own row").contains("<w:trHeight w:val=\"500\" w:hRule=\"exact\"/>")
                .containsOnlyOnce("<w:trHeight");
        // The export writes the card's row no height of its own, only its tables' rows one.
        assertThat(after.replace("<w:trHeight w:val=\"500\" w:hRule=\"exact\"/>", ""))
                .as("nothing else changed, the rows of the tables it holds included").isEqualTo(before);
    }

    @Test
    void aRowHeightTheRowAlreadyHasIsReplacedNotAddedTo() throws Exception {
        String row = "<w:tr><w:trPr><w:cantSplit/><w:trHeight w:val=\"900\" w:hRule=\"atLeast\"/></w:trPr>"
                     + "<w:tc><w:tbl><w:tr><w:trPr><w:trHeight w:val=\"300\" w:hRule=\"atLeast\"/></w:trPr>"
                     + "<w:tc>" + DocxFixtures.paragraph("<w:r><w:t>Card text</w:t></w:r>") + "</w:tc></w:tr></w:tbl>"
                     + DocxFixtures.paragraph("") + "</w:tc></w:tr>";
        byte[] docx = DocxFixtures.docx(Map.of("word/document.xml", DocxFixtures.body("<w:tbl>" + row + "</w:tbl>")));

        String after = body(DocxTamper.withExactRowHeight("Card text", 400).applyTo(docx));

        assertThat(after).contains("<w:tr><w:trPr><w:trHeight w:val=\"400\" w:hRule=\"exact\"/><w:cantSplit/></w:trPr>")
                .contains("<w:trHeight w:val=\"300\" w:hRule=\"atLeast\"/>")
                .doesNotContain("w:val=\"900\"");
    }

    @Test
    void theTableIsAPictureNow() throws Exception {
        Map<String, byte[]> after = parts(DocxTamper.asPicture("Discovery workshop").applyTo(source));
        String body = new String(after.get("word/document.xml"), StandardCharsets.UTF_8);

        assertThat(DocxTamper.letters(body)).doesNotContain("Discovery workshop");
        assertThat(tables(body)).isEqualTo(tables(body(source)) - 1);
        assertThat(body).contains("r:embed=\"rIdTampered\"");
        assertThat(after).containsKey("word/media/tampered.png");
        assertThat(new String(after.get("word/_rels/document.xml.rels"), StandardCharsets.UTF_8))
                .contains("Id=\"rIdTampered\"").contains("Target=\"media/tampered.png\"");
        assertThat(new String(after.get("[Content_Types].xml"), StandardCharsets.UTF_8))
                .contains("Extension=\"png\"");
    }

    @Test
    void aChangeThatFindsNothingToChangeRefuses() {
        assertThatThrownBy(() -> DocxTamper.withoutParagraph("No paragraph holds this").applyTo(source))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("0 paragraphs");
        assertThatThrownBy(() -> DocxTamper.withPageBreakBefore("No paragraph holds this").applyTo(source))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("0 paragraphs");
        assertThatThrownBy(() -> DocxTamper.withExactRowHeight("No row holds this", 500).applyTo(source))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("0 tables");
        assertThatThrownBy(() -> DocxTamper.asPicture("No table holds this").applyTo(source))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("0 tables");
    }

    @Test
    void aChangeThatWouldChangeNothingRefuses() throws Exception {
        byte[] courier = DocxTamper.inFont("Courier New").applyTo(source);

        assertThatThrownBy(() -> DocxTamper.inFont("Courier New").applyTo(courier))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("names no font other than");
    }

    @Test
    void aChangeFindingItsLettersTwiceRefuses() {
        // The heading's table and the parties' both hold the year.
        assertThatThrownBy(() -> DocxTamper.asPicture("2026").applyTo(source))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("2 tables holding \"2026\", not one");
    }

    @Test
    void theGateLetsAChangeThroughThatMovesNothing() {
        FidelityMeasurement page = measured(1, Map.of("0:a", line(0.1), "0:b", line(0.2)));
        DocxFidelityCorpusTest.Change font = new DocxFidelityCorpusTest.Change("invoice-modern",
                DocxTamper.inFont("Courier New"));

        assertThat(DocxFidelityCorpusTest.letThrough(font, page, page, List.of()))
                .isEqualTo("invoice-modern--changed-font: the gate let it through");
        assertThat(DocxFidelityCorpusTest.letThrough(font, page, page, List.of("cv: 1 line moved"))).isNull();
    }

    @Test
    void aChangeThatCostsLinesOrAPageIsLetThroughWithoutThem() {
        FidelityMeasurement page = measured(1, Map.of("0:a", line(0.1), "0:b", line(0.2)));
        FidelityMeasurement lost = measured(1, Map.of("0:a", line(0.1)));
        FidelityMeasurement moved = measured(1, Map.of("0:a", line(0.1), "0:b", line(9)));
        FidelityMeasurement longer = measured(2, Map.of("0:a", line(0.1), "0:b", line(0.2)));
        DocxFidelityCorpusTest.Change paragraph = new DocxFidelityCorpusTest.Change("invoice-modern",
                DocxTamper.withoutParagraph(THANKS));
        DocxFidelityCorpusTest.Change pageBreak = new DocxFidelityCorpusTest.Change("invoice-modern",
                DocxTamper.withPageBreakBefore(THANKS));
        DocxFidelityCorpusTest.Change card = new DocxFidelityCorpusTest.Change("invoice-luma_studio",
                DocxTamper.withExactRowHeight(NOTES, 450));
        List<String> apart = List.of("cv: apart");

        assertThat(DocxFidelityCorpusTest.letThrough(paragraph, page, lost, apart)).isNull();
        assertThat(DocxFidelityCorpusTest.letThrough(paragraph, page, moved, apart))
                .startsWith("invoice-modern--dropped-paragraph: failed, but without LOST_LINES");
        assertThat(DocxFidelityCorpusTest.letThrough(pageBreak, page, longer, apart)).isNull();
        assertThat(DocxFidelityCorpusTest.letThrough(pageBreak, page, lost, apart))
                .startsWith("invoice-modern--extra-page-break: failed, but without PAGES");
        // A card held short of its text that only moves what follows it has not shown that it clips.
        assertThat(DocxFidelityCorpusTest.letThrough(card, page, lost, apart)).isNull();
        assertThat(DocxFidelityCorpusTest.letThrough(card, page, moved, apart))
                .startsWith("invoice-luma_studio--exact-card-height: failed, but without LOST_LINES");
    }

    private static FidelityMeasurement measured(int editorPages, Map<String, FidelityMeasurement.Found> found) {
        return new FidelityMeasurement("invoice-modern", 1, editorPages, 2, found.size(), 0, 0, 0, found);
    }

    @Test
    void aChangeThatCostsWidthIsLetThroughWhenItsLinesRunAsWideAsTheyDid() {
        FidelityMeasurement page = measured(1, Map.of("0:a", line(0.1), "0:b", line(0.2)));
        FidelityMeasurement wider = measured(1, Map.of("0:a", line(0.1), "0:b", line(0.2, 0.51)));
        FidelityMeasurement withinRounding = measured(1, Map.of("0:a", line(0.1), "0:b", line(0.2, 0.5)));
        FidelityMeasurement lost = measured(1, Map.of("0:a", line(0.1)));
        FidelityMeasurement moved = measured(1, Map.of("0:a", line(0.1), "0:b", line(9)));
        DocxFidelityCorpusTest.Change face = new DocxFidelityCorpusTest.Change("synthetic-card_over_pages",
                DocxTamper.inFont("Times New Roman").costing(DocxTamper.Cost.WIDTH));
        List<String> apart = List.of("cv: apart");

        assertThat(DocxFidelityCorpusTest.letThrough(face, page, wider, apart)).isNull();
        assertThat(DocxFidelityCorpusTest.letThrough(face, page, withinRounding, apart))
                .startsWith("synthetic-card_over_pages--changed-font: failed, but without WIDTH");
        assertThat(DocxFidelityCorpusTest.letThrough(face, page, lost, apart))
                .as("a line lost has no width to compare").startsWith("synthetic-card_over_pages--changed-font: failed, but without WIDTH");
        assertThat(DocxFidelityCorpusTest.letThrough(face, page, moved, apart))
                .startsWith("synthetic-card_over_pages--changed-font: failed, but without WIDTH");
        assertThat(DocxFidelityCorpusTest.letThrough(face, page, wider, List.of()))
                .isEqualTo("synthetic-card_over_pages--changed-font: the gate let it through");
    }

    private static FidelityMeasurement.Found line(double drift) {
        return line(drift, 0);
    }

    private static FidelityMeasurement.Found line(double drift, double wider) {
        return new FidelityMeasurement.Found(0, "line", drift, 0, wider);
    }

    private static Map<String, byte[]> parts(byte[] docx) throws Exception {
        Map<String, byte[]> parts = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                parts.put(entry.getName(), zip.readAllBytes());
            }
        }
        return parts;
    }

    private static Map<String, java.time.LocalDateTime> times(byte[] docx) throws Exception {
        Map<String, java.time.LocalDateTime> times = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                times.put(entry.getName(), entry.getTimeLocal());
            }
        }
        return times;
    }

    private static String body(byte[] docx) throws Exception {
        return new String(parts(docx).get("word/document.xml"), StandardCharsets.UTF_8);
    }

    private static String paragraphHolding(String xml, String letters) {
        return DocxTamper.children(xml, DocxTamper.body(xml), Set.of("w:p")).stream()
                .map(span -> xml.substring(span[0], span[1]))
                .filter(paragraph -> DocxTamper.letters(paragraph).contains(letters))
                .findFirst().orElseThrow();
    }

    private static String letters(String xml) {
        return DocxTamper.letters(xml);
    }

    private static int paragraphs(String xml) {
        return DocxTamper.children(xml, DocxTamper.body(xml), Set.of("w:p")).size();
    }

    private static int tables(String xml) {
        return DocxTamper.children(xml, DocxTamper.body(xml), Set.of("w:tbl")).size();
    }

    private static final Pattern R_FONTS = Pattern.compile("<w:rFonts\\b[^>]*>");

    /** How many {@code w:rFonts} each {@code word/} part holds, by part. */
    private static Map<String, Integer> rFonts(Map<String, byte[]> parts) {
        Map<String, Integer> counts = new java.util.TreeMap<>();
        for (String name : parts.keySet()) {
            if (name.startsWith("word/") && name.endsWith(".xml")) {
                counts.put(name, (int) R_FONTS.matcher(new String(parts.get(name), StandardCharsets.UTF_8))
                        .results().count());
            }
        }
        return counts;
    }

    /** Every font, theme fonts included, a {@code w:rFonts} of a {@code word/} part names. */
    private static List<String> fonts(Map<String, byte[]> parts) {
        Pattern font = Pattern.compile("w:(?:ascii|hAnsi|cs|eastAsia)(?:Theme)?=\"([^\"]+)\"");
        List<String> fonts = new java.util.ArrayList<>();
        for (String name : parts.keySet()) {
            if (!name.startsWith("word/") || !name.endsWith(".xml")) {
                continue;
            }
            Matcher elements = R_FONTS.matcher(new String(parts.get(name), StandardCharsets.UTF_8));
            while (elements.find()) {
                Matcher names = font.matcher(elements.group());
                while (names.find()) {
                    fonts.add(names.group(1));
                }
            }
        }
        return fonts;
    }
}
