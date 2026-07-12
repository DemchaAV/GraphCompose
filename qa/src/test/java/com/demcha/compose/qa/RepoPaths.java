package com.demcha.compose.qa;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves repository-root-relative paths independently of which module's working
 * directory the suite runs from. Several cross-module suites moved into qa read
 * sibling-module sources or repo-root assets (e.g. {@code fonts/src/...},
 * {@code examples/src/...}, {@code assets/...}); those paths resolved from the
 * engine module's basedir (the repo root) but not from qa's, so they go through
 * here instead.
 */
public final class RepoPaths {

    private RepoPaths() {
    }

    /**
     * The repository root, located by walking up from the working directory until a
     * directory that contains the Maven wrapper ({@code mvnw}) is found — a marker
     * unique to the repository root and stable across the module layout.
     *
     * @return the absolute repository-root path
     */
    public static Path repoRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.exists(dir.resolve("mvnw"))) {
                return dir;
            }
        }
        return Path.of("").toAbsolutePath();
    }

    /**
     * Resolves a repository-root-relative path.
     *
     * @param first the first path segment (may itself contain separators)
     * @param more  additional path segments
     * @return the resolved absolute path under the repository root
     */
    public static Path resolve(String first, String... more) {
        return repoRoot().resolve(Path.of(first, more));
    }
}
