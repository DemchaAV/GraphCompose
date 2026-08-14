package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds {@code AGENTS.md} to naming things that exist.
 *
 * <p>The file is a map: read these documents, work in these packages, these are the
 * modules. A map is only worth having while it is true, and this one is read by
 * automation that will follow a dead path without noticing — the failure is not a
 * confused contributor but a change made in the wrong place, confidently.</p>
 *
 * <p>Nothing here judges the advice. It checks that every document it sends a reader
 * to opens, every package it points at exists, and every module it names is built —
 * the claims that rot on their own when the repository moves underneath them, which
 * is how a contributing guide once went on routing new code into two packages the 2.0
 * split had already emptied.</p>
 */
class AgentsGuideGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();

    private static String agentsGuide() throws IOException {
        return Files.readString(PROJECT_ROOT.resolve("AGENTS.md"));
    }

    @Test
    void everyDocumentItSendsAReaderToExists() throws IOException {
        // Backticked paths under docs/, plus the root-level guides. A path that has
        // moved leaves the instruction "read this first" pointing at nothing.
        Matcher referenced = Pattern.compile("`((?:docs/|CONTRIBUTING\\.md|README\\.md)[^`]*)`")
                .matcher(agentsGuide());

        Set<String> missing = new TreeSet<>();
        Set<String> checked = new TreeSet<>();
        while (referenced.find()) {
            String path = referenced.group(1).replaceAll("/$", "");
            checked.add(path);
            if (!Files.exists(PROJECT_ROOT.resolve(path))) {
                missing.add(path);
            }
        }

        assertThat(checked)
                .describedAs("AGENTS.md must keep pointing readers at the repository's own "
                        + "documentation — finding none suggests the reading list was "
                        + "dropped or its formatting changed under this guard")
                .isNotEmpty();
        assertThat(missing)
                .describedAs("AGENTS.md sends a reader to documents that are not there")
                .isEmpty();
    }

    @Test
    void everyPackageItPointsAtExists() throws IOException {
        // The canonical surface is listed by coordinate. An emptied or renamed package
        // read as current is how code lands somewhere nothing loads it from.
        Matcher referenced = Pattern.compile("`(com\\.demcha\\.compose\\.[a-z0-9.]+)`")
                .matcher(agentsGuide());

        Set<String> missing = new TreeSet<>();
        Set<String> checked = new TreeSet<>();
        while (referenced.find()) {
            String coordinate = referenced.group(1);
            if (coordinate.endsWith(".")) {
                continue;
            }
            checked.add(coordinate);
            String relative = coordinate.replace('.', '/');
            boolean found = SOURCE_ROOTS.stream()
                    .anyMatch(root -> Files.isDirectory(PROJECT_ROOT.resolve(root).resolve(relative)));
            if (!found) {
                missing.add(coordinate);
            }
        }

        assertThat(checked).describedAs("AGENTS.md must name the packages it routes work into")
                .isNotEmpty();
        assertThat(missing)
                .describedAs("AGENTS.md points at packages that exist in no source tree")
                .isEmpty();
    }

    private static final List<String> SOURCE_ROOTS = List.of(
            "core/src/main/java", "render-pdf/src/main/java", "render-pptx/src/main/java",
            "render-docx/src/main/java", "templates/src/main/java", "testing/src/main/java");

    @Test
    void everyModuleItNamesIsBuilt() throws IOException {
        // Read from the module list rather than from every backtick in the file: the
        // section names the modules as a set, and a module dropped from the reactor
        // should fail here rather than mislead someone about where code belongs.
        String guide = agentsGuide();
        int start = guide.indexOf("Important modules:");
        assertThat(start).describedAs("AGENTS.md must carry its module list").isNotNegative();
        String section = guide.substring(start, guide.indexOf("\n## ", start));

        Matcher named = Pattern.compile("(?m)^- `([a-z-]+)` —").matcher(section);
        Set<String> missing = new TreeSet<>();
        Set<String> checked = new TreeSet<>();
        while (named.find()) {
            String module = named.group(1);
            checked.add(module);
            if (!Files.exists(PROJECT_ROOT.resolve(module).resolve("pom.xml"))) {
                missing.add(module);
            }
        }

        assertThat(checked).describedAs("the module list must name modules").isNotEmpty();
        assertThat(missing)
                .describedAs("AGENTS.md names modules with no pom.xml — either they were "
                        + "removed or renamed, and the list still sends work to them")
                .isEmpty();
    }

    @Test
    void theClaudePointerLeadsToTheOneSetOfRules() throws IOException {
        // Claude Code reads CLAUDE.md and several other tools read AGENTS.md. The
        // pointer is what keeps that from becoming two divergent rule sets, so it has
        // to stay a pointer: a CLAUDE.md that grew its own instructions is the
        // duplication it exists to prevent.
        Path pointer = PROJECT_ROOT.resolve("CLAUDE.md");
        assertThat(pointer)
                .describedAs("CLAUDE.md must exist, or Claude Code reads no rules at all")
                .exists();

        String text = Files.readString(pointer);
        assertThat(text)
                .describedAs("CLAUDE.md must point at AGENTS.md")
                .contains("AGENTS.md");
        assertThat(text.lines().count())
                .describedAs("CLAUDE.md is a pointer, not a second rule set — it has grown "
                        + "to %d lines, which is long enough to have started disagreeing "
                        + "with AGENTS.md", text.lines().count())
                .isLessThan(20L);
    }
}
