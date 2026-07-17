package com.demcha.compose.document.backend.fixed.pptx;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.table.DocumentTableCell;
import com.demcha.compose.document.table.DocumentTableStyle;
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
 * Produces the reviewable PDF/PPTX table parity pair: a styled multi-page
 * table with a repeated header, zebra rows, and a row-spanning cell, plus
 * per-page PNG previews from both formats at matching scale.
 */
class PptxTableParityDemoTest {

    private static final double SCALE = 2.0;

    @Test
    void writesTheTableParityDemoPair() throws Exception {
        Path output = Path.of("target", "visual-tests", "pptx-parity", "tables");
        Files.createDirectories(output);

        try (DocumentSession session = composeDemo()) {
            byte[] pdf = session.toPdfBytes();
            Files.write(output.resolve("tables.pdf"), pdf);
            byte[] pptx = session.render(new PptxFixedLayoutBackend());
            Files.write(output.resolve("tables.pptx"), pptx);

            List<BufferedImage> pdfPages = session.toImages((int) Math.round(72 * SCALE));
            try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(pptx))) {
                assertThat(show.getSlides()).hasSameSizeAs(pdfPages);
                for (int i = 0; i < pdfPages.size(); i++) {
                    ImageIO.write(pdfPages.get(i), "png",
                            output.resolve("tables-page-" + (i + 1) + ".pdf.png").toFile());
                    ImageIO.write(rasterize(show.getSlides().get(i),
                                    show.getPageSize().width, show.getPageSize().height),
                            "png", output.resolve("tables-page-" + (i + 1) + ".pptx.png").toFile());
                }
            }
        }
    }

    private static DocumentSession composeDemo() {
        DocumentSession session = GraphCompose.document()
                .pageSize(460, 280)
                .margin(DocumentInsets.of(24))
                .create();
        DocumentTableStyle body = DocumentTableStyle.builder()
                .stroke(DocumentStroke.of(DocumentColor.rgb(203, 213, 225), 1))
                .padding(4)
                .textStyle(DocumentTextStyle.builder().size(10).build())
                .build();
        session.pageFlow(page -> page.module("engines", module -> module.table(table -> {
            table.autoColumns(3)
                    .headerRow("Engine", "Output", "Geometry")
                    .repeatHeader()
                    .headerStyle(DocumentTableStyle.builder()
                            .fillColor(DocumentColor.rgb(30, 64, 175))
                            .textStyle(DocumentTextStyle.builder()
                                    .color(DocumentColor.WHITE).size(10).build())
                            .stroke(DocumentStroke.of(DocumentColor.rgb(30, 64, 175), 1))
                            .padding(4)
                            .build())
                    .defaultCellStyle(body)
                    .zebra(DocumentColor.WHITE, DocumentColor.rgb(241, 245, 249));
            table.rowCells(
                    new DocumentTableCell(List.of("Fixed layout"), body).rowSpan(2),
                    new DocumentTableCell(List.of("PDF"), body),
                    new DocumentTableCell(List.of("exact"), body));
            table.rowCells(
                    new DocumentTableCell(List.of("PPTX"), body),
                    new DocumentTableCell(List.of("exact"), body));
            for (int row = 1; row <= 14; row++) {
                table.row("run " + row, "page " + (row % 3 + 1), "cell " + row);
            }
        })));
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
