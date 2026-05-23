package com.demcha.compose.document.templates.cv.v2.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CvDocumentSlotTest {

    private static CvIdentity identity() {
        return CvIdentity.builder()
                .name("Jane", "Doe")
                .contact("+44 0", "j@d.com", "London")
                .build();
    }

    private static ParagraphSection summary() {
        return new ParagraphSection("Summary", "body");
    }

    private static RowsSection skills() {
        return RowsSection.builder("Skills", RowStyle.BULLETED)
                .row("Languages", "Java").build();
    }

    @Test
    void builder_section_defaults_to_main() {
        ParagraphSection s = summary();
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(s)
                .build();

        assertThat(doc.placements()).hasSize(1);
        assertThat(doc.placements().get(0).slot()).isEqualTo(Slot.MAIN);
        assertThat(doc.slotOf(s)).isEqualTo(Slot.MAIN);
    }

    @Test
    void builder_section_with_explicit_slot_uses_it() {
        ParagraphSection s = summary();
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(Slot.SIDEBAR, s)
                .build();

        assertThat(doc.placements().get(0).slot()).isEqualTo(Slot.SIDEBAR);
        assertThat(doc.slotOf(s)).isEqualTo(Slot.SIDEBAR);
    }

    @Test
    void sections_returns_all_regardless_of_slot() {
        ParagraphSection mainSec = summary();
        RowsSection sidebarSec = skills();
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(mainSec)
                .section(Slot.SIDEBAR, sidebarSec)
                .build();

        assertThat(doc.sections()).containsExactly(mainSec, sidebarSec);
    }

    @Test
    void sectionsIn_filters_by_slot() {
        ParagraphSection mainSec = summary();
        RowsSection sidebarSec = skills();
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(mainSec)
                .section(Slot.SIDEBAR, sidebarSec)
                .build();

        assertThat(doc.sectionsIn(Slot.MAIN)).containsExactly(mainSec);
        assertThat(doc.sectionsIn(Slot.SIDEBAR)).containsExactly(sidebarSec);
        assertThat(doc.sectionsIn(Slot.FOOTER)).isEmpty();
    }

    @Test
    void slotOf_returns_main_for_unknown_section() {
        ParagraphSection orphan = new ParagraphSection("Orphan", "x");
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(summary())
                .build();

        assertThat(doc.slotOf(orphan)).isEqualTo(Slot.MAIN);
    }

    @Test
    void sections_varargs_with_slot_places_all_in_that_slot() {
        ParagraphSection a = new ParagraphSection("A", "x");
        ParagraphSection b = new ParagraphSection("B", "y");
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .sections(Slot.SIDEBAR, a, b)
                .build();

        assertThat(doc.sectionsIn(Slot.SIDEBAR)).containsExactly(a, b);
        assertThat(doc.sectionsIn(Slot.MAIN)).isEmpty();
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecated_ofMainSections_wraps_everything_in_main() {
        ParagraphSection a = new ParagraphSection("A", "x");
        ParagraphSection b = new ParagraphSection("B", "y");
        CvDocument doc = CvDocument.ofMainSections(identity(), List.of(a, b));

        assertThat(doc.sectionsIn(Slot.MAIN)).containsExactly(a, b);
        assertThat(doc.sectionsIn(Slot.SIDEBAR)).isEmpty();
    }

    @Test
    void placements_preserves_source_order_across_slots() {
        ParagraphSection a = new ParagraphSection("A", "x");
        RowsSection b = skills();
        ParagraphSection c = new ParagraphSection("C", "z");
        CvDocument doc = CvDocument.builder()
                .identity(identity())
                .section(a)
                .section(Slot.SIDEBAR, b)
                .section(c)
                .build();

        // sections() preserves the global add order
        assertThat(doc.sections()).containsExactly(a, b, c);
        // sectionsIn filters but keeps relative order
        assertThat(doc.sectionsIn(Slot.MAIN)).containsExactly(a, c);
    }

    @Test
    void rejects_null_section() {
        CvDocument.Builder b = CvDocument.builder().identity(identity());
        assertThatThrownBy(() -> b.section(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> b.section(Slot.SIDEBAR, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_slot() {
        CvDocument.Builder b = CvDocument.builder().identity(identity());
        assertThatThrownBy(() -> b.section(null, summary()))
                .isInstanceOf(NullPointerException.class);
    }
}
