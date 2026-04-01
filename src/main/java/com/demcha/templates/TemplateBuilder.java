package com.demcha.templates;

import com.demcha.compose.layout_core.components.components_builders.ComponentBuilder;
import com.demcha.compose.layout_core.components.components_builders.BlockIndentStrategy;
import com.demcha.compose.layout_core.components.components_builders.BlockTextBuilder;
import com.demcha.compose.layout_core.components.components_builders.DisplayUrlTextBuilder;
import com.demcha.compose.layout_core.components.components_builders.HContainerBuilder;
import com.demcha.compose.layout_core.components.components_builders.LinkBuilder;
import com.demcha.compose.layout_core.components.components_builders.ModuleBuilder;
import com.demcha.compose.layout_core.components.components_builders.RectangleBuilder;
import com.demcha.compose.layout_core.components.components_builders.TextBuilder;
import com.demcha.compose.layout_core.components.components_builders.VContainerBuilder;
import com.demcha.compose.layout_core.components.containers.abstract_builders.BuildEntity;
import com.demcha.compose.layout_core.components.content.link.LinkUrl;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.Anchor;
import com.demcha.compose.layout_core.components.style.ComponentColor;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.core.Canvas;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.pdfbox.pdmodel.PDPage;

import java.util.List;
import java.util.Objects;

/**
 * Higher-level builder layer for reusable CV-like document sections.
 * <p>
 * {@code TemplateBuilder} sits above the engine-level {@code ComponentBuilder}.
 * It packages common composition patterns such as names, info panels, modules,
 * and themed text blocks so application code can work with reusable document
 * structures instead of assembling every low-level entity manually.
 * </p>
 *
 * <p>The template layer still produces normal engine entities. Layout and
 * rendering are therefore handled by the same pipeline as manually built content.</p>
 */
@Accessors(fluent = true)
public class TemplateBuilder {
    private static final double DEFAULT_BLOCK_TEXT_WIDTH = 500;
    private static final double INFO_PANEL_SEPARATOR_WIDTH = 1;
    private static final double INFO_PANEL_SEPARATOR_FALLBACK_HEIGHT = 10;
    private static final double INFO_PANEL_SEPARATOR_MARGIN = 2;
    private static final double TITLE_MARGIN = 5;

    @Getter
    private final ComponentBuilder componentBuilder;
    @Getter
    private final CvTheme theme;

    /**
     * Creates a template builder bound to a component builder and theme.
     */
    private TemplateBuilder(ComponentBuilder componentBuilder, CvTheme theme) {
        this.componentBuilder = Objects.requireNonNull(componentBuilder, "componentBuilder");
        this.theme = Objects.requireNonNullElse(theme, CvTheme.courier());
    }

    public static TemplateBuilder from(ComponentBuilder componentBuilder) {
        return new TemplateBuilder(componentBuilder, CvTheme.courier());
    }

    public static TemplateBuilder from(ComponentBuilder componentBuilder, CvTheme theme) {
        return new TemplateBuilder(componentBuilder, theme);
    }

    // ==========================================
    //              HEADER SECTIONS
    // ==========================================

    public Entity name(String name) {
        return componentBuilder.text()
                .textWithAutoSize(name)
                .anchor(Anchor.topRight()) // Anchor can be parameterized if needed
                .margin(Margin.bottom(5))
                .textStyle(theme.nameTextStyle()) // Using Theme
                .build();
    }

    public Entity info(String info) {
        return componentBuilder.text()
                .textWithAutoSize(info)
                .anchor(Anchor.center())
                .textStyle(theme.smallBodyTextStyle()) // Using Theme
                .build();
    }

    public Entity infoPanel(List<Entity> entities) {
        return infoPanel(entities, null, null);
    }

