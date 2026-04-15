package com.demcha.compose.document.templates.support;

import com.demcha.compose.layout_core.components.content.text.TextStyle;

import java.awt.Color;

/**
 * Lightweight style collaborator for invoice and proposal scene composers.
 */
public final class BusinessDocumentSceneStyles {

    public TextStyle titleStyle(double size) {
        return BusinessDocumentStyles.titleStyle(size);
    }

    public TextStyle headingStyle(double size) {
        return BusinessDocumentStyles.headingStyle(size);
    }

    public TextStyle labelStyle(double size) {
        return BusinessDocumentStyles.labelStyle(size);
    }

    public TextStyle bodyStyle(double size) {
        return BusinessDocumentStyles.bodyStyle(size);
    }

    public TextStyle bodyBoldStyle(double size) {
        return BusinessDocumentStyles.bodyBoldStyle(size);
    }

    public TextStyle metaStyle(double size) {
        return BusinessDocumentStyles.metaStyle(size);
    }

    public Color accentColor() {
        return BusinessDocumentStyles.ACCENT_COLOR;
    }

    public Color borderColor() {
        return BusinessDocumentStyles.BORDER_COLOR;
    }

    public Color softFill() {
        return BusinessDocumentStyles.SOFT_FILL;
    }

    public Color strongFill() {
        return BusinessDocumentStyles.STRONG_FILL;
    }
}
