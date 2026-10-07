package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.ListBuilder;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A list item the page reads as markdown is written as the page sets it: its marks dropped and
 * the text they mark in the face the page sets it in, the marker where Word or the file puts it,
 * and nothing named but a heading the page draws past its line. Each item is matched to the lines
 * the page laid it out in, which say whether it reads the item so; where they do not, or the
 * items are not matched to the layout's, an item is written as authored and named.
 */
class DocxListMarkdownTest {

    private static final String MARKS = "its items' markdown marks are written as letters, where the page sets the "
                                        + "text they mark and drops them";
    private static final DocumentTextStyle BOLD = DocumentTextStyle.builder().decoration(DocumentTextDecoration.BOLD).build();

    @Test
    void aListsItemsAreWrittenInThePiecesThePageSetsThem() throws Exception {
        Export export = export(true, list -> list.items("**Java** lead", "Kotlin",
                "a *long* item that runs on past the edge of the narrow page and wraps"));
        XWPFParagraph java = export.paragraphWith("Java");
        assertThat(java.getNumID()).as("a Word list, its marker Word's").isNotNull();
        assertThat(java.getRuns()).extracting(XWPFRun::text).containsExactly("Java", " lead");
        assertThat(java.getRuns()).extracting(XWPFRun::isBold).containsExactly(true, false);
        XWPFParagraph wrapped = export.paragraphWith("wraps");
        assertThat(wrapped.getText()).isEqualTo("a long item that runs on past the edge of the narrow page and wraps");
        assertThat(wrapped.getRuns()).filteredOn(XWPFRun::isItalic).extracting(XWPFRun::text).containsExactly("long");
        assertThat(export.paragraphWith("Kotlin").getRuns()).extracting(XWPFRun::text).containsExactly("Kotlin");
        assertThat(export.notes()).isEmpty();
    }

    @Test
    void itemsWhoseMarkersStandInAColumnOfTheirOwnAreWrittenSo() throws Exception {
        Export column = export(true, list -> list.hangingIndent(true).dash().items("**Java** lead", "`code` here"));
        assertThat(column.paragraphWith("Java").getRuns()).extracting(XWPFRun::isBold).containsExactly(true, false);
        assertThat(column.paragraphWith("code").getText()).isEqualTo("code here");
        assertThat(column.notes()).isEmpty();
        // A marker that is runs: the item is written after it, as a rich one is.
        Export drawn = export(true, list -> list.hangingIndent(true).marker(marker -> marker.plain("»"))
                .items("**Java** lead"));
        XWPFParagraph item = drawn.paragraphWith("Java");
        assertThat(item.getText()).doesNotContain("*");
        assertThat(item.getRuns()).filteredOn(XWPFRun::isBold).extracting(XWPFRun::text).containsExactly("Java");
        assertThat(drawn.notes()).noneMatch(note -> note.contains("markdown"));
    }

    @Test
    void nestedItemsAreWrittenAsThePageSetsThem() throws Exception {
        // Without hangingIndent the page lays a nested item's indent and marker out in its text,
        // and reads them with it; Word draws the marker, and the item's own text is written.
        for (boolean hanging : List.of(false, true)) {
            Export export = export(true, list -> list.hangingIndent(hanging).addItem("Languages **x**",
                    child -> child.addItem("**Java**").addItem("Kot_lin", grand -> grand.addItem("*deep* one"))));
            assertThat(export.paragraphWith("Languages").getRuns()).extracting(XWPFRun::text)
                    .containsExactly("Languages ", "x");
            XWPFParagraph java = export.paragraphWith("Java");
            assertThat(java.getNumIlvl()).hasToString("1");
            assertThat(java.getRuns()).extracting(XWPFRun::text).containsExactly("Java");
            assertThat(java.getRuns()).extracting(XWPFRun::isBold).containsExactly(true);
            assertThat(export.paragraphWith("Kot_lin").getText()).as("a mark the parser keeps").isEqualTo("Kot_lin");
            XWPFParagraph deep = export.paragraphWith("deep");
            assertThat(deep.getNumIlvl()).hasToString("2");
            assertThat(deep.getRuns()).extracting(XWPFRun::text).containsExactly("deep", " one");
            assertThat(deep.getRuns()).extracting(XWPFRun::isItalic).containsExactly(true, false);
            assertThat(export.notes()).as("hangingIndent " + hanging).noneMatch(note -> note.contains("markdown"));
        }
    }

