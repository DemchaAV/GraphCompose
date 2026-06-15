package com.demcha.compose;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.backend.fixed.pdf.PdfMeasurementResources;
import com.demcha.compose.document.layout.DocumentGraph;
import com.demcha.compose.document.layout.DocumentLayoutPassContext;
import com.demcha.compose.document.layout.LayoutCanvas;
import com.demcha.compose.document.layout.LayoutCompiler;
import com.demcha.compose.document.layout.LayoutGraph;
import com.demcha.compose.document.layout.NodeRegistry;
import com.demcha.compose.document.node.DocumentNode;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

/**
 * Deterministic allocation probe for the v1.8 chart subsystem: warm
 * (JIT-steady) bytes allocated by the layout-compile pass of a chart-heavy
 * document (a grouped bar, a multi-series line, and a pie). Charts are resolved
 * into engine primitives during compile, so this isolates the chart-resolve +
 * geometry-emission allocation — the noise-free signal a develop-vs-branch A/B
 * needs. No {@code src/main} changes.
 *
 * @author Artem Demchyshyn
 */
public final class ChartAllocProbe {

    private static final com.sun.management.ThreadMXBean THREAD_MX =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    private static final int WARMUP = 60;
    private static final int MEASURE = 11;

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        enableAllocationMeasurement();

        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            session.pageFlow(flow -> flow
                    .chart(ChartBenchmarkFixtures.barSpec(), ChartBenchmarkFixtures.barStyle())
                    .chart(ChartBenchmarkFixtures.lineSpec(), ChartBenchmarkFixtures.lineStyle())
                    .chart(ChartBenchmarkFixtures.pieSpec()));

            List<DocumentNode> roots = session.roots();
            LayoutCanvas canvas = session.canvas();
            NodeRegistry registry = session.registry();

            try (PdfMeasurementResources resources = PdfMeasurementResources.open(List.of())) {
                LayoutCompiler compiler = new LayoutCompiler(registry);
                DocumentGraph graph = new DocumentGraph(roots);

                int pages = 0;
                // Warm up so the measured allocation is JIT steady state, not
                // class-load / first-call cold start.
                for (int i = 0; i < WARMUP; i++) {
                    pages = compile(compiler, graph, registry, canvas, resources).totalPages();
                }

                long[] alloc = new long[MEASURE];
                for (int m = 0; m < MEASURE; m++) {
                    long before = currentThreadAllocatedBytes();
                    LayoutGraph layout = compile(compiler, graph, registry, canvas, resources);
                    alloc[m] = before < 0 ? -1 : currentThreadAllocatedBytes() - before;
                    pages = layout.totalPages();
                }
                Arrays.sort(alloc);

                System.out.println("GraphCompose chart layout-compile allocation probe");
                System.out.printf("document: grouped bar + line (12 cats x 3 series) + 6-slice pie, pages: %d%n", pages);
                System.out.printf("warm compile allocation (median of %d): %s%n",
                        MEASURE, kb(alloc[MEASURE / 2]));
                System.out.printf("  min %s / max %s%n", kb(alloc[0]), kb(alloc[MEASURE - 1]));
            }
        }
    }

    private static LayoutGraph compile(LayoutCompiler compiler, DocumentGraph graph,
                                       NodeRegistry registry, LayoutCanvas canvas,
                                       PdfMeasurementResources resources) {
        DocumentLayoutPassContext context = new DocumentLayoutPassContext(
                registry, canvas, resources.fontLibrary(), resources.textMeasurementSystem(), false);
        return compiler.compile(graph, context, context);
    }

    private static String kb(long bytes) {
        return bytes < 0 ? "n/a (allocation measurement unsupported)" : "%.1f KB".formatted(bytes / 1024.0);
    }

    private static void enableAllocationMeasurement() {
        try {
            if (THREAD_MX.isThreadAllocatedMemorySupported() && !THREAD_MX.isThreadAllocatedMemoryEnabled()) {
                THREAD_MX.setThreadAllocatedMemoryEnabled(true);
            }
        } catch (UnsupportedOperationException ignored) {
            // Allocation measurement unsupported on this JVM; the probe reports n/a.
        }
    }

    private static long currentThreadAllocatedBytes() {
        try {
            if (!THREAD_MX.isThreadAllocatedMemorySupported() || !THREAD_MX.isThreadAllocatedMemoryEnabled()) {
                return -1;
            }
        } catch (UnsupportedOperationException ex) {
            return -1;
        }
        return THREAD_MX.getCurrentThreadAllocatedBytes();
    }
}
