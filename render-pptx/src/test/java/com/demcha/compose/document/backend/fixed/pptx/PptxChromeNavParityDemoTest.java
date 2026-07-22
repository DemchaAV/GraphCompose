package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.PdfFixedLayoutBackend;
import com.demcha.compose.document.backend.fixed.pdf.PdfOutputOptionsTranslator;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.node.DocumentBookmarkOptions;
import com.demcha.compose.document.node.PageBreakNode;
import com.demcha.compose.document.output.DocumentHeaderFooter;
import com.demcha.compose.document.output.DocumentMetadata;
import com.demcha.compose.document.output.DocumentWatermark;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reviewable chrome-and-navigation parity pair: the same two-page document
 * with metadata, a DRAFT watermark, tokenized header/footer, an internal
 * table-of-contents link, and a bookmark rendered to PDF and PPTX with
 * per-page PNG previews at matching scale.
 */
class PptxChromeNavParityDemoTest {

    private static final double SCALE = 2.0;

    @Test
    void writesTheChromeNavDemoPair() throws Exception {
        Path output = Path.of("target", "visual-tests", "pptx-parity", "chrome-nav");
        Files.createDirectories(output);

        DocumentMetadata metadata = DocumentMetadata.builder()
                .title("Engine Chrome Demo")
                .author("DemchaAV")
                .subject("Chrome and navigation parity")
                .keywords("graph-compose, pptx, chrome")
                .build();
        DocumentWatermark watermark = DocumentWatermark.builder()
                .text("DRAFT")
                .build();
        DocumentHeaderFooter header = DocumentHeaderFooter.builder()
                .leftText("Engine Report")
                .rightText("Page {page} of {pages}")
                .showSeparator(true)
                .build();
        DocumentHeaderFooter footer = DocumentHeaderFooter.builder()
                .centerText("graph-compose · chrome & navigation")
                .build();

        try (DocumentSession session = composeDemo()) {
            byte[] pdf = session.render(PdfFixedLayoutBackend.builder()
                    .metadata(PdfOutputOptionsTranslator.toPdf(metadata))
                    .watermark(PdfOutputOptionsTranslator.toPdf(watermark))
                    .header(PdfOutputOptionsTranslator.toPdf(header))
                    .footer(PdfOutputOptionsTranslator.toPdf(footer))
                    .build());
            Files.write(output.resolve("chrome-nav.pdf"), pdf);

            byte[] pptx = session.render(PptxFixedLayoutBackend.builder()
                    .metadata(metadata)
                    .watermark(watermark)
                    .header(header)
                    .footer(footer)
                    .build());
            Files.write(output.resolve("chrome-nav.pptx"), pptx);

            try (PDDocument document = Loader.loadPDF(pdf);
                 XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getSlides()).hasSize(document.getNumberOfPages());
                PDFRenderer renderer = new PDFRenderer(document);
                for (int page = 0; page < document.getNumberOfPages(); page++) {
                    ImageIO.write(renderer.renderImageWithDPI(page, (float) (72 * SCALE)),
                            "png", output.resolve("chrome-nav-page-" + (page + 1) + ".pdf.png").toFile());
                    ImageIO.write(rasterize(show.getSlides().get(page),
                                    show.getPageSize().width, show.getPageSize().height),
                            "png", output.resolve("chrome-nav-page-" + (page + 1) + ".pptx.png").toFile());
                }
            }
        }
    }

    private static DocumentSession composeDemo() {
        DocumentSession session = GraphCompose.document()
                .pageSize(480, 320)
                .margin(DocumentInsets.of(36))
                .create();
        session.add(new ParagraphBuilder().name("Title")
                .text("Engine Chrome Demo")
                .textStyle(DocumentTextStyle.builder().size(20)
                        .decoration(DocumentTextDecoration.BOLD).build())
                .build());
        session.add(new ParagraphBuilder().name("Toc")
                .inlineText("Contents: ")
                .inlineLinkTo("Benchmark details »", "details")
                .build());
        session.add(session.dsl().shape().name("CoverCard").size(180, 50)
                .fillColor(DocumentColor.ROYAL_BLUE)
                .cornerRadius(8)
                .linkTo("details")
                .build());
        session.add(new PageBreakNode("Break", DocumentInsets.zero()));
        session.add(new ParagraphBuilder().name("DetailsTitle")
                .text("Benchmark details")
                .textStyle(DocumentTextStyle.builder().size(16)
                        .decoration(DocumentTextDecoration.BOLD).build())
                .anchor("details")
                .bookmark(new DocumentBookmarkOptions("Benchmark details"))
                .build());
        session.add(new ParagraphBuilder().name("DetailsBody")
                .text("This page is the target of the cover's internal links; "
                        + "the slide carries the bookmark's name.")
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
