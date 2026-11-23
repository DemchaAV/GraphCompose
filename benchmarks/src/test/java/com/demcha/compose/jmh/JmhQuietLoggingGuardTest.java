package com.demcha.compose.jmh;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.BenchmarkListEntry;
import org.openjdk.jmh.runner.format.OutputFormatFactory;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.io.File;
import java.lang.reflect.Method;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every JMH benchmark in the runner jar must start its forked JVM with the
 * quiet benchmark logging unless the run opts out.
 *
 * <p>The benchmarks are read from {@code META-INF/BenchmarkList}, the list the
 * JMH runner itself reads, so a benchmark added later is checked without being
 * named here. Checked with them are the {@code @State} classes their
 * {@code @Benchmark}, {@code @Setup} and {@code @TearDown} methods take, in any
 * package, and every {@code @State} class compiled into this package.
 * Each class is initialized in a class loader of its own, as the
 * forked JVM does, and the test then asks that loader's SLF4J which level the
 * engine's loggers ended up at.</p>
 */
class JmhQuietLoggingGuardTest {

    private static final String BENCHMARK_LOGGING_PROPERTY = "graphcompose.benchmark.logging";
    private static final String LOGBACK_CONFIG_PROPERTY = "logback.configurationFile";
    private static final String LOGBACK_STATUS_LISTENER_PROPERTY = "logback.statusListenerClass";
    private static final String ENGINE_LOGGER = "com.demcha.compose";
    private static final List<String> LOGGING_PROPERTIES = List.of(
            BENCHMARK_LOGGING_PROPERTY, LOGBACK_CONFIG_PROPERTY, LOGBACK_STATUS_LISTENER_PROPERTY);

    private final Map<String, String> saved = new TreeMap<>();

    @BeforeEach
    void saveAndClearLoggingProperties() {
        for (String name : LOGGING_PROPERTIES) {
            saved.put(name, System.getProperty(name));
        }
        clearLoggingProperties();
    }

    private static void clearLoggingProperties() {
        LOGGING_PROPERTIES.forEach(System::clearProperty);
    }

    @AfterEach
    void restoreLoggingProperties() {
        saved.forEach((name, value) -> {
            if (value == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, value);
            }
        });
    }

    @Test
    void everyJmhBenchmarkStartsWithQuietLogging() throws Exception {
        List<String> benchmarks = benchmarkClasses();
        assertThat(benchmarks)
                .as("benchmark classes read from META-INF/BenchmarkList")
                .contains(CanonicalRenderJmhBenchmark.class.getName(),
                        DocxExportJmhBenchmark.class.getName());
        // A @State class of its own may be initialized before the benchmark
        // class, so it has to be quiet on its own as well.
        TreeSet<String> classes = new TreeSet<>(benchmarks);
        classes.addAll(stateClassesTakenBy(benchmarks));
        classes.addAll(stateClassesInThisPackage());

        List<String> loud = new ArrayList<>();
        for (String className : classes) {
            clearLoggingProperties();
            if (engineDebugEnabledAfterInitializing(className)) {
                loud.add(className);
            }
        }
        assertThat(loud)
                .as("JMH benchmarks whose forked JVM would log the engine at DEBUG;"
                        + " extend QuietJmhBenchmark")
                .isEmpty();
    }

    @Test
    void withoutTheHookTheClasspathLogsAtDebug() throws Exception {
        // The guard above is only worth something if an unconfigured JVM on this
        // classpath is loud: a logback.xml arriving on the classpath would make
        // every benchmark look quiet whether it is hooked or not.
        assertThat(engineDebugEnabledAfterInitializing(null)).isTrue();
    }

    @Test
    void debugOptOutStillLogs() throws Exception {
        System.setProperty(BENCHMARK_LOGGING_PROPERTY, "debug");

        assertThat(engineDebugEnabledAfterInitializing(CanonicalRenderJmhBenchmark.class.getName()))
                .isTrue();
    }

    private static List<String> benchmarkClasses() {
        PrintStream discard = new PrintStream(OutputStream.nullOutputStream());
        TreeSet<String> names = new TreeSet<>();
        for (BenchmarkListEntry entry : BenchmarkList.defaultList()
                .getAll(OutputFormatFactory.createFormatInstance(discard, VerboseMode.SILENT), List.of())) {
            names.add(entry.getUserClassQName());
        }
        return new ArrayList<>(names);
    }

    /**
     * The {@code @State} classes the benchmarks' {@code @Benchmark},
     * {@code @Setup} and {@code @TearDown} methods take, followed through the
     * states' own fixture methods: the set JMH instantiates, whatever package
     * a state class lives in.
     */
    private static List<String> stateClassesTakenBy(List<String> benchmarks) throws Exception {
        ClassLoader loader = JmhQuietLoggingGuardTest.class.getClassLoader();
        ArrayDeque<Class<?>> pending = new ArrayDeque<>();
        for (String name : benchmarks) {
            pending.add(Class.forName(name, false, loader));
        }
        TreeSet<String> seen = new TreeSet<>();
        List<String> states = new ArrayList<>();
        while (!pending.isEmpty()) {
            Class<?> type = pending.poll();
            if (!seen.add(type.getName())) {
                continue;
            }
            for (Method method : type.getMethods()) {
                if (!method.isAnnotationPresent(Benchmark.class) && !method.isAnnotationPresent(Setup.class)
                        && !method.isAnnotationPresent(TearDown.class)) {
                    continue;
                }
                for (Class<?> parameter : method.getParameterTypes()) {
                    if (parameter.isAnnotationPresent(State.class) && !seen.contains(parameter.getName())) {
                        states.add(parameter.getName());
                        pending.add(parameter);
                    }
                }
            }
        }
        return states;
    }

    /**
     * Every {@code @State} class compiled into this package, nested ones
     * included; JMH's generated code lives in a sub-package and is skipped.
     */
    private static List<String> stateClassesInThisPackage() throws Exception {
        String pkg = QuietJmhBenchmark.class.getPackageName();
        Path root = Path.of(QuietJmhBenchmark.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertThat(root).as("benchmark classes are read from a directory, not a jar").isDirectory();
        Path dir = root.resolve(pkg.replace('.', '/'));
        List<String> states = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                String simple = file.getFileName().toString().replaceFirst("\\.class$", "");
                Class<?> type = Class.forName(pkg + "." + simple, false,
                        JmhQuietLoggingGuardTest.class.getClassLoader());
                if (type.isAnnotationPresent(State.class)) {
                    states.add(type.getName());
                }
            }
        }
        assertThat(states).as("@State classes found in " + dir).isNotEmpty();
        return states;
    }

    /**
     * Initializes {@code className} (none when {@code null}) in a fresh class
     * loader over this test's classpath, then reports whether the engine's
     * logger in that loader is enabled for DEBUG.
     */
    private static boolean engineDebugEnabledAfterInitializing(String className) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(classpath(), ClassLoader.getPlatformClassLoader())) {
            if (className != null) {
                Class.forName(className, true, loader);
            }
            Class<?> factory = loader.loadClass("org.slf4j.LoggerFactory");
            Object logger = factory.getMethod("getLogger", String.class).invoke(null, ENGINE_LOGGER);
            return (boolean) loader.loadClass("org.slf4j.Logger")
                    .getMethod("isDebugEnabled")
                    .invoke(logger);
        }
    }

    private static URL[] classpath() throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!entry.isBlank()) {
                urls.add(Path.of(entry).toUri().toURL());
            }
        }
        return urls.toArray(URL[]::new);
    }
}
