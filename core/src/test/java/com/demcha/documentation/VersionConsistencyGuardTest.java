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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * <p>The snippet checks accept the version as matching <strong>either</strong>
 * the current {@code pom.xml} version <strong>or</strong> the version named in
 * the top {@code CHANGELOG.md} {@code Planned} entry. This makes the
 * forward-looking Maven Central snippet (which has to advertise the
 * about-to-ship version so users copy a coord that will resolve once the tag
 * lands) compatible with the pre-cut window where {@code pom.xml} still carries
 * the previous release version. {@code cut-release.ps1} bumps both pom and
 * snippet to the same target in the release commit, after which the two paths
 * converge and the test continues to pass.
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

    @Test
    void readmeInstallSnippetsMatchTheProjectVersion() throws Exception {
        Set<String> targets = acceptableTargets();
        String readme = Files.readString(PROJECT_ROOT.resolve("README.md"));

        String mavenSnippetVersion = firstMatchingGroup(readme, INSTALL_SNIPPET_PATTERNS_README_MAVEN);
        String gradleSnippetVersion = firstMatchingGroup(readme, INSTALL_SNIPPET_PATTERNS_README_GRADLE);

        assertThat(mavenSnippetVersion)
                .describedAs("README Maven install snippet must reference the current pom or CHANGELOG Planned version (one of %s)", targets)
                .isIn(targets);
        assertThat(gradleSnippetVersion)
                .describedAs("README Gradle install snippet must reference the current pom or CHANGELOG Planned version (one of %s)", targets)
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
                .describedAs("web/index.html JSON-LD softwareVersion must equal the current pom or planned version (one of %s)", targets)
                .isIn(targets);
        // Match up to the delimiter (space before `&middot;`) rather than
        // digits-and-dots only, so a pre-release version like 2.0.0-rc.1 is
        // captured too — consistent with the JSON-LD and snippet patterns.
        assertThat(firstGroup(site, "v([0-9][^\\s&]*)\\s*&middot;\\s*MIT"))
                .describedAs("web/index.html hero version badge must equal the current pom or planned version (one of %s)", targets)
                .isIn(targets);
        assertThat(firstMatchingGroup(site, INSTALL_SNIPPET_PATTERNS_SHOWCASE_MAVEN))
                .describedAs("web/index.html Maven install snippet must equal the current pom or planned version (one of %s)", targets)
                .isIn(targets);
        assertThat(firstMatchingGroup(site, INSTALL_SNIPPET_PATTERNS_SHOWCASE_GRADLE))
                .describedAs("web/index.html Gradle install snippet must equal the current pom or planned version (one of %s)", targets)
                .isIn(targets);
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
     * Returns the set of versions that any install snippet may legitimately
     * advertise: the current {@code pom.xml} version always, plus the version
     * named in the top {@code CHANGELOG.md} {@code Planned} entry if one
     * exists. The Planned entry covers the pre-cut window where {@code pom.xml}
     * still carries the previous release version while the README + showcase
     * already advertise the about-to-ship version.
     */
    private Set<String> acceptableTargets() throws Exception {
        Set<String> targets = new LinkedHashSet<>();
        targets.add(effectiveVersion(PROJECT_ROOT.resolve("core/pom.xml")));
        String changelog = Files.readString(PROJECT_ROOT.resolve("CHANGELOG.md"));
        Matcher planned = Pattern.compile("^## v([0-9][^ \\n]*)\\s*[\\u2014\\-]\\s*Planned\\b", Pattern.MULTILINE)
                .matcher(changelog);
        if (planned.find()) {
            targets.add(planned.group(1));
        }
        return targets;
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
