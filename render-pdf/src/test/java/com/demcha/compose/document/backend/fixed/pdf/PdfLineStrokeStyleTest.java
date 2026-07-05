package com.demcha.compose.document.backend.fixed.pdf;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentLineCap;
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
 * Verifies that {@link LineBuilder#lineCap(DocumentLineCap)} reaches the PDF as
 * the {@code J} (line-cap) operator, that the {@code BUTT} default emits none
 * (byte-identical with the pre-cap backend), and that a round cap composes with
 * a dash to draw a dotted line.
 */
class PdfLineStrokeStyleTest {

    private static final DocumentColor INK = DocumentColor.rgb(20, 60, 120);

    @TempDir
    Path tempDir;

    private Path render(String name, Consumer<LineBuilder> spec) throws Exception {
        Path out = tempDir.resolve(name + ".pdf");
        try (DocumentSession document = GraphCompose.document(out)
                .pageSize(160, 100)
                .margin(DocumentInsets.of(16))
                .create()) {
            document.pageFlow().name("Flow").addLine(spec).build();
            document.buildPdf();
        }
        return out;
    }

    /** Collects every {@code (operandInt, operator)} pair in the page stream. */
    private static List<Integer> operatorInts(Path pdf, String operatorName) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDPage page = doc.getPage(0);
            PDFStreamParser parser = new PDFStreamParser(page);
            List<Integer> hits = new ArrayList<>();
            List<COSBase> operands = new ArrayList<>();
            for (Object token = parser.parseNextToken(); token != null; token = parser.parseNextToken()) {
                if (token instanceof COSBase base) {
                    operands.add(base);
                } else if (token instanceof Operator op) {
                    if (op.getName().equals(operatorName) && !operands.isEmpty()
                        && operands.get(operands.size() - 1) instanceof COSNumber n) {
                        hits.add(n.intValue());
                    }
                    operands.clear();
                }
            }
            return hits;
        }
    }

    @Test
    void roundCapEmitsJ1() throws Exception {
        Path pdf = render("round", l -> l.horizontal(120)
                .stroke(DocumentStroke.of(INK, 6))
                .lineCap(DocumentLineCap.ROUND));

        assertThat(operatorInts(pdf, "J")).contains(1);
    }

    @Test
    void squareCapEmitsJ2() throws Exception {
        Path pdf = render("square", l -> l.horizontal(120)
                .stroke(DocumentStroke.of(INK, 6))
                .lineCap(DocumentLineCap.SQUARE));

        assertThat(operatorInts(pdf, "J")).contains(2);
    }

    @Test
    void defaultButtEmitsNoCapOperator() throws Exception {
        Path pdf = render("default", l -> l.horizontal(120)
                .stroke(DocumentStroke.of(INK, 6)));

        assertThat(operatorInts(pdf, "J")).isEmpty();
    }

    @Test
    void roundCapWithDashDrawsDottedLine() throws Exception {
        Path pdf = render("dotted", l -> l.horizontal(120)
                .stroke(DocumentStroke.of(INK, 6))
                .dashed(0.1, 4)
                .lineCap(DocumentLineCap.ROUND));

        // ROUND cap on a near-zero on-dash = round dots; the cap operator must be present.
        assertThat(operatorInts(pdf, "J")).contains(1);
    }
}
