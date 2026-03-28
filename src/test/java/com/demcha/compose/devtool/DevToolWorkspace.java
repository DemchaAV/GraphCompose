package com.demcha.compose.devtool;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Describes the current workspace used by the dev tool.
 */
public record DevToolWorkspace(
        Path projectRoot,
        List<Path> javaRoots,
        List<Path> resourceRoots,
        Path compiledOutputRoot,
        String previewProviderClassName) {

    public static final String PREVIEW_CLASS_PROPERTY = "graphcompose.preview.class";
    public static final String DEFAULT_PREVIEW_PROVIDER = "com.demcha.preview.LivePreviewProvider";

    public DevToolWorkspace {
        projectRoot = normalize(projectRoot);
        javaRoots = List.copyOf(javaRoots.stream().map(DevToolWorkspace::normalize).toList());
        resourceRoots = List.copyOf(resourceRoots.stream().map(DevToolWorkspace::normalize).toList());
        compiledOutputRoot = normalize(compiledOutputRoot);
        previewProviderClassName = Objects.requireNonNull(previewProviderClassName, "previewProviderClassName");
    }

    public static DevToolWorkspace currentProject() {
        var root = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        var previewClass = System.getProperty(PREVIEW_CLASS_PROPERTY, DEFAULT_PREVIEW_PROVIDER);

        return new DevToolWorkspace(
                root,
                List.of(root.resolve("src/main/java"), root.resolve("src/test/java")),
                List.of(root.resolve("src/main/resources"), root.resolve("src/test/resources")),
                root.resolve("target/devtool-classes"),
                previewClass);
    }

    public List<Path> watchRoots() {
        return Stream.concat(javaRoots.stream(), resourceRoots.stream()).toList();
    }

    public List<Path> existingWatchRoots() {
        return watchRoots().stream().filter(Files::isDirectory).toList();
    }

    public List<Path> existingJavaRoots() {
        return javaRoots.stream().filter(Files::isDirectory).toList();
    }

    public List<Path> existingResourceRoots() {
        return resourceRoots.stream().filter(Files::isDirectory).toList();
    }

    public List<Path> collectJavaSources() throws IOException {
        var sources = new ArrayList<Path>();
        for (Path root : existingJavaRoots()) {
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .sorted()
                        .forEach(sources::add);
            }
        }
        return List.copyOf(sources);
    }

    public boolean isJavaSource(Path path) {
        return path != null
                && path.getFileName() != null
                && path.getFileName().toString().endsWith(".java")
                && javaRoots.stream().anyMatch(root -> isUnderRoot(root, path));
    }

    public boolean isResourcePath(Path path) {
        return path != null && resourceRoots.stream().anyMatch(root -> isUnderRoot(root, path));
    }

    public URL[] buildClassLoaderUrls(Path compiledOutputDir) throws MalformedURLException {
        var urls = new ArrayList<URL>();
        urls.add(normalize(compiledOutputDir).toUri().toURL());
        for (Path resourceRoot : existingResourceRoots()) {
            urls.add(resourceRoot.toUri().toURL());
        }
        return urls.toArray(URL[]::new);
    }

    public String displayPath(Path path) {
        if (path == null) {
            return "";
        }

        Path normalized = normalize(path);
        if (normalized.startsWith(projectRoot)) {
            return projectRoot.relativize(normalized).toString().replace('\\', '/');
        }
        return normalized.toString();
    }

    private static boolean isUnderRoot(Path root, Path path) {
        Path normalizedRoot = normalize(root);
        Path normalizedPath = normalize(path);
        return normalizedPath.startsWith(normalizedRoot);
    }

    private static Path normalize(Path path) {
        return Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }
}
