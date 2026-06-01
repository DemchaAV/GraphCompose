package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.RichText;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the v1.6.8 extension of {@link MarkdownInline} that
 * recognises {@code [label](url)} inline-markdown links and emits
 * them as {@link RichText#link(String, String)} runs, while still
 * routing {@code **bold**} / {@code *italic*} through the
 * {@code MarkdownText} emphasis parser as before.
 */
class MarkdownInlineTest {

    private static final DocumentTextStyle BASE = DocumentTextStyle.builder()
            .fontName(FontName.HELVETICA)
            .size(11)
            .decoration(DocumentTextDecoration.DEFAULT)
            .color(DocumentColor.BLACK)
            .build();

    // --- plainText -----------------------------------------------------------

    @Test
    void plainTextStripsLinkSyntaxLeavingOnlyTheVisibleLabel() {
        assertThat(MarkdownInline.plainText("[GraphCompose](https://github.com/x/y)"))
                .isEqualTo("GraphCompose");
    }

    @Test
    void plainTextStripsLinkAndEmphasisTogether() {
        assertThat(MarkdownInline.plainText("**[GraphCompose](https://x/y) (Java)**"))
                .isEqualTo("GraphCompose (Java)");
    }

    @Test
    void plainTextLeavesBareBracketsIntact() {
        // No (url) follows -> not a markdown link.
        assertThat(MarkdownInline.plainText("[just brackets]"))
                .isEqualTo("[just brackets]");
    }

    @Test
    void plainTextHandlesMultipleLinksInOneString() {
        assertThat(MarkdownInline.plainText(
                "[GraphCompose](https://gc) ships [docs](https://docs)"))
                .isEqualTo("GraphCompose ships docs");
    }

    @Test
    void plainTextOnNullReturnsEmptyString() {
        assertThat(MarkdownInline.plainText(null)).isEmpty();
    }

    // --- append: link emission -----------------------------------------------

    @Test
    void appendEmitsHyperlinkRunForMarkdownLink() {
        RichText rich = RichText.empty();
        MarkdownInline.append(rich, "[GraphCompose](https://github.com/x/y)", BASE);

        List<InlineRun> runs = rich.runs();
        assertThat(runs).hasSize(1);
        InlineTextRun only = (InlineTextRun) runs.get(0);
        assertThat(only.text()).isEqualTo("GraphCompose");
        assertThat(only.linkOptions())
                .isNotNull()
                .extracting(DocumentLinkOptions::uri)
                .isEqualTo("https://github.com/x/y");
    }

    @Test
    void appendMixesPlainEmphasisAndLink() {
        RichText rich = RichText.empty();
        MarkdownInline.append(rich,
                "Built **[GraphCompose](https://gc)** for fun",
                BASE);

        List<InlineTextRun> runs = rich.runs().stream()
                .map(r -> (InlineTextRun) r)
                .toList();

        // Sequence: "Built ", "" or "**" stripped, then link run "GraphCompose",
        // then any closing "**" stripped, then " for fun".
        // What matters: exactly ONE run carries link metadata, and its text
        // is the visible label.
        long linkCount = runs.stream()
                .filter(r -> r.linkOptions() != null)
                .count();
        assertThat(linkCount).isEqualTo(1);

        InlineTextRun link = runs.stream()
                .filter(r -> r.linkOptions() != null)
                .findFirst()
                .orElseThrow();
        assertThat(link.text()).isEqualTo("GraphCompose");
        assertThat(link.linkOptions().uri()).isEqualTo("https://gc");

        // Surrounding plain text must still be present somewhere in the
        // run sequence — the emphasis parser is free to fragment it as it
        // sees fit.
        String concatenated = runs.stream()
                .map(InlineTextRun::text)
                .reduce("", String::concat);
        assertThat(concatenated)
                .contains("Built ")
                .contains("GraphCompose")
                .contains(" for fun");
    }

    @Test
    void appendHandlesMultipleLinksAndPreservesOrdering() {
        RichText rich = RichText.empty();
        MarkdownInline.append(rich,
                "[A](https://a) - [B](https://b)",
                BASE);

        List<InlineTextRun> linkRuns = rich.runs().stream()
                .map(r -> (InlineTextRun) r)
                .filter(r -> r.linkOptions() != null)
                .toList();
        assertThat(linkRuns).hasSize(2);
        assertThat(linkRuns.get(0).text()).isEqualTo("A");
        assertThat(linkRuns.get(0).linkOptions().uri()).isEqualTo("https://a");
        assertThat(linkRuns.get(1).text()).isEqualTo("B");
        assertThat(linkRuns.get(1).linkOptions().uri()).isEqualTo("https://b");
    }

    @Test
    void appendLeavesBareBracketsAsLiteralText() {
        RichText rich = RichText.empty();
        MarkdownInline.append(rich, "[just brackets]", BASE);

        List<InlineRun> runs = rich.runs();
        // No link run — the entire string flows through the emphasis
        // pipeline as literal text.
        assertThat(runs).isNotEmpty();
        assertThat(runs).allSatisfy(run ->
                assertThat(((InlineTextRun) run).linkOptions()).isNull());
        String concatenated = runs.stream()
                .map(r -> ((InlineTextRun) r).text())
                .reduce("", String::concat);
        assertThat(concatenated).isEqualTo("[just brackets]");
    }

    @Test
    void appendKeepsPreExistingBoldItalicEmphasis() {
        RichText rich = RichText.empty();
        MarkdownInline.append(rich, "Plain **bold** and *italic*", BASE);

        List<InlineRun> runs = rich.runs();
        assertThat(runs).isNotEmpty();
        // No link runs in this input.
        assertThat(runs).allSatisfy(run ->
                assertThat(((InlineTextRun) run).linkOptions()).isNull());
    }

    @Test
    void appendOnNullOrEmptyTextIsANoOp() {
        RichText rich = RichText.empty();
        MarkdownInline.append(rich, null, BASE);
        MarkdownInline.append(rich, "", BASE);
        assertThat(rich.runs()).isEmpty();
    }

    @Test
    void appendTrimmedStripsLeadingAndTrailingWhitespaceBeforeParsing() {
        RichText richA = RichText.empty();
        MarkdownInline.appendTrimmed(richA, "   [hi](https://h)   ", BASE);

        RichText richB = RichText.empty();
        MarkdownInline.append(richB, "[hi](https://h)", BASE);

        // Both produce the same single link run with text "hi".
        assertThat(richA.runs()).hasSize(richB.runs().size());
        InlineTextRun a = (InlineTextRun) richA.runs().get(0);
        InlineTextRun b = (InlineTextRun) richB.runs().get(0);
        assertThat(a.text()).isEqualTo(b.text()).isEqualTo("hi");
        assertThat(a.linkOptions().uri()).isEqualTo(b.linkOptions().uri()).isEqualTo("https://h");
    }
}
