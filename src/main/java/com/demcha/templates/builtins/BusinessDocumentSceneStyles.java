package com.demcha.templates.builtins;

import com.demcha.compose.layout_core.components.content.text.TextStyle;

import java.awt.Color;

/**
 * Lightweight style collaborator for backend-neutral business document scene builders.
 */
final class BusinessDocumentSceneStyles {

    TextStyle titleStyle(double size) {
        return BusinessDocumentStyles.titleStyle(size);
    }

    TextStyle headingStyle(double size) {
        return BusinessDocumentStyles.headingStyle(size);
    }

    TextStyle labelStyle(double size) {
        return BusinessDocumentStyles.labelStyle(size);
    }

    TextStyle bodyStyle(double size) {
        return BusinessDocumentStyles.bodyStyle(size);
    }

    TextStyle bodyBoldStyle(double size) {
        return BusinessDocumentStyles.bodyBoldStyle(size);
    }

    TextStyle metaStyle(double size) {
        return BusinessDocumentStyles.metaStyle(size);
    }

    Color accentColor() {
        return BusinessDocumentStyles.ACCENT_COLOR;
    }

    Color borderColor() {
        return BusinessDocumentStyles.BORDER_COLOR;
    }

    Color softFill() {
        return BusinessDocumentStyles.SOFT_FILL;
    }

    Color strongFill() {
        return BusinessDocumentStyles.STRONG_FILL;
    }
}
