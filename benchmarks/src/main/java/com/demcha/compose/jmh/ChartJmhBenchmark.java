package com.demcha.compose.jmh;

import com.demcha.compose.ChartBenchmarkFixtures;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
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
 * Strict JMH micro-benchmark: end-to-end render of a chart-heavy document — a
 * grouped bar, a multi-series line (both 12 categories × 3 series) and a 6-slice
 * pie — to PDF bytes. Charts compile into engine primitives at layout time, so
 * this exercises {@code ChartLayoutResolver} + per-primitive geometry + label
 * text-metrics on top of the normal compose / layout / render pipeline.
 *
 * <pre>
 *   ./mvnw -f benchmarks/pom.xml clean package -DskipTests
 *   java -jar benchmarks/target/benchmarks.jar Chart
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
public class ChartJmhBenchmark {

    /**
     * Builds the three-chart document and renders it to PDF bytes.
     *
     * @param blackhole JMH sink that consumes the rendered bytes
     * @throws Exception if rendering fails
     */
    @Benchmark
    public void renderChartDocument(Blackhole blackhole) throws Exception {
        blackhole.consume(renderDocument());
    }

    private static byte[] renderDocument() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(36))
                .create()) {
            document.pageFlow()
                    .name("ChartBenchmark")
                    .spacing(12)
                    .chart(ChartBenchmarkFixtures.barSpec(), ChartBenchmarkFixtures.barStyle())
                    .chart(ChartBenchmarkFixtures.lineSpec(), ChartBenchmarkFixtures.lineStyle())
                    .chart(ChartBenchmarkFixtures.pieSpec())
                    .build();
            return document.toPdfBytes();
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
