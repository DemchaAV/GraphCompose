package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the CodeQL analysis scope from falling behind the build.
 *
 * <p>The Java extractor sees whatever the build compiles, so the {@code -pl} list in the
 * scanning workflow <em>is</em> the analysis scope — a module absent from it is not
 * partially covered, it is not scanned at all. That is invisible: the job stays green,
 * the badge stays green, and the alert list simply never mentions the module. It held
 * for a full release, with the PDFBox, POI, SVG, font and ZIP/OPC paths — the code most
 * exposed to untrusted input — outside the scan while core was analysed.
 *
 * <p>The expectation is derived rather than listed, from two sources that fail
 * differently. Against the verify gate, a module CI compiles must also be scanned — that
 * catches the scan falling behind the build. Against the publish workflows, a module a
 * release deploys must be scanned — that catches what the first comparison cannot, since
 * a new artifact forgotten in both CI and the scan is missing from both sides of it and
 * the sets agree perfectly.
 */
class CodeQlScopeGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();
    private static final Path CI_WORKFLOW = PROJECT_ROOT.resolve(".github/workflows/ci.yml");
    private static final Path CODEQL_WORKFLOW = PROJECT_ROOT.resolve(".github/workflows/codeql.yml");

    /**
     * A {@code -pl} value. Selectors are collected token by token rather than by one
     * regex: a wrapped command joins into {@code -pl :a,:b, :c -am}, and a pattern that
     * stopped at the first space would silently shrink the very set it compares against.
     */
    private static final Pattern OPTION = Pattern.compile("^-{1,2}\\w.*");

    @Test
    void everyCodeBearingModuleTheVerifyGateBuildsIsAlsoScanned() throws IOException {
        Set<String> verified = modulesWithSources(selectorsFrom(CI_WORKFLOW, "clean verify"));
        Set<String> scanned = modulesWithSources(selectorsFrom(CODEQL_WORKFLOW, "package"));

        assertThat(verified)
                .describedAs("no code-bearing module was found in the ci.yml verify gate — the "
                        + "gate's -pl list or the module layout moved, and this guard is comparing "
                        + "two empty sets")
                .isNotEmpty();

        Set<String> unscanned = new TreeSet<>(verified);
        unscanned.removeAll(scanned);

        assertThat(unscanned)
                .describedAs("a published module that CI compiles but CodeQL does not: the "
                        + "extractor only sees what the build compiles, so this code is not "
                        + "partially analysed, it is absent from the scan entirely — and nothing "
                        + "about the result says so")
                .isEmpty();
    }

    /**
     * Everything a release deploys, and that carries code, is scanned.
     *
     * <p>The independent half. Comparing the scan against CI answers the narrower
     * question of whether the two lists agree, and two lists agree perfectly when a
     * module is missing from both — which is the shape a new artifact takes when it is
     * added to the publish train and forgotten everywhere else. The inventory here comes
     * from the publish workflows, so the question becomes the one the promise makes:
     * does the scan cover what users can actually depend on.</p>
     */
    @Test
    void everyDeployedModuleWithSourcesIsScanned() throws IOException {
        Set<String> deployed = withSources(PublishedModules.deployed(PROJECT_ROOT));
        Set<String> scanned = modulesWithSources(selectorsFrom(CODEQL_WORKFLOW, "package"));

        assertThat(deployed)
                .describedAs("no deployed module with sources was found — the publish workflows' "
                        + "deploy steps moved, and this guard is comparing against nothing")
                .isNotEmpty();

        Set<String> unscanned = new TreeSet<>(deployed);
        unscanned.removeAll(scanned);

        assertThat(unscanned)
                .describedAs("a module a release deploys, and whose code a consumer therefore "
                        + "runs, is outside the scan. Naming it in the scan's -pl list is the fix; "
                        + "relying on it arriving as somebody's dependency is not, because that "
                        + "stops the day the dependency does")
                .isEmpty();
    }

    /**
     * The {@code -pl} selectors of the {@code mvnw} invocation carrying {@code goal}, or
     * every reactor module when the command carries no {@code -pl} at all — a build
     * without one compiles the whole reactor, which is more coverage, not less.
     */
    private static List<String> selectorsFrom(Path workflow, String goal) throws IOException {
        List<String> tokens = List.of(buildStep(workflow, goal).strip().split("\\s+"));
        int at = tokens.indexOf("-pl");
        if (at < 0) {
            return PublishedModules.of(PROJECT_ROOT).stream().map(module -> ":" + module).toList();
        }

        StringBuilder selectors = new StringBuilder();
        for (String token : tokens.subList(at + 1, tokens.size())) {
            if (OPTION.matcher(token).matches()) {
                break;
            }
            selectors.append(token);
        }
        assertThat(selectors.length())
                .describedAs("the -pl in the %s invocation of %s is followed by no selectors",
                        goal, PROJECT_ROOT.relativize(workflow))
                .isPositive();
        return List.of(selectors.toString().split(","));
    }

    /**
     * The text of the {@code mvnw} invocation carrying {@code goal}, joined across the
     * line continuations the longer commands are wrapped in.
     */
    private static String buildStep(Path workflow, String goal) throws IOException {
        List<String> lines = Files.readAllLines(workflow);
        List<String> matches = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).contains("mvnw")) {
                continue;
            }
            StringBuilder command = new StringBuilder(lines.get(i));
            for (int j = i + 1; j < lines.size() && command.toString().strip().endsWith("\\"); j++) {
                command.append(' ').append(lines.get(j));
            }
            String joined = command.toString().replace("\\", " ");
            if (joined.contains(goal)) {
                matches.add(joined);
            }
        }

        assertThat(matches)
                .describedAs("no mvnw invocation carrying '%s' in %s — the step this guard reads "
                        + "was renamed or its goal changed", goal, PROJECT_ROOT.relativize(workflow))
                .isNotEmpty();

        // A module selection is what this guard is after, so prefer the invocation that
        // carries one. Falling back to the first match keeps a command that drops `-pl`
        // entirely readable rather than throwing — building the whole reactor scans more,
        // not less, and a guard that fails on the stricter setup would push toward the
        // looser one.
        return matches.stream().filter(command -> command.contains("-pl")).findFirst()
                .orElse(matches.get(0));
    }

    /** Of the given module directories, the artifact ids of those carrying main sources. */
    private static Set<String> withSources(java.util.List<String> moduleDirectories) throws IOException {
        Map<String, Path> byArtifactId = PublishedModules.byArtifactId(PROJECT_ROOT);
        Set<String> withSources = new TreeSet<>();
        byArtifactId.forEach((artifactId, directory) -> {
            String name = directory.getFileName().toString();
            if (moduleDirectories.contains(name)
                    && Files.isDirectory(directory.resolve("src/main/java"))) {
                withSources.add(artifactId);
            }
        });
        return withSources;
    }

    /** Of the given {@code :artifact-id} selectors, those whose module carries main sources. */
    private static Set<String> modulesWithSources(List<String> selectors) throws IOException {
        Map<String, Path> byArtifactId = PublishedModules.byArtifactId(PROJECT_ROOT);

        assertThat(byArtifactId)
                .describedAs("no module resolved to a directory — the root pom's module list or "
                        + "the poms' own coordinates moved, and every selector below would be "
                        + "dropped as unknown")
                .isNotEmpty();

        Set<String> withSources = new TreeSet<>();
        for (String selector : selectors) {
            String artifactId = selector.strip().replaceFirst("^:", "");
            Path module = byArtifactId.get(artifactId);
            if (module != null && Files.isDirectory(module.resolve("src/main/java"))) {
                withSources.add(artifactId);
            }
        }
        return withSources;
    }
}
