package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.DocumentLinkTarget;
import com.demcha.compose.document.node.ExternalLinkTarget;
import com.demcha.compose.document.node.InlineShapeRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.InternalLinkTarget;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.ShapeNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.ShapeOutline;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-level guards for the {@code DocumentLinkTarget} DSL mapping — that
 * {@code link(...)} produces an external target and {@code linkTo(...)} /
 * {@code anchor(...)} produce internal targets / destinations on the resolved
 * nodes, independent of the PDF backend.
 *
 * @author Artem Demchyshyn
 */
class DocumentLinkTargetDslTest {

    @Test
    void richTextLinkToProducesInternalTarget() {
        InlineTextRun run = (InlineTextRun) RichText.text("see ").linkTo("intro", "intro").runs().get(1);
        assertThat(run.linkTarget()).isInstanceOf(InternalLinkTarget.class);
        assertThat(((InternalLinkTarget) run.linkTarget()).anchor()).isEqualTo("intro");
    }

    @Test
    void richTextLinkProducesExternalTarget() {
        InlineTextRun run = (InlineTextRun) RichText.text("see ").link("site", "https://example.com").runs().get(1);
        assertThat(run.linkTarget()).isInstanceOf(ExternalLinkTarget.class);
        assertThat(((ExternalLinkTarget) run.linkTarget()).options().uri()).isEqualTo("https://example.com");
    }

    @Test
    void paragraphBuilderThreadsAnchorAndInternalLink() {
        ParagraphNode node = new ParagraphBuilder().text("body").anchor("here").linkTo("there").build();
        assertThat(node.anchor()).isEqualTo("here");
        assertThat(node.linkTarget()).isInstanceOf(InternalLinkTarget.class);
        assertThat(((InternalLinkTarget) node.linkTarget()).anchor()).isEqualTo("there");
    }

    @Test
    void shapeBuilderThreadsAnchorAndInternalLink() {
        ShapeNode node = new ShapeBuilder().size(10, 10).anchor("sa").linkTo("st").build();
        assertThat(node.anchor()).isEqualTo("sa");
        assertThat(((InternalLinkTarget) node.linkTarget()).anchor()).isEqualTo("st");
    }

    @Test
    void linkAcceptsPrebuiltTargetAndClearsWithNull() {
        ParagraphNode targeted =
                new ParagraphBuilder().text("t").linkTarget(DocumentLinkTarget.anchor("z")).build();
        assertThat(((InternalLinkTarget) targeted.linkTarget()).anchor()).isEqualTo("z");

        // link(null) stays unambiguous against link(DocumentLinkOptions) — no link set.
        ParagraphNode cleared = new ParagraphBuilder().text("t").link((DocumentLinkOptions) null).build();
        assertThat(cleared.linkTarget()).isNull();
    }

    @Test
    void richTextShapeLinkToProducesInternalGraphicLink() {
        InlineShapeRun run = (InlineShapeRun) RichText.text("rate ")
                .shapeLinkTo(ShapeOutline.circle(6), DocumentColor.of(Color.RED), "scale")
                .runs().get(1);
        assertThat(run.linkTarget()).isInstanceOf(InternalLinkTarget.class);
        assertThat(((InternalLinkTarget) run.linkTarget()).anchor()).isEqualTo("scale");
    }

    @Test
    void paragraphBuilderInlineShapeLinkToProducesInternalGraphicLink() {
        ParagraphNode node = new ParagraphBuilder()
                .shapeLinkTo(ShapeOutline.circle(6), DocumentColor.of(Color.RED), "scale")
                .build();
        InlineShapeRun run = (InlineShapeRun) node.inlineRuns().get(0);
        assertThat(((InternalLinkTarget) run.linkTarget()).anchor()).isEqualTo("scale");
    }

    @Test
    void internalLinkTargetRejectsBlankAnchor() {
        assertThatThrownBy(() -> new InternalLinkTarget("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anchor");
    }
}
