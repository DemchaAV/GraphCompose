package com.demcha.compose.document.output;

import java.util.Objects;

/**
 * Debug-overlay configuration for fixed-layout document rendering.
 *
 * <p>Debug overlays are development aids drawn on top of the regular page
 * content. They never participate in measurement or pagination, so enabling
 * them does not change the layout graph — and leaving them disabled (the
 * default) keeps rendered documents byte-identical to previous releases.
 * Like the other backend-neutral output options in this package
 * ({@link DocumentWatermark}, {@link DocumentMetadata}, …), the carrier is
 * format-agnostic; the canonical PDF backend implements both overlays today,
 * and future fixed-layout backends can honour the same options.</p>
 *
 * <p>Two overlays are available:</p>
 * <ul>
 *   <li><b>Guides</b> — fragment boxes plus dashed margin/padding rectangles,
 *       the overlay previously toggled by the {@code guideLines(boolean)}
 *       convenience switch.</li>
 *   <li><b>Node labels</b> — the stable semantic path of the owning node,
 *       printed once per node and page as a small corner badge straddling
 *       the top edge of the node's bounds (right-aligned, so it rarely
 *       covers the node's own text). Labels make a misplaced block traceable
 *       back to the exact builder call that authored it: name nodes via the
 *       DSL (for example {@code pageFlow().name("InvoiceSheet")}; module
 *       titles auto-name their blocks) and the same name appears on the
 *       sheet and in {@code DocumentSession.layoutSnapshot()}.</li>
 * </ul>
 *
 * <p>Typical usage through the session convenience API:</p>
 * <pre>{@code
 * try (DocumentSession document = GraphCompose.document(out)
 *         .debug(DocumentDebugOptions.guidesAndNodeLabels())
 *         .create()) {
 *     // author content ...
 *     document.buildPdf();
 * }
 * }</pre>
 *
 * @param showGuides     whether the guide-line overlay (fragment boxes,
 *                       margin and padding rectangles) is drawn
 * @param showNodeLabels whether semantic node labels are drawn
 * @param labelText      which text the node-label overlay prints; never
 *                       {@code null}
 * @author Artem Demchyshyn
 * @since 1.8.0
 */
public record DocumentDebugOptions(boolean showGuides, boolean showNodeLabels, LabelText labelText) {

    private static final DocumentDebugOptions NONE = new DocumentDebugOptions(false, false, LabelText.NAME);

    /**
     * Validates record invariants.
     */
    public DocumentDebugOptions {
        Objects.requireNonNull(labelText, "labelText");
    }

    /**
     * Returns the default configuration with every overlay disabled.
     *
     * @return options with all debug overlays off
     */
    public static DocumentDebugOptions none() {
        return NONE;
    }

    /**
     * Returns options with only the guide-line overlay enabled — the
     * equivalent of the {@code guideLines(true)} convenience switch.
     *
     * @return options drawing fragment boxes and margin/padding guides
     */
    public static DocumentDebugOptions guides() {
        return new DocumentDebugOptions(true, false, LabelText.NAME);
    }

    /**
     * Returns options with only the node-label overlay enabled.
     *
     * @return options drawing semantic node labels
     */
    public static DocumentDebugOptions nodeLabels() {
        return new DocumentDebugOptions(false, true, LabelText.NAME);
    }

    /**
     * Returns options with both the guide-line and node-label overlays
     * enabled.
     *
     * @return options drawing guides and semantic node labels
     */
    public static DocumentDebugOptions guidesAndNodeLabels() {
        return new DocumentDebugOptions(true, true, LabelText.NAME);
    }

    /**
     * Returns a copy with the guide-line overlay toggled.
     *
     * @param enabled {@code true} to draw guide lines
     * @return new options instance with the requested guide state
     */
    public DocumentDebugOptions withGuides(boolean enabled) {
        return enabled == showGuides ? this : new DocumentDebugOptions(enabled, showNodeLabels, labelText);
    }

    /**
     * Returns a copy with the node-label overlay toggled.
     *
     * @param enabled {@code true} to draw semantic node labels
     * @return new options instance with the requested label state
     */
    public DocumentDebugOptions withNodeLabels(boolean enabled) {
        return enabled == showNodeLabels ? this : new DocumentDebugOptions(showGuides, enabled, labelText);
    }

    /**
     * Returns a copy printing the requested label text.
     *
     * @param text label text mode; must not be {@code null}
     * @return new options instance with the requested label text mode
     */
    public DocumentDebugOptions withLabelText(LabelText text) {
        Objects.requireNonNull(text, "text");
        return text == labelText ? this : new DocumentDebugOptions(showGuides, showNodeLabels, text);
    }

    /**
     * Indicates whether any debug overlay is enabled.
     *
     * @return {@code true} when at least one overlay draws
     */
    public boolean enabled() {
        return showGuides || showNodeLabels;
    }

    /**
     * Text printed by the node-label overlay.
     *
     * @since 1.8.0
     */
    public enum LabelText {
        /**
         * Only the node's own path segment, for example {@code InvoiceHeader[0]}.
         * Compact; the default.
         */
        NAME,
        /**
         * The full ancestor chain, for example
         * {@code Root[0]/InvoiceHeader[0]/Paragraph[2]}. Verbose but
         * unambiguous on documents with repeated component names.
         */
        FULL_PATH
    }
}
