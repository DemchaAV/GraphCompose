package com.demcha.Templatese;

import com.demcha.loyaut_core.components.components_builders.*;
import com.demcha.loyaut_core.components.content.link.LinkUrl;
import com.demcha.loyaut_core.components.content.text.TextStyle;
import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.components.geometry.ContentSize;
import com.demcha.loyaut_core.components.layout.Align;
import com.demcha.loyaut_core.components.layout.Anchor;
import com.demcha.loyaut_core.components.style.ComponentColor;
import com.demcha.loyaut_core.components.style.Margin;
import com.demcha.loyaut_core.core.EntityManager;
import org.apache.pdfbox.pdmodel.PDPage;

import java.util.List;

/**
 * Builder class responsible for constructing CV components.
 * It uses {@link CvTheme} to determine visual appearance (fonts, colors, sizes).
 */
public class ModelBuilder {
    private final EntityManager entityManager;
    private final CvTheme theme;

    /**
     * Constructor with custom theme.
     */
    public ModelBuilder(EntityManager entityManager, CvTheme theme) {
        this.entityManager = entityManager;
        this.theme = theme;
    }

    /**
     * Default constructor uses the default theme.
     */
    public ModelBuilder(EntityManager entityManager) {
        this(entityManager, CvTheme.defaultTheme());
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
        if (entities == null || entities.isEmpty()) return null;

        // Calculate max height for separators based on content
        double height = entities.stream()
                .map(entity -> entity.getComponent(ContentSize.class).orElse(new ContentSize(0, 10)))
                .map(ContentSize::height)
                .max(Double::compareTo)
                .orElse(10.0);

        // Use body color for separators to maintain consistency
        ComponentColor separatorColor = new ComponentColor(theme.bodyColor());

        var defaultAnchor = Anchor.topRight();
        var container = new HContainerBuilder(entityManager, Align.right(5))
                .anchor(anchorContainer == null ? defaultAnchor : anchorContainer);


        for (int i = 0; i < entities.size(); i++) {
            // Add separator before element (except the first one)
            if (i > 0) {
                var separator = new RectangleBuilder(entityManager)
                        .size(new ContentSize(1, height))
                        .fillColor(separatorColor)
                        .margin(0, 2, 0, 2) // Small margins around pipe
                        .anchor(elements == null ? defaultAnchor : elements)
                        .build();
                container.addChild(separator);
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
                .margin(new Margin(5, 5, 5, 10))
                .textStyle(theme.sectionHeaderTextStyle()) // Using Theme Header Style
                .build();
    }

    public ModuleBuilder moduleBuilder(String moduleName, PDPage page) {
        var moduleHeader = new ModuleBuilder(entityManager, Align.middle(5), page)
                .margin(Margin.of(20))
                .anchor(Anchor.topRight());

        if (moduleName != null) {
            moduleHeader.addChild(createModuleTitle(moduleName));
        }
        return moduleHeader;
    }

    public ModuleBuilder moduleBuilder(String moduleName, Canvas canvas) {
        var moduleHeader = new ModuleBuilder(entityManager, Align.middle(5), canvas)

                .margin(Margin.of(5))
                .anchor(Anchor.topLeft());

        if (moduleName != null) {
            moduleHeader.addChild(createModuleTitle(moduleName));
        }
        return moduleHeader;
    }

    /**
     * Shortcut method to build a module with a title and a list of bullet points.
     */
    public ModuleBuilder moduleBuilder(String moduleName, Canvas canvas, List<String> modulePoints) {
        var moduleHeader = moduleBuilder(moduleName, canvas);

        // Create a container for the list items
        var vbox = new VContainerBuilder(entityManager, Align.middle(5))
                .size(new ContentSize(canvas.innerWidth(), 50)); // Height adapts automatically usually

        for (String point : modulePoints) {
            vbox.addChild(
                    blockTextBuilder(point, canvas.innerWidth())
                            .anchor(Anchor.left())
                            .build()
            );
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
        return blockText(text, 500);
    }

    public Entity blockText(String text, double width) {
        return blockTextBuilder(text, width)
                .anchor(Anchor.center())
                .build();
    }

    /**
     * Creates a block of text (potentially multi-line or with bullets).
     */
    public Entity blockText(List<String> text, double width, String bulletOffset) {
        // We use the standard body style for lists
        TextStyle style = theme.bodyTextStyle();


        return new BlockTextBuilder(entityManager, Align.left(theme.spacing()))
                .size(width, 2)
                .padding(0, 5, 0, 25)
                .text(text, style, null, null, bulletOffset)
                .anchor(Anchor.center())
                .build();
    }

    /**
     * Internal helper to create the text builder with the correct theme.
     */
    private BlockTextBuilder blockTextBuilder(String text, double width) {
        TextBuilder textBuilder = new TextBuilder(entityManager)
                .textWithAutoSize(text)
                .textStyle(theme.bodyTextStyle()); // Using Theme Body Style

        return new BlockTextBuilder(entityManager, Align.left(theme.spacing()))
                .size(width, 2)
                .padding(0, 5, 0, 25)
                .text(textBuilder);
    }
}