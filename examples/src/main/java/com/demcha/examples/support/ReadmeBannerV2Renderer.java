package com.demcha.examples.support;

import com.demcha.examples.flagships.EngineDeckV2Example;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Renders the modular engine-deck v2 banner as a parallel README preview.
 * This utility deliberately writes only to the explicit output path supplied
 * by the caller: it does not replace {@link ReadmeBannerRenderer} or change
 * the established release flow and its committed banner asset.
 *
 * <p>Usage — pass an explicit output path; DPI defaults to 200:</p>
 * <pre>
 * ./mvnw -B -ntp -f examples/pom.xml -DskipTests exec:java \
 *   -Dexec.mainClass=com.demcha.examples.support.ReadmeBannerV2Renderer \
 *   -Dexec.args="&lt;outputPng&gt; [dpi=200]"
 * </pre>
 *
 * @author Artem Demchyshyn
 * @since 2.0.0
 */
public final class ReadmeBannerV2Renderer {

    /** Resolution used when the caller does not request another preview DPI. */
    private static final int DEFAULT_DPI = 200;

    private ReadmeBannerV2Renderer() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args[0].isBlank()) {
            System.err.println("Usage: ReadmeBannerV2Renderer <outputPng> [dpi=200]");
            System.exit(2);
        }
        Path outputPng = Paths.get(args[0]).toAbsolutePath().normalize();
        int dpi = args.length >= 2 ? Integer.parseInt(args[1]) : DEFAULT_DPI;

        Path written = render(outputPng, dpi);
        System.out.println("Rendered README banner v2 preview -> " + written + " (" + dpi + " DPI)");
    }

    /**
     * Renders the v2 banner and writes it as a PNG, creating parent directories
     * as needed.
     *
     * @param outputPng destination file
     * @param dpi       raster resolution in dots per inch
     * @return the written path
     * @throws Exception             if rendering fails
     * @throws IllegalStateException if the PNG encoder rejects the image
     */
    public static Path render(Path outputPng, int dpi) throws Exception {
        BufferedImage image = EngineDeckV2Example.renderBannerImage(dpi);
        Path parent = outputPng.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!ImageIO.write(image, "png", outputPng.toFile())) {
            throw new IllegalStateException("No PNG ImageIO writer accepted the v2 banner: " + outputPng);
        }
        return outputPng;
    }
}
