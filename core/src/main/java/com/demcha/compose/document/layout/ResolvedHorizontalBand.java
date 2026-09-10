package com.demcha.compose.document.layout;

/**
 * One column of a horizontal composite, as the layout resolved it.
 *
 * <p>A row works out where each of its columns starts and how wide it is — from fixed
 * points, from shares of what is left, or from a mixture — and then lays its children into
 * those slots and forgets the arithmetic. A band is that arithmetic, kept: the slot itself,
 * before the child in it applies its own margin, so it means the same thing whatever was put
 * there.</p>
 *
 * <p>It exists so that content <em>after</em> a row can line up with one of its columns
 * without recomputing the column. Recomputing is the failure this avoids: a second copy of
 * the width formula agrees with the first until a share, a gap or a fixed column changes,
 * and then disagrees silently.</p>
 *
 * @param x     the slot's left edge, in the same coordinates the row was placed in
 * @param width the slot's width
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public record ResolvedHorizontalBand(double x, double width) {
}
