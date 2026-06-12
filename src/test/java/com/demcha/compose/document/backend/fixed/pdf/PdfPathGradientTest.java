package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PathBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.svg.SvgIcon;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDShadingPattern;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that path gradients reach the PDF natively: gradient fills as
 * shading resources (clipped {@code sh}), gradient strokes as shading
 * patterns in the stroking colour space — and that flat-colour paths emit
 * neither, keeping pre-gradient output byte-identical.
 */
class PdfPathGradientTest {

    private static final DocumentColor VIOLET = DocumentColor.rgb(167, 139, 250);
    private static final DocumentColor DEEP = DocumentColor.rgb(97, 40, 217);
    private static final DocumentPaint AXIS = new DocumentPaint.LinearAxis(List.of(
            new DocumentPaint.Stop(0.0, VIOLET),
            new DocumentPaint.Stop(1.0, DEEP)), 0.0, 0.0, 1.0, 1.0);

    /** The brand-mark essence: gradient stroke plus a gradient-filled dot. */
    private static final String MINI_MARK_SVG = """
            <svg viewBox="0 0 96 96">
              <defs>
                <linearGradient id="g" gradientUnits="userSpaceOnUse" x1="22" y1="24" x2="74" y2="72">
                  <stop offset="0" stop-color="#A78BFA"/>
                  <stop offset="1" stop-color="#6128D9"/>
                </linearGradient>
              </defs>
              <path d="M68 24 L34 24 L22 36 L22 60" fill="none" stroke="url(#g)" stroke-width="7"/>
              <circle cx="68" cy="24" r="7.2" fill="url(#g)"/>
            </svg>
            """;

    @TempDir
    Path tempDir;

    private Path render(String name, Consumer<PathBuilder> spec) throws Exception {
        Path out = tempDir.resolve(name + ".pdf");
        try (DocumentSession document = GraphCompose.document(out)
                .pageSize(220, 160)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").addPath(spec).build();
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

    private static List<PDAbstractPattern> patterns(Path pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDResources resources = doc.getPage(0).getResources();
            List<PDAbstractPattern> result = new ArrayList<>();
            for (COSName name : resources.getPatternNames()) {
                result.add(resources.getPattern(name));
            }
            return result;
        }
    }

    @Test
    void gradientFillClipsToThePathAndEmitsAShading() throws Exception {
        Path pdf = render("fill", p -> p.size(120, 60)
                .moveTo(0, 0).lineTo(1, 0).lineTo(0.5, 1).closePath()
                .fill(AXIS));

        List<PDShading> shadings = shadings(pdf);
        assertThat(shadings).hasSize(1);
        assertThat(shadings.get(0).getShadingType()).isEqualTo(PDShading.SHADING_TYPE2);
        assertThat(patterns(pdf)).isEmpty();
    }

    @Test
    void gradientStrokeEmitsAShadingPatternColour() throws Exception {
        Path pdf = render("stroke", p -> p.size(120, 60)
                .moveTo(0, 0.5).curveTo(0.25, 1, 0.75, 0, 1, 0.5)
                .stroke(DocumentStroke.of(VIOLET, 3))
                .strokePaint(AXIS));

        List<PDAbstractPattern> patterns = patterns(pdf);
        assertThat(patterns).hasSize(1);
        assertThat(patterns.get(0)).isInstanceOf(PDShadingPattern.class);
        PDShading patternShading = ((PDShadingPattern) patterns.get(0)).getShading();
        assertThat(patternShading.getShadingType()).isEqualTo(PDShading.SHADING_TYPE2);
        // The colour function must survive serialization too — a dangling
        // /Function reference renders as no gradient at all.
        assertThat(patternShading.getFunction()).isNotNull();
        // No fill gradient → no page-level shading resource.
        assertThat(shadings(pdf)).isEmpty();
    }

    @Test
    void solidPaintsNormaliseAwayAndEmitNoGradientResources() throws Exception {
        Path pdf = render("solid", p -> p.size(120, 60)
                .moveTo(0, 0).lineTo(1, 0).lineTo(0.5, 1).closePath()
                .fill(DocumentPaint.solid(VIOLET))
                .stroke(DocumentStroke.of(DEEP, 2))
                .strokePaint(DocumentPaint.solid(DEEP)));

        assertThat(shadings(pdf)).isEmpty();
        assertThat(patterns(pdf)).isEmpty();
    }

    @Test
    void svgIconWithGradientFillAndStrokeRendersEndToEnd() throws Exception {
        SvgIcon icon = SvgIcon.parse(MINI_MARK_SVG);
        Path out = tempDir.resolve("mark.pdf");
        try (DocumentSession document = GraphCompose.document(out)
                .pageSize(220, 160)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow").addSvgIcon(icon, 96).build();
            document.buildPdf();
        }

        // The stroked G-outline arrives as a pattern, the filled dot as a shading.
        assertThat(patterns(out)).hasSize(1);
        assertThat(shadings(out)).hasSize(1);
    }
}
