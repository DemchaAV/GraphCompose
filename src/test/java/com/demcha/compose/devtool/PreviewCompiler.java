package com.demcha.compose.devtool;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles the GraphCompose preview sources into a fresh output directory.
 */
public final class PreviewCompiler {
    private static final Pattern LOMBOK_VERSION_PATTERN =
            Pattern.compile("<lombok\\.version>([^<]+)</lombok\\.version>");

    public CompilationResult compile(DevToolWorkspace workspace, long revision) {
        Objects.requireNonNull(workspace, "workspace");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return CompilationResult.failure(
                    "No system Java compiler found. Run the dev tool on a full JDK, not a JRE.",
                    0,
                    0);
        }

        List<Path> sourceFiles;
        try {
            sourceFiles = workspace.collectJavaSources();
        } catch (IOException ex) {
            return CompilationResult.failure("Failed to collect Java sources:%n%s".formatted(stackTrace(ex)), 0, 0);
        }

        if (sourceFiles.isEmpty()) {
            return CompilationResult.failure("No Java sources found in watched source roots.", 0, 0);
        }

        Path outputDir = workspace.compiledOutputRoot().resolve("rev-" + revision);
        long startedAt = System.nanoTime();
        deleteDirectoryQuietly(outputDir);

        try {
            Files.createDirectories(outputDir);
        } catch (IOException ex) {
            return CompilationResult.failure(
                    "Failed to create compiler output directory %s:%n%s"
                            .formatted(outputDir, stackTrace(ex)),
                    0,
                    sourceFiles.size());
        }

        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var stdout = new StringWriter();

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnostics,
                Locale.ROOT,
                StandardCharsets.UTF_8)) {

            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(outputDir));
            fileManager.setLocationFromPaths(StandardLocation.SOURCE_PATH, workspace.existingJavaRoots());

            var compilationUnits = fileManager.getJavaFileObjectsFromPaths(sourceFiles);
            var compilerClasspath = buildCompilerClasspath(workspace);

            var options = new ArrayList<String>();
            options.add("--release");
            options.add("21");
            options.add("-encoding");
            options.add("UTF-8");
            options.add("-classpath");
            options.add(String.join(System.getProperty("path.separator"), compilerClasspath));
            options.add("-processorpath");
            options.add(String.join(System.getProperty("path.separator"), compilerClasspath));

            boolean success = Boolean.TRUE.equals(compiler.getTask(
                    stdout,
                    fileManager,
                    diagnostics,
                    options,
                    null,
                    compilationUnits).call());

            long compileMillis = (System.nanoTime() - startedAt) / 1_000_000;
            String diagnosticText = formatDiagnostics(diagnostics, stdout.toString());

            if (!success) {
                deleteDirectoryQuietly(outputDir);
                return CompilationResult.failure(diagnosticText, compileMillis, sourceFiles.size());
            }

            return CompilationResult.success(outputDir, compileMillis, sourceFiles.size());
        } catch (IOException ex) {
            deleteDirectoryQuietly(outputDir);
            long compileMillis = (System.nanoTime() - startedAt) / 1_000_000;
            return CompilationResult.failure(
                    "Compilation infrastructure failed:%n%s".formatted(stackTrace(ex)),
                    compileMillis,
                    sourceFiles.size());
        }
    }

    static void deleteDirectoryQuietly(Path path) {
        if (path == null || Files.notExists(path)) {
            return;
        }

        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(candidate -> {
                try {
                    Files.deleteIfExists(candidate);
                } catch (IOException ignored) {
                    // Best effort cleanup only.
                }
            });
        } catch (IOException ignored) {
            // Best effort cleanup only.
        }
    }

    private List<String> buildCompilerClasspath(DevToolWorkspace workspace) {
        var separator = System.getProperty("path.separator");
        var entries = new ArrayList<>(List.of(System.getProperty("java.class.path", "").split(Pattern.quote(separator))));
        entries.removeIf(String::isBlank);

        findLombokJar(workspace).ifPresent(path -> {
            String lombokPath = path.toAbsolutePath().normalize().toString();
            if (!entries.contains(lombokPath)) {
                entries.add(lombokPath);
            }
        });

        return entries;
    }

    private Optional<Path> findLombokJar(DevToolWorkspace workspace) {
        return readLombokVersionFromPom(workspace.projectRoot())
                .flatMap(this::resolveLombokJar)
                .or(() -> findLatestInstalledLombokJar());
    }

    private Optional<String> readLombokVersionFromPom(Path projectRoot) {
        Path pom = projectRoot.resolve("pom.xml");
        if (Files.notExists(pom)) {
            return Optional.empty();
        }

        try {
            String content = Files.readString(pom, StandardCharsets.UTF_8);
            Matcher matcher = LOMBOK_VERSION_PATTERN.matcher(content);
            if (matcher.find()) {
                return Optional.of(matcher.group(1).trim());
            }
        } catch (IOException ignored) {
            // Best effort only.
        }

        return Optional.empty();
    }

    private Optional<Path> resolveLombokJar(String version) {
        Path jar = Path.of(System.getProperty("user.home"), ".m2", "repository",
                "org", "projectlombok", "lombok", version, "lombok-%s.jar".formatted(version));
        return Files.isRegularFile(jar) ? Optional.of(jar) : Optional.empty();
    }

    private Optional<Path> findLatestInstalledLombokJar() {
        Path lombokRoot = Path.of(System.getProperty("user.home"), ".m2", "repository", "org", "projectlombok",
                "lombok");
        if (Files.notExists(lombokRoot)) {
            return Optional.empty();
        }

        try (var stream = Files.list(lombokRoot)) {
            return stream.filter(Files::isDirectory)
                    .sorted()
                    .map(versionDir -> versionDir.resolve("lombok-%s.jar".formatted(versionDir.getFileName())))
                    .filter(Files::isRegularFile)
                    .reduce((first, second) -> second);
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private String formatDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics, String stdout) {
        var builder = new StringBuilder();

        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            JavaFileObject source = diagnostic.getSource();
            String sourceName = source == null ? "<unknown>" : source.getName();

            builder.append(diagnostic.getKind())
                    .append(" ")
                    .append(sourceName);

            if (diagnostic.getLineNumber() >= 0) {
                builder.append(":").append(diagnostic.getLineNumber());
                if (diagnostic.getColumnNumber() >= 0) {
                    builder.append(":").append(diagnostic.getColumnNumber());
                }
            }

            builder.append(System.lineSeparator())
                    .append(diagnostic.getMessage(Locale.ROOT))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }

        if (!stdout.isBlank()) {
            builder.append(stdout.strip()).append(System.lineSeparator());
        }

        String text = builder.toString().strip();
        return text.isBlank() ? "Compilation failed with no diagnostics." : text;
    }

    private static String stackTrace(Throwable throwable) {
        return throwable == null
                ? "Unknown error"
                : throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
    }

    public record CompilationResult(
            boolean success,
            Path outputDir,
            long compileMillis,
            int sourceCount,
            String diagnostics) {

        public static CompilationResult success(Path outputDir, long compileMillis, int sourceCount) {
            return new CompilationResult(true, outputDir, compileMillis, sourceCount, "");
        }

        public static CompilationResult failure(String diagnostics, long compileMillis, int sourceCount) {
            return new CompilationResult(false, null, compileMillis, sourceCount, diagnostics);
        }
    }
}
