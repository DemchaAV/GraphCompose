package com.demcha.compose.jmh;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.semantic.docx.DocxSemanticBackend;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.style.DocumentInsets;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * What a DOCX export costs, next to the PDF render of the same document.
 *
 * <p>Four shapes of document, each exported through the session the way a caller does — so the
 * measurement includes building the document and laying it out, which the DOCX export asks for:
 * one small page, a report of about twenty pages, a thousand-row table, and twenty repetitions of
 * one image. The PDF render of each is measured beside it, so the numbers read as a ratio to a
 * cost that is already understood rather than as absolutes on one machine.</p>
 *
 * <p>Run with allocation profiling for the memory half:
 * {@code java -cp <benchmark classpath> org.openjdk.jmh.Main DocxExportJmhBenchmark -prof gc}.</p>
 *
 * @author Artem Demchyshyn
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class DocxExportJmhBenchmark {

    @Param({"small", "report20", "table1000", "images20"})
    public String shape;

    private byte[] image;

    @Setup
    public void prepareImage() throws IOException {
        BufferedImage picture = new BufferedImage(400, 225, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = picture.createGraphics();
        graphics.setColor(new Color(40, 90, 150));
        graphics.fillRect(0, 0, 400, 225);
        graphics.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(picture, "png", png);
        image = png.toByteArray();
    }

    @Benchmark
    public void docx(Blackhole blackhole) throws Exception {
        try (DocumentSession session = session()) {
            blackhole.consume(session.export(new DocxSemanticBackend()));
        }
    }

    @Benchmark
    public void pdf(Blackhole blackhole) throws Exception {
        try (DocumentSession session = session()) {
            blackhole.consume(session.toPdfBytes());
        }
    }

    private DocumentSession session() {
        DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(36))
                .create();
        session.pageFlow(content()::accept);
        return session;
    }

    private Consumer<PageFlowBuilder> content() {
        return switch (shape) {
            case "small" -> flow -> flow
                    .addParagraph("Invoice INV-2026-0147")
                    .addParagraph("Billed to Northwind Traders, 12 Harbour Road.")
                    .addTable(t -> t.autoColumns(3)
                            .header("Item", "Qty", "Amount")
                            .row("Platform subscription", "12", "1 440.00")
                            .row("Priority support", "12", "720.00")
                            .totalRow("Total", "", "2 160.00"))
                    .addParagraph("Payment is due within thirty days of the invoice date.");
            case "report20" -> flow -> {
                for (int section = 0; section < 60; section++) {
                    final int index = section;
                    flow.addSection("Section" + index, block -> block
                            .addParagraph("Section " + index)
                            .addParagraph("Body paragraph one for section " + index
                                    + " with enough text to occupy a line or two on the page and exercise wrapping.")
                            .addList(list -> list.bullet().items("First point", "Second point", "Third point"))
                            .addParagraph("Body paragraph two for section " + index
                                    + ", continuing the content so pagination has real work to do."));
                }
            };
            case "table1000" -> flow -> flow.addTable(t -> {
                t.name("Lines").autoColumns(5).header("#", "Item", "Qty", "Unit", "Total").repeatHeader();
                for (int row = 1; row <= 1000; row++) {
                    t.row(String.valueOf(row), "Line item " + row, "3", "12.50", "37.50");
                }
            });
            case "images20" -> flow -> {
                for (int picture = 0; picture < 20; picture++) {
                    flow.addImage(img -> img.source(DocumentImageData.fromBytes(image)).width(240).height(135));
                }
            };
            default -> throw new IllegalArgumentException("Unknown shape: " + shape);
        };
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
