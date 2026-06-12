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
 * Finding 10 probe — warm per-compile allocation of paginating one large table
 * across many pages.
 *
 * <p>A table that spans N pages is split page-by-page: each split re-slices the
 * shrinking tail via {@code TableLayoutSupport.sliceTablePreparedNode}. On the
 * body-only path that slice currently re-copies the already-immutable row /
 * row-height sub-lists ({@code List.copyOf} of a {@code subList}), so the
 * cumulative copy work is O(rows x pages). This probe compiles a big multi-page
 * table through the canonical {@link LayoutCompiler} and reports the warm
 * (JIT-steady) bytes allocated by the compile pass, so a develop (copy) vs
 * branch (sub-list view) A/B shows the slice-allocation reduction directly. No
 * {@code src/main} changes.</p>
 */
public final class TablePaginationAllocProbe {

    private static final com.sun.management.ThreadMXBean THREAD_MX =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    private static final int ROWS = 2500;
    private static final int COLUMNS = 4;
    private static final int WARMUP = 60;
    private static final int MEASURE = 11;

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        enableAllocationMeasurement();

        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(24, 24, 24, 24)
                .create()) {
            session.pageFlow(flow -> flow.addTable(table -> {
                table.autoColumns(COLUMNS).header("Item", "Qty", "Price", "Total");
                for (int row = 1; row <= ROWS; row++) {
                    table.row("Line item " + row, "3", "12.50", "38.75");
                }
            }));

            List<DocumentNode> roots = session.roots();
            LayoutCanvas canvas = session.canvas();
            NodeRegistry registry = session.registry();

            try (PdfMeasurementResources resources = PdfMeasurementResources.open(List.of())) {
                LayoutCompiler compiler = new LayoutCompiler(registry);
                DocumentGraph graph = new DocumentGraph(roots);

                int pages = 0;
                // Warm up the compile path so the measured allocation reflects
                // JIT steady state, not class-load / first-call cold start.
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

                System.out.println("GraphCompose Finding-10 Table-Pagination Allocation Probe");
                System.out.printf("table: %d rows x %d cols, pages: %d (≈%d page splits)%n",
                        ROWS, COLUMNS, pages, Math.max(0, pages - 1));
                System.out.printf("warm compile allocation (median of %d): %.1f KB%n",
                        MEASURE, alloc[MEASURE / 2] / 1024.0);
                System.out.printf("  min %.1f KB / max %.1f KB%n",
                        alloc[0] / 1024.0, alloc[MEASURE - 1] / 1024.0);
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
