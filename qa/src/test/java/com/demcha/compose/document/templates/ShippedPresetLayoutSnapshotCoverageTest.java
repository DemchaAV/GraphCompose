package com.demcha.compose.document.templates;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every shipped preset is rendered by an active layout-snapshot suite.
 *
 * <p>The pixel parity gates tolerate tens of thousands of mismatched pixels, so a shift of a
 * few points in a preset passes them; only the exact layout snapshot sees it. Thirty-three
 * shipped presets once went two months with no snapshot at all: the suites that pinned them
 * were deleted along with an older template family, their baselines stayed behind, and
 * nothing noticed that no test read those files any more.</p>
 *
 * <p>So the two sets are derived independently and compared. The shipped presets come from
 * the templates sources — every class under a {@code presets} package declaring a public
 * {@code ID}. The covered presets come from the qa sources — a suite is any test calling
 * {@code assertCanonicalSnapshot} and not disabled, and it covers a preset of its own family
 * it creates directly ({@code Preset.create(} or {@code Preset::create}) or through a
 * {@code Stream<Arguments>} roster it reads from a fixture. A fixture read only for its
 * document covers nothing. Neither side is a hand-kept list, so adding a preset without a
 * suite reddens this test on its own.</p>
 */
class ShippedPresetLayoutSnapshotCoverageTest {

    private static final Path PRESET_SOURCES = Path.of("..", "templates", "src", "main", "java",
            "com", "demcha", "compose", "document", "templates");
    private static final Path SUITE_SOURCES = Path.of("src", "test", "java",
            "com", "demcha", "compose", "document", "templates");

    /**
     * Shipped presets deliberately left without a layout snapshot, as family/Preset to the
     * reason. Empty: every shipped preset has one.
     */
    private static final Map<String, String> ALLOW_LIST = Map.of();

    private static final Pattern ROSTER_CALL =
            Pattern.compile("\\b([A-Z]\\w*Fixtures)\\s*\\.\\s*(\\w+)\\s*\\(\\s*\\)");

    /** {@code @Disabled}, imported or fully qualified. */
    private static final Pattern DISABLED = Pattern.compile("@\\s*(?:[\\w.]+\\.)?Disabled\\b");

    @Test
    void everyShippedPresetIsRenderedByAnActiveLayoutSnapshotSuite() throws IOException {
        Set<String> shipped = shippedPresets();
        Map<String, Set<String>> covered = coverage(shipped);

        assertThat(shipped)
                .describedAs("no shipped preset found under %s — the scan is looking in the wrong place",
                        PRESET_SOURCES.toAbsolutePath().normalize())
                .isNotEmpty();

        List<String> uncovered = shipped.stream()
                .filter(preset -> !covered.containsKey(preset) && !ALLOW_LIST.containsKey(preset))
                .toList();
        assertThat(uncovered)
                .describedAs("shipped presets no active layout-snapshot suite renders — add each to a "
                        + "family roster or give it a *LayoutSnapshotTest, or allow-list it with the reason")
                .isEmpty();
    }

    @Test
    void theAllowListNamesOnlyShippedPresetsThatReallyHaveNoSuite() throws IOException {
        Set<String> shipped = shippedPresets();
        Map<String, Set<String>> covered = coverage(shipped);

        assertThat(ALLOW_LIST.keySet().stream().filter(preset -> !shipped.contains(preset)).sorted().toList())
                .describedAs("the allow-list names presets that do not ship")
                .isEmpty();
        assertThat(ALLOW_LIST.keySet().stream().filter(covered::containsKey).sorted().toList())
                .describedAs("the allow-list exempts presets a suite already renders — drop the exemptions")
                .isEmpty();
    }

    /** family/Preset for every class under a presets package that declares a public ID. */
    private static Set<String> shippedPresets() throws IOException {
        Set<String> shipped = new TreeSet<>();
        try (Stream<Path> files = Files.walk(PRESET_SOURCES)) {
            for (Path file : files.filter(ShippedPresetLayoutSnapshotCoverageTest::isJava).toList()) {
                Path rel = PRESET_SOURCES.relativize(file);
                if (rel.getNameCount() == 3 && rel.getName(1).toString().equals("presets")
                        && code(Files.readString(file)).contains("public static final String ID")) {
                    shipped.add(rel.getName(0) + "/" + simpleName(file));
                }
            }
        }
        return shipped;
    }

    /** family/Preset to the suites that render it. */
    private static Map<String, Set<String>> coverage(Set<String> shipped) throws IOException {
        Map<String, Set<String>> covered = new TreeMap<>();
        try (Stream<Path> files = Files.walk(SUITE_SOURCES)) {
            for (Path suite : files.filter(ShippedPresetLayoutSnapshotCoverageTest::isJava).toList()) {
                String code = code(Files.readString(suite));
                if (!code.contains("assertCanonicalSnapshot(") || DISABLED.matcher(code).find()
                        || suite.getFileName().toString().equals("TemplateTestSupport.java")) {
                    continue;
                }
                String family = SUITE_SOURCES.relativize(suite).getName(0).toString();
                List<String> rendered = new ArrayList<>(created(code, shipped, family));
                Matcher roster = ROSTER_CALL.matcher(code);
                while (roster.find()) {
                    Path fixture = suite.resolveSibling(roster.group(1) + ".java");
                    if (Files.exists(fixture)) {
                        String body = rosterBody(code(Files.readString(fixture)), roster.group(2));
                        rendered.addAll(created(body, shipped, family));
                    }
                }
                for (String preset : rendered) {
                    covered.computeIfAbsent(preset, key -> new TreeSet<>()).add(simpleName(suite));
                }
            }
        }
        return covered;
    }

    /** The shipped presets of one family that a piece of code creates. */
    private static List<String> created(String code, Set<String> shipped, String family) {
        List<String> found = new ArrayList<>();
        for (String preset : shipped) {
            if (!preset.startsWith(family + "/")) {
                continue;
            }
            String name = Pattern.quote(preset.substring(family.length() + 1));
            if (Pattern.compile("\\b" + name + "\\s*(::\\s*create\\b|\\.\\s*create\\s*\\()").matcher(code).find()) {
                found.add(preset);
            }
        }
        return found;
    }

    /** The body of a {@code Stream<Arguments>} method; empty when the method is not a roster. */
    private static String rosterBody(String code, String method) {
        Matcher start = Pattern.compile("Stream\\s*<\\s*Arguments\\s*>\\s+" + Pattern.quote(method) + "\\s*\\(\\s*\\)\\s*\\{")
                .matcher(code);
        if (!start.find()) {
            return "";
        }
        int depth = 1;
        for (int i = start.end(); i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return code.substring(start.end(), i);
            }
        }
        return "";
    }

    /** Source with comments and string and character literals blanked, so prose cannot count. */
    private static String code(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (c == '/' && next == '/') {
                while (i < source.length() && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && next == '*') {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? source.length() : end + 2;
                out.append(' ');
            } else if (c == '"' || c == '\'') {
                boolean textBlock = c == '"' && source.startsWith("\"\"\"", i);
                String close = textBlock ? "\"\"\"" : String.valueOf(c);
                i += close.length();
                while (i < source.length() && !source.startsWith(close, i)) {
                    i += source.charAt(i) == '\\' ? 2 : 1;
                }
                i += close.length();
                out.append("\"\"");
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static boolean isJava(Path path) {
        return path.getFileName().toString().endsWith(".java");
    }

    private static String simpleName(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - ".java".length());
    }
}
