package com.demcha.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts every {@code public} type in the canonical authoring entry-point
 * packages carries a class-level {@code @since} Javadoc tag. The guard is
 * intentionally narrow — it covers only the surfaces a user first reaches
 * for ({@link com.demcha.compose.GraphCompose} factory, the
 * {@code document.api} session / builder seam, and the {@code document.dsl}
 * authoring builders) — not the whole public surface. A broader sweep
 * across {@code document.node}, {@code document.style}, and the template
 * packages is tracked separately.
 *
 * <p>The check is source-level rather than reflective because
 * {@code @since} is a Javadoc tag, not a runtime annotation. The guard
 * scans each {@code *.java} file and verifies that an {@code @since}
 * token appears somewhere before the first {@code public class},
 * {@code public final class}, {@code public sealed class}, {@code public
 * abstract class}, {@code public interface}, {@code public sealed
 * interface}, {@code public record}, {@code public final record}, or
 * {@code public enum} declaration.</p>
 *
 * <p>Files with no public top-level type ({@code package-info.java} and
 * the like) are skipped.</p>
 */
class PublicApiSinceTagCoverageTest {

    private static final Path PROJECT_ROOT = Paths.get(".").toAbsolutePath().normalize();

    /**
     * The narrow set of "entry-point" sources this guard scans. New
     * packages should be added here only when they qualify as primary
     * user-reached surface; otherwise leave them for the broader sweep
     * tracked under the v1.6.6 H-track.
     */
    private static final List<Path> ROOTS = List.of(
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/GraphCompose.java"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/api"),
            PROJECT_ROOT.resolve("src/main/java/com/demcha/compose/document/dsl")
    );

    /**
     * Files explicitly excused from the {@code @since} requirement.
     * Add an entry here only when the file truly does not declare a
     * public top-level type that callers can reach — e.g. an internal
     * helper that ended up in an entry-point package by accident and
     * is on its way out.
     */
    private static final Set<String> ALLOWLIST = Set.of();

    private static final Pattern FIRST_PUBLIC_TYPE = Pattern.compile(
            "(?m)^public\\s+(?:final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+)*"
                    + "(?:class|interface|record|enum|@interface)\\b");

    @Test
    void publicEntryPointFilesCarryClassLevelSinceTag() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Path root : ROOTS) {
            scan(root, missing);
        }
        assertThat(missing)
                .as("public entry-point files missing class-level @since tag")
                .isEmpty();
    }

    private void scan(Path root, List<String> missing) throws IOException {
        if (!Files.exists(root)) {
            // Source layout change — let the test fail loudly so the
            // maintainer notices the dropped root rather than silently
            // skipping coverage.
            throw new IllegalStateException("Guard root does not exist: " + root);
        }
        if (Files.isRegularFile(root)) {
            checkFile(root, missing);
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals("package-info.java"))
                    // Files inside a `.internal` subpackage are internal by
                    // package-naming convention; treat the same way as the
                    // package-level @Internal annotation in `document.layout`.
                    // Coverage on those packages is enforced separately by
                    // InternalAnnotationCoverageTest.
                    .filter(p -> !p.toString().replace('\\', '/').contains("/internal/"))
                    .forEach(p -> checkFile(p, missing));
        }
    }

    private void checkFile(Path file, List<String> missing) {
        String relative = PROJECT_ROOT.relativize(file).toString().replace('\\', '/');
        if (ALLOWLIST.contains(relative)) {
            return;
        }
        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + file, e);
        }
        Matcher m = FIRST_PUBLIC_TYPE.matcher(content);
        if (!m.find()) {
            // Source file has no public top-level type — skip silently.
            return;
        }
        String beforeFirstPublicType = content.substring(0, m.start());
        if (!beforeFirstPublicType.contains("@since")) {
            missing.add(relative);
        }
    }
}