    @Test
    void aListWrittenAsAParagraphPerItemWritesItsMarkerAndIndentBeforeThePieces() throws Exception {
        // A level with no marker makes the list no Word list: each item writes its marker and its
        // nesting indent as characters, and the pieces after them.
        Export export = export(true, list -> list.markerFor(1, ListMarker.none())
                .addItem("**Lead** item", child -> child.addItem("**Java** sub")));
        XWPFParagraph lead = export.paragraphWith("Lead");
        assertThat(lead.getNumID()).isNull();
        assertThat(lead.getRuns()).extracting(XWPFRun::text).containsExactly("• ", "Lead", " item");
        assertThat(lead.getRuns()).extracting(XWPFRun::isBold).containsExactly(false, true, false);
        assertThat(export.paragraphWith("Java").getRuns()).extracting(XWPFRun::text).containsExactly("  ", "Java", " sub");
        assertThat(export.notes()).noneMatch(note -> note.contains("markdown"));

        Export flat = export(true, list -> list.noMarker().continuationIndent("  ").items("**Java** lead", "Kotlin"));
        assertThat(flat.paragraphWith("Java").getRuns()).extracting(XWPFRun::text).containsExactly("Java", " lead");
        assertThat(flat.notes()).noneMatch(note -> note.contains("markdown"));
    }

    @Test
    void aMarkerWrittenAsCharactersIsWrittenInTheFaceThePageSetsItIn() throws Exception {
        // The page's parser sets the marker of a bold list's tree of items, read with the item,
        // regular. Written as characters, the marker takes the face the page sets it in.
        Export text = export(true, list -> list.textStyle(BOLD).markerFor(1, ListMarker.none())
                .addItem("**Lead** item", child -> child.addItem("sub")));
        assertThat(text.paragraphWith("Lead").getRuns()).extracting(XWPFRun::text).containsExactly("• ", "Lead", " item");
        assertThat(text.paragraphWith("Lead").getRuns()).extracting(XWPFRun::isBold).containsExactly(false, true, false);
        assertThat(text.notes()).noneMatch(note -> note.contains("markdown"));
        // A flat list sets its marker before the item's text, in the list's face, as Word draws it;
        // the text the parser keeps every mark of it sets regular, as it sets every piece.
        Export flat = export(true, list -> list.textStyle(BOLD).items("**Lead** item", "node_js"));
        assertThat(flat.paragraphWith("Lead").getRuns()).extracting(XWPFRun::isBold).containsExactly(true, false);
        assertThat(flat.paragraphWith("node_js").getRuns()).extracting(XWPFRun::isBold).containsExactly(false);
        assertThat(flat.notes()).isEmpty();
    }

    @Test
    void anItemWhoseMarkerWordDrawsInAnotherFaceIsWrittenAsAuthoredAndNamed() throws Exception {
        // Word draws a Word list's marker in the list's face, where the page sets it regular: the
        // item is written as authored, and named whatever marks the page keeps of it.
        Export word = export(true, list -> list.textStyle(BOLD).addItem("**Lead** item",
                child -> child.addItem("Kot_lin").addItem("Plain")));
        assertThat(word.paragraphWith("Lead").getNumID()).isNotNull();
        assertThat(word.paragraphWith("Lead").getText()).isEqualTo("**Lead** item");
        assertThat(word.paragraphWith("Kot_lin").getRuns()).extracting(XWPFRun::isBold).containsExactly(true);
        assertThat(word.notes()).singleElement().asString().endsWith("; 2 of its 3 items are written as authored, "
                + "where the page reads them as markdown and sets their markers in another face than the list's, the "
                + "face Word draws a list's marker in");
        Export one = export(true, list -> list.textStyle(BOLD).addItem("snake_case", child -> child.addItem("sub")));
        assertThat(one.notes()).singleElement().asString().endsWith("; 1 of its 2 items is written as authored, where "
                + "the page reads it as markdown and sets its marker in another face than the list's, the face Word "
                + "draws a list's marker in");
    }

