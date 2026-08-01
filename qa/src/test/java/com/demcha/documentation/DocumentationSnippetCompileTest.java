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
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compiles the Java snippets published in {@code docs/} and in the READMEs so that
 * an API change which breaks a documented snippet fails the build instead of
 * silently rotting the public docs.
 *
 * <p>The READMEs are in scope because they are what a reader compiles first: the
 * root page is the landing copy-paste, and each module page is the answer to "how do
 * I use this artefact". A snippet there that no longer compiles is read by more
 * people than any page under {@code docs/}.</p>
 *
 * <p>Complements {@code DocumentationExamplesTest} (which renders hand-kept Java
 * copies of representative examples): this guard reads the literal markdown
 * fences, so the published page itself cannot drift from the API.
 *
 * <p>Only a fenced {@code java} block immediately preceded by an invisible marker
 * comment is compiled —
 *
 * <pre>{@code
 * <!-- doc-example: id=first-document-smallest mode=method -->
 * ```java
 * ...
 * ```
 * }</pre>
 *
 * The marker is an HTML comment, so it renders to nothing on GitHub and keeps
 * the published page clean.
 *
 * <p>Under {@code docs/} that is <strong>opt-in</strong>: those pages teach with
 * deliberate fragments referencing symbols defined only in prose (a bare
 * {@code invoice} variable, pseudo-code), and marking them would be all false
 * positives. In a README it is <strong>mandatory</strong> — every Java fence carries
 * either a {@code doc-example} marker or {@code <!-- doc-example-ignore: reason -->},
 * because an unmarked fence there is indistinguishable from a covered one and the
 * blocks a reader copies first would rot behind a green build.
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
 *   <dt>{@code imports=a.b.C,d.e.F}</dt><dd>optional; imports added to the
 *   compilation unit without appearing on the page. A short module-README taste
 *   block is three lines of API and would be doubled in length by the imports it
 *   needs, so the guard would in practice only ever cover the long snippets. The
 *   attribute is verified by the compile itself: a name that does not resolve is a
 *   failure like any other.</dd>
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
    /** Exempts the fence below it, and says why in the same breath. */
    private static final Pattern IGNORE_MARKER =
            Pattern.compile("^<!--\\s*doc-example-ignore:\\s*(\\S.*?)\\s*-->\\s*$");
    /** The same marker with the reason left out — recognised only to reject it by name. */
    private static final Pattern REASONLESS_IGNORE_MARKER =
            Pattern.compile("^<!--\\s*doc-example-ignore:\\s*-->\\s*$");
    private static final Set<String> SUPPORTED_MODES = Set.of("method", "members");
    private static final Set<String> SUPPORTED_ATTRIBUTES = Set.of("id", "mode", "imports");

    @Test
    void publishedJavaSnippetsShouldCompile() throws IOException {
        List<Example> examples = collectExamples();

        // The scan must find work — a silent zero would let the guard pass while
        // covering nothing (e.g. a moved docs folder or a broken marker regex).
        assertThat(examples)
                .describedAs("No doc-example markers found under %s — the guard would cover nothing", DOCS_ROOT)
                .isNotEmpty();

        assertThat(compile(examples))
                .describedAs("Every marked Java snippet under docs/ and in the READMEs must "
                        + "compile against the current API")
                .isEmpty();
    }

    /**
     * Every Java fence in a README is either compiled or exempt with a stated reason.
     *
     * <p>Opt-in is the right default for {@code docs/}, where a page teaches with
     * deliberate fragments. It is the wrong one for a README: the pages are short, the
     * snippets are the install-and-use path, and an unmarked fence is indistinguishable
     * from a covered one — the guard reports green while the block a reader is most
     * likely to copy rots untouched. So a README fence must carry either
     * {@code <!-- doc-example: … -->} or {@code <!-- doc-example-ignore: <reason> -->},
     * and the reason is mandatory: an exemption nobody had to justify is opt-in again
     * with extra steps.</p>
     */
    @Test
    void everyJavaFenceInAReadmeIsCompiledOrExemptWithAReason() throws IOException {
        List<String> unaccounted = new ArrayList<>();
        for (Path readme : readmeFiles()) {
            List<String> lines = Files.readAllLines(readme, StandardCharsets.UTF_8);
            String rel = relative(readme);
            for (int i = 0; i < lines.size(); i++) {
                if (!JAVA_FENCE.matcher(lines.get(i).trim()).matches()) {
                    continue;
                }
                if (markerAbove(lines, i) == null) {
                    unaccounted.add("%s:%d".formatted(rel, i + 1));
                }
            }
        }

        assertThat(unaccounted)
                .describedAs("a java fence in a README must be compiled (doc-example) or carry "
                        + "doc-example-ignore with the reason it cannot be — silence reads as "
                        + "coverage and is how a rotting snippet stays published")
                .isEmpty();
    }

    /**
     * Both roots keep contributing compiled snippets.
     *
     * <p>The two are reached differently and can be lost independently, and the overall
     * non-empty check cannot see it: one root's snippets satisfy it on their own while
     * the other falls to zero.</p>
     */
    @Test
    void bothDocumentationRootsContributeCompiledSnippets() throws IOException {
        Set<String> readmes = new TreeSet<>();
        for (Path readme : readmeFiles()) {
            readmes.add(relative(readme));
        }

        Set<String> coveredReadmes = new TreeSet<>();
        Set<String> coveredDocs = new TreeSet<>();
        for (Example example : collectExamples()) {
            String rel = relative(example.file());
            (readmes.contains(rel) ? coveredReadmes : coveredDocs).add(rel);
        }

        assertThat(coveredReadmes)
                .describedAs("the README snippets are the ones a reader compiles first")
                .contains("README.md")
                .anySatisfy(path -> assertThat(path)
                        .describedAs("a module README must be covered, not only the root")
                        .contains("/"));
        assertThat(coveredDocs)
                .describedAs("the docs tree must still contribute compiled snippets; the READMEs "
                        + "alone satisfy the overall non-empty check, so docs/ can fall to zero "
                        + "behind a green build")
                .isNotEmpty();
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

                // Whatever the attribute parser cannot read, it drops without a word.
                // A misspelled `import=` takes its whole import list with it, and a
                // space after a comma splits one list into a value and a stray token —
                // both surface far away, as an unresolved symbol inside the snippet.
                for (String token : marker.group(1).trim().split("\\s+")) {
                    if (token.indexOf('=') <= 0) {
                        problems.add(("%s:%d — doc-example '%s' has a stray token '%s'; an "
                                + "attribute is name=value and its value may not contain a space")
                                .formatted(rel, i + 1, id, token));
                    }
                }
                for (String attribute : attributes.keySet()) {
                    if (!SUPPORTED_ATTRIBUTES.contains(attribute)) {
                        problems.add("%s:%d — doc-example '%s' has unknown attribute '%s' (use %s)"
                                .formatted(rel, i + 1, id, attribute, SUPPORTED_ATTRIBUTES));
                    }
                }

                if (fenceAfter(lines, i) == null) {
                    problems.add("%s:%d — doc-example '%s' is not followed by a java fence"
                            .formatted(rel, i + 1, id));
                }
            }

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                // An exemption that introduces nothing is a leftover: the fence it excused
                // has moved or gone, and the next one to land under it inherits the excuse.
                if (IGNORE_MARKER.matcher(line).matches() && fenceAfter(lines, i) == null) {
                    problems.add("%s:%d — doc-example-ignore is not followed by a java fence"
                            .formatted(rel, i + 1));
                }
                if (REASONLESS_IGNORE_MARKER.matcher(line).matches()) {
                    problems.add(("%s:%d — doc-example-ignore carries no reason; the reason is "
                            + "what separates a considered exemption from opt-in with extra steps")
                            .formatted(rel, i + 1));
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
                    examples.add(new Example(id, mode, fence, doc, hiddenImports(attributes)));
                }
            }
        }
        return examples;
    }

    /** Every page a reader lands on: the docs tree plus the root and module READMEs. */
    private List<Path> markdownFiles() throws IOException {
        return PublishedDocs.all(PROJECT_ROOT);
    }

    /** The root README plus one per Maven module. */
    private List<Path> readmeFiles() throws IOException {
        return PublishedDocs.readmes(PROJECT_ROOT);
    }

    /**
     * The {@code doc-example} or {@code doc-example-ignore} marker introducing the fence
     * at {@code fenceIndex}, or null when the fence carries neither. Blank lines between
     * the two are allowed; anything else ends the search, so a marker further up the
     * page cannot be mistaken for this fence's.
     */
    private static String markerAbove(List<String> lines, int fenceIndex) {
        for (int i = fenceIndex - 1; i >= 0; i--) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (MARKER.matcher(line).matches() || IGNORE_MARKER.matcher(line).matches()) {
                return line;
            }
            return null;
        }
        return null;
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

    /** The comma-separated {@code imports=} attribute, or an empty list when absent. */
    private static List<String> hiddenImports(Map<String, String> attributes) {
        String raw = attributes.get("imports");
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Stream.of(raw.split(","))
                .map(String::trim)
                .filter(type -> !type.isEmpty())
                .toList();
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

    private record Example(String id, String mode, String fence, Path file, List<String> hiddenImports) {

        Example(String id, String mode, String fence, Path file) {
            this(id, mode, fence, file, List.of());
        }

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
            for (String type : hiddenImports) {
                imports.add("import " + type + ";");
            }
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
