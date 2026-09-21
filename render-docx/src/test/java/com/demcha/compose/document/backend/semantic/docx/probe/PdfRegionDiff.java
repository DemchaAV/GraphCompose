package com.demcha.compose.document.backend.semantic.docx.probe;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Rasterises two PDFs and compares them region by region.
 *
 * <p>A single similarity percentage over a whole page is the wrong instrument here. An
 * A4 page is mostly margin, so a paragraph that vanished entirely still scores well
 * above ninety percent, and a result that cannot tell "identical" from "the body text is
 * gone" is not a measurement. This splits each page into a grid and reports every cell
 * separately, plus the one comparison that needs no tolerance at all: a cell carrying ink
 * in the reference and none in the candidate means content was lost, whatever the
 * percentages say.</p>
 *
 * <p>Nothing here decides what an acceptable difference is. It reports what the
 * difference is; the budgets belong to whichever corpus is being measured, and are not
 * verified until something measures them.</p>
 *
 * @author Artem Demchyshyn
 */
public final class PdfRegionDiff {

    /** Rasterisation resolution. Fixed so two runs are comparable. */
    public static final int DPI = 144;

    /** Grid resolution used when a caller does not choose one. */
    public static final int GRID = 12;

    /**
     * Per-channel difference below which two pixels count as equal. Covers
     * antialiasing only; it is deliberately far too small to hide a moved glyph.
     */
    private static final int PIXEL_TOLERANCE = 12;

    /** Fraction of differing pixels above which a cell counts as carrying ink. */
    private static final double INK_THRESHOLD = 0.002;

    private PdfRegionDiff() {
    }

    /**
     * The outcome of comparing two rendered documents.
     *
     * @param pageCountMatches whether both PDFs have the same number of pages
     * @param referencePages page count of the reference
     * @param candidatePages page count of the candidate
     * @param sizeMismatches human-readable page-size differences, empty when none
     * @param cells one entry per compared grid cell
     */
    public record Report(boolean pageCountMatches,
                         int referencePages,
                         int candidatePages,
                         List<String> sizeMismatches,
                         List<Cell> cells) {

        /** @return cells whose reference had ink and whose candidate is blank */
        public List<Cell> lostContent() {
            return cells.stream().filter(Cell::contentLost).toList();
        }

        /**
         * @param budget maximum tolerated fraction of differing pixels in one cell
         * @return cells exceeding that budget, worst first
         */
        public List<Cell> over(double budget) {
            return cells.stream()
                    .filter(c -> c.differingFraction() > budget)
                    .sorted((a, b) -> Double.compare(b.differingFraction(), a.differingFraction()))
                    .toList();
        }

        /** @return the worst differing fraction across every cell, or 0 when there are none */
        public double worstCell() {
            return cells.stream().mapToDouble(Cell::differingFraction).max().orElse(0);
        }
    }

    /**
     * One grid cell of one page.
     *
     * @param page zero-based page index
     * @param column zero-based grid column
     * @param row zero-based grid row
     * @param differingFraction fraction of pixels differing beyond the antialiasing tolerance
     * @param referenceHasInk whether the reference cell carries any non-background pixels
     * @param candidateHasInk whether the candidate cell carries any non-background pixels
     */
    public record Cell(int page,
                       int column,
                       int row,
                       double differingFraction,
                       boolean referenceHasInk,
                       boolean candidateHasInk) {

        /** @return true when the reference drew something here and the candidate drew nothing */
        public boolean contentLost() {
            return referenceHasInk && !candidateHasInk;
        }
    }