    @Test
    void aLeadThePageReadsAsMarkdownLeavesTheItemWrittenAsAuthoredAndNamed() throws Exception {
        // A marker of marks, laid out in a nested item's text, is read with it: the page sets
        // neither as the item's text after its marker.
        Export export = export(true, list -> list.markerFor(0, ListMarker.custom("*a*"))
                .addItem("**Java**", child -> child.addItem("sub")));
        assertThat(export.paragraphWith("Java").getText()).isEqualTo("**Java**");
        assertThat(export.notes()).singleElement().asString().endsWith("; " + MARKS);
    }

    @Test
    void aHeadingInAnItemIsWrittenAndNamedWhereWordCutsIt() throws Exception {
        Export export = export(true, list -> list.items("# Title *x*", "Kotlin"));
        XWPFRun title = export.paragraphWith("Title").getRuns().get(0);
        assertThat(title.text()).isEqualTo("Title *x*");
        assertThat(title.isBold()).isTrue();
        assertThat(title.getFontSizeAsDouble()).isEqualTo(28.0);
        assertThat(export.notes()).singleElement().asString()
                .startsWith("written as a Word list; its items' markdown heading is written at 28pt in a line ")
                .endsWith("pt tall, as tall as the item's own line on the page: the page draws its letters past "
                          + "the line, and Word cuts their tops on screen");
    }

    @Test
    void anItemThePageReadsIntoNothingIsWrittenAsItStandsAndNamed() throws Exception {
        Export export = export(true, list -> list.items("***", "**Java**", "Kotlin"));
        assertThat(export.document().getDocument().xmlText()).contains(">***<");
        assertThat(export.paragraphWith("Java").getText()).isEqualTo("Java");
        assertThat(export.notes()).containsExactly("written as a Word list; 1 of its 3 items is written as authored, "
                                                   + "where the page reads it as markdown and sets none of its text");
        // With its marker in a column of its own, the item's line holds nothing at all.
        Export column = export(true, list -> list.hangingIndent(true).items("***", "***", "Kotlin"));
        assertThat(column.notes()).containsExactly("written as a Word list; 2 of its 3 items are written as authored, "
                                                   + "where the page reads them as markdown and sets none of their text");
    }

    @Test
    void itemsNotMatchedToTheLayoutsAreWrittenAsAuthoredAndNamed() throws Exception {
        // An item run onto the next page is laid out as a piece on each: the items no longer count
        // as many as the layout's.
        Export export = export(true, list -> list.items("Lead", "**Long** item that runs on and on. ".repeat(30)));
        assertThat(export.paragraphWith("Long").getText()).startsWith("**Long**");
        assertThat(export.notes()).singleElement().asString().endsWith("; " + MARKS);
        // A blank item a hangingIndent list draws as a marker alone is a row the export writes none of.
        Export blank = export(true, list -> list.hangingIndent(true).items("**Java**", "", "Kotlin"));
        assertThat(blank.paragraphWith("Java").getText()).isEqualTo("**Java**");
        assertThat(blank.notes()).singleElement().asString().endsWith("; " + MARKS);
    }

    @Test
    void aHangingIndentTreeWrittenAsAParagraphPerItemWritesThePiecesAfterItsMarker() throws Exception {
        // A level with no marker makes it no Word list; its markers stand in a column of their own,
        // and the pieces are the item's text alone.
        Export export = export(true, list -> list.hangingIndent(true).markerFor(1, ListMarker.none())
                .addItem("**Lead** item", child -> child.addItem("*Java* sub")));
        XWPFParagraph lead = export.paragraphWith("Lead");
        assertThat(lead.getNumID()).isNull();
        assertThat(lead.getText()).doesNotContain("*").endsWith("Lead item");
        assertThat(lead.getRuns()).filteredOn(XWPFRun::isBold).extracting(XWPFRun::text).containsExactly("Lead");
        assertThat(export.paragraphWith("Java").getRuns()).filteredOn(XWPFRun::isItalic).extracting(XWPFRun::text)
                .containsExactly("Java");
        assertThat(export.notes()).noneMatch(note -> note.contains("markdown"));
    }

    @Test
    void anItemThePageSetsAsAuthoredIsWrittenAsItStands() throws Exception {
        Export off = export(false, list -> list.items("**Java** lead", "Kotlin"));
        assertThat(off.paragraphWith("Java").getText()).as("markdown off").isEqualTo("**Java** lead");
        assertThat(off.notes()).isEmpty();
        Export kept = export(true, list -> list.items("node_js  first", "Kotlin"));
        assertThat(kept.paragraphWith("node_js").getRuns()).as("a mark the parser keeps, its white space and all")
                .extracting(XWPFRun::text).containsExactly("node_js  first");
        assertThat(kept.notes()).isEmpty();
    }

