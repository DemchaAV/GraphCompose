package com.demcha.compose.document.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Pins the contract of the {@link Beta} annotation in the same shape
 * {@link InternalAnnotationDocumentationTest} pins {@link Internal}.
 * If the retention, target set, or Javadoc policy drifts, the build
 * fails here rather than during a future architecture refactor.
 */
class BetaAnnotationDocumentationTest {

    @Test
    void annotationHasRuntimeRetentionForArchitectureGuards() {
        Retention retention = Beta.class.getAnnotation(Retention.class);
        assertThat(retention)
                .describedAs("@Beta must declare @Retention so guard tests can read it reflectively")
                .isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void annotationIsDocumentedSoJavadocSurfacesIt() {
        assertThat(Beta.class.getAnnotation(Documented.class))
                .describedAs("@Beta must be @Documented so Javadoc surfaces the marker on annotated types")
                .isNotNull();
    }

    @Test
    void annotationTargetsAllExpectedElementKinds() {
        Target target = Beta.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();
        assertThat(target.value()).contains(
                ElementType.TYPE,
                ElementType.METHOD,
                ElementType.FIELD,
                ElementType.CONSTRUCTOR,
                ElementType.PACKAGE);
    }

    @Test
    void annotationSourceJavadocStatesTheStabilityContract() throws IOException {
        Path source = Path.of("src/main/java/com/demcha/compose/document/api/Beta.java");
        String body = Files.readString(source);
        assertThat(body)
                .describedAs("Javadoc must name both Extension SPI and Experimental usages")
                .contains("Extension SPI")
                .contains("Experimental");
        assertThat(body)
                .describedAs("Javadoc must link to the API stability policy page")
                .contains("docs/api-stability.md");
        assertThat(body)
                .describedAs("Javadoc must direct users to the issue tracker for stabilisation requests")
                .contains("github.com/DemchaAV/GraphCompose/issues");
    }

    @Test
    void betaAndInternalLiveInTheSameApiPackage() {
        assertThat(Beta.class.getPackage().getName())
                .describedAs("@Beta should live next to @Internal so IDE autocomplete surfaces both")
                .isEqualTo(Internal.class.getPackage().getName());
    }
}
