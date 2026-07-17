package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontFamilyDefinition;
import com.demcha.compose.font.FontName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code toImages} must rasterize embedded binary fonts with their real glyph
 * programs. PDFBox writes a {@code PDType0Font} subset only during
 * {@code save()}, so rendering the unsaved in-memory document falls back to
 * substitute glyphs — visually garbled text with correct advances. The
 * backend therefore saves and reloads before rasterizing; this test pins the
 * contract by comparing {@code toImages} pixels against an independent
 * save-reload-render of the same session.
 */
class PdfRenderToImagesEmbeddedFontTest {

    private static final FontName FONT = FontName.of("RasterLato");
    private static final FontFamilyDefinition FAMILY = FontFamilyDefinition.classpath(
                    FONT, "fonts/google/lato/Lato-Regular.ttf")
            .build();

    @Test
    void toImagesMatchesTheSaveReloadRenderOfTheSameSession() throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .registerFontFamily(FAMILY)
                .pageSize(400, 120)
                .margin(DocumentInsets.of(20))
                .create()) {
            session.pageFlow().name("Raster")
                    .addParagraph(p -> p.text("The quick brown fox jumps over the lazy dog")
                            .textStyle(DocumentTextStyle.builder().fontName(FONT).size(14).build()))
                    .build();

            List<BufferedImage> images = session.toImages(144);
            assertThat(images).hasSize(1);
            BufferedImage viaToImages = images.get(0);

            BufferedImage viaSaveReload;
            try (PDDocument document = Loader.loadPDF(session.toPdfBytes())) {
                viaSaveReload = new PDFRenderer(document)
                        .renderImageWithDPI(0, 144, ImageType.RGB);
            }

            assertThat(viaToImages.getWidth()).isEqualTo(viaSaveReload.getWidth());
            assertThat(viaToImages.getHeight()).isEqualTo(viaSaveReload.getHeight());
            int mismatched = 0;
            for (int y = 0; y < viaSaveReload.getHeight(); y++) {
                for (int x = 0; x < viaSaveReload.getWidth(); x++) {
                    if (viaToImages.getRGB(x, y) != viaSaveReload.getRGB(x, y)) {
                        mismatched++;
                    }
                }
            }
            assertThat(mismatched)
                    .as("toImages raster must match the save-reload raster pixel for pixel")
                    .isZero();
        }
    }
}