    /**
     * Compares two PDFs at {@link #DPI} on a {@link #GRID}-square grid.
     *
     * @param reference the PDF that defines the expected look
     * @param candidate the PDF produced from the export under test
     * @param diffOutput directory to receive, per page, the reference render, the editor's
     *                   render and the overlay of the two; {@code null} writes nothing
     * @return the comparison report
     * @throws IOException if either file cannot be read or a diff image cannot be written
     */
    public static Report compare(Path reference, Path candidate, Path diffOutput) throws IOException {
        try (PDDocument ref = Loader.loadPDF(reference.toFile());
             PDDocument cand = Loader.loadPDF(candidate.toFile())) {

            List<String> sizeMismatches = new ArrayList<>();
            int comparable = Math.min(ref.getNumberOfPages(), cand.getNumberOfPages());
            for (int page = 0; page < comparable; page++) {
                float refWidth = ref.getPage(page).getMediaBox().getWidth();
                float refHeight = ref.getPage(page).getMediaBox().getHeight();
                float candWidth = cand.getPage(page).getMediaBox().getWidth();
                float candHeight = cand.getPage(page).getMediaBox().getHeight();
                // Half a point: below that the two are the same box written differently.
                if (Math.abs(refWidth - candWidth) > 0.5f || Math.abs(refHeight - candHeight) > 0.5f) {
                    sizeMismatches.add("page %d: reference %.1fx%.1f, candidate %.1fx%.1f"
                            .formatted(page + 1, refWidth, refHeight, candWidth, candHeight));
                }
            }

            PDFRenderer refRenderer = new PDFRenderer(ref);
            PDFRenderer candRenderer = new PDFRenderer(cand);
            List<Cell> cells = new ArrayList<>();
            if (diffOutput != null) {
                Files.createDirectories(diffOutput);
            }

            for (int page = 0; page < comparable; page++) {
                BufferedImage a = refRenderer.renderImageWithDPI(page, DPI, ImageType.RGB);
                BufferedImage b = candRenderer.renderImageWithDPI(page, DPI, ImageType.RGB);
                BufferedImage diff = diffOutput == null ? null
                        : new BufferedImage(Math.min(a.getWidth(), b.getWidth()),
                        Math.min(a.getHeight(), b.getHeight()), BufferedImage.TYPE_INT_RGB);
                cells.addAll(comparePage(page, a, b, diff));
                if (diff != null) {
                    // The overlay shows where the two disagree; the two page images
                    // show what each side actually drew. A reader needs all three:
                    // an overlay alone cannot say which of the two is wrong.
                    ImageIO.write(diff, "png",
                            diffOutput.resolve("page-%d-diff.png".formatted(page + 1)).toFile());
                    ImageIO.write(a, "png",
                            diffOutput.resolve("page-%d-reference.png".formatted(page + 1)).toFile());
                    ImageIO.write(b, "png",
                            diffOutput.resolve("page-%d-editor.png".formatted(page + 1)).toFile());
                }
            }

            return new Report(ref.getNumberOfPages() == cand.getNumberOfPages(),
                    ref.getNumberOfPages(), cand.getNumberOfPages(), sizeMismatches, cells);
        }
    }

    private static List<Cell> comparePage(int page, BufferedImage a, BufferedImage b, BufferedImage diff) {
        int width = Math.min(a.getWidth(), b.getWidth());
        int height = Math.min(a.getHeight(), b.getHeight());
        int[] differing = new int[GRID * GRID];
        int[] total = new int[GRID * GRID];
        int[] refInk = new int[GRID * GRID];
        int[] candInk = new int[GRID * GRID];

        for (int y = 0; y < height; y++) {
            int row = Math.min(GRID - 1, y * GRID / height);
            for (int x = 0; x < width; x++) {
                int cell = Math.min(GRID - 1, x * GRID / width) + row * GRID;
                int pa = a.getRGB(x, y);
                int pb = b.getRGB(x, y);
                total[cell]++;
                if (isInk(pa)) {
                    refInk[cell]++;
                }
                if (isInk(pb)) {
                    candInk[cell]++;
                }
                boolean differs = channelDelta(pa, pb) > PIXEL_TOLERANCE;
                if (differs) {
                    differing[cell]++;
                }
                if (diff != null) {
                    // Differences in red over a faded reference, so a reader can see
                    // both what changed and where on the page it sits.
                    diff.setRGB(x, y, differs ? 0xFFE0263C : fade(pa));
                }
            }
        }

        List<Cell> cells = new ArrayList<>(GRID * GRID);
        for (int index = 0; index < GRID * GRID; index++) {
            if (total[index] == 0) {
                continue;
            }
            cells.add(new Cell(page, index % GRID, index / GRID,
                    (double) differing[index] / total[index],
                    (double) refInk[index] / total[index] > INK_THRESHOLD,
                    (double) candInk[index] / total[index] > INK_THRESHOLD));
        }
        return cells;
    }

    /** A pixel counts as ink when it is meaningfully darker than paper white. */
    private static boolean isInk(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r + g + b) / 3 < 232;
    }

    private static int channelDelta(int first, int second) {
        return Math.max(Math.abs(((first >> 16) & 0xFF) - ((second >> 16) & 0xFF)),
                Math.max(Math.abs(((first >> 8) & 0xFF) - ((second >> 8) & 0xFF)),
                        Math.abs((first & 0xFF) - (second & 0xFF))));
    }

    private static int fade(int rgb) {
        int r = 255 - (255 - ((rgb >> 16) & 0xFF)) / 4;
        int g = 255 - (255 - ((rgb >> 8) & 0xFF)) / 4;
        int b = 255 - (255 - (rgb & 0xFF)) / 4;
        return (r << 16) | (g << 8) | b;
    }
}
