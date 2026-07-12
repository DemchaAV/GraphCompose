package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentStroke;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers {@link AbstractFlowBuilder#headingBar} — the filled title-band shortcut
 * adds a child bar section (soft panel + label) ahead of the body.
 */
class HeadingBarTest {

    private static final DocumentColor NAVY = DocumentColor.rgb(20, 30, 60);

    @Test
    void headingBarAddsChildBarWithLabel() {
        SectionNode section = new SectionBuilder()
                .headingBar("EXPERIENCE", bar -> bar.fill(NAVY).cornerRadius(4))
                .build();

        assertThat(section.children()).hasSize(1);
        DocumentNode child = section.children().get(0);
        assertThat(child).isInstanceOf(SectionNode.class);
        SectionNode bar = (SectionNode) child;
        assertThat(bar.fillColor()).isEqualTo(NAVY);
        assertThat(bar.children()).hasSize(1);
        assertThat(bar.children().get(0)).isInstanceOf(ParagraphNode.class);
        assertThat(((ParagraphNode) bar.children().get(0)).text()).isEqualTo("EXPERIENCE");
    }

    @Test
    void defaultHeadingBarUsesGreyBandFill() {
        SectionNode section = new SectionBuilder().headingBar("Summary").build();
        SectionNode bar = (SectionNode) section.children().get(0);
        // DocumentColor has identity equals, so compare the underlying AWT value.
        assertThat(bar.fillColor().color()).isEqualTo(new java.awt.Color(238, 238, 238));
    }

    @Test
    void headingBarAppliesOptionalStroke() {
        DocumentStroke outline = DocumentStroke.of(DocumentColor.rgb(10, 10, 10), 1.0);
        SectionNode section = new SectionBuilder()
                .headingBar("Skills", bar -> bar.stroke(outline))
                .build();
        SectionNode bar = (SectionNode) section.children().get(0);
        assertThat(bar.stroke()).isEqualTo(outline);
    }

    @Test
    void headingBarThenBodyComposeInOrder() {
        SectionNode section = new SectionBuilder()
                .headingBar("EXPERIENCE", bar -> bar.fill(NAVY))
                .addParagraph(p -> p.text("Body text"))
                .build();
        assertThat(section.children()).hasSize(2);
        assertThat(section.children().get(0)).isInstanceOf(SectionNode.class);
        assertThat(section.children().get(1)).isInstanceOf(ParagraphNode.class);
    }
}
