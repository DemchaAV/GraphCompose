package com.demcha.examples.support;

import com.demcha.examples.flagships.MavenBannerPptxExample;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Renders the README hero image — the first page of the Maven Central banner.
 *
 * <p>The banner carries the released version in its own text, so the hero is not a
 * picture that can be drawn once: {@code cut-release.ps1} runs this after the version
 * bump and commits what it writes, which is why the entry point takes the destination
 * as an argument rather than deciding it. The banner itself is
 * {@link MavenBannerPptxExample} — the same document the release publishes as a PPTX
 * and a PDF, so the image beside README is a page of a file a reader can open.</p>
 *
 * <p>Usage — pass an explicit output path; DPI defaults to 200:</p>
 * <pre>
 * ./mvnw -B -ntp -f examples/pom.xml -DskipTests exec:java \
 *   -Dexec.mainClass=com.demcha.examples.support.ReadmeHeroRenderer \
 *   -Dexec.args="&lt;outputPng&gt; [dpi=200]"
 * </pre>
 *
 * @author Artem Demchyshyn
 * @since 2.0.0
 */
public final class ReadmeHeroRenderer {

    /** Resolution used when the caller does not request another preview DPI. */
    private static final int DEFAULT_DPI = 200;

    private ReadmeHeroRenderer() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args[0].isBlank()) {
            System.err.println("Usage: ReadmeHeroRenderer <outputPng> [dpi=200]");
            System.exit(2);
        }
        Path outputPng = Paths.get(args[0]).toAbsolutePath().normalize();
        int dpi = args.length >= 2 ? Integer.parseInt(args[1]) : DEFAULT_DPI;

        Path written = render(outputPng, dpi);
        System.out.println("Rendered README banner -> " + written + " (" + dpi + " DPI)");
    }

    /**
     * Renders the banner's first page and writes it as a PNG, creating parent
     * directories as needed.
     *
     * @param outputPng destination file
     * @param dpi       raster resolution in dots per inch
     * @return the written path
     * @throws Exception if rendering fails
     */
    public static Path render(Path outputPng, int dpi) throws Exception {
        return MavenBannerPptxExample.renderPreview(outputPng, dpi);
    }
}
