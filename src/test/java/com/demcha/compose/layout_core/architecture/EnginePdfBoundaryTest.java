package com.demcha.compose.layout_core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EnginePdfBoundaryTest {

    @Test
    void componentsPackageShouldNotDependOnPdfboxOrPdfSystems() throws IOException {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path sourceRoot = projectRoot.resolve("src/main/java/com/demcha/compose/layout_core/components");

        try (var paths = Files.walk(sourceRoot)) {
            List<String> violations = new TreeSet<>(paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::containsForbiddenImports)
                    .map(projectRoot::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.toList()))
                    .stream()
                    .toList();

            assertThat(violations).isEmpty();
        }
    }

    @Test
    void engineRenderInterfacesShouldStayBackendNeutral() throws IOException {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path sourceRoot = projectRoot.resolve("src/main/java/com/demcha/compose/layout_core/system/interfaces");

        try (var paths = Files.walk(sourceRoot)) {
            List<String> violations = new TreeSet<>(paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::containsForbiddenImports)
                    .map(projectRoot::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.toList()))
                    .stream()
                    .toList();

            assertThat(violations).isEmpty();
        }
    }

    private boolean containsForbiddenImports(Path path) {
        try {
            String source = Files.readString(path);
            return source.contains("org.apache.pdfbox")
                   || source.contains("implemented_systems.pdf_systems");
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect " + path, e);
        }
    }
}
