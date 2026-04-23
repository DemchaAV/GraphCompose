package com.demcha.compose.document.templates.support.cv;

import com.demcha.compose.document.templates.support.common.*;

import com.demcha.compose.document.model.node.ListMarker;
import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.data.cv.CvModule;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.content.text.TextDecoration;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Shared scene composer for the executive slate CV template.
 *
 * <p>The design keeps the same compose-first module model as the standard CV
 * template, but applies a quieter business rhythm: one compact header link
 * row, consistent module gaps, and aligned body blocks.</p>
 *
 * @author Artem Demchyshyn
 */
public final class ExecutiveSlateCvTemplateComposer {
    private static final Margin HEADER_LINK_MARGIN = Margin.top(1);

    private final CvTheme theme;
    private final TemplateLayoutPolicy layout;

    /**
     * Creates a composer with the supplied visual theme.
     *
     * @param theme visual theme
     */
    public ExecutiveSlateCvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.layout = TemplateLayoutPolicy.executiveCv();
    }

    /**
     * Composes a CV from the public compose-first document spec.
     *
     * @param target active template target
     * @param documentSpec header plus ordered modules
     */
    public void compose(TemplateComposeTarget target, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");
        target.startDocument("ExecutiveSlateRoot", layout.rootSpacing());
        addHeader(target, spec.header());
        for (CvModule module : spec.modules()) {
            addModule(target, module);
        }
        target.finishDocument();
    }

    private void addHeader(TemplateComposeTarget target, Header header) {
        if (header == null) {
            return;
        }

        target.addParagraph(TemplateSceneSupport.paragraph(
                "ExecutiveSlateName",
                Objects.requireNonNullElse(header.getName(), "").toUpperCase(Locale.ROOT),
                nameStyle(),
                TextAlign.LEFT,
                1.0,
                Padding.zero(),
                Margin.zero()));

        String info = TemplateSceneSupport.joinNonBlank(" | ",
                header.getAddress(),
                header.getPhoneNumber());
        if (!info.isBlank()) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "ExecutiveSlateInfo",
                    info,
                    metaStyle(),
                    TextAlign.LEFT,
                    1.0,
                    Padding.zero(),
                    Margin.top(2)));
        }

        TemplateParagraphSpec linkRow = TemplateHeaderContactSupport.linkRow(
                "ExecutiveSlateLinks",
                header,
                theme,
                TextAlign.LEFT,
                HEADER_LINK_MARGIN);
        if (linkRow != null) {
            target.addParagraph(linkRow);
        }

        target.addDivider(TemplateSceneSupport.divider(
                "ExecutiveSlateHeaderRule",
                target.pageWidth(),
                1.1,
                mutedRuleColor(),
                Margin.top(5)));
    }

    private void addModule(TemplateComposeTarget target, CvModule module) {
        if (module == null || module.bodyBlocks().isEmpty()) {
            return;
        }
        target.addParagraph(moduleTitle(module.name(), module.title()));
        for (int index = 0; index < module.bodyBlocks().size(); index++) {
            TemplateModuleBlock block = toTemplateBlock(module, module.bodyBlocks().get(index), index);
            if (block != null) {
                block.render(target);
            }
        }
    }

    private TemplateModuleBlock toTemplateBlock(CvModule module, CvModule.BodyBlock block, int index) {
        return switch (block.kind()) {
            case PARAGRAPH -> TemplateModuleBlock.paragraph(TemplateSceneSupport.blockParagraph(
                    blockName(module, block, index),
                    block.text(),
                    bodyStyle(),
                    TextAlign.LEFT,
                    layout.bodyLineSpacing(),
                    block.firstLineIndent(),
                    block.firstLineIndent().isEmpty() ? BlockIndentStrategy.NONE : BlockIndentStrategy.FIRST_LINE,
                    layout.bodyPadding(),
                    layout.blockMargin()));
            case LIST -> listBlock(module, block, index);
            case TABLE -> TemplateModuleBlock.table(Objects.requireNonNull(block.table(), "table"));
            case DIVIDER -> TemplateModuleBlock.divider(Objects.requireNonNull(block.divider(), "divider"));
            case PAGE_BREAK -> TemplateModuleBlock.pageBreak(
                    block.pageBreakName().isBlank() ? blockName(module, block, index) : block.pageBreakName());
            case CUSTOM -> TemplateModuleBlock.custom(Objects.requireNonNull(block.customRenderer(), "customRenderer"));
        };
    }

    private TemplateModuleBlock listBlock(CvModule module, CvModule.BodyBlock block, int index) {
        List<String> items = TemplateSceneSupport.sanitizeLines(block.items());
        if (items.isEmpty()) {
            return null;
        }
        ListMarker marker = Objects.requireNonNullElse(block.marker(), ListMarker.bullet());
        String continuationIndent = marker.isVisible()
                ? ""
                : (block.continuationIndent().isEmpty() ? layout.markerlessContinuationIndent() : block.continuationIndent());
        return TemplateModuleBlock.list(new TemplateListSpec(
                blockName(module, block, index),
                items,
                marker,
                bodyStyle(),
                TextAlign.LEFT,
                layout.bodyLineSpacing(),
                layout.bodyItemSpacing(),
                continuationIndent,
                block.normalizeMarkers(),
                layout.bodyPadding(),
                layout.blockMargin()));
    }

    private TemplateParagraphSpec moduleTitle(String moduleName, String title) {
        return TemplateSceneSupport.paragraph(
                moduleName + "Heading",
                Objects.requireNonNullElse(title, "").toUpperCase(Locale.ROOT),
                sectionTitleStyle(),
                TextAlign.LEFT,
                1.0,
                Padding.zero(),
                layout.sectionMargin());
    }

    private String blockName(CvModule module, CvModule.BodyBlock block, int index) {
        if (block.name() != null && !block.name().isBlank()) {
            return block.name();
        }
        return index == 0 ? module.name() + "Body" : module.name() + "Body_" + index;
    }

    private TextStyle nameStyle() {
        return TextStyle.builder()
                .fontName(theme.headerFont())
                .size(theme.nameFontSize())
                .decoration(TextDecoration.BOLD)
                .color(primaryTextColor())
                .build();
    }

    private TextStyle sectionTitleStyle() {
        return TextStyle.builder()
                .fontName(theme.headerFont())
                .size(theme.headerFontSize())
                .decoration(TextDecoration.BOLD)
                .color(accentColor())
                .build();
    }

    private TextStyle bodyStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(theme.bodyFontSize())
                .decoration(TextDecoration.DEFAULT)
                .color(bodyTextColor())
                .build();
    }

    private TextStyle metaStyle() {
        return TextStyle.builder()
                .fontName(theme.bodyFont())
                .size(theme.bodyFontSize() - 0.4)
                .decoration(TextDecoration.DEFAULT)
                .color(bodyTextColor())
                .build();
    }

    private Color primaryTextColor() {
        return theme.primaryColor();
    }

    private Color bodyTextColor() {
        return theme.bodyColor();
    }

    private Color accentColor() {
        return theme.accentColor();
    }

    private Color mutedRuleColor() {
        return new Color(193, 201, 211);
    }
}
