package com.demcha.compose.document.layout;

import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.engine.components.style.Margin;

/**
 * Converts canonical {@link DocumentInsets} into the engine's {@link Margin}
 * value type at the layout boundary. Keeping this bridge here — rather than in
 * the canonical {@code document.api} — lets the public session API feed page
 * geometry into the compiler without ever referencing an engine type.
 */
final class LayoutInsets {

    private LayoutInsets() {
    }

    /**
     * Maps canonical page insets to an engine {@link Margin}; {@code null}
     * insets become {@link Margin#zero()}.
     *
     * @param insets canonical page insets, or {@code null}
     * @return the equivalent engine margin
     */
    static Margin toMargin(DocumentInsets insets) {
        if (insets == null) {
            return Margin.zero();
        }
        return new Margin(insets.top(), insets.right(), insets.bottom(), insets.left());
    }
}
