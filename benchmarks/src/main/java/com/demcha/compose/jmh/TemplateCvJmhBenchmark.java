package com.demcha.compose.jmh;

import com.demcha.compose.CanonicalBenchmarkSupport;
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.presets.ModernProfessional;
import com.demcha.compose.document.templates.cv.spec.CvSpec;
import com.demcha.compose.document.theme.BusinessTheme;
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
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Strict JMH benchmark: render the canonical CV through the
 * {@code ModernProfessional} layered template to PDF bytes — the realistic
 * "render a CV from data" workload, heavier than the bare-DSL
 * {@link CanonicalRenderJmhBenchmark}. The spec and template are built once in
 * {@link #setUp()} so the measured work is the render, not fixture
 * construction.
 *
 * <p>Run through the shaded runner jar (see the module README); forked runs
 * cannot use {@code exec:java}.</p>
 *
 * @author Artem Demchyshyn
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class TemplateCvJmhBenchmark {

    private CvSpec cv;
    private DocumentTemplate<CvSpec> template;

    /**
     * Builds the canonical CV spec and the template once, outside measurement.
     */
    @Setup
    public void setUp() {
        cv = CanonicalBenchmarkSupport.canonicalCv();
        template = ModernProfessional.create(BusinessTheme.modern());
    }

    /**
     * Renders the canonical CV through the template and consumes the bytes.
     *
     * @param blackhole JMH sink that consumes the rendered bytes
     * @throws Exception if rendering fails
     */
    @Benchmark
    public void renderModernProfessionalCv(Blackhole blackhole) throws Exception {
        try (DocumentSession document = GraphCompose.document()
                .pageSize(DocumentPageSize.A4)
                .margin(DocumentInsets.of(36))
                .create()) {
            template.compose(document, cv);
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
