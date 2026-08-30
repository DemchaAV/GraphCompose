package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.PathBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.svg.SvgPath;
import com.demcha.compose.document.templates.core.text.TextOrnaments;
import com.demcha.compose.document.templates.data.proposal.ProposalBrand;

import java.util.List;

import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.ACCENT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.BODY;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.DOC_LABEL;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.DOC_LABEL_INSET;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.DOC_LABEL_RULE_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.DOC_LABEL_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GAP_HEADING;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.GAP_MASTHEAD_RULE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.HEADING_RULE_THICKNESS;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.HEADING_RULE_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.INK;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.LIST_LEADING;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.LOGO_GAP;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.LOGO_HEIGHT;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.LOGO_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.MASTHEAD_RULE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.PAGE_BACKGROUND;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.SECTION_HEADING;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.WORDMARK;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.WORDMARK_LINE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.WORDMARK_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.WORDMARK_SUB;
import static com.demcha.compose.document.templates.proposal.presets.EditorialStyles.WORDMARK_WIDTH;

/**
 * The building blocks the Editorial Proposal bands share: the masthead that
 * opens every page, the drawn logo mark, the serif section heading over its
 * accent rule, the bullet, and the stacked-line box both the wordmark and
 * the title are set in.
 */
final class EditorialWidgets {

    private EditorialWidgets() {
    }

    /**
     * The masthead: the drawn mark, the stacked wordmark, the tracked
     * document label over its accent rule, and the full-width rule beneath.
     *
     * <p>It is {@code keepWithNext}, which is what stops the page-two
     * masthead from being orphaned at the foot of page one — the preset
     * issues no explicit page break.</p>
     */
    static void renderMasthead(PageFlowBuilder page, ProposalBrand brand) {
        page.addSection("Masthead", masthead -> {
            masthead.spacing(0);
            masthead.keepWithNext();
            masthead.addRow("MastheadRow", row -> {
                row.verticalAlign(RowVerticalAlign.CENTER)
                        .gap(LOGO_GAP)
                        // Not auto(): a section's natural width is the width
                        // it is offered, so two auto columns would each claim
                        // the whole row. Both ends are measured strips.
                        .columns(DocumentRowColumn.fixed(LOGO_WIDTH),
                                DocumentRowColumn.fixed(WORDMARK_WIDTH),
                                DocumentRowColumn.weight(1),
                                DocumentRowColumn.fixed(DOC_LABEL_WIDTH));
                row.add(logoMark());
                // The wordmark stacks at 0.82 of its type size, tighter than
                // the 1.21 Lato asks for, which is what makes it read as one
                // mark rather than two words — and a section cannot carry the
                // negative spacing that pitch would need.
                row.add(stackedLines("Wordmark", WORDMARK_WIDTH,
                        WORDMARK_SIZE * WORDMARK_LINE, WORDMARK_SIZE,
                        List.of(brand.nameLine1(), brand.nameLine2()),
                        List.of(WORDMARK, WORDMARK_SUB)));
                // A zero-width spacer: the row distributes by columns, and the
                // weight(1) column above already holds the ends apart.
                row.addSpacer(0);
                row.addSection("DocumentLabel", label -> {
                    label.spacing(0);
                    label.padding(0f, (float) DOC_LABEL_INSET, 0f, 0f);
                    label.addParagraph(p -> p
                            .text(TextOrnaments.spacedUpper(brand.documentLabel()))
                            .textStyle(DOC_LABEL)
                            .align(TextAlign.RIGHT));
                    // addAligned, not addLine: a section stacks children at the
                    // content width with no child alignment, and this rule sits
                    // flush RIGHT under the label.
                    label.addAligned(HorizontalAlign.RIGHT, new LineBuilder()
                            .name("DocumentLabelRule")
                            .horizontal(DOC_LABEL_RULE_WIDTH)
                            .thickness(HEADING_RULE_THICKNESS)
                            .color(ACCENT)
                            .margin(DocumentInsets.top(5))
                            .build());
                });
            });
            masthead.addLine(line -> line
                    .fill()
                    .thickness(MASTHEAD_RULE)
                    .color(INK)
                    .margin(DocumentInsets.top(GAP_MASTHEAD_RULE)));
        });
    }

