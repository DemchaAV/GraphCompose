package com.demcha.compose.document.templates.cv.spec;

import com.demcha.compose.document.templates.blocks.ParagraphBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CvSpecTest {

    private static CvHeader sampleHeader() {
        return CvHeader.builder()
                .name("Alex Carter")
                .address("EH3, Edinburgh, UK")
                .phone("+44 7700 900123")
                .email("alex.carter@example.dev")
                .link("LinkedIn", "https://linkedin.com/in/alex")
                .link("GitHub", "https://github.com/alexc")
                .build();
    }

    private static CvModule paragraphModule(String name) {
        return CvModule.of(name, new ParagraphBlock("body for " + name));
    }

    // CvHeader --------------------------------------------------------

    @Test
    void cvHeaderRequiresNonBlankName() {
        assertThatThrownBy(() -> new CvHeader("", "", "a", "b", "c", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CvHeader(null, "", "a", "b", "c", List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void cvHeaderNormalisesNullStringsToEmpty() {
        CvHeader h = new CvHeader("X", null, null, null, null, null);
        assertThat(h.jobTitle()).isEmpty();
        assertThat(h.address()).isEmpty();
        assertThat(h.phone()).isEmpty();
        assertThat(h.email()).isEmpty();
        assertThat(h.links()).isEmpty();
    }

    @Test
    void contactItemsSkipsBlanks() {
        CvHeader h = CvHeader.builder().name("X").address("A").phone("").build();
        assertThat(h.contactItems()).containsExactly("A");
    }

    @Test
    void linkLabelsIncludeEmailFirstThenLinkLabels() {
        CvHeader h = sampleHeader();
        assertThat(h.linkLabels()).containsExactly(
                "alex.carter@example.dev", "LinkedIn", "GitHub");
    }

    @Test
    void linkRequiresNonBlankLabel() {
        assertThatThrownBy(() -> new CvHeader.Link("", "url"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CvHeader.Link(null, "url"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CvHeader.Link("Label", null))
                .isInstanceOf(NullPointerException.class);
    }

    // CvModule --------------------------------------------------------

    @Test
    void cvModuleOfUsesNameAsTitle() {
        CvModule m = CvModule.of("Skills", new ParagraphBlock("text"));
        assertThat(m.name()).isEqualTo("Skills");
        assertThat(m.title()).isEqualTo("Skills");
    }

    @Test
    void cvModuleSeparateTitlePreserved() {
        CvModule m = new CvModule("skills", "Technical Skills", new ParagraphBlock("text"));
        assertThat(m.name()).isEqualTo("skills");
        assertThat(m.title()).isEqualTo("Technical Skills");
    }

    @Test
    void cvModuleRequiresNonBlankName() {
        assertThatThrownBy(() -> new CvModule("", "title", new ParagraphBlock("x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cvModuleRejectsNullFields() {
        assertThatThrownBy(() -> new CvModule(null, "t", new ParagraphBlock("x")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CvModule("n", null, new ParagraphBlock("x")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CvModule("n", "t", null))
                .isInstanceOf(NullPointerException.class);
    }

    // CvSpec ----------------------------------------------------------

    @Test
    void cvSpecBuilderProducesOrderedSpec() {
        CvSpec spec = CvSpec.builder()
                .header(sampleHeader())
                .module(paragraphModule("A"))
                .module(paragraphModule("B"))
                .module(paragraphModule("C"))
                .build();
        assertThat(spec.modules()).extracting(CvModule::name)
                .containsExactly("A", "B", "C");
    }

    @Test
    void cvSpecRejectsDuplicateModuleNames() {
        assertThatThrownBy(() -> CvSpec.builder()
                .header(sampleHeader())
                .module(paragraphModule("A"))
                .module(paragraphModule("A"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void findModuleReturnsByName() {
        CvSpec spec = CvSpec.builder()
                .header(sampleHeader())
                .module(paragraphModule("Summary"))
                .module(paragraphModule("Skills"))
                .build();
        assertThat(spec.findModule("Skills")).isPresent();
        assertThat(spec.findModule("Skills").orElseThrow().name()).isEqualTo("Skills");
    }

    @Test
    void findModuleReturnsEmptyWhenAbsent() {
        CvSpec spec = CvSpec.builder()
                .header(sampleHeader())
                .module(paragraphModule("Summary"))
                .build();
        assertThat(spec.findModule("Missing")).isEmpty();
    }

    @Test
    void cvSpecRejectsNullHeader() {
        assertThatThrownBy(() -> new CvSpec(null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }
}
