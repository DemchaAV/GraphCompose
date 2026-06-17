package com.demcha.compose;

/**
 * Shared SVG fixtures for the v1.8 vector-import benchmarks (path parse, whole
 * icon read, icon → node build).
 *
 * <p>Self-contained on purpose: the benchmarks module cannot reach the
 * main-module test constants or the examples module, so the heart path is
 * vendored here (it also lives in {@code SvgPathTest} / {@code VectorPathExample}
 * in their own modules). The icon is a synthetic but realistic multi-layer
 * document — a gradient-filled background, a {@code translate}+{@code scale}
 * group of filled paths and a stroked circle, and a {@code rotate} group with a
 * polygon and a quadratic-curve stroke — so it exercises XML parse, {@code <g>}
 * transform accumulation, gradient resolution and per-layer path lowering the
 * way a real exporter file would, while staying entirely within the reader's
 * supported subset (so it never throws).</p>
 *
 * @author Artem Demchyshyn
 */
public final class SvgBenchmarkFixtures {

    /** Material "favorite" heart — the same {@code d} used in the SVG tests/examples. */
    public static final String MATERIAL_HEART_D =
            "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3"
            + "c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5"
            + "c0 3.78-3.4 6.86-8.55 11.54L12 21.35z";

    /** Heart viewBox edge (square 24×24), passed to {@code SvgPath.parse}. */
    public static final double HEART_VIEWBOX = 24.0;

    /** A realistic multi-layer icon: gradient bg + transformed groups + stroked curves. */
    public static final String MULTI_LAYER_ICON_SVG = """
            <svg viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="sky" x1="0" y1="0" x2="0" y2="48" gradientUnits="userSpaceOnUse">
                  <stop offset="0" stop-color="#3b82f6"/>
                  <stop offset="1" stop-color="#1e3a8a"/>
                </linearGradient>
              </defs>
              <rect x="0" y="0" width="48" height="48" rx="6" fill="url(#sky)"/>
              <g transform="translate(6 6) scale(1.1)">
                <path d="M0 24 L12 4 L24 24 Z" fill="#fbbf24"/>
                <path d="M6 24 L16 10 L26 24 Z" fill="#f59e0b"/>
                <circle cx="20" cy="8" r="4" fill="#fde68a" stroke="#92400e" stroke-width="1.5"/>
              </g>
              <g transform="rotate(8 24 40)">
                <polygon points="4,40 44,40 40,46 8,46" fill="#10b981"/>
                <path d="M10 42 Q24 38 38 42" fill="none" stroke="#065f46" stroke-width="2"/>
              </g>
            </svg>
            """;

    private SvgBenchmarkFixtures() {
    }
}
