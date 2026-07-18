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
 * the PPTX backend ({@code graph-compose-render-pptx}), and the built-in
 * templates ({@code graph-compose-templates}) only meet on one classpath here
 * in the qa module, so the boundaries <em>between</em> them can only be
 * asserted from here.
 *
 * <p>These complement the intra-core {@code ModuleBoundaryArchTest} in the engine
 * module. In particular the templates &rarr; engine boundary went unguarded when
 * the templates left the engine jar: {@code PublicApiNoEngineLeakTest} and
 * {@code PdfBackendIsolationGuardTest} scan the engine's own sources only.
 */
class CrossModuleBoundaryArchTest {

    // Every module's production classes on the qa classpath (engine + render-pdf +
    // render-pptx + templates + testing). DO_NOT_INCLUDE_TESTS drops qa's own test
    // classes, which legitimately depend on all of them.
    private static final JavaClasses MODULE_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.demcha.compose");

    private static final String TEMPLATES = "com.demcha.compose.document.templates..";
    private static final String[] PDF_BACKEND = {
            "com.demcha.compose.document.backend.fixed.pdf..",
            "com.demcha.compose.engine.render.pdf.."
    };
    private static final String[] PPTX_BACKEND = {
            "com.demcha.compose.document.backend.fixed.pptx..",
            "com.demcha.compose.document.backend.semantic.pptx.."
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
        assertThat(MODULE_CLASSES.contain("com.demcha.compose.document.backend.fixed.pptx.PptxFixedLayoutBackend"))
                .as("the PPTX backend classes must be on the qa classpath so the POI rules have subjects")
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
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.apache.pdfbox..", "org.apache.poi..")
                .because("templates are pure authoring code over the canonical DSL; PDFBox and POI "
                        + "live behind the render-backend seams, not in a preset");

        rule.check(MODULE_CLASSES);
    }

    @Test
    void theRenderBackendsDoNotDependOnTemplates() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(concat(PDF_BACKEND, PPTX_BACKEND))
                .should().dependOnClassesThat().resideInAPackage(TEMPLATES)
                .because("the render backends are template-agnostic — they render the resolved "
                        + "layout graph and know nothing about the built-in presets");

        rule.check(MODULE_CLASSES);
    }

    @Test
    void onlyRenderBackendsDependOnPoi() {
        // The DOCX semantic backend also legitimately uses POI; it is not on
        // the qa classpath today, but the exclusion keeps this rule correct
        // the day render-docx joins for its own boundary guards.
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackages(concat(PPTX_BACKEND,
                        new String[]{"com.demcha.compose.document.backend.semantic.docx.."}))
                .should().dependOnClassesThat().resideInAPackage("org.apache.poi..")
                .because("POI is a render-backend implementation detail — the engine, the PDF "
                        + "backend, and the templates must stay free of it so the core artifact "
                        + "never drags the POI dependency in");

        rule.check(MODULE_CLASSES);
    }

    @Test
    void thePdfBackendDoesNotDependOnThePptxBackend() {
        // The reverse dependency is by design: the PPTX backend reuses the PDF
        // measurement stack and raster sub-renders for geometry identity.
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(PDF_BACKEND)
                .should().dependOnClassesThat().resideInAnyPackage(PPTX_BACKEND)
                .because("the PDF backend is the reference implementation and must remain "
                        + "deployable without the PPTX artifact");

        rule.check(MODULE_CLASSES);
    }

    private static String[] concat(String[] first, String[] second) {
        String[] merged = new String[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }
}
