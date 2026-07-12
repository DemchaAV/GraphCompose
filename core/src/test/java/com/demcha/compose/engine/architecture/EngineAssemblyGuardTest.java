package com.demcha.compose.engine.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class EngineAssemblyGuardTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final List<String> FORBIDDEN_PRODUCTION_TOKENS = List.of(
            "components_" + "builders",
            "abstract_" + "builders",
            "com.demcha.compose.engine.components.components_" + "builders",
            "com.demcha.compose.engine.components.containers.abstract_" + "builders");

    @Test
    void productionSourcesShouldNotUseLegacyBuilderPackages() throws IOException {
        Path sourceRoot = PROJECT_ROOT.resolve("src/main/java");
        Set<String> violations = new TreeSet<>();

        try (var paths = Files.walk(sourceRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(this::containsForbiddenToken)
                    .map(this::relative)
                    .forEach(violations::add);
        }

        assertThat(violations)
                .describedAs("Production V2 engine must not depend on removed legacy builder packages")
                .isEmpty();
    }

    private boolean containsForbiddenToken(Path path) {
        try {
            String source = Files.readString(path);
            return FORBIDDEN_PRODUCTION_TOKENS.stream().anyMatch(source::contains);
        } catch (IOException e) {
            throw new RuntimeException("Failed to inspect " + path, e);
        }
    }

    private String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
