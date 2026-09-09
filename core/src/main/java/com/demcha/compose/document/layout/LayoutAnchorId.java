package com.demcha.compose.document.layout;

import java.util.Objects;

/**
 * Identity of a resolved-layout anchor, compared by reference rather than by name.
 *
 * <p>A feature that needs to draw from resolved geometry — a rail down a timeline, a
 * bracket across sections, a connector between two nodes — has to find its own anchors in
 * the finished layout. Matching them by generated path or node name would tie the feature
 * to the compiler's naming, which is a private detail: {@code LayoutCompiler.pathFor} uses
 * the node kind as the path segment of an unnamed node, so a rename anywhere reshuffles
 * every path. It is also ambiguous — two timelines on one page have equally plausible
 * paths.</p>
 *
 * <p>So identity is explicit. {@code groupKey} and {@code kind} are compared with
 * {@code ==}: the caller allocates one object per logical group (one per timeline) and
 * uses a constant for the kind (an enum constant reads best). Nothing is parsed, and two
 * groups can never collide however similar their content.</p>
 *
 * @param groupKey the logical owner this anchor belongs to; compared by reference
 * @param kind     what sort of anchor this is within that owner; compared by reference
 * @param index    position within the group, in declaration order, starting at zero
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
public record LayoutAnchorId(Object groupKey, Object kind, int index) {

    /**
     * Validates the identity.
     *
     * @throws NullPointerException     if {@code groupKey} or {@code kind} is null
     * @throws IllegalArgumentException if {@code index} is negative
     */
    public LayoutAnchorId {
        Objects.requireNonNull(groupKey, "groupKey");
        Objects.requireNonNull(kind, "kind");
        rejectValueLike(groupKey, "groupKey");
        rejectValueLike(kind, "kind");
        if (index < 0) {
            throw new IllegalArgumentException("Anchor index must not be negative, was " + index + ".");
        }
    }

    /**
     * Refuses keys whose reference identity is decided by the JVM rather than the caller.
     *
     * <p>Comparison here is {@code ==}, which is a deliberate choice — but it turns a
     * {@code String} or a boxed number into a coin flip: {@code "timeline"} written twice
     * is one interned instance and compares equal, while the same text computed at runtime
     * does not, and {@code Integer.valueOf(127)} is cached where {@code 128} is not. A
     * caller who reached for a readable key would get an anchor set that silently fails to
     * match, with nothing drawn and no error to read. Refusing them at construction turns
     * that into a message at the call site.</p>
     */
    private static void rejectValueLike(Object key, String name) {
        if (key instanceof String || key instanceof Number || key instanceof Character
            || key instanceof Boolean) {
            throw new IllegalArgumentException(
                    "Anchor " + name + " must be an identity key, not a " + key.getClass().getSimpleName()
                    + ": these are compared with == and interning would decide whether two of them match. "
                    + "Allocate one object per group (or use an enum constant for the kind).");
        }
    }

    /**
     * Compares by reference on {@code groupKey} and {@code kind}, and by value on the
     * index — deliberately not the record default, which would call {@code equals} on
     * both and let two distinct groups compare equal because their keys happen to.
     *
     * @param other candidate
     * @return whether both identities name the same anchor
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LayoutAnchorId otherId)) {
            return false;
        }
        return groupKey == otherId.groupKey
               && kind == otherId.kind
               && index == otherId.index;
    }

    /**
     * Hashes on identity hash codes, to stay consistent with {@link #equals(Object)}.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return (System.identityHashCode(groupKey) * 31 + System.identityHashCode(kind)) * 31 + index;
    }
}
