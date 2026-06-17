package com.demcha.compose.jmh;

import com.demcha.compose.ChartBenchmarkFixtures;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.chart.ChartSpec;
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
 * Strict JMH micro-benchmark: end-to-end render of a single chart, parameterized
 * over the chart-layout branches the resolver takes — grouped bar, horizontal
 * bar, stacked bar, a non-zero value-axis minimum (lifted baseline), line, pie,
 * and donut. {@code ChartJmhBenchmark} renders one grouped-bar + line + pie
 * document; this isolates each distinct {@code ChartLayoutResolver} branch so a
 * regression in, say, the stacking or horizontal-transpose geometry shows up on
 * its own row rather than blended into a three-chart total.
 *
 * <pre>
 *   ./mvnw -f benchmarks/pom.xml clean package -DskipTests
 *   java -jar benchmarks/target/benchmarks.jar ChartVariant
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
public class ChartVariantJmhBenchmark {

    @Param({"grouped-bar", "horizontal-bar", "stacked-bar", "axis-min-bar", "line", "pie", "donut"})
    public String variant;

    /** Resolved once per trial so the bench measures the render, not spec assembly. */
    private ChartSpec spec;

    @Setup
    public void setUp() {
        spec = switch (variant) {
            case "grouped-bar" -> ChartBenchmarkFixtures.barSpec();
            case "horizontal-bar" -> ChartBenchmarkFixtures.horizontalBarSpec();
            case "stacked-bar" -> ChartBenchmarkFixtures.stackedBarSpec();
            case "axis-min-bar" -> ChartBenchmarkFixtures.axisMinBarSpec();
            case "line" -> ChartBenchmarkFixtures.lineSpec();
            case "pie" -> ChartBenchmarkFixtures.pieSpec();
            case "donut" -> ChartBenchmarkFixtures.donutSpec();
            default -> throw new IllegalArgumentException("Unknown chart variant: " + variant);
        };
    }

    /**
     * Renders a one-chart document of the parameterized variant to PDF bytes.
     *
     * @param blackhole JMH sink
     * @throws Exception if rendering fails
     */
    @Benchmark
    public void renderChartVariant(Blackhole blackhole) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(36))
                .create()) {
            document.pageFlow().name("ChartVariant").spacing(12).chart(spec).build();
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
