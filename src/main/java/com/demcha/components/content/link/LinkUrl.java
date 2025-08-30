package com.demcha.components.content.link;

import com.demcha.components.core.Component;

import java.util.Objects;

public  class LinkUrl implements Component {
    private final String url;

    public LinkUrl(String url) {
        this.url = url;
    }

    public String url() {
        return url;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LinkUrl) obj;
        return Objects.equals(this.url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "LinkUrl[" +
               "url=" + url + ']';
    }

}
