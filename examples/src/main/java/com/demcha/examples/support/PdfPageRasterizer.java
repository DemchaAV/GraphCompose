package com.demcha.examples.support;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Rasterises one page of a PDF into a PNG at a chosen DPI. Tiny CLI built
 * on top of PDFBox so this repo never has to depend on Ghostscript /
 * pdftoppm / ImageMagick for README hero asset regeneration. Used during
 * v1.6.6 release prep to produce the new {@code repository_showcase_render.png}
 * (CV mint-editorial page 1 hero) without an external rasterizer.
 *
 * <p>Usage:</p>
 * <pre>
 * ./mvnw -B -ntp -f examples/pom.xml -DskipTests exec:java \
 *   -Dexec.mainClass=com.demcha.examples.support.PdfPageRasterizer \
 *   -Dexec.args="&lt;inputPdf&gt; &lt;outputPng&gt; [pageIndex0=0] [dpi=200]"
 * </pre>
 *
 * <p>Defaults: page index 0 (first page), 200 DPI (~1654 × 2339 for A4 —
 * comfortable for README hero quality).</p>
 *
 * @author Artem Demchyshyn
 * @since 1.6.6
 */
public final class PdfPageRasterizer {

    private PdfPageRasterizer() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: PdfPageRasterizer <inputPdf> <outputPng> [pageIndex0=0] [dpi=200]");
            System.exit(2);
        }
        Path inputPdf = Paths.get(args[0]).toAbsolutePath();
        Path outputPng = Paths.get(args[1]).toAbsolutePath();
        int pageIndex = args.length >= 3 ? Integer.parseInt(args[2]) : 0;
        float dpi = args.length >= 4 ? Float.parseFloat(args[3]) : 200f;

        if (!Files.isRegularFile(inputPdf)) {
            System.err.println("Input PDF not found: " + inputPdf);
            System.exit(3);
        }
        Files.createDirectories(outputPng.getParent());

        try (PDDocument doc = Loader.loadPDF(inputPdf.toFile())) {
            int totalPages = doc.getNumberOfPages();
            if (pageIndex < 0 || pageIndex >= totalPages) {
                System.err.println("Page index " + pageIndex + " out of range (0.." + (totalPages - 1) + ")");
                System.exit(4);
            }
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
            if (!ImageIO.write(image, "png", outputPng.toFile())) {
                System.err.println("Failed to write PNG to " + outputPng);
                System.exit(5);
            }
            System.out.println("Rasterised page " + pageIndex + " of " + inputPdf
                    + " at " + dpi + " DPI -> " + outputPng
                    + " (" + image.getWidth() + " x " + image.getHeight() + " px)");
        }
    }
}
