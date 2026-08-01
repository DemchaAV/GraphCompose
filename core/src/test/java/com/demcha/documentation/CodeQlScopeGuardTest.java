package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
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
 * <p>The expectation is derived rather than listed: every module the verify gate builds
 * that carries production sources must also be built for the scan. A module added to
 * the reactor and to CI is therefore covered on the day it lands, and a module dropped
 * from the scan fails here rather than going quiet.
 */
class CodeQlScopeGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();
    private static final Path CI_WORKFLOW = PROJECT_ROOT.resolve(".github/workflows/ci.yml");
    private static final Path CODEQL_WORKFLOW = PROJECT_ROOT.resolve(".github/workflows/codeql.yml");

    /** A {@code -pl} value: the comma-separated run of module selectors that follows it. */
    private static final Pattern MODULE_LIST = Pattern.compile("-pl\\s+(:[^\\s\\\\]+)");

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
     * The scan has to build its dependencies from source.
     *
     * <p>Without {@code -am} Maven resolves the reactor siblings from the repository and
     * compiles only the named modules — which, for a list whose first entry everything
     * else depends on, means the build fails or silently narrows. It is one token, and
     * losing it would look like a formatting change.
     */
    @Test
    void theScanBuildsItsDependenciesFromSource() throws IOException {
        String step = buildStep(CODEQL_WORKFLOW, "package");

        assertThat(step)
                .describedAs("the CodeQL build must pass -am, or the modules it names are "
                        + "resolved as jars instead of compiled — and a jar is not extracted")
                .contains("-am");
    }

    /** The {@code -pl} selectors of the first {@code mvnw} invocation containing {@code goal}. */
    private static List<String> selectorsFrom(Path workflow, String goal) throws IOException {
        Matcher matcher = MODULE_LIST.matcher(buildStep(workflow, goal));
        assertThat(matcher.find())
                .describedAs("no -pl module list found in the %s invocation of %s",
                        goal, PROJECT_ROOT.relativize(workflow))
                .isTrue();
        return List.of(matcher.group(1).split(","));
    }

    /**
     * The text of the {@code mvnw} invocation carrying {@code goal}, joined across the
     * line continuations the longer commands are wrapped in.
     */
    private static String buildStep(Path workflow, String goal) throws IOException {
        List<String> lines = Files.readAllLines(workflow);
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).contains("mvnw")) {
                continue;
            }
            StringBuilder command = new StringBuilder(lines.get(i));
            for (int j = i + 1; j < lines.size() && command.toString().strip().endsWith("\\"); j++) {
                command.append(' ').append(lines.get(j));
            }
            String joined = command.toString().replace("\\", " ");
            if (joined.contains(goal) && joined.contains("-pl")) {
                return joined;
            }
        }
        throw new AssertionError("no mvnw invocation with '" + goal + "' and -pl in "
                + PROJECT_ROOT.relativize(workflow));
    }

    /** Of the given {@code :artifact-id} selectors, those whose module carries main sources. */
    private static Set<String> modulesWithSources(List<String> selectors) throws IOException {
        Set<String> withSources = new TreeSet<>();
        for (String selector : selectors) {
            String artifactId = selector.strip().replaceFirst("^:", "");
            Path module = moduleDirectoryOf(artifactId);
            if (module != null && Files.isDirectory(module.resolve("src/main/java"))) {
                withSources.add(artifactId);
            }
        }
        return withSources;
    }

    /** The reactor module directory declaring {@code artifactId}, or null when none does. */
    private static Path moduleDirectoryOf(String artifactId) throws IOException {
        String rootPom = Files.readString(PROJECT_ROOT.resolve("pom.xml"));
        Matcher modules = Pattern.compile("<module>\\s*([^<]+?)\\s*</module>").matcher(rootPom);
        List<String> candidates = new ArrayList<>();
        while (modules.find()) {
            candidates.add(modules.group(1));
        }
        for (String module : candidates) {
            Path pom = PROJECT_ROOT.resolve(module).resolve("pom.xml");
            if (Files.isRegularFile(pom)
                    && Files.readString(pom).contains("<artifactId>" + artifactId + "</artifactId>")) {
                return PROJECT_ROOT.resolve(module);
            }
        }
        return null;
    }
}
