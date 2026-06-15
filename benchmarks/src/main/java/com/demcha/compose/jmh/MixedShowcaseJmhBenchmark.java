package com.demcha.compose.jmh;

import com.demcha.compose.ChartBenchmarkFixtures;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.SvgBenchmarkFixtures;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentPaint;
import com.demcha.compose.document.svg.SvgIcon;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Strict JMH micro-benchmark: a representative "v1.8 showcase" document that
 * mixes every new vector feature in one render — running prose with two inline
 * sparklines, a grouped bar chart and a pie chart, a row of SVG icons, and
 * gradient accent paths. This is the integration canary: it answers "did adding
 * any v1.8 feature blow up a realistic document?" in one number.
 *
 * <pre>
 *   ./mvnw -f benchmarks/pom.xml clean package -DskipTests
 *   java -jar benchmarks/target/benchmarks.jar MixedShowcase
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
public class MixedShowcaseJmhBenchmark {

    private static final int ICONS = 8;

    /** Parsed once; the bench measures the mixed render, not icon parsing. */
    private final SvgIcon icon = SvgIcon.parse(SvgBenchmarkFixtures.MULTI_LAYER_ICON_SVG);

    /**
     * Renders the mixed v1.8 showcase document to PDF bytes.
     *
     * @param blackhole JMH sink
     * @throws Exception if rendering fails
     */
    @Benchmark
    public void renderMixedShowcase(Blackhole blackhole) throws Exception {
        DocumentPaint accent = DocumentPaint.linear(
                DocumentColor.rgb(167, 139, 250), DocumentColor.rgb(97, 40, 217));
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(32))
                .create()) {
            PageFlowBuilder flow = document.pageFlow().name("MixedShowcase").spacing(12);
            flow.addParagraph("v1.8 feature showcase");
            flow.addRich(r -> r
                    .plain("Revenue ")
                    .sparkline(42, 9, DocumentColor.rgb(20, 80, 95), 65.2, 69.8, 74.1, 81.3, 88.2)
                    .plain("   profit ")
                    .sparklineLine(42, 9, 1.6, DocumentColor.rgb(196, 153, 76), 28.1, 30.7, 32.9, 36.4, 39.5));
            flow.chart(ChartBenchmarkFixtures.barSpec(), ChartBenchmarkFixtures.barStyle());
            flow.chart(ChartBenchmarkFixtures.pieSpec());
            for (int i = 0; i < ICONS; i++) {
                flow.addSvgIcon(icon, 32);
            }
            flow.addPath(p -> p.size(220, 28)
                    .moveTo(0.0, 0.5).curveTo(0.25, 1.0, 0.75, 0.0, 1.0, 0.5).fill(accent));
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
