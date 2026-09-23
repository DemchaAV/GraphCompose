package com.demcha.compose.document.backend.semantic;

import com.demcha.compose.document.layout.DocumentGraph;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A semantic backend that does not override {@code exportSections} still exports one section,
 * and refuses several rather than running them together.
 *
 * @author Artem Demchyshyn
 */
class SemanticBackendSectionsTest {

    @Test
    void oneSectionIsExportedAsTheDocument() throws Exception {
        Recording backend = new Recording();
        SemanticSection only = section();

        assertThat(backend.exportSections(List.of(only))).isEqualTo("exported");
        assertThat(backend.graphs).containsExactly(only.graph());
    }

    @Test
    void severalSectionsAreRefusedByABackendThatCannotCombineThem() {
        Recording backend = new Recording();

        assertThatThrownBy(() -> backend.exportSections(List.of(section(), section())))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("recording")
                .hasMessageContaining("2 sections");
        assertThat(backend.graphs)
                .as("nothing is exported on the way to refusing")
                .isEmpty();
    }

    @Test
    void noSectionsIsNotADocument() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Recording().exportSections(List.of()));
    }

    private static SemanticSection section() {
        return new SemanticSection(new DocumentGraph(List.of()),
                new SemanticExportContext(null, List.of(), null));
    }

    private static final class Recording implements SemanticBackend<String> {

        private final List<DocumentGraph> graphs = new ArrayList<>();

        @Override
        public String name() {
            return "recording";
        }

        @Override
        public String export(DocumentGraph graph, SemanticExportContext context) {
            graphs.add(graph);
            return "exported";
        }
    }
}
