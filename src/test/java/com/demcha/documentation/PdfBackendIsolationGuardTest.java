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
 * Architectural guard that keeps PDFBox out of the canonical document API and
 * non-PDF fixed-layout contracts.
 */
class PdfBackendIsolationGuardTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final String PDFBOX_IMPORT_PREFIX = "import org.apache.pdfbox.";
    private static final String PDFBOX_REFERENCE = "org.apache.pdfbox.";

    private static final List<Path> CANONICAL_ROOTS = List.of(
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/GraphCompose.java"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/api"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/dsl"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/node"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/style"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/table"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/image"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/layout"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/snapshot"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/backend/fixed"));

    @Test
    void pdfboxShouldStayOutOfCanonicalAndNonPdfBackendContracts() throws IOException {
        Map<String, Set<String>> violations = new LinkedHashMap<>();
        for (Path file : canonicalJavaFiles()) {
            if (isPdfBackendFile(file)) {
                continue;
            }
            Set<String> references = pdfBoxReferencesIn(file);
            if (!references.isEmpty()) {
                violations.put(relative(file), references);
            }
        }

        assertThat(violations)
                .describedAs("PDFBox dependencies must stay behind document.backend.fixed.pdf.* "
                        + "or the legacy engine.render.pdf.* renderer. Canonical API and layout contracts "
                        + "should use document-level value objects such as DocumentPageSize.")
                .isEmpty();
    }

    private List<Path> canonicalJavaFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path root : CANONICAL_ROOTS) {
            if (Files.notExists(root)) {
                continue;
            }
            if (Files.isRegularFile(root)) {
                files.add(root);
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

    private boolean isPdfBackendFile(Path file) {
        String relative = relative(file);
        return relative.startsWith("src/main/java/com/demcha/compose/document/backend/fixed/pdf/");
    }

    private Set<String> pdfBoxReferencesIn(Path file) throws IOException {
        Set<String> references = new TreeSet<>();
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.trim();
            if (trimmed.startsWith(PDFBOX_IMPORT_PREFIX)) {
                int semicolon = trimmed.indexOf(';');
                if (semicolon >= 0) {
                    references.add(trimmed.substring("import ".length(), semicolon).trim());
                }
            } else if (trimmed.contains(PDFBOX_REFERENCE)) {
                references.add(trimmed);
            }
        }
        return references;
    }

    private String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
