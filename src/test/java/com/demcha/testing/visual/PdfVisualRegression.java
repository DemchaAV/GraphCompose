package com.demcha.testing.visual;

import com.demcha.compose.devtool.PdfRenderBridge;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Test harness for "render PDF → diff PNG" visual regression checks.
 *
 * <p>Each baseline lives at {@code src/test/resources/visual-baselines/&lt;name&gt;-page-N.png}.
 * In the default mode the harness renders the supplied PDF, converts each page
 * to a {@link BufferedImage} via {@link PdfRenderBridge}, and compares against
 * the baseline using {@link ImageDiff}. A failing comparison writes the actual
 * render and the diff image next to the baseline for inspection.</p>
 *
 * <p>To re-bless baselines, run the test with the system property
 * {@code -Dgraphcompose.visual.approve=true} (or environment variable
 * {@code GRAPHCOMPOSE_VISUAL_APPROVE=true}). In approve mode the harness simply
 * writes the current renders to the baseline location and skips the diff
 * assertion.</p>
 *
 * @author Artem Demchyshyn
 */
public final class PdfVisualRegression {

    private static final String APPROVE_SYS_PROP = "graphcompose.visual.approve";
    private static final String APPROVE_ENV_VAR = "GRAPHCOMPOSE_VISUAL_APPROVE";

    private final Path baselineRoot;
    private final float renderScale;
    private final int perPixelTolerance;
    private final long mismatchedPixelBudget;

    private PdfVisualRegression(Path baselineRoot,
                                float renderScale,
                                int perPixelTolerance,
                                long mismatchedPixelBudget) {
        this.baselineRoot = baselineRoot;
        this.renderScale = renderScale;
        this.perPixelTolerance = perPixelTolerance;
        this.mismatchedPixelBudget = mismatchedPixelBudget;
    }

    /**
     * Creates a regression harness with the canonical defaults
     * ({@code src/test/resources/visual-baselines}, scale 1.0, tolerance 6,
     * budget 0 mismatched pixels).
     *
     * @return regression harness
     */
    public static PdfVisualRegression standard() {
        return new PdfVisualRegression(
                Path.of("src", "test", "resources", "visual-baselines"),
                1.0f,
                6,
                0L);
    }

    /**
     * Returns a copy with a different baseline directory.
     *
     * @param baselineRoot baseline directory
     * @return updated harness
     */
    public PdfVisualRegression baselineRoot(Path baselineRoot) {
        return new PdfVisualRegression(Objects.requireNonNull(baselineRoot, "baselineRoot"),
                renderScale, perPixelTolerance, mismatchedPixelBudget);
    }

    /**
     * Returns a copy with a different render scale (1.0 = native, 2.0 = retina).
     *
     * @param renderScale render scale
     * @return updated harness
     */
    public PdfVisualRegression renderScale(float renderScale) {
        if (renderScale <= 0) {
            throw new IllegalArgumentException("renderScale must be > 0");
        }
        return new PdfVisualRegression(baselineRoot, renderScale, perPixelTolerance, mismatchedPixelBudget);
    }

    /**
     * Returns a copy with a different per-pixel tolerance (0..255).
     *
     * @param perPixelTolerance tolerance per channel
     * @return updated harness
     */
    public PdfVisualRegression perPixelTolerance(int perPixelTolerance) {
        return new PdfVisualRegression(baselineRoot, renderScale, perPixelTolerance, mismatchedPixelBudget);
    }

    /**
     * Returns a copy with a different mismatched-pixel budget.
     *
     * @param mismatchedPixelBudget maximum allowed mismatched pixels
     * @return updated harness
     */
    public PdfVisualRegression mismatchedPixelBudget(long mismatchedPixelBudget) {
        if (mismatchedPixelBudget < 0) {
            throw new IllegalArgumentException("mismatchedPixelBudget cannot be negative");
        }
        return new PdfVisualRegression(baselineRoot, renderScale, perPixelTolerance, mismatchedPixelBudget);
    }

    /**
     * Renders {@code pdfBytes} page by page and compares each page against the
     * stored baseline. Throws an {@link AssertionError} when any page differs
     * beyond the configured budget.
     *
     * @param baselineName baseline base name (no extension, no page suffix)
     * @param pdfBytes rendered PDF bytes
     * @throws IOException when reading or writing baseline files fails
     */
    public void assertMatchesBaseline(String baselineName, byte[] pdfBytes) throws IOException {
        Objects.requireNonNull(baselineName, "baselineName");
        Objects.requireNonNull(pdfBytes, "pdfBytes");

        List<BufferedImage> rendered = renderPages(pdfBytes);
        Files.createDirectories(baselineRoot);

        if (approveMode()) {
            for (int page = 0; page < rendered.size(); page++) {
                Path baseline = baselinePath(baselineName, page);
                ImageIO.write(rendered.get(page), "png", baseline.toFile());
            }
            return;
        }

        List<String> failures = new ArrayList<>();
        for (int page = 0; page < rendered.size(); page++) {
            Path baseline = baselinePath(baselineName, page);
            if (!Files.exists(baseline)) {
                Path actualOut = sidecarPath(baselineName, page, "actual");
                ImageIO.write(rendered.get(page), "png", actualOut.toFile());
                failures.add("Missing baseline " + baseline + " — wrote rendered output to " + actualOut
                        + ". Re-run with -D" + APPROVE_SYS_PROP + "=true to approve.");
                continue;
            }
            BufferedImage expected = ImageIO.read(baseline.toFile());
            ImageDiff.Result diff = ImageDiff.compare(expected, rendered.get(page), perPixelTolerance);
            if (!diff.withinBudget(mismatchedPixelBudget)) {
                Path actualOut = sidecarPath(baselineName, page, "actual");
                Path diffOut = sidecarPath(baselineName, page, "diff");
                ImageIO.write(rendered.get(page), "png", actualOut.toFile());
                if (diff.diffImage() != null) {
                    ImageIO.write(diff.diffImage(), "png", diffOut.toFile());
                }
                failures.add("Visual diff over budget for " + baseline + " — " + diff.summary()
                        + ". Wrote actual=" + actualOut + " diff=" + diffOut);
            }
        }

        if (!failures.isEmpty()) {
            throw new AssertionError(String.join("\n", failures));
        }
    }

    /**
     * Renders {@code pdfBytes} into a list of one image per page, useful when a
     * test wants to call {@link ImageDiff} directly.
     *
     * @param pdfBytes rendered PDF bytes
     * @return list of page images at the configured render scale
     * @throws IOException when PDF parsing or rendering fails
     */
    public List<BufferedImage> renderPages(byte[] pdfBytes) throws IOException {
        Objects.requireNonNull(pdfBytes, "pdfBytes");
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            List<BufferedImage> pages = new ArrayList<>(document.getNumberOfPages());
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                pages.add(PdfRenderBridge.renderToImage(document, i, renderScale));
            }
            return pages;
        }
    }

    private Path baselinePath(String baselineName, int pageIndex) {
        return baselineRoot.resolve(baselineName + "-page-" + pageIndex + ".png");
    }

    private Path sidecarPath(String baselineName, int pageIndex, String suffix) {
        return baselineRoot.resolve(baselineName + "-page-" + pageIndex + "." + suffix + ".png");
    }

    private static boolean approveMode() {
        String prop = System.getProperty(APPROVE_SYS_PROP);
        if (prop != null) {
            return Boolean.parseBoolean(prop);
        }
        String env = System.getenv(APPROVE_ENV_VAR);
        return env != null && Boolean.parseBoolean(env);
    }
}
