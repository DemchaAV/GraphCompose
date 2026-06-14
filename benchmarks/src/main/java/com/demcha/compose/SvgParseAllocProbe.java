package com.demcha.compose;

import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.svg.SvgPath;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Deterministic allocation probe for the v1.8 SVG-import path: warm
 * (JIT-steady) bytes allocated per {@link SvgPath#parse}, per
 * {@link SvgIcon#parse}, and per {@link SvgIcon#node} — the three operations
 * with no analogue in the rest of the suite (which is text / table only).
 *
 * <p>Allocation counts are noise-free (unlike wall-clock or {@code peakHeapMb}),
 * so this is the signal the "optimize the engine, not benchmarks" rule wants:
 * a develop-vs-branch A/B shows a parse/read/node allocation change directly.
 * No {@code src/main} changes.</p>
 *
 * @author Artem Demchyshyn
 */
public final class SvgParseAllocProbe {

    private static final com.sun.management.ThreadMXBean THREAD_MX =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    private static final int WARMUP = 60;
    private static final int MEASURE = 11;

    /** Escape sink so the JIT cannot elide the measured allocations. */
    private static long sink;

    public static void main(String[] args) {
        BenchmarkSupport.configureQuietLogging();
        enableAllocationMeasurement();

        SvgIcon icon = SvgIcon.parse(SvgBenchmarkFixtures.MULTI_LAYER_ICON_SVG);

        double parseKb = measureAllocKb(() -> SvgPath.parse(
                SvgBenchmarkFixtures.MATERIAL_HEART_D,
                0, 0, SvgBenchmarkFixtures.HEART_VIEWBOX, SvgBenchmarkFixtures.HEART_VIEWBOX));
        double readKb = measureAllocKb(() -> SvgIcon.parse(SvgBenchmarkFixtures.MULTI_LAYER_ICON_SVG));
        double nodeKb = measureAllocKb(() -> icon.node(48.0));

        System.out.println("GraphCompose SVG-import allocation probe (median of " + MEASURE + ")");
        System.out.printf("  SvgPath.parse (heart d)     : %s%n", kb(parseKb));
        System.out.printf("  SvgIcon.parse (multi-layer) : %s%n", kb(readKb));
        System.out.printf("  SvgIcon.node(48)            : %s%n", kb(nodeKb));
        System.out.println("alloc sink: " + sink);
    }

    private static double measureAllocKb(Supplier<Object> op) {
        for (int i = 0; i < WARMUP; i++) {
            sink += System.identityHashCode(op.get());
        }
        long[] alloc = new long[MEASURE];
        for (int m = 0; m < MEASURE; m++) {
            long before = currentThreadAllocatedBytes();
            Object result = op.get();
            long after = currentThreadAllocatedBytes();
            sink += System.identityHashCode(result);
            alloc[m] = before < 0 ? -1 : after - before;
        }
        Arrays.sort(alloc);
        return alloc[MEASURE / 2] / 1024.0;
    }

    private static String kb(double value) {
        return value < 0 ? "n/a (allocation measurement unsupported)" : "%.1f KB/op".formatted(value);
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
