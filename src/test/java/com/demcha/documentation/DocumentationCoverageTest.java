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
    void readmeQuickStartShouldUseCanonicalDsl() throws IOException {
        String readme = Files.readString(PROJECT_ROOT.resolve("README.md"));
        int quickStart = readme.indexOf("## Quick start");
        int builtIns = readme.indexOf("### Built-in templates (compose-first)");
        assertThat(quickStart).isGreaterThanOrEqualTo(0);
        assertThat(builtIns).isGreaterThan(quickStart);

        String quickStartSection = readme.substring(quickStart, builtIns);
        assertThat(quickStartSection).contains("GraphCompose.document(Path.of(\"output.pdf\"))");
        assertThat(quickStartSection).contains("document.pageFlow(page -> page");
        assertThat(quickStartSection).contains(".module(\"Summary\", module -> module.paragraph(\"Hello GraphCompose\"))");
        assertThat(quickStartSection).doesNotContain("import com.demcha.compose.engine");
        assertThat(quickStartSection).doesNotContain("document.dsl()");
        assertThat(quickStartSection).doesNotContain("try (PdfComposer composer = GraphCompose.pdf(");
    }

    @Test
    void readmeLinePrimitiveSectionShouldUseCanonicalDsl() throws IOException {
        String readme = Files.readString(PROJECT_ROOT.resolve("README.md"));
        int linePrimitive = readme.indexOf("## Line primitive");
        int architecture = readme.indexOf("## Architecture at a glance");
        assertThat(linePrimitive).isGreaterThanOrEqualTo(0);
        assertThat(architecture).isGreaterThan(linePrimitive);

        String linePrimitiveSection = readme.substring(linePrimitive, architecture);
        assertThat(linePrimitiveSection).contains("document.pageFlow()");
        assertThat(linePrimitiveSection).contains(".addDivider(");
        assertThat(linePrimitiveSection).contains(".addShape(");
        assertThat(linePrimitiveSection).contains("DocumentColor.ROYAL_BLUE");
        assertThat(linePrimitiveSection).doesNotContain("ComponentColor.");
        assertThat(linePrimitiveSection).doesNotContain("document.dsl()");
        assertThat(linePrimitiveSection).doesNotContain("composer.componentBuilder()");
        assertThat(linePrimitiveSection).doesNotContain("GraphCompose.pdf(");
    }

    @Test
    void readmeTableSectionShouldUseCanonicalDsl() throws IOException {
        String readme = Files.readString(PROJECT_ROOT.resolve("README.md"));
        int tableComponent = readme.indexOf("## Table component");
        int linePrimitive = readme.indexOf("## Line primitive");
        assertThat(tableComponent).isGreaterThanOrEqualTo(0);
        assertThat(linePrimitive).isGreaterThan(tableComponent);

        String tableSection = readme.substring(tableComponent, linePrimitive);
        assertThat(tableSection).contains("document.pageFlow()");
        assertThat(tableSection).contains(".addTable(");
        assertThat(tableSection).contains(".header(");
        assertThat(tableSection).contains(".rows(");
        assertThat(tableSection).contains("DocumentTableColumn.fixed(90)");
        assertThat(tableSection).doesNotContain("TableColumnLayout.");
        assertThat(tableSection).doesNotContain("TableCellLayoutStyle.");
        assertThat(tableSection).doesNotContain(".row(\"Role\", \"Owner\", \"Status\")");
        assertThat(tableSection).doesNotContain("document.dsl()");
        assertThat(tableSection).doesNotContain("composer.componentBuilder()");
        assertThat(tableSection).doesNotContain("GraphCompose.pdf(");
    }

    @Test
    void readmeContainerGuidanceShouldPreferCanonicalDsl() throws IOException {
        String readme = Files.readString(PROJECT_ROOT.resolve("README.md"));
        int containers = readme.indexOf("### 4. Containers express structure");
        int templateLayer = readme.indexOf("### 5. The template layer is optional");
        assertThat(containers).isGreaterThanOrEqualTo(0);
        assertThat(templateLayer).isGreaterThan(containers);

        String containersSection = readme.substring(containers, templateLayer);
        assertThat(containersSection).contains("document.pageFlow()");
        assertThat(containersSection).contains("section()");
        assertThat(containersSection).doesNotContain("vContainer(");
        assertThat(containersSection).doesNotContain("hContainer(");
        assertThat(containersSection).doesNotContain("moduleBuilder(");
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
