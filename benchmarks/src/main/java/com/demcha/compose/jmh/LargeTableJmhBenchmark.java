package com.demcha.compose.jmh;

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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Strict JMH micro-benchmark: end-to-end render of a production-scale priced
 * table that paginates across many pages, parameterized over row count so the
 * scaling trend of large-table pagination + render is visible.
 *
 * <p>The rest of the suite renders small documents; nothing measured how the
 * engine handles a genuinely large multi-page table (the existing
 * {@code TablePaginationAllocProbe} measures layout-compile allocation only, not
 * end-to-end render). The header repeats on every page (the realistic report
 * layout), so this exercises per-page header re-emission as well as row layout
 * and slicing at scale.</p>
 *
 * <pre>
 *   ./mvnw -f benchmarks/pom.xml clean package -DskipTests
 *   java -jar benchmarks/target/benchmarks.jar LargeTable
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
public class LargeTableJmhBenchmark {

    @Param({"100", "500", "1000"})
    public int rows;

    /**
     * Renders the priced table document to PDF bytes.
     *
     * @param blackhole JMH sink
     * @throws Exception if rendering fails
     */
    @Benchmark
    public void renderLargeTable(Blackhole blackhole) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(28))
                .create()) {
            document.pageFlow(flow -> {
                flow.name("LargeTable").spacing(8);
                flow.addParagraph("Priced line items");
                flow.addTable(t -> {
                    t.autoColumns(5).header("#", "Item", "Qty", "Unit", "Total").repeatHeader();
                    for (int r = 1; r <= rows; r++) {
                        t.row(String.valueOf(r), "Line item " + r, "3", "12.50", "37.50");
                    }
                });
            });
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
