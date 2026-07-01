package com.demcha.compose.jmh;

import com.demcha.compose.CanonicalBenchmarkSupport;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.presets.ModernProfessional;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.presets.ModernInvoice;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Strict JMH <em>single-shot</em> benchmark: the JIT-cold cost of the first PDF
 * render in a fresh JVM. Every other JMH bench in this module reports
 * steady-state ({@code AverageTime} after warmup), which is what a long-lived
 * server pays — but a short-lived CLI invocation or a serverless (Lambda)
 * cold-start pays the <em>first</em> render, with the layout and PDFBox classes
 * unloaded and uncompiled. This bench measures exactly that.
 *
 * <p>{@code Mode.SingleShotTime} with {@code @Warmup(0)} and {@code @Measurement(1)}
 * times a single invocation; {@code @Fork(10)} repeats it in ten fresh JVMs so the
 * reported number is a distribution of cold first-renders, not one lucky start.
 * The spec/template objects are built in {@link #setUp()} so the measured shot is
 * the cold render path, not fixture assembly. Same workloads as the warm benches
 * ({@code engine-simple} inline, {@code ModernInvoice}, {@code ModernProfessional})
 * so cold and warm numbers are directly comparable.</p>
 *
 * <pre>
 *   ./mvnw -f benchmarks/pom.xml clean package -DskipTests
 *   java -jar benchmarks/target/benchmarks.jar ColdStart
 * </pre>
 *
 * @author Artem Demchyshyn
 */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
@Fork(10)
public class ColdStartJmhBenchmark {

    private InvoiceDocumentSpec invoice;
    private DocumentTemplate<InvoiceDocumentSpec> invoiceTemplate;
    private CvDocument cv;
    private DocumentTemplate<CvDocument> cvTemplate;

    /** Builds the specs and templates once per fork, outside the measured cold shot. */
    @Setup
    public void setUp() {
        invoice = CanonicalBenchmarkSupport.canonicalInvoice();
        invoiceTemplate = ModernInvoice.create();
        cv = CanonicalBenchmarkSupport.canonicalCv();
        cvTemplate = ModernProfessional.create();
    }

    /**
     * Cold first render of a small inline engine document.
     *
     * @return the rendered PDF bytes (consumed by JMH)
     * @throws Exception if rendering fails
     */
    @Benchmark
    public byte[] coldEngineSimple() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(36))
                .create()) {
            document.pageFlow()
                    .name("ColdEngineSimple")
                    .spacing(10)
                    .addParagraph("GraphCompose cold-start check")
                    .addSection("Summary", section -> section
                            .addParagraph("First render in a fresh JVM, layout and PDFBox classes cold."))
                    .addSection("Body", section -> section
                            .addParagraph("Structured business document composition.")
                            .addParagraph("Semantic layout, pagination, deterministic output."))
                    .build();
            return document.toPdfBytes();
        }
    }

    /**
     * Cold first render of the canonical invoice through {@code ModernInvoice}.
     *
     * @return the rendered PDF bytes (consumed by JMH)
     * @throws Exception if rendering fails
     */
    @Benchmark
    public byte[] coldInvoiceTemplate() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(36))
                .create()) {
            invoiceTemplate.compose(document, invoice);
            return document.toPdfBytes();
        }
    }

    /**
     * Cold first render of the canonical CV through the {@code ModernProfessional} preset.
     *
     * @return the rendered PDF bytes (consumed by JMH)
     * @throws Exception if rendering fails
     */
    @Benchmark
    public byte[] coldCvTemplate() throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(36))
                .create()) {
            cvTemplate.compose(document, cv);
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
