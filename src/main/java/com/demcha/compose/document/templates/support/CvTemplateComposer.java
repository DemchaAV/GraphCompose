package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.model.node.ListMarker;
import com.demcha.compose.document.model.node.TextAlign;
import com.demcha.compose.document.templates.data.CvDocumentSpec;
import com.demcha.compose.document.templates.data.CvModule;
import com.demcha.compose.document.templates.data.Header;
import com.demcha.compose.document.templates.data.MainPageCV;
import com.demcha.compose.document.templates.data.MainPageCvDTO;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shared scene composer for the standard CV template.
 *
 * @author Artem Demchyshyn
 */
public final class CvTemplateComposer {
    private static final Padding LEGACY_BLOCK_PADDING = new Padding(0, 5, 0, 20);
    private static final Margin LEGACY_HEADER_RIGHT_MARGIN = new Margin(0, 10, 0, 0);
    private static final String LEGACY_CONTINUATION_INDENT = "  ";

    private final CvTheme theme;

    public CvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public void compose(TemplateComposeTarget target, MainPageCV originalCv, MainPageCvDTO rewrittenCv) {
        compose(target, CvDocumentSpec.from(originalCv, rewrittenCv));
    }

    public void compose(TemplateComposeTarget target, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");
        target.startDocument("MainVBoxContainer", theme.spacingModuleName());
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

        for (TemplateParagraphSpec linkParagraph : TemplateHeaderContactSupport.linkParagraphs(
                "ModuleHeaderLinks",
                header,
                theme,
                LEGACY_HEADER_RIGHT_MARGIN)) {
            target.addParagraph(linkParagraph);
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
                    theme.spacing(),
                    block.firstLineIndent(),
                    block.firstLineIndent().isEmpty() ? BlockIndentStrategy.NONE : BlockIndentStrategy.FIRST_LINE,
                    LEGACY_BLOCK_PADDING,
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
                : (block.continuationIndent().isEmpty() ? LEGACY_CONTINUATION_INDENT : block.continuationIndent());
        return TemplateModuleBlock.list(new TemplateListSpec(
                blockName(module, block, index),
                items,
                Objects.requireNonNullElse(block.marker(), ListMarker.bullet()),
                theme.bodyTextStyle(),
                TextAlign.LEFT,
                theme.spacing(),
                theme.spacing(),
                continuationIndent,
                block.normalizeMarkers(),
                LEGACY_BLOCK_PADDING,
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
                Margin.of(5));
    }

    private String blockName(CvModule module, CvModule.BodyBlock block, int index) {
        if (block.name() != null && !block.name().isBlank()) {
            return block.name();
        }
        return index == 0 ? module.name() + "Body" : module.name() + "Body_" + index;
    }
}
