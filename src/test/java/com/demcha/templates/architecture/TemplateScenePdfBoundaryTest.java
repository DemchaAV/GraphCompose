package com.demcha.templates.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateScenePdfBoundaryTest {

    @Test
    void sceneBuildersShouldNotDependOnPdfTypesOrPdfComposer() throws IOException {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path sourceRoot = projectRoot.resolve("src/main/java/com/demcha/templates/builtins");

        try (var paths = Files.walk(sourceRoot)) {
            List<String> violations = new TreeSet<>(paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("SceneBuilder.java"))
                    .filter(this::containsForbiddenReferences)
                    .map(projectRoot::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.toList()))
                    .stream()
                    .toList();

            assertThat(violations).isEmpty();
        }
    }

    private boolean containsForbiddenReferences(Path path) {
        try {
            String source = Files.readString(path);
            return source.contains("PDDocument")
                   || source.contains("PDPage")
                   || source.contains("PDRectangle")
                   || source.contains("PdfComposer");
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect " + path, e);
        }
    }
}
