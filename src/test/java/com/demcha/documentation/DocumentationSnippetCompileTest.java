package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compiles the Java snippets published in {@code docs/} so that an API change
 * which breaks a documented snippet fails the build instead of silently rotting
 * the public docs.
 *
 * <p>Complements {@code DocumentationExamplesTest} (which renders hand-kept Java
 * copies of representative examples): this guard reads the literal markdown
 * fences, so the published page itself cannot drift from the API.
 *
 * <p>The guard is <strong>opt-in</strong>: only a fenced {@code java} block
 * immediately preceded by an invisible marker comment is compiled —
 *
 * <pre>{@code
 * <!-- doc-example: id=first-document-smallest mode=method -->
 * ```java
 * ...
 * ```
 * }</pre>
 *
 * The marker is an HTML comment, so it renders to nothing on GitHub and keeps
 * the published page clean. Teaching fragments that intentionally reference
 * symbols defined only in prose (a bare {@code invoice} variable, pseudo-code)
 * carry no marker and are left untouched, which keeps the guard free of false
 * positives.
 *
 * <p>Each marked block is wrapped into a compilation unit according to its
 * {@code mode} and compiled in-memory against the test runtime classpath (the
 * same canonical classes and dependencies the rest of the suite sees). Compiler
 * <em>errors</em> fail the test; warnings are ignored.
 *
 * <dl>
 *   <dt>{@code mode=method}</dt><dd>statements; wrapped in a {@code void} method
 *   that {@code throws Exception}. Leading {@code import} lines are lifted above
 *   the wrapper.</dd>
 *   <dt>{@code mode=members}</dt><dd>field/method declarations; inserted as class
 *   members. Leading imports are lifted.</dd>
 *   <dt>{@code mode=class}</dt><dd>a complete compilation unit; compiled
 *   verbatim.</dd>
 * </dl>
 */
class DocumentationSnippetCompileTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path DOCS_ROOT = PROJECT_ROOT.resolve("docs");

    private static final Pattern MARKER =
            Pattern.compile("^<!--\\s*doc-example:\\s*(.+?)\\s*-->\\s*$");
    private static final Pattern IMPORT_LINE =
            Pattern.compile("^\\s*import\\s+(?:static\\s+)?[\\w.]+(?:\\.\\*)?\\s*;\\s*$");
    private static final Pattern TYPE_NAME =
            Pattern.compile("\\b(?:class|interface|record|enum)\\s+(\\w+)");
    private static final Set<String> SUPPORTED_MODES = Set.of("method", "members", "class");

    @Test
    void publishedJavaSnippetsShouldCompile() throws IOException {
        List<Example> examples = collectExamples();

        // The scan must find work — a silent zero would let the guard pass while
        // covering nothing (e.g. a moved docs folder or a broken marker regex).
        assertThat(examples)
                .describedAs("No doc-example markers found under %s — the guard would cover nothing", DOCS_ROOT)
                .isNotEmpty();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler)
                .describedAs("A JDK compiler is required to compile doc snippets; run the build on a JDK, not a JRE")
                .isNotNull();

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);

        Path classOutput = Files.createTempDirectory("doc-snippets-classes");
        Map<String, Example> byUnitName = new LinkedHashMap<>();
        List<JavaFileObject> units = new ArrayList<>();
        for (Example example : examples) {
            String unitName = example.unitName();
            byUnitName.put(unitName, example);
            units.add(new StringSource(unitName, example.toCompilationUnit()));
        }

        try {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classOutput.toFile()));
            List<String> options = List.of(
                    "-proc:none",
                    "-classpath", System.getProperty("java.class.path"));
            compiler.getTask(null, fileManager, diagnostics, options, null, units).call();
        } finally {
            fileManager.close();
            deleteRecursively(classOutput);
        }

        List<String> errors = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            Example example = exampleFor(diagnostic, byUnitName);
            String origin = example == null
                    ? "(unknown unit)"
                    : example.id + " — " + relative(example.file);
            errors.add("[%s] %s".formatted(origin, diagnostic.getMessage(null).replaceAll("\\s+", " ").trim()));
        }

        assertThat(errors)
                .describedAs("Every marked Java snippet under docs/ must compile against the current API")
                .isEmpty();
    }

    @Test
    void docExampleMarkersShouldBeWellFormed() throws IOException {
        List<String> problems = new ArrayList<>();
        Map<String, String> idToFile = new LinkedHashMap<>();

        for (Path doc : markdownFiles()) {
            List<String> lines = Files.readAllLines(doc, StandardCharsets.UTF_8);
            String rel = relative(doc);
            for (int i = 0; i < lines.size(); i++) {
                Matcher marker = MARKER.matcher(lines.get(i).trim());
                if (!marker.matches()) {
                    continue;
                }
                Map<String, String> attributes = parseAttributes(marker.group(1));
                String id = attributes.get("id");
                String mode = attributes.get("mode");

                if (id == null || id.isBlank()) {
                    problems.add("%s:%d — doc-example marker is missing an id".formatted(rel, i + 1));
                } else if (idToFile.containsKey(id)) {
                    problems.add("%s:%d — duplicate doc-example id '%s' (also in %s)"
                            .formatted(rel, i + 1, id, idToFile.get(id)));
                } else {
                    idToFile.put(id, rel + ":" + (i + 1));
                }

                if (mode == null || !SUPPORTED_MODES.contains(mode)) {
                    problems.add("%s:%d — doc-example '%s' has unsupported mode '%s' (use %s)"
                            .formatted(rel, i + 1, id, mode, SUPPORTED_MODES));
                }

                if (fenceAfter(lines, i) == null) {
                    problems.add("%s:%d — doc-example '%s' is not followed by a java fence"
                            .formatted(rel, i + 1, id));
                }
            }
        }

        assertThat(problems)
                .describedAs("doc-example markers must be well-formed (unique id, supported mode, followed by a java fence)")
                .isEmpty();
    }

    private List<Example> collectExamples() throws IOException {
        List<Example> examples = new ArrayList<>();
        for (Path doc : markdownFiles()) {
            List<String> lines = Files.readAllLines(doc, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                Matcher marker = MARKER.matcher(lines.get(i).trim());
                if (!marker.matches()) {
                    continue;
                }
                Map<String, String> attributes = parseAttributes(marker.group(1));
                String id = attributes.get("id");
                String mode = attributes.get("mode");
                if (id == null || id.isBlank() || mode == null || !SUPPORTED_MODES.contains(mode)) {
                    continue; // structural problems are reported by docExampleMarkersShouldBeWellFormed
                }
                String fence = fenceAfter(lines, i);
                if (fence != null) {
                    examples.add(new Example(id, mode, fence, doc));
                }
            }
        }
        return examples;
    }

    private List<Path> markdownFiles() throws IOException {
        if (!Files.isDirectory(DOCS_ROOT)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(DOCS_ROOT)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .sorted()
                    .toList();
        }
    }

    /** Returns the body of the next {@code java} fence after {@code markerIndex}, or null. */
    private static String fenceAfter(List<String> lines, int markerIndex) {
        int i = markerIndex + 1;
        while (i < lines.size() && lines.get(i).isBlank()) {
            i++;
        }
        if (i >= lines.size() || !lines.get(i).trim().startsWith("```java")) {
            return null;
        }
        StringBuilder body = new StringBuilder();
        for (int j = i + 1; j < lines.size(); j++) {
            if (lines.get(j).trim().equals("```")) {
                return body.toString();
            }
            body.append(lines.get(j)).append('\n');
        }
        return null; // unterminated fence
    }

    private static Map<String, String> parseAttributes(String raw) {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (String token : raw.trim().split("\\s+")) {
            int eq = token.indexOf('=');
            if (eq > 0) {
                attributes.put(token.substring(0, eq), token.substring(eq + 1));
            }
        }
        return attributes;
    }

    private static Example exampleFor(Diagnostic<? extends JavaFileObject> diagnostic, Map<String, Example> byUnitName) {
        JavaFileObject source = diagnostic.getSource();
        if (source == null) {
            return null;
        }
        String name = source.getName();
        for (Map.Entry<String, Example> entry : byUnitName.entrySet()) {
            if (name.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }

    private static void deleteRecursively(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best-effort temp cleanup
                }
            });
        } catch (IOException ignored) {
            // best-effort temp cleanup
        }
    }

    private record Example(String id, String mode, String fence, Path file) {

        String unitName() {
            if (mode.equals("class")) {
                Matcher typeName = TYPE_NAME.matcher(fence);
                if (typeName.find()) {
                    return typeName.group(1);
                }
            }
            return "DocExample_" + id.replaceAll("[^A-Za-z0-9]", "_");
        }

        String toCompilationUnit() {
            if (mode.equals("class")) {
                return fence;
            }

            List<String> imports = new ArrayList<>();
            StringBuilder body = new StringBuilder();
            for (String line : fence.split("\\n", -1)) {
                if (IMPORT_LINE.matcher(line).matches()) {
                    imports.add(line.trim());
                } else {
                    body.append(line).append('\n');
                }
            }

            StringBuilder unit = new StringBuilder();
            for (String anImport : imports) {
                unit.append(anImport).append('\n');
            }
            unit.append('\n');
            unit.append("public final class ").append(unitName()).append(" {\n");
            if (mode.equals("method")) {
                unit.append("    @SuppressWarnings({\"unused\", \"try\"})\n");
                unit.append("    void __example() throws Exception {\n");
                unit.append(body);
                unit.append("    }\n");
            } else { // members
                unit.append(body);
            }
            unit.append("}\n");
            return unit.toString();
        }
    }

    private static final class StringSource extends SimpleJavaFileObject {
        private final String code;

        StringSource(String unitName, String code) {
            super(URI.create("string:///" + unitName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
