package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.layout.payloads.ParagraphLine;
import com.demcha.compose.document.layout.payloads.ParagraphShapeSpan;
import com.demcha.compose.document.layout.payloads.ParagraphSpan;
import com.demcha.compose.document.layout.payloads.ParagraphTextSpan;
import com.demcha.compose.document.style.DocumentLetterSpacing;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A paragraph's text is read as the page reads its markdown, and its pieces are the page's only
 * where the lines it laid out hold their letters in their faces and sizes.
 */
class DocxMarkdownTest {

    private static final DocumentTextStyle BODY = DocumentTextStyle.builder().size(10).build();

    @Test
    void textIsReadIntoThePiecesItsMarksStyle() {
        assertThat(DocxMarkdown.read("Some **bold** and `code` text", BODY)).containsExactly(
                piece("Some ", DocumentTextDecoration.DEFAULT, 10),
                piece("bold", DocumentTextDecoration.BOLD, 10),
                piece(" and code text", DocumentTextDecoration.DEFAULT, 10));
        assertThat(DocxMarkdown.read("*a* ***b*** _c_", BODY)).containsExactly(
                piece("a", DocumentTextDecoration.ITALIC, 10),
                piece(" ", DocumentTextDecoration.DEFAULT, 10),
                piece("b", DocumentTextDecoration.BOLD_ITALIC, 10),
                piece(" ", DocumentTextDecoration.DEFAULT, 10),
                piece("c", DocumentTextDecoration.ITALIC, 10));
        assertThat(DocxMarkdown.read("Read the file_name [docs](https://x.org)", BODY)).as("a link keeps its text")
                .containsExactly(piece("Read the file_name docs", DocumentTextDecoration.DEFAULT, 10));
    }

    @Test
    void eachLineIsReadOnItsOwnAListMarkerKept() {
        assertThat(DocxMarkdown.read("# Title *now*\nnext **b**\n- dash *i*\n\n", BODY)).containsExactly(
                // The parser takes a heading's text as it stands, marks and all, bold at twice the size.
                piece("Title *now*", DocumentTextDecoration.BOLD, 20),
                piece("\nnext ", DocumentTextDecoration.DEFAULT, 10),
                piece("b", DocumentTextDecoration.BOLD, 10),
                piece("\n- dash ", DocumentTextDecoration.DEFAULT, 10),
                piece("i", DocumentTextDecoration.ITALIC, 10),
                piece("\n\n", DocumentTextDecoration.DEFAULT, 10));
    }

    @Test
    void thePiecesTakeTheParsersFaceNotTheParagraphs() {
        DocumentTextStyle bold = DocumentTextStyle.builder().size(10).decoration(DocumentTextDecoration.BOLD).build();
        assertThat(DocxMarkdown.read("file_name *x*", bold)).containsExactly(
                piece("file_name ", DocumentTextDecoration.DEFAULT, 10),
                piece("x", DocumentTextDecoration.ITALIC, 10));
        // A list marker is the paragraph's, face and all.
        assertThat(DocxMarkdown.read("* *x*", bold)).containsExactly(
                piece("* ", DocumentTextDecoration.BOLD, 10),
                piece("x", DocumentTextDecoration.ITALIC, 10));
    }

    @Test
    void aHeadingKeepsTheParagraphsTrackingInPoints() {
        DocumentTextStyle tracked = DocumentTextStyle.builder().size(10)
                .letterSpacing(DocumentLetterSpacing.ofFontSize(0.1)).build();
        List<DocxMarkdown.Piece> pieces = DocxMarkdown.read("# Head\nbody *x*", tracked);
        assertThat(pieces.get(0).style().size()).isEqualTo(20);
        assertThat(pieces.get(0).style().letterSpacing()).isEqualTo(DocumentLetterSpacing.points(1.0));
        assertThat(pieces.get(1).style().letterSpacing()).as("the body's own").isEqualTo(tracked.letterSpacing());
    }

