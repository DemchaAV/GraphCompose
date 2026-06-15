package com.demcha.compose;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.style.DocumentStroke;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.util.List;

/**
 * Deterministic content-stream operator probe for the v1.8 vector-paint render
 * paths (S5/S6): the same {@code N} curved blob paths rendered three ways —
 * flat solid fill, linear gradient, and translucent (alpha) fill — so the
 * operator deltas isolate exactly what each paint mode costs at the PDF level.
 *
 * <p>A flat path takes the fast {@code fillAndStrokePath} route (just curve +
 * fill operators). A gradient fill clips to the path and paints a shading
 * ({@code q} / {@code W n} clip / {@code sh} / {@code Q} per shape); a
 * translucent fill sets an ExtGState alpha ({@code gs}). Counting {@code sh} /
 * {@code gs} / {@code W} against the flat baseline proves the per-shape cost
 * structure and catches a regression where a flat path accidentally takes the
 * heavier gradient branch. Byte-deterministic — no A/B build needed.</p>
 *
 * @author Artem Demchyshyn
 */
public final class VectorRenderOperatorProbe {

    static final int PATHS = 40;

    enum PaintMode { FLAT, GRADIENT, ALPHA, STROKED, DASHED }

    /**
     * PDF operator counts for one paint mode: cubic curves ({@code c}), shadings
     * ({@code sh}), ExtGState sets ({@code gs}), clips ({@code W}), strokes
     * ({@code S}/{@code s}) and dash-array sets ({@code d}).
     */
    record OperatorCounts(int curves, int shadings, int extGStates, int clips, int strokes, int dashes) {
    }

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();

        System.out.println("GraphCompose vector-paint render-operator probe (" + PATHS + " blob paths each)");
        System.out.printf("%-10s | %6s | %6s | %6s | %6s | %6s | %6s%n", "Mode", "c", "sh", "gs", "W", "S", "d");
        System.out.println("-".repeat(64));
        for (PaintMode mode : PaintMode.values()) {
            report(mode);
        }
        System.out.println();
        System.out.println("c=cubic curve, sh=shading fill, gs=ExtGState (alpha), W=clip, S=stroke, d=dash set. "
                + "Flat takes the fast fill path (no sh/gs/W/S/d); gradient adds sh+W per shape; alpha adds gs; "
                + "stroked adds S per shape; dashed adds d+S per shape.");
    }

    private static void report(PaintMode mode) throws Exception {
        OperatorCounts counts = countOperators(mode);
        System.out.printf("%-10s | %6d | %6d | %6d | %6d | %6d | %6d%n",
                mode.name().toLowerCase(),
                counts.curves(),
                counts.shadings(),
                counts.extGStates(),
                counts.clips(),
                counts.strokes(),
                counts.dashes());
    }

    /**
     * Renders {@link #PATHS} blob paths in the given paint mode and counts the PDF
     * operators. Exposed (package-visible) so {@code VectorRenderOperatorGateTest}
     * can pin the per-mode cost structure: flat takes the fast fill path (no
     * shading / alpha / clip), gradient adds a shading + clip per shape, alpha
     * adds an ExtGState per shape — and a flat path must never take the heavier
     * gradient branch.
     *
     * @param mode the paint mode to exercise
     * @return the operator counts of the rendered document
     * @throws Exception if rendering fails
     */
    static OperatorCounts countOperators(PaintMode mode) throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4).margin(28, 28, 28, 28).create()) {
            session.pageFlow(flow -> authorBlobs(flow, mode));
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new OperatorCounts(
                    count(document, "c"),
                    count(document, "sh"),
                    count(document, "gs"),
                    count(document, "W"),
                    count(document, "S") + count(document, "s"),
                    count(document, "d"));
        }
    }

    private static void authorBlobs(PageFlowBuilder flow, PaintMode mode) {
        DocumentPaint gradient = DocumentPaint.linear(
                DocumentColor.rgb(167, 139, 250), DocumentColor.rgb(97, 40, 217));
        DocumentColor flat = DocumentColor.rgb(40, 90, 160);
        DocumentColor translucent = DocumentColor.rgb(40, 90, 160).withOpacity(0.5);
        DocumentStroke stroke = DocumentStroke.of(DocumentColor.rgb(40, 90, 160), 2.0);
        for (int i = 0; i < PATHS; i++) {
            flow.addPath(p -> {
                p.size(60, 36)
                        .moveTo(0.0, 0.5)
                        .curveTo(0.25, 1.0, 0.75, 1.0, 1.0, 0.5)
                        .curveTo(0.75, 0.0, 0.25, 0.0, 0.0, 0.5)
                        .closePath();
                switch (mode) {
                    case FLAT -> p.fillColor(flat);
                    case GRADIENT -> p.fill(gradient);
                    case ALPHA -> p.fillColor(translucent);
                    case STROKED -> p.stroke(stroke);
                    case DASHED -> p.stroke(stroke).dashed(4.0, 2.0);
                }
            });
        }
    }

    private static int count(PDDocument document, String op) throws IOException {
        int n = 0;
        for (var page : document.getPages()) {
            List<Object> tokens = new PDFStreamParser(page).parse();
            for (Object token : tokens) {
                if (token instanceof Operator operator && op.equals(operator.getName())) {
                    n++;
                }
            }
        }
        return n;
    }
}
