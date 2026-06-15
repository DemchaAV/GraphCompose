package com.demcha.compose;

import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.builtins.InvoiceTemplateV1;
import com.demcha.compose.document.templates.builtins.ProposalTemplateV1;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/**
 * Allocation-rate and GC-pressure probe over realistic templates. The endurance
 * and stress harnesses only check that sustained rendering stays stable / under a
 * heap ceiling; nothing reports how much garbage a single render churns, which is
 * what drives GC pressure for a high-throughput server.
 *
 * <p>For each template it renders many warm documents and reports two things: the
 * warm per-document allocation (ThreadMXBean current-thread bytes / doc — a
 * deterministic figure ideal for an A/B), and the JVM garbage collections those
 * renders triggered (count + time via {@code GarbageCollectorMXBean} — JVM-wide
 * and GC-timing sensitive, so advisory). No {@code src/main} changes.</p>
 *
 * <pre>
 *   ./mvnw -f benchmarks/pom.xml exec:java -Dexec.mainClass=com.demcha.compose.AllocationRateProbe
 * </pre>
 *
 * @author Artem Demchyshyn
 */
public final class AllocationRateProbe {

    private static final com.sun.management.ThreadMXBean THREAD_MX =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    private static final int WARMUP = 60;
    private static final int MEASURE = 300;

    @FunctionalInterface
    private interface Render {
        byte[] run() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        BenchmarkSupport.configureQuietLogging();
        enableAllocationMeasurement();

        InvoiceDocumentSpec invoice = CanonicalBenchmarkSupport.canonicalInvoice();
        InvoiceTemplateV1 invoiceTemplate = new InvoiceTemplateV1();
        ProposalDocumentSpec proposal = CanonicalBenchmarkSupport.canonicalProposal();
        ProposalTemplateV1 proposalTemplate = new ProposalTemplateV1();

        System.out.println("GraphCompose allocation-rate / GC-pressure probe (" + MEASURE + " warm renders each)");
        System.out.printf("%-12s | %14s | %10s | %12s | %12s%n",
                "Template", "Alloc / doc", "GC count", "GC time ms", "Total alloc");
        System.out.println("-".repeat(70));
        report("invoice", () -> renderTemplate(s -> invoiceTemplate.compose(s, invoice)));
        report("proposal", () -> renderTemplate(s -> proposalTemplate.compose(s, proposal)));
        System.out.println();
        System.out.println("Alloc/doc = warm ThreadMXBean bytes per render (deterministic A/B signal). "
                + "GC count/time = JVM collections those renders triggered (advisory, GC-timing sensitive).");
    }

    private interface Compose {
        void into(DocumentSession session);
    }

    private static byte[] renderTemplate(Compose compose) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(22, 22, 22, 22)
                .create()) {
            compose.into(session);
            return session.toPdfBytes();
        }
    }

    private static void report(String name, Render render) throws Exception {
        long dummy = 0;
        for (int i = 0; i < WARMUP; i++) {
            dummy += render.run().length;
        }

        System.gc();
        Thread.sleep(50);

        long gcCountStart = totalGcCount();
        long gcTimeStart = totalGcTime();
        long allocStart = currentThreadAllocatedBytes();

        for (int i = 0; i < MEASURE; i++) {
            dummy += render.run().length;
        }

        long alloc = allocStart < 0 ? -1 : currentThreadAllocatedBytes() - allocStart;
        long gcCount = totalGcCount() - gcCountStart;
        long gcTime = totalGcTime() - gcTimeStart;

        System.out.printf("%-12s | %14s | %10d | %12d | %12s%n",
                name,
                alloc < 0 ? "n/a" : kb(alloc / (double) MEASURE),
                gcCount,
                gcTime,
                alloc < 0 ? "n/a" : mb(alloc));

        if (dummy == 0) {
            System.out.println("Error: no bytes generated");
        }
    }

    private static long totalGcCount() {
        long total = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = bean.getCollectionCount();
            if (count > 0) {
                total += count;
            }
        }
        return total;
    }

    private static long totalGcTime() {
        long total = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long time = bean.getCollectionTime();
            if (time > 0) {
                total += time;
            }
        }
        return total;
    }

    private static String kb(double bytes) {
        return "%.1f KB".formatted(bytes / 1024.0);
    }

    private static String mb(long bytes) {
        return "%.1f MB".formatted(bytes / (1024.0 * 1024.0));
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
