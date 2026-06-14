package com.demcha.compose.jmh;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.SvgBenchmarkFixtures;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.svg.SvgIcon;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Strict JMH micro-benchmark: an "icon ramp" — place {@code N} copies of a
 * multi-layer SVG icon (the realistic icon-grid / skills-ribbon workload) and
 * render to PDF. Parameterized over N so the trend (node-build + layout +
 * render per icon) is visible; the icon is parsed once in setup so the ramp
 * measures placement scaling, not re-parsing.
 *
 * <pre>
 *   ./mvnw -f benchmarks/pom.xml clean package -DskipTests
 *   java -jar benchmarks/target/benchmarks.jar IconRamp
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
public class IconRampJmhBenchmark {

    @Param({"8", "32", "128"})
    public int iconCount;

    /** Parsed once: the ramp measures node-build + layout + render scaling, not re-parsing. */
    private final SvgIcon icon = SvgIcon.parse(SvgBenchmarkFixtures.MULTI_LAYER_ICON_SVG);

    /**
     * Places {@code iconCount} icons in a flow and renders the document.
     *
     * @param blackhole JMH sink
     * @throws Exception if rendering fails
     */
    @Benchmark
    public void renderIconRamp(Blackhole blackhole) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(24))
                .create()) {
            PageFlowBuilder flow = document.pageFlow().name("IconRamp").spacing(4);
            for (int i = 0; i < iconCount; i++) {
                flow.addSvgIcon(icon, 32);
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
