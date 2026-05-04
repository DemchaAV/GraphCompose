package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentationCoverageTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path DOCUMENT_SOURCE_ROOT = PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document");
    private static final Pattern PUBLIC_TOP_LEVEL_TYPE = Pattern.compile(
            "\\bpublic\\s+(?:final\\s+)?(?:class|interface|record|enum)\\b");
    private static final Pattern PUBLIC_TYPE_WITH_JAVADOC = Pattern.compile(
            "(?s)/\\*\\*.*?\\*/\\s*(?:@[^\\r\\n]+\\s*)*public\\s+(?:final\\s+)?(?:class|interface|record|enum)\\b");

    @Test
    void canonicalDocumentPackagesShouldDeclarePackageInfo() throws IOException {
        List<String> missing = new ArrayList<>();

        try (Stream<Path> directories = Files.walk(DOCUMENT_SOURCE_ROOT)) {
            directories
                    .filter(Files::isDirectory)
                    .forEach(directory -> {
                        try (Stream<Path> files = Files.list(directory)) {
                            boolean hasJavaSources = files
                                    .anyMatch(path -> path.toString().endsWith(".java") && !path.getFileName().toString().equals("package-info.java"));
                            if (hasJavaSources && Files.notExists(directory.resolve("package-info.java"))) {
                                missing.add(PROJECT_ROOT.relativize(directory).toString().replace('\\', '/'));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to inspect " + directory, e);
                        }
                    });
        }

        assertThat(missing)
                .describedAs("Every canonical document package with Java sources must declare package-info.java")
                .isEmpty();
    }

    @Test
    void canonicalPublicTypesShouldExposeTopLevelJavadocs() throws IOException {
        List<String> missing = new ArrayList<>();

        try (Stream<Path> files = Files.walk(DOCUMENT_SOURCE_ROOT)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                    .forEach(path -> {
                        try {
                            String source = Files.readString(path);
                            if (PUBLIC_TOP_LEVEL_TYPE.matcher(source).find()
                                    && !PUBLIC_TYPE_WITH_JAVADOC.matcher(source).find()) {
                                missing.add(PROJECT_ROOT.relativize(path).toString().replace('\\', '/'));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to inspect " + path, e);
                        }
                    });
        }

        assertThat(missing)
                .describedAs("Every public canonical document type must start with a top-level Javadoc block")
                .isEmpty();
    }

    @Test
    void highLevelApiAnchorsShouldRetainMethodJavadocs() throws IOException {
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/GraphCompose.java"),
                "public static DocumentBuilder document()");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/GraphCompose.java"),
                "public static DocumentBuilder document(Path outputFile)");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/GraphCompose.java"),
                "public DocumentSession create()");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/GraphCompose.java"),
                "public DocumentBuilder guideLines(boolean enabled)");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/api/DocumentSession.java"),
                "public DocumentDsl dsl()");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/api/DocumentSession.java"),
                "public DocumentSession compose(Consumer<DocumentDsl> spec)");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/api/DocumentSession.java"),
                "public PageFlowBuilder pageFlow()");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/api/DocumentSession.java"),
                "public ContainerNode pageFlow(Consumer<PageFlowBuilder> spec)");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/api/DocumentSession.java"),
                "public DocumentSession guideLines(boolean enabled)");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/api/DocumentSession.java"),
                "public LayoutGraph layoutGraph()");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/dsl/DocumentDsl.java"),
                "public ContainerNode pageFlow(Consumer<PageFlowBuilder> spec)");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/dsl/AbstractFlowBuilder.java"),
                "public T addSection(String name, Consumer<SectionBuilder> spec)");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/dsl/TableBuilder.java"),
                "public TableBuilder header(String... values)");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/dsl/TableBuilder.java"),
                "public TableBuilder rows(String[]... rows)");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/dsl/TableBuilder.java"),
                "public TableBuilder headerStyle(DocumentTableStyle style)");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/api/DocumentSession.java"),
                "public byte[] toPdfBytes()");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/api/DocumentSession.java"),
                "public void writePdf(OutputStream output)");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/api/DocumentSession.java"),
                "public void buildPdf()");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/dsl/DocumentDsl.java"),
                "public PageFlowBuilder pageFlow()");
        assertHasJavadocBefore(
                PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/dsl/PageFlowBuilder.java"),
                "public ContainerNode build()");
    }

    @Test
    void readmeShouldUseCanonicalDslAndAvoidLegacyApis() throws IOException {
        String readme = Files.readString(PROJECT_ROOT.resolve("README.md"));

        // Canonical-DSL fingerprints — the slim v1.5 landing README must
        // surface at least one example using the canonical authoring path.
        assertThat(readme).contains("GraphCompose.document(");
        assertThat(readme).contains("DocumentSession");
        assertThat(readme).contains("document.pageFlow(");
        assertThat(readme).contains("BusinessTheme");

        // Legacy-API guard — the README may not advertise any retired
        // entry point, builder shape, or pre-canonical field-based CV
        // type, even if examples that demonstrate them still exist
        // elsewhere in the codebase.
        assertThat(readme)
                .doesNotContain("import com.demcha.compose.engine")
                .doesNotContain("document.dsl()")
                .doesNotContain("composer.componentBuilder()")
                .doesNotContain("GraphCompose.pdf(")
                .doesNotContain("PdfComposer")
                .doesNotContain("TemplateBuilder")
                .doesNotContain("ComponentColor.")
                .doesNotContain("TableColumnLayout.")
                .doesNotContain("TableCellLayoutStyle.")
                .doesNotContain("vContainer(")
                .doesNotContain("hContainer(")
                .doesNotContain("moduleBuilder(")
                .doesNotContain("MainPageCV")
                .doesNotContain("MainPageCvDTO")
                .doesNotContain("ModuleYml");
    }

    @Test
    void contributingGuideShouldRequireCleanVerifyGate() throws IOException {
        String contributing = Files.readString(PROJECT_ROOT.resolve("CONTRIBUTING.md"));
        assertThat(contributing).contains("./mvnw -B -ntp clean verify");
        assertThat(contributing).doesNotContain("Run the full test suite with `mvn test`.");
    }

    private void assertHasJavadocBefore(Path file, String signature) throws IOException {
        String source = Files.readString(file);
        int signatureIndex = source.indexOf(signature);
        assertThat(signatureIndex)
                .describedAs("Expected signature %s in %s", signature, PROJECT_ROOT.relativize(file))
                .isGreaterThanOrEqualTo(0);

        String prefix = source.substring(0, signatureIndex);
        int javadocIndex = prefix.lastIndexOf("/**");
        int closingIndex = prefix.lastIndexOf("*/");
        int lineGap = prefix.substring(Math.max(closingIndex, 0)).split("\\R", -1).length - 1;

        assertThat(javadocIndex)
                .describedAs("Expected Javadoc block before %s in %s", signature, PROJECT_ROOT.relativize(file))
                .isGreaterThanOrEqualTo(0);
        assertThat(closingIndex)
                .describedAs("Expected Javadoc terminator before %s in %s", signature, PROJECT_ROOT.relativize(file))
                .isGreaterThan(javadocIndex);
        assertThat(lineGap)
                .describedAs("Expected Javadoc to be adjacent to %s in %s", signature, PROJECT_ROOT.relativize(file))
                .isLessThanOrEqualTo(2);
    }
}