    @Test
    void thePiecesAreThePagesWhereItsLinesHoldThemSoAndNotOtherwise() {
        List<DocxMarkdown.Piece> pieces = DocxMarkdown.read("Some **bold** text", BODY);
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(line(span("Some", TextDecoration.DEFAULT, 10),
                span(" ", TextDecoration.DEFAULT, 10), span("bold", TextDecoration.BOLD, 10),
                span(" text", TextDecoration.DEFAULT, 10))), "", false)).isTrue();
        // Broken over two lines, the space at the break dropped.
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(line(span("Some ", TextDecoration.DEFAULT, 10),
                span("bold", TextDecoration.BOLD, 10)), line(span("text", TextDecoration.DEFAULT, 10))), "", false)).isTrue();
        // An auto-sized paragraph's text, at a size its style does not hold, in proportion.
        List<ParagraphLine> fitted = List.of(line(span("A *b*", TextDecoration.BOLD, 14)),
                line(span("c", TextDecoration.DEFAULT, 7)));
        assertThat(DocxMarkdown.laidOutIn(DocxMarkdown.read("# A *b*\nc", BODY), fitted, "", true)).isTrue();
        assertThat(DocxMarkdown.laidOutIn(DocxMarkdown.read("# A *b*\nc", BODY), fitted, "", false))
                .as("in proportion, where the page fits no size of its own").isFalse();
        // A prefix the page sets before the first line leads its letters.
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(line(span("• ", TextDecoration.DEFAULT, 10),
                span("Some ", TextDecoration.DEFAULT, 10), span("bold", TextDecoration.BOLD, 10),
                span(" text", TextDecoration.DEFAULT, 10))), "• ", false)).isTrue();
        // Marks alone the page sets as nothing, which are not taken for the page's.
        assertThat(DocxMarkdown.read("***", BODY)).isEmpty();
        assertThat(DocxMarkdown.laidOutIn(List.of(), List.of(line()), "", false)).isFalse();

        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(line(span("Some **bold** text", TextDecoration.DEFAULT, 10))),
                "", false)).as("the marks laid out: the session reads no markdown").isFalse();
        assertThat(DocxMarkdown.laidOutIn(List.of(), List.of(line(span("***", TextDecoration.DEFAULT, 10))), "", false))
                .as("marks alone laid out").isFalse();
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(line(span("Some bold text", TextDecoration.DEFAULT, 10))),
                "", false)).as("another face").isFalse();
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(line(span("Some ", TextDecoration.DEFAULT, 10),
                span("bold", TextDecoration.BOLD, 12), span(" text", TextDecoration.DEFAULT, 10))), "", true))
                .as("sizes out of proportion").isFalse();
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(line(span("Some ", TextDecoration.DEFAULT, 10),
                span("bold", TextDecoration.BOLD, 10), span(" text", TextDecoration.DEFAULT, 10, FontName.COURIER,
                        Color.BLACK))), "", false)).as("another family").isFalse();
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(line(span("Some ", TextDecoration.DEFAULT, 10),
                span("bold", TextDecoration.BOLD, 10), span(" text", TextDecoration.DEFAULT, 10, BODY.fontName(),
                        Color.RED))), "", false)).as("another colour").isFalse();
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(line(span("Some ", TextDecoration.DEFAULT, 10),
                span("bold", TextDecoration.BOLD, 10), new ParagraphTextSpan(" text",
                        new TextStyle(BODY.fontName(), 10, TextDecoration.DEFAULT, BODY.color().color(), 0.5), 25, 10,
                        null, null, false))), "", false)).as("another tracking").isFalse();
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(line(span("Some ", TextDecoration.DEFAULT, 10),
                span("bolt", TextDecoration.BOLD, 10), span(" text", TextDecoration.DEFAULT, 10))), "", false))
                .as("another letter").isFalse();
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(line(span("Some ", TextDecoration.DEFAULT, 10),
                span("bold", TextDecoration.BOLD, 10))), "", false)).as("a letter short").isFalse();
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(line(span("- Some ", TextDecoration.DEFAULT, 10),
                span("bold", TextDecoration.BOLD, 10), span(" text", TextDecoration.DEFAULT, 10))), "• ", false))
                .as("a prefix of other letters").isFalse();
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(), "", false)).as("no lines").isFalse();
        List<ParagraphSpan> withAPicture = new ArrayList<>(line(span("Some ", TextDecoration.DEFAULT, 10),
                span("bold", TextDecoration.BOLD, 10), span(" text", TextDecoration.DEFAULT, 10)).spans());
        withAPicture.add(new ParagraphShapeSpan(List.of(), 4, 4, null, 0, null));
        assertThat(DocxMarkdown.laidOutIn(pieces, List.of(new ParagraphLine("", 0, 10, 10, 8, 2, withAPicture)), "", false))
                .as("anything but text").isFalse();
    }

    @Test
    void piecesAreSplitAtTheEndOfTheLeadTheyOpenWith() {
        // A nested item's indent and marker, laid out in its text: no-break spaces are letters.
        String indent = Character.toString(0x00A0).repeat(2);
        List<DocxMarkdown.Piece> pieces = DocxMarkdown.read(indent + "◦ **Java** lead", BODY);
        DocxMarkdown.Split split = DocxMarkdown.split(pieces, indent + "◦ ");
        assertThat(split.lead()).isEqualTo(BODY);
        assertThat(split.after()).containsExactly(piece("Java", DocumentTextDecoration.BOLD, 10),
                piece(" lead", DocumentTextDecoration.DEFAULT, 10));
        // The lead may end inside a piece and span pieces of one style.
        assertThat(DocxMarkdown.split(List.of(piece("- ", DocumentTextDecoration.DEFAULT, 10),
                piece("a b", DocumentTextDecoration.DEFAULT, 10)), "- a").after())
                .containsExactly(piece(" b", DocumentTextDecoration.DEFAULT, 10));
        // No lead leaves the pieces whole.
        assertThat(DocxMarkdown.split(pieces, "")).isEqualTo(new DocxMarkdown.Split(null, pieces));

        assertThat(DocxMarkdown.split(DocxMarkdown.read("*a* **Java**", BODY), "*a* "))
                .as("a lead the parser reads, its marks dropped").isNull();
        assertThat(DocxMarkdown.split(List.of(piece("-", DocumentTextDecoration.DEFAULT, 10),
                piece(" x", DocumentTextDecoration.BOLD, 10), piece(" y", DocumentTextDecoration.DEFAULT, 10)), "- x"))
                .as("a lead in two styles").isNull();
        assertThat(DocxMarkdown.split(pieces, indent + "▪ ")).as("another lead").isNull();
        assertThat(DocxMarkdown.split(List.of(piece("◦ ab", DocumentTextDecoration.DEFAULT, 10),
                piece("c", DocumentTextDecoration.BOLD, 10)), "▪ ")).as("another lead, in a longer piece").isNull();
        assertThat(DocxMarkdown.split(List.of(piece("◦ ", DocumentTextDecoration.DEFAULT, 10)), "◦ "))
                .as("nothing after the lead").isNull();
        assertThat(DocxMarkdown.split(List.of(piece("◦", DocumentTextDecoration.DEFAULT, 10)), "◦ "))
                .as("pieces shorter than the lead").isNull();
    }

    @Test
    void whatThePageMayReadAsMarkdownHoldsAMarkOfEmphasisOrCode() {
        assertThat(DocxMarkdown.holdsAMark("a *b*")).isTrue();
        assertThat(DocxMarkdown.holdsAMark("snake_case")).isTrue();
        assertThat(DocxMarkdown.holdsAMark("`x`")).isTrue();
        assertThat(DocxMarkdown.holdsAMark("# Title [link](u)")).as("a heading or a link alone").isFalse();
        assertThat(DocxMarkdown.holdsAMark(null)).isFalse();
    }

    private static DocxMarkdown.Piece piece(String text, DocumentTextDecoration face, double size) {
        return new DocxMarkdown.Piece(text, new DocumentTextStyle(BODY.fontName(), size, face, BODY.color(),
                BODY.letterSpacing()));
    }

    private static ParagraphTextSpan span(String text, TextDecoration face, double size) {
        return span(text, face, size, BODY.fontName(), BODY.color().color());
    }

    private static ParagraphTextSpan span(String text, TextDecoration face, double size, FontName family, Color color) {
        return new ParagraphTextSpan(text, new TextStyle(family, size, face, color), text.length() * 5.0,
                size, null, null, false);
    }

    private static ParagraphLine line(ParagraphSpan... spans) {
        return new ParagraphLine("", 100, 12, 12, 9, 3, List.of(spans));
    }
}
