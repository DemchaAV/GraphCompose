package com.demcha.compose.document.dsl;

import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.InlineTextRun;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.SectionNode;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentTextDecoration;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RichTextTest {

    private static final double EPS = 1e-6;
    private static final DocumentColor ACCENT = DocumentColor.of(new Color(40, 90, 180));
    private static final DocumentColor RED = DocumentColor.of(Color.RED);

    @Test
    void emptyBuilderHasNoRuns() {
        assertThat(RichText.empty().runs()).isEmpty();
        assertThat(RichText.empty().isEmpty()).isTrue();
        assertThat(RichText.empty().size()).isEqualTo(0);
    }

    @Test
    void textFactorySeedsBuilderWithPlainRun() {
        List<InlineTextRun> runs = RichText.text("Hello").runs();
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).text()).isEqualTo("Hello");
        assertThat(runs.get(0).textStyle()).isNull();
        assertThat(runs.get(0).linkOptions()).isNull();
    }

    @Test
    void labelValuePatternProducesThreeRunsInSourceOrder() {
        List<InlineTextRun> runs = RichText.text("Status: ")
                .bold("Pending")
                .plain(" — review needed")
                .runs();

        assertThat(runs).hasSize(3);

        assertThat(runs.get(0).text()).isEqualTo("Status: ");
        assertThat(runs.get(0).textStyle()).isNull();

        assertThat(runs.get(1).text()).isEqualTo("Pending");
        assertThat(runs.get(1).textStyle()).isNotNull();
        assertThat(runs.get(1).textStyle().decoration()).isEqualTo(DocumentTextDecoration.BOLD);

        assertThat(runs.get(2).text()).isEqualTo(" — review needed");
        assertThat(runs.get(2).textStyle()).isNull();
    }

    @Test
    void colorRunCarriesColorButLeavesDecorationDefault() {
        List<InlineTextRun> runs = RichText.empty().color("Highlighted", RED).runs();
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).textStyle().color()).isEqualTo(RED);
        assertThat(runs.get(0).textStyle().decoration()).isEqualTo(DocumentTextDecoration.DEFAULT);
    }

    @Test
    void accentRunCarriesBothBoldAndColor() {
        List<InlineTextRun> runs = RichText.empty().accent("Important", ACCENT).runs();
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).textStyle().color()).isEqualTo(ACCENT);
        assertThat(runs.get(0).textStyle().decoration()).isEqualTo(DocumentTextDecoration.BOLD);
    }

    @Test
    void italicAndBoldItalicAndUnderlineAndStrikethroughEachUseCorrectDecoration() {
        assertThat(RichText.empty().italic("x").runs().get(0).textStyle().decoration())
                .isEqualTo(DocumentTextDecoration.ITALIC);
        assertThat(RichText.empty().boldItalic("x").runs().get(0).textStyle().decoration())
                .isEqualTo(DocumentTextDecoration.BOLD_ITALIC);
        assertThat(RichText.empty().underline("x").runs().get(0).textStyle().decoration())
                .isEqualTo(DocumentTextDecoration.UNDERLINE);
        assertThat(RichText.empty().strikethrough("x").runs().get(0).textStyle().decoration())
                .isEqualTo(DocumentTextDecoration.STRIKETHROUGH);
    }

    @Test
    void sizeRunOverridesFontSize() {
        InlineTextRun run = RichText.empty().size("HEADLINE", 32.0).runs().get(0);
        assertThat(run.textStyle().size()).isEqualTo(32.0, within(EPS));
    }

    @Test
    void linkRunCarriesLinkOptionsWithoutStyle() {
        InlineTextRun run = RichText.empty().link("Click", "https://example.com").runs().get(0);
        assertThat(run.linkOptions()).isNotNull();
        assertThat(run.linkOptions().uri()).isEqualTo("https://example.com");
        assertThat(run.textStyle()).isNull();
    }

    @Test
    void appendMergesAnotherRichTextInOrder() {
        RichText prefix = RichText.text("Status: ").bold("Pending");
        RichText suffix = RichText.empty().plain(" — ").italic("retry tomorrow");

        List<InlineTextRun> runs = prefix.append(suffix).runs();

        assertThat(runs).hasSize(4);
        assertThat(runs.get(0).text()).isEqualTo("Status: ");
        assertThat(runs.get(1).text()).isEqualTo("Pending");
        assertThat(runs.get(2).text()).isEqualTo(" — ");
        assertThat(runs.get(3).text()).isEqualTo("retry tomorrow");
    }

    @Test
    void nullTextNormalizesToEmptyString() {
        InlineTextRun run = RichText.empty().plain(null).runs().get(0);
        assertThat(run.text()).isEqualTo("");
    }

    @Test
    void paragraphBuilderRichTakesRichTextDirectly() {
        ParagraphNode paragraph = new ParagraphBuilder()
                .name("StatusLine")
                .rich(RichText.text("Status: ").bold("Pending"))
                .build();

        assertThat(paragraph.inlineTextRuns()).hasSize(2);
        assertThat(paragraph.inlineTextRuns().get(0).text()).isEqualTo("Status: ");
        assertThat(paragraph.inlineTextRuns().get(0).textStyle()).isNull();
        assertThat(paragraph.inlineTextRuns().get(1).text()).isEqualTo("Pending");
        assertThat(paragraph.inlineTextRuns().get(1).textStyle().decoration())
                .isEqualTo(DocumentTextDecoration.BOLD);
    }

    @Test
    void paragraphBuilderRichConsumerVariantConfiguresFreshBuilder() {
        ParagraphNode paragraph = new ParagraphBuilder()
                .rich(t -> t.plain("Total: ").accent("$200.00", ACCENT))
                .build();

        assertThat(paragraph.inlineTextRuns()).hasSize(2);
        assertThat(paragraph.inlineTextRuns().get(1).text()).isEqualTo("$200.00");
        assertThat(paragraph.inlineTextRuns().get(1).textStyle().color()).isEqualTo(ACCENT);
    }

    @Test
    void sectionBuilderAddRichConsumerProducesSingleParagraphChild() {
        SectionNode section = new SectionBuilder()
                .name("Body")
                .addRich(t -> t.plain("Status: ").bold("Pending"))
                .build();

        assertThat(section.children()).hasSize(1);
        assertThat(section.children().get(0)).isInstanceOf(ParagraphNode.class);

        ParagraphNode paragraph = (ParagraphNode) section.children().get(0);
        assertThat(paragraph.inlineTextRuns()).hasSize(2);
        assertThat(paragraph.inlineTextRuns().get(1).textStyle().decoration())
                .isEqualTo(DocumentTextDecoration.BOLD);
    }

    @Test
    void sectionBuilderAddRichWithExistingBuilderWorksToo() {
        RichText rich = RichText.text("Note: ").italic("informational");

        SectionNode section = new SectionBuilder()
                .addRich(rich)
                .build();

        ParagraphNode paragraph = (ParagraphNode) section.children().get(0);
        assertThat(paragraph.inlineTextRuns()).hasSize(2);
        assertThat(paragraph.inlineTextRuns().get(1).textStyle().decoration())
                .isEqualTo(DocumentTextDecoration.ITALIC);
    }

    @Test
    void linkRunWithExplicitOptionsPreservesAllFields() {
        DocumentLinkOptions options = new DocumentLinkOptions("https://demcha.io");
        InlineTextRun run = RichText.empty().link("Author", options).runs().get(0);
        assertThat(run.linkOptions()).isSameAs(options);
        assertThat(run.text()).isEqualTo("Author");
    }
}
