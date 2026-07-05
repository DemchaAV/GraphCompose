package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architectural guard that keeps Apache POI out of the lean engine module.
 *
 * <p>The semantic DOCX/PPTX backend — the only code that ever touched POI —
 * lives in the separate {@code graph-compose-render-docx} module. The engine
 * (root module) must stay free of every {@code org.apache.poi.*} import or
 * fully-qualified reference so it never drags {@code poi-ooxml} back onto its
 * classpath.</p>
 *
 * <p>This freezes that invariant at the source level: the retired {@code no-poi}
 * CI job only ran the suite <em>without</em> POI on the classpath, it never
 * asserted the sources were POI-free. Mirrors
 * {@link PdfBackendIsolationGuardTest}'s grep-style walk/assert.</p>
 */
class EnginePoiIsolationGuardTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final String POI_IMPORT_PREFIX = "import org.apache.poi.";
    private static final String POI_REFERENCE = "org.apache.poi.";

    private static final List<Path> ENGINE_ROOTS = List.of(
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose"),
            PROJECT_ROOT.resolve("src/test/java/com/demcha/compose"));

    @Test
    void poiShouldStayOutOfTheEngineModule() throws IOException {
        Map<String, Set<String>> violations = new LinkedHashMap<>();
        for (Path file : engineJavaFiles()) {
            Set<String> references = poiReferencesIn(file);
            if (!references.isEmpty()) {
                violations.put(relative(file), references);
            }
        }

        assertThat(violations)
                .describedAs("Apache POI must stay out of the lean engine module. The semantic "
                        + "DOCX/PPTX backend lives in graph-compose-render-docx — keep every "
                        + "org.apache.poi.* import and reference there, not under com.demcha.compose.")
                .isEmpty();
    }

    private List<Path> engineJavaFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path root : ENGINE_ROOTS) {
            if (Files.notExists(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .forEach(files::add);
            }
        }
        return files;
    }

    private Set<String> poiReferencesIn(Path file) throws IOException {
        Set<String> references = new TreeSet<>();
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.trim();
            if (trimmed.startsWith(POI_IMPORT_PREFIX)) {
                int semicolon = trimmed.indexOf(';');
                if (semicolon >= 0) {
                    references.add(trimmed.substring("import ".length(), semicolon).trim());
                }
            } else if (trimmed.contains(POI_REFERENCE)) {
                references.add(trimmed);
            }
        }
        return references;
    }

    private String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
