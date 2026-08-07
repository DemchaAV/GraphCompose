package com.demcha.documentation;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
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
 * Guards that the GraphCompose version propagates identically to every module
 * and to the README + showcase install snippets.
 *
 * <p>This is the safety net behind the aggregator-reactor version model
 * (the root {@code pom.xml} is the reactor aggregator): the engine
 * {@code core/pom.xml} is the single version source, the aggregator bumps every
 * module in lockstep via {@code versions:set}, and the child modules inherit
 * their version rather than pinning a literal. This test fails the build the
 * moment a bump leaves any
 * module — or a copy-paste install snippet — pointing at a different version,
 * which is the drift class that previously let the benchmarks module run
 * against the previous release.
 *
 * <p>The install-snippet checks require the snippets to advertise a version that
 * a user can actually resolve. During a normal {@code -SNAPSHOT} development cycle
 * that is the latest <em>published</em> release (the topmost dated
 * {@code CHANGELOG.md} entry) — not the in-development {@code -SNAPSHOT} and not a
 * forward-looking {@code Planned} version. In the release commit itself
 * {@code cut-release.ps1} bumps the pom off {@code -SNAPSHOT} and rewrites the
 * snippets to the new release version, so once the pom carries a concrete release
 * version the snippets must equal it. See {@link #acceptableTargets()}.
 */
class VersionConsistencyGuardTest {

    private static final Path PROJECT_ROOT = RepoRoot.get();

    @Test
    void everyModuleResolvesToTheRootProjectVersion() throws Exception {
        String root = effectiveVersion(PROJECT_ROOT.resolve("core/pom.xml"));

        assertThat(effectiveVersion(PROJECT_ROOT.resolve("pom.xml")))
                .describedAs("the root aggregator pom.xml must equal the engine (core/pom.xml) version (%s)", root)
                .isEqualTo(root);
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("examples/pom.xml")))
                .describedAs("examples must inherit the root version (%s)", root)
                .isEqualTo(root);
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("benchmarks/pom.xml")))
                .describedAs("benchmarks must inherit the root version (%s)", root)
                .isEqualTo(root);
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("bundle/pom.xml")))
                .describedAs("bundle (graph-compose-bundle) tracks the engine line and must equal the root version (%s)", root)
                .isEqualTo(root);
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("render-docx/pom.xml")))
                .describedAs("render-docx (graph-compose-render-docx) tracks the engine line and must equal the root version (%s)", root)
                .isEqualTo(root);
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("render-pptx/pom.xml")))
                .describedAs("render-pptx (graph-compose-render-pptx) tracks the engine line and must equal the root version (%s)", root)
                .isEqualTo(root);
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("render-pdf/pom.xml")))
                .describedAs("render-pdf (graph-compose-render-pdf) tracks the engine line and must equal the root version (%s)", root)
                .isEqualTo(root);
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("templates/pom.xml")))
                .describedAs("templates (graph-compose-templates) tracks the engine line and must equal the root version (%s)", root)
                .isEqualTo(root);
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("testing/pom.xml")))
                .describedAs("testing (graph-compose-testing) tracks the engine line and must equal the root version (%s)", root)
                .isEqualTo(root);
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("wrapper/pom.xml")))
                .describedAs("wrapper (the graph-compose compat wrapper) tracks the engine line and must equal the root version (%s)", root)
                .isEqualTo(root);
        // qa + coverage inherit their version from the aggregator, but the inherited
        // <parent><version> is a literal that must track the root — otherwise a clean
        // reactor build (no local m2 cache) cannot resolve graph-compose-build after a
        // release bump. effectiveVersion reads that inherited parent value here.
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("qa/pom.xml")))
                .describedAs("qa (graph-compose-qa) inherits the root version; its <parent> version must equal the root (%s)", root)
                .isEqualTo(root);
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("coverage/pom.xml")))
                .describedAs("coverage (graph-compose-coverage) inherits the root version; its <parent> version must equal the root (%s)", root)
                .isEqualTo(root);

        // NOTE: fonts/pom.xml (graph-compose-fonts) is intentionally NOT checked
        // here. It carries an independent version line (it ships on its own
        // fonts-v* tag and is bumped only when the font set changes), so it must
        // be free to diverge from the engine version.
    }

    @Test
    void bundledFontsVersionAgreesAcrossModules() throws Exception {
        // graph-compose-fonts carries an independent version line. The engine
        // does NOT depend on the fonts artifact (its tests read the fonts from
        // the sibling module's source), so the ${graphcompose.fonts.version}
        // property that pins the artifact lives only in the modules that consume
        // it: the aggregator (inherited by examples + benchmarks) and the bundle.
        // This guards the version-literal drift class: those must always agree, even
        // though they differ from the engine version line.
        String aggregator = pinnedVersionProperty(PROJECT_ROOT.resolve("pom.xml"), "graphcompose.fonts.version");

        assertThat(pinnedVersionProperty(PROJECT_ROOT.resolve("bundle/pom.xml"), "graphcompose.fonts.version"))
                .describedAs("bundle graphcompose.fonts.version must match the aggregator's (%s)", aggregator)
                .isEqualTo(aggregator);
    }

    @Test
    void bundledEmojiVersionAgreesAcrossModules() throws Exception {
        // graph-compose-emoji, like graph-compose-fonts, carries an independent
        // version line (emoji-v* tag) and the ${graphcompose.emoji.version} property
        // that pins it lives in the aggregator (inherited by examples) and the bundle
        // (which now ships the colour-emoji set). Guard the same version-literal drift.
        String aggregator = pinnedVersionProperty(PROJECT_ROOT.resolve("pom.xml"), "graphcompose.emoji.version");

        assertThat(pinnedVersionProperty(PROJECT_ROOT.resolve("bundle/pom.xml"), "graphcompose.emoji.version"))
                .describedAs("bundle graphcompose.emoji.version must match the aggregator's (%s)", aggregator)
                .isEqualTo(aggregator);
    }

    @Test
    void childModulesInheritVersionInsteadOfDeclaringTheirOwn() throws Exception {
        assertThat(declaresOwnVersion(PROJECT_ROOT.resolve("examples/pom.xml")))
                .describedAs("examples/pom.xml must inherit <version> from the aggregator parent, not declare its own")
                .isFalse();
        assertThat(declaresOwnVersion(PROJECT_ROOT.resolve("benchmarks/pom.xml")))
                .describedAs("benchmarks/pom.xml must inherit <version> from the aggregator parent, not declare its own")
                .isFalse();
        assertThat(declaresOwnVersion(PROJECT_ROOT.resolve("qa/pom.xml")))
                .describedAs("qa/pom.xml must inherit <version> from the aggregator parent, not declare its own")
                .isFalse();
        assertThat(declaresOwnVersion(PROJECT_ROOT.resolve("coverage/pom.xml")))
                .describedAs("coverage/pom.xml must inherit <version> from the aggregator parent, not declare its own")
                .isFalse();
    }

    @Test
    void jacocoPluginVersionAgreesAcrossModules() throws Exception {
        // The coverage agent (which writes each module's exec) and report-aggregate
        // (which reads them) must run the same JaCoCo version — a mismatch can break
        // the exec-format read. The version is pinned as a literal in each standalone
        // module that measures coverage (engine, render-pdf, templates) plus the
        // aggregator that the qa + coverage children inherit, so guard the four
        // against drift.
        String core = pinnedVersionProperty(PROJECT_ROOT.resolve("core/pom.xml"), "jacoco.plugin.version");

        assertThat(pinnedVersionProperty(PROJECT_ROOT.resolve("pom.xml"), "jacoco.plugin.version"))
                .describedAs("aggregator jacoco.plugin.version must match the engine pom's (%s)", core)
                .isEqualTo(core);
        assertThat(pinnedVersionProperty(PROJECT_ROOT.resolve("render-pdf/pom.xml"), "jacoco.plugin.version"))
                .describedAs("render-pdf jacoco.plugin.version must match the engine pom's (%s)", core)
                .isEqualTo(core);
        assertThat(pinnedVersionProperty(PROJECT_ROOT.resolve("templates/pom.xml"), "jacoco.plugin.version"))
                .describedAs("templates jacoco.plugin.version must match the engine pom's (%s)", core)
                .isEqualTo(core);
    }

    /**
     * The wrapper builds its javadoc jar from the engine's sources, which are
     * Lombok-annotated, so it feeds Lombok to the javadoc plugin as an additional
     * dependency. That pins a second literal of a version the engine already owns —
     * and a javadoc classpath one release behind fails with "package lombok does not
     * exist" only during a release build, which is the worst place to find out.
     */
    @Test
    void wrapperJavadocLombokVersionTracksTheEngine() throws Exception {
        String engine = pinnedVersionProperty(PROJECT_ROOT.resolve("core/pom.xml"), "lombok.version");

        assertThat(pinnedVersionProperty(PROJECT_ROOT.resolve("wrapper/pom.xml"), "lombok.version"))
                .describedAs("wrapper lombok.version feeds the javadoc plugin that documents the "
                        + "engine's sources; it must match the engine pom's (%s)", engine)
                .isEqualTo(engine);
    }

    @Test
    void graphComposeWrapperStaysAJarOverCore() throws Exception {
        Element project = parse(PROJECT_ROOT.resolve("wrapper/pom.xml")).getDocumentElement();

        // Packaging MUST stay jar (the default when <packaging> is absent). A pom
        // would force every existing graph-compose consumer to add <type>pom</type>
        // and break their build — the entire point of the wrapper is a drop-in jar.
        Element packaging = directChild(project, "packaging");
        assertThat(packaging == null ? "jar" : packaging.getTextContent().trim())
                .describedAs("wrapper/pom.xml (the graph-compose coordinate) must stay a jar, never pom — a pom would force consumers to add <type>pom</type>")
                .isEqualTo("jar");

        // And it must aggregate the engine: the dependency on graph-compose-core is
        // what makes a bare `graph-compose` bring the engine transitively.
        assertThat(declaresDependencyOn(project, "graph-compose-core"))
                .describedAs("wrapper/pom.xml must depend on graph-compose-core so `graph-compose` still brings the engine")
                .isTrue();
    }

    @Test
    void benchmarksDependencyDerivesVersionAndIsNotHardcoded() throws IOException {
        String pom = Files.readString(PROJECT_ROOT.resolve("benchmarks/pom.xml"));

        assertThat(pom)
                .describedAs("benchmarks graphcompose.version must derive from ${project.version}")
                .contains("<graphcompose.version>${project.version}</graphcompose.version>");
        assertThat(Pattern.compile("<graphcompose\\.version>\\s*\\d").matcher(pom).find())
                .describedAs("benchmarks must not hardcode a numeric graphcompose.version — that is the original drift source")
                .isFalse();
    }

    /**
     * The README hero banner reads its version pill from the filtered examples
     * {@code banner.properties}; its {@code version} line must be the Maven
     * {@code @project.version@} token so the rendered hero always carries the
     * release version instead of a hand-bumped literal (the drift that left the
     * banner reading v1.8.0 while the repo was on v1.9.0). {@code cut-release.ps1}
     * re-renders the banner on release, and this fails the verify gate the moment
     * the source is hardcoded again.
     */
    @Test
    void readmeBannerVersionDerivesFromProjectVersion() throws Exception {
        String props = Files.readString(
                PROJECT_ROOT.resolve("examples/src/main/resources/banner.properties"));

        assertThat(Pattern.compile("(?m)^\\s*version\\s*=\\s*@project\\.version@\\s*$").matcher(props).find())
                .describedAs("examples/src/main/resources/banner.properties must source the hero version from @project.version@, not a literal")
                .isTrue();
        assertThat(Pattern.compile("(?m)^\\s*version\\s*=\\s*v?\\d").matcher(props).find())
                .describedAs("banner.properties must not hardcode a numeric version — that reintroduces the stale-hero drift")
                .isFalse();
    }

    /**
     * The README release-status block advertises a version that exists.
     *
     * <p>It used to be a hand-edit no test covered, and the release script demanded it
     * already name the version being cut — so between releases {@code develop} carried
     * an unpublished version as "latest stable" behind a tag URL that 404s. The script
     * rewrites the block now; this pins the result, and pins the link to the text so a
     * half-updated block cannot pass.</p>
     */
    @Test
    void readmeReleaseStatusNamesAPublishedVersion() throws Exception {
        Set<String> targets = acceptableTargets();
        String readme = Files.readString(PROJECT_ROOT.resolve("README.md"));

        Matcher stable = Pattern.compile(
                        "\\*\\*Latest stable\\*\\*:\\s*\\[v(\\d+\\.\\d+\\.\\d+)]\\(([^)]+)\\)")
                .matcher(readme);
        assertThat(stable.find())
                .describedAs("README must carry a '**Latest stable**: [vX.Y.Z](…)' release-status "
                        + "line — cut-release.ps1 rewrites it during the cut and aborts without it")
                .isTrue();

        assertThat(stable.group(1))
                .describedAs("README 'Latest stable' must name a published release, not the one "
                        + "in development: its tag page does not exist until the cut (one of %s)", targets)
                .isIn(targets);
        assertThat(stable.group(2))
                .describedAs("the 'Latest stable' link must point at the tag it names, or the badge "
                        + "sends readers to a different release than the text claims")
                .endsWith("/releases/tag/v" + stable.group(1));
    }

    /**
     * The open {@code CHANGELOG.md} entry and the in-development pom version name the
     * same release line.
     *
     * <p>The post-release step opens the next line by incrementing the patch
     * unconditionally, so a GA of {@code X.Y.Z} always leaves the train on
     * {@code X.Y.(Z+1)-SNAPSHOT}. When the next release turns out to be a minor, the
     * poms and the CHANGELOG name different releases for the rest of the cycle, and an
     * {@code @since} tag written meanwhile has two answers to choose between. It is a
     * public API contract, so whichever the author picks outlives the cycle: the last
     * time these two disagreed, tags were written against both — most against the
     * CHANGELOG heading, the rest against the previous release, and the latter had to
     * be retagged when the line was corrected.</p>
     *
     * <p>What this pins is that correction. The real next version is recorded in the
     * CHANGELOG heading first, because that is where the cycle's opening entry goes;
     * from that commit on, the build stays red until the poms name the same line. What
     * it cannot catch is a heading and a pom that are wrong <em>together</em> — both
     * states are internally consistent, and nothing in the tree distinguishes them.</p>
     *
     * <p>Having no open entry at all passes. The post-release bump writes no heading and
     * runs this test as its own gate before committing, so requiring one here would fail
     * the very commit that opens the window. The entry arrives with the cycle's first
     * CHANGELOG addition and is held from then on.</p>
     */
    @Test
    void theOpenChangelogEntryNamesTheVersionUnderDevelopment() throws Exception {
        String changelog = Files.readString(PROJECT_ROOT.resolve("CHANGELOG.md"));
        String pomVersion = effectiveVersion(PROJECT_ROOT.resolve("core/pom.xml"));

        assertThat(versionDriftProblem(changelog, pomVersion))
                .describedAs("the CHANGELOG entry left open and the working pom version must name the "
                        + "same release, or an @since written this cycle has two answers to pick from")
                .isNull();
    }

    @Test
    void readmeInstallSnippetsMatchTheProjectVersion() throws Exception {
        Set<String> targets = acceptableTargets();
        String readme = Files.readString(PROJECT_ROOT.resolve("README.md"));

        String mavenSnippetVersion = firstMatchingGroup(readme, INSTALL_SNIPPET_PATTERNS_README_MAVEN);
        String gradleSnippetVersion = firstMatchingGroup(readme, INSTALL_SNIPPET_PATTERNS_README_GRADLE);

        assertThat(mavenSnippetVersion)
                .describedAs("README Maven install snippet must reference the latest published release, or the release version in a release commit (one of %s)", targets)
                .isIn(targets);
        assertThat(gradleSnippetVersion)
                .describedAs("README Gradle install snippet must reference the latest published release, or the release version in a release commit (one of %s)", targets)
                .isIn(targets);
    }

    /**
     * The GitHub Pages showcase ({@code web/index.html}) hardcodes the version
     * in three spots — JSON-LD {@code softwareVersion}, the hero badge, and the
     * Maven + Gradle install snippets. None inherit from the pom, so without
     * this guard they silently drift. {@code cut-release.ps1} flips them on
     * release; this fails the verify gate if any spot lags behind.
     */
    @Test
    void showcaseSiteVersionMatchesTheProjectVersion() throws Exception {
        Set<String> targets = acceptableTargets();
        String site = Files.readString(PROJECT_ROOT.resolve("web/index.html"));

        assertThat(firstGroup(site, "\"softwareVersion\":\\s*\"v?([0-9][^\"]*)\""))
                .describedAs("web/index.html JSON-LD softwareVersion must equal the latest published release, or the release version in a release commit (one of %s)", targets)
                .isIn(targets);
        // Match up to the delimiter (space before `&middot;`) rather than
        // digits-and-dots only, so a pre-release version like 2.0.0-rc.1 is
        // captured too — consistent with the JSON-LD and snippet patterns.
        assertThat(firstGroup(site, "v([0-9][^\\s&]*)\\s*&middot;\\s*MIT"))
                .describedAs("web/index.html hero version badge must equal the latest published release, or the release version in a release commit (one of %s)", targets)
                .isIn(targets);
        assertThat(firstMatchingGroup(site, INSTALL_SNIPPET_PATTERNS_SHOWCASE_MAVEN))
                .describedAs("web/index.html Maven install snippet must equal the latest published release, or the release version in a release commit (one of %s)", targets)
                .isIn(targets);
        assertThat(firstMatchingGroup(site, INSTALL_SNIPPET_PATTERNS_SHOWCASE_GRADLE))
                .describedAs("web/index.html Gradle install snippet must equal the latest published release, or the release version in a release commit (one of %s)", targets)
                .isIn(targets);
    }

    /**
     * Every train module's README carries a copy-paste install snippet (Maven +
     * Gradle) for its own artifact. {@code cut-release.ps1} bumps them in the
     * release commit; this fails the verify gate if any snippet lags the pom —
     * the drift class where a browsed module README advertises the previous
     * release's coordinate.
     */
    @Test
    void moduleReadmeInstallSnippetsMatchTheProjectVersion() throws Exception {
        Set<String> targets = acceptableTargets();
        Map<String, String> trainReadmes = new LinkedHashMap<>();
        trainReadmes.put("core/README.md", "graph-compose-core");
        trainReadmes.put("render-pdf/README.md", "graph-compose-render-pdf");
        trainReadmes.put("render-docx/README.md", "graph-compose-render-docx");
        trainReadmes.put("render-pptx/README.md", "graph-compose-render-pptx");
        trainReadmes.put("templates/README.md", "graph-compose-templates");
        trainReadmes.put("testing/README.md", "graph-compose-testing");
        trainReadmes.put("wrapper/README.md", "graph-compose");
        trainReadmes.put("bundle/README.md", "graph-compose-bundle");

        for (Map.Entry<String, String> module : trainReadmes.entrySet()) {
            String readme = Files.readString(PROJECT_ROOT.resolve(module.getKey()));
            String artifact = Pattern.quote(module.getValue());
            assertThat(firstGroup(readme, "<artifactId>" + artifact + "</artifactId>\\s*<version>v?([0-9][^<]*)</version>"))
                    .describedAs("%s Maven install snippet must equal the latest published release, or the release version in a release commit (one of %s)", module.getKey(), targets)
                    .isIn(targets);
            assertThat(firstGroup(readme, "io\\.github\\.demchaav:" + artifact + ":v?([0-9][\\w.\\-]*)"))
                    .describedAs("%s Gradle install snippet must equal the latest published release, or the release version in a release commit (one of %s)", module.getKey(), targets)
                    .isIn(targets);
        }
    }

    /**
     * fonts/emoji carry independent version lines, so their README snippets are
     * checked against their own pom versions — never the engine train's.
     */
    @Test
    void companionReadmeInstallSnippetsMatchTheirPomVersions() throws Exception {
        for (String module : new String[] {"fonts", "emoji"}) {
            String own = effectiveVersion(PROJECT_ROOT.resolve(module + "/pom.xml"));
            String readme = Files.readString(PROJECT_ROOT.resolve(module + "/README.md"));
            String artifact = Pattern.quote("graph-compose-" + module);
            assertThat(firstGroup(readme, "<artifactId>" + artifact + "</artifactId>\\s*<version>v?([0-9][^<]*)</version>"))
                    .describedAs("%s/README.md Maven install snippet must equal the module's own pom version (%s)", module, own)
                    .isEqualTo(own);
            assertThat(firstGroup(readme, "io\\.github\\.demchaav:" + artifact + ":v?([0-9][\\w.\\-]*)"))
                    .describedAs("%s/README.md Gradle install snippet must equal the module's own pom version (%s)", module, own)
                    .isEqualTo(own);
        }
    }

    /**
     * Install snippets published under {@code docs/} advertise a version a reader
     * can resolve.
     *
     * <p>The root README, the module READMEs and the showcase site were already
     * pinned; the docs tree was not, and that is where a backend coordinate sat a
     * minor behind everything else — a reader who opened the troubleshooting page
     * because a session would not start was handed a {@code graph-compose-render-pdf}
     * one minor older than the engine they were running.</p>
     *
     * <p>Historical trees are excluded by <em>path prefix</em> rather than by a
     * file allowlist: migration guides, archived pages and shipped roadmaps pin an
     * old version on purpose, and a new one of those is covered the day it is
     * written instead of the day someone remembers to extend a list.</p>
     */
    @Test
    void documentationInstallSnippetsMatchTheProjectVersion() throws Exception {
        Set<String> trainTargets = acceptableTargets();
        List<DocCoordinate> coordinates = documentationCoordinates();
        List<String> drift = new ArrayList<>();

        for (DocCoordinate coordinate : coordinates) {
            Set<String> expected = expectedVersionsFor(coordinate.artifact(), trainTargets);
            if (!expected.contains(coordinate.version())) {
                drift.add("%s:%d %s advertises %s, expected one of %s".formatted(
                        coordinate.page(), coordinate.line(), coordinate.artifact(), coordinate.version(), expected));
            }
        }

        assertThat(coordinates)
                .describedAs("no versioned install coordinate found under docs/ — this guard would cover nothing")
                .isNotEmpty();
        assertThat(drift)
                .describedAs("every versioned install snippet under docs/ must advertise the version a reader can resolve today")
                .isEmpty();
    }

    /**
     * Every documentation page carrying a train install snippet is both bumped and
     * staged by {@code cut-release.ps1}.
     *
     * <p>{@link #documentationInstallSnippetsMatchTheProjectVersion()} catches a stale
     * snippet, but only once it is already stale — one release after the page was
     * added. This closes the gap at the source: the script rewrites a hand-maintained
     * list of pages, and a page that carries a snippet without being on that list
     * silently keeps the previous version through every future cut. Both halves are
     * required, because bumping a file the commit never stages leaves the release tag
     * carrying the old text and the working tree carrying the new.</p>
     */
    @Test
    void documentationPagesWithInstallSnippetsAreBumpedByTheReleaseScript() throws Exception {
        Set<String> pagesWithSnippets = new LinkedHashSet<>();
        for (DocCoordinate coordinate : documentationCoordinates()) {
            // A companion snippet rides its own release line, so an engine cut must
            // never touch the page on its account.
            if (!isCompanionArtifact(coordinate.artifact())) {
                pagesWithSnippets.add(coordinate.page());
            }
        }
        String script = Files.readString(PROJECT_ROOT.resolve("scripts/cut-release.ps1"));

        List<Set<String>> lists = docPageLists(script);
        assertThat(lists)
                .describedAs("cut-release.ps1 must declare a `foreach ($docPage in @(...))` list where it "
                        + "bumps the pages and another where it stages them")
                .hasSize(2);

        for (String page : pagesWithSnippets) {
            assertThat(lists.get(0))
                    .describedAs("%s carries a train install snippet, so cut-release.ps1 must bump it", page)
                    .contains(page);
            assertThat(lists.get(1))
                    .describedAs("%s is bumped by cut-release.ps1, so the release commit must stage it", page)
                    .contains(page);
        }
    }

    /** One versioned GraphCompose coordinate found in a documentation page. */
    private record DocCoordinate(String page, int line, String artifact, String version) {
    }

    /**
     * Every versioned GraphCompose coordinate under {@code docs/}, skipping the trees
     * that pin an old version on purpose. Both documentation guards read this.
     */
    private static List<DocCoordinate> documentationCoordinates() throws IOException {
        Path docs = PROJECT_ROOT.resolve("docs");
        List<DocCoordinate> found = new ArrayList<>();
        List<Path> pages;
        try (Stream<Path> tree = Files.walk(docs)) {
            pages = tree.filter(Files::isRegularFile)
                    .filter(page -> page.getFileName().toString().endsWith(".md"))
                    .filter(page -> !pinsAnOldVersionOnPurpose(docs.relativize(page)))
                    .sorted()
                    .toList();
        }
        for (Path page : pages) {
            String text = Files.readString(page);
            String name = PROJECT_ROOT.relativize(page).toString().replace('\\', '/');
            for (Pattern pattern : List.of(DOCS_MAVEN_COORDINATE, DOCS_GRADLE_COORDINATE)) {
                Matcher coordinate = pattern.matcher(text);
                while (coordinate.find()) {
                    found.add(new DocCoordinate(name, lineOf(text, coordinate.start()),
                            coordinate.group(1), coordinate.group(2).trim()));
                }
            }
        }
        return found;
    }

    /** The {@code $docPage} lists in {@code cut-release.ps1}, in source order: bump first, staging second. */
    private static List<Set<String>> docPageLists(String script) {
        List<Set<String>> lists = new ArrayList<>();
        Matcher loop = Pattern.compile("foreach \\(\\$docPage in @\\(([^)]*)\\)\\)").matcher(script);
        while (loop.find()) {
            Set<String> paths = new LinkedHashSet<>();
            Matcher quoted = Pattern.compile("'([^']+)'").matcher(loop.group(1));
            while (quoted.find()) {
                paths.add(quoted.group(1));
            }
            lists.add(paths);
        }
        return lists;
    }

    /** Historical doc trees, which pin an old version deliberately. Relative to {@code docs/}. */
    private static final String[] DOCS_PINNED_ON_PURPOSE = {
            "archive/", "roadmaps/", "migration/", "private/", "templates/v1-classic/"
    };

    private static final Pattern DOCS_MAVEN_COORDINATE = Pattern.compile(
            "<artifactId>(graph-compose[\\w-]*)</artifactId>\\s*<version>v?([0-9][^<]*)</version>");
    private static final Pattern DOCS_GRADLE_COORDINATE = Pattern.compile(
            "io\\.github\\.demchaav:(graph-compose[\\w-]*):v?([0-9][\\w.\\-]*)");

    private static boolean pinsAnOldVersionOnPurpose(Path relativeToDocs) {
        String path = relativeToDocs.toString().replace('\\', '/');
        for (String prefix : DOCS_PINNED_ON_PURPOSE) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * fonts and emoji carry independent version lines (their own {@code fonts-v*} /
     * {@code emoji-v*} tags), so a docs snippet naming one of them is checked
     * against that module's own pom — never the engine train's target set.
     */
    private Set<String> expectedVersionsFor(String artifact, Set<String> trainTargets) throws Exception {
        if (isCompanionArtifact(artifact)) {
            String module = artifact.substring("graph-compose-".length());
            return Set.of(effectiveVersion(PROJECT_ROOT.resolve(module + "/pom.xml")));
        }
        return trainTargets;
    }

    private static boolean isCompanionArtifact(String artifact) {
        return artifact.equals("graph-compose-fonts") || artifact.equals("graph-compose-emoji");
    }

    private static int lineOf(String text, int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    // ── Install-snippet patterns ────────────────────────────────────
    // Each install snippet check tries the Maven Central format first
    // (canonical from v1.6.6 onwards) and falls back to the legacy
    // JitPack format (kept resolvable for v1.6.5-and-earlier pinned
    // consumers and possibly still present during the migration
    // window). The first regex with a match wins.

    private static final String[] INSTALL_SNIPPET_PATTERNS_README_MAVEN = {
            "<artifactId>graph-compose</artifactId>\\s*<version>v?([0-9][^<]*)</version>",
            "<artifactId>graphcompose</artifactId>\\s*<version>v?([0-9][^<]*)</version>",
            "<artifactId>GraphCompose</artifactId>\\s*<version>v?([0-9][^<]*)</version>"
    };
    private static final String[] INSTALL_SNIPPET_PATTERNS_README_GRADLE = {
            "io\\.github\\.demchaav:graph-compose:v?([0-9][^\")]*)",
            "io\\.github\\.demchaav:graphcompose:v?([0-9][^\")]*)",
            "GraphCompose:v?([0-9][^\")]*)"
    };
    private static final String[] INSTALL_SNIPPET_PATTERNS_SHOWCASE_MAVEN = {
            "&lt;artifactId&gt;graph-compose&lt;/artifactId&gt;\\s*&lt;version&gt;v?([0-9][^&]*)&lt;/version&gt;",
            "&lt;artifactId&gt;graphcompose&lt;/artifactId&gt;\\s*&lt;version&gt;v?([0-9][^&]*)&lt;/version&gt;",
            "&lt;version&gt;v?([0-9][^&]*)&lt;/version&gt;"
    };
    private static final String[] INSTALL_SNIPPET_PATTERNS_SHOWCASE_GRADLE = {
            "io\\.github\\.demchaav:graph-compose:v?([0-9][^')]*)",
            "io\\.github\\.demchaav:graphcompose:v?([0-9][^')]*)",
            "GraphCompose:v?([0-9][^')]*)"
    };

    /**
     * Returns the set of versions an install snippet may legitimately advertise.
     *
     * <p>Only a <strong>final</strong> release ({@code X.Y.Z}, no suffix) lands on Maven
     * Central, so only then must the snippets equal the pom version. For any non-final
     * working version — a normal {@code -SNAPSHOT} development cycle, or a pre-release
     * ({@code X.Y.Z-rc.N} / {@code -alpha} / {@code -beta}), which the publish workflow
     * never ships to Central — the snippets must stay on the <strong>latest published
     * release</strong>, the version a user can actually resolve today, never the
     * in-development or unpublished pre-release version. In the release commit itself
     * {@code cut-release.ps1} bumps the pom to the final release version and rewrites the
     * snippets to match, so once the pom is a concrete final version the two must agree.</p>
     */
    private Set<String> acceptableTargets() throws Exception {
        String pomVersion = effectiveVersion(PROJECT_ROOT.resolve("core/pom.xml"));
        if (pomVersion.matches("\\d+\\.\\d+\\.\\d+")) {
            return Set.of(pomVersion);
        }
        return Set.of(latestPublishedRelease());
    }

    /**
     * The latest published release: the topmost dated {@code CHANGELOG.md} entry
     * ({@code ## vX.Y.Z — YYYY-MM-DD}). {@code find()} returns the newest such
     * entry; a {@code — Planned} entry is skipped because it carries no date.
     */
    private String latestPublishedRelease() throws Exception {
        String changelog = Files.readString(PROJECT_ROOT.resolve("CHANGELOG.md"));
        // Only a FINAL semver header (## vX.Y.Z — YYYY-MM-DD) counts as published on
        // Maven Central. A dated pre-release header (## vX.Y.Z-rc.N — …) must NOT be
        // treated as the published version — pre-releases never ship to Central — so the
        // version group is anchored to \d+\.\d+\.\d+ with no suffix.
        Matcher released = Pattern.compile("^## v(\\d+\\.\\d+\\.\\d+)\\s*[\\u2014\\-]\\s*\\d{4}-\\d{2}-\\d{2}", Pattern.MULTILINE)
                .matcher(changelog);
        assertThat(released.find())
                .describedAs("CHANGELOG.md must contain a dated final release entry (## vX.Y.Z — YYYY-MM-DD) to anchor the install snippets")
                .isTrue();
        return released.group(1);
    }

    /**
     * One {@code ## vX.Y.Z — <marker>} entry: the release line it names, and whatever
     * follows the version on that line — a date once shipped, anything else while open.
     */
    record ChangelogEntry(String version, String marker) {
        boolean isDated() {
            return marker.matches("\\d{4}-\\d{2}-\\d{2}\\b.*");
        }
    }

    /**
     * Every {@code ## vX.Y.Z} entry in {@code changelog}, in file order.
     *
     * <p>Deliberately drivable from a string rather than reading the file: an entry this
     * stops recognising is one {@link #versionDriftProblem} cannot compare, and a guard
     * that compares nothing is green forever without having failed once.
     * {@code ChangelogVersionParsingTest} holds the shapes it must keep seeing.</p>
     */
    static List<ChangelogEntry> changelogEntriesIn(String changelog) {
        Matcher heading = Pattern.compile(
                        "^## v(\\d+\\.\\d+\\.\\d+)[ \\t]*(?:[\\u2014\\-][ \\t]*)?(.*)$", Pattern.MULTILINE)
                .matcher(changelog);
        List<ChangelogEntry> entries = new ArrayList<>();
        while (heading.find()) {
            entries.add(new ChangelogEntry(heading.group(1), heading.group(2).trim()));
        }
        return entries;
    }

    /**
     * What is wrong between the open {@code CHANGELOG.md} entry and {@code pomVersion},
     * or {@code null} when the two agree — pure, so the red paths can be driven from
     * strings instead of only ever being observed on the repository's own files.
     *
     * <p>An entry counts as open because it carries no <em>date</em>, never because of
     * the word after the version. Recognising one spelling and skipping the others is
     * how this check would go quiet for a whole development line rather than fail: the
     * 2.1.0 line was opened as {@code — in progress} and stayed that way for 31 commits
     * while the poms named a different release, which is precisely the drift this
     * exists to catch. The spelling is then held separately, because
     * {@code cut-release.ps1} dates a heading by matching the literal
     * {@code — Planned} and silently leaves any other wording undated.</p>
     */
    static String versionDriftProblem(String changelog, String pomVersion) {
        List<ChangelogEntry> entries = changelogEntriesIn(changelog);
        List<ChangelogEntry> open = entries.stream().filter(entry -> !entry.isDated()).toList();

        if (open.isEmpty()) {
            return null;
        }
        if (open.size() > 1) {
            return "CHANGELOG.md leaves %d entries undated (%s) — two open entries leave the next release ambiguous"
                    .formatted(open.size(), open.stream().map(ChangelogEntry::version).toList());
        }

        ChangelogEntry entry = open.get(0);
        if (!entries.get(0).equals(entry)) {
            return "the open CHANGELOG entry (v%s) sits below a shipped release — an undated entry under a dated one is a leftover, not the next line"
                    .formatted(entry.version());
        }
        if (!"Planned".equals(entry.marker())) {
            return ("the open CHANGELOG entry reads '## v%s — %s'; cut-release.ps1 dates a heading by matching "
                    + "the literal '— Planned', so this one would go out undated")
                    .formatted(entry.version(), entry.marker());
        }

        String pomLine = releaseLineOf(pomVersion);
        if (!entry.version().equals(pomLine)) {
            return ("the open CHANGELOG entry (v%s) and the working pom version (%s) name different releases — "
                    + "whichever is right, the other was left behind")
                    .formatted(entry.version(), pomVersion);
        }
        return null;
    }

    /**
     * The {@code X.Y.Z} release line a working version belongs to. {@code 2.2.0-SNAPSHOT}
     * and {@code 2.2.0-rc.1} both target {@code 2.2.0}, which is the version the open
     * CHANGELOG heading names while either sits in the poms.
     */
    static String releaseLineOf(String version) {
        Matcher line = Pattern.compile("^(\\d+\\.\\d+\\.\\d+)").matcher(version);
        assertThat(line.find())
                .describedAs("the working pom version (%s) must start with an X.Y.Z release line", version)
                .isTrue();
        return line.group(1);
    }

    /**
     * Returns the captured group of the first regex in {@code patterns} that
     * matches. Fails with a descriptive message listing every pattern tried if
     * none matches.
     */
    private static String firstMatchingGroup(String text, String[] patterns) {
        for (String regex : patterns) {
            Matcher matcher = Pattern.compile(regex).matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        assertThat(false)
                .describedAs("Expected at least one version token matching one of these patterns: %s", String.join(" | ", patterns))
                .isTrue();
        throw new IllegalStateException("unreachable");
    }

    private static String firstGroup(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        assertThat(matcher.find())
                .describedAs("Expected a GraphCompose version token matching /%s/ in the scanned file", regex)
                .isTrue();
        return matcher.group(1);
    }

    private static boolean declaresOwnVersion(Path pom) throws Exception {
        return directChild(parse(pom).getDocumentElement(), "version") != null;
    }

    private static String pinnedVersionProperty(Path pom, String property) throws IOException {
        Matcher matcher = Pattern.compile("<" + Pattern.quote(property) + ">([^<]+)</" + Pattern.quote(property) + ">")
                .matcher(Files.readString(pom));
        assertThat(matcher.find())
                .describedAs("expected a <%s> property in %s", property, pom)
                .isTrue();
        return matcher.group(1).trim();
    }

    private static String effectiveVersion(Path pom) throws Exception {
        Element project = parse(pom).getDocumentElement();

        Element ownVersion = directChild(project, "version");
        if (ownVersion != null) {
            return ownVersion.getTextContent().trim();
        }
        Element parent = directChild(project, "parent");
        if (parent != null) {
            Element parentVersion = directChild(parent, "version");
            if (parentVersion != null) {
                return parentVersion.getTextContent().trim();
            }
        }
        throw new IllegalStateException("No <project>/<version> or inherited <parent>/<version> in " + pom);
    }

    private static boolean declaresDependencyOn(Element project, String artifactId) {
        Element deps = directChild(project, "dependencies");
        if (deps == null) {
            return false;
        }
        NodeList children = deps.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("dependency")) {
                Element aid = directChild((Element) node, "artifactId");
                if (aid != null && artifactId.equals(aid.getTextContent().trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Element directChild(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
                return (Element) node;
            }
        }
        return null;
    }

    private static Document parse(Path pom) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(pom.toFile());
    }
}
