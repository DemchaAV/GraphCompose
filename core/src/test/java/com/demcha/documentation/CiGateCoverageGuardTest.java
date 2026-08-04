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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards that the aggregate CI status check watches every job that can run on a
 * pull request.
 *
 * <p>{@code CI Gate} exists so branch protection has one stable check to require
 * instead of matrix legs the path filter sometimes skips. It fails when a job it
 * {@code needs} reports {@code failure} or {@code cancelled}; a job the filter
 * legitimately skipped counts as success, which is the whole point.</p>
 *
 * <p>That design has one hole, and it is not hypothetical. {@code changes} — the
 * path-detection job every heavy job gates on — was absent from the gate's
 * {@code needs}. When its {@code git fetch} returned HTTP 503 the job failed, the
 * four reactor jobs downstream resolved to {@code skipped} rather than
 * {@code failure}, and the gate reported success over a run that compiled nothing.
 * Both checks branch protection is pointed at were green with zero tests
 * executed.</p>
 *
 * <p>So the rule is: every job in the workflow is either aggregated by the gate or
 * demonstrably cannot run on a pull request. The second case is derived from the
 * job's own {@code if:} condition rather than a list somebody maintains, so a new
 * job joins the gate or fails this test — it cannot slip through by being
 * forgotten.</p>
 */
class CiGateCoverageGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();
    private static final Path WORKFLOW = PROJECT_ROOT.resolve(".github/workflows/ci.yml");

    /** The aggregate check. It cannot depend on itself. */
    private static final String GATE = "ci-gate";

    /**
     * A job key: a two-space-indented mapping key under {@code jobs:}, whatever
     * follows the colon.
     *
     * <p>The key is matched <em>structurally</em> — anything YAML puts at that
     * level — rather than against GitHub's identifier grammar, and the difference
     * is the whole point. A pattern that only admits legal job ids silently drops
     * everything else, and a job this guard never sees is one it cannot report
     * missing from the gate: absent from {@code needs}, test green, aggregate check
     * blind to it. Matching everything and judging afterwards means an id the guard
     * cannot interpret becomes a failure rather than an omission — see
     * {@link #everyJobIdIsOneGitHubWouldAccept}.</p>
     *
     * <p>Accepting any tail after the colon is what admits a job written as an
     * inline mapping ({@code deploy: {runs-on: ubuntu-latest}}). GitHub documents
     * the value of {@code jobs.<job_id>} as "a map of the job's configuration
     * data", and an inline mapping is a map; requiring the line to end at the colon
     * excluded a legal spelling and hid any job using it. YAML only treats a colon
     * as a key separator when a space or a line end follows it, which is what the
     * lookahead encodes.</p>
     *
     * <p>Quotes around the key are YAML's, not part of the id, and
     * {@link #unquoted(String)} strips them. Left on, {@code "build-and-test":}
     * produced an id that could never match the bare name in {@code needs}, so a
     * legal workflow was reported as having an unwatched job.</p>
     *
     * <p>{@link #jobBlocks(String)} is package-private so
     * {@code CiGateCoverageGuardParsingTest} can drive it with the spellings this
     * repository's own workflow does not contain.</p>
     */
    private static final Pattern JOB_KEY =
            Pattern.compile("(?m)^  (?!#)(\\S[^:]*?):(?=[ \\t]|$)[^\\r\\n]*$");

    /**
     * A job id GitHub accepts: "must start with a letter or {@code _} and contain
     * only alphanumeric characters, {@code -}, or {@code _}".
     */
    private static final Pattern LEGAL_JOB_ID = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]*");

    /** A job-level {@code if:} — four-space indent, first line only. */
    private static final Pattern JOB_IF = Pattern.compile("(?m)^    if: (.*)$");

    /** An inline {@code needs: [a, b, c]} flow sequence. */
    private static final Pattern JOB_NEEDS = Pattern.compile("(?m)^    needs: \\[([^]]*)]");

    /**
     * A job that only runs on a schedule or a manual dispatch never appears on a
     * pull request, so the gate has nothing to aggregate from it.
     */
    private static final Pattern SCHEDULE_ONLY = Pattern.compile("github\\.event_name == 'schedule'");

    @Test
    void ciGateAggregatesEveryJobThatCanRunOnAPullRequest() throws IOException {
        Map<String, String> jobs = jobBlocks();

        assertThat(jobs)
                .describedAs("no jobs parsed out of %s — this guard is reading a workflow shape "
                        + "that moved, so it is no longer guarding anything", relative(WORKFLOW))
                .isNotEmpty()
                .containsKey(GATE);

        Set<String> aggregated = needsOf(jobs.get(GATE));
        assertThat(aggregated)
                .describedAs("the '%s' job must declare an inline 'needs: [...]' list", GATE)
                .isNotEmpty();

        List<String> unwatched = new ArrayList<>();
        for (Map.Entry<String, String> job : jobs.entrySet()) {
            String id = job.getKey();
            if (id.equals(GATE) || runsOnlyOnASchedule(job.getValue()) || aggregated.contains(id)) {
                continue;
            }
            unwatched.add(id);
        }

        assertThat(unwatched)
                .describedAs("every job that can run on a pull request must be in the '%s' needs "
                        + "list, or a failure there leaves the aggregate check green over a run "
                        + "that built nothing", GATE)
                .isEmpty();
    }

    /**
     * The parser takes every key at job level, so it can also pick up something that
     * is not a job id at all — a malformed workflow, or a shape nobody anticipated.
     *
     * <p>That is deliberate, and this is the other half of it. Skipping such a key
     * would put the guard back where it started: quietly reporting on a subset. Here
     * it fails instead, and names what it could not interpret.</p>
     */
    @Test
    void everyJobIdIsOneGitHubWouldAccept() throws IOException {
        List<String> illegal = new ArrayList<>();
        for (String id : jobBlocks().keySet()) {
            if (!LEGAL_JOB_ID.matcher(id).matches()) {
                illegal.add(id);
            }
        }

        assertThat(illegal)
                .describedAs("job-level keys in %s that GitHub would not accept as job ids — a "
                        + "job id must start with a letter or '_' and hold only letters, digits, "
                        + "'-' or '_'. Either the workflow is malformed, or it uses a shape this "
                        + "guard parses wrongly; both are findings, and neither should be passed "
                        + "over in silence", relative(WORKFLOW))
                .isEmpty();
    }

    @Test
    void ciGateDoesNotDependOnAJobThatIsGone() throws IOException {
        Map<String, String> jobs = jobBlocks();
        Set<String> aggregated = needsOf(jobs.get(GATE));

        assertThat(jobs.keySet())
                .describedAs("'%s' aggregates a job id that no longer exists: the workflow would "
                        + "fail to load, and the rename that caused it is the finding", GATE)
                .containsAll(aggregated);
    }

    /** Job id to the source block that declares it, for the workflow on disk. */
    private static Map<String, String> jobBlocks() throws IOException {
        return jobBlocks(Files.readString(WORKFLOW));
    }

    /**
     * Job id to the source block that declares it, in workflow order.
     *
     * <p>Package-private, and taking the workflow as text rather than reading it,
     * so the parser can be driven with job shapes the repository's own workflow
     * does not happen to contain. A parser that only ever sees valid input it
     * already handles is one whose blind spots stay theoretical until a new job
     * lands on one.</p>
     *
     * @param workflowText the workflow source
     * @return each job id mapped to the block that declares it
     */
    static Map<String, String> jobBlocks(String workflowText) {
        // The workflow is checked out with CRLF on Windows. Normalise once, so the
        // line-anchored patterns below capture ids and conditions without a trailing
        // carriage return riding along into every comparison.
        String workflow = workflowText.replace("\r\n", "\n");
        // `on:` carries keys at the same indentation as a job (`push:`, `schedule:`),
        // so parsing starts after the `jobs:` key rather than at the top of the file.
        int jobsAt = workflow.indexOf("\njobs:\n");
        assertThat(jobsAt)
                .describedAs("no top-level 'jobs:' key in %s", relative(WORKFLOW))
                .isNotNegative();
        String body = workflow.substring(jobsAt);

        Map<String, String> blocks = new LinkedHashMap<>();
        Matcher key = JOB_KEY.matcher(body);
        List<String> ids = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        while (key.find()) {
            ids.add(unquoted(key.group(1).trim()));
            starts.add(key.end());
        }
        for (int i = 0; i < ids.size(); i++) {
            int end = i + 1 < ids.size() ? starts.get(i + 1) : body.length();
            blocks.put(ids.get(i), body.substring(starts.get(i), end));
        }
        return blocks;
    }

    /**
     * Strips the quotes YAML may put around a mapping key.
     *
     * <p>{@code "build-and-test"} and {@code build-and-test} are the same key; the
     * quotes are the encoding, not the name. Only a matched surrounding pair is
     * removed, so a key that merely contains a quote is left alone.</p>
     *
     * @param key the key exactly as it appears in the source
     * @return the key with one matched pair of surrounding quotes removed
     */
    static String unquoted(String key) {
        if (key.length() < 2) {
            return key;
        }
        char first = key.charAt(0);
        if ((first == '"' || first == '\'') && key.charAt(key.length() - 1) == first) {
            return key.substring(1, key.length() - 1);
        }
        return key;
    }

    private static Set<String> needsOf(String jobBlock) {
        Matcher needs = JOB_NEEDS.matcher(jobBlock);
        if (!needs.find()) {
            return Set.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        for (String id : needs.group(1).split(",")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                ids.add(trimmed);
            }
        }
        return ids;
    }

    private static boolean runsOnlyOnASchedule(String jobBlock) {
        Matcher condition = JOB_IF.matcher(jobBlock);
        return condition.find() && SCHEDULE_ONLY.matcher(condition.group(1)).find();
    }

    private static String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