    @Test
    void anItemThePageSetsInOtherLettersIsWrittenAsAuthoredAndNamed() throws Exception {
        Export export = export(true, list -> list.textStyle(DocumentTextStyle.builder().fontName(FontName.AMIRI).build())
                .items("مرحبا **بالعالم**"));
        assertThat(export.document().getDocument().xmlText()).contains("**");
        assertThat(export.notes()).singleElement().asString().endsWith("; " + MARKS);
        // The marker the page sets before the item's first line holds marks of its own, which are
        // not the item's: its lines hold as many as the item, less the marker's none.
        Export marked = export(true, list -> list.marker("**")
                .textStyle(DocumentTextStyle.builder().fontName(FontName.AMIRI).build()).items("مرحبا *بالعالم*"));
        assertThat(marked.notes()).singleElement().asString().endsWith("; " + MARKS);
    }

    @Test
    void theFacesTheItemsPiecesAreSetInTravelWithTheDocument() throws Exception {
        Export export = export(true, list -> list.textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).build())
                .addItem("Languages", child -> child.addItem("**Java**").addItem("*Kotlin*")));
        String table = partXml(export.document(), "/word/fontTable");
        assertThat(table).contains("<w:embedRegular").contains("<w:embedBold").contains("<w:embedItalic")
                .doesNotContain("<w:embedBoldItalic");
        Export flat = export(true, list -> list.textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).build())
                .items("**Java**", "Kotlin"));
        assertThat(partXml(flat.document(), "/word/fontTable")).contains("<w:embedBold")
                .doesNotContain("<w:embedItalic");
        // Read as the page reads it: a tree's marker, read with the item, in the face the page sets
        // it in; a marker typed before an item taken off, and nothing read off what is left.
        DocumentTextStyle boldLato = DocumentTextStyle.builder().fontName(FontName.LATO)
                .decoration(DocumentTextDecoration.BOLD).build();
        Export lead = export(true, list -> list.textStyle(boldLato).markerFor(1, ListMarker.none())
                .addItem("**Lead**", child -> child.addItem("sub")));
        assertThat(partXml(lead.document(), "/word/fontTable")).contains("<w:embedRegular").contains("<w:embedBold");
        Export typed = export(true, list -> list.textStyle(boldLato).items("* Java", "* Kotlin"));
        assertThat(partXml(typed.document(), "/word/fontTable")).contains("<w:embedBold")
                .doesNotContain("<w:embedRegular");
    }

    private record Export(XWPFDocument document, DocxExportReport report) {

        XWPFParagraph paragraphWith(String text) {
            return document.getParagraphs().stream().filter(paragraph -> paragraph.getText().contains(text))
                    .findFirst().orElseThrow(() -> new AssertionError("no paragraph holds " + text));
        }

        List<String> notes() {
            return report.bySubject().getOrDefault("ListNode", List.of()).stream()
                    .map(DocxExportReport.Note::detail).toList();
        }
    }

    private static Export export(boolean markdown, Consumer<ListBuilder> list) throws Exception {
        AtomicReference<DocxExportReport> report = new AtomicReference<>();
        byte[] docx;
        try (DocumentSession session = GraphCompose.document().pageSize(300, 400).margin(DocumentInsets.of(30))
                .markdown(markdown).create()) {
            session.pageFlow(page -> page.addList(builder -> list.accept(builder.name("Skills"))));
            docx = session.export(new DocxSemanticBackend(report::set));
        }
        return new Export(new XWPFDocument(new ByteArrayInputStream(docx)), report.get());
    }

    private static String partXml(XWPFDocument document, String prefix) throws Exception {
        StringBuilder xml = new StringBuilder();
        for (PackagePart part : document.getPackage().getParts()) {
            if (part.getPartName().getName().startsWith(prefix)) {
                try (InputStream in = part.getInputStream()) {
                    xml.append(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        assertThat(xml).as("the document holds " + prefix).isNotEmpty();
        return xml.toString();
    }
}
