package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.exceptions.MissingBackendException;
import com.demcha.compose.document.layout.DocumentGraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Choosing one semantic export backend out of what a classpath happens to carry.
 *
 * <p>The rules are the fixed-layout locator's, for the reason they were chosen there: a
 * classpath is assembled by a build rather than by the person exporting, so a format
 * nobody provides has to name the artifact to add, and two providers for one format has to
 * fail rather than let {@link java.util.ServiceLoader} order decide — otherwise one
 * document exports differently on two machines and nothing says why.</p>
 *
 * @author Artem Demchyshyn
 */
class SemanticBackendProvidersTest {

    @Test
    void theFormatIsAKeyRatherThanASpelling() {
        SemanticBackendProvider docx = provider("DocX");

        assertThat(SemanticBackendProviders.select("docx", List.of(docx))).isSameAs(docx);
    }

    @Test
    void aFormatNobodyProvidesNamesTheArtifactToAdd() {
        assertThatThrownBy(() -> SemanticBackendProviders.select("docx", List.of(provider("rtf"))))
                .isInstanceOf(MissingBackendException.class)
                .hasMessageContaining("graph-compose-render-docx")
                .as("and says what to do instead of naming an artifact")
                .hasMessageContaining("export(...)");
    }

    @Test
    void anUnknownFormatStillSaysWhatIsMissing() {
        // Nothing here knows which artifact would provide "odt", and saying nothing at all
        // would leave a caller with a null and no idea why.
        assertThatThrownBy(() -> SemanticBackendProviders.select("odt", List.of()))
                .isInstanceOf(MissingBackendException.class)
                .hasMessageContaining("odt")
                .hasMessageContaining("classpath");
    }

    @Test
    void twoProvidersForOneFormatIsReportedRatherThanResolved() {
        assertThatThrownBy(() -> SemanticBackendProviders.select(
                "docx", List.of(provider("docx"), provider("docx"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple semantic export backends")
                .hasMessageContaining("docx");
    }

    @Test
    void aProviderDeclaringNoFormatIsSkippedRatherThanMatchingEverything() {
        SemanticBackendProvider docx = provider("docx");

        assertThat(SemanticBackendProviders.select("docx", List.of(provider(null), docx)))
                .isSameAs(docx);
    }

    private static SemanticBackendProvider provider(String format) {
        return new SemanticBackendProvider() {
            @Override
            public String format() {
                return format;
            }

            @Override
            public SemanticBackend<byte[]> create() {
                return new SemanticBackend<>() {
                    @Override
                    public String name() {
                        return "probe";
                    }

                    @Override
                    public byte[] export(DocumentGraph graph, SemanticExportContext context) {
                        return new byte[0];
                    }
                };
            }
        };
    }
}
