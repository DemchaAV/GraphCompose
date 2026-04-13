package com.demcha.compose.layout_core.system.implemented_systems.pdf_systems;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyPdfRenderAllowlistTest {

    private static final Set<String> ALLOWLIST = Set.of(
            "src/main/java/com/demcha/compose/layout_core/components/renderable/BlockText.java",
            "src/main/java/com/demcha/compose/layout_core/components/renderable/Circle.java",
            "src/main/java/com/demcha/compose/layout_core/components/renderable/Container.java",
            "src/main/java/com/demcha/compose/layout_core/components/renderable/Element.java",
            "src/main/java/com/demcha/compose/layout_core/components/renderable/ImageComponent.java",
            "src/main/java/com/demcha/compose/layout_core/components/renderable/Line.java",
            "src/main/java/com/demcha/compose/layout_core/components/renderable/Link.java",
            "src/main/java/com/demcha/compose/layout_core/components/renderable/Rectangle.java",
            "src/main/java/com/demcha/compose/layout_core/components/renderable/TableCellBox.java"
    );

    private static final Pattern PDF_RENDER_PATTERN = Pattern.compile(
            "implements\\s+[^\\{;]*\\bPdfRender\\b",
            Pattern.MULTILINE | Pattern.DOTALL
    );

    @Test
    void shouldKeepLegacyPdfRenderUsageExplicitlyAllowlisted() throws IOException {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path sourceRoot = projectRoot.resolve("src/main/java");

        try (var paths = Files.walk(sourceRoot)) {
            Set<String> actual = new TreeSet<>(paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::implementsLegacyPdfRender)
                    .map(projectRoot::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.toSet()));

            assertThat(actual).containsExactlyInAnyOrderElementsOf(ALLOWLIST);
        }
    }

    private boolean implementsLegacyPdfRender(Path path) {
        try {
            return PDF_RENDER_PATTERN.matcher(Files.readString(path)).find();
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect " + path, e);
        }
    }
}
