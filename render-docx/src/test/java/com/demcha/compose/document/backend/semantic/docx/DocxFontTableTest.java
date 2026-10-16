package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.TableBuilder;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentHeaderFooterZone;
import com.demcha.compose.document.output.DocumentPageZone;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.font.FontName;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The document ships the faces it is set in.
 *
 * <p>A face was named and never shipped, so on a machine without it installed the reader
 * saw a substituted one — measured here, where Lato is not installed, Word swapped it. A
 * substituted face has different glyph widths, so every line breaks somewhere else and the
 * geometry above it stops meaning anything.</p>
 *
 * <p>What is shipped is narrow on purpose: the faces the document uses, and only those. One
 * family's four faces are about two and a half megabytes, which is not a price a document
 * setting a single line in it should pay.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxFontTableTest {

    @Test
    void aBundledFamilyTravelsWithTheDocument() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .text("Set in Lato")
                .textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).build())))) {

            String table = fontTable(document);
            assertThat(table).contains("w:name=\"Lato\"").contains("<w:embedRegular");
            assertThat(fontParts(document))
                    .as("one face used, one face shipped")
                    .hasSize(1);
        }
    }

    @Test
    void onlyTheFacesTheDocumentUsesAreShipped() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Regular")
                        .textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).build()))
                .addParagraph(p -> p.text("Bold")
                        .textStyle(DocumentTextStyle.builder().fontName(FontName.LATO)
                                .decoration(DocumentTextDecoration.BOLD).build())))) {

            String table = fontTable(document);
            assertThat(table).contains("<w:embedRegular").contains("<w:embedBold");
            assertThat(table)
                    .as("nothing asked for italic, so italic does not travel")
                    .doesNotContain("<w:embedItalic");
            assertThat(fontParts(document)).hasSize(2);
        }
    }

    @Test
    void aStandardFaceShipsNothingBecauseThereIsNothingToShip() throws Exception {
        // Helvetica is a name, not a file: nothing bundles it, and a reader opening the
        // document gets Word's substitution — the same one a PDF viewer applies.
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .text("Set in Helvetica")
                .textStyle(DocumentTextStyle.builder().fontName(FontName.HELVETICA).build())))) {

            assertThat(fontParts(document)).isEmpty();
            assertThat(partNames(document))
                    .as("and no font table either, rather than an empty one")
                    .noneMatch(name -> name.contains("fontTable"));
        }
    }

    @Test
    void aFaceSetOnlyInATablesCellBuiltFromANodeTravelsToo() throws Exception {
        // A cell built from a node keeps it in the cell, not among its table's children: read by
        // the children alone, the faces of the cells — a nested table's among them — were named on
        // their runs and never shipped.
        try (XWPFDocument document = export(page -> page
                .addParagraph("Invoice", face(FontName.LATO))
                .addTable(t -> t.autoColumns(1).rowCells(DocumentTableCell.node(new ParagraphBuilder()
                        .text("Consulting").textStyle(face(FontName.POPPINS)).build())))
                .addTable(t -> t.autoColumns(1).rowCells(DocumentTableCell.node(new TableBuilder().autoColumns(1)
                        .rowCells(DocumentTableCell.node(new ParagraphBuilder()
                                .text("Nested").textStyle(face(FontName.PT_SERIF)).build()))
                        .build()))))) {

            assertThat(families(document)).containsExactlyInAnyOrder("Lato", "Poppins", "PT Serif");
        }
    }

    @Test
    void theFacesThePageZonesAndBandsAreSetInTravelToo() throws Exception {
        // The header and footer parts are written from the zones and the bands of text, not from
        // the document's tree.
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(400, 600)
                .margin(DocumentInsets.of(20)).create()) {
            session.chrome().zone(DocumentPageZone.footer(24, page -> new ParagraphBuilder().text("Acme")
                    .textStyle(face(FontName.BARLOW)).build()));
            session.chrome().zone(DocumentPageZone.header(24, page -> page.pageNumber(face(FontName.UBUNTU))));
            session.chrome().footer(DocumentHeaderFooter.builder().zone(DocumentHeaderFooterZone.FOOTER)
                    .fontName(FontName.ARSENAL).centerText("Page {page}").build());
            session.pageFlow(page -> page.addParagraph("Invoice", face(FontName.LATO)));
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(families(document)).containsExactlyInAnyOrder("Lato", "Barlow", "Ubuntu", "Arsenal");
        }
    }

    @Test
    void theFacesAZoneIsSetInOnItsFirstPageAndOnTheRestBothTravel() throws Exception {
        // Built otherwise for the first page, the zone is written into the first page's footer and
        // the others': each is set in a face of its own.
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(300, 200)
                .margin(DocumentInsets.of(30)).create()) {
            session.chrome().zone(DocumentPageZone.footer(20, page -> new ParagraphBuilder()
                    .text(page.isFirst() ? "Cover" : "Running")
                    .textStyle(face(page.isFirst() ? FontName.BARLOW : FontName.UBUNTU)).build()));
            session.pageFlow(page -> {
                for (int line = 0; line < 12; line++) {
                    page.addParagraph("Line " + line, face(FontName.LATO));
                }
            });
            docx = session.export(new DocxSemanticBackend());
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(families(document)).containsExactlyInAnyOrder("Lato", "Barlow", "Ubuntu");
        }
    }

    @Test
    void aPageReferenceAndTheRunsOfAListsItemsAndMarkersShipTheirFaces() throws Exception {
        try (XWPFDocument document = export(page -> page
                .addParagraph(p -> p.text("Terms").anchor("terms").textStyle(face(FontName.LATO)))
                .addPageReference("terms", face(FontName.CARLITO), TextAlign.LEFT)
                // A list of plain items keeps its marker of runs as its own; one of items made of
                // runs gives each item its marker.
                .addList(list -> list.textStyle(face(FontName.LATO)).hangingIndent(true)
                        .marker(marker -> marker.style("*", face(FontName.FIRA_SANS)))
                        .items("plain"))
                .addList(list -> list.textStyle(face(FontName.LATO)).hangingIndent(true)
                        .markerFor(1, ListMarker.ofRuns(List.of(new InlineTextRun("-", face(FontName.UBUNTU)))))
                        .addItem(item -> item.plain("plain ").style("rich", face(FontName.ARSENAL)),
                                body -> body.addItem(item -> item.style("deeper", face(FontName.CRIMSON_TEXT))))))) {

            assertThat(families(document)).containsExactlyInAnyOrder(
                    "Lato", "Carlito", "Fira Sans", "Arsenal", "Ubuntu", "Crimson Text");
        }
    }

    @Test
    void theShippedFaceIsTheRealFontBehindItsScrambledHeader() throws Exception {
        try (XWPFDocument document = export(page -> page.addParagraph(p -> p
                .text("Set in Lato")
                .textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).build())))) {

            byte[] part = fontParts(document).get(0);
            byte[] source = latoRegular();

            assertThat(part).hasSameSizeAs(source);
            assertThat(java.util.Arrays.copyOfRange(part, 0, 32))
                    .as("the header is not the font's own")
                    .isNotEqualTo(java.util.Arrays.copyOfRange(source, 0, 32));
            assertThat(java.util.Arrays.copyOfRange(part, 32, part.length))
                    .as("and everything past it is, byte for byte")
                    .isEqualTo(java.util.Arrays.copyOfRange(source, 32, source.length));

            // Unscrambling with the key the table states gives the font back, which is the
            // only thing that makes the part usable.
            java.util.UUID key = java.util.UUID.fromString(
                    fontTable(document).replaceAll("(?s).*w:fontKey=\"\\{([^}]+)\\}\".*", "$1"));
            assertThat(DocxFontEmbedding.obfuscate(part, key).bytes()).isEqualTo(source);
        }
    }

    private static byte[] latoRegular() throws Exception {
        try (InputStream in = DocxFontTableTest.class.getClassLoader()
                .getResourceAsStream("fonts/google/lato/Lato-Regular.ttf")) {
            assertThat(in).as("the bundled font artifact is on the test classpath").isNotNull();
            return in.readAllBytes();
        }
    }

    private static String fontTable(XWPFDocument document) throws Exception {
        PackagePart part = document.getPackage()
                .getPart(org.apache.poi.openxml4j.opc.PackagingURIHelper
                        .createPartName("/word/fontTable.xml"));
        assertThat(part).as("the document carries a font table").isNotNull();
        try (InputStream in = part.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** The families the font table ships faces for. */
    private static List<String> families(XWPFDocument document) throws Exception {
        java.util.regex.Matcher name = java.util.regex.Pattern.compile("<w:font w:name=\"([^\"]+)\"")
                .matcher(fontTable(document));
        List<String> families = new java.util.ArrayList<>();
        while (name.find()) {
            families.add(name.group(1));
        }
        return families;
    }

    private static DocumentTextStyle face(FontName name) {
        return DocumentTextStyle.builder().fontName(name).size(10).build();
    }

    private static List<byte[]> fontParts(XWPFDocument document) throws Exception {
        return document.getPackage().getParts().stream()
                .filter(part -> part.getPartName().getName().startsWith("/word/fonts/"))
                .map(part -> {
                    try (InputStream in = part.getInputStream()) {
                        return in.readAllBytes();
                    } catch (Exception failure) {
                        throw new IllegalStateException(failure);
                    }
                })
                .toList();
    }

    private static List<String> partNames(XWPFDocument document) throws Exception {
        return document.getPackage().getParts().stream()
                .map(part -> part.getPartName().getName())
                .toList();
    }

    private static XWPFDocument export(Consumer<PageFlowBuilder> content) throws Exception {
        return DocxExports.withLayout(400, 600, 20, content);
    }
}
