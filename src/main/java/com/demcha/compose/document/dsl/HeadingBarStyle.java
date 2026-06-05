package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;

/**
 * Mutable look of a heading bar — a filled, rounded title band with a single
 * label — customised inside the
 * {@link AbstractFlowBuilder#headingBar(String, java.util.function.Consumer)}
 * lambda ({@code headingBar(text, bar -> ...)}). Every knob has a sensible
 * default (a light-grey band with a centred bold label), so
 * {@code bar -> bar.fill(navy).textStyle(white)} is usually enough.
 *
 * @author Artem Demchyshyn
 * @since 1.7.0
 */
public final class HeadingBarStyle {

    private static final DocumentTextStyle DEFAULT_TEXT = DocumentTextStyle.builder()
            .fontName(FontName.HELVETICA_BOLD)
            .decoration(DocumentTextDecoration.BOLD)
            .size(11)
            .build();

    private DocumentColor fill = DocumentColor.rgb(238, 238, 238);
    private DocumentCornerRadius cornerRadius = DocumentCornerRadius.of(8.0);
    private DocumentInsets padding = DocumentInsets.symmetric(6.0, 12.0);
    private DocumentInsets margin = DocumentInsets.zero();
    private DocumentTextStyle textStyle = DEFAULT_TEXT;
    private TextAlign align = TextAlign.CENTER;
    private DocumentStroke stroke;

    /**
     * Creates a heading-bar style with the default light-grey band look.
     */
    public HeadingBarStyle() {
    }

    /**
     * Sets the bar background fill.
     *
     * @param fill background colour; ignored when {@code null}
     * @return this style
     */
    public HeadingBarStyle fill(DocumentColor fill) {
        if (fill != null) {
            this.fill = fill;
        }
        return this;
    }

    /**
     * Sets a uniform corner radius for the bar.
     *
     * @param radius corner radius in points
     * @return this style
     */
    public HeadingBarStyle cornerRadius(double radius) {
        return cornerRadius(DocumentCornerRadius.of(radius));
    }

    /**
     * Sets a per-corner radius for the bar.
     *
     * @param cornerRadius corner radii; ignored when {@code null}
     * @return this style
     */
    public HeadingBarStyle cornerRadius(DocumentCornerRadius cornerRadius) {
        if (cornerRadius != null) {
            this.cornerRadius = cornerRadius;
        }
        return this;
    }

    /**
     * Sets a uniform inner padding around the label.
     *
     * @param padding padding in points applied to all sides
     * @return this style
     */
    public HeadingBarStyle padding(double padding) {
        return padding(DocumentInsets.of(padding));
    }

    /**
     * Sets the inner padding around the label.
     *
     * @param padding inner padding; ignored when {@code null}
     * @return this style
     */
    public HeadingBarStyle padding(DocumentInsets padding) {
        if (padding != null) {
            this.padding = padding;
        }
        return this;
    }

    /**
     * Sets the outer margin of the bar.
     *
     * @param margin outer margin; ignored when {@code null}
     * @return this style
     */
    public HeadingBarStyle margin(DocumentInsets margin) {
        if (margin != null) {
            this.margin = margin;
        }
        return this;
    }

    /**
     * Sets the label text style.
     *
     * @param textStyle label text style; ignored when {@code null}
     * @return this style
     */
    public HeadingBarStyle textStyle(DocumentTextStyle textStyle) {
        if (textStyle != null) {
            this.textStyle = textStyle;
        }
        return this;
    }

    /**
     * Sets the horizontal alignment of the label inside the bar.
     *
     * @param align label alignment; ignored when {@code null}
     * @return this style
     */
    public HeadingBarStyle align(TextAlign align) {
        if (align != null) {
            this.align = align;
        }
        return this;
    }

    /**
     * Sets an optional outline stroke around the bar.
     *
     * @param stroke border stroke, or {@code null} for no outline
     * @return this style
     */
    public HeadingBarStyle stroke(DocumentStroke stroke) {
        this.stroke = stroke;
        return this;
    }

    DocumentColor fill() {
        return fill;
    }

    DocumentCornerRadius cornerRadius() {
        return cornerRadius;
    }

    DocumentInsets padding() {
        return padding;
    }

    DocumentInsets margin() {
        return margin;
    }

    DocumentTextStyle textStyle() {
        return textStyle;
    }

    TextAlign align() {
        return align;
    }

    DocumentStroke stroke() {
        return stroke;
    }
}
