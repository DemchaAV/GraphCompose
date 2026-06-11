package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.components.MarkdownInline;

import java.util.Objects;

/**
 * CV profile/summary band widget: a titled rich-text block with
 * optional fill and accents.
 */
public final class ProfileBand {
    private ProfileBand() {
    }

    /**
     * Renders the profile band as a new named section in the page flow.
     * Does nothing when {@code body} is {@code null} or blank.
     *
     * @param flow  the page-flow builder the section is added to
     * @param name  node name used in snapshots and layout graph paths
     * @param title the optional band title; skipped when null or blank
     * @param body  the rich-text body in inline-markdown form
     * @param style styling knobs for the band; {@code null} uses
     *              {@link Style#defaults()}
     */
    public static void render(PageFlowBuilder flow,
                              String name,
                              String title,
                              String body,
                              Style style) {
        if (body == null || body.isBlank()) {
            return;
        }
        flow.addSection(name, host -> render(host, title, body, style));
    }

    /**
     * Renders the profile band into an existing section. Does nothing
     * when {@code body} is {@code null} or blank.
     *
     * @param host  the section builder the band is rendered into
     * @param title the optional band title; skipped when null or blank
     * @param body  the rich-text body in inline-markdown form
     * @param style styling knobs for the band; {@code null} uses
     *              {@link Style#defaults()}
     */
    public static void render(SectionBuilder host,
                              String title,
                              String body,
                              Style style) {
        if (body == null || body.isBlank()) {
            return;
        }
        Style safeStyle = style == null ? Style.defaults() : style;
        host.spacing(safeStyle.spacing())
                .padding(safeStyle.padding());
        if (safeStyle.fillColor() != null) {
            host.fillColor(safeStyle.fillColor());
        }
        if (safeStyle.cornerRadius() != null) {
            host.cornerRadius(safeStyle.cornerRadius());
        }
        if (safeStyle.accentLeftColor() != null) {
            host.accentLeft(safeStyle.accentLeftColor(),
                    safeStyle.accentLeftWidth());
        }
        if (safeStyle.accentTopColor() != null) {
            host.accentTop(safeStyle.accentTopColor(),
                    safeStyle.accentTopWidth());
        }
        if (safeStyle.accentBottomColor() != null) {
            host.accentBottom(safeStyle.accentBottomColor(),
                    safeStyle.accentBottomWidth());
        }
        DocumentTextStyle bodyStyle = Objects.requireNonNull(
                safeStyle.bodyStyle(), "ProfileBand bodyStyle");
        if (title != null && !title.isBlank()
            && safeStyle.titleStyle() != null) {
            host.addParagraph(paragraph -> paragraph
                    .text(safeStyle.transformTitle()
                            ? title.toUpperCase(java.util.Locale.ROOT)
                            : title)
                    .textStyle(safeStyle.titleStyle())
                    .align(safeStyle.titleAlign())
                    .margin(DocumentInsets.zero()));
        }
        host.addParagraph(paragraph -> paragraph
                .textStyle(bodyStyle)
                .lineSpacing(safeStyle.bodyLineSpacing())
                .align(safeStyle.bodyAlign())
                .margin(DocumentInsets.zero())
                .rich(rich -> MarkdownInline.appendTrimmed(rich, body,
                        bodyStyle)));
    }

