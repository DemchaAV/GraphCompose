package com.demcha.documentation;

import com.demcha.compose.qa.RepoPaths;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Holds the DOCX column of the backend capability matrix to the code it describes.
 *
 * <p>{@code docs/architecture/backend-capability-matrix.md} lists every subject a
 * {@code DocxExportReport} note can carry, with its severity and the capability row it belongs to,
 * and names the class behind each DOCX cell. Both are written by hand, in the same change as the
 * code.</p>
 *
 * <p>This reads the export's own sources. Every note is added with its severity written out
 * ({@code Severity.DROPPED} or {@code Severity.APPROXIMATED}), followed by its subject; the subject
 * is resolved to the text the report carries — a string, a constant, a local its method assigns
 * once, a method's returns, a record a subject is handed through — and a node's kind, which is the
 * node's own, stands as {@code <kind>}, as the table writes it. An expression it cannot resolve
 * fails the guard rather than being skipped — a parameter, a local assigned again, an accessor it
 * does not know — and so does a severity not written out where the note is added: a subject this
 * cannot read is one the table cannot be held to.</p>
 */
class DocxCapabilityMatrixGuardTest {

    private static final Path ROOT = RepoPaths.repoRoot();
    private static final Path MATRIX = ROOT.resolve("docs/architecture/backend-capability-matrix.md");
    private static final Path EXPORT = ROOT.resolve(
            "render-docx/src/main/java/com/demcha/compose/document/backend/semantic/docx");

    /** A node's kind: the report names the node's own ({@code ParagraphNode}, {@code TableNode}). */
    private static final String KIND = "<kind>";

    /**
     * Subjects handed through a record, by the file and the variable that read them: the record
     * the variable holds, by the class that declares it. A lambda's parameter states no type the
     * guard could read, so each is named here; one this does not name fails the guard, and so does
     * a variable whose method declares it of another type.
     */
    private static final Map<String, String> CARRIED = Map.of(
            "DocxSemanticBackend.java owner", "DocxDrawingAnchors.Owner",
            "DocxSemanticBackend.java drawingFor", "DocxDrawingAnchors.Owner",
            "DocxSemanticBackend.java shape", "DocxSemanticBackend.DrawnBehind",
            "DocxSemanticBackend.java block", "DocxSemanticBackend.UnmarkedBlock",
            "DocxSemanticBackend.java reference", "DocxSemanticBackend.FieldReference");

    /** Expressions whose value is open-ended, as the table writes them. */
    private static final Map<String, String> OPEN = Map.of(
            "run.getClass().getSimpleName()", "<run kind>");

    /** Phrases in the table's Row column that point elsewhere than a row. */
    private static final Set<String> NOT_A_ROW = Set.of(
            "the node's own row",
            "the DOCX backend, at the top of this file");

    /** What the export's sources write, read once for the tests that compare it. */
    private static Map<String, Set<String>> exportWritten;

    private static synchronized Map<String, Set<String>> exportWritten() throws IOException {
        if (exportWritten == null) {
            List<Source> sources = exportSources();
            assertThat(sources.stream().filter(source -> REPORT_BUILDER.matcher(source.code()).find()))
                    .describedAs("no source holds a DocxExportReport.Builder: the check that every call "
                            + "handing it a note writes its severity out is reading nothing")
                    .isNotEmpty();
            exportWritten = written(sources);
        }
        return exportWritten;
    }

    @Test
    void everySubjectTheExportWritesIsListedWithItsSeverity() throws IOException {
        Map<String, Set<String>> written = exportWritten();
        Map<String, Set<String>> listed = listed(Files.readString(MATRIX));

        assertThat(written.keySet())
                .describedAs("found too few subjects in the export's sources: the scan is not "
                        + "reading them, and the guard would pass on nothing")
                .hasSizeGreaterThan(30);
        assertThat(missing(written, listed))
                .describedAs("the export writes these report subjects (severity) and the "
                        + "'DOCX export report subjects' table of backend-capability-matrix.md "
                        + "does not list them so: add a row saying what the file holds instead")
                .isEmpty();
    }

    @Test
    void everySubjectTheMatrixListsIsOneTheExportWrites() throws IOException {
        Map<String, Set<String>> written = exportWritten();
        Map<String, Set<String>> listed = listed(Files.readString(MATRIX));

        assertThat(listed.keySet())
                .describedAs("found too few subjects in the matrix's table: the section moved, and "
                        + "the guard would pass on nothing")
                .hasSizeGreaterThan(30);
        assertThat(missing(listed, written))
                .describedAs("backend-capability-matrix.md lists these report subjects (severity) "
                        + "and the export writes none so: the row describes a note that is never given")
                .isEmpty();
    }

