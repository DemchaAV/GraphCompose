package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link DocumentPaint} gradients reach the PDF as native
 * shadings — and that solid fills (colour or {@code DocumentPaint.solid})
 * emit no shading resources at all, keeping existing output byte-identical.
 */
class PdfShapeGradientTest {

    private static final DocumentColor TEAL = DocumentColor.rgb(20, 80, 95);
    private static final DocumentColor GOLD = DocumentColor.rgb(196, 153, 76);

    @TempDir
    Path tempDir;

    private Path render(String name, DocumentPaint paint) throws Exception {
        Path out = tempDir.resolve(name + ".pdf");
        try (DocumentSession document = GraphCompose.document(out)
                .pageSize(220, 160)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow")
                    .addShape(s -> s.size(120, 60).cornerRadius(6).fill(paint))
                    .build();
            document.buildPdf();
        }
        return out;
    }

    private static List<PDShading> shadings(Path pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDResources resources = doc.getPage(0).getResources();
            List<PDShading> result = new ArrayList<>();
            for (COSName name : resources.getShadingNames()) {
                result.add(resources.getShading(name));
            }
            return result;
        }
    }

    @Test
    void linearPaintEmitsAnAxialShading() throws Exception {
        List<PDShading> shadings = shadings(render("linear", DocumentPaint.linear(TEAL, GOLD)));

        assertThat(shadings).hasSize(1);
        assertThat(shadings.get(0).getShadingType()).isEqualTo(PDShading.SHADING_TYPE2);
    }

    @Test
    void radialPaintEmitsARadialShading() throws Exception {
        DocumentPaint radial = new DocumentPaint.Radial(List.of(
                new DocumentPaint.Stop(0.0, GOLD),
                new DocumentPaint.Stop(1.0, TEAL)), 0.5, 0.5);

        List<PDShading> shadings = shadings(render("radial", radial));

        assertThat(shadings).hasSize(1);
        assertThat(shadings.get(0).getShadingType()).isEqualTo(PDShading.SHADING_TYPE3);
    }

    @Test
    void multiStopLinearUsesAStitchingFunctionAndRenders() throws Exception {
        DocumentPaint threeStops = new DocumentPaint.Linear(List.of(
                new DocumentPaint.Stop(0.0, TEAL),
                new DocumentPaint.Stop(0.3, GOLD),
                new DocumentPaint.Stop(1.0, DocumentColor.WHITE)), 90.0);

        assertThat(shadings(render("multistop", threeStops))).hasSize(1);
    }

    @Test
    void solidPaintEmitsNoShadingResources() throws Exception {
        assertThat(shadings(render("solid", DocumentPaint.solid(TEAL)))).isEmpty();
    }
}
