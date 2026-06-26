package com.demcha.compose.document.layout;

import com.demcha.compose.engine.components.style.Margin;

import java.util.List;
import java.util.Objects;

/**
 * Resolves page geometry (margin, inner width, inner height) as a function of the
 * page index, so the compiler can place content against a margin that varies from
 * page to page. Backed by the document-wide {@link LayoutCanvas} plus a list of
 * {@link PageMarginOverride}s; a page no override covers resolves to the canvas's
 * own margin.
 *
 * <p>Overrides are consulted in list order and the <em>last</em> one that covers a
 * page wins (mirroring the page-background layering rule). When the override list
 * is empty the resolver returns the canvas geometry unchanged for every page, so a
 * document with no per-page margins is laid out exactly as before.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public final class PageGeometry {

    private final LayoutCanvas canvas;
    private final List<PageMarginOverride> overrides;

    private PageGeometry(LayoutCanvas canvas, List<PageMarginOverride> overrides) {
        this.canvas = Objects.requireNonNull(canvas, "canvas");
        this.overrides = List.copyOf(overrides);
    }

    /**
     * Creates a resolver for the given canvas and per-page overrides.
     *
     * @param canvas    the document-wide canvas (its margin is the fallback)
     * @param overrides per-page margin overrides; may be empty
     * @return a page-geometry resolver
     */
    public static PageGeometry of(LayoutCanvas canvas, List<PageMarginOverride> overrides) {
        return new PageGeometry(canvas, overrides == null ? List.of() : overrides);
    }

    /**
     * The margin to apply on the given page.
     *
     * @param pageIndex 0-based page index
     * @return the last covering override's margin, or the canvas margin
     */
    public Margin marginForPage(int pageIndex) {
        Margin resolved = canvas.margin();
        for (PageMarginOverride override : overrides) {
            if (override.coversPage(pageIndex)) {
                resolved = override.margin();
            }
        }
        return resolved;
    }

    /**
     * The content width on the given page (page width minus the page's horizontal margin).
     *
     * @param pageIndex 0-based page index
     * @return inner width, clamped to be non-negative
     */
    public double innerWidthForPage(int pageIndex) {
        return Math.max(0.0, canvas.width() - marginForPage(pageIndex).horizontal());
    }

    /**
     * The content height on the given page (page height minus the page's vertical margin).
     *
     * @param pageIndex 0-based page index
     * @return inner height, clamped to be non-negative
     */
    public double innerHeightForPage(int pageIndex) {
        return Math.max(0.0, canvas.height() - marginForPage(pageIndex).vertical());
    }
}
