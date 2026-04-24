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

class PackageMapGuardTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final List<String> FORBIDDEN_OLD_PACKAGE_TOKENS = List.of(
            "com.demcha.compose." + "layout_" + "core",
            "com.demcha.compose.document.model." + "node",
            "com.demcha.compose." + "font_" + "library",
            "com.demcha.compose." + "markdown",
            "layout_" + "core",
            "font_" + "library",
            "pdf_" + "systems",
            "implemented_" + "systems",
            "components_" + "builders",
            "abstract_" + "builders");

    @Test
    void productionPackagesShouldHavePackageInfo() throws IOException {
        Path sourceRoot = PROJECT_ROOT.resolve("src/main/java/com/demcha/compose");

        try (var paths = Files.walk(sourceRoot)) {
            Set<String> missing = new TreeSet<>(paths
                    .filter(Files::isDirectory)
                    .filter(this::containsJavaSource)
                    .filter(path -> Files.notExists(path.resolve("package-info.java")))
                    .map(this::relative)
                    .collect(Collectors.toSet()));

            assertThat(missing)
                    .describedAs("Every production package should document ownership and extension rules")
                    .isEmpty();
        }
    }

    @Test
    void oldPackageNamesShouldNotReturnToSourcesDocsOrExamples() throws IOException {
        List<Path> roots = List.of(
                PROJECT_ROOT.resolve("src/main/java"),
                PROJECT_ROOT.resolve("src/test/java"),
                PROJECT_ROOT.resolve("examples/src/main/java"),
                PROJECT_ROOT.resolve("README.md"),
                PROJECT_ROOT.resolve("CONTRIBUTING.md"),
                PROJECT_ROOT.resolve("docs"));

        Set<String> violations = new TreeSet<>();
        for (Path root : roots) {
            collectForbiddenReferences(root, violations);
        }

        assertThat(violations)
                .describedAs("Old package names should not appear in source, public docs, or examples")
                .isEmpty();
    }

    private void collectForbiddenReferences(Path root, Set<String> violations) throws IOException {
        if (Files.isRegularFile(root)) {
            if (isScannedTextFile(root) && containsForbiddenToken(root)) {
                violations.add(relative(root));
            }
            return;
        }

        try (var paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isScannedTextFile)
                    .filter(path -> !isIgnored(path))
                    .filter(this::containsForbiddenToken)
                    .map(this::relative)
                    .forEach(violations::add);
        }
    }

    private boolean containsJavaSource(Path directory) {
        try (var children = Files.list(directory)) {
            return children.anyMatch(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to inspect " + directory, ex);
        }
    }

    private boolean isScannedTextFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".java")
                || fileName.endsWith(".md")
                || fileName.endsWith(".xml")
                || fileName.endsWith(".properties")
                || fileName.endsWith(".yml")
                || fileName.endsWith(".yaml");
    }

    private boolean containsForbiddenToken(Path path) {
        try {
            String source = Files.readString(path);
            return FORBIDDEN_OLD_PACKAGE_TOKENS.stream().anyMatch(source::contains);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to inspect " + path, ex);
        }
    }

    private boolean isIgnored(Path path) {
        String relative = relative(path);
        return relative.startsWith(".git/")
                || relative.startsWith(".claude/")
                || relative.startsWith(".idea/")
                || relative.contains("/target/")
                || relative.startsWith("target/")
                || relative.startsWith("logs/");
    }

    private String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
