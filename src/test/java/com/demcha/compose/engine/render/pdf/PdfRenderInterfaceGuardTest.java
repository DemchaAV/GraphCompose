package com.demcha.compose.engine.render.pdf;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PdfRenderInterfaceGuardTest {

    private static final Set<String> ALLOWLIST = Set.of();

    private static final Pattern PDF_RENDER_PATTERN = Pattern.compile(
            "implements\\s+[^\\{;]*\\bPdfRender\\b",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    @Test
    void shouldNotAllowBackendSpecificPdfRenderUsageInMainSources() throws IOException {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path sourceRoot = projectRoot.resolve("src/main/java");

        try (var paths = Files.walk(sourceRoot)) {
            Set<String> actual = new TreeSet<>(paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::implementsBackendSpecificPdfRender)
                    .map(projectRoot::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.toSet()));

            assertThat(actual).containsExactlyInAnyOrderElementsOf(ALLOWLIST);
        }
    }

    private boolean implementsBackendSpecificPdfRender(Path path) {
        try {
            return PDF_RENDER_PATTERN.matcher(Files.readString(path)).find();
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect " + path, e);
        }
    }
}
