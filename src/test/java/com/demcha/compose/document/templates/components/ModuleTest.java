package com.demcha.compose.document.templates.components;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.templates.blocks.BulletListBlock;
import com.demcha.compose.document.templates.blocks.IndentedBlock;
import com.demcha.compose.document.templates.blocks.KeyValueBlock;
import com.demcha.compose.document.templates.blocks.MultiParagraphBlock;
import com.demcha.compose.document.templates.blocks.NumberedListBlock;
import com.demcha.compose.document.templates.blocks.ParagraphBlock;
import com.demcha.compose.document.templates.themes.Spacing;
import com.demcha.compose.document.theme.BusinessTheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuleTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final Spacing SPACING = Spacing.compact();

    @Test
    void composeWithParagraphBlockProducesHeadingPlusBody() {
        Module module = Module.of(
                "Summary",
                "Professional Summary",
                new ParagraphBlock("Backend Java Developer building Spring Boot..."),
                Module.headingFlat(THEME));

        DocumentNode node = module.compose(THEME, SPACING);

        assertThat(node).isInstanceOf(ContainerNode.class);
        ContainerNode container = (ContainerNode) node;
        assertThat(container.name()).isEqualTo("Summary");
        assertThat(container.children()).hasSize(2);
        assertThat(container.children().get(0)).isInstanceOf(ParagraphNode.class);
        assertThat(((ParagraphNode) container.children().get(0)).text())
                .isEqualTo("Professional Summary");
        assertThat(container.children().get(1)).isInstanceOf(ParagraphNode.class);
        assertThat(((ParagraphNode) container.children().get(1)).text())
                .isEqualTo("Backend Java Developer building Spring Boot...");
    }

    @Test
    void composeWithBulletListBlockProducesListNodeBody() {
        Module module = Module.of(
                "Skills",
                "Technical Skills",
                new BulletListBlock(List.of("Java", "Kotlin", "SQL")),
                Module.headingFlat(THEME));

        ContainerNode container = (ContainerNode) module.compose(THEME, SPACING);
        assertThat(container.children().get(1)).isInstanceOf(ListNode.class);
        ListNode list = (ListNode) container.children().get(1);
        assertThat(list.items()).containsExactly("Java", "Kotlin", "SQL");
    }

    @Test
    void composeWithNumberedListBlockProducesListNodeBody() {
        Module module = Module.of(
                "Steps",
                "Process",
                new NumberedListBlock(List.of("Plan", "Execute", "Review")),
                Module.headingFlat(THEME));

        ContainerNode container = (ContainerNode) module.compose(THEME, SPACING);
        assertThat(container.children().get(1)).isInstanceOf(ListNode.class);
    }

    @Test
    void composeWithMultiParagraphBlockProducesContainerOfParagraphs() {
        Module module = Module.of(
                "Letter",
                "Cover Letter",
                new MultiParagraphBlock(List.of("Dear Hiring Manager,", "Body paragraph.", "Sincerely,")),
                Module.headingFlat(THEME));

        ContainerNode container = (ContainerNode) module.compose(THEME, SPACING);
        DocumentNode body = container.children().get(1);
        assertThat(body).isInstanceOf(ContainerNode.class);
        assertThat(((ContainerNode) body).children()).hasSize(3);
    }

    @Test
    void composeWithIndentedBlockProducesAlternatingTitleBodyEntries() {
        Module module = Module.of(
                "Education",
                "Education & Certifications",
                new IndentedBlock(List.of(
                        new IndentedBlock.Item("MSc CS", "University of Manchester | 2021"),
                        new IndentedBlock.Item("BSc CS", "Northbridge | 2018"))),
                Module.headingFlat(THEME));

        ContainerNode container = (ContainerNode) module.compose(THEME, SPACING);
        DocumentNode body = container.children().get(1);
        assertThat(body).isInstanceOf(ContainerNode.class);
        assertThat(((ContainerNode) body).children()).hasSize(4); // 2 items × (title + body)
    }

    @Test
    void composeWithKeyValueBlockProducesEntryParagraphs() {
        Module module = Module.of(
                "Additional",
                "Additional Information",
                new KeyValueBlock(List.of(
                        new KeyValueBlock.Entry("Languages", "English"),
                        new KeyValueBlock.Entry("Eligibility", "UK"))),
                Module.headingFlat(THEME));

        ContainerNode container = (ContainerNode) module.compose(THEME, SPACING);
        DocumentNode body = container.children().get(1);
        assertThat(body).isInstanceOf(ContainerNode.class);
        ContainerNode bodyContainer = (ContainerNode) body;
        assertThat(bodyContainer.children()).hasSize(2);
        assertThat(((ParagraphNode) bodyContainer.children().get(0)).text())
                .isEqualTo("Languages: English");
    }

    @Test
    void emptyTitleSuppressesHeadingRow() {
        Module module = Module.of(
                "BodyOnly",
                "",
                new ParagraphBlock("text"),
                Module.headingFlat(THEME));

        ContainerNode container = (ContainerNode) module.compose(THEME, SPACING);
        assertThat(container.children()).hasSize(1); // body only
    }

    @Test
    void styleMarginAdjustersReturnNewInstancesWithoutMutating() {
        Module.Style flat = Module.headingFlat(THEME);
        Module.Style above = flat.marginAbove(20.0);
        Module.Style both = above.marginBelow(7.0);

        assertThat(flat.marginAbove()).isEqualTo(8.0);  // unchanged
        assertThat(above.marginAbove()).isEqualTo(20.0);
        assertThat(above.marginBelow()).isEqualTo(4.0); // unchanged from flat
        assertThat(both.marginAbove()).isEqualTo(20.0); // chained
        assertThat(both.marginBelow()).isEqualTo(7.0);
    }

    @Test
    void rejectsNullArguments() {
        ParagraphBlock block = new ParagraphBlock("x");
        Module.Style style = Module.headingFlat(THEME);
        assertThatThrownBy(() -> Module.of(null, "title", block, style))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Module.of("name", null, block, style))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Module.of("name", "title", null, style))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Module.of("name", "title", block, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void composeRejectsNullThemeOrSpacing() {
        Module module = Module.of("X", "T", new ParagraphBlock("x"), Module.headingFlat(THEME));
        assertThatThrownBy(() -> module.compose(null, SPACING))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> module.compose(THEME, null))
                .isInstanceOf(NullPointerException.class);
    }
}
