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

        public Style {
            padding = padding == null ? DocumentInsets.zero() : padding;
            titleAlign = titleAlign == null ? TextAlign.LEFT : titleAlign;
            bodyAlign = bodyAlign == null ? TextAlign.LEFT : bodyAlign;
        }

        public static Style defaults() {
            return builder().build();
        }

        public static Builder builder() {
            return new Builder();
        }

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

            public Builder spacing(double value) {
                this.spacing = value;
                return this;
            }

            public Builder padding(DocumentInsets value) {
                this.padding = value;
                return this;
            }

            public Builder fillColor(DocumentColor value) {
                this.fillColor = value;
                return this;
            }

            public Builder cornerRadius(DocumentCornerRadius value) {
                this.cornerRadius = value;
                return this;
            }

            public Builder accentLeft(DocumentColor color, double width) {
                this.accentLeftColor = color;
                this.accentLeftWidth = width;
                return this;
            }

            public Builder accentTop(DocumentColor color, double width) {
                this.accentTopColor = color;
                this.accentTopWidth = width;
                return this;
            }

            public Builder accentBottom(DocumentColor color, double width) {
                this.accentBottomColor = color;
                this.accentBottomWidth = width;
                return this;
            }

            public Builder titleStyle(DocumentTextStyle value) {
                this.titleStyle = value;
                return this;
            }

            public Builder titleAlign(TextAlign value) {
                this.titleAlign = value;
                return this;
            }

            public Builder transformTitle(boolean value) {
                this.transformTitle = value;
                return this;
            }

            public Builder bodyStyle(DocumentTextStyle value) {
                this.bodyStyle = value;
                return this;
            }

            public Builder bodyAlign(TextAlign value) {
                this.bodyAlign = value;
                return this;
            }

            public Builder bodyLineSpacing(double value) {
                this.bodyLineSpacing = value;
                return this;
            }

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
