package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

/**
 * Page-flow section-header widget for presets whose title treatment
 * includes full-width rules outside the section body.
 *
 * <p>{@link SectionHeader} renders into an existing
 * {@code SectionBuilder}. This widget owns the surrounding
 * {@code PageFlowBuilder.addLine(...)} calls too, so presets such as
 * Blue Banner and Editorial Blue do not repeat the same top-rule,
 * title-section, bottom-rule plumbing.</p>
 */
public final class FlowSectionHeader {

    private FlowSectionHeader() {
    }

    /**
     * Renders a filled banner title with a rule above and below.
     * Visual signature of the Blue Banner preset.
     */
    public static void banner(PageFlowBuilder flow,
                              String name,
                              String title,
                              double ruleWidth,
                              CvTheme theme,
                              DocumentTextStyle titleStyle,
                              DocumentInsets topRuleMargin,
                              DocumentInsets bottomRuleMargin) {
        banner(flow, name, title, ruleWidth, theme, titleStyle,
                theme.palette().rule(), topRuleMargin, bottomRuleMargin);
    }

    /**
     * Renders a filled banner title with caller-controlled rule colour.
     */
    public static void banner(PageFlowBuilder flow,
                              String name,
                              String title,
                              double ruleWidth,
                              CvTheme theme,
                              DocumentTextStyle titleStyle,
                              DocumentColor ruleColor,
                              DocumentInsets topRuleMargin,
                              DocumentInsets bottomRuleMargin) {
        addRule(flow, name + "RuleTop", ruleWidth, ruleColor, theme,
                topRuleMargin);
        flow.addSection(name, host -> SectionHeader.fullWidthBanner(host,
                title, theme, titleStyle));
        addRule(flow, name + "RuleBottom", ruleWidth, ruleColor, theme,
                bottomRuleMargin);
    }

    /**
     * Renders a plain left-aligned title between horizontal rules.
     * Visual signature of the Editorial Blue preset.
     */
    public static void label(PageFlowBuilder flow,
                             String name,
                             String title,
                             double ruleWidth,
                             CvTheme theme,
                             DocumentTextStyle titleStyle,
                             DocumentInsets topRuleMargin,
                             DocumentInsets titlePadding,
                             DocumentInsets bottomRuleMargin,
                             boolean withTopRule) {
        label(flow, name, title, ruleWidth, theme, titleStyle,
                theme.palette().rule(), topRuleMargin, titlePadding,
                bottomRuleMargin, withTopRule);
    }

    /**
     * Renders a plain left-aligned title with caller-controlled rule
     * colour.
     */
    public static void label(PageFlowBuilder flow,
                             String name,
                             String title,
                             double ruleWidth,
                             CvTheme theme,
                             DocumentTextStyle titleStyle,
                             DocumentColor ruleColor,
                             DocumentInsets topRuleMargin,
                             DocumentInsets titlePadding,
                             DocumentInsets bottomRuleMargin,
                             boolean withTopRule) {
        if (withTopRule) {
            addRule(flow, name + "RuleTop", ruleWidth, ruleColor, theme,
                    topRuleMargin);
        }
        flow.addSection(name, section -> section
                .spacing(0)
                .padding(titlePadding)
                .addParagraph(paragraph -> paragraph
                        .text(title)
                        .textStyle(titleStyle)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero())));
        addRule(flow, name + "RuleBottom", ruleWidth, ruleColor, theme,
                bottomRuleMargin);
    }

    private static void addRule(PageFlowBuilder flow,
                                String name,
                                double width,
                                DocumentColor color,
                                CvTheme theme,
                                DocumentInsets margin) {
        flow.addLine(line -> line
                .name(name)
                .horizontal(width)
                .color(color)
                .thickness(theme.spacing().accentRuleWidth())
                .margin(margin));
    }
}
