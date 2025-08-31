package com.demcha.components.components_builders;

import com.demcha.components.containers.abstract_builders.ContainerBuilder;
import com.demcha.components.containers.abstract_builders.StackAxis;
import com.demcha.components.content.text.Text;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.ParentComponent;
import com.demcha.components.renderable.TextComponent;
import com.demcha.components.renderable.VContainer;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class BlockTextBuilder extends ContainerBuilder<BlockTextBuilder> {
    private double lineSpacing = 0.0;

    public BlockTextBuilder(EntityManager entityManager) {
        super(entityManager);
        this.stackAxis = StackAxis.VERTICAL;
    }


    public BlockTextBuilder text(TextBuilder textBuilder) {
        var rowText = textBuilder.build();
        var boundingBox = InnerBoxSize.from(this.entity).orElseThrow();
        breakLines(rowText, boundingBox);
        return this;
    }


    public List<Entity> breakLines(@NonNull Entity entity, @NonNull InnerBoxSize innerBoxSize) {
        // Early exit if not a block of text
        if (!entity.hasAssignable(TextComponent.class)) {
            log.debug("Entity doesn't have BlockText component");
            return children();
        }

        // Required components (fail fast but with a clear message)
        var text = entity.getComponent(Text.class)
                .orElseThrow(() -> new IllegalStateException("Missing Text component"));
        var style = entity.getComponent(TextStyle.class).orElse(TextStyle.defaultStyle());

        var margin = entity.getComponent(Margin.class).orElse(Margin.zero());
        var padding = entity.getComponent(Padding.class).orElse(Padding.zero());

        final double maxWidth = innerBoxSize.innerW();
        final double horizontalMargins = margin.horizontal();

        // Make a shallow copy of components (excluding Text, which we will replace)
        var components = new HashMap<>(entity.view());
        components.remove(Text.class); // we'll set a new Text per line
        components.remove(TextComponent.class);
        components.remove(ContentSize.class);

        // Remove original entity (we're going to replace it with per-line children)
        entityManager.remove(entity);

        // Split text into tokens (simple space-based; enhance with hyphenation if needed)
        String[] words = text.value().split("\\s+");

        Deque<String> line = new ArrayDeque<>();
        double lineWidth = horizontalMargins; // start with margins

        // Pre-compute space width (if your TextStyle has such a method; otherwise use getTextWidth(" "))
        double spaceWidth = style.getTextWidth(" ");

        for (String word : words) {
            double wordWidth = style.getTextWidth(word);

            // If line is empty, try to place the word even if it overflows
            if (line.isEmpty()) {
                if (horizontalMargins + wordWidth <= maxWidth) {
                    line.addLast(word);
                    lineWidth = horizontalMargins + wordWidth;
                } else {
                    // Long single word: place as its own line and warn
                    log.warn("Word is too long; can't break line: '{}'", word);
                    createLine(List.of(word), components); // still create a line
                    line.clear();
                    lineWidth = horizontalMargins;
                }
                continue;
            }

            // If we already have words, include a space before the next word
            double candidateWidth = lineWidth + spaceWidth + wordWidth;

            if (candidateWidth <= maxWidth) {
                line.addLast(word);
                lineWidth = candidateWidth;
            } else {
                // Flush current line
                createLine(new ArrayList<>(line), components);
                // Start a new line with this word
                line.clear();
                if (horizontalMargins + wordWidth <= maxWidth) {
                    line.addLast(word);
                    lineWidth = horizontalMargins + wordWidth;
                } else {
                    log.warn("Word is too long; can't break line: '{}'", word);
                    createLine(List.of(word), components);
                    lineWidth = horizontalMargins;
                }
            }
        }

        // Flush the last line if any
        if (!line.isEmpty()) {
            createLine(new ArrayList<>(line), components);
        }


        return children();
    }

    private void createLine(List<String> words, Map<Class<? extends Component>, Component> baseComponents) {
        // Join with spaces for correct rendering
        String lineText = String.join(" ", words);


        var textBuilder = new TextBuilder(entityManager)
                .textWithAutoSize(lineText);

        // Copy over all base components EXCEPT Text (already set) and anything you explicitly want to override.
        for (Map.Entry<Class<? extends Component>, Component> entry : baseComponents.entrySet()) {
            log.debug("EntryKey: {} value: {}, {}", entry.getKey(), entry.getValue());

            textBuilder.addComponent(entry.getValue());

        }
       var entity =  textBuilder.build();

       textBuilder.addComponent(new ParentComponent(this.entity));

        entity().getChildren().add(entity);
    }


    @Override
    public void initialize() {
        entity.addComponent(new BlockText());
    }

    /**
     * Constructs a new {@code VContainerBuilder} with the specified Entity Manager.
     *
     * @param entityManager The {@link EntityManager} to which the container will be added.
     */

    /**
     * Initializes the container builder with the specified alignment.
     * This method calls the common creation logic from the superclass and then
     * adds a {@link VContainer} component to the entity.
     *
     * @param align The {@link Align} strategy for arranging children within the container.
     * @return This builder instance for method chaining.
     */
    @Override
    public BlockTextBuilder create(Align align) {
        super.create(align); // Call the common logic
        entity.addComponentIfAbsent(new BlockText()); // Add the specific component
        return self();
    }


}
