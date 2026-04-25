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
 * Architectural guard that pins the inventory of {@code com.demcha.compose.engine.*}
 * imports leaking into the public API surface.
 *
 * <p>The baseline below records every currently tolerated leak. New imports of engine
 * types into these packages will fail the test, while removing a leak (the desired
 * direction) requires shrinking the allowlist below.</p>
 *
 * <p>Some leaks are internal bridge points where the canonical API still delegates
 * to the shared engine. The allowlist should shrink as those bridge points gain
 * public adapters.</p>
 */
class PublicApiNoEngineLeakTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final String ENGINE_IMPORT_PREFIX = "import com.demcha.compose.engine.";

    private static final List<Path> PUBLIC_API_ROOTS = List.of(
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/GraphCompose.java"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/api"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/dsl"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/node"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/style"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/table"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/image"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/font"));

    /**
     * Baseline of currently accepted engine leaks per public file. The map is
     * intentionally empty when the public surface has no accepted leaks. Each entry is
     * keyed by the file path (relative to the project root, forward slashes) and
     * contains the exact set of fully-qualified engine imports that the file is
     * allowed to use.
     *
     * <p>To remove a leak, delete the import in the source file and remove the
     * matching entry here. Adding a brand-new leak is intentionally inconvenient.</p>
     */
    private static final Map<String, Set<String>> ALLOWED_ENGINE_IMPORTS = new LinkedHashMap<>();
    static {
    }

    @Test
    void publicApiSurfaceShouldOnlyContainAllowlistedEngineImports() throws IOException {
        Map<String, Set<String>> actual = collectEngineImportsByFile();

        // Files with leaks not in the allowlist (new violations).
        Map<String, Set<String>> unexpectedLeaks = new LinkedHashMap<>();
        for (var entry : actual.entrySet()) {
            String path = entry.getKey();
            Set<String> imports = entry.getValue();
            Set<String> allowed = ALLOWED_ENGINE_IMPORTS.getOrDefault(path, Set.of());
            Set<String> diff = new TreeSet<>(imports);
            diff.removeAll(allowed);
            if (!diff.isEmpty()) {
                unexpectedLeaks.put(path, diff);
            }
        }

        // Allowlist entries that no longer match real source — must be pruned.
        Map<String, Set<String>> staleAllowlistEntries = new LinkedHashMap<>();
        for (var entry : ALLOWED_ENGINE_IMPORTS.entrySet()) {
            String path = entry.getKey();
            Set<String> allowed = entry.getValue();
            Set<String> actualImports = actual.getOrDefault(path, Set.of());
            Set<String> stale = new TreeSet<>(allowed);
            stale.removeAll(actualImports);
            if (!stale.isEmpty()) {
                staleAllowlistEntries.put(path, stale);
            }
        }

        assertThat(unexpectedLeaks)
                .describedAs("New engine.* imports leaked into the public API surface. "
                        + "Either route them through a public adapter or, if intentional, add them to the allowlist.")
                .isEmpty();

        assertThat(staleAllowlistEntries)
                .describedAs("Allowlist entries reference imports that no longer exist. "
                        + "Delete them from PublicApiNoEngineLeakTest.ALLOWED_ENGINE_IMPORTS.")
                .isEmpty();
    }

    private Map<String, Set<String>> collectEngineImportsByFile() throws IOException {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (Path root : PUBLIC_API_ROOTS) {
            if (Files.notExists(root)) {
                continue;
            }
            List<Path> files = new ArrayList<>();
            if (Files.isRegularFile(root)) {
                files.add(root);
            } else {
                try (var stream = Files.walk(root)) {
                    stream.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".java"))
                            .forEach(files::add);
                }
            }
            for (Path file : files) {
                Set<String> imports = engineImportsIn(file);
                if (!imports.isEmpty()) {
                    result.put(relative(file), imports);
                }
            }
        }
        return result;
    }

    private Set<String> engineImportsIn(Path file) throws IOException {
        Set<String> imports = new TreeSet<>();
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.trim();
            if (trimmed.startsWith(ENGINE_IMPORT_PREFIX)) {
                int semicolon = trimmed.indexOf(';');
                if (semicolon < 0) continue;
                String fqn = trimmed.substring("import ".length(), semicolon).trim();
                imports.add(fqn);
            }
        }
        return imports;
    }

    private String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
