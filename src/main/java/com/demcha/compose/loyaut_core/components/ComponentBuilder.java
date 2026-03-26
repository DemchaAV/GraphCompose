package com.demcha.compose.loyaut_core.components;

import com.demcha.compose.loyaut_core.components.components_builders.*;
import com.demcha.compose.loyaut_core.components.containers.abstract_builders.BuildEntity;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.core.EntityManager;

import java.util.ArrayList;
import java.util.List;

public class ComponentBuilder {
    private final List<BuildEntity> builders = new ArrayList<>();
    private EntityManager entityManager;

    private ComponentBuilder() {
    }

    public static ComponentBuilder builder(EntityManager entityManager) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.entityManager = entityManager;
        return builder;
    }

    public BlockTextBuilder blockText(Align align, TextStyle textStyle) {
        BlockTextBuilder blockTextBuilder = new BlockTextBuilder(this.entityManager, align, textStyle);
        builders.add(blockTextBuilder);
        return blockTextBuilder;
    }

    public BlockTextBuilder blockTextParagraph(Align align, TextStyle textStyle, String bulletOffset) {
        BlockTextBuilder blockTextBuilder = new BlockTextBuilder(this.entityManager, align, textStyle);
        blockTextBuilder.bulletOffset(bulletOffset)
                .strategy(BlockIndentStrategy.FIRST_LINE);
        builders.add(blockTextBuilder);
        return blockTextBuilder;
    }

    public ButtonBuilder button() {
        ButtonBuilder buttonBuilder = new ButtonBuilder(this.entityManager);
        builders.add(buttonBuilder);
        return buttonBuilder;
    }

    public DisplayUrlTextBuilder displayUrlText() {
        DisplayUrlTextBuilder displayUrlTextBuilder = new DisplayUrlTextBuilder(this.entityManager);
        builders.add(displayUrlTextBuilder);
        return displayUrlTextBuilder;
    }

    public LinkBuilder link() {
        LinkBuilder linkBuilder = new LinkBuilder(this.entityManager);
        builders.add(linkBuilder);
        return linkBuilder;
    }

    public ModuleBuilder moduleBuilder(Align align) {
        ModuleBuilder moduleBuilder = new ModuleBuilder(this.entityManager, align);
        builders.add(moduleBuilder);
        return moduleBuilder;
    }

    public RectangleBuilder rectangle() {
        RectangleBuilder rectangleBuilder = new RectangleBuilder(this.entityManager);
        builders.add(rectangleBuilder);
        return rectangleBuilder;
    }

    public ImageBuilder image() {
        ImageBuilder imageBuilder = new ImageBuilder(this.entityManager);
        builders.add(imageBuilder);
        return imageBuilder;
    }

    public TextBuilder text() {
        TextBuilder textBuilder = new TextBuilder(this.entityManager);
        builders.add(textBuilder);
        return textBuilder;
    }

    public RowBuilder row(Align align) {
        RowBuilder rowBuilder = new RowBuilder(this.entityManager, align);
        builders.add(rowBuilder);
        return rowBuilder;
    }

    public HContainerBuilder hContainer(Align align) {
        HContainerBuilder hContainerBuilder = new HContainerBuilder(this.entityManager, align);
        builders.add(hContainerBuilder);
        return hContainerBuilder;
    }

    public VContainerBuilder vContainer(Align align) {
        VContainerBuilder vContainerBuilder = new VContainerBuilder(this.entityManager, align);
        builders.add(vContainerBuilder);
        return vContainerBuilder;
    }

    public ElementBuilder element() {
        ElementBuilder elementBuilder = new ElementBuilder(this.entityManager);
        builders.add(elementBuilder);
        return elementBuilder;
    }

    public EntityManager entityManager() {
        return entityManager;
    }

    public void buildsComponents() {
        if (builders.isEmpty())
            return;
        builders.forEach(BuildEntity::build);
    }
}
