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
import java.util.List;
import java.util.Map;
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

    /** A job-level {@code if:} — four-space indent, first line only. */
    private static final Pattern JOB_IF = Pattern.compile("(?m)^    if: (.*)$");

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

    /**
     * An execution that is gone, unbound from {@code verify}, or pointed at another
     * baseline stops a diff from running. A gate that reports a break without failing, or
     * carries a skip switch, still runs and protects nothing. Every one of them leaves CI
     * green.
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
                    .describedAs("%s: the japicmp gate must fail the build on a binary break, "
                            + "everywhere it is configured — reporting one protects nothing", where)
                    .isNotEmpty()
                    .containsOnly("true");
            assertThat(descendantTexts(plugin, "skip"))
                    .describedAs("%s: the japicmp gate must carry no skip switch", where)
                    .isEmpty();
        }
    }

    /**
     * A baseline the templates gate cannot resolve fails the build.
     *
     * <p>japicmp defaults {@code ignoreMissingOldVersion} to {@code true}: a pin naming a
     * mistyped or unpublished version, or one the repository cannot serve, is logged as a
     * warning, the diff is skipped, and the build passes having compared nothing. So the
     * safe setting has to be present — its absence is the unsafe state — and
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
     * The pull-request job diffs every gated module, and runs whenever one of them changes.
     *
     * <p>Three edits each take a module off that path without failing anything: its
     * artifact dropped from the job's {@code -pl} list; its paths dropped from the filter
     * the job's condition reads; or that filter not exported from the {@code changes}
     * job — an output that is not declared reads as empty, so the condition is never true
     * for the module, and a skipped job passes.</p>
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

        assertThat(invocation)
                .describedAs("ci.yml job '%s' must not switch the gate off", PR_JOB)
                .doesNotContain("japicmp.skip");
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
     * direct push reaches without the pull-request job.
     */
    @Test
    void everyGatedModuleIsDiffedBeforeTheTagAndBeforeThePublish() throws Exception {
        String releaseArgs = firstGroup(read(RELEASE_SCRIPT), RELEASE_GATE_ARGS, "scripts/cut-release.ps1");
        List<String> publishGates = allGroups(read(PUBLISH), GATE_RUN);
        assertThat(publishGates)
                .describedAs("publish.yml runs no japicmp gate before it deploys")
                .isNotEmpty()
                .noneMatch(run -> run.contains("japicmp.skip"));
        assertThat(releaseArgs)
                .describedAs("cut-release.ps1 Step 5b must not switch the gate off")
                .doesNotContain("japicmp.skip");

        for (String module : gatedModules()) {
            String artifact = artifactIdOf(module);
            assertThat(releaseArgs)
                    .describedAs("cut-release.ps1 Step 5b must diff %s before the tag is cut", artifact)
                    .contains(":" + artifact);
            assertThat(publishGates)
                    .describedAs("publish.yml must diff %s on the tagged commit before it deploys", artifact)
                    .anyMatch(run -> run.contains("-f " + module + "/pom.xml"));
        }
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

    /** Whether a change to {@code module}'s main sources, and one to its pom, both match a glob. */
    private static boolean watches(List<String> globs, String module) {
        return matchesAny(globs, module + "/src/main/java/Probe.java")
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
