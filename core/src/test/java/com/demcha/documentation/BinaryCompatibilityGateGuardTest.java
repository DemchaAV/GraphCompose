package com.demcha.documentation;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
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
 * Guards the wiring that makes japicmp a gate rather than a report.
 *
 * <p>The binary-compatibility check is a Maven profile, so what it protects is decided
 * outside the code it protects: executions in a pom, a module list in a workflow, a
 * path filter, a release-script argument. Each can be dropped by an edit that reads as
 * tidying, and none of those edits turns anything red — the job diffs less and still
 * reports success. Deleting a Stable method is caught by the diff; deleting the diff is
 * caught here.</p>
 *
 * <p>A list of forbidden edits is never complete, so the paths that run the gate carry
 * their own proof: each ends by checking that every execution left its XML report. An
 * execution that does not run — switched off, unbound, or not selected — never writes
 * one. A baseline japicmp cannot resolve is the exception: the plugin still writes a
 * report for it, which is why the templates gate must fail on that case instead.</p>
 *
 * <p>The other half is where the baseline comes from. japicmp resolves a pin that equals
 * the module's own version to the artifact this build just produced — measured, from the
 * reactor and from the local repository alike — so the pins are held strictly older than
 * the working version by {@code VersionConsistencyGuardTest}, every path drops our
 * cached artifacts before it resolves, and the publish workflow runs the gate before it
 * installs the release it is about to publish.</p>
 *
 * <p>The gated modules are discovered, as every module pom that declares a
 * {@code japicmp} profile, so a module joining the gate is held to the same wiring the
 * day it does. {@link #everyJapicmpExecutionTheGateReliesOnStillRuns} names the
 * executions that must exist today, so deleting a profile outright cannot shrink the
 * discovered set without failing.</p>
 */
class BinaryCompatibilityGateGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();
    private static final Path CI = PROJECT_ROOT.resolve(".github/workflows/ci.yml");
    private static final Path PUBLISH = PROJECT_ROOT.resolve(".github/workflows/publish.yml");
    private static final Path RELEASE_SCRIPT = PROJECT_ROOT.resolve("scripts/cut-release.ps1");

    /** The ci.yml job that runs the gate on pull requests. */
    private static final String PR_JOB = "binary-compat";

    /** The per-element marker: the one exclusion the templates gate may carry. */
    private static final String INTERNAL_MARKER = "@com.demcha.compose.document.api.Internal";

    /** The property japicmp reads its skip switch from. */
    private static final String SKIP_PROPERTY = "japicmp.skip";

    /** The property the templates gate reads its break-the-build setting from. */
    private static final String BREAK_PROPERTY = "${japicmp.break.binary}";

    /** How {@code cut-release.ps1} Step 5b runs the gate. */
    private static final String RELEASE_GATE_RUN = "& $mvnw @japicmpArgs";

    /** The artifacts a gate path must drop from the local repository before it resolves. */
    private static final List<String> CACHED_ARTIFACTS =
            List.of("graph-compose-core", "graph-compose-templates");

    /**
     * Each execution the gate relies on, per pom, with the baseline property its
     * {@code <oldVersion>} must read.
     */
    private static final Map<String, Map<String, String>> REQUIRED_EXECUTIONS = Map.of(
            "core/pom.xml", Map.of("japicmp-against-baseline", "japicmp.baseline"),
            "templates/pom.xml", Map.of(
                    "japicmp-against-major-floor", "japicmp.baseline.floor",
                    "japicmp-against-previous-release", "japicmp.baseline.previous"));

    /** A {@code run:} line that invokes the gate. */
    private static final Pattern GATE_RUN = Pattern.compile("(?m)^\\s+run: (.*-P japicmp.*)$");

    /** The publish workflow's install step, which seeds the repository with this release. */
    private static final Pattern PUBLISH_INSTALL =
            Pattern.compile("(?m)^\\s+run: \\./mvnw .*clean install\\s*$");

    /** A job-level {@code if:} — four-space indent, first line only. */
    private static final Pattern JOB_IF = Pattern.compile("(?m)^    if: (.*)$");

    /** A job-level inline {@code needs: [a, b]} flow sequence. */
    private static final Pattern JOB_NEEDS = Pattern.compile("(?m)^    needs: \\[([^]]*)]");

    /** A {@code changes} output that a job condition tests. */
    private static final Pattern CHANGE_OUTPUT =
            Pattern.compile("needs\\.changes\\.outputs\\.([A-Za-z_][\\w-]*) == 'true'");

    /** An output the {@code changes} job exports, and the path filter it exports. */
    private static final Pattern EXPORTED_OUTPUT = Pattern.compile(
            "(?m)^      ([A-Za-z_][\\w-]*): \\$\\{\\{ steps\\.filter\\.outputs\\.([A-Za-z_][\\w-]*) }}\\s*$");

    /** A filter name, and one of its globs, inside the {@code filters: |} literal. */
    private static final Pattern FILTER_KEY = Pattern.compile("^            ([A-Za-z_][\\w-]*):\\s*$");
    private static final Pattern FILTER_GLOB = Pattern.compile("^              - '([^']+)'\\s*$");

    /** The argument array {@code cut-release.ps1} Step 5b hands to Maven. */
    private static final Pattern RELEASE_GATE_ARGS =
            Pattern.compile("(?m)^\\s*\\$japicmpArgs = @\\((.*)\\)\\s*$");

    /** Step 5b's loop deleting the reports, so none left by an earlier run can stand in. */
    private static final Pattern REPORTS_CLEARED = Pattern.compile(
            "(?m)^\\s*foreach \\(\\$report in \\$japicmpReports\\) \\{\\s*Remove-Item\\b");

    /** Step 5b's loop that throws when a report is missing after the gate ran. */
    private static final Pattern REPORTS_CHECKED = Pattern.compile(
            "(?m)^\\s*foreach \\(\\$report in \\$japicmpReports\\) \\{\\s*if \\(-not \\(Test-Path\\b[^\\n]*\\n\\s*throw\\b");

    /** Step 5b's loop dropping the pinned baselines from the local repository. */
    private static final Pattern BASELINES_DROPPED = Pattern.compile(
            "(?m)^\\s*foreach \\(\\$baseline in \\$japicmpBaselines\\) \\{[^\\n]*\\n[^\\n]*\\n\\s*Remove-Item\\b");

    /**
     * An execution that is gone, unbound from {@code verify}, or pointed at another
     * baseline stops a diff from running. A gate that reports a break without failing,
     * carries a skip switch, or reclassifies a break as compatible still runs and protects
     * nothing. Every one of them leaves CI green.
     */
    @Test
    void everyJapicmpExecutionTheGateReliesOnStillRuns() throws Exception {
        for (Map.Entry<String, Map<String, String>> pom : REQUIRED_EXECUTIONS.entrySet()) {
            String where = pom.getKey();
            Element plugin = japicmpPlugin(PROJECT_ROOT.resolve(where));
            Map<String, Element> executions = executionsById(plugin);

            for (Map.Entry<String, String> required : pom.getValue().entrySet()) {
                String id = required.getKey();
                String baseline = "${" + required.getValue() + "}";
                Element execution = executions.get(id);
                assertThat(execution)
                        .describedAs("%s must keep the japicmp execution '%s' — without it the diff "
                                + "against %s stops running and the job stays green", where, id, baseline)
                        .isNotNull();
                assertThat(textOf(directChild(execution, "phase")))
                        .describedAs("%s execution '%s' must stay bound to verify, the phase every "
                                + "path that runs the gate invokes", where, id)
                        .isEqualTo("verify");
                assertThat(childTexts(directChild(execution, "goals"), "goal"))
                        .describedAs("%s execution '%s' must run japicmp's cmp goal", where, id)
                        .contains("cmp");
                assertThat(oldVersionOf(plugin, execution))
                        .describedAs("%s execution '%s' must diff against %s", where, id, baseline)
                        .contains(baseline);
            }

            assertThat(descendantTexts(plugin, "breakBuildOnBinaryIncompatibleModifications"))
                    .describedAs("%s: the japicmp gate must fail the build on a binary break — either "
                            + "always (true) or through %s, whose value VersionConsistencyGuardTest "
                            + "derives from the CHANGELOG. Reporting one protects nothing", where, BREAK_PROPERTY)
                    .isNotEmpty()
                    .allMatch(value -> value.equals("true") || value.equals(BREAK_PROPERTY));
            assertThat(descendantTexts(plugin, "skip"))
                    .describedAs("%s: the japicmp gate must carry no skip switch", where)
                    .isEmpty();
            assertThat(descendantTexts(plugin, "overrideCompatibilityChangeParameters"))
                    .describedAs("%s: overrideCompatibilityChangeParameters can declare a binary break "
                            + "compatible, which passes it", where)
                    .isEmpty();
        }
    }

    /**
     * A baseline the templates gate cannot resolve fails the build.
     *
     * <p>japicmp defaults {@code ignoreMissingOldVersion} to {@code true}: a pin naming a
     * mistyped or unpublished version, or one the repository cannot serve, is logged as a
     * warning, the diff is skipped, and the build passes having compared nothing — with a
     * report written all the same, so the report checks cannot see it. So the safe
     * setting has to be present — its absence is the unsafe state — and
     * {@code ignoreNonResolvableArtifacts}, which skips the same way, must not switch it
     * back.</p>
     */
    @Test
    void theTemplatesGateFailsWhenABaselineCannotBeResolved() throws Exception {
        Element plugin = japicmpPlugin(PROJECT_ROOT.resolve("templates/pom.xml"));

        assertThat(descendantTexts(plugin, "ignoreMissingOldVersion"))
                .describedAs("templates/pom.xml must set ignoreMissingOldVersion to false: japicmp "
                        + "defaults it to true, which skips a baseline it cannot resolve and passes")
                .isNotEmpty()
                .containsOnly("false");
        assertThat(descendantTexts(plugin, "ignoreNonResolvableArtifacts"))
                .describedAs("templates/pom.xml: ignoreNonResolvableArtifacts skips an unresolvable "
                        + "baseline just as ignoreMissingOldVersion does")
                .noneMatch(value -> value.equalsIgnoreCase("true"));
    }

    /**
     * Every {@code templates.*} package is Stable ({@code docs/api-stability.md}), so the
     * templates gate may narrow itself by nothing but the per-element {@code @Internal}
     * marker. An excluded package or class, or an include list naming part of the
     * module, would let a Stable break ship with the gate green.
     */
    @Test
    void theTemplatesGateCoversTheWholeStableSurface() throws Exception {
        Element plugin = japicmpPlugin(PROJECT_ROOT.resolve("templates/pom.xml"));

        assertThat(descendantTexts(plugin, "exclude"))
                .describedAs("templates/pom.xml: the japicmp gate may exclude only %s — every "
                        + "templates.* package is Stable per docs/api-stability.md", INTERNAL_MARKER)
                .isSubsetOf(List.of(INTERNAL_MARKER));
        assertThat(descendantTexts(plugin, "include"))
                .describedAs("templates/pom.xml: an include list narrows the japicmp gate to part "
                        + "of the Stable surface")
                .isEmpty();
    }

    /**
     * Nothing the build reads sets japicmp's skip switch.
     *
     * <p>The plugin reads {@code skip} from the {@code japicmp.skip} property, so one line in
     * a pom's {@code <properties>}, in {@code .mvn/maven.config}, or on a command line turns
     * the executions off on every path that runs the gate. Those paths' report checks would
     * still fail at run time; this names the cause before anything runs.</p>
     */
    @Test
    void nothingTheBuildReadsSwitchesTheGateOff() throws Exception {
        List<Path> readByTheBuild = new ArrayList<>(List.of(
                PROJECT_ROOT.resolve("pom.xml"), CI, PUBLISH, RELEASE_SCRIPT));
        for (String module : gatedModules()) {
            readByTheBuild.add(PROJECT_ROOT.resolve(module + "/pom.xml"));
        }
        for (String config : List.of(".mvn/maven.config", ".mvn/jvm.config")) {
            Path file = PROJECT_ROOT.resolve(config);
            if (Files.isRegularFile(file)) {
                readByTheBuild.add(file);
            }
        }

        for (Path file : readByTheBuild) {
            assertThat(read(file))
                    .describedAs("%s must not mention %s: it switches japicmp's executions off, and an "
                            + "execution that does not run fails nothing", relative(file), SKIP_PROPERTY)
                    .doesNotContain(SKIP_PROPERTY);
        }
    }

    /**
     * No path lets an artifact of ours stand in for a published baseline.
     *
     * <p>japicmp resolves a pin equal to the module's own version to the artifact the
     * build just produced, from the reactor as readily as from the local repository, and
     * then reports no differences. {@code VersionConsistencyGuardTest} keeps the pins
     * strictly older than the working version, which is what makes that impossible; these
     * are the second line. Every path drops our cached artifacts before it resolves, so a
     * baseline comes from Central rather than from a copy sitting in the repository, and
     * the publish workflow runs the gate <em>before</em> the install that seeds the
     * repository with the release it is about to publish.</p>
     */
    @Test
    void noPathLetsThisBuildSupplyItsOwnBaseline() throws Exception {
        Map<String, String> jobs = CiGateCoverageGuardTest.jobBlocks(Files.readString(CI));
        String job = jobs.get(PR_JOB);
        assertThat(job).describedAs("ci.yml has no '%s' job", PR_JOB).isNotNull();
        String publish = read(PUBLISH);
        String script = read(RELEASE_SCRIPT);

        for (String artifact : CACHED_ARTIFACTS) {
            assertThat(dropsCached(artifact).matcher(job).find())
                    .describedAs("ci.yml job '%s' must drop the cached %s before the gate resolves its "
                            + "baselines, so a copy in the repository cannot stand in for the published "
                            + "release", PR_JOB, artifact)
                    .isTrue();
            assertThat(dropsCached(artifact).matcher(publish).find())
                    .describedAs("publish.yml must drop the cached %s before the gate resolves its "
                            + "baselines", artifact)
                    .isTrue();
        }

        int drops = firstIndexOf(job, dropsCached(CACHED_ARTIFACTS.get(0)));
        int diffs = firstIndexOf(job, GATE_RUN);
        assertThat(drops)
                .describedAs("ci.yml job '%s' must drop the cached artifacts before it runs the gate, "
                        + "not after", PR_JOB)
                .isLessThan(diffs);

        int publishDrops = firstIndexOf(publish, dropsCached(CACHED_ARTIFACTS.get(0)));
        int publishDiffs = firstIndexOf(publish, GATE_RUN);
        int publishInstalls = firstIndexOf(publish, PUBLISH_INSTALL);
        assertThat(publishDiffs)
                .describedAs("publish.yml must run the japicmp gate BEFORE `clean install`: that install "
                        + "puts the release being published into the local repository, where a pin equal "
                        + "to it would resolve, and the gate would compare the release with itself")
                .isGreaterThan(publishDrops)
                .isLessThan(publishInstalls);

        int scriptDrops = firstIndexOf(script, BASELINES_DROPPED);
        int scriptRuns = script.indexOf(RELEASE_GATE_RUN);
        assertThat(scriptDrops)
                .describedAs("cut-release.ps1 Step 5b must drop the pinned baselines from the local "
                        + "repository before it runs the gate — Step 4 installed the just-bumped version "
                        + "there")
                .isNotNegative()
                .isLessThan(scriptRuns);
    }

    /**
     * The pull-request job diffs every gated module, runs whenever one of them changes,
     * and proves each diff ran.
     *
     * <p>Each of these takes a module off that path without failing anything: its
     * artifact dropped from the job's {@code -pl} list; its paths dropped from the filter
     * the job's condition reads; that filter not exported from the {@code changes} job, or
     * {@code changes} dropped from the job's {@code needs} — an output the job cannot read
     * is empty, so the condition is never true, and a skipped job passes; the job told to
     * tolerate its own failure; or a report check removed or commented out, so an
     * execution that does not run for any other reason goes unnoticed.</p>
     */
    @Test
    void everyGatedModuleIsDiffedOnThePullRequestsThatTouchIt() throws Exception {
        Map<String, String> jobs = CiGateCoverageGuardTest.jobBlocks(Files.readString(CI));
        String job = jobs.get(PR_JOB);
        String changes = jobs.get("changes");
        assertThat(job).describedAs("ci.yml has no '%s' job to run the gate on pull requests", PR_JOB).isNotNull();
        assertThat(changes).describedAs("ci.yml has no 'changes' job for the gate's condition to read").isNotNull();

        String invocation = firstGroup(job, GATE_RUN, "ci.yml job '" + PR_JOB + "'");
        String condition = firstGroup(job, JOB_IF, "ci.yml job '" + PR_JOB + "'");
        Map<String, String> exported = exportedOutputs(changes);
        Map<String, List<String>> filters = pathFilters(changes);

        assertThat(needsOf(job))
                .describedAs("ci.yml job '%s' must need 'changes': without it every "
                        + "needs.changes.outputs value reads empty, the condition is never true, and "
                        + "a job that never runs never fails", PR_JOB)
                .contains("changes");
        assertThat(job)
                .describedAs("ci.yml job '%s' must not tolerate its own failure", PR_JOB)
                .doesNotContain("continue-on-error");
        assertThat(invocation)
                .describedAs("ci.yml job '%s' must run verify, the phase the japicmp executions are "
                        + "bound to", PR_JOB)
                .contains(" verify");
        for (String report : requiredReports()) {
            assertThat(reportCheck(report).matcher(job).find())
                    .describedAs("ci.yml job '%s' must prove %s was written, as a `test -s` line of its "
                            + "own — an execution that does not run writes no report and fails nothing",
                            PR_JOB, report)
                    .isTrue();
        }

        for (String module : gatedModules()) {
            String artifact = artifactIdOf(module);
            assertThat(invocation)
                    .describedAs("ci.yml job '%s' must diff %s: its -pl list decides which modules "
                            + "the gate sees", PR_JOB, artifact)
                    .contains(":" + artifact);

            List<String> triggers = new ArrayList<>();
            Matcher read = CHANGE_OUTPUT.matcher(condition);
            while (read.find()) {
                String filter = exported.get(read.group(1));
                if (filter != null && watches(filters.getOrDefault(filter, List.of()), module)) {
                    triggers.add(read.group(1));
                }
            }
            assertThat(triggers)
                    .describedAs("ci.yml job '%s' must run when %s changes: its condition has to read "
                            + "a 'changes' output that is exported and whose filter matches both "
                            + "%s/src/** and %s/pom.xml — otherwise a pull request touching only that "
                            + "module skips the gate, and a skipped job passes", PR_JOB, module, module, module)
                    .isNotEmpty();
        }
    }

    /**
     * The release script diffs every gated module before the tag is cut, and the publish
     * workflow diffs each one on the tagged commit before it deploys — the two paths a
     * direct push reaches without the pull-request job. Both run the profile at
     * {@code verify}, and both end by proving every execution left its report; the script
     * deletes the reports first, so one left by an earlier run cannot stand in.
     */
    @Test
    void everyGatedModuleIsDiffedBeforeTheTagAndBeforeThePublish() throws Exception {
        String script = read(RELEASE_SCRIPT);
        String publish = read(PUBLISH);
        String releaseArgs = firstGroup(script, RELEASE_GATE_ARGS, "scripts/cut-release.ps1");
        List<String> publishGates = allGroups(publish, GATE_RUN);

        assertThat(publishGates)
                .describedAs("publish.yml must run the japicmp profile at verify, the phase its "
                        + "executions are bound to, before it deploys")
                .isNotEmpty()
                .allMatch(run -> run.contains(" verify"));
        assertThat(releaseArgs)
                .describedAs("cut-release.ps1 Step 5b must activate the japicmp profile and run "
                        + "verify, the phase its executions are bound to")
                .contains("'-P', 'japicmp'")
                .contains("'verify'");
        for (String report : requiredReports()) {
            assertThat(script)
                    .describedAs("cut-release.ps1 Step 5b must list %s among the reports it checks", report)
                    .contains("'" + report + "'");
            assertThat(reportCheck(report).matcher(publish).find())
                    .describedAs("publish.yml must prove %s was written before it deploys, as a "
                            + "`test -s` line of its own", report)
                    .isTrue();
        }

        int cleared = firstIndexOf(script, REPORTS_CLEARED);
        int ran = script.indexOf(RELEASE_GATE_RUN);
        int checked = firstIndexOf(script, REPORTS_CHECKED);
        assertThat(cleared)
                .describedAs("cut-release.ps1 Step 5b must delete the reports before the gate runs, "
                        + "or one left by an earlier run stands in for this one")
                .isNotNegative();
        assertThat(checked)
                .describedAs("cut-release.ps1 Step 5b must check the reports after the gate runs and "
                        + "throw when one is missing")
                .isNotNegative();
        assertThat(ran)
                .describedAs("cut-release.ps1 Step 5b must delete the reports, run the gate "
                        + "(%s), then check them — in that order", RELEASE_GATE_RUN)
                .isGreaterThan(cleared)
                .isLessThan(checked);

        for (String module : gatedModules()) {
            String artifact = artifactIdOf(module);
            assertThat(releaseArgs)
                    .describedAs("cut-release.ps1 Step 5b must diff %s before the tag is cut", artifact)
                    .contains(":" + artifact);
            assertThat(publishGates)
                    .describedAs("publish.yml must diff %s on the tagged commit before it deploys", artifact)
                    .anyMatch(run -> run.contains(":" + artifact));
        }
    }

    /** The XML report each required execution writes: {@code <module>/target/japicmp/<id>.xml}. */
    private static List<String> requiredReports() {
        List<String> reports = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> pom : REQUIRED_EXECUTIONS.entrySet()) {
            String module = pom.getKey().substring(0, pom.getKey().indexOf('/'));
            for (String execution : pom.getValue().keySet()) {
                reports.add(module + "/target/japicmp/" + execution + ".xml");
            }
        }
        return reports;
    }

    /** An uncommented shell line that fails unless {@code report} exists and is not empty. */
    private static Pattern reportCheck(String report) {
        return Pattern.compile("(?m)^\\s+test -s " + Pattern.quote(report) + "\\s*$");
    }

    /** An uncommented shell line that deletes our cached {@code artifact} from the local repository. */
    private static Pattern dropsCached(String artifact) {
        return Pattern.compile("(?m)^\\s+rm -rf [^\\n]*/io/github/demchaav/" + Pattern.quote(artifact) + "\\s*$");
    }

    /** Every module directory whose pom declares a {@code japicmp} profile, sorted. */
    private static List<String> gatedModules() throws Exception {
        List<Path> poms;
        try (Stream<Path> entries = Files.list(PROJECT_ROOT)) {
            poms = entries.filter(Files::isDirectory)
                    .map(module -> module.resolve("pom.xml"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        }
        List<String> gated = new ArrayList<>();
        for (Path pom : poms) {
            if (japicmpProfile(pom) != null) {
                gated.add(pom.getParent().getFileName().toString());
            }
        }
        assertThat(gated)
                .describedAs("no module pom declares a 'japicmp' profile — the discovery broke, or the "
                        + "gate is gone; either way every check that reads this list passes over nothing")
                .isNotEmpty();
        return gated;
    }

    private static Element japicmpProfile(Path pom) throws Exception {
        for (Element profile : children(directChild(parse(pom), "profiles"), "profile")) {
            if ("japicmp".equals(textOf(directChild(profile, "id")))) {
                return profile;
            }
        }
        return null;
    }

    private static Element japicmpPlugin(Path pom) throws Exception {
        Element profile = japicmpProfile(pom);
        assertThat(profile)
                .describedAs("%s declares no 'japicmp' profile", relative(pom))
                .isNotNull();
        for (Element plugin : children(directChild(directChild(profile, "build"), "plugins"), "plugin")) {
            if ("japicmp-maven-plugin".equals(textOf(directChild(plugin, "artifactId")))) {
                return plugin;
            }
        }
        throw new AssertionError(relative(pom) + ": the 'japicmp' profile declares no japicmp-maven-plugin");
    }

    private static Map<String, Element> executionsById(Element plugin) {
        Map<String, Element> byId = new LinkedHashMap<>();
        for (Element execution : children(directChild(plugin, "executions"), "execution")) {
            byId.put(textOf(directChild(execution, "id")), execution);
        }
        return byId;
    }

    /**
     * The {@code <oldVersion>} an execution diffs against: its own when it declares one,
     * which is what Maven does when it merges an execution's configuration over the
     * plugin's — otherwise the plugin-level one.
     */
    private static String oldVersionOf(Element plugin, Element execution) {
        Element own = directChild(directChild(execution, "configuration"), "oldVersion");
        Element effective = own != null ? own : directChild(directChild(plugin, "configuration"), "oldVersion");
        return effective == null ? "" : effective.getTextContent();
    }

    private static String artifactIdOf(String module) throws Exception {
        return textOf(directChild(parse(PROJECT_ROOT.resolve(module + "/pom.xml")), "artifactId"));
    }

    /** The job ids an inline {@code needs: [...]} names; empty when there is none. */
    private static Set<String> needsOf(String jobBlock) {
        Set<String> ids = new LinkedHashSet<>();
        Matcher needs = JOB_NEEDS.matcher(jobBlock);
        if (needs.find()) {
            for (String id : needs.group(1).split(",")) {
                if (!id.isBlank()) {
                    ids.add(id.trim());
                }
            }
        }
        return ids;
    }

    /** Output name to the path filter it exports, from the {@code changes} job's {@code outputs:}. */
    private static Map<String, String> exportedOutputs(String changesJob) {
        Map<String, String> exported = new LinkedHashMap<>();
        Matcher output = EXPORTED_OUTPUT.matcher(changesJob);
        while (output.find()) {
            exported.put(output.group(1), output.group(2));
        }
        return exported;
    }

    /** Filter name to its globs, from the {@code filters: |} literal of the {@code changes} job. */
    private static Map<String, List<String>> pathFilters(String changesJob) {
        Map<String, List<String>> filters = new LinkedHashMap<>();
        List<String> current = null;
        for (String line : changesJob.split("\n")) {
            Matcher key = FILTER_KEY.matcher(line);
            Matcher glob = FILTER_GLOB.matcher(line);
            if (key.matches()) {
                current = new ArrayList<>();
                filters.put(key.group(1), current);
            } else if (glob.matches() && current != null) {
                current.add(glob.group(1));
            }
        }
        return filters;
    }

    /**
     * Whether a change to a source file deep inside {@code module}'s main tree, and one to
     * its pom, both match a glob. The probe sits in a package, as every real source does:
     * a filter that only reaches the top of {@code src/main/java} would pass a shallow probe
     * while missing every file that matters.
     */
    private static boolean watches(List<String> globs, String module) {
        return matchesAny(globs, module + "/src/main/java/com/demcha/compose/Probe.java")
                && matchesAny(globs, module + "/pom.xml");
    }

    private static boolean matchesAny(List<String> globs, String path) {
        for (String glob : globs) {
            if (path.matches(globToRegex(glob))) {
                return true;
            }
        }
        return false;
    }

    /** The glob shapes the workflow uses: {@code **} crosses directories, {@code *} stays inside one. */
    private static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*' && i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                regex.append(".*");
                i++;
            } else if (c == '*') {
                regex.append("[^/]*");
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return regex.toString();
    }

    private static String firstGroup(String text, Pattern pattern, String where) {
        Matcher matcher = pattern.matcher(text);
        assertThat(matcher.find())
                .describedAs("%s no longer matches /%s/ — the gate wiring this guard reads has moved, "
                        + "so it is no longer guarding anything", where, pattern.pattern())
                .isTrue();
        return matcher.group(1);
    }

    private static List<String> allGroups(String text, Pattern pattern) {
        List<String> groups = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            groups.add(matcher.group(1));
        }
        return groups;
    }

    private static int firstIndexOf(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.start() : -1;
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file).replace("\r\n", "\n");
    }

    private static Element parse(Path pom) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> found = new ArrayList<>();
        if (parent == null) {
            return found;
        }
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
                found.add((Element) node);
            }
        }
        return found;
    }

    private static Element directChild(Element parent, String name) {
        List<Element> found = children(parent, name);
        return found.isEmpty() ? null : found.get(0);
    }

    private static List<String> childTexts(Element parent, String name) {
        return children(parent, name).stream().map(BinaryCompatibilityGateGuardTest::textOf).toList();
    }

    private static List<String> descendantTexts(Element root, String name) {
        List<String> texts = new ArrayList<>();
        NodeList nodes = root.getElementsByTagName(name);
        for (int i = 0; i < nodes.getLength(); i++) {
            texts.add(nodes.item(i).getTextContent().trim());
        }
        return texts;
    }

    private static String textOf(Element element) {
        return element == null ? null : element.getTextContent().trim();
    }

    private static String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
