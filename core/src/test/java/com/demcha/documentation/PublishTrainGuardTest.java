package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds {@code publish.yml} to shipping the lockstep train as one Maven Central
 * deployment, and nothing else.
 *
 * <p>Central counts every distinct publish operation against an organisation's monthly
 * Release Count, so the train goes up as a single {@code -P release deploy -pl <train>}
 * reactor run: the central-publishing plugin stages each selected module and uploads
 * them together from the last one. That shape has failure modes no build notices. A
 * module left out of {@code -pl} is simply never released; an {@code -am} quietly pulls
 * the independently versioned fonts and emoji into the deployment and re-uploads
 * coordinates Central already holds; and because the upload runs with whichever
 * module's plugin settings the reactor happens to order last, eight release profiles
 * that drift apart publish with settings nobody chose.</p>
 *
 * <p>The train is derived from the poms ({@link PublishedModules#lockstepPublished}),
 * not restated here, so the workflow is held against an independent statement of what
 * a release ships.</p>
 */
class PublishTrainGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();
    private static final Path PUBLISH = PROJECT_ROOT.resolve(".github/workflows/publish.yml");

    /** The central-publishing plugin declaration inside a pom. */
    private static final Pattern CENTRAL_PLUGIN = Pattern.compile(
            "<plugin>\\s*<groupId>org\\.sonatype\\.central</groupId>\\s*"
                    + "<artifactId>central-publishing-maven-plugin</artifactId>.*?</plugin>",
            Pattern.DOTALL);

    private static final Pattern CENTRAL_PLUGIN_VERSION = Pattern.compile(
            "<central\\.publishing\\.plugin\\.version>\\s*([^<]+?)\\s*</central\\.publishing\\.plugin\\.version>");

    /** The {@code deploy} goal as a word — not {@code deployment}, not {@code deploy-web}. */
    private static final Pattern DEPLOY_GOAL = Pattern.compile("(?<![\\w-])deploy(?![\\w-])");

    /** Options that widen a {@code -pl} selection beyond the modules it names. */
    private static final Set<String> ALSO_MAKE = Set.of("-am", "--also-make", "-amd", "--also-make-dependents");

    /**
     * Plugin settings that would silently change what a deployment carries if a pom set
     * them: a pom value overrides the {@code -D} the workflow passes, so a stray
     * {@code ignorePublishedComponents} turns recovery mode on for every release, and a
     * {@code skipPublishing} or {@code excludeArtifacts} drops a module from the train
     * while the job stays green.
     */
    private static final List<String> DEPLOYMENT_SHAPING = List.of(
            "ignorePublishedComponents", "skipPublishing", "excludeArtifacts");

    @Test
    void theTrainShipsAsExactlyOneReactorDeploy() throws IOException {
        List<String> deploys = PublishedModules.deployCommands(PUBLISH);

        assertThat(deploys)
                .describedAs("publish.yml must deploy the train in exactly one `./mvnw … deploy` "
                        + "invocation: each separate invocation is its own Central deployment, "
                        + "and each deployment is its own Release Count event")
                .hasSize(1);

        String deploy = deploys.get(0);
        List<String> tokens = List.of(deploy.split("\\s+"));
        assertThat(tokens)
                .describedAs("the train deploy must select its modules with -pl over the root "
                        + "reactor and run the release profile: %s", deploy)
                .contains("-pl", "-P")
                .doesNotContain("-f");
        assertThat(deploy).contains("-P release");

        Set<String> widening = new TreeSet<>(ALSO_MAKE);
        widening.retainAll(tokens);
        assertThat(widening)
                .describedAs("the train deploy must not widen its -pl selection: also-make pulls "
                        + "core's test-scope fonts and emoji into the reactor, and therefore into "
                        + "the deployment, re-uploading coordinates already on Central")
                .isEmpty();
    }

    /**
     * Every other test here reads deploys through {@link PublishedModules#deployCommands},
     * which recognises one shape: a single-line {@code ./mvnw … deploy}. A deploy written
     * any other way — {@code mvn}, a {@code - run:} list item, a command wrapped with
     * {@code \} continuations — would be invisible to all of them, so a second deployment
     * could ship while "exactly one deploy" stayed green. This keys on the positive
     * signal instead: any non-comment line of a publish workflow naming the
     * {@code deploy} goal must be one the parser read.
     */
    @Test
    void everyDeployInAPublishWorkflowIsOneTheGuardsRead() throws IOException {
        Set<String> unread = new TreeSet<>();
        try (var files = Files.list(PROJECT_ROOT.resolve(".github/workflows"))) {
            for (Path workflow : files.sorted().toList()) {
                String name = workflow.getFileName().toString();
                if (!name.startsWith("publish") || !name.endsWith(".yml")) {
                    continue;
                }
                List<String> parsed = PublishedModules.deployCommands(workflow);
                for (String line : Files.readAllLines(workflow)) {
                    String code = line.strip();
                    if (code.startsWith("#") || !DEPLOY_GOAL.matcher(code).find()) {
                        continue;
                    }
                    if (!parsed.contains(code)) {
                        unread.add(name + ": " + code);
                    }
                }
            }
        }

        assertThat(unread)
                .describedAs("publish workflow lines that name the deploy goal in a shape "
                        + "PublishedModules does not read, so no guard checks what they ship. "
                        + "Write the deploy as one `./mvnw … deploy` line, or teach "
                        + "PublishedModules the new shape")
                .isEmpty();
    }

    @Test
    void theDeploySelectsExactlyTheLockstepTrain() throws IOException {
        List<String> train = PublishedModules.lockstepPublished(PROJECT_ROOT);
        List<String> deployed = PublishedModules.deployedByWorkflow(PROJECT_ROOT)
                .getOrDefault("publish.yml", List.of());

        assertThat(train)
                .describedAs("no lockstep module was derived from the poms — the derivation "
                        + "(standalone pom, central-publishing plugin, reactor version) no longer "
                        + "matches the layout, and the comparison below would be against nothing")
                .contains("core");

        Set<String> unpublished = new TreeSet<>(train);
        unpublished.removeAll(deployed);
        assertThat(unpublished)
                .describedAs("lockstep modules a release would never publish: add them to the "
                        + "-pl list of the deploy in publish.yml")
                .isEmpty();

        Set<String> unexpected = new TreeSet<>(deployed);
        unexpected.removeAll(train);
        assertThat(unexpected)
                .describedAs("publish.yml deploys modules outside the lockstep train (or names a "
                        + "selector that resolves to no reactor module)")
                .isEmpty();
    }

    @Test
    void buildOnlyModulesAreNeverDeployed() throws IOException {
        List<String> buildOnly = PublishedModules.buildOnly(PROJECT_ROOT);

        assertThat(buildOnly)
                .describedAs("no build-only aggregator child was found — the root pom's module "
                        + "list moved, and this guard is checking an empty set")
                .isNotEmpty();

        Set<String> leaked = new TreeSet<>(buildOnly);
        leaked.retainAll(PublishedModules.deployed(PROJECT_ROOT));
        assertThat(leaked)
                .describedAs("a publish workflow deploys a build-only module (examples, "
                        + "benchmarks, qa, coverage …), which has no Central metadata and must "
                        + "never be published")
                .isEmpty();
    }

    @Test
    void fontsAndEmojiPublishOnlyThroughTheirOwnWorkflows() throws IOException {
        Map<String, List<String>> byWorkflow = PublishedModules.deployedByWorkflow(PROJECT_ROOT);

        assertThat(byWorkflow.get("publish-fonts.yml"))
                .describedAs("publish-fonts.yml must deploy graph-compose-fonts, and only it")
                .containsExactly("fonts");
        assertThat(byWorkflow.get("publish-emoji.yml"))
                .describedAs("publish-emoji.yml must deploy graph-compose-emoji, and only it")
                .containsExactly("emoji");
        assertThat(byWorkflow.get("publish.yml"))
                .describedAs("fonts and emoji carry their own version lines and publish only when "
                        + "those move; the engine train must not carry them")
                .doesNotContain("fonts", "emoji");
        assertThat(PublishedModules.lockstepPublished(PROJECT_ROOT))
                .describedAs("fonts or emoji now carry the engine version, which makes them look "
                        + "like train modules — their versions are independent by design")
                .doesNotContain("fonts", "emoji");
    }

    @Test
    void everyTrainModuleConfiguresCentralPublishingIdentically() throws IOException {
        Map<String, String> declarations = new LinkedHashMap<>();
        Map<String, String> versions = new LinkedHashMap<>();
        for (String module : PublishedModules.lockstepPublished(PROJECT_ROOT)) {
            String pom = Files.readString(PROJECT_ROOT.resolve(module).resolve("pom.xml"));
            Matcher plugin = CENTRAL_PLUGIN.matcher(pom);
            assertThat(plugin.find())
                    .describedAs("%s/pom.xml declares no central-publishing plugin block", module)
                    .isTrue();
            declarations.put(module, plugin.group().replaceAll("\\s+", " "));
            Matcher version = CENTRAL_PLUGIN_VERSION.matcher(pom);
            versions.put(module, version.find() ? version.group(1) : "<unset>");

            for (String setting : DEPLOYMENT_SHAPING) {
                assertThat(pom)
                        .describedAs("%s/pom.xml sets %s — that changes what the train deployment "
                                + "carries for every release; pass it from publish.yml instead",
                                module, setting)
                        .doesNotContain("<" + setting + ">");
            }
        }

        assertThat(Set.copyOf(declarations.values()))
                .describedAs("the train's central-publishing declarations differ. The upload runs "
                        + "with the settings of whichever module the reactor orders last, so every "
                        + "train module must declare the plugin identically: %s", declarations)
                .hasSize(1);
        assertThat(Set.copyOf(versions.values()))
                .describedAs("the train's central.publishing.plugin.version properties differ: %s",
                        versions)
                .hasSize(1);
    }

    @Test
    void skippingPublishedComponentsIsAnExplicitRecoveryOnly() throws IOException {
        // A Windows checkout carries CRLF; the patterns below are written against LF.
        String workflow = Files.readString(PUBLISH).replace("\r", "");
        String deploy = PublishedModules.deployCommands(PUBLISH).get(0);

        assertThat(workflow)
                .describedAs("publish.yml must offer skip_published as a boolean dispatch input "
                        + "that defaults to false")
                .containsPattern("(?m)^      skip_published:\\s*$")
                .containsPattern("skip_published:(?:\\n {8}.*)*\\n {8}type: boolean")
                .containsPattern("skip_published:(?:\\n {8}.*)*\\n {8}default: false");
        assertThat(workflow)
                .describedAs("SKIP_PUBLISHED must be true only when a dispatch set skip_published — "
                        + "never on a tag push, where the input is absent")
                .contains("SKIP_PUBLISHED: ${{ github.event.inputs.skip_published == 'true' }}");
        assertThat(deploy)
                .describedAs("the deploy must pass the recovery switch through, and only that way")
                .contains("-DignorePublishedComponents=$SKIP_PUBLISHED");
    }
}
