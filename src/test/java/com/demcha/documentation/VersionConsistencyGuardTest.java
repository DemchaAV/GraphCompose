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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards that the GraphCompose version propagates identically to every module
 * and to the README install snippets.
 *
 * <p>This is the safety net behind the aggregator-reactor version model
 * (see {@code aggregator/pom.xml}): the library {@code pom.xml} is the single
 * version source, the aggregator bumps every module in lockstep via
 * {@code versions:set}, and the child modules inherit their version rather than
 * pinning a literal. This test fails the build the moment a bump leaves any
 * module — or the README copy-paste snippet — pointing at a different version,
 * which is the drift class that previously let the benchmarks module run
 * against the previous release.
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
        String root = effectiveVersion(PROJECT_ROOT.resolve("pom.xml"));
        String readme = Files.readString(PROJECT_ROOT.resolve("README.md"));

        String mavenSnippetVersion = firstGroup(readme,
                "<artifactId>GraphCompose</artifactId>\\s*<version>v?([0-9][^<]*)</version>");
        String gradleSnippetVersion = firstGroup(readme,
                "GraphCompose:v?([0-9][^\")]*)");

        assertThat(mavenSnippetVersion)
                .describedAs("README Maven/JitPack snippet must reference the current project version (%s)", root)
                .isEqualTo(root);
        assertThat(gradleSnippetVersion)
                .describedAs("README Gradle/JitPack snippet must reference the current project version (%s)", root)
                .isEqualTo(root);
    }

    private static String firstGroup(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        assertThat(matcher.find())
                .describedAs("Expected a GraphCompose install-snippet version matching /%s/ in README.md", regex)
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
