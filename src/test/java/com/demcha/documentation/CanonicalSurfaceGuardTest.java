package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalSurfaceGuardTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final List<String> FORBIDDEN_TOKENS = List.of(
            "com.demcha.templates",
            "com.demcha.compose.v2",
            "TemplateBuilder",
            "GraphCompose.pdf(",
            "PdfComposer");

    private static final Set<String> MAIN_CANONICAL_SOURCE_ALLOWLIST = Set.of();

    private static final Set<String> DOCUMENTATION_ALLOWLIST = Set.of(
            "src/test/java/com/demcha/documentation/CanonicalSurfaceGuardTest.java",
            "src/test/java/com/demcha/documentation/DocumentationCoverageTest.java");

    private static final Set<String> CANONICAL_DOCUMENT_TEST_ALLOWLIST = Set.of(
            "src/test/java/com/demcha/compose/document/templates/architecture/CanonicalTemplateComposerPdfBoundaryTest.java");

    private static final Set<String> CANONICAL_BENCHMARK_ALLOWLIST = Set.of(
            "src/test/java/com/demcha/compose/ArchitectureComparisonBenchmark.java",
            "src/test/java/com/demcha/compose/ComparativeBenchmark.java");

    @Test
    void canonicalMainSourcesShouldAvoidLegacySurfaceOutsideTransitionMappers() throws IOException {
        assertNoForbiddenReferences(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document"),
                MAIN_CANONICAL_SOURCE_ALLOWLIST);
    }

    @Test
    void runnableExamplesShouldStayOnCanonicalSurface() throws IOException {
        assertNoForbiddenReferences(
                PROJECT_ROOT.resolve("examples/src/main/java/com/demcha/examples"),
                Set.of());
    }

    @Test
    void documentationTestsShouldAvoidLegacySurfaceOutsideGuardFiles() throws IOException {
        assertNoForbiddenReferences(
                PROJECT_ROOT.resolve("src/test/java/com/demcha/documentation"),
                DOCUMENTATION_ALLOWLIST);
    }

    @Test
    void canonicalDocumentTestsShouldAvoidLegacySurfaceOutsideCompatibilityParity() throws IOException {
        assertNoForbiddenReferences(
                PROJECT_ROOT.resolve("src/test/java/com/demcha/compose/document"),
                CANONICAL_DOCUMENT_TEST_ALLOWLIST);
    }

    @Test
    void canonicalBenchmarkEntryPointsShouldAvoidLegacySurfaceOutsideExplicitMigrationHarnesses() throws IOException {
        assertNoForbiddenReferences(
                PROJECT_ROOT.resolve("src/test/java/com/demcha/compose"),
                path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.endsWith("Benchmark.java")
                            || fileName.endsWith("StressTest.java")
                            || fileName.endsWith("EnduranceTest.java");
                },
                CANONICAL_BENCHMARK_ALLOWLIST);
    }

    private void assertNoForbiddenReferences(Path root, Set<String> allowlist) throws IOException {
        assertNoForbiddenReferences(root, path -> true, allowlist);
    }

    private void assertNoForbiddenReferences(Path root,
                                             Predicate<Path> include,
                                             Set<String> allowlist) throws IOException {
        try (var paths = Files.walk(root)) {
            List<String> violations = new TreeSet<>(paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(include)
                    .filter(path -> !allowlist.contains(relative(path)))
                    .filter(this::containsForbiddenToken)
                    .map(this::relative)
                    .collect(Collectors.toList()))
                    .stream()
                    .toList();

            assertThat(violations)
                    .describedAs("Files under %s must stay on the canonical document surface", relative(root))
                    .isEmpty();
        }
    }

    private boolean containsForbiddenToken(Path path) {
        try {
            String source = Files.readString(path);
            return FORBIDDEN_TOKENS.stream().anyMatch(source::contains);
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect " + path, e);
        }
    }

    private String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
