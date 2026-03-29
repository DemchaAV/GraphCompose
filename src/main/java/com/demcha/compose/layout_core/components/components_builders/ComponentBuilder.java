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

/**
 * Root builder facade for creating document entities.
 * <p>
 * {@code ComponentBuilder} is the public entry to the engine-level builder
 * layer. Each factory method returns a concrete builder for one entity family,
 * such as text, containers, images, lines, or tables. The concrete builder
 * attaches components to a new {@code Entity}; calling {@code build()} then
 * registers that entity in the shared {@code EntityManager}.
 * </p>
 *
 * <p>Use this type when you want direct control over the engine rather than the
 * higher-level template layer.</p>
 */
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

    /**
     * Creates a builder for multi-line block text with wrapping and optional
     * markdown-aware tokenization.
     *
     * <p>The resulting entity is a breakable text leaf whose content is later
     * processed by layout and pagination systems.</p>
     */
    public BlockTextBuilder blockText(Align align, TextStyle textStyle) {
        return register(new BlockTextBuilder(entityManager, align, textStyle));
    }

    /**
     * Creates a block-text builder preconfigured for paragraph-like indentation.
     *
     * <p>This convenience method applies a bullet offset and first-line indent
     * strategy before returning the builder.</p>
     */
    public BlockTextBuilder blockTextParagraph(Align align, TextStyle textStyle, String bulletOffset) {
        BlockTextBuilder blockTextBuilder = register(new BlockTextBuilder(entityManager, align, textStyle));
        blockTextBuilder.bulletOffset(bulletOffset)
                .strategy(BlockIndentStrategy.FIRST_LINE);
        return blockTextBuilder;
    }

    /**
     * Creates a builder for a button-like shape entity.
     */
    public ButtonBuilder button() {
        return register(new ButtonBuilder(entityManager));
    }

    /**
     * Creates a builder for a circle leaf entity.
     */
    public CircleBuilder circle() {
        return register(new CircleBuilder(entityManager));
    }

    /**
     * Creates a text builder specialized for user-facing URL display text.
     */
    public DisplayUrlTextBuilder displayUrlText() {
        return register(new DisplayUrlTextBuilder(entityManager));
    }

    /**
     * Creates a builder for a clickable link entity.
     */
    public LinkBuilder link() {
        return register(new LinkBuilder(entityManager));
    }

    /**
     * Creates a builder for a fixed-size line primitive.
     */
    public LineBuilder line() {
        return register(new LineBuilder(entityManager));
    }

    /**
     * Creates a module/container builder aligned within its parent according to
     * the supplied {@link Align}.
     */
    public ModuleBuilder moduleBuilder(Align align) {
        return register(new ModuleBuilder(entityManager, align));
    }

    /**
     * Creates a module/container builder sized against a canvas-based reference area.
     */
    public ModuleBuilder moduleBuilder(Align align, Canvas canvas) {
        return register(new ModuleBuilder(entityManager, align, canvas));
    }

    /**
     * Creates a module/container builder sized against an explicit content box.
     */
    public ModuleBuilder moduleBuilder(Align align, ContentSize contentSize) {
        return register(new ModuleBuilder(entityManager, align, contentSize));
    }

    /**
     * Creates a module/container builder sized against a precomputed inner box.
     */
    public ModuleBuilder moduleBuilder(Align align, InnerBoxSize innerBoxSize) {
        return register(new ModuleBuilder(entityManager, align, innerBoxSize));
    }

    /**
     * Creates a module/container builder using a PDF page as the initial reference.
     */
    public ModuleBuilder moduleBuilder(Align align, PDPage page) {
        return register(new ModuleBuilder(entityManager, align, page));
    }

    /**
     * Creates a builder for a rectangle leaf entity.
     */
    public RectangleBuilder rectangle() {
        return register(new RectangleBuilder(entityManager));
    }

    /**
     * Creates a builder for an image leaf entity.
     */
    public ImageBuilder image() {
        return register(new ImageBuilder(entityManager));
    }

    /**
     * Creates a builder for single-line text or fixed text leaves.
     */
    public TextBuilder text() {
        return register(new TextBuilder(entityManager));
    }

    /**
     * Creates a container builder that arranges children as a row-like group.
     */
    public RowBuilder row(Align align) {
        return register(new RowBuilder(entityManager, align));
    }

    /**
     * Creates a horizontal container builder.
     */
    public HContainerBuilder hContainer(Align align) {
        return register(new HContainerBuilder(entityManager, align));
    }

    /**
     * Creates a vertical container builder.
     */
    public VContainerBuilder vContainer(Align align) {
        return register(new VContainerBuilder(entityManager, align));
    }

    /**
     * Creates the engine-level table builder.
     *
     * <p>Tables materialize as a breakable vertical root with row-atomic leaf
     * entities, which keeps pagination behavior consistent with the rest of the engine.</p>
     */
    public TableBuilder table() {
        return register(new TableBuilder(entityManager));
    }

    /**
     * Creates a generic element builder for low-level custom composition.
     */
    public ElementBuilder element() {
        return register(new ElementBuilder(entityManager));
    }

    /**
     * Builds every registered builder that has not yet been materialized.
     *
     * <p>This is primarily an orchestration helper used by composer internals to
     * ensure deferred builders are registered before layout begins.</p>
     */
    public void buildsComponents() {
        if (builders.isEmpty())
            return;
        builders.stream()
                .filter(builder -> !builder.built())
                .forEach(BuildEntity::build);
    }
}
