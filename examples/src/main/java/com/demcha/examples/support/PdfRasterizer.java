package com.demcha.examples.support;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

/**
 * Tiny dev/review utility: rasterizes each page of a PDF to a PNG via
 * PDFBox so generated documents can be eyeballed without a system
 * Ghostscript/poppler install.
 *
 * <pre>{@code
 * exec:java -Dexec.mainClass=com.demcha.examples.support.PdfRasterizer \
 *   -Dexec.args="path/to/doc.pdf out/prefix 140"
 * }</pre>
 *
 * <p>Writes {@code prefix-p0.png}, {@code prefix-p1.png}, … one per
 * page.</p>
 */
public final class PdfRasterizer {

    private PdfRasterizer() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: PdfRasterizer <pdf> <outPrefix> [dpi]");
            System.exit(2);
        }
        Path pdf = Path.of(args[0]);
        String prefix = args[1];
        float dpi = args.length > 2 ? Float.parseFloat(args[2]) : 140f;

        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int pages = doc.getNumberOfPages();
            for (int i = 0; i < pages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, dpi);
                File out = new File(prefix + "-p" + i + ".png");
                ImageIO.write(image, "png", out);
                System.out.println("Wrote: " + out.getAbsolutePath());
            }
        }
    }
}
