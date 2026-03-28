package com.demcha.compose.devtool;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PreviewCompilerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReloadChangedProjectClassesWithChildFirstClassLoader() throws Exception {
        var workspace = createWorkspace("com.demcha.preview.TempProvider");

        writeSource(
                workspace.projectRoot().resolve("src/main/java/com/demcha/sample/MessageSource.java"),
                """
                        package com.demcha.sample;

                        public final class MessageSource {
                            private MessageSource() {
                            }

                            public static String message() {
                                return "v1";
                            }
                        }
                        """);

        writeSource(
                workspace.projectRoot().resolve("src/test/java/com/demcha/preview/TempProvider.java"),
                """
                        package com.demcha.preview;

                        import com.demcha.compose.devtool.DevToolPreviewProvider;
                        import com.demcha.sample.MessageSource;
                        import org.apache.pdfbox.pdmodel.PDDocument;
                        import org.apache.pdfbox.pdmodel.PDPage;

                        public final class TempProvider implements DevToolPreviewProvider {
                            @Override
                            public PDDocument buildPreview() {
                                var document = new PDDocument();
                                document.addPage(new PDPage());
                                document.getDocumentInformation().setTitle(MessageSource.message());
                                return document;
                            }
                        }
                        """);

        var compiler = new PreviewCompiler();
        var firstCompilation = compiler.compile(workspace, 1);
        assertThat(firstCompilation.success()).isTrue();

        try (var staleParent = new URLClassLoader(
                workspace.buildClassLoaderUrls(firstCompilation.outputDir()),
                PreviewCompilerTest.class.getClassLoader())) {

            assertThat(documentTitle(staleParent, workspace.previewProviderClassName())).isEqualTo("v1");

            writeSource(
                    workspace.projectRoot().resolve("src/main/java/com/demcha/sample/MessageSource.java"),
                    """
                            package com.demcha.sample;

                            public final class MessageSource {
                                private MessageSource() {
                                }

                                public static String message() {
                                    return "v2";
                                }
                            }
                            """);

            var secondCompilation = compiler.compile(workspace, 2);
            assertThat(secondCompilation.success()).isTrue();

            try (var reloaded = new SelectiveChildFirstClassLoader(
                    workspace.buildClassLoaderUrls(secondCompilation.outputDir()),
                    staleParent,
                    List.of("com.demcha."),
                    List.of("com.demcha.compose.devtool."))) {

                assertThat(documentTitle(reloaded, workspace.previewProviderClassName())).isEqualTo("v2");
            }
        }
    }

    @Test
    void shouldReloadResourcesWithoutRecompiling() throws Exception {
        var workspace = createWorkspace("com.demcha.preview.ResourceProvider");

        writeSource(
                workspace.projectRoot().resolve("src/test/java/com/demcha/preview/ResourceProvider.java"),
                """
                        package com.demcha.preview;

                        import com.demcha.compose.devtool.DevToolPreviewProvider;
                        import org.apache.pdfbox.pdmodel.PDDocument;
                        import org.apache.pdfbox.pdmodel.PDPage;

                        import java.nio.charset.StandardCharsets;

                        public final class ResourceProvider implements DevToolPreviewProvider {
                            @Override
                            public PDDocument buildPreview() throws Exception {
                                var document = new PDDocument();
                                document.addPage(new PDPage());

                                try (var input = getClass().getClassLoader().getResourceAsStream("preview.txt")) {
                                    var value = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
                                    document.getDocumentInformation().setTitle(value);
                                }

                                return document;
                            }
                        }
                        """);

        Path resourcePath = workspace.projectRoot().resolve("src/test/resources/preview.txt");
        writeText(resourcePath, "alpha");

        var compiler = new PreviewCompiler();
        var compilation = compiler.compile(workspace, 1);
        assertThat(compilation.success()).isTrue();

        try (var firstLoader = new SelectiveChildFirstClassLoader(
                workspace.buildClassLoaderUrls(compilation.outputDir()),
                PreviewCompilerTest.class.getClassLoader(),
                List.of("com.demcha."),
                List.of("com.demcha.compose.devtool."))) {

            assertThat(documentTitle(firstLoader, workspace.previewProviderClassName())).isEqualTo("alpha");
        }

        writeText(resourcePath, "beta");

        try (var secondLoader = new SelectiveChildFirstClassLoader(
                workspace.buildClassLoaderUrls(compilation.outputDir()),
                PreviewCompilerTest.class.getClassLoader(),
                List.of("com.demcha."),
                List.of("com.demcha.compose.devtool."))) {

            assertThat(documentTitle(secondLoader, workspace.previewProviderClassName())).isEqualTo("beta");
        }
    }

    @Test
    void shouldReturnCompilerDiagnosticsForBrokenSources() throws Exception {
        var workspace = createWorkspace("com.demcha.preview.BrokenProvider");

        writeSource(
                workspace.projectRoot().resolve("src/test/java/com/demcha/preview/BrokenProvider.java"),
                """
                        package com.demcha.preview;

                        import com.demcha.compose.devtool.DevToolPreviewProvider;
                        import org.apache.pdfbox.pdmodel.PDDocument;

                        public final class BrokenProvider implements DevToolPreviewProvider {
                            @Override
                            public PDDocument buildPreview() {
                                return new PDDocument()
                            }
                        }
                        """);

        var result = new PreviewCompiler().compile(workspace, 1);

        assertThat(result.success()).isFalse();
        assertThat(result.diagnostics()).contains("BrokenProvider.java");
        assertThat(result.diagnostics()).contains("';'");
    }

    private DevToolWorkspace createWorkspace(String providerClassName) throws Exception {
        Path root = tempDir.resolve("workspace");
        Files.createDirectories(root.resolve("src/main/java"));
        Files.createDirectories(root.resolve("src/test/java"));
        Files.createDirectories(root.resolve("src/main/resources"));
        Files.createDirectories(root.resolve("src/test/resources"));

        return new DevToolWorkspace(
                root,
                List.of(root.resolve("src/main/java"), root.resolve("src/test/java")),
                List.of(root.resolve("src/main/resources"), root.resolve("src/test/resources")),
                root.resolve("target/devtool-classes"),
                providerClassName);
    }

    private void writeSource(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void writeText(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private String documentTitle(ClassLoader loader, String providerClassName) throws Exception {
        Class<?> providerType = loader.loadClass(providerClassName);
        DevToolPreviewProvider provider = (DevToolPreviewProvider) providerType.getDeclaredConstructor().newInstance();

        try (PDDocument document = provider.buildPreview()) {
            return document.getDocumentInformation().getTitle();
        }
    }
}
