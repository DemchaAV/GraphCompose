package com.demcha.compose.document.snapshot;

/**
 * Resolved page of a declared {@code anchor(...)} navigation destination — the
 * page the anchored node's top-left lands on after layout. An anchor is a point,
 * so it resolves to a single page.
 *
 * @param anchor the declared anchor name
 * @param page   zero-based page the anchor resolves to
 * @author Artem Demchyshyn
 * @see PageIndex
 * @since 1.9.0
 */
public record PageReference(String anchor, int page) {

    /**
     * Returns the 1-based printable page number of the anchor's page.
     *
     * @return {@code page + 1}
     */
    public int pageNumber() {
        return page + 1;
    }
}
