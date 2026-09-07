package com.demcha.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.demcha.compose.document.api.Internal;

/**
 * Module-local twin of the engine's {@code InternalAnnotationCoverageTest}.
 *
 * <p>{@code docs/api-stability.md} § 4 puts the whole {@code com.demcha.compose.engine}
 * tree in the Internal tier and calls out that {@code engine.render.pdf.*} ships from
 * this module rather than from {@code graph-compose-core}. The engine's own coverage
 * test can only reach classes on the engine's classpath, so nothing there can see the
 * packages this module publishes; a package added here without the marker stays
 * invisible to it. This test closes that half of the boundary.</p>
 *
 * <p>Two assertions, because the marker has two jobs. The package-level one is what
 * policy and the japicmp excludes hang off. The type-level one is what a reader sees:
 * Javadoc renders a package annotation on the package-summary page and nowhere else, so
 * a class in a marked package still publishes an unqualified public class page unless it
 * declares {@code @Internal} itself.</p>
 *
 * <p>Both lists are derived from the source tree rather than hard-coded, so a
 * <em>new</em> {@code com.demcha.compose.engine.*} package or public type fails the
 * build until it carries the marker.</p>
 */
class InternalEnginePackageMarkerTest {

    /** The package prefix {@code docs/api-stability.md} maps to the Internal tier. */
    private static final String ENGINE_ROOT = "com.demcha.compose.engine";

    /** Surefire runs with the module directory as the working directory. */
    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    @Test
    void everyEnginePackageInThisModuleCarriesTheInternalMarker() throws IOException {
        List<String> mainPackages = packagesUnderMainSources();

        // Without this, a scan that silently found nothing would report zero
        // unmarked packages below and pass while inspecting nothing at all.
        assertThat(mainPackages)
                .describedAs("Found no packages under %s — the engine assertion below would"
                        + " then pass without inspecting anything", SOURCE_ROOT)
                .isNotEmpty();

        List<String> unmarked = mainPackages.stream()
                .filter(InternalEnginePackageMarkerTest::isEnginePackage)
                .filter(pkg -> !isMarkedInternal(pkg))
                .toList();

        assertThat(unmarked)
                .describedAs("docs/api-stability.md puts %s.* in the Internal tier — removable"
                        + " in any release, with no deprecation window. These packages ship"
                        + " from this module without the package-level @Internal marker, so"
                        + " their published Javadoc reads as a supported surface", ENGINE_ROOT)
                .isEmpty();
    }

    /** Every package under {@code src/main/java} that holds at least one source file. */
    private static List<String> packagesUnderMainSources() throws IOException {
        try (Stream<Path> tree = Files.walk(SOURCE_ROOT)) {
            return tree.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".java"))
                    .map(InternalEnginePackageMarkerTest::packageOf)
                    .distinct()
                    .sorted()
                    .toList();
        }
    }

    @Test
    void everyPublicTypeInThoseEnginePackagesCarriesItToo() throws IOException {
        List<String> enginePackages = packagesUnderMainSources().stream()
                .filter(InternalEnginePackageMarkerTest::isEnginePackage)
                .toList();
        List<String> publicTypes = publicTopLevelTypesInEnginePackages();

        // A silently-empty type scan would satisfy the assertion below without reading
        // a single class; an engine package with no public type in it cannot happen.
        assertThat(enginePackages.isEmpty() || !publicTypes.isEmpty())
                .describedAs("Found engine packages %s but no public type in any of them —"
                        + " the type scan is not reading this module", enginePackages)
                .isTrue();

        List<String> unmarked = new ArrayList<>();
        for (String type : publicTypes) {
            if (loadClass(type).getAnnotation(Internal.class) == null) {
                unmarked.add(type);
            }
        }

        assertThat(unmarked)
                .describedAs("These public types sit in an Internal-tier package but declare"
                        + " no @Internal of their own, so their published Javadoc page carries"
                        + " no marker at all")
                .isEmpty();
    }

    /** Top-level public types declared in this module's engine packages. */
    private static List<String> publicTopLevelTypesInEnginePackages() throws IOException {
        List<String> types = new ArrayList<>();
        try (Stream<Path> tree = Files.walk(SOURCE_ROOT)) {
            for (Path file : tree.filter(Files::isRegularFile).toList()) {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith(".java") || fileName.equals("package-info.java")) {
                    continue;
                }
                String pkg = packageOf(file);
                if (!isEnginePackage(pkg)) {
                    continue;
                }
                String name = pkg + "." + fileName.substring(0, fileName.length() - ".java".length());
                if (Modifier.isPublic(loadClass(name).getModifiers())) {
                    types.add(name);
                }
            }
        }
        return types;
    }

    private static Class<?> loadClass(String binaryName) {
        try {
            return Class.forName(binaryName, false,
                    InternalEnginePackageMarkerTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(
                    "Source file for " + binaryName + " exists but nothing compiled to it", e);
        }
    }

    /** The package a source file under {@link #SOURCE_ROOT} declares, by its location. */
    private static String packageOf(Path sourceFile) {
        Path directory = SOURCE_ROOT.relativize(sourceFile.getParent());
        return directory.toString().replace(directory.getFileSystem().getSeparator(), ".");
    }

    private static boolean isEnginePackage(String packageName) {
        return packageName.equals(ENGINE_ROOT) || packageName.startsWith(ENGINE_ROOT + ".");
    }

    /**
     * Reads the package-level marker off the compiled {@code package-info} class, which
     * is where javac stores package annotations. Anything the reflection cannot confirm
     * counts as unmarked, so the guard fails closed.
     */
    private static boolean isMarkedInternal(String packageName) {
        try {
            Class<?> packageInfo = Class.forName(
                    packageName + ".package-info",
                    false,
                    InternalEnginePackageMarkerTest.class.getClassLoader());
            return packageInfo.getAnnotation(Internal.class) != null;
        } catch (ClassNotFoundException noCompiledPackageInfo) {
            // javac emits package-info.class only for an annotated package
            // declaration, so a package carrying prose alone lands here — exactly
            // the gap this guard exists to catch.
            return false;
        }
    }
}
