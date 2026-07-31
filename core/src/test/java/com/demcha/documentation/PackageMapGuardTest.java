package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PackageMapGuardTest {
    private static final Path PROJECT_ROOT = RepoRoot.get();
    private static final List<String> FORBIDDEN_OLD_PACKAGE_TOKENS = List.of(
            "com.demcha.compose." + "layout_" + "core",
            "com.demcha.compose.document.model." + "node",
            "com.demcha.compose." + "font_" + "library",
            "com.demcha.compose." + "markdown",
            "layout_" + "core",
            "font_" + "library",
            "pdf_" + "systems",
            "implemented_" + "systems",
            "components_" + "builders",
            "abstract_" + "builders");

    /** Modules that may hold a render or export backend. */
    private static final List<String> BACKEND_MODULE_ROOTS = List.of(
            "core/src/main/java", "render-pdf/src/main/java",
            "render-pptx/src/main/java", "render-docx/src/main/java");

    /** Documents a contributor consults to find where a backend lives. */
    private static final List<String> PACKAGE_ROOT_DOCUMENTS = List.of(
            "CONTRIBUTING.md", "docs/architecture/package-map.md");

    /**
     * Every package holding a backend implementation is findable from the documents
     * that claim to map the packages.
     *
     * <p>Derived from the source tree rather than a list, so it cannot go stale: a
     * package qualifies by containing a {@code *Backend} type, which is what a
     * contributor is looking for when they ask where a format is implemented. The PPTX
     * fixed-layout backend shipped a whole release with neither document naming it.</p>
     *
     * <p>Each package must be named in its own right. Accepting an ancestor instead
     * looks reasonable and guts the guard: once {@code backend.fixed} appears, every
     * {@code backend.fixed.*} is covered for free, including the one that shipped
     * undocumented. Matching is boundary-aware and accepts either the fully-qualified
     * name or the {@code document.backend.…} tail the docs also use, so
     * {@code backend.fixed.pdf} never counts as {@code backend.fixed}.</p>
     */
    @Test
    void everyBackendPackageIsFindableFromThePackageDocumentation() throws IOException {
        Set<String> backendPackages = backendPackages();

        assertThat(backendPackages)
                .describedAs("no package with a *Backend type was found under document/backend — "
                        + "the scan roots moved and this guard is passing vacuously")
                .isNotEmpty();

        Set<String> missing = new TreeSet<>();
        for (String document : PACKAGE_ROOT_DOCUMENTS) {
            String text = Files.readString(PROJECT_ROOT.resolve(document));
            for (String backendPackage : backendPackages) {
                if (!namesPackage(text, backendPackage)
                        && !namesPackage(text, shortForm(backendPackage))) {
                    missing.add(document + " does not name " + backendPackage);
                }
            }
        }

        assertThat(missing)
                .describedAs("a backend a contributor cannot find in the package map is a backend "
                        + "they will not register a handler with — and a fragment kind registered "
                        + "with only one fixed-layout backend renders in one output and silently "
                        + "vanishes from the other")
                .isEmpty();
    }

    /** Packages under {@code document/backend} that declare a {@code *Backend} type. */
    private static Set<String> backendPackages() throws IOException {
        Set<String> packages = new TreeSet<>();
        for (String moduleRoot : BACKEND_MODULE_ROOTS) {
            Path root = PROJECT_ROOT.resolve(moduleRoot);
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith("Backend.java"))
                        .map(path -> relativeTo(root, path))
                        .filter(name -> name.contains("com/demcha/compose/document/backend/"))
                        .map(name -> name.substring(0, name.lastIndexOf('/')).replace('/', '.'))
                        .forEach(packages::add);
            }
        }
        return packages;
    }

    /** The {@code document.backend.…} tail, which the docs use as often as the full name. */
    private static String shortForm(String packageName) {
        return packageName.replace("com.demcha.compose.", "");
    }

    /**
     * Whether the text names exactly this package. A trailing identifier character or a
     * dot followed by one means the match is really a longer package, so naming
     * {@code backend.fixed.pdf} must not count as naming {@code backend.fixed}.
     */
    private static boolean namesPackage(String documentText, String packageName) {
        int from = 0;
        while (true) {
            int at = documentText.indexOf(packageName, from);
            if (at < 0) {
                return false;
            }
            int after = at + packageName.length();
            char next = after < documentText.length() ? documentText.charAt(after) : ' ';
            boolean extendsFurther = Character.isJavaIdentifierPart(next)
                    || (next == '.' && after + 1 < documentText.length()
                        && Character.isJavaIdentifierPart(documentText.charAt(after + 1)));
            if (!extendsFurther) {
                return true;
            }
            from = at + 1;
        }
    }

    private static String relativeTo(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    @Test
    void productionPackagesShouldHavePackageInfo() throws IOException {
        Path sourceRoot = PROJECT_ROOT.resolve("core/src/main/java/com/demcha/compose");

        try (var paths = Files.walk(sourceRoot)) {
            Set<String> missing = new TreeSet<>(paths
                    .filter(Files::isDirectory)
                    .filter(this::containsJavaSource)
                    .filter(path -> Files.notExists(path.resolve("package-info.java")))
                    .map(this::relative)
                    .collect(Collectors.toSet()));

            assertThat(missing)
                    .describedAs("Every production package should document ownership and extension rules")
                    .isEmpty();
        }
    }

    @Test
    void oldPackageNamesShouldNotReturnToSourcesDocsOrExamples() throws IOException {
        List<Path> roots = List.of(
                PROJECT_ROOT.resolve("core/src/main/java"),
                PROJECT_ROOT.resolve("core/src/test/java"),
                PROJECT_ROOT.resolve("examples/src/main/java"),
                PROJECT_ROOT.resolve("README.md"),
                PROJECT_ROOT.resolve("CONTRIBUTING.md"),
                PROJECT_ROOT.resolve("docs"));

        Set<String> violations = new TreeSet<>();
        for (Path root : roots) {
            collectForbiddenReferences(root, violations);
        }

        assertThat(violations)
                .describedAs("Old package names should not appear in source, public docs, or examples")
                .isEmpty();
    }

    private void collectForbiddenReferences(Path root, Set<String> violations) throws IOException {
        if (Files.isRegularFile(root)) {
            if (isScannedTextFile(root) && containsForbiddenToken(root)) {
                violations.add(relative(root));
            }
            return;
        }

        try (var paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isScannedTextFile)
                    .filter(path -> !isIgnored(path))
                    .filter(this::containsForbiddenToken)
                    .map(this::relative)
                    .forEach(violations::add);
        }
    }

    private boolean containsJavaSource(Path directory) {
        try (var children = Files.list(directory)) {
            return children.anyMatch(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to inspect " + directory, ex);
        }
    }

    private boolean isScannedTextFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".java")
                || fileName.endsWith(".md")
                || fileName.endsWith(".xml")
                || fileName.endsWith(".properties")
                || fileName.endsWith(".yml")
                || fileName.endsWith(".yaml");
    }

    private boolean containsForbiddenToken(Path path) {
        try {
            String source = Files.readString(path);
            return FORBIDDEN_OLD_PACKAGE_TOKENS.stream().anyMatch(source::contains);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to inspect " + path, ex);
        }
    }

    private boolean isIgnored(Path path) {
        String relative = relative(path);
        return relative.startsWith(".git/")
                || relative.startsWith(".claude/")
                || relative.startsWith(".idea/")
                // docs/private/ is gitignored — local-only planning notes and
                // audits, not part of the public docs surface. They routinely
                // name a retired package in order to record that it is retired,
                // which reads as a violation here. Because the directory never
                // reaches the remote, a hit fails the release runbook's local
                // verify gate for a reason CI can never reproduce or clear.
                // CanonicalSurfaceGuardTest excludes it for the same reason.
                || relative.startsWith("docs/private/")
                || relative.contains("/target/")
                || relative.startsWith("target/")
                || relative.startsWith("logs/");
    }

    private String relative(Path path) {
        return PROJECT_ROOT.relativize(path).toString().replace('\\', '/');
    }
}
