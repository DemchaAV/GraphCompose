package com.demcha.compose.document.templates.components;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.templates.themes.Spacing;
import com.demcha.compose.document.theme.BusinessTheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeaderTest {

    private static final BusinessTheme THEME = BusinessTheme.modern();
    private static final Spacing SPACING = Spacing.compact();

    @Test
    void rightAlignedComposeProducesNameContactLinksRows() {
        Header header = Header.rightAligned(THEME, SPACING);
        DocumentNode node = header.compose(new Header.Input(
                "Alex Carter",
                List.of("EH3, Edinburgh, UK", "+44 7700 900123"),
                List.of("alex.carter@example.dev", "LinkedIn", "GitHub")));

        assertThat(node).isInstanceOf(ContainerNode.class);
        ContainerNode container = (ContainerNode) node;
        assertThat(container.name()).isEqualTo("Header");
        assertThat(container.children()).hasSize(3);

        ParagraphNode name = (ParagraphNode) container.children().get(0);
        assertThat(name.text()).isEqualTo("Alex Carter");
        assertThat(name.align()).isEqualTo(TextAlign.RIGHT);

        ParagraphNode contact = (ParagraphNode) container.children().get(1);
        assertThat(contact.text()).isEqualTo("EH3, Edinburgh, UK | +44 7700 900123");
        assertThat(contact.align()).isEqualTo(TextAlign.RIGHT);

        ParagraphNode links = (ParagraphNode) container.children().get(2);
        assertThat(links.text()).isEqualTo("alex.carter@example.dev | LinkedIn | GitHub");
        assertThat(links.align()).isEqualTo(TextAlign.RIGHT);
    }

    @Test
    void emptyContactItemsAreSkipped() {
        Header header = Header.rightAligned(THEME, SPACING);
        DocumentNode node = header.compose(new Header.Input(
                "Name",
                List.of(),
                List.of()));

        ContainerNode container = (ContainerNode) node;
        assertThat(container.children()).hasSize(1); // just the name row
    }

    @Test
    void blankAndNullEntriesInListsAreSkippedDuringJoin() {
        // Java's List.copyOf rejects null entries, so we use a list with
        // only blank entries to verify they are stripped during join.
        Header header = Header.rightAligned(THEME, SPACING);
        DocumentNode node = header.compose(new Header.Input(
                "Name",
                List.of("", "  ", "Real Address"),
                List.of()));
        ContainerNode container = (ContainerNode) node;
        ParagraphNode contact = (ParagraphNode) container.children().get(1);
        assertThat(contact.text()).isEqualTo("Real Address");
    }

    @Test
    void inputDefensivelyCopiesLists() {
        java.util.ArrayList<String> source = new java.util.ArrayList<>();
        source.add("a");
        Header.Input input = new Header.Input("Name", source, List.of());
        source.add("mutated");
        assertThat(input.contactItems()).containsExactly("a");
    }

    @Test
    void inputAcceptsNullListsAsEmpty() {
        Header.Input input = new Header.Input("Name", null, null);
        assertThat(input.contactItems()).isEmpty();
        assertThat(input.linkLabels()).isEmpty();
    }

    @Test
    void inputRejectsNullName() {
        assertThatThrownBy(() -> new Header.Input(null, List.of(), List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rightAlignedRejectsNullArguments() {
        assertThatThrownBy(() -> Header.rightAligned(null, SPACING))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Header.rightAligned(THEME, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void composeRejectsNullInput() {
        Header header = Header.rightAligned(THEME, SPACING);
        assertThatThrownBy(() -> header.compose(null))
                .isInstanceOf(NullPointerException.class);
    }
}
