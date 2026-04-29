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
            "PdfComposer",
            "MainPageCV",
            "MainPageCvDTO",
            "ModuleYml",
            "ModuleSummary");

    private static final Set<String> MAIN_CANONICAL_SOURCE_ALLOWLIST = Set.of();

    private static final Set<String> DOCUMENTATION_ALLOWLIST = Set.of(
            "src/test/java/com/demcha/documentation/CanonicalSurfaceGuardTest.java",
            "src/test/java/com/demcha/documentation/DocumentationCoverageTest.java");

    private static final Set<String> CANONICAL_DOCUMENT_TEST_ALLOWLIST = Set.of(
            "src/test/java/com/demcha/compose/document/templates/architecture/CanonicalTemplateComposerPdfBoundaryTest.java");

    private static final Set<String> CANONICAL_BENCHMARK_ALLOWLIST = Set.of();

    // Add an entry here only when a public markdown document genuinely
    // needs to name retired legacy surface (com.demcha.templates.*,
    // com.demcha.compose.v2.*, GraphCompose.pdf(...), PdfComposer) —
    // i.e. an audit / migration / parity log. Internal planning docs
    // should live outside the public docs surface (see .gitignore →
    // docs/private/).
    private static final Set<String> PUBLIC_MARKDOWN_ALLOWLIST = Set.of();
    private static final List<String> FORBIDDEN_PUBLIC_AUTHORING_IMPORTS = List.of(
            "import com.demcha.compose.engine.");

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
    void canonicalBenchmarkEntryPointsShouldAvoidLegacySurface() throws IOException {
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

    @Test
    void publicMarkdownDocsShouldAvoidLegacySurfaceOutsideHistoricalAuditNotes() throws IOException {
        assertNoForbiddenMarkdownReferences(
                List.of(
                        PROJECT_ROOT.resolve("README.md"),
                        PROJECT_ROOT.resolve("CONTRIBUTING.md"),
                        PROJECT_ROOT.resolve("examples/README.md"),
                        PROJECT_ROOT.resolve("docs")),
                PUBLIC_MARKDOWN_ALLOWLIST);
    }

    @Test
    void publicAuthoringDocsAndExamplesShouldNotImportEngineInternals() throws IOException {
        assertNoForbiddenAuthoringImports(
                List.of(
                        PROJECT_ROOT.resolve("README.md"),
                        PROJECT_ROOT.resolve("docs/getting-started.md"),
                        PROJECT_ROOT.resolve("docs/recipes.md"),
                        PROJECT_ROOT.resolve("docs/layout-snapshot-testing.md"),
                        PROJECT_ROOT.resolve("examples/src/main/java/com/demcha/examples"),
                        PROJECT_ROOT.resolve("src/test/java/com/demcha/documentation/DocumentationExamplesTest.java")));
    }

    @Test
    void semanticAuthoringValuePackagesShouldNotImportEngineInternals() throws IOException {
        assertNoForbiddenAuthoringImports(
                List.of(
                        PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/node"),
                        PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/dsl"),
                        PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/style"),
                        PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/table"),
                        PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/image")));
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

    private void assertNoForbiddenMarkdownReferences(List<Path> roots, Set<String> allowlist) throws IOException {
        Set<String> violations = new TreeSet<>();

        for (Path root : roots) {
            if (Files.isRegularFile(root)) {
                if (!allowlist.contains(relative(root)) && containsForbiddenToken(root)) {
                    violations.add(relative(root));
                }
                continue;
            }

            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".md"))
                        // docs/private/ is gitignored — it holds local-only
                        // planning notes, audits, and roadmap drafts that
                        // are not part of the public docs surface and so
                        // are not subject to this guard.
                        .filter(path -> !relative(path).startsWith("docs/private/"))
                        .filter(path -> !allowlist.contains(relative(path)))
                        .filter(this::containsForbiddenToken)
                        .map(this::relative)
                        .forEach(violations::add);
            }
        }

        assertThat(violations)
                .describedAs("Public markdown docs must stay on the canonical document surface")
                .isEmpty();
    }

    private boolean containsForbiddenToken(Path path) {
        try {
            String source = Files.readString(path);
            return FORBIDDEN_TOKENS.stream().anyMatch(source::contains);
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect " + path, e);
        }
    }

    private void assertNoForbiddenAuthoringImports(List<Path> roots) throws IOException {
        Set<String> violations = new TreeSet<>();
        for (Path root : roots) {
            if (Files.notExists(root)) {
                continue;
            }
            if (Files.isRegularFile(root)) {
                if (containsForbiddenAuthoringImport(root)) {
                    violations.add(relative(root));
                }
                continue;
            }
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".md"))
                        .filter(this::containsForbiddenAuthoringImport)
                        .map(this::relative)
                        .forEach(violations::add);
            }
        }

        assertThat(violations)
                .describedAs("Public authoring docs and runnable examples should not import engine internals")
                .isEmpty();
    }

    private boolean containsForbiddenAuthoringImport(Path path) {
        try {
            String source = Files.readString(path);
            return FORBIDDEN_PUBLIC_AUTHORING_IMPORTS.stream().anyMatch(source::contains);
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect " + path, e);
        }
    }

    private String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
