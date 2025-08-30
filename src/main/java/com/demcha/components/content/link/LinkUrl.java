package com.demcha.components.content.link;

import com.demcha.components.core.Component;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Getter
@EqualsAndHashCode
public class LinkUrl implements Component {
    private final String url;
    private final boolean valid;

    public LinkUrl(String url) {
        this.url = url;
        this.valid = validateUrl(url);
    }

    private boolean validateUrl(String url) {
        if (url == null || url.isBlank()) {
            log.warn("URL is null or blank");
            return false;
        }
        try {
            URI uri = new URI(url);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (URISyntaxException e) {
            log.warn("Invalid link url: '{}'", url);
            return false;
        }
    }
}
