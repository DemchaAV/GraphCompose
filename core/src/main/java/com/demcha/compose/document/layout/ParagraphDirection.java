package com.demcha.compose.document.layout;

import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextDirection;
import com.demcha.compose.engine.text.bidi.BidiParagraphResolver;

/**
 * Answers which way a paragraph actually runs.
 *
 * <p>{@link TextDirection#AUTO} is a question, not an answer: it says to read the
 * direction off the first strong character. Everything downstream needs the answer —
 * the layout to order the line, Word to be told the paragraph's base direction, the
 * builder to know which edge an unaligned paragraph sits at — and every one of them
 * asking separately is how they come to disagree.</p>
 *
 * <p>They did. The alignment default was resolved with a hand-rolled scan for the first
 * character of strong directionality, while the layout asked {@code java.text.Bidi}.
 * The two agree on ordinary text and part company on <em>isolates</em>: UAX&nbsp;#9 rule
 * P2 skips the characters between an isolate initiator and its matching PDI when looking
 * for the first strong character, and a plain scan reads straight into them. So
 * {@code LRI + Hebrew + PDI + "hello"} resolved right-to-left for the alignment and
 * left-to-right for the layout — a paragraph aligned to one edge and laid out from the
 * other. Isolates are the documented way to steer a neutral stretch of text and this
 * release is the one that started carrying them through sanitizing, so the disagreement
 * was reachable by exactly the author who read the documentation.</p>
 *
 * <p>This class is that single answer. It lives in the layout package because that is
 * the sanctioned bridge from the canonical surface to the engine: the builder may call
 * here, and only here does the call reach {@code BidiParagraphResolver}.</p>
 *
 * <p>Ownership: layout bridge.</p>
 *
 * @since 2.2.0
 */
public final class ParagraphDirection {

    private ParagraphDirection() {
    }

    /**
     * Resolves the direction a paragraph runs in, never returning {@link TextDirection#AUTO}.
     *
     * @param node paragraph to read
     * @return {@link TextDirection#RTL} or {@link TextDirection#LTR}
     */
    public static TextDirection resolve(ParagraphNode node) {
        return resolve(node.text(), node.direction());
    }

    /**
     * Resolves the direction declared text runs in, never returning {@link TextDirection#AUTO}.
     *
     * <p>The text form exists for the builder, which has to answer this question before
     * there is a node to ask about.</p>
     *
     * @param text the paragraph's text in logical order
     * @param declared the direction the caller asked for
     * @return {@link TextDirection#RTL} or {@link TextDirection#LTR}
     */
    public static TextDirection resolve(String text, TextDirection declared) {
        if (declared == TextDirection.RTL) {
            return TextDirection.RTL;
        }
        if (declared != TextDirection.AUTO) {
            return TextDirection.LTR;
        }
        int baseLevel = BidiParagraphResolver.baseLevel(
                text, BidiParagraphResolver.BaseDirection.FIRST_STRONG_CHARACTER);
        return BidiParagraphResolver.isRightToLeftLevel(baseLevel)
                ? TextDirection.RTL
                : TextDirection.LTR;
    }

    /**
     * Maps the resolved direction onto the engine's base direction.
     *
     * @param node paragraph to read
     * @return base direction for the bidirectional algorithm
     */
    public static BidiParagraphResolver.BaseDirection baseDirection(ParagraphNode node) {
        return resolve(node) == TextDirection.RTL
                ? BidiParagraphResolver.BaseDirection.RIGHT_TO_LEFT
                : BidiParagraphResolver.BaseDirection.LEFT_TO_RIGHT;
    }
}
