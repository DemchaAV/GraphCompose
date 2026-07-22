package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.DocumentDsl;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Produces the reviewable side-by-side parity artifacts for the vector
 * capabilities: the same session rendered to {@code demo.pdf} and
 * {@code demo.pptx} under {@code target/visual-tests/pptx-parity/}, plus a
 * PNG per page from each format at matching scale. The PDF raster is exact;
 * the PPTX raster uses POI's Graphics2D drawing (a preview, not PowerPoint's
 * renderer) — good for geometry inspection, approximate for text shaping.
 */
class PptxParityDemoTest {

    private static final double SCALE = 2.0;

    @Test
    void writesTheVectorParityDemoPair() throws Exception {
        Path outputDirectory = Path.of("target", "visual-tests", "pptx-parity", "vector-shapes");
        Files.createDirectories(outputDirectory);

        try (DocumentSession session = composeDemo()) {
            byte[] pdf = session.toPdfBytes();
            Files.write(outputDirectory.resolve("demo.pdf"), pdf);

            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            Files.write(outputDirectory.resolve("demo.pptx"), pptx);

            List<BufferedImage> pdfPages = session.toImages((int) Math.round(72 * SCALE));
            for (int i = 0; i < pdfPages.size(); i++) {
                ImageIO.write(pdfPages.get(i), "png",
                        outputDirectory.resolve("demo-page-" + (i + 1) + ".pdf.png").toFile());
            }

            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                List<XSLFSlide> slides = show.getSlides();
                assertThat(slides).hasSameSizeAs(pdfPages);
                for (int i = 0; i < slides.size(); i++) {
                    BufferedImage image = rasterize(slides.get(i),
                            show.getPageSize().width, show.getPageSize().height);
                    ImageIO.write(image, "png",
                            outputDirectory.resolve("demo-page-" + (i + 1) + ".pptx.png").toFile());
                }
            }
        }
    }

    private static DocumentSession composeDemo() {
        DocumentSession session = GraphCompose.document()
                .pageSize(400, 300)
                .margin(DocumentInsets.of(20))
                .create();
        DocumentDsl dsl = session.dsl();
        session.add(dsl.shape().name("HeroCard").size(200, 70)
                .fillColor(DocumentColor.ROYAL_BLUE)
                .stroke(DocumentStroke.of(DocumentColor.BLACK, 2))
                .cornerRadius(10)
                .build());
        session.add(dsl.shape().name("AccentBar").size(320, 14)
                .fillColor(DocumentColor.ORANGE)
                .build());
        session.add(new EllipseBuilder().name("Badge").circle(44)
                .fillColor(DocumentColor.DARK_GRAY)
                .stroke(DocumentStroke.of(DocumentColor.BLACK, 1))
                .build());
        session.add(new LineBuilder().name("Rule").horizontal(320)
                .color(DocumentColor.GRAY)
                .thickness(3)
                .build());
        session.add(new LineBuilder().name("Descending").diagonal(140, 50)
                .color(DocumentColor.BLACK)
                .thickness(2)
                .build());
        session.add(new LineBuilder().name("Ascending").size(140, 50).from(0, 50).to(140, 0)
                .color(DocumentColor.ROYAL_BLUE)
                .thickness(2)
                .build());
        return session;
    }

    private static BufferedImage rasterize(XSLFSlide slide, int widthPt, int heightPt) {
        BufferedImage image = new BufferedImage(
                (int) Math.round(widthPt * SCALE),
                (int) Math.round(heightPt * SCALE),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.scale(SCALE, SCALE);
            slide.draw(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
