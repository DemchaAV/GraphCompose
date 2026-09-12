package com.demcha.compose.document.templates.rota.presets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.TemplateTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exact layout snapshot gate for {@link CobaltRota} — freezes the page frame and
 * the table's own box.
 *
 * <p><strong>What this gate can and cannot see.</strong> The sheet is composed
 * table cells almost end to end, and a composed cell emits fragments rather than
 * a placed node, so the snapshot holds two nodes for eighty-four shift cells. It
 * pins the page and the table that carries them — which is what would move if
 * the margins, the column widths or the orientation changed — and the pixel
 * baseline is what pins their contents. Neither gate is sufficient alone here,
 * which is why both are fed by one fixture.</p>
 *
 * <p>Refresh with {@code -Dgraphcompose.updateSnapshots=true} after a deliberate
 * layout change, and commit the JSON with the change.</p>
 */
class CobaltRotaLayoutSnapshotTest {

    @Test
    void canonicalRotaMatchesLayoutSnapshot() throws Exception {
        try (DocumentSession session = GraphCompose.document().create()) {
            CobaltRota.create().compose(session, CobaltRotaFixtures.canonicalRota());
            assertThat(session.layoutSnapshot().totalPages()).isEqualTo(1);
            TemplateTestSupport.assertCanonicalSnapshot(session, "cobalt_rota_layout", "rota");
        }
    }
}
