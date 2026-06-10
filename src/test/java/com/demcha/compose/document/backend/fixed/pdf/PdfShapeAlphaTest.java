package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Verifies that the alpha channel of {@link DocumentColor#rgba(int, int, int, int)}
 * reaches the PDF graphics state on shape fills — and, critically, that fully
 * opaque documents emit no extended graphics state at all, keeping existing
 * output byte-identical.
 */
class PdfShapeAlphaTest {

    @TempDir
    Path tempDir;

    private Path renderShape(DocumentColor fill) throws Exception {
        Path out = tempDir.resolve("alpha-" + fill.color().getAlpha() + ".pdf");
        try (DocumentSession document = GraphCompose.document(out)
                .pageSize(200, 150)
                .margin(DocumentInsets.of(20))
                .create()) {
            document.pageFlow().name("Flow")
                    .addShape(100, 50, fill)
                    .build();
            document.buildPdf();
        }
        return out;
    }

    private static List<PDExtendedGraphicsState> extGStates(Path pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDResources resources = doc.getPage(0).getResources();
            List<PDExtendedGraphicsState> states = new ArrayList<>();
            for (COSName name : resources.getExtGStateNames()) {
                states.add(resources.getExtGState(name));
            }
            return states;
        }
    }

    @Test
    void translucentFillEmitsGraphicsStateAlpha() throws Exception {
        Path pdf = renderShape(DocumentColor.rgba(20, 80, 95, 128));

        List<PDExtendedGraphicsState> states = extGStates(pdf);
        assertThat(states).isNotEmpty();
        assertThat(states.get(0).getNonStrokingAlphaConstant())
                .isCloseTo(128f / 255f, within(0.005f));
    }

    @Test
    void opaqueFillEmitsNoGraphicsState() throws Exception {
        Path pdf = renderShape(DocumentColor.rgb(20, 80, 95));

        assertThat(extGStates(pdf)).isEmpty();
    }
}
