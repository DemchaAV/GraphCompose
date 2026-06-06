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
 * (see {@code aggregator/pom.xml}): the library {@code pom.xml} is the single
 * version source, the aggregator bumps every module in lockstep via
 * {@code versions:set}, and the child modules inherit their version rather than
 * pinning a literal. This test fails the build the moment a bump leaves any
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

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void everyModuleResolvesToTheRootProjectVersion() throws Exception {
        String root = effectiveVersion(PROJECT_ROOT.resolve("pom.xml"));

        assertThat(effectiveVersion(PROJECT_ROOT.resolve("aggregator/pom.xml")))
                .describedAs("aggregator/pom.xml version must equal root pom.xml version (%s)", root)
                .isEqualTo(root);
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("examples/pom.xml")))
                .describedAs("examples must inherit the root version (%s)", root)
                .isEqualTo(root);
        assertThat(effectiveVersion(PROJECT_ROOT.resolve("benchmarks/pom.xml")))
                .describedAs("benchmarks must inherit the root version (%s)", root)
                .isEqualTo(root);
    }

    @Test
    void childModulesInheritVersionInsteadOfDeclaringTheirOwn() throws Exception {
        assertThat(declaresOwnVersion(PROJECT_ROOT.resolve("examples/pom.xml")))
                .describedAs("examples/pom.xml must inherit <version> from the aggregator parent, not declare its own")
                .isFalse();
        assertThat(declaresOwnVersion(PROJECT_ROOT.resolve("benchmarks/pom.xml")))
                .describedAs("benchmarks/pom.xml must inherit <version> from the aggregator parent, not declare its own")
                .isFalse();
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
        assertThat(firstGroup(site, "v([0-9][0-9.]*)\\s*&middot;\\s*MIT"))
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
        targets.add(effectiveVersion(PROJECT_ROOT.resolve("pom.xml")));
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