    @Test
    void everyRowTheSubjectsTableNamesIsARowOfTheMatrix() throws IOException {
        String matrix = Files.readString(MATRIX);
        List<String> capabilities = capabilityNames(matrix);
        Set<String> unknown = new TreeSet<>();
        int named = 0;
        for (List<String> row : subjectRows(matrix)) {
            for (String segment : row.get(3).split(";")) {
                String phrase = segment.strip();
                if (NOT_A_ROW.contains(phrase)) {
                    continue;
                }
                for (String name : phrase.split(",")) {
                    String capability = name.strip();
                    named++;
                    if (capabilities.stream().noneMatch(cell -> namesRow(cell, capability))) {
                        unknown.add(capability + " (for " + row.get(0) + ")");
                    }
                }
            }
        }
        assertThat(named).isGreaterThan(30);
        assertThat(unknown)
                .describedAs("the Row column names capabilities the matrix has no row for")
                .isEmpty();
    }

    @Test
    void everyDocxClassTheMatrixNamesExists() throws IOException {
        String matrix = Files.readString(MATRIX);
        Map<String, Path> types = declaredTypes();
        Map<Path, String> read = new HashMap<>();
        Pattern symbol = Pattern.compile("`(Docx\\w+)(?:\\.(\\w+))?(?:\\([^`]*\\))?`");
        Set<String> unknown = new TreeSet<>();
        int named = 0;
        for (String cell : docxCells(matrix)) {
            Matcher found = symbol.matcher(cell);
            while (found.find()) {
                named++;
                Path declaring = types.get(found.group(1));
                if (declaring == null) {
                    unknown.add(found.group(1));
                    continue;
                }
                if (found.group(2) == null) {
                    continue;
                }
                String code = read.computeIfAbsent(declaring, DocxCapabilityMatrixGuardTest::readWithoutComments);
                // A declaration: a return type before the name, not a call's receiver.
                Pattern declared = Pattern.compile("(?<![\\w.])(?!(?:return|new|else|throw)\\b)"
                        + "[A-Za-z_][\\w<>\\[\\],.?]*\\s+" + found.group(2) + "\\s*\\(");
                if (!declared.matcher(code).find()) {
                    unknown.add(found.group(1) + "." + found.group(2));
                }
            }
        }
        assertThat(named).isGreaterThan(30);
        assertThat(unknown)
                .describedAs("the DOCX column names classes or methods no source declares")
                .isEmpty();
    }

    // The resolver, held to what it claims on sources small enough to read.

    @Test
    void resolvesALiteralAConstantATernaryAndANodesKind() {
        Map<String, Set<String>> written = written(List.of(source("A", """
                class A {
                    static final String EDGE = "left edge";
                    void f() {
                        report.add(Severity.APPROXIMATED, "chart", path, "a");
                        report.add(DocxExportReport.Severity.DROPPED, EDGE, path, "b");
                        report.add(Severity.APPROXIMATED, x ? "page zone" : node.nodeKind(), path, "c");
                        report.add(Severity.DROPPED, "clipped " + node.nodeKind(), path, "d");
                    }
                }
                """)));
        assertThat(written).isEqualTo(Map.of(
                "chart", Set.of("APPROXIMATED"),
                "left edge", Set.of("DROPPED"),
                "page zone", Set.of("APPROXIMATED"),
                KIND, Set.of("APPROXIMATED"),
                "clipped <kind>", Set.of("DROPPED")));
    }

    @Test
    void followsALocalAMethodsReturnsAndASubjectARecordCarries() {
        Map<String, Set<String>> written = written(List.of(source("DocxDrawingAnchors.java", """
                class DocxDrawingAnchors {
                    record Owner(String subject, String path) {
                        Owner copy() { return new Owner(subject, path); }
                    }
                    void f() {
                        new Owner("timeline rail", p);
                    }
                }
                """), source("DocxSemanticBackend.java", """
                class DocxSemanticBackend {
                    static String side(boolean head) { String kept = "unread"; return head ? "header" : "footer"; }
                    void e() { String subject = "from another method"; }
                    void f() {
                        String subject = "page " + side(true);
                        report.add(Severity.APPROXIMATED, subject, path, "a");
                        leftovers.forEach((owner, whole) -> report.add(Severity.DROPPED, owner.subject(), path, "b"));
                    }
                }
                """), source("DocxPageMarginSections.java", """
                class DocxPageMarginSections {
                    record Owner(String subject, int page) { }
                    void g() {
                        new DocxDrawingAnchors.Owner(node.nodeKind(), p);
                        new Owner("another record of the same name", 0);
                    }
                }
                """)));
        assertThat(written).isEqualTo(Map.of(
                "page header", Set.of("APPROXIMATED"),
                "page footer", Set.of("APPROXIMATED"),
                "timeline rail", Set.of("DROPPED"),
                KIND, Set.of("DROPPED")));
    }

    @Test
    void aNoteInACommentIsNotOneTheExportWrites() {
        Map<String, Set<String>> written = written(List.of(source("A", """
                class A {
                    // report.add(Severity.DROPPED, "ghost", path, "commented out");
                    /* report.add(Severity.DROPPED, "phantom", path, "and this"); */
                    void f() { report.add(Severity.DROPPED, "watermark", null, "a // not a comment"); }
                }
                """)));
        assertThat(written).isEqualTo(Map.of("watermark", Set.of("DROPPED")));
    }

