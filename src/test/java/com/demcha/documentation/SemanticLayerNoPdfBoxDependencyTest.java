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
 * Architectural guard for semantic layer purity.
 *
 * <p>This test pins two invariants for the {@code document.node} semantic layer:</p>
 * <ol>
 *   <li><b>Strict:</b> semantic node types must never import {@code org.apache.pdfbox.*}
 *       directly. The PDF rendering layer is the only place allowed to depend on PDFBox.</li>
 *   <li><b>Strict:</b> semantic nodes must not import PDF backend option records.
 *       Link, bookmark, and barcode metadata use renderer-neutral
 *       {@code Document*Options} types instead.</li>
 * </ol>
 */
class SemanticLayerNoPdfBoxDependencyTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path SEMANTIC_NODE_ROOT =
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/node");

    private static final String PDFBOX_IMPORT_PREFIX = "import org.apache.pdfbox.";
    private static final String BACKEND_OPTIONS_IMPORT_PREFIX =
            "import com.demcha.compose.document.backend.fixed.pdf.options.";

    private static final Map<String, Set<String>> ALLOWED_BACKEND_OPTIONS = new LinkedHashMap<>();

    @Test
    void semanticNodesMustNotImportPdfBoxDirectly() throws IOException {
        Map<String, Set<String>> violations = new LinkedHashMap<>();
        for (Path file : semanticJavaFiles()) {
            Set<String> pdfboxImports = importsStartingWith(file, PDFBOX_IMPORT_PREFIX);
            if (!pdfboxImports.isEmpty()) {
                violations.put(relative(file), pdfboxImports);
            }
        }
        assertThat(violations)
                .describedAs("Semantic node types in document.node must remain renderer-neutral. "
                        + "Move PDFBox-specific concerns into document.backend.fixed.pdf.*.")
                .isEmpty();
    }

    @Test
    void semanticNodeBackendOptionLeaksMatchAllowlist() throws IOException {
        Map<String, Set<String>> actual = new LinkedHashMap<>();
        for (Path file : semanticJavaFiles()) {
            Set<String> options = importsStartingWith(file, BACKEND_OPTIONS_IMPORT_PREFIX);
            if (!options.isEmpty()) {
                actual.put(relative(file), options);
            }
        }

        Map<String, Set<String>> unexpected = new LinkedHashMap<>();
        for (var entry : actual.entrySet()) {
            Set<String> allowed = ALLOWED_BACKEND_OPTIONS.getOrDefault(entry.getKey(), Set.of());
            Set<String> diff = new TreeSet<>(entry.getValue());
            diff.removeAll(allowed);
            if (!diff.isEmpty()) {
                unexpected.put(entry.getKey(), diff);
            }
        }

        Map<String, Set<String>> stale = new LinkedHashMap<>();
        for (var entry : ALLOWED_BACKEND_OPTIONS.entrySet()) {
            Set<String> imports = actual.getOrDefault(entry.getKey(), Set.of());
            Set<String> missing = new TreeSet<>(entry.getValue());
            missing.removeAll(imports);
            if (!missing.isEmpty()) {
                stale.put(entry.getKey(), missing);
            }
        }

        assertThat(unexpected)
                .describedAs("New backend.fixed.pdf.options.* leaks appeared in document.node.*. "
                        + "Either move the option to document.node or, for Phase 3, document the leak in the allowlist.")
                .isEmpty();

        assertThat(stale)
                .describedAs("Allowlist entries no longer match real imports — prune them from "
                        + "SemanticLayerNoPdfBoxDependencyTest.ALLOWED_BACKEND_OPTIONS.")
                .isEmpty();
    }

    private List<Path> semanticJavaFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        try (var stream = Files.walk(SEMANTIC_NODE_ROOT)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(files::add);
        }
        return files;
    }

    private Set<String> importsStartingWith(Path file, String prefix) throws IOException {
        Set<String> imports = new TreeSet<>();
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                int semicolon = trimmed.indexOf(';');
                if (semicolon < 0) continue;
                imports.add(trimmed.substring("import ".length(), semicolon).trim());
            }
        }
        return imports;
    }

    private String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
