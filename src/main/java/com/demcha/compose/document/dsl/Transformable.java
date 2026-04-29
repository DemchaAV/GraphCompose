package com.demcha.compose.document.dsl;

import com.demcha.compose.document.style.DocumentTransform;

/**
 * Mixin interface for builders that can attach a {@link DocumentTransform}
 * (rotation around the placement centre and/or scaling) to the node they
 * build. Builders opt in by implementing this interface and providing
 * {@link #transform(DocumentTransform)} plus {@link #currentTransform()};
 * the {@code rotate(...)} and {@code scale(...)} shortcuts are inherited
 * default methods so each builder gets the full surface for free.
 *
 * <p>The transform is a render-time concern. The canonical layout
 * compiler still measures and places the node against its natural
 * bounding box; backends apply the transform when drawing (PDF backend
 * uses graphics-state {@code cm} push/pop around the placement centre).
 * Layout snapshots therefore stay deterministic regardless of the
 * transform — the transform shows up in the rendered PDF, not in the
 * placement coordinates.</p>
 *
 * @param <T> concrete builder type — returned from each setter so calls
 *            can chain naturally
 *
 * @author Artem Demchyshyn
 */
public interface Transformable<T extends Transformable<T>> {

    /**
     * Replaces the current transform with the supplied value.
     *
     * @param transform new transform; {@code null} resets to
     *                  {@link DocumentTransform#NONE}
     * @return this builder
     */
    T transform(DocumentTransform transform);

    /**
     * @return the transform currently configured on the builder; never
     *         {@code null} (defaults to {@link DocumentTransform#NONE})
     */
    DocumentTransform currentTransform();

    /**
     * Sets a clockwise rotation in degrees, preserving the current
     * scale factors.
     *
     * @param degrees clockwise rotation in degrees
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    default T rotate(double degrees) {
        DocumentTransform current = currentTransform();
        DocumentTransform updated = current == null ? DocumentTransform.rotate(degrees)
                : current.withRotation(degrees);
        transform(updated);
        return (T) this;
    }

    /**
     * Uniform scaling shortcut — both axes share the same factor.
     * Preserves the current rotation.
     *
     * @param uniformFactor scale factor for both axes
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    default T scale(double uniformFactor) {
        DocumentTransform current = currentTransform();
        DocumentTransform updated = current == null ? DocumentTransform.scale(uniformFactor)
                : current.withScale(uniformFactor, uniformFactor);
        transform(updated);
        return (T) this;
    }

    /**
     * Non-uniform scaling. Preserves the current rotation.
     *
     * @param scaleX horizontal scale factor
     * @param scaleY vertical scale factor
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    default T scale(double scaleX, double scaleY) {
        DocumentTransform current = currentTransform();
        DocumentTransform updated = current == null ? DocumentTransform.scale(scaleX, scaleY)
                : current.withScale(scaleX, scaleY);
        transform(updated);
        return (T) this;
    }
}
