package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.compose.layout_core.components.containers.abstract_builders.StackAxis;
import com.demcha.compose.layout_core.components.content.bookmark.BookmarkEntry;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.geometry.InnerBoxSize;
import com.demcha.compose.layout_core.components.geometry.ModuleWidthSeed;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.renderable.Module;
import com.demcha.compose.layout_core.components.renderable.VContainer;
import com.demcha.compose.layout_core.core.Canvas;
import com.demcha.compose.layout_core.core.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModuleBuilder extends ContainerBuilder<ModuleBuilder> {
    /**
     * Constructs a new {@code ModuleBuilder} with layout-resolved full-width
     * semantics and no explicit root-width seed.
     */
    ModuleBuilder(EntityManager entityManager, Align align) {
        super(entityManager, align);
    }

    /**
     * Constructs a new {@code ModuleBuilder} with a root-width seed derived from
     * the supplied content size.
     */
    ModuleBuilder(EntityManager entityManager, Align align, ContentSize contentSize) {
        this(entityManager, align);
        if (contentSize != null) {
            seedWidth(contentSize.width());
            entity.addComponent(contentSize);
        }
    }

    /**
     * Constructs a new {@code ModuleBuilder} with a root-width seed based on the
     * canvas inner width.
     */
    ModuleBuilder(EntityManager entityManager, Align align, Canvas canvas) {
        this(entityManager, align);
        if (canvas != null) {
            double seedWidth = canvas.innerWidth();
            seedWidth(seedWidth);
            entity.addComponent(new ContentSize(seedWidth, 0));
        }
    }

    /**
     * Constructs a new {@code ModuleBuilder} with a root-width seed based on a
     * precomputed inner box.
     */
    ModuleBuilder(EntityManager entityManager, Align align, InnerBoxSize innerBoxSize) {
        this(entityManager, align,
                innerBoxSize == null ? null : new ContentSize(innerBoxSize.width(), innerBoxSize.height()));
    }

    @Override
    public void initialize() {
        entity.addComponent(new Module());
        entity.addComponent(StackAxis.VERTICAL);
        entity.addComponentIfAbsent(new VContainer());
        entity.addComponentIfAbsent(new ContentSize(0, 0));
    }

    private void seedWidth(double width) {
        entity.addComponent(new ModuleWidthSeed(Math.max(0, width)));
    }

    /**
     * Marks this module as a PDF bookmark entry.
     *
     * <p>The bookmark title will appear in the PDF reader's outline panel,
     * pointing to the page and Y-position of this module.</p>
     *
     * @param title the bookmark display title
     * @return this builder
     */
    public ModuleBuilder bookmark(String title) {
        entity.addComponent(new BookmarkEntry(title));
        return this;
    }

    /**
     * Marks this module as a nested PDF bookmark entry.
     *
     * @param title the bookmark display title
     * @param level nesting level (0 = root, 1 = child, etc.)
     * @return this builder
     */
    public ModuleBuilder bookmark(String title, int level) {
        entity.addComponent(new BookmarkEntry(title, level));
        return this;
    }
}
