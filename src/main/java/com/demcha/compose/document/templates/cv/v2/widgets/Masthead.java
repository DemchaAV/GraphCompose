package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvLink;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * CV masthead widget for centred editorial headers: name, optional
 * job title, compact contact metadata, and a separate link row.
 */
public final class Masthead {

    private Masthead() {
    }

    /**
     * Renders the centred masthead: name, optional job title, compact
     * contact metadata, and a separate link row.
     *
     * @param host     the section builder the masthead is appended to
     * @param identity the CV identity supplying name, contact, and links
     * @param theme    the active theme supplying palette, typography, and spacing
     * @param style    styling knobs for the masthead; {@code null} uses
     *                 {@link Style#defaults(CvTheme)}
     */
    public static void centered(SectionBuilder host,
                                CvIdentity identity,
                                CvTheme theme,
                                Style style) {
        Style safeStyle = style == null ? Style.defaults(theme) : style;
        DocumentTextStyle nameStyle = safeStyle.nameStyle() == null
                ? theme.headlineStyle()
                : safeStyle.nameStyle();
        Headline.uppercaseCentered(host, identity.name(), theme,
                nameStyle);
        addOptionalLine(host, identity.jobTitle(), safeStyle.titleStyle(),
                safeStyle.lineMargin());
        addOptionalLine(host,
                join(safeStyle.metaJoiner(),
                        identity.contact().phone(),
                        identity.contact().address()),
                safeStyle.metaStyle(), safeStyle.lineMargin());
        addLinkRow(host, identity, theme, safeStyle);
    }

    private static void addOptionalLine(SectionBuilder host,
                                        String text,
                                        DocumentTextStyle style,
                                        DocumentInsets margin) {
        if (style == null || text == null || text.isBlank()) {
            return;
        }
        host.addParagraph(paragraph -> paragraph
                .text(text)
                .textStyle(style)
                .align(TextAlign.CENTER)
                .margin(margin));
    }

    private static void addLinkRow(SectionBuilder host,
                                   CvIdentity identity,
                                   CvTheme theme,
                                   Style style) {
        List<LinkPart> parts = linkParts(identity);
        if (parts.isEmpty()) {
            return;
        }

        DocumentTextStyle base = style.metaStyle() == null
                ? theme.contactStyle()
                : style.metaStyle();
        DocumentTextStyle linkStyle = style.linkStyle() == null
                ? base
                : style.linkStyle();
        DocumentTextStyle separatorStyle = style.separatorStyle() == null
                ? base
                : style.separatorStyle();
        host.addParagraph(paragraph -> paragraph
                .textStyle(base)
                .align(TextAlign.CENTER)
                .margin(style.lineMargin())
                .rich(rich -> {
                    for (int i = 0; i < parts.size(); i++) {
                        LinkPart part = parts.get(i);
                        if (part.link() == null) {
                            rich.style(part.text(), base);
                        } else {
                            rich.with(part.text(), linkStyle, part.link());
                        }
                        if (i < parts.size() - 1) {
                            rich.style(theme.decoration().contactSeparator(),
                                    separatorStyle);
                        }
                    }
                }));
    }

    private static List<LinkPart> linkParts(CvIdentity identity) {
        List<LinkPart> parts = new ArrayList<>();
        String email = identity.contact().email();
        if (!email.isBlank()) {
            parts.add(new LinkPart(email,
                    new DocumentLinkOptions("mailto:" + email)));
        }
        for (CvLink link : identity.links()) {
            if (!link.label().isBlank()) {
                parts.add(new LinkPart(link.label(), link.url().isBlank()
                        ? null
                        : new DocumentLinkOptions(link.url())));
            }
        }
        return parts;
    }

