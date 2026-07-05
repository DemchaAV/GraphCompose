package com.demcha.documentation;
import com.demcha.compose.qa.RepoPaths;

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
 * <em>errors</em> fail the test; warnings are ignored. Leading {@code import}
 * lines are lifted above the wrapper.
 *
 * <dl>
 *   <dt>{@code mode=method}</dt><dd>statements; wrapped in a {@code void} method
 *   that {@code throws Exception}.</dd>
 *   <dt>{@code mode=members}</dt><dd>field/method declarations; inserted as class
 *   members.</dd>
 * </dl>
 *
 * <p>The guard self-tests both directions: {@link #compilerReportsErrorForBrokenSnippet()}
 * proves a broken snippet is actually surfaced as a failure, and
 * {@link #knownCanonicalTypeResolvesOnTestClasspath()} proves the compile classpath
 * resolves canonical types — so a classpath regression is distinguishable from a
 * real doc break.
 */
class DocumentationSnippetCompileTest {

    private static final Path PROJECT_ROOT = RepoPaths.repoRoot();
    private static final Path DOCS_ROOT = PROJECT_ROOT.resolve("docs");

    private static final Pattern MARKER =
            Pattern.compile("^<!--\\s*doc-example:\\s*(.+?)\\s*-->\\s*$");
    private static final Pattern IMPORT_LINE =
            Pattern.compile("^\\s*import\\s+(?:static\\s+)?[\\w.]+(?:\\.\\*)?\\s*;\\s*$");
    private static final Pattern JAVA_FENCE =
            Pattern.compile("^```java\\s*$");
    private static final Set<String> SUPPORTED_MODES = Set.of("method", "members");

    @Test
    void publishedJavaSnippetsShouldCompile() throws IOException {
        List<Example> examples = collectExamples();

        // The scan must find work — a silent zero would let the guard pass while
        // covering nothing (e.g. a moved docs folder or a broken marker regex).
        assertThat(examples)
                .describedAs("No doc-example markers found under %s — the guard would cover nothing", DOCS_ROOT)
                .isNotEmpty();

        assertThat(compile(examples))
                .describedAs("Every marked Java snippet under docs/ must compile against the current API")
                .isEmpty();
    }

    @Test
    void compilerReportsErrorForBrokenSnippet() throws IOException {
        // Drives the full mechanism (wrap -> compile -> collect -> attribute) on a
        // snippet that references a symbol that does not exist. Proves the guard
        // actually fails — and names the offending snippet — instead of passing
        // vacuously if any stage regressed.
        Example broken = new Example(
                "synthetic-broken-snippet", "method",
                "thisMethodDoesNotExistOnAnyType();\n",
                PROJECT_ROOT.resolve("docs/(synthetic).md"));

        List<String> errors = compile(List.of(broken));

        assertThat(errors)
                .describedAs("A snippet referencing a missing symbol must be reported as an error")
                .isNotEmpty()
                .allSatisfy(error -> assertThat(error).contains("synthetic-broken-snippet"));
    }

    @Test
    void knownCanonicalTypeResolvesOnTestClasspath() throws IOException {
        // A trivial snippet that imports and calls a known canonical type. If this
        // fails, the compile classpath is not resolving the library — a classpath /
        // Surefire booter problem, NOT a documentation defect. Keeping it separate
        // makes that distinction unambiguous when the suite goes red.
        Example probe = new Example(
                "synthetic-classpath-probe", "method",
                "import com.demcha.compose.GraphCompose;\nGraphCompose.document();\n",
                PROJECT_ROOT.resolve("docs/(synthetic).md"));

        assertThat(compile(List.of(probe)))
                .describedAs("A known canonical type must resolve on the test classpath; "
                        + "a failure here is a classpath problem, not a docs problem")
                .isEmpty();
    }

    @Test
    void docExampleMarkersShouldBeWellFormed() throws IOException {
        List<String> problems = new ArrayList<>();
        Map<String, String> idToFile = new LinkedHashMap<>();
        Map<String, String> unitNameToId = new LinkedHashMap<>();

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
                    String unitName = Example.unitNameFor(id);
                    String clash = unitNameToId.put(unitName, id);
                    if (clash != null) {
                        problems.add("%s:%d — doc-example id '%s' sanitizes to the same unit name as '%s'"
                                .formatted(rel, i + 1, id, clash));
                    }
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
                .describedAs("doc-example markers must be well-formed (unique id + unit name, supported mode, followed by a java fence)")
                .isEmpty();
    }

    /** Compiles the given examples in-memory and returns one string per compiler error, attributed to its example. */
    private static List<String> compile(List<Example> examples) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler)
                .describedAs("A JDK compiler is required to compile doc snippets; run the build on a JDK, not a JRE")
                .isNotNull();

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);

        Map<JavaFileObject, Example> bySource = new LinkedHashMap<>();
        List<JavaFileObject> units = new ArrayList<>();
        for (Example example : examples) {
            JavaFileObject source = new StringSource(example.unitName(), example.toCompilationUnit());
            bySource.put(source, example);
            units.add(source);
        }

        Path classOutput = null;
        try {
            classOutput = Files.createTempDirectory("doc-snippets-classes");
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classOutput.toFile()));
            List<String> options = List.of(
                    "-proc:none",
                    "-classpath", System.getProperty("java.class.path"));
            compiler.getTask(null, fileManager, diagnostics, options, null, units).call();
        } finally {
            fileManager.close();
            if (classOutput != null) {
                deleteRecursively(classOutput);
            }
        }

        List<String> errors = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            Example example = bySource.get(diagnostic.getSource());
            String origin = example == null
                    ? "(unknown unit)"
                    : example.id + " — " + relative(example.file);
            errors.add("[%s] %s".formatted(origin, diagnostic.getMessage(null).replaceAll("\\s+", " ").trim()));
        }
        return errors;
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
        if (i >= lines.size() || !JAVA_FENCE.matcher(lines.get(i).trim()).matches()) {
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

        static String unitNameFor(String id) {
            return "DocExample_" + id.replaceAll("[^A-Za-z0-9]", "_");
        }

        String unitName() {
            return unitNameFor(id);
        }

        String toCompilationUnit() {
            // Lift only the leading run of import lines; an import-shaped line that
            // appears after real code (e.g. inside a text block) stays in the body.
            List<String> imports = new ArrayList<>();
            StringBuilder body = new StringBuilder();
            boolean inBody = false;
            for (String line : fence.split("\\n", -1)) {
                if (!inBody && IMPORT_LINE.matcher(line).matches()) {
                    imports.add(line.trim());
                    continue;
                }
                if (!inBody && !line.isBlank()) {
                    inBody = true;
                }
                body.append(line).append('\n');
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
