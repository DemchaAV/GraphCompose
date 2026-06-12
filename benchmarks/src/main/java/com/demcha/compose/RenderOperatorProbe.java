package com.demcha.compose;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Finding 5 probe — counts content-stream operators in rendered documents.
 *
 * <p>Before F5 the paragraph render handler wrote one {@code setFont} (Tf) and one
 * {@code setNonStrokingColor} (rg/g) per text span, i.e. exactly one of each per
 * text-show ({@code Tj}/{@code TJ}). So the pre-F5 Tf and colour counts equal the
 * text-draw count — that is the deterministic baseline this probe prints alongside
 * the post-F5 counts; no A/B build needed. Tables render through a different
 * handler that F5 does not touch, included to show the honest boundary.</p>
 */
public final class RenderOperatorProbe {

    private static final DocumentTextStyle BODY = DocumentTextStyle.builder()
            .size(10).decoration(DocumentTextDecoration.DEFAULT)
            .color(DocumentColor.of(new Color(40, 50, 60))).build();
    private static final DocumentTextStyle HEAD = DocumentTextStyle.builder()
            .size(16).decoration(DocumentTextDecoration.BOLD)
            .color(DocumentColor.of(new Color(18, 40, 74))).build();

    private static final String SENTENCE =
            "GraphCompose lays out structured business documents across many pages "
                    + "while keeping header and footer placement stable. ";

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();

        System.out.println("GraphCompose Finding-5 Render-Operator Probe");
        System.out.printf("%-22s | %8s | %8s | %8s | %12s | %9s%n",
                "Scenario", "Draws", "Tf(now)", "rg(now)", "Tf+rg saved", "Reduction");
        System.out.println("-".repeat(80));

        report("long-paragraph", flow -> flow.addParagraph(p -> p.text(SENTENCE.repeat(40)).textStyle(BODY)));
        report("20-paragraph body", flow -> {
            for (int i = 0; i < 20; i++) {
                flow.addParagraph(p -> p.text(SENTENCE.repeat(3)).textStyle(BODY));
            }
        });
        report("headed sections", flow -> {
            for (int i = 0; i < 8; i++) {
                flow.addParagraph(p -> p.text("Section heading").textStyle(HEAD));
                flow.addParagraph(p -> p.text(SENTENCE.repeat(4)).textStyle(BODY));
            }
        });
        report("50-row table (F5 N/A)", flow -> flow.addTable(t -> {
            t.autoColumns(4).header("Item", "Qty", "Unit", "Total");
            for (int r = 1; r <= 50; r++) {
                t.row("Line item " + r, "3", "ea", "38.75");
            }
        }));

        System.out.println();
        System.out.println("Pre-F5 Tf == rg == Draws (one font + one colour op per span). 'saved' = (Draws-Tf)+(Draws-rg).");
    }

    private static void report(String scenario, Consumer<com.demcha.compose.document.dsl.PageFlowBuilder> author) throws Exception {
        byte[] pdf;
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4).margin(28, 28, 28, 28).create()) {
            session.pageFlow(author);
            pdf = session.toPdfBytes();
        }
        try (PDDocument document = Loader.loadPDF(pdf)) {
            int draws = count(document, "Tj") + count(document, "TJ");
            int tf = count(document, "Tf");
            int rg = count(document, "rg") + count(document, "g") + count(document, "sc") + count(document, "scn");
            int saved = Math.max(0, draws - tf) + Math.max(0, draws - rg);
            double reduction = draws == 0 ? 0 : 100.0 * (2.0 * draws - tf - rg) / (2.0 * draws);
            System.out.printf("%-22s | %8d | %8d | %8d | %12d | %8.1f%%%n",
                    scenario, draws, tf, rg, saved, reduction);
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
