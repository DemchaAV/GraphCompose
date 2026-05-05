package com.demcha.compose.document.layout;

/**
 * Mutable bookkeeping for the page-flow path of {@link LayoutCompiler}: the
 * canvas the document is being placed on, the active page index, the height
 * already consumed on that page, and the highest page touched so far.
 *
 * <p>Lifted to a sibling class (Phase E.4) so {@link MutatingPlacementContext}
 * can hold and mutate the same state object that {@code LayoutCompiler} reads
 * from. Stays package-private — callers outside {@code document.layout} should
 * use {@link PlacementContext} for placement-strategy access.</p>
 */
final class CompilerState {
    final LayoutCanvas canvas;
    int pageIndex;
    double usedHeight;
    int maxTouchedPage = -1;

    CompilerState(LayoutCanvas canvas) {
        this.canvas = canvas;
    }

    double remainingHeight() {
        return Math.max(0.0, canvas.innerHeight() - usedHeight);
    }

    double pageTop() {
        return canvas.height() - canvas.margin().top();
    }

    void newPage() {
        pageIndex++;
        usedHeight = 0.0;
        touchPage();
    }

    void touchPage() {
        maxTouchedPage = Math.max(maxTouchedPage, pageIndex);
    }
}
