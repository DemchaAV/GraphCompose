package com.demcha.compose.document.templates.support.business;

import com.demcha.compose.document.templates.support.common.*;

import com.demcha.compose.engine.components.content.text.TextStyle;

import java.awt.Color;

/**
 * Lightweight style collaborator for invoice and proposal scene composers.
 */
public final class BusinessDocumentSceneStyles {

    /**
     * Creates the shared business document style adapter.
     */
    public BusinessDocumentSceneStyles() {
    }

    /**
     * Returns a title text style.
     *
     * @param size font size in points
     * @return title text style
     */
    public TextStyle titleStyle(double size) {
        return BusinessDocumentStyles.titleStyle(size);
    }

    /**
     * Returns a section heading text style.
     *
     * @param size font size in points
     * @return heading text style
     */
    public TextStyle headingStyle(double size) {
        return BusinessDocumentStyles.headingStyle(size);
    }

    /**
     * Returns a label text style.
     *
     * @param size font size in points
     * @return label text style
     */
    public TextStyle labelStyle(double size) {
        return BusinessDocumentStyles.labelStyle(size);
    }

    /**
     * Returns a body text style.
     *
     * @param size font size in points
     * @return body text style
     */
    public TextStyle bodyStyle(double size) {
        return BusinessDocumentStyles.bodyStyle(size);
    }

    /**
     * Returns a bold body text style.
     *
     * @param size font size in points
     * @return bold body text style
     */
    public TextStyle bodyBoldStyle(double size) {
        return BusinessDocumentStyles.bodyBoldStyle(size);
    }

    /**
     * Returns a compact metadata text style.
     *
     * @param size font size in points
     * @return metadata text style
     */
    public TextStyle metaStyle(double size) {
        return BusinessDocumentStyles.metaStyle(size);
    }

    /**
     * Returns the business accent color.
     *
     * @return accent color
     */
    public Color accentColor() {
        return BusinessDocumentStyles.ACCENT_COLOR;
    }

    /**
     * Returns the business border color.
     *
     * @return border color
     */
    public Color borderColor() {
        return BusinessDocumentStyles.BORDER_COLOR;
    }

    /**
     * Returns the soft background fill.
     *
     * @return soft fill color
     */
    public Color softFill() {
        return BusinessDocumentStyles.SOFT_FILL;
    }

    /**
     * Returns the strong background fill.
     *
     * @return strong fill color
     */
    public Color strongFill() {
        return BusinessDocumentStyles.STRONG_FILL;
    }
}
