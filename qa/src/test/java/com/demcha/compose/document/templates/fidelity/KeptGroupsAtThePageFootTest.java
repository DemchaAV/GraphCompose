package com.demcha.compose.document.templates.fidelity;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.layout.PlacedNode;
import com.demcha.compose.document.templates.fidelity.KeptGroupsAtThePageFoot.AtTheFoot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The documents {@link KeptGroupsAtThePageFoot} adds to the DOCX fidelity corpus stand where their
 * names say: each group whole on page 1 in the document that fits it there, and whole on page 2 in
 * the other, and a group kept with the next with the block after it on its page. Since the second
 * document stands one line of filler lower, the first also fails here when it is not as low as the
 * group fits. The fidelity gate holds where an editor sets their lines; this
 * holds that the layout puts each group at the foot of a page, which is what the documents are
 * for, and which a change to the layout's metrics could otherwise move without a word.
 */
class KeptGroupsAtThePageFootTest {

    @Test
    void eachGroupStandsWholeOnThePageItsDocumentIsNamedFor() {
        List<AtTheFoot> fixtures = KeptGroupsAtThePageFoot.fixtures();
        assertThat(fixtures).hasSize(2 * KeptGroupsAtThePageFoot.forms().size());
        for (AtTheFoot fixture : fixtures) {
            DocxCorpusDocument document = fixture.document();
            try (DocumentSession session = DocxFidelityCorpusTest.composed(document)) {
                List<PlacedNode> groups = session.layoutGraph().nodes().stream()
                        .filter(placed -> KeptGroupsAtThePageFoot.GROUP.equals(placed.semanticName()))
                        .toList();
                assertThat(groups).as("%s places its group once", document.stem()).hasSize(1);
                PlacedNode group = groups.get(0);
                assertThat(group.startPage()).as("%s: the page its group opens on", document.stem())
                        .isEqualTo(fixture.groupPage());
                assertThat(group.endPage()).as("%s: the page its group closes on", document.stem())
                        .isEqualTo(fixture.groupPage());
                if (fixture.form().keptWithNext()) {
                    PlacedNode next = session.layoutGraph().nodes().stream()
                            .filter(placed -> group.parentPath().equals(placed.parentPath())
                                              && placed.childIndex() == group.childIndex() + 1)
                            .findFirst().orElseThrow(() -> new AssertionError(
                                    document.stem() + ": no block follows the group it is kept with"));
                    assertThat(next.startPage()).as("%s: the page the block kept with its group opens on",
                            document.stem()).isEqualTo(fixture.groupPage());
                }
            }
        }
    }
}
