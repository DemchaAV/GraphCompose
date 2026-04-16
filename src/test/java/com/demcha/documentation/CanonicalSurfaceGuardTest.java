package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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

    private static final Set<String> DOCUMENTATION_ALLOWLIST = Set.of(
            "src/test/java/com/demcha/documentation/CanonicalSurfaceGuardTest.java",
            "src/test/java/com/demcha/documentation/DocumentationCoverageTest.java");

    private static final Set<String> CANONICAL_DOCUMENT_TEST_ALLOWLIST = Set.of(
            "src/test/java/com/demcha/compose/document/templates/CanonicalTemplateParityTest.java");

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

    private void assertNoForbiddenReferences(Path root, Set<String> allowlist) throws IOException {
        try (var paths = Files.walk(root)) {
            List<String> violations = new TreeSet<>(paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
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
