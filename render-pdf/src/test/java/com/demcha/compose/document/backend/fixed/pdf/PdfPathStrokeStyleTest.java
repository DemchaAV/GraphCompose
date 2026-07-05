package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PathBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLineCap;
import com.demcha.compose.document.style.DocumentLineJoin;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that path stroke cap / join styles reach the PDF as the
 * {@code J} / {@code j} operators, and that the defaults emit neither —
 * keeping default-styled output byte-identical with the pre-style backend.
 */
class PdfPathStrokeStyleTest {

    private static final DocumentColor INK = DocumentColor.rgb(20, 60, 120);

    @TempDir
    Path tempDir;

    private Path render(String name, Consumer<PathBuilder> spec) throws Exception {
        Path out = tempDir.resolve(name + ".pdf");
        try (DocumentSession document = GraphCompose.document(out)
                .pageSize(160, 100)
                .margin(DocumentInsets.of(16))
                .create()) {
            document.pageFlow().name("Flow").addPath(spec).build();
            document.buildPdf();
        }
        return out;
    }

    /** Collects every {@code (operandInt, operator)} pair in the page stream. */
    private static List<int[]> operatorInts(Path pdf, String operatorName) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDPage page = doc.getPage(0);
            PDFStreamParser parser = new PDFStreamParser(page);
            List<int[]> hits = new ArrayList<>();
            List<COSBase> operands = new ArrayList<>();
            for (Object token = parser.parseNextToken(); token != null; token = parser.parseNextToken()) {
                if (token instanceof COSBase base) {
                    operands.add(base);
                } else if (token instanceof Operator op) {
                    if (op.getName().equals(operatorName) && !operands.isEmpty()
                        && operands.get(operands.size() - 1) instanceof COSNumber n) {
                        hits.add(new int[]{n.intValue()});
                    }
                    operands.clear();
                }
            }
            return hits;
        }
    }

    @Test
    void roundCapAndJoinEmitTheCorrectOperators() throws Exception {
        Path pdf = render("round", p -> p.size(120, 40)
                .moveTo(0.0, 0.5).lineTo(0.5, 1.0).lineTo(1.0, 0.5)
                .stroke(DocumentStroke.of(INK, 6))
                .lineCap(DocumentLineCap.ROUND)
                .lineJoin(DocumentLineJoin.ROUND));

        assertThat(operatorInts(pdf, "J")).contains(new int[]{1});
        assertThat(operatorInts(pdf, "j")).contains(new int[]{1});
    }

    @Test
    void squareCapAndBevelJoinEmitTheCorrectOperators() throws Exception {
        Path pdf = render("square", p -> p.size(120, 40)
                .moveTo(0.0, 0.5).lineTo(0.5, 1.0).lineTo(1.0, 0.5)
                .stroke(DocumentStroke.of(INK, 6))
                .lineCap(DocumentLineCap.SQUARE)
                .lineJoin(DocumentLineJoin.BEVEL));

        assertThat(operatorInts(pdf, "J")).contains(new int[]{2});
        assertThat(operatorInts(pdf, "j")).contains(new int[]{2});
    }

    @Test
    void defaultButtMiterEmitNoStyleOperators() throws Exception {
        Path pdf = render("default", p -> p.size(120, 40)
                .moveTo(0.0, 0.5).lineTo(0.5, 1.0).lineTo(1.0, 0.5)
                .stroke(DocumentStroke.of(INK, 6)));

        assertThat(operatorInts(pdf, "J")).isEmpty();
        assertThat(operatorInts(pdf, "j")).isEmpty();
    }
}
