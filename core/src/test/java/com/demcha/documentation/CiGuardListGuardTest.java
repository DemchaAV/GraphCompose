package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the hand-written {@code -Dtest=} list in the CI guard job against name rot.
 *
 * <p>The "Architecture and Documentation Guards" job selects a fixed set of test
 * classes by name and runs them with {@code -pl :graph-compose-core}. Surefire
 * treats that selection as a whole: it aborts only when <em>every</em> pattern
 * matches nothing. A single name that matches nothing while its siblings match is
 * dropped without a warning, and the job reports success having run fewer tests
 * than it names.</p>
 *
 * <p>Two failure modes therefore pass CI silently. A guard renamed or deleted
 * leaves a dead name behind — the job keeps claiming coverage it no longer has.
 * A guard that lives in another module ({@code qa}, {@code render-pdf}) can be
 * added to the list in good faith and never run, because the module scope
 * excludes it. Both had happened here: the list carried two classes deleted
 * months earlier plus two that live outside {@code core}, so the job executed
 * four of the eight names it advertised.</p>
 *
 * <p>This test parses the workflow and asserts every selected name resolves to a
 * test source under {@code core/src/test/java}. It runs inside the very job it
 * protects, so a bad edit to that line fails the step that made it. Guards owned
 * by other modules belong in the reactor build, not here.</p>
 */
class CiGuardListGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();
    private static final Path WORKFLOW = PROJECT_ROOT.resolve(".github/workflows/ci.yml");
    private static final Path CORE_TESTS = PROJECT_ROOT.resolve("core/src/test/java");

    /**
     * The scope that makes a {@code -Dtest=} selection core-only. Anchored at the
     * end of the reactor list so {@code -pl :graph-compose-core,:graph-compose-qa}
     * — a genuinely multi-module selection — is not held to core residency.
     */
    private static final Pattern CORE_SCOPE =
            Pattern.compile("-pl :graph-compose-core(?![\\w:,-])");

    /**
     * A {@code -Dtest=} value, quoted or bare, up to the end of the shell word.
     * The character class must admit every character Surefire accepts in a
     * selector — including the {@code ?} and {@code /} of a path-shaped wildcard
     * and the leading {@code !} of an exclusion. A character missing here does not
     * fail: it truncates the selector, and the guard then checks a name nobody
     * wrote.
     */
    private static final Pattern TEST_SELECTION =
            Pattern.compile("-Dtest=\"?([A-Za-z0-9_$.,*?/!+#\\[\\]-]+)\"?");

    /** Workflow steps start at a {@code - name:} key at any indentation. */
    private static final Pattern STEP_BOUNDARY = Pattern.compile("(?m)^\\s*- name:");

    @Test
    void everyGuardNamedInTheCoreScopedCiStepLivesInCore() throws IOException {
        List<String> selected = coreScopedTestSelections();

        assertThat(selected)
                .describedAs("no core-scoped '-Dtest=' step found in %s — this guard is reading a "
                        + "workflow shape that moved, so it is no longer guarding anything",
                        relative(WORKFLOW))
                .isNotEmpty();

        Set<String> missing = new TreeSet<>();
        for (String name : selected) {
            if (!resolvesUnderCoreTests(name)) {
                missing.add(name);
            }
        }

        assertThat(missing)
                .describedAs("every class named in the core-scoped '-Dtest=' list of %s must exist "
                        + "under core/src/test/java. Surefire drops an unmatched name in silence "
                        + "whenever another name in the same selection matches, so a deleted guard "
                        + "— or one that lives in qa / render-pdf — leaves the job reporting "
                        + "coverage it does not have. Guards owned by another module run in the "
                        + "reactor build instead.", relative(WORKFLOW))
                .isEmpty();
    }

    /**
     * Class names selected by every workflow step that both filters with
     * {@code -Dtest=} and scopes the reactor to {@code graph-compose-core}. A step
     * that selects tests for a different module is intentionally not checked here.
     */
    private static List<String> coreScopedTestSelections() throws IOException {
        String workflow = Files.readString(WORKFLOW);
        List<String> names = new ArrayList<>();

        for (String step : STEP_BOUNDARY.split(workflow)) {
            if (!CORE_SCOPE.matcher(step).find()) {
                continue;
            }
            Matcher selection = TEST_SELECTION.matcher(step);
            while (selection.find()) {
                Arrays.stream(selection.group(1).split(","))
                        .map(String::trim)
                        .filter(name -> !name.isEmpty())
                        // An exclusion subtracts from the selection instead of
                        // claiming coverage, so requiring it to resolve would
                        // fail a legitimate `!StaleName` left behind on purpose.
                        .filter(name -> !name.startsWith("!"))
                        .forEach(names::add);
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(names));
    }

    /**
     * Whether a Surefire selector selects at least one test source in the engine
     * module. Every accepted form is resolved against the tree — a plain class
     * name, a package-qualified name, a {@code #method} suffix, and a wildcard
     * pattern alike. A wildcard that matches nothing is the same silent skip as a
     * deleted class name, so it is checked rather than waved through.
     */
    private static boolean resolvesUnderCoreTests(String selector) throws IOException {
        Pattern pattern = selectorPattern(selector.split("#", 2)[0]);
        try (Stream<Path> sources = Files.walk(CORE_TESTS)) {
            return sources
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .anyMatch(path -> selects(pattern, path));
        }
    }

    /**
     * Translates a Surefire class selector into a regex. Package separators are
     * normalised to {@code /} so a dotted name and a path form compare alike;
     * {@code **} spans packages, {@code *} and {@code ?} stay inside one segment.
     */
    private static Pattern selectorPattern(String selector) {
        String normalized = selector.endsWith(".java")
                ? selector.substring(0, selector.length() - ".java".length())
                : selector;
        normalized = normalized.replace('.', '/');

        StringBuilder regex = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c != '*' && c != '?') {
                literal.append(c);
                continue;
            }
            if (literal.length() > 0) {
                regex.append(Pattern.quote(literal.toString()));
                literal.setLength(0);
            }
            if (c == '?') {
                regex.append("[^/]");
            } else if (i + 1 < normalized.length() && normalized.charAt(i + 1) == '*') {
                regex.append(".*");
                i++;
            } else {
                regex.append("[^/]*");
            }
        }
        if (literal.length() > 0) {
            regex.append(Pattern.quote(literal.toString()));
        }
        return Pattern.compile(regex.toString());
    }

    /** Whether the selector matches this source by simple name or by package path. */
    private static boolean selects(Pattern pattern, Path source) {
        String fileName = source.getFileName().toString();
        String simpleName = fileName.substring(0, fileName.length() - ".java".length());
        String relative = CORE_TESTS.relativize(source).toString().replace('\\', '/');
        String qualified = relative.substring(0, relative.length() - ".java".length());
        return pattern.matcher(simpleName).matches() || pattern.matcher(qualified).matches();
    }

    private static String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
