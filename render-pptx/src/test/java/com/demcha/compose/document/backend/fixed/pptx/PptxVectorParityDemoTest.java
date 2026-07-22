package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.BarcodeBuilder;
import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.image.DocumentImageFitMode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTransform;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reviewable vector parity pair: image fit modes, a ZXing QR code, and a
 * rotated, scaled shape-container badge rendered to PDF and PPTX with
 * per-page PNG previews at matching scale.
 */
class PptxVectorParityDemoTest {

    private static final double SCALE = 2.0;

    @Test
    void writesTheVectorParityDemoPair() throws Exception {
        Path output = Path.of("target", "visual-tests", "pptx-parity", "vector-fragments");
        Files.createDirectories(output);

        try (DocumentSession session = composeDemo()) {
            byte[] pdf = session.toPdfBytes();
            Files.write(output.resolve("vectors.pdf"), pdf);
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            Files.write(output.resolve("vectors.pptx"), pptx);

            List<BufferedImage> pdfPages = session.toImages((int) Math.round(72 * SCALE));
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getSlides()).hasSameSizeAs(pdfPages);
                for (int i = 0; i < pdfPages.size(); i++) {
                    ImageIO.write(pdfPages.get(i), "png",
                            output.resolve("vectors-page-" + (i + 1) + ".pdf.png").toFile());
                    ImageIO.write(rasterize(show.getSlides().get(i),
                                    show.getPageSize().width, show.getPageSize().height),
                            "png", output.resolve("vectors-page-" + (i + 1) + ".pptx.png").toFile());
                }
            }
        }
    }

    private static DocumentSession composeDemo() throws Exception {
        byte[] photo = gradientPhoto();
        DocumentSession session = GraphCompose.document()
                .pageSize(480, 320)
                .margin(DocumentInsets.of(24))
                .create();
        session.add(new ImageBuilder().source(photo).size(120, 60)
                .fitMode(DocumentImageFitMode.STRETCH).build());
        session.add(new ImageBuilder().source(photo).size(120, 60)
                .fitMode(DocumentImageFitMode.COVER).build());
        session.add(new BarcodeBuilder().data("https://github.com/DemchaAV/GraphCompose")
                .qrCode().size(70, 70).build());
        session.add(new ShapeContainerBuilder()
                .ellipse(70, 70)
                .layer(new EllipseBuilder().circle(36)
                        .fillColor(DocumentColor.ROYAL_BLUE).build())
                .transform(new DocumentTransform(25, 1.3, 1.3))
                .build());
        return session;
    }

    private static byte[] gradientPhoto() throws Exception {
        BufferedImage image = new BufferedImage(160, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setPaint(new GradientPaint(0, 0, new Color(30, 64, 175),
                    160, 40, new Color(249, 115, 22)));
            graphics.fillRect(0, 0, 160, 40);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
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
