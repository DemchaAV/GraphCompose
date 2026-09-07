package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.style.DocumentFlowWidth;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * The authoring half of the fixed-width flow: what {@code fixedWidth(...)} puts on
 * the node, what it refuses, and that a flow which never calls it carries the same
 * natural width it always did.
 */
class FlowFixedWidthApiTest {

    @Test
    void aSectionCarriesTheRequestedFixedWidth() {
        SectionNode section = new SectionBuilder().name("Card").fixedWidth(240).build();

        assertThat(section.flowWidth()).isEqualTo(DocumentFlowWidth.of(240));
        assertThat(section.flowWidth().isFixed()).isTrue();
    }

    @Test
    void aModuleCarriesTheRequestedFixedWidth() {
        SectionNode module = new ModuleBuilder().title("Aside").fixedWidth(180).build();

        assertThat(module.flowWidth()).isEqualTo(DocumentFlowWidth.of(180));
    }

    @Test
    void aSectionThatNeverAsksIsNatural() {
        SectionNode section = new SectionBuilder().name("Plain").addParagraph("body").build();

        assertThat(section.flowWidth()).isEqualTo(DocumentFlowWidth.natural());
        assertThat(section.flowWidth().isFixed()).isFalse();
    }

    @Test
    void everyOtherNodeKindIsNaturalByDefault() {
        DocumentNode paragraph = new ParagraphBuilder().text("body").build();

        assertThat(paragraph).isInstanceOf(ParagraphNode.class);
        assertThat(paragraph.flowWidth()).isEqualTo(DocumentFlowWidth.natural());
    }

    @Test
    void fixedWidthRejectsZero() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SectionBuilder().fixedWidth(0));
    }

    @Test
    void fixedWidthRejectsNegativeValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SectionBuilder().fixedWidth(-12));
    }

    @Test
    void fixedWidthRejectsNaNAndInfinity() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SectionBuilder().fixedWidth(Double.NaN));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SectionBuilder().fixedWidth(Double.POSITIVE_INFINITY));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SectionBuilder().fixedWidth(Double.NEGATIVE_INFINITY));
    }

    @Test
    void aRejectedWidthLeavesTheBuilderUntouched() {
        SectionBuilder builder = new SectionBuilder().name("Untouched");

        assertThatIllegalArgumentException().isThrownBy(() -> builder.fixedWidth(0));

        assertThat(builder.build().flowWidth()).isEqualTo(DocumentFlowWidth.natural());
    }

    @Test
    void theCompatibilityConstructorsDefaultBothNodesToNatural() {
        SectionNode section = new SectionNode("S", List.of(), 0, null, null, null, null, null, null,
                false, null, null, null, false);
        ContainerNode container = new ContainerNode("C", List.of(), 0, null, null, null, null, null, null,
                null, null);

        assertThat(section.flowWidth()).isEqualTo(DocumentFlowWidth.natural());
        assertThat(container.flowWidth()).isEqualTo(DocumentFlowWidth.natural());
    }

    @Test
    void aNullFlowWidthNormalizesToNatural() {
        SectionNode section = new SectionNode("S", List.of(), 0, null, null, null, null, null, null,
                false, null, null, null, false, null);
        ContainerNode container = new ContainerNode("C", List.of(), 0, null, null, null, null, null, null,
                null, null, null);

        assertThat(section.flowWidth()).isEqualTo(DocumentFlowWidth.natural());
        assertThat(container.flowWidth()).isEqualTo(DocumentFlowWidth.natural());
    }
}
