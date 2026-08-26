package com.demcha.compose.document.snapshot;

/**
 * One laid-out line of text, in absolute page coordinates.
 *
 * <p>The line's text is deliberately absent. A snapshot excludes raw text payload —
 * it is a geometry baseline, and the words are already in the document that produced
 * it — so a line is identified by its {@code index} within the fragment.</p>
 *
 * <p>The coordinates are the ones the fixed-layout backends draw at, computed
 * through the same {@code ParagraphLineGeometry} helper the PDF handler uses, so
 * a diagnostic reading this and a reader looking at the rendered page are
 * talking about the same line. As everywhere else in the snapshot, {@code y}
 * grows <em>upward</em> from the page bottom.</p>
 *
 * <p><b>{@code baselineExact} is not decoration.</b> A line seated by a
 * non-default {@link com.demcha.compose.document.node.TextVerticalAlign} is
 * drawn with its baseline shifted by a correction derived from the backend
 * font's cap height, and a renderer-neutral snapshot has no backend font to ask.
 * For those lines the baseline below is the unshifted one and this flag is
 * {@code false}: a consumer that needs the true baseline must decline rather
 * than use it. Every line of a default-aligned paragraph — which is nearly all
 * of them — is exact.</p>
 *
 * <p>Note that the shift moves the <em>glyphs</em> and not the line box, so when
 * this flag is {@code false} the box below is the seat the line was laid out in
 * rather than where its glyphs landed: under {@code BOTTOM} seating the drawn
 * baseline coincides with {@code y}, putting descenders below it. Treat the whole
 * entry as positional, not as a bound on painted output, whenever the flag is
 * clear.</p>
 *
 * <p>The box is the laid-out line box, not tight glyph ink: an inline code chip's
 * fill extends past it by the chip's own padding, and an inline graphic seated on
 * the baseline can rise above {@code y + height}.</p>
 *
 * @param index         zero-based position of the line within its fragment
 * @param x             left edge of the line box after alignment
 * @param y             bottom edge of the line box
 * @param width         measured line width
 * @param height        resolved line height, including any inline image that enlarged it
 * @param baseline      absolute y of the text baseline, before vertical seating
 * @param baselineExact whether {@code baseline} accounts for vertical seating; see above
 * @author Artem Demchyshyn
 * @since 2.2.2
 */
public record LayoutTextLineSnapshot(
        int index,
        double x,
        double y,
        double width,
        double height,
        double baseline,
        boolean baselineExact) {
}
