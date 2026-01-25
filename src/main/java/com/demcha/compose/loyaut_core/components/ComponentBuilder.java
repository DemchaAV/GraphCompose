package com.demcha.compose.loyaut_core.components;

import com.demcha.compose.Templatese.CvTheme;
import com.demcha.compose.Templatese.TemplateBuilder;
import com.demcha.compose.loyaut_core.components.components_builders.*;
import com.demcha.compose.loyaut_core.components.content.text.TextStyle;
import com.demcha.compose.loyaut_core.components.layout.Align;
import com.demcha.compose.loyaut_core.core.EntityManager;

public class ComponentBuilder {
    private EntityManager entityManager;

    private ComponentBuilder() {
    }

    public static ComponentBuilder builder(EntityManager entityManager) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.entityManager = entityManager;
        return builder;
    }

    public BlockTextBuilder blockText(Align align, TextStyle textStyle) {
        return new BlockTextBuilder(this.entityManager, align, textStyle);
    }

    public ButtonBuilder button() {
        return new ButtonBuilder(this.entityManager);
    }

    public DisplayUrlTextBuilder displayUrlText() {
        return new DisplayUrlTextBuilder(this.entityManager);
    }

    public LinkBuilder link() {
        return new LinkBuilder(this.entityManager);
    }

    public ModuleBuilder moduleBuilder(Align align) {
        return new ModuleBuilder(this.entityManager, Align.middle(5));
    }

    public RectangleBuilder rectangle() {
        return new RectangleBuilder(this.entityManager);
    }

    public TextBuilder text() {
        return new TextBuilder(this.entityManager);
    }

    public RowBuilder row(Align align) {
        return new RowBuilder(this.entityManager, align);
    }

    public HContainerBuilder hContainer(Align align) {
        return new HContainerBuilder(this.entityManager, align);
    }

    public VContainerBuilder vContainer(Align align) {
        return new VContainerBuilder(this.entityManager, align);
    }

    public TemplateBuilder template(CvTheme theme) {
        return new TemplateBuilder(this.entityManager, theme);
    }

    public TemplateBuilder template() {
        return template(null);
    }

    public ElementBuilder element() {
        return new ElementBuilder(this.entityManager);
    }
}
