package com.demcha.compose.document.templates.data.common;

import com.demcha.compose.engine.components.content.link.LinkUrl;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
/**
 * Mutable hyperlink payload used by canonical template header and profile data.
 *
 * <p><b>Pipeline role:</b> stores a destination plus display text that shared
 * template composers translate into visible contact links.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
public class LinkYml {
    private LinkUrl linkUrl;
    private String displayText;

    public static Builder builder() {
        return new Builder();
    }

    public static LinkYml of(String url, String displayText) {
        return builder()
                .url(url)
                .displayText(displayText)
                .build();
    }

    public static final class Builder {
        private LinkUrl linkUrl;
        private String displayText;

        private Builder() {
        }

        public Builder linkUrl(LinkUrl linkUrl) {
            this.linkUrl = linkUrl;
            return this;
        }

        public Builder url(String url) {
            this.linkUrl = url == null || url.isBlank() ? null : new LinkUrl(url);
            return this;
        }

        public Builder displayText(String displayText) {
            this.displayText = displayText;
            return this;
        }

        public LinkYml build() {
            LinkYml link = new LinkYml();
            link.setLinkUrl(linkUrl);
            link.setDisplayText(displayText);
            return link;
        }
    }
}
