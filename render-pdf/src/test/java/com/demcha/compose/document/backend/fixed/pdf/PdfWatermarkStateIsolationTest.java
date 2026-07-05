package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.output.DocumentWatermark;
import com.demcha.compose.document.output.DocumentWatermarkLayer;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A {@code BEHIND_CONTENT} watermark renders through a PREPEND content
 * stream, and PDFBox's {@code resetContext} flag only isolates APPEND
 * streams — so the watermark must save/restore the graphics state itself.
 * Without that {@code q}/{@code Q} pair the watermark's low alpha constant
 * bled into the original page stream and washed out the entire page.
 */
class PdfWatermarkStateIsolationTest {

    private static final DocumentColor NAVY = DocumentColor.rgb(20, 40, 90);

    @TempDir
    Path tempDir;

    @Test
    void behindContentWatermarkOpacityDoesNotBleedIntoPageContent() throws Exception {
        Path out = tempDir.resolve("watermark-isolation.pdf");
        try (DocumentSession document = GraphCompose.document(out)
                .pageSize(200, 150)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.watermark(DocumentWatermark.builder()
                    .text("WM")
                    .opacity(0.05f)
                    .layer(DocumentWatermarkLayer.BEHIND_CONTENT)
                    .build());
            document.pageFlow().name("Flow")
                    .addShape(100, 50, NAVY)
                    .build();
            document.buildPdf();
        }

        try (PDDocument doc = Loader.loadPDF(out.toFile())) {
            BufferedImage image = new PDFRenderer(doc).renderImageWithDPI(0, 96);
            // Centre of the 100x50 shape placed at the top-left margin.
            float scale = 96f / 72f;
            int x = Math.round((20 + 50) * scale);
            int y = Math.round((20 + 25) * scale);
            Color sampled = new Color(image.getRGB(x, y));

            // With the alpha leak the navy fill blends 5% over white and
            // samples near (243, 244, 247); the fix keeps it solid navy.
            assertThat(sampled.getRed()).as("red at shape centre").isCloseTo(20, within(30));
            assertThat(sampled.getGreen()).as("green at shape centre").isCloseTo(40, within(30));
            assertThat(sampled.getBlue()).as("blue at shape centre").isCloseTo(90, within(30));

            // The watermark itself must still carry its low-alpha state.
            List<PDExtendedGraphicsState> states = extGStates(doc);
            assertThat(states)
                    .as("watermark extended graphics state")
                    .anySatisfy(state -> assertThat(state.getNonStrokingAlphaConstant())
                            .isCloseTo(0.05f, within(0.005f)));
        }
    }

    private static List<PDExtendedGraphicsState> extGStates(PDDocument doc) throws Exception {
        PDResources resources = doc.getPage(0).getResources();
        List<PDExtendedGraphicsState> states = new ArrayList<>();
        for (COSName name : resources.getExtGStateNames()) {
            states.add(resources.getExtGState(name));
        }
        return states;
    }
}
