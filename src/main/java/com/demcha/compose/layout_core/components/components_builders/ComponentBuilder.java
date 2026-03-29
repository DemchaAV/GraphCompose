package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.BuildEntity;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.geometry.InnerBoxSize;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.core.EntityManager;
import org.apache.pdfbox.pdmodel.PDPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ComponentBuilder {
    private final List<BuildEntity> builders = new ArrayList<>();
    private final EntityManager entityManager;

    private ComponentBuilder(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    private <T extends BuildEntity> T register(T builder) {
        builders.add(builder);
        return builder;
    }

    public BlockTextBuilder blockText(Align align, TextStyle textStyle) {
        return register(new BlockTextBuilder(entityManager, align, textStyle));
    }

    public BlockTextBuilder blockTextParagraph(Align align, TextStyle textStyle, String bulletOffset) {
        BlockTextBuilder blockTextBuilder = register(new BlockTextBuilder(entityManager, align, textStyle));
        blockTextBuilder.bulletOffset(bulletOffset)
                .strategy(BlockIndentStrategy.FIRST_LINE);
        return blockTextBuilder;
    }

    public ButtonBuilder button() {
        return register(new ButtonBuilder(entityManager));
    }

    public CircleBuilder circle() {
        return register(new CircleBuilder(entityManager));
    }

    public DisplayUrlTextBuilder displayUrlText() {
        return register(new DisplayUrlTextBuilder(entityManager));
    }

    public LinkBuilder link() {
        return register(new LinkBuilder(entityManager));
    }

    public LineBuilder line() {
        return register(new LineBuilder(entityManager));
    }

    public ModuleBuilder moduleBuilder(Align align) {
        return register(new ModuleBuilder(entityManager, align));
    }

    public ModuleBuilder moduleBuilder(Align align, Canvas canvas) {
        return register(new ModuleBuilder(entityManager, align, canvas));
    }

    public ModuleBuilder moduleBuilder(Align align, ContentSize contentSize) {
        return register(new ModuleBuilder(entityManager, align, contentSize));
    }

    public ModuleBuilder moduleBuilder(Align align, InnerBoxSize innerBoxSize) {
        return register(new ModuleBuilder(entityManager, align, innerBoxSize));
    }

    public ModuleBuilder moduleBuilder(Align align, PDPage page) {
        return register(new ModuleBuilder(entityManager, align, page));
    }

    public RectangleBuilder rectangle() {
        return register(new RectangleBuilder(entityManager));
    }

    public ImageBuilder image() {
        return register(new ImageBuilder(entityManager));
    }

    public TextBuilder text() {
        return register(new TextBuilder(entityManager));
    }

    public RowBuilder row(Align align) {
        return register(new RowBuilder(entityManager, align));
    }

    public HContainerBuilder hContainer(Align align) {
        return register(new HContainerBuilder(entityManager, align));
    }

    public VContainerBuilder vContainer(Align align) {
        return register(new VContainerBuilder(entityManager, align));
    }

    public TableBuilder table() {
        return register(new TableBuilder(entityManager));
    }

    public ElementBuilder element() {
        return register(new ElementBuilder(entityManager));
    }

    public void buildsComponents() {
        if (builders.isEmpty())
            return;
        builders.stream()
                .filter(builder -> !builder.built())
                .forEach(BuildEntity::build);
    }
}
