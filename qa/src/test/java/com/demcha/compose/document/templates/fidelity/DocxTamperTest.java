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

    private static byte[] source;

    @BeforeAll
    static void exportTheSource() throws Exception {
        DocxCorpusDocument document = DocxFidelityCorpusTest.corpus().stream()
                .filter(each -> each.stem().equals(DocxFidelityCorpusTest.TAMPERED_SOURCE)).findFirst().orElseThrow();
        try (DocumentSession session = DocxFidelityCorpusTest.composed(document)) {
            source = session.export(DocxSemanticBackend.builder().deterministic(true).build());
        }
    }

    @Test
    void everyChangeTheRunMakesStillOpensAsXmlAndIsMadeTheSameEachTime() throws Exception {
        assertThat(DocxFidelityCorpusTest.tampers()).extracting(DocxTamper::name).containsExactly(
                "dropped-paragraph", "changed-font", "extra-page-break", "exact-card-height", "table-as-picture");
        for (DocxTamper tamper : DocxFidelityCorpusTest.tampers()) {
            byte[] changed = tamper.applyTo(source);
            assertThat(changed).as(tamper.name()).isNotEqualTo(source);
            assertThat(tamper.applyTo(source)).as("%s, made again", tamper.name()).isEqualTo(changed);
            Map<String, byte[]> parts = parts(changed);
            for (Map.Entry<String, byte[]> part : parts.entrySet()) {
                if (part.getKey().endsWith(".xml") || part.getKey().endsWith(".rels")) {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    factory.newDocumentBuilder().parse(new ByteArrayInputStream(part.getValue()));
                }
            }
            assertThat(parts.keySet()).as(tamper.name()).containsAll(parts(source).keySet());
        }
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
                        + "<w:lang w:val=\"en-GB\" w:eastAsia=\"zh-CN\"/></w:rPr></w:rPrDefault></w:docDefaults></w:styles>";
        byte[] docx = DocxFixtures.docx(Map.of("word/document.xml", DocxFixtures.body(""), "word/styles.xml", styles));

        String after = new String(parts(DocxTamper.inFont("Courier New").applyTo(docx)).get("word/styles.xml"),
                StandardCharsets.UTF_8);

        assertThat(after).doesNotContain("Theme").doesNotContain("Calibri")
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
        for (DocxTamper tamper : DocxFidelityCorpusTest.converted()) {
            java.nio.file.Files.copy(engine, dir.resolve(DocxFidelityCorpusTest.tamperedStem(tamper) + ".pdf"));
        }

        assertThat(DocxFidelityCorpusTest.letThrough(page, engine, dir))
                .containsExactlyElementsOf(DocxFidelityCorpusTest.tampers().stream()
                        .map(tamper -> tamper.name() + ": the gate let it through").toList());

        // The unchanged copy set as another document: it vouches for nothing.
        try (DocumentSession other = DocxFidelityCorpusTest.composed(DocxFidelityCorpusTest.corpus().get(0))) {
            java.nio.file.Files.write(dir.resolve(DocxFidelityCorpusTest.TAMPERED_SOURCE + "--unchanged.pdf"),
                    other.toPdfBytes());
        }
        assertThat(DocxFidelityCorpusTest.letThrough(page, engine, dir))
                .first().asString().startsWith("unchanged: converted with the changes");
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
                .hasSize(DocxFidelityCorpusTest.converted().size())
                .allMatch(why -> why.endsWith(": the editor wrote no PDF of it"));
    }

    @Test
    void theUnchangedCopyStandingApartVouchesForNothing() {
        assertThat(DocxFidelityCorpusTest.unchangedApart(List.of())).isNull();
        assertThat(DocxFidelityCorpusTest.unchangedApart(List.of("invoice-modern: 1 line moved")))
                .startsWith("unchanged: converted with the changes").endsWith("invoice-modern: 1 line moved");
    }

    @Test
    void aWordConversionOfOtherChangedDocumentsIsStale() throws Exception {
        Map<String, String> converted = new LinkedHashMap<>();
        for (DocxTamper tamper : DocxFidelityCorpusTest.converted()) {
            converted.put(DocxFidelityCorpusTest.tamperedStem(tamper) + ".docx",
                    WordConversion.sha256(tamper.applyTo(source)));
        }
        assertThat(DocxFidelityCorpusTest.staleTampered(new WordConversion("16.0", converted), source)).isEmpty();

        converted.put("invoice-modern--changed-font.docx", WordConversion.sha256(source));
        converted.remove("invoice-modern--unchanged.docx");
        assertThat(DocxFidelityCorpusTest.staleTampered(new WordConversion("16.0", converted), source))
                .containsExactly("invoice-modern--unchanged", "invoice-modern--changed-font");
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
        DocxTamper font = DocxTamper.inFont("Courier New");

        assertThat(DocxFidelityCorpusTest.letThrough(font, page, page, List.of()))
                .isEqualTo("changed-font: the gate let it through");
        assertThat(DocxFidelityCorpusTest.letThrough(font, page, page, List.of("cv: 1 line moved"))).isNull();
    }

    @Test
    void aChangeThatCostsLinesOrAPageIsLetThroughWithoutThem() {
        FidelityMeasurement page = measured(1, Map.of("0:a", line(0.1), "0:b", line(0.2)));
        FidelityMeasurement lost = measured(1, Map.of("0:a", line(0.1)));
        FidelityMeasurement moved = measured(1, Map.of("0:a", line(0.1), "0:b", line(9)));
        FidelityMeasurement longer = measured(2, Map.of("0:a", line(0.1), "0:b", line(0.2)));
        DocxTamper paragraph = DocxTamper.withoutParagraph(THANKS);
        DocxTamper pageBreak = DocxTamper.withPageBreakBefore(THANKS);
        List<String> apart = List.of("cv: apart");

        assertThat(DocxFidelityCorpusTest.letThrough(paragraph, page, lost, apart)).isNull();
        assertThat(DocxFidelityCorpusTest.letThrough(paragraph, page, moved, apart))
                .startsWith("dropped-paragraph: failed, but without LOST_LINES");
        assertThat(DocxFidelityCorpusTest.letThrough(pageBreak, page, longer, apart)).isNull();
        assertThat(DocxFidelityCorpusTest.letThrough(pageBreak, page, lost, apart))
                .startsWith("extra-page-break: failed, but without PAGES");
    }

    private static FidelityMeasurement measured(int editorPages, Map<String, FidelityMeasurement.Found> found) {
        return new FidelityMeasurement("invoice-modern", 1, editorPages, 2, found.size(), 0, 0, 0, found);
    }

    private static FidelityMeasurement.Found line(double drift) {
        return new FidelityMeasurement.Found(0, "line", drift, 0);
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
