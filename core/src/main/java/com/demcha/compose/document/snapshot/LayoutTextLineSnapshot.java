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
 * <p><b>{@code baselineExact} is not decoration.</b> The baseline of a line
 * seated by a non-default {@link com.demcha.compose.document.node.TextVerticalAlign}
 * is shifted by a correction derived from the backend font's cap height, and a
 * renderer-neutral snapshot has no backend font to ask. For those lines the
 * baseline below is the unshifted one and this flag is {@code false}: a
 * consumer that needs the true baseline must decline rather than use it. Every
 * line of a default-aligned paragraph — which is nearly all of them — is
 * exact.</p>
 *
 * @param index         zero-based position of the line within its fragment
 * @param x             left edge of the line's ink box after alignment
 * @param y             bottom edge of the line box
 * @param width         measured line width
 * @param height        resolved line height, including any inline image that enlarged it
 * @param baseline      absolute y of the text baseline
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
