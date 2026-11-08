package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNumPr;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PDF ↔ DOCX list parity: the PDF and the Word export of one session set the same marker beside
 * the same text. Each test exports the session both ways and reads both: in Word, the marker
 * each item's level of the list definition draws and the item's text; on the page, the marker
 * and the text each line draws.
 *
 * <p>The nested lists are set in Lato. The page's default face is the standard Helvetica, which
 * cannot encode ◦, ▪ or →; in a face that has them, the two outputs can be compared.</p>
 */
class DocxListParityTest {

    private static final DocumentTextStyle LATO = new DocumentTextStyle(
            FontName.LATO, 12, DocumentTextDecoration.DEFAULT, DocumentColor.BLACK);

    @Test
    void nestedFallbackFollowsTheDepthCascade() throws Exception {
        Outputs outputs = export(flow -> flow
                .addList(list -> list
                        .name("Outline")
                        .textStyle(LATO)
                        .addItem("alpha", l1 -> l1
                                .addItem("beta", l2 -> l2
                                        .addItem("gamma")))));

        assertThat(outputs.word()).containsExactly(
                new Item("•", "alpha"), new Item("◦", "beta"), new Item("▪", "gamma"));
        assertThat(outputs.page()).isEqualTo(outputs.word());
        assertThat(outputs.definedLevels())
                .as("the list definition holds the cascade's levels and no others")
                .containsExactly("•", "◦", "▪");
    }

    @Test
    void explicitMarkersStillBeatTheCascade() throws Exception {
        Outputs outputs = export(flow -> flow
                .addList(list -> list
                        .name("Outline")
                        .textStyle(LATO)
                        .markerFor(1, ListMarker.custom("→"))
                        .addItem("alpha", l1 -> l1.addItem("beta"))));

        assertThat(outputs.word())
                .as("the override wins over the cascade's ◦ at this depth")
                .containsExactly(new Item("•", "alpha"), new Item("→", "beta"));
        assertThat(outputs.page()).isEqualTo(outputs.word());
    }

    @Test
    void flatItemsStripAuthorTypedMarkers() throws Exception {
        Outputs outputs = export(flow -> flow
                .addList("- dashed", "• bulleted", "* starred", "+ plussed"));

        // An author-typed marker that survived normalization would show up as a leading
        // "- " or "• " in the item's text, beside the marker the list draws.
        assertThat(outputs.word()).containsExactly(
                new Item("•", "dashed"), new Item("•", "bulleted"),
                new Item("•", "starred"), new Item("•", "plussed"));
        assertThat(outputs.page()).isEqualTo(outputs.word());
    }

    @Test
    void boldLeadIsNotMistakenForAMarker() throws Exception {
        // A session that reads no markdown sets the item as authored, its bold lead whole.
        Outputs authored = export(false, flow -> flow.addList("**bold** lead stays intact"));
        assertThat(authored.word()).containsExactly(new Item("•", "**bold** lead stays intact"));
        assertThat(authored.page()).isEqualTo(authored.word());

        // One that reads it sets the bold lead's text bold, its marks dropped, and the item is
        // written so: not one of them was taken off as a typed marker.
        Outputs read = export(flow -> flow.addList("**bold** lead stays intact"));
        assertThat(read.word()).containsExactly(new Item("•", "bold lead stays intact"));
        assertThat(read.page()).isEqualTo(read.word());
    }

    @Test
    void blankFlatItemsAreDropped() throws Exception {
        // Every paragraph Word draws a marker beside is an item here, whatever its text: a
        // numbered empty paragraph draws a marker with nothing beside it, and filtering blank
        // text before asserting would step right over it.
        Outputs outputs = export(flow -> flow.addList("kept", "", "   "));

        assertThat(outputs.word())
                .as("one item survives; the blank ones leave no marker behind")
                .containsExactly(new Item("•", "kept"));
        assertThat(outputs.page()).isEqualTo(outputs.word());
    }

    @Test
    void normalizeMarkersFalsePreservesRawItems() throws Exception {
        Outputs outputs = export(flow -> flow
                .addList(list -> list
                        .name("Raw")
                        .normalizeMarkers(false)
                        .items("- raw dash survives")));

        assertThat(outputs.word()).containsExactly(new Item("•", "- raw dash survives"));
        assertThat(outputs.page()).isEqualTo(outputs.word());
    }

    /** A list item as an output draws it: the marker beside it and its text. */
    private record Item(String marker, String text) {
    }

    /**
     * The items of one session, as Word draws them and as the page does, and the marker of every
     * level the Word list definition holds, whether an item uses it or not.
     */
    private record Outputs(List<Item> word, List<Item> page, List<String> definedLevels) {
    }

    private static Outputs export(Consumer<PageFlowBuilder> author) throws Exception {
        return export(true, author);
    }

    private static Outputs export(boolean markdown, Consumer<PageFlowBuilder> author) throws Exception {
        byte[] docx;
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(595, 842)
                .margin(DocumentInsets.of(36))
                .markdown(markdown)
                .create()) {
            var flow = session.dsl().pageFlow().name("Flow");
            author.accept(flow);
            flow.build();
            docx = session.export(new DocxSemanticBackend());
            pdf = session.toPdfBytes();
        }
        return new Outputs(wordItems(docx), pageItems(pdf), definedLevels(docx));
    }

    /** The marker of each level of the document's one list definition, in order. */
    private static List<String> definedLevels(byte[] docx) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getNumbering().getAbstractNums()).as("one list, one definition").hasSize(1);
            return document.getNumbering().getAbstractNums().get(0).getCTAbstractNum().getLvlList().stream()
                    .map(level -> level.getLvlText().getVal())
                    .toList();
        }
    }

    /**
     * Each paragraph with text or a marker, in order: the marker its level of the list definition
     * draws, or none for a paragraph outside a list.
     */
    private static List<Item> wordItems(byte[] docx) throws Exception {
        List<Item> items = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            XWPFNumbering numbering = document.getNumbering();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                CTNumPr numbered = paragraph.getCTP().getPPr() == null ? null
                        : paragraph.getCTP().getPPr().getNumPr();
                if (numbered == null) {
                    if (!paragraph.getText().isBlank()) {
                        items.add(new Item("", paragraph.getText()));
                    }
                    continue;
                }
                BigInteger abstractNum = numbering.getAbstractNumID(numbered.getNumId().getVal());
                int level = numbered.getIlvl().getVal().intValue();
                String marker = numbering.getAbstractNum(abstractNum).getCTAbstractNum()
                        .getLvlArray(level).getLvlText().getVal();
                items.add(new Item(marker, paragraph.getText()));
            }
        }
        return items;
    }

    /**
     * Each line of text the page draws, read as a marker, a space and the item's text. So a
     * fixture holds one-line items and nothing else: a line of text outside the list, or an item
     * that wraps, would read here as a marker it does not have and fail the comparison.
     */
    private static List<Item> pageItems(byte[] pdf) throws Exception {
        List<Item> items = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setLineSeparator("\n");
            stripper.setPageEnd("\n");
            for (String line : stripper.getText(document).split("\n")) {
                String drawn = line.replace((char) 0x00A0, ' ').strip();
                if (drawn.isEmpty()) {
                    continue;
                }
                int space = drawn.indexOf(' ');
                items.add(space < 0 ? new Item(drawn, "")
                        : new Item(drawn.substring(0, space), drawn.substring(space + 1)));
            }
        }
        return items;
    }
}
