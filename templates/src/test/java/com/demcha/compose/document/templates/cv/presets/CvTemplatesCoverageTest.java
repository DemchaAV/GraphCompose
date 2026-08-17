package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The catalogue lists every preset in the package.
 *
 * <p>A registry is only useful while it is complete, and the way it stops
 * being complete is that someone ships a preset and forgets the one line.
 * Nothing about that fails: the preset works, its tests pass, its example
 * renders — it is merely invisible to every caller that picks a template by
 * id, which is the whole audience the catalogue exists for. So the list is
 * derived from the package rather than trusted, by reading the directory
 * the presets live in.</p>
 *
 * <p>Reading source files rather than scanning the classpath is deliberate:
 * it needs no reflection dependency, and the failure message can name the
 * file to add.</p>
 */
class CvTemplatesCoverageTest {

    /** The presets package, relative to this module's directory. */
    private static final Path PRESETS = Path.of(
            "src/main/java/com/demcha/compose/document/templates/cv/presets");

    @Test
    void everyPresetInThePackageIsInTheCatalogue() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String preset : presetClassNames()) {
            if (CvTemplates.all().stream().noneMatch(t -> declaredBy(t, preset))) {
                missing.add(preset);
            }
        }

        assertThat(missing)
                .as("every preset in %s must be registered in CvTemplates — a preset "
                        + "missing from the catalogue is invisible to every caller that "
                        + "picks a template by id", PRESETS)
                .isEmpty();
    }

    @Test
    void theCatalogueListsNothingTwice() {
        assertThat(CvTemplates.ids()).doesNotHaveDuplicates();
        assertThat(CvTemplates.all()).hasSameSizeAs(CvTemplates.ids());
    }

    @Test
    void everyRegisteredIdResolvesToTheTemplateThatPublishesIt() {
        for (String id : CvTemplates.ids()) {
            assertThat(CvTemplates.byId(id))
                    .as("byId(%s) must resolve", id)
                    .isPresent()
                    .get()
                    .extracting(DocumentTemplate::id)
                    .as("byId(%s) must return the template publishing that id", id)
                    .isEqualTo(id);
            assertThat(CvTemplates.recommendedMargin(id))
                    .as("recommendedMargin(%s) must be known", id)
                    .isPresent();
        }
    }

    @Test
    void anUnknownOrNullIdIsAnEmptyResultNotAnException() {
        // The id arrives from a config file or a request; a caller validating
        // input should not have to catch anything.
        assertThat(CvTemplates.byId("no-such-preset")).isEmpty();
        assertThat(CvTemplates.byId(null)).isEmpty();
        assertThat(CvTemplates.byId("")).isEmpty();
        assertThat(CvTemplates.recommendedMargin("no-such-preset")).isEmpty();
    }

    @Test
    void surroundingWhitespaceInAnIdIsIgnored() {
        assertThat(CvTemplates.byId("  modern-professional  "))
                .get()
                .extracting(DocumentTemplate::id)
                .isEqualTo(ModernProfessional.ID);
    }

    @Test
    void everyTemplateBuildsAFreshInstance() {
        // all() hands each caller its own template rather than a shared one,
        // so a caller cannot be surprised by another's theme.
        List<DocumentTemplate<CvDocument>> first = CvTemplates.all();
        List<DocumentTemplate<CvDocument>> second = CvTemplates.all();

        for (int i = 0; i < first.size(); i++) {
            assertThat(first.get(i)).isNotSameAs(second.get(i));
            assertThat(first.get(i).id()).isEqualTo(second.get(i).id());
        }
    }

    /**
     * The class names in the presets package that are actually presets:
     * public types with a factory. {@code package-info} carries no class, and
     * {@code ColumnPagination} is a package-private helper rather than a
     * template, so neither belongs in a catalogue of templates.
     */
    private static List<String> presetClassNames() throws IOException {
        assertThat(PRESETS).as("presets package must exist at %s", PRESETS).exists();
        try (Stream<Path> files = Files.list(PRESETS)) {
            List<String> names = new ArrayList<>();
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".java") || name.equals("package-info.java")) {
                    continue;
                }
                String simpleName = name.substring(0, name.length() - ".java".length());
                String source = Files.readString(file);
                if (source.contains("public final class " + simpleName)
                        && source.contains("public static final String ID")) {
                    names.add(simpleName);
                }
            }
            assertThat(names)
                    .as("the scan must find the presets it is meant to guard")
                    .hasSizeGreaterThan(10);
            return names;
        }
    }

    /**
     * Whether {@code template} is the one {@code presetClass} publishes,
     * decided by the id constant that class declares.
     */
    private static boolean declaredBy(DocumentTemplate<CvDocument> template, String presetClass) {
        try {
            Class<?> type = Class.forName(
                    "com.demcha.compose.document.templates.cv.presets." + presetClass);
            Object id = type.getField("ID").get(null);
            return template.id().equals(id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("preset " + presetClass + " must publish a public ID", e);
        }
    }
}
