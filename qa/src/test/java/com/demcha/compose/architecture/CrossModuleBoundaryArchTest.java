package com.demcha.compose.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-module architecture guards, enforced at bytecode level. The engine
 * ({@code graph-compose-core}), the PDF backend ({@code graph-compose-render-pdf}),
 * and the built-in templates ({@code graph-compose-templates}) only meet on one
 * classpath here in the qa module, so the boundaries <em>between</em> them can
 * only be asserted from here.
 *
 * <p>These complement the intra-core {@code ModuleBoundaryArchTest} in the engine
 * module. In particular the templates &rarr; engine boundary went unguarded when
 * the templates left the engine jar: {@code PublicApiNoEngineLeakTest} and
 * {@code PdfBackendIsolationGuardTest} scan the engine's own sources only.
 */
class CrossModuleBoundaryArchTest {

    // Every module's production classes on the qa classpath (engine + render-pdf +
    // templates + testing). DO_NOT_INCLUDE_TESTS drops qa's own test classes, which
    // legitimately depend on all of them.
    private static final JavaClasses MODULE_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.demcha.compose");

    private static final String TEMPLATES = "com.demcha.compose.document.templates..";
    private static final String[] PDF_BACKEND = {
            "com.demcha.compose.document.backend.fixed.pdf..",
            "com.demcha.compose.engine.render.pdf.."
    };

    @Test
    void importedTheTemplatesAndEngineModuleClasses() {
        assertThat(MODULE_CLASSES.contain("com.demcha.compose.document.api.DocumentSession"))
                .as("the engine classes must be on the qa classpath")
                .isTrue();
        assertThat(MODULE_CLASSES.stream()
                .anyMatch(c -> c.getPackageName().startsWith("com.demcha.compose.document.templates")))
                .as("the templates classes must be on the qa classpath so the boundary rules have subjects")
                .isTrue();
    }

    @Test
    void templatesDoNotDependOnTheEngine() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(TEMPLATES)
                .should().dependOnClassesThat().resideInAPackage("com.demcha.compose.engine..")
                .because("the built-in templates compose over the canonical DSL only — they must "
                        + "reach the engine through the public document.* API, never by referencing "
                        + "engine.* internals directly");

        rule.check(MODULE_CLASSES);
    }

    @Test
    void templatesStayBackendNeutral() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(TEMPLATES)
                .should().dependOnClassesThat().resideInAPackage("org.apache.pdfbox..")
                .because("templates are pure authoring code over the canonical DSL; PDFBox lives "
                        + "behind the graph-compose-render-pdf backend seam, not in a preset");

        rule.check(MODULE_CLASSES);
    }

    @Test
    void thePdfBackendDoesNotDependOnTemplates() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(PDF_BACKEND)
                .should().dependOnClassesThat().resideInAPackage(TEMPLATES)
                .because("the render backend is template-agnostic — it renders the resolved layout "
                        + "graph and knows nothing about the built-in presets");

        rule.check(MODULE_CLASSES);
    }
}