    public Entity infoPanel(List<Entity> entities, Anchor anchorContainer, Anchor elements) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }

        double height = entities.stream()
                .map(entity -> entity.getComponent(ContentSize.class).orElse(new ContentSize(0, 10)))
                .map(ContentSize::height)
                .max(Double::compareTo)
                .orElse(INFO_PANEL_SEPARATOR_FALLBACK_HEIGHT);

        Anchor defaultAnchor = Anchor.topRight();
        var container = componentBuilder.hContainer(Align.right(5))
                .anchor(resolveAnchor(anchorContainer, defaultAnchor));

        for (int i = 0; i < entities.size(); i++) {
            if (i > 0) {
                container.addChild(createInfoSeparator(height, resolveAnchor(elements, defaultAnchor)));
            }
            container.addChild(entities.get(i));
        }
        return container.build();
    }

    public <T extends LinkUrl> Entity link(T link, String displayText) {
        return componentBuilder.link()
                .linkUrl(link)
                .anchor(Anchor.centerRight())
                .displayText(componentBuilder.displayUrlText()
                        .textWithAutoSize(displayText)
                        .textStyle(theme.linkTextStyle()) // Using Theme Link Style
                )
                .build();
    }

    // ==========================================
    //              MODULE / SECTIONS
    // ==========================================

    /**
     * Creates the canonical root page flow for template documents.
     *
     * <p>The page flow is an ordinary vertical container that owns page-level
     * semantic modules. Each nested module then resolves its width against this
     * root container rather than acting as the document root itself.</p>
     */
    public VContainerBuilder pageFlow(Canvas canvas) {
        return componentBuilder.vContainer(Align.middle(theme().spacingModuleName()))
                .size(canvas.innerWidth(), 0)
                .anchor(Anchor.topLeft());
    }

    /**
     * Creates a consistently themed section title entity.
     */
    private Entity createModuleTitle(String title) {
        return componentBuilder.text()
                .textWithAutoSize(title)
                .entityName("Title_" + title)
                .anchor(Anchor.topLeft())
                .margin(Margin.of(TITLE_MARGIN))
                .textStyle(theme.sectionHeaderTextStyle())
                .build();
    }

    public ModuleBuilder moduleBuilder(String moduleName, PDPage page) {
        var moduleHeader = componentBuilder.moduleBuilder(Align.middle(5), page)
                .margin(Margin.of(20))
                .anchor(Anchor.topRight());

        addModuleTitleIfPresent(moduleHeader, moduleName);
        return moduleHeader;
    }

    public ModuleBuilder moduleBuilder(String moduleName, Canvas canvas) {
        var moduleHeader = componentBuilder.moduleBuilder(Align.middle(theme().spacingModuleName()), canvas)
                .anchor(Anchor.topLeft());

        addModuleTitleIfPresent(moduleHeader, moduleName);
        return moduleHeader;
    }

    /**
     * Convenience overload that creates a module and fills it with block-text bullet items.
     */
    public ModuleBuilder moduleBuilder(String moduleName, Canvas canvas, List<String> modulePoints) {
        var moduleHeader = moduleBuilder(moduleName, canvas);

        var vbox = componentBuilder.vContainer(Align.middle(5))
                .size(new ContentSize(canvas.innerWidth(), 50));

        for (String point : modulePoints) {
            vbox.addChild(blockTextBuilder(point, canvas.innerWidth())
                    .anchor(Anchor.left())
                    .build());
        }
        moduleHeader.addChild(vbox.build());

        return moduleHeader;
    }

    // Overloads for convenience
    public ModuleBuilder moduleBuilder(PDPage page) {
        return moduleBuilder(null, page);
    }

    public ModuleBuilder moduleBuilder(Canvas canvas) {
        return moduleBuilder(null, canvas);
    }

    // ==========================================
    //              TEXT BLOCKS
    // ==========================================

    public Entity blockText(String text) {
        return blockText(text, DEFAULT_BLOCK_TEXT_WIDTH);
    }

    public Entity blockText(String text, double width) {
        return blockTextBuilder(text, width)
                .strategy(BlockIndentStrategy.FIRST_LINE)
                .anchor(Anchor.center())
                .build();
    }

    /**
     * Creates a themed block-text entity, optionally with bullet-aware indentation.
     */
    public Entity blockText(List<String> text, double width, String bulletOffset, BlockIndentStrategy strategy) {
        TextStyle style = theme.bodyTextStyle();
        return componentBuilder.blockText(bodyAlign(), style)
                .size(width, 2)
                .strategy(strategy)
                .padding(0, 5, 0, 20)
                .bulletOffset(bulletOffset)
                .text(text, style, null, null)
                .anchor(Anchor.left())
                .build();
    }

    /**
     * Creates the underlying block-text builder configured with the current theme.
     */
    private BlockTextBuilder blockTextBuilder(String text, double width) {
        TextStyle bodyStyle = theme.bodyTextStyle();
        TextBuilder textBuilder = componentBuilder.text()
                .textWithAutoSize(text)
                .textStyle(bodyStyle);

        return componentBuilder.blockText(bodyAlign(), bodyStyle)
                .size(width, 2)
                .padding(0, 5, 0, 25)
                .text(textBuilder);
    }

    private Align bodyAlign() {
        return Align.left(theme.spacing());
    }

    private Anchor resolveAnchor(Anchor anchor, Anchor fallback) {
        return anchor != null ? anchor : fallback;
    }

    private Entity createInfoSeparator(double height, Anchor anchor) {
        return componentBuilder.rectangle()
                .size(new ContentSize(INFO_PANEL_SEPARATOR_WIDTH, height))
                .fillColor(new ComponentColor(theme.bodyColor()))
                .margin(0, INFO_PANEL_SEPARATOR_MARGIN, 0, INFO_PANEL_SEPARATOR_MARGIN)
                .anchor(anchor)
                .build();
    }

    private void addModuleTitleIfPresent(ModuleBuilder moduleBuilder, String moduleName) {
        if (moduleName != null) {
            moduleBuilder.addChild(createModuleTitle(moduleName));
        }
    }
}

