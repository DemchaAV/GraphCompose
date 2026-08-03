package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards that the coordinate the docs send readers to for an API reference is one
 * that actually publishes a javadoc jar.
 *
 * <p>javadoc.io renders the newest version of a coordinate that carries a
 * {@code -javadoc.jar}, and quietly falls back to an older one when the newest does
 * not. That is not an error page a reader would notice: it is a complete, correct
 * API reference for the wrong version, presented as current.</p>
 *
 * <p>Which is what happened. The {@code graph-compose} wrapper has no sources of its
 * own, so the javadoc goal found nothing to archive and attached no artifact at all
 * — not an empty one. Every 2.x release shipped without it, and the only
 * API-reference link in the repository served the 1.9.1 API for two releases running
 * while the prose beside it promised documentation fresh after each release.</p>
 *
 * <p>So a documented coordinate must resolve to a module that will produce that jar:
 * either it has sources of its own, or its javadoc execution builds the jar from a
 * dependency's ({@code includeDependencySources}). Checking the build files rather
 * than the hosted page keeps the guard offline and deterministic.</p>
 */
class PublishedJavadocCoordinateGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();

    /** Pages that may advertise an API reference. */
    private static final List<String> DOC_PAGES = List.of("README.md", "CONTRIBUTING.md");

    /** A javadoc.io link, capturing the artifactId it points at. */
    private static final Pattern JAVADOC_IO_LINK =
            Pattern.compile("javadoc\\.io/doc/io\\.github\\.demchaav/([A-Za-z0-9.-]+)");

    private static final Pattern MODULE_ENTRY = Pattern.compile("<module>([^<]+)</module>");
    private static final Pattern ARTIFACT_ID = Pattern.compile("<artifactId>([^<]+)</artifactId>");
    private static final Pattern PARENT_BLOCK = Pattern.compile("<parent>.*?</parent>", Pattern.DOTALL);
    private static final Pattern DEPENDENCY_SOURCE_INCLUDE =
            Pattern.compile("<dependencySourceInclude>([^<]+)</dependencySourceInclude>");

    @Test
    void everyDocumentedJavadocCoordinatePublishesAJavadocJar() throws IOException {
        Set<String> documented = documentedJavadocCoordinates();

        assertThat(documented)
                .describedAs("no javadoc.io link found in %s — this guard is reading pages that "
                        + "no longer advertise an API reference, so it guards nothing", DOC_PAGES)
                .isNotEmpty();

        Map<String, Path> modules = modulesByArtifactId();
        List<String> problems = new ArrayList<>();

        for (String artifactId : documented) {
            Path module = modules.get(artifactId);
            if (module == null) {
                problems.add(artifactId + " is documented as an API reference but no module in the "
                        + "reactor builds it");
                continue;
            }
            String pom = Files.readString(module.resolve("pom.xml"));
            if (!pom.contains("attach-javadocs")) {
                problems.add(artifactId + " (" + relative(module) + ") has no attach-javadocs "
                        + "execution, so its release ships no javadoc jar");
                continue;
            }
            if (hasOwnSources(module)) {
                continue;
            }
            if (!pom.contains("<includeDependencySources>true</includeDependencySources>")) {
                problems.add(artifactId + " (" + relative(module) + ") has no sources of its own and "
                        + "does not set includeDependencySources: the javadoc goal will archive "
                        + "nothing and attach no artifact, and javadoc.io will keep serving "
                        + "whichever older version last carried one");
                continue;
            }
            // Aggregating from a dependency only works if the named dependency exists
            // and has sources to aggregate. A typo here fails the same way an absent
            // config does — silently, with no artifact — so it is checked, not assumed.
            List<String> aggregated = aggregatedSourceModules(pom, modules);
            if (aggregated.isEmpty()) {
                problems.add(artifactId + " (" + relative(module) + ") sets includeDependencySources "
                        + "but no <dependencySourceInclude> names a reactor module that has sources; "
                        + "the aggregation would produce nothing");
            }
        }

        assertThat(problems)
                .describedAs("a coordinate the documentation points at for an API reference must "
                        + "publish a javadoc jar")
                .isEmpty();
    }

    private static Set<String> documentedJavadocCoordinates() throws IOException {
        Set<String> coordinates = new LinkedHashSet<>();
        for (String page : DOC_PAGES) {
            Matcher link = JAVADOC_IO_LINK.matcher(Files.readString(PROJECT_ROOT.resolve(page)));
            while (link.find()) {
                coordinates.add(link.group(1));
            }
        }
        return coordinates;
    }

    /** Reactor modules keyed by the artifactId they build. */
    private static Map<String, Path> modulesByArtifactId() throws IOException {
        Map<String, Path> modules = new LinkedHashMap<>();
        Matcher entry = MODULE_ENTRY.matcher(Files.readString(PROJECT_ROOT.resolve("pom.xml")));
        while (entry.find()) {
            Path module = PROJECT_ROOT.resolve(entry.group(1).trim());
            Path pom = module.resolve("pom.xml");
            if (!Files.exists(pom)) {
                continue;
            }
            // Drop the <parent> block first: qa and coverage inherit from the
            // aggregator, and its artifactId is declared before their own.
            String text = PARENT_BLOCK.matcher(Files.readString(pom)).replaceFirst("");
            Matcher artifact = ARTIFACT_ID.matcher(text);
            if (artifact.find()) {
                modules.putIfAbsent(artifact.group(1).trim(), module);
            }
        }
        return modules;
    }

    /**
     * The reactor modules a pom's {@code <dependencySourceInclude>} entries name and
     * that actually carry sources — the ones whose API the aggregated javadoc will
     * document.
     */
    private static List<String> aggregatedSourceModules(String pom, Map<String, Path> modules) throws IOException {
        List<String> documented = new ArrayList<>();
        Matcher include = DEPENDENCY_SOURCE_INCLUDE.matcher(pom);
        while (include.find()) {
            String[] coordinate = include.group(1).trim().split(":");
            if (coordinate.length != 2) {
                continue;
            }
            Path source = modules.get(coordinate[1].trim());
            if (source != null && hasOwnSources(source)) {
                documented.add(coordinate[1].trim());
            }
        }
        return documented;
    }

    private static boolean hasOwnSources(Path module) throws IOException {
        Path sources = module.resolve("src/main/java");
        if (!Files.isDirectory(sources)) {
            return false;
        }
        try (Stream<Path> tree = Files.walk(sources)) {
            return tree.anyMatch(file -> file.getFileName().toString().endsWith(".java"));
        }
    }

    private static String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