    /**
     * Styling knobs for the profile band.
     *
     * @param spacing           vertical spacing between children
     * @param padding           inner padding
     * @param fillColor         optional background fill
     * @param cornerRadius      optional render-only corner radius
     * @param accentLeftColor   optional left accent-bar colour
     * @param accentLeftWidth   width in points of the left accent bar
     * @param accentTopColor    optional top accent-bar colour
     * @param accentTopWidth    width in points of the top accent bar
     * @param accentBottomColor optional bottom accent-bar colour
     * @param accentBottomWidth width in points of the bottom accent bar
     * @param titleStyle        text style for the title; null suppresses the title
     * @param titleAlign        horizontal alignment of the title
     * @param transformTitle    whether the title is transformed to uppercase
     * @param bodyStyle         base text style for the body
     * @param bodyAlign         horizontal alignment of the body
     * @param bodyLineSpacing   extra space between wrapped body lines
     */
    public record Style(double spacing,
                        DocumentInsets padding,
                        DocumentColor fillColor,
                        DocumentCornerRadius cornerRadius,
                        DocumentColor accentLeftColor,
                        double accentLeftWidth,
                        DocumentColor accentTopColor,
                        double accentTopWidth,
                        DocumentColor accentBottomColor,
                        double accentBottomWidth,
                        DocumentTextStyle titleStyle,
                        TextAlign titleAlign,
                        boolean transformTitle,
                        DocumentTextStyle bodyStyle,
                        TextAlign bodyAlign,
                        double bodyLineSpacing) {

        /**
         * Applies defaults for {@code padding}, {@code titleAlign}, and {@code bodyAlign}.
         */
        public Style {
            padding = padding == null ? DocumentInsets.zero() : padding;
            titleAlign = titleAlign == null ? TextAlign.LEFT : titleAlign;
            bodyAlign = bodyAlign == null ? TextAlign.LEFT : bodyAlign;
        }

        /**
         * Style with all defaults applied.
         *
         * @return a {@code Style} built from default values
         */
        public static Style defaults() {
            return builder().build();
        }

        /**
         * Creates a new {@link Builder} for the band style.
         *
         * @return a new empty {@code Builder}
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Mutable builder for {@link Style}.
         */
        public static final class Builder {
            private double spacing;
            private DocumentInsets padding = DocumentInsets.zero();
            private DocumentColor fillColor;
            private DocumentCornerRadius cornerRadius;
            private DocumentColor accentLeftColor;
            private double accentLeftWidth;
            private DocumentColor accentTopColor;
            private double accentTopWidth;
            private DocumentColor accentBottomColor;
            private double accentBottomWidth;
            private DocumentTextStyle titleStyle;
            private TextAlign titleAlign = TextAlign.LEFT;
            private boolean transformTitle;
            private DocumentTextStyle bodyStyle;
            private TextAlign bodyAlign = TextAlign.LEFT;
            private double bodyLineSpacing = 1.0;

            private Builder() {
            }

            /**
             * Sets the vertical spacing between children.
             *
             * @param value vertical spacing between children
             * @return this builder for chaining
             */
            public Builder spacing(double value) {
                this.spacing = value;
                return this;
            }

            /**
             * Sets the inner padding.
             *
             * @param value inner padding
             * @return this builder for chaining
             */
            public Builder padding(DocumentInsets value) {
                this.padding = value;
                return this;
            }

            /**
             * Sets the optional background fill.
             *
             * @param value optional background fill
             * @return this builder for chaining
             */
            public Builder fillColor(DocumentColor value) {
                this.fillColor = value;
                return this;
            }

            /**
             * Sets the optional render-only corner radius.
             *
             * @param value optional render-only corner radius
             * @return this builder for chaining
             */
            public Builder cornerRadius(DocumentCornerRadius value) {
                this.cornerRadius = value;
                return this;
            }

            /**
             * Sets the left accent bar.
             *
             * @param color the left accent-bar colour
             * @param width width in points of the left accent bar
             * @return this builder for chaining
             */
            public Builder accentLeft(DocumentColor color, double width) {
                this.accentLeftColor = color;
                this.accentLeftWidth = width;
                return this;
            }

            /**
             * Sets the top accent bar.
             *
             * @param color the top accent-bar colour
             * @param width width in points of the top accent bar
             * @return this builder for chaining
             */
            public Builder accentTop(DocumentColor color, double width) {
                this.accentTopColor = color;
                this.accentTopWidth = width;
                return this;
            }

            /**
             * Sets the bottom accent bar.
             *
             * @param color the bottom accent-bar colour
             * @param width width in points of the bottom accent bar
             * @return this builder for chaining
             */
            public Builder accentBottom(DocumentColor color, double width) {
                this.accentBottomColor = color;
                this.accentBottomWidth = width;
                return this;
            }

            /**
             * Sets the title text style.
             *
             * @param value text style for the title; null suppresses the title
             * @return this builder for chaining
             */
            public Builder titleStyle(DocumentTextStyle value) {
                this.titleStyle = value;
                return this;
            }

            /**
             * Sets the horizontal alignment of the title.
             *
             * @param value horizontal alignment of the title
             * @return this builder for chaining
             */
            public Builder titleAlign(TextAlign value) {
                this.titleAlign = value;
                return this;
            }

            /**
             * Sets whether the title is transformed to uppercase.
             *
             * @param value {@code true} to uppercase the title
             * @return this builder for chaining
             */
            public Builder transformTitle(boolean value) {
                this.transformTitle = value;
                return this;
            }

            /**
             * Sets the base text style for the body.
             *
             * @param value base text style for the body
             * @return this builder for chaining
             */
            public Builder bodyStyle(DocumentTextStyle value) {
                this.bodyStyle = value;
                return this;
            }

            /**
             * Sets the horizontal alignment of the body.
             *
             * @param value horizontal alignment of the body
             * @return this builder for chaining
             */
            public Builder bodyAlign(TextAlign value) {
                this.bodyAlign = value;
                return this;
            }

            /**
             * Sets the extra space between wrapped body lines.
             *
             * @param value extra space between wrapped body lines
             * @return this builder for chaining
             */
            public Builder bodyLineSpacing(double value) {
                this.bodyLineSpacing = value;
                return this;
            }

            /**
             * Builds the {@link Style} from the configured values.
             *
             * @return a new {@code Style}
             */
            public Style build() {
                return new Style(spacing, padding, fillColor, cornerRadius,
                        accentLeftColor, accentLeftWidth,
                        accentTopColor, accentTopWidth,
                        accentBottomColor, accentBottomWidth,
                        titleStyle, titleAlign, transformTitle,
                        bodyStyle, bodyAlign, bodyLineSpacing);
            }
        }
    }
}
