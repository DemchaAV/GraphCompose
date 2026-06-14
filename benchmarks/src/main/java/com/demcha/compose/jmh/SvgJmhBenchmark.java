package com.demcha.compose.jmh;

import com.demcha.compose.SvgBenchmarkFixtures;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.svg.SvgPath;
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
 * Strict JMH micro-benchmark for the v1.8 SVG-import surface — the first
 * feature-object benchmark (the rest of the suite renders text / tables only).
 *
 * <p>Three measured operations, all pure CPU + allocation (no
 * {@code DocumentSession}, no PDF render):</p>
 * <ul>
 *   <li>{@code parseSvgPath} — {@link SvgPath#parse} of a real Material icon
 *       {@code d} string (arc→cubic conversion, normalization).</li>
 *   <li>{@code readSvgIcon} — {@link SvgIcon#parse} of a multi-layer icon (XML
 *       parse, {@code <g>} transform accumulation, gradient resolution, one
 *       {@link SvgPath} per layer).</li>
 *   <li>{@code svgIconToNode} — {@link SvgIcon#node} on a pre-parsed icon (the
 *       {@code PathNode} / layer-stack allocation done once per placement).</li>
 * </ul>
 *
 * <p>Microsecond-scale work, so it needs the forked, JIT-stable JMH harness
 * (an {@code exec:java} run cannot fork). Build the runner jar and run:</p>
 *
 * <pre>
 *   ./mvnw -f benchmarks/pom.xml clean package -DskipTests
 *   java -jar benchmarks/target/benchmarks.jar Svg
 * </pre>
 *
 * @author Artem Demchyshyn
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class SvgJmhBenchmark {

    /** Parsed once so {@code svgIconToNode} measures only the node-build cost. */
    private final SvgIcon icon = SvgIcon.parse(SvgBenchmarkFixtures.MULTI_LAYER_ICON_SVG);

    /**
     * Parses a real icon path-data string into normalized segments.
     *
     * @param blackhole JMH sink
     */
    @Benchmark
    public void parseSvgPath(Blackhole blackhole) {
        blackhole.consume(SvgPath.parse(
                SvgBenchmarkFixtures.MATERIAL_HEART_D,
                0, 0, SvgBenchmarkFixtures.HEART_VIEWBOX, SvgBenchmarkFixtures.HEART_VIEWBOX));
    }

    /**
     * Reads a whole multi-layer SVG icon (XML parse → layers).
     *
     * @param blackhole JMH sink
     */
    @Benchmark
    public void readSvgIcon(Blackhole blackhole) {
        blackhole.consume(SvgIcon.parse(SvgBenchmarkFixtures.MULTI_LAYER_ICON_SVG));
    }

    /**
     * Builds a placeable node (path nodes + layer stack) from a parsed icon.
     *
     * @param blackhole JMH sink
     */
    @Benchmark
    public void svgIconToNode(Blackhole blackhole) {
        blackhole.consume(icon.node(48.0));
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
