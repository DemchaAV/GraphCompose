package com.demcha.compose.document.templates.coverletter.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.RichParagraphRenderer;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Shared cover-letter body renderer — the letter analog of
 * {@code SectionDispatcher}.
 *
 * <p>Stacks the greeting line, the ordered body paragraphs, and the
 * closing sign-off into a single host section. Each block is rendered
 * with the theme's body font / size / ink and line spacing through the
 * shared {@link RichParagraphRenderer}, so inline markdown
 * ({@code **bold**}, {@code *italic*}) is honoured exactly as in the
 * paired CV preset's body prose.</p>
 *
 * <p>Every v2 letter preset reuses this, so all letters share one
 * identical reading rhythm and only the masthead differs by brand —
 * which is what makes a CV and its letter read as a matched set.</p>
 */
public final class LetterBody {

    private LetterBody() {
    }

    /**
     * Renders greeting + body paragraphs + closing into {@code host}
     * using the theme's body style and line spacing.
     *
     * @param host  host section (typically one page-flow section)
     * @param doc   cover-letter content
     * @param theme active brand theme — share the same instance with the
     *              paired CV preset so the body colour / font / size match
     */
    public static void render(SectionBuilder host, CoverLetterDocument doc,
                              CvTheme theme) {
        render(host, doc, theme, theme.typography().sizeBody());
    }

    /**
     * Variant that renders the letter prose at an explicit point size
     * instead of {@code theme.typography().sizeBody()}. Used by presets
     * whose paired CV theme carries a very small body size tuned for a
     * dense multi-column CV (e.g. Monogram Sidebar, Timeline Minimal) —
     * a single-column letter needs a more readable size, so the preset
     * supplies one without disturbing the CV.
     *
     * @param host     host section
     * @param doc      cover-letter content
     * @param theme    active brand theme (font, line spacing, ink)
     * @param bodySize body text size in points
     */
    public static void render(SectionBuilder host, CoverLetterDocument doc,
                              CvTheme theme, double bodySize) {
        DocumentTextStyle bodyStyle = CvTextStyles.of(
                theme.typography().bodyFont(),
                bodySize,
                DocumentTextDecoration.DEFAULT,
                theme.palette().ink());
        double lineSpacing = theme.typography().bodyLineSpacing();
        // Letter paragraph rhythm scales with the body size so a compact
        // brand stays tight and a roomy serif brand breathes, without a
        // separate hand-tuned token per brand.
        double gap = bodySize * 1.25;
        // Clear breathing room below the masthead so the letter body never
        // reads as "stuck" to the header. Normalised: the page-flow gap
        // between the header section and this body section already supplies
        // some space, so we top it up to a consistent ~1.8x the body size
        // total and subtract what the brand's pageFlowSpacing already gives.
        // The result is the same comfortable separation under every brand's
        // masthead, whether its CV uses a dense (0pt) or roomy (8pt) gap.
        double headerGap = Math.max(2.0,
                bodySize * 1.8 - theme.spacing().pageFlowSpacing());
        // The signed name sits on the line directly below the sign-off
        // (standard letter convention), so it gets only a small gap.
        double signatureGap = bodySize * 0.4;
        DocumentTextStyle signatureStyle = CvTextStyles.of(
                theme.typography().bodyFont(),
                bodySize,
                DocumentTextDecoration.ITALIC,
                theme.palette().ink());

        boolean[] emitted = {false};
        emit(host, doc.greeting(), bodyStyle, lineSpacing, headerGap, gap, emitted);
        for (String paragraph : doc.body()) {
            emit(host, paragraph, bodyStyle, lineSpacing, headerGap, gap, emitted);
        }
        // Closing block: the sign-off ("Sincerely,") on one line, then the
        // signer's name on the line directly below it. The name is pulled
        // from the shared identity so the signature always matches the
        // masthead and never drifts from the paired CV.
        if (!doc.closing().isBlank()) {
            double top = emitted[0] ? gap : headerGap;
            RichParagraphRenderer.render(host, doc.closing(), bodyStyle,
                    lineSpacing, DocumentInsets.top(top));
            String signature = doc.identity().name().full();
            if (!signature.isBlank()) {
                RichParagraphRenderer.render(host, signature, signatureStyle,
                        lineSpacing, DocumentInsets.top(signatureGap));
            }
        }
    }

    private static void emit(SectionBuilder host, String text,
                             DocumentTextStyle style, double lineSpacing,
                             double firstTop, double gap, boolean[] emitted) {
        if (text == null || text.isBlank()) {
            return;
        }
        // First emitted block gets the larger header gap (separation from
        // the masthead); every subsequent block gets the inter-paragraph gap.
        double top = emitted[0] ? gap : firstTop;
        RichParagraphRenderer.render(host, text, style, lineSpacing,
                DocumentInsets.top(top));
        emitted[0] = true;
    }
}