    /**
     * The brand mark, drawn as four stacked vector paths rather than loaded:
     * an accent stem, an accent diagonal, a page-coloured slash that cuts it,
     * and an ink stem.
     */
    private static DocumentNode logoMark() {
        return new ShapeContainerBuilder()
                .name("LogoMark")
                .rectangle(LOGO_WIDTH, LOGO_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(markPath("MarkStem", "M0,0 L14,0 L14,50 L0,50 Z", ACCENT),
                        0, 0, LayerAlign.TOP_LEFT, 0)
                .position(markPath("MarkDiagonal", "M13,0 L28,0 L48,50 L33,50 Z", ACCENT),
                        0, 0, LayerAlign.TOP_LEFT, 1)
                .position(markPath("MarkSlash", "M3,0 L8,0 L45,50 L40,50 Z", PAGE_BACKGROUND),
                        0, 0, LayerAlign.TOP_LEFT, 2)
                .position(markPath("MarkRightStem", "M40,0 L58,0 L58,50 L40,50 Z", INK),
                        0, 0, LayerAlign.TOP_LEFT, 3)
                .build();
    }

    private static DocumentNode markPath(String name, String d, DocumentColor fill) {
        return new PathBuilder()
                .name(name)
                .size(LOGO_WIDTH, LOGO_HEIGHT)
                .svg(SvgPath.parse(d, 0, 0, 58, 50))
                .fillColor(fill)
                .build();
    }

    /**
     * The heading every section opens with: the serif title over a short
     * accent rule. Unlike its teal sibling there is no badge, so the data's
     * section icon tokens are not read here.
     *
     * @param title the heading text
     * @return the heading node, kept with whatever follows it
     */
    static DocumentNode sectionHeading(String title) {
        SectionBuilder heading = new SectionBuilder();
        heading.name("SectionHeading");
        heading.spacing(0);
        heading.keepWithNext();
        heading.addParagraph(p -> p
                .name("SectionHeadingTitle")
                .text(title)
                .textStyle(SECTION_HEADING));
        heading.addLine(line -> line
                .name("SectionHeadingRule")
                .horizontal(HEADING_RULE_WIDTH)
                .thickness(HEADING_RULE_THICKNESS)
                .color(ACCENT)
                .margin(DocumentInsets.top(GAP_HEADING)));
        return heading.build();
    }

    /**
     * A bullet item: an inline accent dot, then the text, with a hanging
     * indent so wrapped lines clear the dot.
     */
    static void renderBullet(SectionBuilder host, String text) {
        host.addParagraph(p -> p
                .dot(3.6, ACCENT)
                .inlineText("   " + text, BODY)
                .bulletOffset("   ")
                .indentStrategy(DocumentTextIndent.FROM_SECOND_LINE)
                .lineSpacing(LIST_LEADING));
    }

    /**
     * Lines stacked on an authored pitch: each line is anchored inside one
     * container, because leading is additive and the engine rejects the
     * negative value a tighter-than-natural pitch would need.
     *
     * @param name           node name of the box and its lines
     * @param width          the box width
     * @param pitch          distance between line tops
     * @param lastLineHeight height reserved for the final line
     * @param texts          the lines, in order
     * @param styles         one style per line
     * @return the stacked box
     */
    static DocumentNode stackedLines(String name,
                                     double width,
                                     double pitch,
                                     double lastLineHeight,
                                     List<String> texts,
                                     List<DocumentTextStyle> styles) {
        ShapeContainerBuilder box = new ShapeContainerBuilder()
                .name(name)
                .rectangle(width, (texts.size() - 1) * pitch + lastLineHeight)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE);
        for (int i = 0; i < texts.size(); i++) {
            DocumentNode line = new ParagraphBuilder()
                    .name(name + "Line" + (i + 1))
                    .text(texts.get(i))
                    .textStyle(styles.get(i))
                    .build();
            box.position(line, 0, i * pitch, LayerAlign.TOP_LEFT);
        }
        return box.build();
    }
}