    private static String join(String separator, String... values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(separator);
            }
            out.append(value.trim());
        }
        return out.toString();
    }

    /**
     * Styling knobs for the centred masthead.
     *
     * @param nameStyle      text style for the name headline
     * @param titleStyle     text style for the optional job-title line
     * @param metaStyle      text style for the compact contact metadata line
     * @param linkStyle      text style for clickable links in the link row
     * @param separatorStyle text style for the separator glyph between links
     * @param metaJoiner     string joining contact metadata values; defaults to {@code " - "}
     * @param lineMargin     outer margin applied to each masthead line
     */
    public record Style(DocumentTextStyle nameStyle,
                        DocumentTextStyle titleStyle,
                        DocumentTextStyle metaStyle,
                        DocumentTextStyle linkStyle,
                        DocumentTextStyle separatorStyle,
                        String metaJoiner,
                        DocumentInsets lineMargin) {

        /** Applies defaults for {@code metaJoiner} and {@code lineMargin}. */
        public Style {
            metaJoiner = metaJoiner == null ? " - " : metaJoiner;
            lineMargin = lineMargin == null
                    ? DocumentInsets.zero()
                    : lineMargin;
        }

        /**
         * Style derived from the theme's headline, body, contact, and
         * separator styles.
         *
         * @param theme the active theme supplying the default styles
         * @return a {@code Style} populated from the theme defaults
         */
        public static Style defaults(CvTheme theme) {
            return builder()
                    .nameStyle(theme.headlineStyle())
                    .titleStyle(theme.bodyStyle())
                    .metaStyle(theme.contactStyle())
                    .linkStyle(theme.contactStyle())
                    .separatorStyle(theme.contactSeparatorStyle())
                    .build();
        }

        /**
         * Creates a new {@link Builder} for the masthead style.
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
            private DocumentTextStyle nameStyle;
            private DocumentTextStyle titleStyle;
            private DocumentTextStyle metaStyle;
            private DocumentTextStyle linkStyle;
            private DocumentTextStyle separatorStyle;
            private String metaJoiner = " - ";
            private DocumentInsets lineMargin = DocumentInsets.zero();

            private Builder() {
            }

            /**
             * Sets the name headline style.
             *
             * @param value text style for the name headline
             * @return this builder for chaining
             */
            public Builder nameStyle(DocumentTextStyle value) {
                this.nameStyle = value;
                return this;
            }

            /**
             * Sets the optional job-title line style.
             *
             * @param value text style for the job-title line
             * @return this builder for chaining
             */
            public Builder titleStyle(DocumentTextStyle value) {
                this.titleStyle = value;
                return this;
            }

            /**
             * Sets the contact metadata line style.
             *
             * @param value text style for the contact metadata line
             * @return this builder for chaining
             */
            public Builder metaStyle(DocumentTextStyle value) {
                this.metaStyle = value;
                return this;
            }

            /**
             * Sets the clickable-link style for the link row.
             *
             * @param value text style for clickable links
             * @return this builder for chaining
             */
            public Builder linkStyle(DocumentTextStyle value) {
                this.linkStyle = value;
                return this;
            }

            /**
             * Sets the separator-glyph style between links.
             *
             * @param value text style for the separator glyph
             * @return this builder for chaining
             */
            public Builder separatorStyle(DocumentTextStyle value) {
                this.separatorStyle = value;
                return this;
            }

            /**
             * Sets the string joining contact metadata values.
             *
             * @param value the metadata joiner string
             * @return this builder for chaining
             */
            public Builder metaJoiner(String value) {
                this.metaJoiner = value;
                return this;
            }

            /**
             * Sets the outer margin applied to each masthead line.
             *
             * @param value outer margin for each line
             * @return this builder for chaining
             */
            public Builder lineMargin(DocumentInsets value) {
                this.lineMargin = value;
                return this;
            }

            /**
             * Builds the {@link Style} from the configured values.
             *
             * @return a new {@code Style}
             */
            public Style build() {
                return new Style(nameStyle, titleStyle, metaStyle, linkStyle,
                        separatorStyle, metaJoiner, lineMargin);
            }
        }
    }

    private record LinkPart(String text, DocumentLinkOptions link) {
    }
}
