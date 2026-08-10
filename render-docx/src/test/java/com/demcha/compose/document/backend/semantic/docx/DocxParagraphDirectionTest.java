package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextDirection;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the one thing Word needs told and cannot work out.
 *
 * <p>Word has its own bidirectional engine, so the text stays in logical order and the
 * reordering and Arabic joining are its job, not ours — the opposite of the fixed-layout
 * backends. What it cannot infer is the paragraph's base direction: without
 * {@code w:bidi} a line that begins with a neutral character, or one that mixes scripts,
 * is laid out as left-to-right text that happens to contain Hebrew. The document opens,
 * the letters are all there, and the line reads from the wrong end.</p>
 */
class DocxParagraphDirectionTest {

    private static final String HEBREW = "שלום עולם";

    @Test
    void aRightToLeftParagraphIsMarkedForWord() throws Exception {
        List<XWPFParagraph> paragraphs = paragraphsOf(export(HEBREW, TextDirection.RTL));

        assertThat(paragraphs).isNotEmpty();
        assertThat(paragraphs.get(0).getCTP().getPPr().isSetBidi())
                .describedAs("w:bidi is what tells Word the paragraph runs right to left")
                .isTrue();
    }

    @Test
    void theTextItselfIsHandedOverUnchanged() throws Exception {
        List<XWPFParagraph> paragraphs = paragraphsOf(export(HEBREW, TextDirection.RTL));

        assertThat(paragraphs.get(0).getText())
                .describedAs("reversing here as well would undo what Word does for itself")
                .isEqualTo(HEBREW);
    }

    @Test
    void aLeftToRightParagraphCarriesNoDirectionMarkup() throws Exception {
        XWPFParagraph paragraph = paragraphsOf(export("Hello world", TextDirection.LTR)).get(0);

        assertThat(paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetBidi())
                .describedAs("every existing document must export exactly as it did")
                .isFalse();
    }

    @Test
    void autoIsNotPassedOnForWordToGuessAgain() throws Exception {
        XWPFParagraph paragraph = paragraphsOf(export(HEBREW, TextDirection.AUTO)).get(0);

        assertThat(paragraph.getCTP().isSetPPr() && paragraph.getCTP().getPPr().isSetBidi())
                .describedAs("AUTO was already resolved into a concrete alignment when the node "
                        + "was built; letting Word guess again could reach a different answer "
                        + "than the page did")
                .isFalse();
    }

    private static byte[] export(String text, TextDirection direction) throws Exception {
        try (DocumentSession document = GraphCompose.document().create()) {
            document.pageFlow(page -> page
                    .addParagraph(p -> p.text(text).direction(direction)));
            return document.export(new DocxSemanticBackend());
        }
    }

    private static List<XWPFParagraph> paragraphsOf(byte[] docx) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            return List.copyOf(document.getParagraphs());
        } catch (Exception e) {
            throw new AssertionError("the export must be a readable .docx", e);
        }
    }
}
