package com.demcha.compose.document.templates.data.common;

import com.demcha.compose.engine.components.content.link.LinkUrl;
import lombok.Data;

/**
 * Mutable hyperlink payload used by canonical template header and profile data.
 *
 * <p><b>Pipeline role:</b> stores a destination plus display text that shared
 * template composers translate into visible contact links.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
@Data
public class LinkYml {
    private LinkUrl linkUrl;
    private String displayText;

    /**
     * Creates an empty mutable link payload.
     */
    public LinkYml() {
    }

    /**
     * Starts a fluent link builder.
     *
     * @return link builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a link payload from URL and display text.
     *
     * @param url target URL
     * @param displayText visible link text
     * @return link payload
     */
    public static LinkYml of(String url, String displayText) {
        return builder()
                .url(url)
                .displayText(displayText)
                .build();
    }

    /**
     * Fluent builder for template hyperlink metadata.
     */
    public static final class Builder {
        private LinkUrl linkUrl;
        private String displayText;

        private Builder() {
        }

        /**
         * Sets the typed link URL.
         *
         * @param linkUrl link URL
         * @return this builder
         */
        public Builder linkUrl(LinkUrl linkUrl) {
            this.linkUrl = linkUrl;
            return this;
        }

        /**
         * Sets the link URL from text.
         *
         * @param url target URL
         * @return this builder
         */
        public Builder url(String url) {
            this.linkUrl = url == null || url.isBlank() ? null : new LinkUrl(url);
            return this;
        }

        /**
         * Sets the visible link text.
         *
         * @param displayText visible link text
         * @return this builder
         */
        public Builder displayText(String displayText) {
            this.displayText = displayText;
            return this;
        }

        /**
         * Builds the mutable link payload.
         *
         * @return link payload
         */
        public LinkYml build() {
            LinkYml link = new LinkYml();
            link.setLinkUrl(linkUrl);
            link.setDisplayText(displayText);
            return link;
        }
    }
}
