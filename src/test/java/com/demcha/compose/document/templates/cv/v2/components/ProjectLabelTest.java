package com.demcha.compose.document.templates.cv.v2.components;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pinned behaviour for {@link ProjectLabel#parse(String)}, the entry
 * point that splits a CV project row's label into a title segment
 * and an optional trailing technology-stack segment.
 *
 * <p>Since v1.6.8 the title preserves inline Markdown syntax so
 * downstream renderers can route it through
 * {@link MarkdownInline#append(com.demcha.compose.document.dsl.RichText,
 * String, com.demcha.compose.document.style.DocumentTextStyle)} for
 * hyperlink rendering. Tests below pin both the legacy "GraphCompose
 * (Java, PDFBox)" shape and the new "[GraphCompose](url) (Java)"
 * shape.</p>
 */
class ProjectLabelTest {

    @Test
    void parseLegacyTitleAndStack() {
        ProjectLabel label = ProjectLabel.parse("GraphCompose (Java, PDFBox)");
        assertThat(label.title()).isEqualTo("GraphCompose");
        assertThat(label.stack()).isEqualTo("Java, PDFBox");
    }

    @Test
    void parseTitleOnly() {
        ProjectLabel label = ProjectLabel.parse("GraphCompose");
        assertThat(label.title()).isEqualTo("GraphCompose");
        assertThat(label.stack()).isEmpty();
    }

    @Test
    void parseNullReturnsEmptyLabel() {
        ProjectLabel label = ProjectLabel.parse(null);
        assertThat(label.title()).isEmpty();
        assertThat(label.stack()).isEmpty();
    }

    @Test
    void parsePreservesMarkdownLinkInsideTitle() {
        ProjectLabel label = ProjectLabel.parse(
                "[GraphCompose](https://github.com/x/y) (Java, PDFBox)");
        // Title keeps the link syntax — downstream renderer is
        // expected to route it through MarkdownInline.append.
        assertThat(label.title())
                .isEqualTo("[GraphCompose](https://github.com/x/y)");
        assertThat(label.stack()).isEqualTo("Java, PDFBox");
    }

    @Test
    void parseLinkOnlyKeepsMarkdownAndEmptyStack() {
        ProjectLabel label = ProjectLabel.parse(
                "[GraphCompose](https://github.com/x/y)");
        assertThat(label.title())
                .isEqualTo("[GraphCompose](https://github.com/x/y)");
        // The URL parens do not match the trailing-stack pattern
        // because there is no whitespace before the opening paren.
        assertThat(label.stack()).isEmpty();
    }

    @Test
    void parseTrimsLeadingAndTrailingWhitespace() {
        ProjectLabel label = ProjectLabel.parse("  GraphCompose (Java)  ");
        assertThat(label.title()).isEqualTo("GraphCompose");
        assertThat(label.stack()).isEqualTo("Java");
    }

    @Test
    void parseDoesNotConfuseUrlParensWithStackParens() {
        // Pattern requires whitespace before the stack's opening
        // paren, so the URL's `(...)` segment is left alone.
        ProjectLabel label = ProjectLabel.parse(
                "[Graph Compose](https://github.com/x/y)");
        assertThat(label.title())
                .isEqualTo("[Graph Compose](https://github.com/x/y)");
        assertThat(label.stack()).isEmpty();
    }

    @Test
    void parseLinkWithStackUsesTrailingStackOnly() {
        // Two paren groups in the input: the URL's `(https://...)` and
        // the stack's ` (Java)`. The whitespace requirement of the
        // trailing-stack pattern ensures only the second is treated
        // as the stack delimiter.
        ProjectLabel label = ProjectLabel.parse(
                "[Foo](https://example.com/foo) (Bar)");
        assertThat(label.title()).isEqualTo("[Foo](https://example.com/foo)");
        assertThat(label.stack()).isEqualTo("Bar");
    }
}
