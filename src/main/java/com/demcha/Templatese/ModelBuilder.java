package com.demcha.Templatese;

import com.demcha.components.components_builders.*;
import com.demcha.components.content.link.LinkUrl;
import com.demcha.components.content.text.TextDecoration;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.Anchor;
import com.demcha.components.style.ComponentColor;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import lombok.AllArgsConstructor;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.*;
import java.util.List;

/**
 * Represent class for building CvTemplates
 */

@AllArgsConstructor
public class ModelBuilder {
    private EntityManager entityManager;

    public Entity name(String name) {
        Entity nameEntity = new TextBuilder(entityManager)
                .textWithAutoSize(name)
                .anchor(Anchor.topRight())
                .margin(Margin.bottom(5))
                .textStyle(TextStyle.builder()
                        .size(30)
                        .color(new Color(44, 62, 80))
                        .font(TextStyle.HELVETICA_BOLD)
                        .build())
                .build();
        return nameEntity;
    }

    public Entity info(String info) {
        Entity infoEntity = new TextBuilder(entityManager)
                .textWithAutoSize(info)
                .anchor(Anchor.center())
                .textStyle(TextStyle.builder()
                        .size(12)
                        .color(ComponentColor.MODULE_LINE_TEXT)
                        .font(TextStyle.HELVETICA)
                        .build())

                .build();
        return infoEntity;
    }

    public Entity infoPanel(java.util.List<Entity> entities) {
        var heigh = entities.stream()
                .map(entity -> entity.getComponent(ContentSize.class).orElseThrow())
                .map(size -> size.height())
                .max(Double::compareTo).get();
        ComponentColor componentColor = entities.get(0).getComponent(ComponentColor.class).orElse(new ComponentColor(ComponentColor.MODULE_LINE_TEXT));

        var links = new HContainerBuilder(entityManager,Align.right(5))
                .anchor(Anchor.topRight());


        for (int i = 0; i < entities.size(); i++) {
            if (i < entities.size() && i != 0) {
                var separator = new RectangleBuilder(entityManager)
                        .size(new ContentSize(1, heigh))
                        .fillColor(componentColor)
                        .margin(0, 2, 0, 2)
                        .anchor(Anchor.center())
                        .build();
                links.addChild(separator);
            }
            links.addChild(entities.get(i));
        }
        return links.build();
    }

    public <T extends LinkUrl> Entity link(T link, String displayText) {
        var linkEntity = new LinkBuilder(entityManager)
                .linkUrl(link)
                .anchor(Anchor.centerRight())
                .displayText(new DisplayUrlTextBuilder(entityManager)
                        .textWithAutoSize(displayText)
                        .textStyle(TextStyle.builder()
                                .size(12)
                                .color(ComponentColor.ROYAL_BLUE)
                                .font(TextStyle.HELVETICA_OBLIQUE)
                                .build())
                )
                .build();
        return linkEntity;
    }

    private Entity moduleName(String moduleName) {
        Entity moduleNameEntity = new TextBuilder(entityManager)
                .textWithAutoSize(moduleName)
                .entityName(moduleName)
                .anchor(Anchor.topLeft())
                .margin(new Margin(5, 5, 5, 10))
                .textStyle(TextStyle.builder()
                        .size(18.4)
                        .color(new Color(41, 128, 185))
                        .font(TextStyle.HELVETICA_BOLD)
                        .build())
                .build();
        return moduleNameEntity;
    }

    public ModuleBuilder moduleBuilder(String moduleName, PDPage page) {
        var moduleHeader = new ModuleBuilder(entityManager,Align.middle(5), page)
                .margin(Margin.of(20))
                .anchor(Anchor.topRight());
        if (moduleName != null) {
            moduleHeader.addChild(moduleName("Professional Summary"));
        }

        return moduleHeader;
    }

    public ModuleBuilder moduleBuilder(String moduleName, InnerBoxSize innerBoxSize) {
        var moduleHeader = new ModuleBuilder(entityManager,Align.middle(5), innerBoxSize)
                .margin(Margin.of(5))
                .anchor(Anchor.topLeft());
        if (moduleName != null) {
            moduleHeader.addChild(moduleName(moduleName));
        }

        return moduleHeader;
    }

    public ModuleBuilder moduleBuilder(String moduleName, InnerBoxSize innerBoxSize, List<String> modulePoints) {
        var moduleHeader = new ModuleBuilder(entityManager,Align.middle(5), innerBoxSize)
                .margin(Margin.of(5))
                .anchor(Anchor.topLeft());
        if (moduleName != null) {
            moduleHeader.addChild(moduleName(moduleName));
        }
        var vbox = new VContainerBuilder(entityManager,Align.middle(5))
                .size(new ContentSize(innerBoxSize.width(), 50));
        for (int i = 0; i < modulePoints.size(); i++) {
            vbox.addChild(
                    blockTextBuilder(modulePoints.get(i), innerBoxSize.width())
                            .anchor(Anchor.left())
                            .build()
            );
        }
        moduleHeader.addChild(vbox.build());

        return moduleHeader;
    }

    public ModuleBuilder moduleBuilder(PDPage page) {
        return moduleBuilder(null, page);
    }

    public ModuleBuilder moduleBuilder(InnerBoxSize innerBoxSize) {
        return moduleBuilder(null, innerBoxSize);
    }

    public Entity blockText(String text) {
        return blockText(text, 500);

    }

    public Entity blockText(String text, double width) {
        return blockTextBuilder(text, width)
                .anchor(Anchor.center())
                .build();

    }

    public Entity blockText(List<String> text, double width, String bulletOffset) {
        Padding padding = null;
        Margin margin = null;

        TextStyle stye = TextStyle.builder()
                .size(12)
                .color(ComponentColor.TITLE)
                .font(new PDType1Font(Standard14Fonts.FontName.HELVETICA))
                .decoration(TextDecoration.UNDERLINE)
                .build();

        var blockText = new BlockTextBuilder(entityManager,Align.left(5))
                .size(width, 2)
                .padding(0, 5, 0, 25)
                .text(text, stye, padding, margin, bulletOffset)
                .anchor(Anchor.center())
                .build();


        return blockText;

    }


    private BlockTextBuilder blockTextBuilder(String text, double width) {
        TextBuilder textBuilder = new TextBuilder(entityManager)
                .textWithAutoSize(text)
                .textStyle(TextStyle.builder()
                        .size(12)
                        .color(ComponentColor.TITLE)
                        .font(new PDType1Font(Standard14Fonts.FontName.HELVETICA))
                        .decoration(TextDecoration.UNDERLINE)
                        .build());


        var blockText = new BlockTextBuilder(entityManager,Align.left(5))
                .size(width, 2)
                .padding(0, 5, 0, 25)
                .text(textBuilder);

        return blockText;

    }
}


