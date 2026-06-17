package com.demcha.compose.jmh;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentPaint;
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

import java.util.concurrent.TimeUnit;

/**
 * Strict JMH micro-benchmark: render {@code N} identical curved blob paths in one
 * paint mode — flat solid fill, linear gradient, or translucent (alpha) fill —
 * parameterized over the mode, so the render-<em>time</em> cost of each vector
 * paint branch is isolated. This is the timing complement to
 * {@code VectorRenderOperatorProbe} (which counts the PDF operators each mode
 * emits): gradient shading and alpha ExtGState are heavier than the flat fast
 * fill path, and this puts a millisecond number on that.
 *
 * <pre>
 *   ./mvnw -f benchmarks/pom.xml clean package -DskipTests
 *   java -jar benchmarks/target/benchmarks.jar VectorPaint
 * </pre>
 *
 * @author Artem Demchyshyn
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class VectorPaintJmhBenchmark {

    private static final int PATHS = 40;

    @Param({"flat", "gradient", "alpha"})
    public String paint;

    private DocumentPaint gradient;
    private DocumentColor flat;
    private DocumentColor translucent;

    /** Paint objects built once per trial, outside the measured render. */
    @Setup
    public void setUp() {
        gradient = DocumentPaint.linear(
                DocumentColor.rgb(167, 139, 250), DocumentColor.rgb(97, 40, 217));
        flat = DocumentColor.rgb(40, 90, 160);
        translucent = DocumentColor.rgb(40, 90, 160).withOpacity(0.5);
    }

    /**
     * Renders {@code PATHS} blob paths in the parameterized paint mode to PDF bytes.
     *
     * @param blackhole JMH sink
     * @throws Exception if rendering fails
     */
    @Benchmark
    public void renderVectorPaint(Blackhole blackhole) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(28))
                .create()) {
            PageFlowBuilder flow = document.pageFlow().name("VectorPaint").spacing(4);
            for (int i = 0; i < PATHS; i++) {
                flow.addPath(p -> {
                    p.size(60, 36)
                            .moveTo(0.0, 0.5)
                            .curveTo(0.25, 1.0, 0.75, 1.0, 1.0, 0.5)
                            .curveTo(0.75, 0.0, 0.25, 0.0, 0.0, 0.5)
                            .closePath();
                    switch (paint) {
                        case "flat" -> p.fillColor(flat);
                        case "gradient" -> p.fill(gradient);
                        case "alpha" -> p.fillColor(translucent);
                        default -> throw new IllegalArgumentException("Unknown paint mode: " + paint);
                    }
                });
            }
            flow.build();
            blackhole.consume(document.toPdfBytes());
        }
    }

    /**
     * Runs the JMH harness over this benchmark.
     *
     * @param args JMH CLI arguments
     * @throws Exception if the JMH runner fails
     */
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
