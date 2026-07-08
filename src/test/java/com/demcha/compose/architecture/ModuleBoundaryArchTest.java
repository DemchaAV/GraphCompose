package com.demcha.compose.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structural architecture guards enforced at bytecode level, so a
 * fully-qualified reference or a moved source file cannot slip past the
 * text-scanning guards in {@code com.demcha.documentation}.
 *
 * <p>Complements {@code PublicApiNoEngineLeakTest} and
 * {@code PdfBackendIsolationGuardTest}: those read source files and only match
 * {@code import} lines, so a fully-qualified reference is invisible to them;
 * these assert the same canonical / engine / font boundaries against the
 * compiled classes and are location-independent.
 */
class ModuleBoundaryArchTest {

    // The core module's own classes (document + engine + font + the GraphCompose
    // facade). DO_NOT_INCLUDE_TESTS drops the test classes — some reside in the
    // guarded packages and legitimately reach across these boundaries.
    private static final JavaClasses CORE_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.demcha.compose");

    /**
     * The canonical public surface: the {@code GraphCompose} facade, the session
     * API, the DSL builders, the node / style value objects, and the chart / svg
     * / table / image / output / snapshot helpers. These describe what a document
     * says; the engine reads them, never the reverse.
     *
     * <p>{@code document.layout} (the layout compiler) is the sanctioned engine
     * bridge and is intentionally absent. {@code document.templates} lives in the
     * separate graph-compose-templates module and is not on this module's
     * classpath — its boundary is a candidate for a cross-module ArchUnit rule in
     * the qa suite.
     */
    private static final String[] CANONICAL_SURFACE_PACKAGES = {
            "com.demcha.compose",                          // GraphCompose facade (exact package)
            "com.demcha.compose.document.api..",
            "com.demcha.compose.document.dsl..",
            "com.demcha.compose.document.node..",
            "com.demcha.compose.document.style..",
            "com.demcha.compose.document.chart..",
            "com.demcha.compose.document.svg..",
            "com.demcha.compose.document.table..",
            "com.demcha.compose.document.image..",
            "com.demcha.compose.document.output..",
            "com.demcha.compose.document.snapshot..",
            "com.demcha.compose.document.exceptions..",
            "com.demcha.compose.document.emoji..",
            "com.demcha.compose.font.."
    };

    /**
     * The two session-orchestration classes that still convert canonical insets
     * to engine value objects by fully-qualified reference
     * ({@code DocumentSession.toEngineMargin} → {@code engine.components.style.Margin};
     * {@code DocumentPageBackgrounds} → {@code Margin.zero()} / {@code Padding.zero()}).
     * They are excluded by name — not by whole-package — so the other public
     * {@code document.api} types stay guarded. Routing these conversions behind a
     * public adapter (so the exclusion can be dropped) is tracked as a follow-up.
     */
    private static final String KNOWN_ENGINE_BRIDGE_CLASSES =
            "com\\.demcha\\.compose\\.document\\.api\\.(DocumentSession|DocumentPageBackgrounds)(\\$.+)?";

    @Test
    void importedTheCoreProductionClasses() {
        assertThat(CORE_CLASSES.contain("com.demcha.compose.document.api.DocumentSession"))
                .as("ArchUnit must analyze the compiled core classes; an empty import would let "
                        + "every rule pass vacuously")
                .isTrue();
        assertThat(CORE_CLASSES.contain("com.demcha.compose.document.node.ParagraphNode"))
                .as("a guarded-package class must be present so the boundary rules have subjects")
                .isTrue();
    }

    @Test
    void canonicalSurfaceDoesNotDependOnTheEngine() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(CANONICAL_SURFACE_PACKAGES)
                .and().haveNameNotMatching(KNOWN_ENGINE_BRIDGE_CLASSES)
                .should().dependOnClassesThat().resideInAPackage("com.demcha.compose.engine..")
                .because("the canonical public surface must reach the engine only through the "
                        + "document.layout bridge and the backend ServiceLoader seam, never by "
                        + "referencing engine.* types directly");

        rule.check(CORE_CLASSES);
    }

    @Test
    void fontCatalogDoesNotDependOnTheDocumentLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.demcha.compose.font..")
                .should().dependOnClassesThat().resideInAPackage("com.demcha.compose.document..")
                .because("com.demcha.compose.font is the logical font catalog — it names fonts "
                        + "and never loads them; a render backend materializes them behind the "
                        + "FontMetricsProvider seam");

        rule.check(CORE_CLASSES);
    }
}