    @Test
    void aSubjectItCannotReadFailsTheGuard() {
        assertThatThrownBy(() -> written(List.of(source("A", """
                class A { void f() { report.add(Severity.DROPPED, describe(node), path, "a"); } }
                """)))).isInstanceOf(AssertionError.class).hasMessageContaining("describe(node)");
        assertThatThrownBy(() -> written(List.of(source("A", """
                class A { void f() { report.add(Severity.DROPPED, stranger.subject(), path, "a"); } }
                """)))).isInstanceOf(AssertionError.class).hasMessageContaining("stranger.subject()");
    }

    @Test
    void aLocalItsMethodDoesNotAssignOnceFailsTheGuard() {
        // A parameter, though an earlier method assigns a local of the same name.
        assertThatThrownBy(() -> written(List.of(source("A", """
                class A {
                    void e() { String subject = "from another method"; }
                    void f(String subject) { report.add(Severity.DROPPED, subject, path, "a"); }
                }
                """)))).isInstanceOf(AssertionError.class).hasMessageContaining("a parameter");
        assertThatThrownBy(() -> written(List.of(source("A", """
                class A {
                    void f() {
                        String subject = "a";
                        subject = "b";
                        report.add(Severity.DROPPED, subject, path, "a");
                    }
                }
                """)))).isInstanceOf(AssertionError.class).hasMessageContaining("assigned again");
        assertThatThrownBy(() -> written(List.of(source("A", """
                class A {
                    void e() { String subject = "from another method"; }
                    void f() { report.add(Severity.DROPPED, subject, path, "a"); }
                }
                """)))).isInstanceOf(AssertionError.class).hasMessageContaining("assigns no String local");
        assertThatThrownBy(() -> written(List.of(source("A", """
                class A {
                    void f() {
                        String subject = "a";
                        subjects.forEach(subject -> report.add(Severity.DROPPED, subject, path, "a"));
                    }
                }
                """)))).isInstanceOf(AssertionError.class).hasMessageContaining("a lambda's parameter");
    }

    @Test
    void aCallItsMethodsReturnsDoNotGiveFailsTheGuard() {
        assertThatThrownBy(() -> written(List.of(source("A", """
                class A {
                    static String side(boolean head) { return head ? "header" : "footer"; }
                    void f() { report.add(Severity.DROPPED, side(true).toUpperCase(), path, "a"); }
                }
                """)))).isInstanceOf(AssertionError.class).hasMessageContaining("toUpperCase");
    }

    @Test
    void aCarriedSubjectReadThroughAnotherTypeFailsTheGuard() {
        assertThatThrownBy(() -> written(List.of(source("DocxSemanticBackend.java", """
                class DocxSemanticBackend {
                    record DrawnBehind(String subject) { }
                    void f() {
                        new DrawnBehind("drawing");
                        for (Badge shape : badges) { report.add(Severity.DROPPED, shape.subject(), path, "a"); }
                    }
                }
                """)))).isInstanceOf(AssertionError.class).hasMessageContaining("Badge");
    }

    @Test
    void aSeverityChosenAtRunTimeFailsTheGuard() {
        assertThatThrownBy(() -> written(List.of(source("A", """
                class A { void f(Severity severity) { report.add(severity, "chart", path, "a"); } }
                """)))).isInstanceOf(AssertionError.class).hasMessageContaining("severity");
        assertThatThrownBy(() -> written(List.of(source("A", """
                import static com.demcha.compose.document.backend.semantic.docx.DocxExportReport.Severity.DROPPED;
                class A { void f() { report.add(DROPPED, "chart", path, "a"); } }
                """)))).isInstanceOf(AssertionError.class).hasMessageContaining("A:1");
        // A note passed on with its own severity: no Severity token in the call to be found by.
        assertThatThrownBy(() -> written(List.of(source("A", """
                class A {
                    private DocxExportReport.Builder report;
                    void f(DocxExportReport.Note note) { report.add(note.severity(), "chart", path, "a"); }
                }
                """)))).isInstanceOf(AssertionError.class).hasMessageContaining("without Severity.DROPPED");
    }

    @Test
    void aNoteTheReportAddsItselfFailsTheGuard() {
        assertThatThrownBy(() -> written(List.of(source("DocxExportReport.java", """
                class DocxExportReport {
                    void dropped(String subject) { add(Severity.DROPPED, subject, null, ""); }
                }
                """)))).isInstanceOf(AssertionError.class).hasMessageContaining("adds a note of its own");
    }

    // Reading the export.

    private record Source(String name, String code) {
        int line(int offset) {
            return (int) code.substring(0, offset).chars().filter(c -> c == '\n').count() + 1;
        }
    }

    private static Source source(String name, String text) {
        return new Source(name, withoutComments(text));
    }

