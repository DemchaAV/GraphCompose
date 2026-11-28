package com.demcha.compose.engine.text.markdown;

import com.demcha.compose.engine.components.content.text.TextDataBody;
import com.demcha.compose.engine.components.content.text.TextDecoration;
import com.demcha.compose.engine.components.content.text.TextStyle;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The face each piece of a paragraph read as markdown is set in: the paragraph's own, with the
 * emphasis around the piece added to it.
 *
 * <p>The marks themselves are read as CommonMark reads them — an underscore inside a word opens no
 * emphasis, an asterisk inside one does — and the paragraph's face is where every piece starts, so
 * a bold paragraph holding one of those marks stays bold.</p>
 */
class MarkDownParserTest {

    private static List<TextDataBody> read(String text, TextDecoration paragraph) {
        return new MarkDownParser().getBody(text,
                new TextStyle(FontName.HELVETICA, 12, paragraph, Color.BLACK));
    }

    /** The pieces as text and face, runs of one face side by side joined. */
    private static List<org.assertj.core.groups.Tuple> faces(String text, TextDecoration paragraph) {
        List<String> texts = new ArrayList<>();
        List<TextDecoration> decorations = new ArrayList<>();
        for (TextDataBody body : read(text, paragraph)) {
            int last = decorations.size() - 1;
            if (last >= 0 && decorations.get(last) == body.textStyle().decoration()) {
                texts.set(last, texts.get(last) + body.text());
            } else {
                texts.add(body.text());
                decorations.add(body.textStyle().decoration());
            }
        }
        List<org.assertj.core.groups.Tuple> pieces = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            pieces.add(tuple(texts.get(i), decorations.get(i)));
        }
        return pieces;
    }

    @Test
    void aBoldParagraphHoldingAnUnderscoreInsideAWordStaysBold() {
        // The underscore is text, as CommonMark has it, and the paragraph's face is kept: the
        // marks made the page read the text as markdown, and that is all they did.
        assertThat(faces("Senior snake_case_name", TextDecoration.BOLD))
                .containsExactly(tuple("Senior snake_case_name", TextDecoration.BOLD));
    }

    @Test
    void everyFaceOfAParagraphIsKeptOnTextWithNoEmphasis() {
        assertThat(faces("Zilla BOLD_ITALIC style", TextDecoration.BOLD_ITALIC))
                .containsExactly(tuple("Zilla BOLD_ITALIC style", TextDecoration.BOLD_ITALIC));
        assertThat(faces("an italic file_name", TextDecoration.ITALIC))
                .containsExactly(tuple("an italic file_name", TextDecoration.ITALIC));
        assertThat(faces("an underlined file_name", TextDecoration.UNDERLINE))
                .containsExactly(tuple("an underlined file_name", TextDecoration.UNDERLINE));
        assertThat(faces("a struck file_name", TextDecoration.STRIKETHROUGH))
                .containsExactly(tuple("a struck file_name", TextDecoration.STRIKETHROUGH));
    }

    @Test
    void aCodeSpanKeepsTheParagraphsFace() {
        assertThat(faces("run `make` now", TextDecoration.BOLD))
                .containsExactly(tuple("run make now", TextDecoration.BOLD));
    }

    @Test
    void emphasisAddsToTheParagraphsFace() {
        assertThat(faces("a *b* c", TextDecoration.BOLD))
                .containsExactly(tuple("a ", TextDecoration.BOLD), tuple("b", TextDecoration.BOLD_ITALIC),
                        tuple(" c", TextDecoration.BOLD));
        assertThat(faces("x **y** z", TextDecoration.ITALIC))
                .containsExactly(tuple("x ", TextDecoration.ITALIC), tuple("y", TextDecoration.BOLD_ITALIC),
                        tuple(" z", TextDecoration.ITALIC));
        assertThat(faces("x **y**", TextDecoration.BOLD))
                .as("emphasis a face already holds changes nothing")
                .containsExactly(tuple("x y", TextDecoration.BOLD));
    }

    @Test
    void anUnderlinedParagraphsEmphasisTakesTheEmphasisFaceAlone() {
        // A piece carries one decoration, and none is both underlined and italic: the emphasised
        // piece takes the emphasis, the rest of the paragraph keeps its underline.
        assertThat(faces("a_b *c* d", TextDecoration.UNDERLINE))
                .containsExactly(tuple("a_b ", TextDecoration.UNDERLINE), tuple("c", TextDecoration.ITALIC),
                        tuple(" d", TextDecoration.UNDERLINE));
        assertThat(faces("a **c**", TextDecoration.UNDERLINE))
                .containsExactly(tuple("a ", TextDecoration.UNDERLINE), tuple("c", TextDecoration.BOLD));
        assertThat(faces("a_b *c*", TextDecoration.STRIKETHROUGH))
                .containsExactly(tuple("a_b ", TextDecoration.STRIKETHROUGH), tuple("c", TextDecoration.ITALIC));
    }

    @Test
    void aHeadingLineIsBoldWhateverTheParagraphsFace() {
        // The one line the paragraph's face does not reach: a heading is set bold, as it always was.
        assertThat(faces("# Head_x", TextDecoration.ITALIC))
                .containsExactly(tuple("Head_x", TextDecoration.BOLD));
    }

    @Test
    void anOrderedListLineKeepsTheParagraphsFace() {
        // The parser reads "1. " as an ordered list item and drops its number; the bullet its
        // ListItem handler would add does not appear, so that handler is not reached here. What is
        // left of the line is text in the paragraph's face, with nothing in another face before it.
        assertThat(faces("1. a_b", TextDecoration.BOLD))
                .containsExactly(tuple("a_b", TextDecoration.BOLD));
    }

    @Test
    void aStyleWithNoFaceReadsAsTheDefaultOne() {
        assertThat(faces("a_b *c*", null))
                .containsExactly(tuple("a_b ", TextDecoration.DEFAULT), tuple("c", TextDecoration.ITALIC));
    }

    @Test
    void aParagraphInTheDefaultFaceReadsItsMarksAsBefore() {
        // The paragraphs this change must leave alone: emphasis still emphasises, by either mark,
        // and an underscore inside a word is still text.
        assertThat(faces("_em_ and *em* and **s** snake_case", TextDecoration.DEFAULT))
                .containsExactly(tuple("em", TextDecoration.ITALIC), tuple(" and ", TextDecoration.DEFAULT),
                        tuple("em", TextDecoration.ITALIC), tuple(" and ", TextDecoration.DEFAULT),
                        tuple("s", TextDecoration.BOLD), tuple(" snake_case", TextDecoration.DEFAULT));
        assertThat(faces("a*b*c", TextDecoration.DEFAULT))
                .as("an asterisk inside a word does open emphasis")
                .containsExactly(tuple("a", TextDecoration.DEFAULT), tuple("b", TextDecoration.ITALIC),
                        tuple("c", TextDecoration.DEFAULT));
        assertThat(faces("a_b_c", TextDecoration.DEFAULT))
                .as("an underscore inside a word does not")
                .containsExactly(tuple("a_b_c", TextDecoration.DEFAULT));
    }
}
