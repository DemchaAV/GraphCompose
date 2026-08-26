package com.demcha.compose.document.snapshot;

import java.util.List;

/**
 * What a run of text actually became: which font it ended up in, at what size,
 * broken into how many lines, and where each of those lines sits.
 *
 * <p>Opt-in: this record only appears in a snapshot taken with
 * {@link LayoutSnapshotOptions.Builder#typography(boolean)} enabled. The default
 * snapshot does not carry it, so a baseline recorded before it existed stays
 * byte-identical.</p>
 *
 * <p>Attached to a <em>fragment</em> rather than to a node, because that is what
 * text is. A paragraph that breaks across a page boundary emits one fragment per
 * page, each with its own lines and its own geometry, and a per-node projection
 * would have to pick one of them and discard the other.</p>
 *
 * <h2>Declared versus resolved, and why decoration is here</h2>
 *
 * <p>Three fields describe the type: {@link #declaredFont()} is the name the
 * style used, {@link #resolvedFamily()} is the family the text was laid out in,
 * and {@link #decoration()} is what picks the concrete <em>face</em> within that
 * family. All three are needed, because the family alone does not identify the
 * face: {@code Helvetica + DEFAULT} and {@code Helvetica + BOLD} are one family
 * measured and drawn as two different faces.</p>
 *
 * <p>A style may also name a font the document is not set in:</p>
 *
 * <ul>
 *   <li>{@code DEFAULT} resolves to the {@code HELVETICA} family;</li>
 *   <li>a standard-14 <em>face</em> such as {@code HELVETICA_BOLD} is an alias of
 *       its family and resolves to {@code HELVETICA} — the face is then chosen
 *       from the decoration, so a style that names the bold face and sets no
 *       decoration renders <em>regular</em>.</li>
 * </ul>
 *
 * <p>{@link #fontSubstituted()} reports that second case only when the decoration
 * does <em>not</em> recover the named face. {@code HELVETICA_BOLD} with
 * {@code BOLD} draws bold and is not a substitution; {@code HELVETICA_BOLD} with
 * no decoration draws regular and is. That distinction is why {@code decoration}
 * is carried at all — without it the wrong document and the right one emit
 * identical fields.</p>
 *
 * <p>A font that is neither registered nor aliased does not reach this record —
 * measurement fails first, loudly. So a substitution recorded here is always one
 * of the quiet rewrites above, never a missing file.</p>
 *
 * <h2>One style per fragment, taken from the text actually laid out</h2>
 *
 * <p>{@code resolvedFamily}, {@code decoration} and {@code fontSize} describe the
 * fragment's <em>first text span</em> — the style the engine measured, after any
 * {@code autoSize} shrink and after any span-level override. {@code declaredFont}
 * stays the name the paragraph's base style used, so the pair still reads
 * "asked for this, got that".</p>
 *
 * <p>A paragraph whose spans use <em>several</em> fonts — body text with an
 * inline code chip in it — is described by its first span only. Per-span
 * typography is a larger surface and is deliberately not here; a consumer that
 * needs it should not read these fields as if it were.</p>
 *
 * <h2>Bounds are laid-out line boxes, not glyph ink</h2>
 *
 * <p>{@code textX}/{@code textY}/{@code textWidth}/{@code textHeight} bound the
 * <em>line boxes</em> this fragment laid out: horizontally from the leftmost line
 * start to the rightmost line end, vertically the stack of line boxes from the
 * first line's top to the last line's bottom. They are not tight glyph ink
 * bounds, and they are deliberately not the content column either — a
 * right-aligned line sits at the far end of a column it does not fill.</p>
 *
 * <p>Because they are line boxes, ink a backend deliberately paints outside a
 * line box falls outside them too: an inline code chip's fill extends by its own
 * vertical padding, and an inline graphic seated on the baseline can rise above
 * the line top. A consumer bounding painted output must not read this box as
 * covering those.</p>
 *
 * <h2>What this section does not cover</h2>
 *
 * <ul>
 *   <li><b>Text that is not a paragraph fragment.</b> A table cell written as a
 *       plain string is measured and drawn by the table's own layout, so it
 *       produces no entry here. An empty list means "no paragraph text", not
 *       "no text".</li>
 *   <li><b>Transformed or clipped containers.</b> These coordinates are the
 *       laid-out ones. A shape container carrying a {@code transform} has its
 *       children drawn through that matrix, and a clipping container can drop
 *       lines entirely; neither is reflected here.</li>
 * </ul>
 *
 * <h2>Identifying a run</h2>
 *
 * <p>{@code path} is the owning <em>node</em>, so the join to {@code nodes} is
 * one-to-many: a chart emits one entry per label, and a paragraph composed into a
 * table cell reports the table's path. {@code fragmentIndex} is the owner's
 * emission ordinal, which separates runs on one page but is not a stable
 * identifier — a paragraph split across pages restarts it at zero on each page,
 * which is why {@code page} is part of the ordering.</p>
 *
 * @param path            path of the node that owns the text
 * @param fragmentIndex   emission ordinal of this fragment within its owner, per page
 * @param page            zero-based page the fragment sits on
 * @param declaredFont    font name as the paragraph's base style declared it
 * @param resolvedFamily  font family the text was actually laid out in
 * @param decoration      decoration that selects the face within that family
 * @param fontSubstituted whether the face the declaration implies is not the face used
 * @param fontSize        point size the text was actually laid out at
 * @param lineCount       number of laid-out lines in this fragment
 * @param textX           left edge of the laid-out line boxes
 * @param textY           bottom edge of the laid-out line boxes
 * @param textWidth       width spanned by the laid-out line boxes
 * @param textHeight      height spanned by the laid-out line boxes
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
        String resolvedFamily,
        String decoration,
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
