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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards every workflow job that builds the benchmarks module against resolving a
 * sibling module from Maven Central.
 *
 * <p>{@code benchmarks/pom.xml} depends on four first-party artifacts, and one of
 * them cannot come from Central at any version: the {@code tests}-classifier jar of
 * {@code graph-compose-templates}, which carries the template data fixtures
 * {@code CanonicalBenchmarkSupport} renders. The templates pom builds it as a local
 * aid and its {@code release} profile unbinds it, so it is never deployed. A job
 * that does not install the templates module from source therefore cannot resolve
 * the benchmarks module, whatever else it gets right.</p>
 *
 * <p>The failure is invisible where it is introduced. The JMH workflow runs weekly
 * from the default branch and on manual dispatch, never on a pull request, so a job
 * missing an install step goes red every Monday while every PR that could have
 * caught it stays green. That job resolved {@code graph-compose-templates:jar:tests}
 * from Central and died at dependency resolution on five of its last six weekly runs,
 * each looking for the jar under the version the release before it had just
 * published.</p>
 *
 * <p>The check is keyed on the presence of an install, never on recognising a
 * command that looks wrong: for each job that points Maven at
 * {@code benchmarks/pom.xml}, every first-party dependency the benchmarks pom
 * declares must be installed by an earlier step of the same job. Reading that list
 * out of the pom instead of hard-coding it means a newly added sibling dependency
 * arrives already guarded. A job that installs its dependencies transitively
 * ({@code -pl … -am}) reads as missing here, deliberately: this repository installs
 * each module explicitly, and a guard that reasons about reachability is a guard
 * that can be argued with.</p>
 */
class BenchmarkDependencyInstallGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();
    private static final Path WORKFLOWS = PROJECT_ROOT.resolve(".github/workflows");
    private static final Path BENCHMARKS_POM = PROJECT_ROOT.resolve("benchmarks/pom.xml");
    private static final Path ROOT_POM = PROJECT_ROOT.resolve("pom.xml");

    /** The build the guarded jobs run: anything that points Maven at the benchmarks pom. */
    private static final String BENCHMARK_BUILD = "-f benchmarks/pom.xml";

    /** The group every module of this repository publishes under. */
    private static final String GROUP = "io.github.demchaav";

    /**
     * XML comments, stripped before either pom is read. The root pom documents the
     * per-module deploy invocations in prose that spells {@code <module>} literally,
     * and a commented-out dependency is not a dependency; parsing either as live
     * markup makes the guard require an install nobody owes.
     */
    private static final Pattern XML_COMMENT = Pattern.compile("(?s)<!--.*?-->");

    private static final Pattern DEPENDENCY = Pattern.compile("(?s)<dependency>(.*?)</dependency>");
    private static final Pattern PARENT = Pattern.compile("(?s)<parent>.*?</parent>");
    private static final Pattern GROUP_ID = Pattern.compile("<groupId>\\s*([^<]+?)\\s*</groupId>");
    private static final Pattern ARTIFACT_ID = Pattern.compile("<artifactId>\\s*([^<]+?)\\s*</artifactId>");
    private static final Pattern MODULE = Pattern.compile("<module>\\s*([^<]+?)\\s*</module>");

    /** The {@code jobs:} mapping — from its key down to the next top-level key or EOF. */
    private static final Pattern JOBS_REGION = Pattern.compile("(?ms)^jobs:[ \\t]*$(.*?)(?=^\\S|\\z)");

    /** A job key: a two-space-indented mapping key inside {@code jobs:}. */
    private static final Pattern JOB_KEY =
            Pattern.compile("(?m)^  (?!#)(\\S[^:]*?):(?=[ \\t]|$)[^\\r\\n]*$");

    /**
     * A step boundary: a {@code - name:} sequence item at any indentation. A step
     * written without a name folds into the one above it, which can only cost an
     * install its own step and report a job that has one as missing — wrong in the
     * direction that gets looked at.
     */
    private static final Pattern STEP_BOUNDARY = Pattern.compile("(?m)^\\s*- name:");

    /** A YAML comment line: never executed, so never evidence that a step installs anything. */
    private static final Pattern COMMENT_LINE = Pattern.compile("(?m)^\\s*#.*$");

    @Test
    void everyJobThatBuildsTheBenchmarksInstallsItsSiblingModulesFromSource() throws IOException {
        Set<String> required = firstPartyDependenciesOfTheBenchmarks();
        Map<String, String> moduleDirectories = moduleDirectoriesByArtifactId();

        assertThat(required)
                .describedAs("no '%s' dependency parsed out of %s — this guard is reading a pom "
                                + "shape that moved, so it is no longer guarding anything",
                        GROUP, relative(BENCHMARKS_POM))
                .isNotEmpty();
        assertThat(moduleDirectories.keySet())
                .describedAs("every '%s' dependency of %s must be a module of the reactor: a "
                                + "dependency with no module behind it cannot be built from source, "
                                + "so no workflow step can satisfy this guard for it",
                        GROUP, relative(BENCHMARKS_POM))
                .containsAll(required);

        List<String> uninstalled = new ArrayList<>();
        int guardedJobs = 0;

        for (Path workflow : workflows()) {
            String yaml = Files.readString(workflow);
            for (Map.Entry<String, String> job : jobs(yaml).entrySet()) {
                List<String> steps = steps(job.getValue());
                int build = indexOfTheBenchmarkBuild(steps);
                if (build < 0) {
                    continue;
                }
                guardedJobs++;
                List<String> earlier = steps.subList(0, build);
                for (String artifact : required) {
                    String moduleDirectory = moduleDirectories.get(artifact);
                    if (earlier.stream().noneMatch(step -> installs(step, artifact, moduleDirectory))) {
                        uninstalled.add(("%s → job '%s' builds the benchmarks without first "
                                + "installing %s (./mvnw -f %s/pom.xml -DskipTests install)")
                                .formatted(relative(workflow), job.getKey(), artifact, moduleDirectory));
                    }
                }
            }
        }

        assertThat(guardedJobs)
                .describedAs("no job under %s runs '%s' — this guard is reading a workflow shape "
                                + "that moved, so it is no longer guarding anything",
                        relative(WORKFLOWS), BENCHMARK_BUILD)
                .isPositive();
        assertThat(uninstalled)
                .describedAs("a benchmark job resolves a sibling module from Maven Central. The "
                        + "templates 'tests' jar is never published, so such a job fails at "
                        + "dependency resolution — and on the JMH workflow it fails weekly, off "
                        + "the pull request that introduced it")
                .isEmpty();
    }

    /** The artifact ids of the {@link #GROUP} dependencies {@link #BENCHMARKS_POM} declares. */
    private static Set<String> firstPartyDependenciesOfTheBenchmarks() throws IOException {
        Set<String> artifacts = new LinkedHashSet<>();
        Matcher dependency = DEPENDENCY.matcher(markup(BENCHMARKS_POM));
        while (dependency.find()) {
            String block = dependency.group(1);
            if (first(GROUP_ID, block).filter(GROUP::equals).isPresent()) {
                first(ARTIFACT_ID, block).ifPresent(artifacts::add);
            }
        }
        return artifacts;
    }

    /** Every reactor module, keyed by the artifact id its own pom declares. */
    private static Map<String, String> moduleDirectoriesByArtifactId() throws IOException {
        Map<String, String> byArtifactId = new LinkedHashMap<>();
        Matcher module = MODULE.matcher(markup(ROOT_POM));
        while (module.find()) {
            String directory = module.group(1);
            String pom = PARENT.matcher(markup(PROJECT_ROOT.resolve(directory).resolve("pom.xml")))
                    .replaceFirst("");
            first(ARTIFACT_ID, pom).ifPresent(artifactId -> byArtifactId.put(artifactId, directory));
        }
        return byArtifactId;
    }

    /**
     * Whether {@code step} installs {@code artifactId} from source, by module pom
     * ({@code -f templates/pom.xml … install}) or by reactor selector
     * ({@code -pl :graph-compose-core … install}).
     */
    private static boolean installs(String step, String artifactId, String moduleDirectory) {
        String executable = COMMENT_LINE.matcher(step).replaceAll("");
        return executable.contains("install")
                && (executable.contains("-f " + moduleDirectory + "/pom.xml")
                || Pattern.compile("-pl [^\\r\\n]*:" + Pattern.quote(artifactId) + "(?![\\w-])")
                        .matcher(executable).find());
    }

    private static int indexOfTheBenchmarkBuild(List<String> steps) {
        for (int i = 0; i < steps.size(); i++) {
            if (COMMENT_LINE.matcher(steps.get(i)).replaceAll("").contains(BENCHMARK_BUILD)) {
                return i;
            }
        }
        return -1;
    }

    /** The jobs of one workflow, in file order, keyed by job id. */
    private static Map<String, String> jobs(String yaml) {
        Matcher region = JOBS_REGION.matcher(yaml);
        if (!region.find()) {
            return Map.of();
        }
        String jobs = region.group(1);

        Map<String, String> byId = new LinkedHashMap<>();
        Matcher key = JOB_KEY.matcher(jobs);
        List<String> ids = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        while (key.find()) {
            ids.add(unquoted(key.group(1)));
            starts.add(key.start());
        }
        for (int i = 0; i < ids.size(); i++) {
            int end = i + 1 < ids.size() ? starts.get(i + 1) : jobs.length();
            byId.put(ids.get(i), jobs.substring(starts.get(i), end));
        }
        return byId;
    }

    /** The steps of one job, in run order. Text before the first step is job configuration. */
    private static List<String> steps(String job) {
        List<Integer> starts = new ArrayList<>();
        Matcher boundary = STEP_BOUNDARY.matcher(job);
        while (boundary.find()) {
            starts.add(boundary.start());
        }
        List<String> steps = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int end = i + 1 < starts.size() ? starts.get(i + 1) : job.length();
            steps.add(job.substring(starts.get(i), end));
        }
        return steps;
    }

    /**
     * Every workflow in the directory, under either spelling GitHub accepts. A guard
     * that only reads {@code .yml} is blind to a job written as {@code .yaml} — the
     * one way this check could go green over a workflow it never opened.
     */
    private static List<Path> workflows() throws IOException {
        try (Stream<Path> files = Files.list(WORKFLOWS)) {
            return files.filter(file -> {
                String name = file.getFileName().toString();
                return name.endsWith(".yml") || name.endsWith(".yaml");
            }).sorted().toList();
        }
    }

    private static String markup(Path pom) throws IOException {
        return XML_COMMENT.matcher(Files.readString(pom)).replaceAll("");
    }

    private static Optional<String> first(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static String unquoted(String key) {
        String trimmed = key.trim();
        boolean quoted = trimmed.length() > 1
                && (trimmed.startsWith("\"") && trimmed.endsWith("\"")
                || trimmed.startsWith("'") && trimmed.endsWith("'"));
        return quoted ? trimmed.substring(1, trimmed.length() - 1) : trimmed;
    }

    private static String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
