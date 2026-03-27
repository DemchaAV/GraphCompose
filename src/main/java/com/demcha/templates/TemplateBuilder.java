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
import com.demcha.compose.layout_core.core.EntityManager;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.pdfbox.pdmodel.PDPage;

import java.util.List;
import java.util.Objects;

/**
 * Builder class responsible for constructing CV components.
 * It uses {@link CvTheme} to determine visual appearance (fonts, colors, sizes).
 */
@Accessors(fluent = true)
public class TemplateBuilder implements BuildEntity {
    private static final double DEFAULT_BLOCK_TEXT_WIDTH = 500;
    private static final double INFO_PANEL_SEPARATOR_WIDTH = 1;
    private static final double INFO_PANEL_SEPARATOR_FALLBACK_HEIGHT = 10;
    private static final double INFO_PANEL_SEPARATOR_MARGIN = 2;
    private static final double TITLE_MARGIN = 5;

    @Getter
    private final EntityManager entityManager;
    @Getter
    private final CvTheme theme;

    /**
     * Constructor with custom theme.
     */
    public TemplateBuilder(EntityManager entityManager, CvTheme theme) {
        this.entityManager = entityManager;
        this.theme = Objects.requireNonNullElse(theme, CvTheme.courier());
    }

    /**
     * Default constructor uses the default theme.
     */
    public TemplateBuilder(EntityManager entityManager) {
        this(entityManager, CvTheme.courier());
    }

    public static TemplateBuilder from(ComponentBuilder componentBuilder) {
        return new TemplateBuilder(componentBuilder.entityManager());
    }

    public static TemplateBuilder from(ComponentBuilder componentBuilder, CvTheme theme) {
        return new TemplateBuilder(componentBuilder.entityManager(), theme);
    }

    // ==========================================
    //              HEADER SECTIONS
    // ==========================================

    public Entity name(String name) {
        return new TextBuilder(entityManager)
                .textWithAutoSize(name)
                .anchor(Anchor.topRight()) // Anchor can be parameterized if needed
                .margin(Margin.bottom(5))
                .textStyle(theme.nameTextStyle()) // Using Theme
                .build();
    }

    public Entity info(String info) {
        return new TextBuilder(entityManager)
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
        var container = new HContainerBuilder(entityManager, Align.right(5))
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
        return new LinkBuilder(entityManager)
                .linkUrl(link)
                .anchor(Anchor.centerRight())
                .displayText(new DisplayUrlTextBuilder(entityManager)
                        .textWithAutoSize(displayText)
                        .textStyle(theme.linkTextStyle()) // Using Theme Link Style
                )
                .build();
    }

    // ==========================================
    //              MODULE / SECTIONS
    // ==========================================

    /**
     * Private helper to create consistently styled section titles.
     */
    private Entity createModuleTitle(String title) {
        return new TextBuilder(entityManager)
                .textWithAutoSize(title)
                .entityName("Title_" + title)
                .anchor(Anchor.topLeft())
                .margin(Margin.of(TITLE_MARGIN))
                .textStyle(theme.sectionHeaderTextStyle())
                .build();
    }

    public ModuleBuilder moduleBuilder(String moduleName, PDPage page) {
        var moduleHeader = new ModuleBuilder(entityManager, Align.middle(5), page)
                .margin(Margin.of(20))
                .anchor(Anchor.topRight());

        addModuleTitleIfPresent(moduleHeader, moduleName);
        return moduleHeader;
    }

    public ModuleBuilder moduleBuilder(String moduleName, Canvas canvas) {
        var moduleHeader = new ModuleBuilder(entityManager, Align.middle(theme().spacingModuleName()), canvas)
                .anchor(Anchor.topLeft());

        addModuleTitleIfPresent(moduleHeader, moduleName);
        return moduleHeader;
    }

    /**
     * Shortcut method to build a module with a title and a list of bullet points.
     */
    public ModuleBuilder moduleBuilder(String moduleName, Canvas canvas, List<String> modulePoints) {
        var moduleHeader = moduleBuilder(moduleName, canvas);

        var vbox = new VContainerBuilder(entityManager, Align.middle(5))
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
     * Creates a block of text (potentially multi-line or with bullets).
     */
    public Entity blockText(List<String> text, double width, String bulletOffset, BlockIndentStrategy strategy) {
        TextStyle style = theme.bodyTextStyle();
        return new BlockTextBuilder(entityManager, bodyAlign(), style)
                .size(width, 2)
                .strategy(strategy)
                .padding(0, 5, 0, 20)
                .bulletOffset(bulletOffset)
                .text(text, style, null, null)
                .anchor(Anchor.center())
                .build();
    }

    /**
     * Internal helper to create the text builder with the correct theme.
     */
    private BlockTextBuilder blockTextBuilder(String text, double width) {
        TextStyle bodyStyle = theme.bodyTextStyle();
        TextBuilder textBuilder = new TextBuilder(entityManager)
                .textWithAutoSize(text)
                .textStyle(bodyStyle);

        return new BlockTextBuilder(entityManager, bodyAlign(), bodyStyle)
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
        return new RectangleBuilder(entityManager)
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

    /**
     * Returns the {@link EntityManager} associated with this builder.
     *
     * @return The associated {@link EntityManager}.
     */
    @Override
    public EntityManager manager() {
        return entityManager;
    }

    /**
     * Returns the {@link Entity} object that is currently being built by this builder.
     *
     * @return The entity being built.
     */
    @Override
    public Entity entity() {
        return null;
    }
}

