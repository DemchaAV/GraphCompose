package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.api.Beta;
import com.demcha.compose.document.backend.semantic.SemanticExportContext;
import com.demcha.compose.document.layout.DocumentGraph;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which parts of the Word export are Experimental, and which stay Stable.
 *
 * <p>{@code docs/api-stability.md} names what is Experimental, and the API reference reads
 * the annotation. A member left unmarked is listed there as Stable whatever the page says,
 * and a nested type does not take its enclosing type's marker. These pin the marker on each
 * part the export added in 2.5, and its absence on the Stable path a caller already had.</p>
 *
 * @author Artem Demchyshyn
 */
class DocxBetaSurfaceTest {

    @Test
    void theProviderIsExperimental() {
        assertBeta(DocxBackendProvider.class);
    }

    @Test
    void theReportAndTheTypesNestedInItAreExperimental() {
        assertBeta(DocxExportReport.class);
        assertBeta(DocxExportReport.Note.class);
        assertBeta(DocxExportReport.Severity.class);
    }

    @Test
    void theConstructorThatTakesAReportSinkIsExperimental() throws Exception {
        assertBeta(DocxSemanticBackend.class.getConstructor(Consumer.class));
    }

    @Test
    void theBuilderIsExperimental() throws Exception {
        assertBeta(DocxSemanticBackend.class.getMethod("builder"));
        assertBeta(DocxSemanticBackend.Builder.class);
    }

    @Test
    void theBackendsOverridesOfTheExperimentalSeamAreExperimental() throws Exception {
        assertBeta(DocxSemanticBackend.class.getDeclaredMethod("requiresResolvedLayout"));
        assertBeta(DocxSemanticBackend.class.getDeclaredMethod("exportSections", List.class));
    }

    @Test
    void theStablePathStaysUnmarked() throws Exception {
        assertStable(DocxSemanticBackend.class);
        assertStable(DocxSemanticBackend.class.getConstructor());
        assertStable(DocxSemanticBackend.class.getMethod("name"));
        assertStable(DocxSemanticBackend.class.getDeclaredMethod(
                "export", DocumentGraph.class, SemanticExportContext.class));
    }

    private static void assertBeta(AnnotatedElement element) {
        assertThat(element.isAnnotationPresent(Beta.class))
                .as("%s should carry @Beta", element)
                .isTrue();
    }

    private static void assertStable(AnnotatedElement element) {
        assertThat(element.isAnnotationPresent(Beta.class))
                .as("%s should not carry @Beta", element)
                .isFalse();
    }
}
