package com.demcha.compose.fonts;

/**
 * Marker and metadata for the {@code graph-compose-fonts} companion artifact.
 *
 * <p>This artifact carries the bundled Google font binaries that the engine's
 * {@code com.demcha.compose.font.DefaultFonts} catalog resolves from the
 * classpath. The fonts were split out of the core {@code graph-compose} jar in
 * the engine's v1.8.0 cycle so that the engine stays lean and the two release on
 * independent cadences — upgrading the engine never re-downloads the fonts.</p>
 *
 * <p>There is no API to call here: simply having this jar on the classpath makes
 * the bundled families available. The font <em>descriptors</em> and the public
 * {@code FontName} constants live in the engine, not in this module, so this
 * class intentionally depends on nothing from the engine.</p>
 *
 * @author Artem Demchyshyn
 * @since 1.0.0
 */
public final class GraphComposeFonts {

    /**
     * Classpath resource prefix under which the bundled font families are
     * packaged (for example {@code fonts/google/lato/Lato-Regular.ttf}). This
     * layout is preserved byte-for-byte from the pre-split engine jar.
     */
    public static final String RESOURCE_ROOT = "fonts/google/";

    private GraphComposeFonts() {
    }

    /**
     * Reports whether the bundled font binaries are reachable on the current
     * classpath. A {@code true} result means this artifact (or an equivalent
     * resource set) is present and the engine's bundled Google families will
     * resolve; {@code false} means only the standard-14 PDF fonts are available.
     *
     * @return {@code true} if the bundled font resources are on the classpath
     */
    public static boolean isAvailable() {
        return GraphComposeFonts.class.getClassLoader()
                .getResource(RESOURCE_ROOT + "lato/Lato-Regular.ttf") != null;
    }
}
