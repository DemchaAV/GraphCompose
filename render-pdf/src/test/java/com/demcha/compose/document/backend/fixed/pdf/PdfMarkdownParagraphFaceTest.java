package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The font a markdown paragraph is drawn in, read off the page: a decorated paragraph whose text
 * holds a mark the session reads markdown on keeps its face.
 *
 * <p>Markdown is on by default and is read as soon as the text holds {@code *}, {@code _} or a
 * backtick, so an underscore inside a word is enough to send a paragraph down that path.</p>
 */
class PdfMarkdownParagraphFaceTest {

    @Test
    void aDecoratedParagraphHoldingAMarkIsDrawnInItsOwnFace() throws Exception {
        Map<String, TreeSet<String>> fonts = fontsByWord(
                paragraph(DocumentTextDecoration.BOLD, "Boldp snake_case_name"),
                paragraph(DocumentTextDecoration.BOLD_ITALIC, "Bolditalicp BOLD_ITALIC"),
                paragraph(DocumentTextDecoration.BOLD, "Mixedp *Emph* Tail"),
                paragraph(DocumentTextDecoration.DEFAULT, "Plainp _Em_ *Em2* snake_case"));

        assertThat(fonts.get("snake_case_name")).as("bold, though the text holds an underscore")
                .containsExactly("Lato-Bold");
        assertThat(fonts.get("BOLD_ITALIC")).containsExactly("Lato-BoldItalic");
        assertThat(fonts.get("Emph")).as("emphasis added to the paragraph's bold")
                .containsExactly("Lato-BoldItalic");
        assertThat(fonts.get("Tail")).containsExactly("Lato-Bold");
        // A paragraph in the default face reads its marks as it always did.
        assertThat(fonts.get("Em")).containsExactly("Lato-Italic");
        assertThat(fonts.get("Em2")).containsExactly("Lato-Italic");
        assertThat(fonts.get("snake_case")).containsExactly("Lato-Regular");
    }

    private record Paragraph(DocumentTextDecoration decoration, String text) {
    }

    private static Paragraph paragraph(DocumentTextDecoration decoration, String text) {
        return new Paragraph(decoration, text);
    }

    /** Each word drawn, with the fonts its glyphs were drawn in, subset prefixes taken off. */
    private static Map<String, TreeSet<String>> fontsByWord(Paragraph... paragraphs) throws IOException {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document().pageSize(600, 400)
                .margin(DocumentInsets.of(24)).create()) {
            session.pageFlow(page -> {
                for (Paragraph paragraph : paragraphs) {
                    page.addParagraph(p -> p.text(paragraph.text())
                            .textStyle(DocumentTextStyle.builder().fontName(FontName.LATO).size(14)
                                    .decoration(paragraph.decoration()).build()));
                }
            });
            pdf = session.toPdfBytes();
        }
        Map<String, TreeSet<String>> words = new LinkedHashMap<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) {
                    StringBuilder word = new StringBuilder();
                    TreeSet<String> faces = new TreeSet<>();
                    for (TextPosition position : positions) {
                        String unicode = position.getUnicode();
                        if (unicode.isBlank()) {
                            flush(word, faces, words);
                            continue;
                        }
                        word.append(unicode);
                        String name = position.getFont().getName();
                        faces.add(name.contains("+") ? name.substring(name.indexOf('+') + 1) : name);
                    }
                    flush(word, faces, words);
                }
            };
            stripper.getText(document);
        }
        return words;
    }

    private static void flush(StringBuilder word, TreeSet<String> faces, Map<String, TreeSet<String>> into) {
        if (!word.isEmpty()) {
            into.computeIfAbsent(word.toString(), key -> new TreeSet<>()).addAll(faces);
        }
        word.setLength(0);
        faces.clear();
    }
}
