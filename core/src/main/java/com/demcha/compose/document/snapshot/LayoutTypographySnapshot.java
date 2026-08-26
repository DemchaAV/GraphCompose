package com.demcha.compose.document.snapshot;

import java.util.List;

/**
 * What a run of text actually became: which font it ended up in, at what size,
 * broken into how many lines, and where each of those lines sits.
 *
 * <p>Attached to a <em>fragment</em> rather than to a node, because that is what
 * text is. A paragraph that breaks across a page boundary emits one fragment per
 * page, each with its own lines and its own geometry, and a per-node projection
 * would have to pick one of them and discard the other.</p>
 *
 * <h2>Declared versus resolved</h2>
 *
 * <p>The pair {@link #declaredFont()} / {@link #resolvedFont()} is the reason
 * this record exists at all. A style may name a font that is not the font the
 * document is set in:</p>
 *
 * <ul>
 *   <li>{@code DEFAULT} resolves to the {@code HELVETICA} family;</li>
 *   <li>a standard-14 <em>face</em> such as {@code HELVETICA_BOLD} is an alias of
 *       its family and resolves to {@code HELVETICA} — the face is then chosen
 *       from the style's decoration, so a style that names the bold face and
 *       sets no decoration renders <em>regular</em>.</li>
 * </ul>
 *
 * <p>Both of those lay out and draw without error, which is exactly what makes
 * them expensive: the Java is correct, the document is wrong, and nothing says
 * so. {@link #fontSubstituted()} says so.</p>
 *
 * <p>A font that is neither registered nor aliased does not reach this record —
 * measurement fails first, loudly. So a substitution recorded here is always one
 * of the two quiet rewrites above, never a missing file.</p>
 *
 * <h2>One style per fragment, and it is the base one</h2>
 *
 * <p>{@code declaredFont}, {@code resolvedFont} and {@code fontSize} come from the
 * paragraph's <em>base</em> style. A paragraph whose spans override the font or
 * the size — an inline code run, a coloured chip, a bolded phrase — reports the
 * style it was declared with, not the several its spans resolved to. Per-span
 * typography is a larger surface and is deliberately not here; a consumer that
 * needs it should not read these three fields as if it were.</p>
 *
 * <h2>Text bounds are the ink, not the column</h2>
 *
 * <p>{@code textX}/{@code textY}/{@code textWidth}/{@code textHeight} bound the
 * lines this fragment actually drew, so the box always contains them. That is
 * deliberately not the content box the text was laid out into: a right-aligned
 * line sits at the far end of a column it does not fill, and a rectangle mixing
 * the column's left edge with the ink's width would contain neither.</p>
 *
 * @param path            path of the node that owns the text
 * @param fragmentIndex   index of this fragment within that node's fragments
 * @param page            zero-based page the fragment sits on
 * @param declaredFont    font name as the style declared it
 * @param resolvedFont    font family the text is actually laid out in
 * @param fontSubstituted whether the declared and resolved names differ
 * @param fontSize        declared point size of the fragment's base style
 * @param lineCount       number of laid-out lines in this fragment
 * @param textX           left edge of the box the ink occupies
 * @param textY           bottom edge of the box the ink occupies
 * @param textWidth       width of the box the ink occupies
 * @param textHeight      height of the box the ink occupies
 * @param verticalAlign   vertical seating mode, as a stable name
 * @param lines           the laid-out lines, in reading order
 * @author Artem Demchyshyn
 * @since 2.2.2
 */
public record LayoutTypographySnapshot(
        String path,
        int fragmentIndex,
        int page,
        String declaredFont,
        String resolvedFont,
        boolean fontSubstituted,
        double fontSize,
        int lineCount,
        double textX,
        double textY,
        double textWidth,
        double textHeight,
        String verticalAlign,
        List<LayoutTextLineSnapshot> lines) {

    /**
     * Freezes the line list so a snapshot cannot be mutated after extraction.
     */
    public LayoutTypographySnapshot {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
