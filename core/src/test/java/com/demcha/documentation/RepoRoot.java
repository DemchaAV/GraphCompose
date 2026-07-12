package com.demcha.documentation;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves the repository root for the documentation / architecture guards, which
 * read repo-root and sibling-module files (README, CONTRIBUTING, docs, the other
 * modules' poms). The engine module lives in {@code core/}, so its basedir is no
 * longer the repository root; these guards walk up to the root instead of assuming
 * the working directory is it.
 *
 * <p>The root is located by the Maven wrapper ({@code mvnw}), a marker unique to
 * the repository root and stable across the module layout. Core-module-local files
 * are resolved under {@code core/} from the returned root.</p>
 */
final class RepoRoot {

    private RepoRoot() {
    }

    /** The absolute repository-root path (the directory that holds {@code mvnw}). */
    static Path get() {
        for (Path dir = Path.of("").toAbsolutePath().normalize(); dir != null; dir = dir.getParent()) {
            if (Files.exists(dir.resolve("mvnw"))) {
                return dir;
            }
        }
        return Path.of("").toAbsolutePath().normalize();
    }
}
