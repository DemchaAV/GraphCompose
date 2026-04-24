package com.demcha.compose.document.templates.support.cv;

import com.demcha.compose.document.templates.support.common.*;

import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.data.cv.CvModule;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.engine.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.engine.components.style.Padding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shared scene composer for the standard CV template.
 *
 * @author Artem Demchyshyn
 */
public final class CvTemplateComposer {
    private static final Margin LEGACY_HEADER_RIGHT_MARGIN = new Margin(0, 10, 0, 0);

    private final CvTheme theme;
    private final TemplateLayoutPolicy layout;

    public CvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.layout = TemplateLayoutPolicy.standardCv(this.theme);
    }

    public void compose(TemplateComposeTarget target, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");
        target.startDocument("MainVBoxContainer", layout.rootSpacing());
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
                "ModuleHeaderName",
                Objects.requireNonNullElse(header.getName(), ""),
                theme.nameTextStyle(),
                TextAlign.RIGHT,
                1.0,
                Padding.zero(),
                new Margin(0, 10, Math.max(0.0, theme.spacing() - 3), 0)));

        String info = TemplateSceneSupport.joinNonBlank(" | ",
                header.getAddress(),
                header.getPhoneNumber());
        if (!info.isBlank()) {
            target.addParagraph(TemplateSceneSupport.paragraph(
                    "ModuleHeaderInfo",
                    info,
                    theme.smallBodyTextStyle(),
                    TextAlign.RIGHT,
                    1.0,
                    Padding.zero(),
                    LEGACY_HEADER_RIGHT_MARGIN));
        }

        TemplateParagraphSpec linkRow = TemplateHeaderContactSupport.linkRow(
                "ModuleHeaderLinks",
                header,
                theme,
                TextAlign.RIGHT,
                LEGACY_HEADER_RIGHT_MARGIN);
        if (linkRow != null) {
            target.addParagraph(linkRow);
        }
    }

    private void addModule(TemplateComposeTarget target, CvModule module) {
        if (module == null) {
            return;
        }
        TemplateModuleSpec spec = toTemplateModule(module);
        if (spec == null || spec.blocks().isEmpty()) {
            return;
        }
        target.addModule(spec);
    }

    private TemplateModuleSpec toTemplateModule(CvModule module) {
        String moduleName = module.name();
        List<TemplateModuleBlock> blocks = new ArrayList<>();
        for (int index = 0; index < module.bodyBlocks().size(); index++) {
            TemplateModuleBlock block = toTemplateBlock(module, module.bodyBlocks().get(index), index);
            if (block != null) {
                blocks.add(block);
            }
        }
        if (blocks.isEmpty()) {
            return null;
        }
        return new TemplateModuleSpec(
                moduleName,
                moduleTitle(moduleName, module.title()),
                blocks);
    }

    private TemplateModuleBlock toTemplateBlock(CvModule module, CvModule.BodyBlock block, int index) {
        return switch (block.kind()) {
            case PARAGRAPH -> TemplateModuleBlock.paragraph(TemplateSceneSupport.blockParagraph(
                    blockName(module, block, index),
                    block.text(),
                    theme.bodyTextStyle(),
                    TextAlign.LEFT,
                    layout.bodyLineSpacing(),
                    block.firstLineIndent(),
                    block.firstLineIndent().isEmpty() ? BlockIndentStrategy.NONE : BlockIndentStrategy.FIRST_LINE,
                    layout.bodyPadding(),
                    Margin.zero()));
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
        String continuationIndent = block.marker().isVisible()
                ? ""
                : (block.continuationIndent().isEmpty() ? layout.markerlessContinuationIndent() : block.continuationIndent());
        return TemplateModuleBlock.list(new TemplateListSpec(
                blockName(module, block, index),
                items,
                Objects.requireNonNullElse(block.marker(), ListMarker.bullet()),
                theme.bodyTextStyle(),
                TextAlign.LEFT,
                layout.bodyLineSpacing(),
                layout.bodyItemSpacing(),
                continuationIndent,
                block.normalizeMarkers(),
                layout.bodyPadding(),
                Margin.zero()));
    }

    private TemplateParagraphSpec moduleTitle(String moduleName, String title) {
        return TemplateSceneSupport.paragraph(
                moduleName + "Heading",
                Objects.requireNonNullElse(title, ""),
                theme.sectionHeaderTextStyle(),
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
}
