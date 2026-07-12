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

class InternalAnnotationDocumentationTest {

    @Test
    void annotationHasRuntimeRetentionForArchitectureGuards() {
        Retention retention = Internal.class.getAnnotation(Retention.class);
        assertThat(retention)
                .describedAs("@Internal must declare @Retention so guard tests can read it reflectively")
                .isNotNull();
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void annotationIsDocumentedSoJavadocSurfacesIt() {
        assertThat(Internal.class.getAnnotation(Documented.class))
                .describedAs("@Internal must be @Documented so Javadoc surfaces the marker on annotated types")
                .isNotNull();
    }

    @Test
    void annotationTargetsAllExpectedElementKinds() {
        Target target = Internal.class.getAnnotation(Target.class);
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
        Path source = Path.of("src/main/java/com/demcha/compose/document/api/Internal.java");
        String body = Files.readString(source);
        assertThat(body)
                .describedAs("Javadoc must explain that annotated elements may change without notice")
                .contains("may change in any release without notice");
        assertThat(body)
                .describedAs("Javadoc must direct users to the issue tracker for missing stable extensions")
                .contains("github.com/DemchaAV/GraphCompose/issues");
    }
}