    private static List<Source> exportSources() throws IOException {
        List<Source> sources = new ArrayList<>();
        try (Stream<Path> files = Files.walk(EXPORT)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).sorted().toList()) {
                sources.add(source(file.getFileName().toString(), Files.readString(file)));
            }
        }
        assertThat(sources).describedAs("no sources under " + EXPORT).isNotEmpty();
        return sources;
    }

    private static final Pattern SEVERITY = Pattern.compile("\\bSeverity\\b");
    private static final Pattern SITE = Pattern.compile("\\bSeverity\\.(DROPPED|APPROXIMATED)\\s*,");

    /** Subject to the severities the sources add it with. */
    static Map<String, Set<String>> written(List<Source> sources) {
        Map<String, Set<String>> written = new TreeMap<>();
        for (Source source : sources) {
            if (source.name().equals("DocxExportReport.java")) {
                // It declares the severities and takes them as parameters; a note it added itself,
                // for its callers to name only a subject, would hide every one of them.
                Matcher own = SITE.matcher(source.code());
                if (own.find()) {
                    throw new AssertionError("DocxExportReport.java:" + source.line(own.start())
                            + " adds a note of its own: its callers' subjects are not read");
                }
                continue;
            }
            requireASeverityInEveryNoteCall(source);
            Matcher token = SEVERITY.matcher(source.code());
            while (token.find()) {
                Matcher site = SITE.matcher(source.code()).region(token.start(), source.code().length());
                if (!site.lookingAt()) {
                    String line = lineAt(source.code(), token.start()).strip();
                    if (line.startsWith("import ") && !line.startsWith("import static ")) {
                        continue;
                    }
                    throw new AssertionError(source.name() + ":" + source.line(token.start())
                            + " uses a severity the guard cannot read where a note is added: write "
                            + "Severity.DROPPED or Severity.APPROXIMATED in the call");
                }
                String subject = argument(source.code(), site.end());
                for (String value : resolve(subject, source, site.end(), sources, 0)) {
                    written.computeIfAbsent(value, v -> new TreeSet<>()).add(site.group(1));
                }
            }
        }
        return written;
    }

    private static final Pattern REPORT_BUILDER = Pattern.compile("\\bDocxExportReport\\.Builder\\s+(\\w+)\\s*[,;=)]");

    /**
     * Every call that hands the report builder a note — through any name a source holds it under —
     * writes its severity out. One that does not (a note's own {@code severity()} passed on, a
     * severity in a variable) has no {@code Severity} token to be found by, and would go unread.
     */
    private static void requireASeverityInEveryNoteCall(Source source) {
        Set<String> names = new TreeSet<>();
        Matcher held = REPORT_BUILDER.matcher(source.code());
        while (held.find()) {
            names.add(held.group(1));
        }
        for (String name : names) {
            Matcher call = Pattern.compile("(?<!\\w)" + name + "\\s*\\.\\s*(?:add|set|insert|replace|addTo)\\s*\\(")
                    .matcher(source.code());
            while (call.find()) {
                if (!SITE.matcher(argumentList(source.code(), call.end())).find()) {
                    throw new AssertionError(source.name() + ":" + source.line(call.start())
                            + " hands the report a note without Severity.DROPPED or Severity.APPROXIMATED "
                            + "written in the call: its subject cannot be read");
                }
            }
        }
    }

    private static Set<String> resolve(String raw, Source source, int at, List<Source> sources, int depth) {
        String expr = unwrap(raw.strip());
        if (depth > 12) {
            throw unreadable(expr, source, at, "it refers to itself");
        }
        int question = topLevel(expr, '?');
        if (question >= 0) {
            String branches = expr.substring(question + 1);
            int colon = matchingColon(branches);
            Set<String> values = new LinkedHashSet<>(resolve(branches.substring(0, colon), source, at, sources, depth + 1));
            values.addAll(resolve(branches.substring(colon + 1), source, at, sources, depth + 1));
            return values;
        }
        List<String> parts = splitTopLevel(expr, '+', false);
        if (parts.size() > 1) {
            Set<String> values = new LinkedHashSet<>(List.of(""));
            for (String part : parts) {
                Set<String> next = new LinkedHashSet<>();
                for (String head : values) {
                    for (String tail : resolve(part, source, at, sources, depth + 1)) {
                        next.add(head + tail);
                    }
                }
                values = next;
            }
            return values;
        }
        if (expr.matches("\"(?:[^\"\\\\]|\\\\.)*\"")) {
            return Set.of(expr.substring(1, expr.length() - 1));
        }
        if (OPEN.containsKey(expr)) {
            return Set.of(OPEN.get(expr));
        }
        if (expr.matches("(?:[\\w.()]*\\.)?nodeKind\\(\\)")) {
            return Set.of(KIND);
        }
        Matcher carried = Pattern.compile("(\\w+)\\.subject\\(\\)").matcher(expr);
        if (carried.matches()) {
            String record = CARRIED.get(source.name() + " " + carried.group(1));
            if (record == null) {
                throw unreadable(expr, source, at, "name the record it reads in CARRIED");
            }
            String declaredAs = declaredType(carried.group(1), source, at);
            // A qualified type names its class too: another class's record of the same name is not it.
            boolean same = declaredAs == null || (declaredAs.contains(".")
                    ? record.equals(declaredAs) || declaredAs.endsWith("." + record)
                    : record.endsWith("." + declaredAs));
            if (!same) {
                throw unreadable(expr, source, at, "its method declares it a " + declaredAs + ", not the "
                        + record + " CARRIED names");
            }
            return carriedBy(record, sources, depth);
        }
        if (expr.matches("[A-Z][A-Z0-9_]*")) {
            for (Source each : callerFirst(source, sources)) {
                Matcher constant = Pattern.compile("\\bstatic\\s+final\\s+String\\s+" + expr + "\\s*=").matcher(each.code());
                if (constant.find()) {
                    return resolve(statement(each.code(), constant.end()), each, constant.end(), sources, depth + 1);
                }
            }
            throw unreadable(expr, source, at, "no source declares that constant");
        }
        if (expr.matches("[a-z]\\w*")) {
            int declared = localAssignment(expr, source, at);
            return resolve(statement(source.code(), declared), source, declared, sources, depth + 1);
        }
        Matcher call = Pattern.compile("(\\w+)\\(", Pattern.DOTALL).matcher(expr);
        if (call.lookingAt() && argumentList(expr, call.end()).length() == expr.length() - call.end() - 1) {
            // Every overload's returns: which one the call reaches is not read.
            for (Source each : callerFirst(source, sources)) {
                Matcher method = Pattern.compile("\\bString\\s+" + call.group(1) + "\\s*\\([^)]*\\)\\s*\\{").matcher(each.code());
                Set<String> values = new LinkedHashSet<>();
                while (method.find()) {
                    int open = method.end() - 1;
                    String body = block(each.code(), open);
                    Matcher returned = Pattern.compile("\\breturn\\b").matcher(body);
                    while (returned.find()) {
                        values.addAll(resolve(statement(body, returned.end()), each, open + returned.end(),
                                sources, depth + 1));
                    }
                }
                if (!values.isEmpty()) {
                    return values;
                }
            }
        }
        throw unreadable(expr, source, at, "the guard does not know what it holds");
    }

    /**
     * The simple type the method around an index last declares a variable of that name as before
     * it, in a declaration, a parameter or an enhanced for; null where it states none, as for a
     * lambda's parameter or a field.
     */
    private static String declaredType(String name, Source source, int at) {
        int[] method = enclosingMethod(source.code(), at);
        if (method == null) {
            return null;
        }
        Matcher typed = Pattern.compile("\\b([A-Z][\\w.]*)(?:<[^<>]*(?:<[^<>]*>[^<>]*)*>)?\\s+" + name + "\\s*[:=;,)]")
                .matcher(source.code()).region(method[0], at);
        String type = null;
        while (typed.find()) {
            type = typed.group(1);
        }
        return type;
    }

    /** The sources with the one an expression stands in first, so its own declarations win. */
    private static List<Source> callerFirst(Source source, List<Source> sources) {
        List<Source> ordered = new ArrayList<>(List.of(source));
        sources.stream().filter(each -> each != source).forEach(ordered::add);
        return ordered;
    }

    /**
     * Where the one {@code String} local of that name in the method around an index is assigned,
     * before the index. A parameter, a lambda's parameter, a local assigned again, or a name the
     * method does not declare fails: what it holds is not in the method to read.
     */
    private static int localAssignment(String name, Source source, int at) {
        int[] method = enclosingMethod(source.code(), at);
        if (method == null) {
            throw unreadable(name, source, at, "it stands in no method the guard can find");
        }
        if (Pattern.compile("\\b" + name + "\\b").matcher(source.code().substring(method[0], method[1])).find()) {
            throw unreadable(name, source, at, "a parameter: what its callers pass is not read");
        }
        String body = source.code().substring(method[2], method[3]);
        int offset = at - method[2];
        if (Pattern.compile("(?:\\b" + name + "|\\([^()]*\\b" + name + "\\b[^()]*\\))\\s*->").matcher(body).find()) {
            throw unreadable(name, source, at, "a lambda's parameter: what it is handed is not read");
        }
        Matcher declaration = Pattern.compile("\\bString\\s+" + name + "\\s*=").matcher(body);
        int declared = -1;
        int declarations = 0;
        while (declaration.find()) {
            declarations++;
            if (declaration.end() <= offset) {
                declared = declaration.end();
            }
        }
        long assignments = Pattern.compile("(?<![\\w.])" + name + "\\s*(?:\\+=|=(?!=))").matcher(body).results().count();
        if (declared < 0) {
            throw unreadable(name, source, at, "the method assigns no String local of that name before it");
        }
        if (assignments > declarations) {
            throw unreadable(name, source, at, "assigned again in its method, which the guard does not follow");
        }
        return method[2] + declared;
    }

    private static final Pattern METHOD = Pattern.compile(
            "(?<![\\w.])(?!(?:if|for|while|switch|catch|synchronized|return|new|else|throw|try|do|record|class|"
            + "interface|enum)\\b)[A-Za-z_][\\w<>\\[\\],.?]*\\s+(?!(?:if|for|while|switch|catch|synchronized)\\b)\\w+\\s*\\(");
    private static final Pattern BODY_OPENS = Pattern.compile("\\s*(?:throws\\s+[\\w.,\\s]+)?\\{");

    /**
     * The innermost method or constructor around an index: its parameters' and its body's bounds,
     * {@code [paramsFrom, paramsTo, bodyFrom, bodyTo]}, or null.
     */
    private static int[] enclosingMethod(String code, int at) {
        int[] found = null;
        Matcher header = METHOD.matcher(code);
        while (header.find() && header.end() <= at) {
            String parameters = argumentList(code, header.end());
            int close = header.end() + parameters.length() + 1;
            Matcher opens = BODY_OPENS.matcher(code).region(close, code.length());
            if (!opens.lookingAt()) {
                continue;
            }
            int bodyFrom = opens.end() - 1;
            int bodyTo = bodyFrom + block(code, bodyFrom).length();
            if (bodyFrom < at && at < bodyTo) {
                found = new int[] {header.end(), header.end() + parameters.length(), bodyFrom, bodyTo};
            }
        }
        return found;
    }

    /**
     * Every subject a record is built with — named bare in the class that declares it, through
     * that class elsewhere — but where its own body copies the one it holds.
     *
     * @param qualified the declaring class and the record, {@code DocxDrawingAnchors.Owner}
     */
    private static Set<String> carriedBy(String qualified, List<Source> sources, int depth) {
        String outer = qualified.substring(0, qualified.indexOf('.'));
        String record = qualified.substring(qualified.indexOf('.') + 1);
        Source source = sources.stream().filter(each -> each.name().equals(outer + ".java")).findFirst()
                .orElseThrow(() -> new AssertionError("no source " + outer + ".java declares " + qualified));
        Matcher header = Pattern.compile("\\brecord\\s+" + record + "\\s*\\(").matcher(source.code());
        if (!header.find()) {
            throw new AssertionError(outer + ".java declares no record " + record);
        }
        String declared = argumentList(source.code(), header.end());
        List<String> components = splitTopLevel(declared, ',', true);
        int index = -1;
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i).strip().matches("String\\s+subject")) {
                index = i;
            }
        }
        if (index < 0) {
            throw new AssertionError("record " + qualified + " carries no String subject");
        }
        int bodyStart = source.code().indexOf('{', header.end() + declared.length());
        int bodyEnd = bodyStart + block(source.code(), bodyStart).length();
        Set<String> values = new LinkedHashSet<>();
        for (Source each : sources) {
            Pattern constructor = Pattern.compile(each == source
                    ? "\\bnew\\s+(?:" + outer + "\\.)?" + record + "\\s*\\("
                    : "\\bnew\\s+(?:[\\w.]*\\.)?" + outer + "\\." + record + "\\s*\\(");
            Matcher built = constructor.matcher(each.code());
            while (built.find()) {
                if (each == source && built.start() > bodyStart && built.start() < bodyEnd) {
                    continue;
                }
                List<String> arguments = splitTopLevel(argumentList(each.code(), built.end()), ',', false);
                values.addAll(resolve(arguments.get(index), each, built.start(), sources, depth + 1));
            }
        }
        if (values.isEmpty()) {
            throw new AssertionError("no source builds " + qualified + ": the guard is not reading its subjects");
        }
        return values;
    }

    private static AssertionError unreadable(String expr, Source source, int at, String why) {
        return new AssertionError("cannot tell which report subject `" + expr + "` at " + source.name() + ":"
                + source.line(Math.min(at, source.code().length())) + " gives: " + why);
    }

    // Reading the matrix.

    /** Subject to the severities the table lists it with. */
    static Map<String, Set<String>> listed(String matrix) {
        Map<String, Set<String>> listed = new TreeMap<>();
        for (List<String> row : subjectRows(matrix)) {
            Matcher severity = Pattern.compile("`(\\w+)`").matcher(row.get(1));
            Set<String> severities = new TreeSet<>();
            while (severity.find()) {
                assertThat(severity.group(1)).describedAs("severity of " + row.get(0)).isIn("DROPPED", "APPROXIMATED");
                severities.add(severity.group(1));
            }
            assertThat(severities).describedAs("severity of " + row.get(0)).isNotEmpty();
            Matcher subject = Pattern.compile("`([^`]+)`").matcher(row.get(0));
            while (subject.find()) {
                listed.computeIfAbsent(subject.group(1), s -> new TreeSet<>()).addAll(severities);
            }
        }
        return listed;
    }

    private static List<List<String>> subjectRows(String matrix) {
        int start = matrix.indexOf("\n## DOCX export report subjects");
        assertThat(start).describedAs("the matrix has no 'DOCX export report subjects' section").isPositive();
        int end = matrix.indexOf("\n## ", start + 1);
        String section = matrix.substring(start, end < 0 ? matrix.length() : end);
        List<List<String>> rows = new ArrayList<>();
        for (String line : section.split("\n")) {
            if (line.startsWith("| `")) {
                List<String> cells = cells(line);
                assertThat(cells).describedAs(line).hasSize(4);
                rows.add(cells);
            }
        }
        return rows;
    }

    /** The DOCX cell of every four-column capability row, and the text above the first table. */
    private static List<String> docxCells(String matrix) {
        List<String> cells = new ArrayList<>();
        cells.add(matrix.substring(0, matrix.indexOf("\n## ")));
        for (List<String> row : capabilityRows(matrix)) {
            cells.add(row.get(3));
        }
        return cells;
    }

    private static List<String> capabilityNames(String matrix) {
        return capabilityRows(matrix).stream().map(row -> row.get(0)).toList();
    }

    private static List<List<String>> capabilityRows(String matrix) {
        String tables = matrix.substring(0, matrix.indexOf("\n## DOCX export report subjects"));
        List<List<String>> rows = new ArrayList<>();
        for (String line : tables.split("\n")) {
            if (line.startsWith("|") && !line.startsWith("|---")) {
                List<String> cells = cells(line);
                // A row with another count — an escaped pipe, a fifth column — would have its DOCX
                // cell read wrong or not at all.
                assertThat(cells).describedAs("a capability row of four cells: " + line).hasSize(4);
                if (!cells.get(3).equals("DOCX (semantic)")) {
                    rows.add(cells);
                }
            }
        }
        return rows;
    }

    /** Whether a row's capability is that name: its cell up to the dash or the bracket that explains it. */
    private static boolean namesRow(String capability, String name) {
        return capability.split(" — | \\(", 2)[0].strip().equals(name);
    }

    private static List<String> cells(String line) {
        String inner = line.strip();
        inner = inner.substring(1, inner.endsWith("|") ? inner.length() - 1 : inner.length());
        return Stream.of(inner.split("\\|", -1)).map(String::strip).toList();
    }

    /**
     * Every type the repository's sources declare, by its simple name. A hidden directory (another
     * checkout under {@code .claude}, {@code .git}), a build's output and {@code node_modules} are
     * not walked, so a name resolves to this tree's own file.
     */
    private static Map<String, Path> declaredTypes() throws IOException {
        Map<String, Path> types = new TreeMap<>();
        Pattern declaration = Pattern.compile("\\b(?:class|record|interface|enum)\\s+(\\w+)");
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(ROOT, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
                String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                boolean skipped = !dir.equals(ROOT)
                        && (name.startsWith(".") || name.equals("target") || name.equals("node_modules"));
                return skipped ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (file.toString().endsWith(".java")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        // A main source first, so a test declaring a type of the same name does not stand for it.
        Comparator<Path> mainFirst = Comparator.comparing(
                (Path file) -> !ROOT.relativize(file).toString().replace('\\', '/').contains("src/main/"));
        for (Path file : files.stream().sorted(mainFirst.thenComparing(Comparator.naturalOrder())).toList()) {
            // Strings blanked too: a test's source in a text block declares nothing.
            Matcher found = declaration.matcher(blankStrings(readWithoutComments(file)));
            while (found.find()) {
                types.putIfAbsent(found.group(1), file);
            }
        }
        return types;
    }

    private static String readWithoutComments(Path file) {
        try {
            return withoutComments(Files.readString(file));
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }
    }

    private static Set<String> missing(Map<String, Set<String>> from, Map<String, Set<String>> in) {
        Set<String> missing = new TreeSet<>();
        from.forEach((subject, severities) -> {
            for (String severity : severities) {
                if (!in.getOrDefault(subject, Set.of()).contains(severity)) {
                    missing.add(subject + " (" + severity + ")");
                }
            }
        });
        return missing;
    }

    // Java text, read only as far as the guard needs: strings, nesting and comments.

    /** Comments blanked to spaces, lines kept, so a commented-out call is not read. */
    private static String withoutComments(String text) {
        StringBuilder out = new StringBuilder(text);
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (text.startsWith("\"\"\"", i)) {
                int end = text.indexOf("\"\"\"", i + 3);
                i = end < 0 ? text.length() : end + 3;
            } else if (c == '"' || c == '\'') {
                i = endOfQuoted(text, i);
            } else if (text.startsWith("//", i)) {
                int end = text.indexOf('\n', i);
                end = end < 0 ? text.length() : end;
                blank(out, i, end);
                i = end;
            } else if (text.startsWith("/*", i)) {
                int end = text.indexOf("*/", i + 2);
                end = end < 0 ? text.length() : end + 2;
                blank(out, i, end);
                i = end;
            } else {
                i++;
            }
        }
        return out.toString();
    }

    /** String and text-block contents blanked to spaces, comments already gone. */
    private static String blankStrings(String code) {
        StringBuilder out = new StringBuilder(code);
        int i = 0;
        while (i < code.length()) {
            if (code.startsWith("\"\"\"", i)) {
                int end = code.indexOf("\"\"\"", i + 3);
                end = end < 0 ? code.length() : end + 3;
                blank(out, i, end);
                i = end;
            } else if (code.charAt(i) == '"' || code.charAt(i) == '\'') {
                int end = Math.min(endOfQuoted(code, i), code.length());
                blank(out, i, end);
                i = end;
            } else {
                i++;
            }
        }
        return out.toString();
    }

    private static void blank(StringBuilder out, int from, int to) {
        for (int i = from; i < to; i++) {
            if (out.charAt(i) != '\n' && out.charAt(i) != '\r') {
                out.setCharAt(i, ' ');
            }
        }
    }

    private static int endOfQuoted(String text, int open) {
        char quote = text.charAt(open);
        int i = open + 1;
        while (i < text.length() && text.charAt(i) != quote) {
            i += text.charAt(i) == '\\' ? 2 : 1;
        }
        return i + 1;
    }

    /** The argument starting at an index: up to the comma or bracket that closes it. */
    private static String argument(String code, int from) {
        int i = from;
        int depth = 0;
        while (i < code.length()) {
            char c = code.charAt(i);
            if (c == '"' || c == '\'') {
                i = endOfQuoted(code, i);
                continue;
            }
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                if (depth == 0) {
                    break;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                break;
            }
            i++;
        }
        return code.substring(from, i);
    }

    /** What a call's or a record's parentheses hold, from just after the opening one. */
    private static String argumentList(String code, int afterOpen) {
        int i = afterOpen;
        int depth = 0;
        while (i < code.length()) {
            char c = code.charAt(i);
            if (c == '"' || c == '\'') {
                i = endOfQuoted(code, i);
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) {
                    break;
                }
                depth--;
            }
            i++;
        }
        return code.substring(afterOpen, i);
    }

    /** The expression from an index to the semicolon that ends its statement. */
    private static String statement(String code, int from) {
        int i = from;
        int depth = 0;
        while (i < code.length()) {
            char c = code.charAt(i);
            if (c == '"' || c == '\'') {
                i = endOfQuoted(code, i);
                continue;
            }
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            } else if (c == ';' && depth == 0) {
                break;
            }
            i++;
        }
        return code.substring(from, i);
    }

    /** A brace block, from its opening brace to its closing one, both included. */
    private static String block(String code, int open) {
        int i = open;
        int depth = 0;
        while (i < code.length()) {
            char c = code.charAt(i);
            if (c == '"' || c == '\'') {
                i = endOfQuoted(code, i);
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return code.substring(open, i + 1);
            }
            i++;
        }
        return code.substring(open);
    }

    private static String lineAt(String code, int offset) {
        int start = code.lastIndexOf('\n', offset) + 1;
        int end = code.indexOf('\n', offset);
        return code.substring(start, end < 0 ? code.length() : end);
    }

    /** An expression without the parentheses that wrap the whole of it. */
    private static String unwrap(String expr) {
        while (expr.startsWith("(") && expr.endsWith(")") && argumentList(expr, 1).length() == expr.length() - 2) {
            expr = expr.substring(1, expr.length() - 1).strip();
        }
        return expr;
    }

    /** Where a character first stands outside strings and brackets, or -1. */
    private static int topLevel(String expr, char wanted) {
        int depth = 0;
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (c == '"' || c == '\'') {
                i = endOfQuoted(expr, i);
                continue;
            }
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            } else if (c == wanted && depth == 0) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /** The colon that closes a ternary's first branch, nested ternaries counted. */
    private static int matchingColon(String branches) {
        int depth = 0;
        int open = 0;
        int i = 0;
        while (i < branches.length()) {
            char c = branches.charAt(i);
            if (c == '"' || c == '\'') {
                i = endOfQuoted(branches, i);
                continue;
            }
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            } else if (depth == 0 && c == '?') {
                open++;
            } else if (depth == 0 && c == ':') {
                if (open == 0) {
                    return i;
                }
                open--;
            }
            i++;
        }
        throw new AssertionError("a ternary without its colon: " + branches);
    }

    /** Splits at a separator outside strings and brackets, and outside angle brackets where generics are read. */
    private static List<String> splitTopLevel(String expr, char separator, boolean generics) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (c == '"' || c == '\'') {
                i = endOfQuoted(expr, i);
                continue;
            }
            if (c == '(' || c == '[' || c == '{' || generics && c == '<') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}' || generics && c == '>') {
                depth--;
            } else if (c == separator && depth == 0) {
                parts.add(expr.substring(start, i));
                start = i + 1;
            }
            i++;
        }
        parts.add(expr.substring(start));
        return parts;
    }
}
