package com.demcha.examples.support;

import com.demcha.examples.flagships.EngineDeckExample;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Renders the README hero banner straight to its committed PNG via the engine's
 * {@code DocumentSession.toImage(...)} path ({@code @since 1.9.0}) — the
 * replacement for the old render-to-PDF-then-{@link PdfPageRasterizer}
 * round-trip. The banner's version pill is sourced from the filtered
 * {@code banner.properties}, so running this after a version bump stamps the
 * hero with the current release. {@code cut-release.ps1} invokes it during the
 * release commit so {@code assets/readme/repository_showcase_render.png} ships
 * in lockstep with every tag, instead of drifting until someone re-renders by
 * hand.
 *
 * <p>Usage — pass an explicit output path (exec:java runs with the examples
 * module as its working directory, so a relative default would land in the
 * wrong place); DPI defaults to 200, matching the committed asset:</p>
 * <pre>
 * ./mvnw -B -ntp -f examples/pom.xml -DskipTests exec:java \
 *   -Dexec.mainClass=com.demcha.examples.support.ReadmeBannerRenderer \
 *   -Dexec.args="&lt;outputPng&gt; [dpi=200]"
 * </pre>
 *
 * @author Artem Demchyshyn
 * @since 1.9.0
 */
public final class ReadmeBannerRenderer {

    /** Resolution of the committed hero asset; high enough for a crisp README image. */
    private static final int DEFAULT_DPI = 200;

    private ReadmeBannerRenderer() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args[0].isBlank()) {
            System.err.println("Usage: ReadmeBannerRenderer <outputPng> [dpi=200]");
            System.exit(2);
        }
        Path outputPng = Paths.get(args[0]).toAbsolutePath().normalize();
        int dpi = args.length >= 2 ? Integer.parseInt(args[1]) : DEFAULT_DPI;

        Path written = render(outputPng, dpi);
        System.out.println("Rendered README banner -> " + written + " (" + dpi + " DPI)");
    }

    /**
     * Renders the hero banner and writes it as a PNG, creating parent
     * directories as needed.
     *
     * @param outputPng destination file
     * @param dpi       raster resolution in dots per inch
     * @return the written path
     * @throws Exception                if rendering fails
     * @throws IllegalStateException    if the PNG encoder rejects the image
     */
    public static Path render(Path outputPng, int dpi) throws Exception {
        BufferedImage image = EngineDeckExample.renderBannerImage(dpi);
        Path parent = outputPng.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!ImageIO.write(image, "png", outputPng.toFile())) {
            throw new IllegalStateException("No PNG ImageIO writer accepted the banner: " + outputPng);
        }
        return outputPng;
    }
}
