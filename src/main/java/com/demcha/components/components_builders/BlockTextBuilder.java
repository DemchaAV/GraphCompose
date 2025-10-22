package com.demcha.components.components_builders;

import com.demcha.components.LineTextData;
import com.demcha.components.containers.abstract_builders.EmptyBox;
import com.demcha.components.content.text.BlockTextData;
import com.demcha.components.content.text.Text;
import com.demcha.components.content.text.TextStyle;
import com.demcha.components.core.Component;
import com.demcha.components.core.Entity;
import com.demcha.components.geometry.ContentSize;
import com.demcha.components.geometry.InnerBoxSize;
import com.demcha.components.layout.Align;
import com.demcha.components.layout.HAnchor;
import com.demcha.components.renderable.BlockText;
import com.demcha.components.renderable.TextComponent;
import com.demcha.components.style.Margin;
import com.demcha.components.style.Padding;
import com.demcha.core.EntityManager;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class BlockTextBuilder extends EmptyBox<BlockTextBuilder> {
    Map<Class<? extends Component>, Component> baseComponents;
    @Setter
    private double lineSpacing = 0.0;
    private List<LineTextData> lines;
    private TextStyle textStyle;

    public BlockTextBuilder(EntityManager entityManager, Align align) {
        super(entityManager);
        align(align);
    }


    public BlockTextBuilder text(TextBuilder textBuilder) {
        var rowText = textBuilder.build();
        var style = rowText.getComponent(TextStyle.class).orElse(TextStyle.DEFAULT_STYLE);
        this.textStyle = style;
        lines = new ArrayList<>();


        var boundingBox = InnerBoxSize.from(this.entity).orElseThrow();
        BlockTextData blockTextData = breakLines(rowText, boundingBox);


        addComponent(blockTextData);
        addComponent(textStyle);
        return this;
    }

    public BlockTextBuilder text(List<String> text, TextStyle style, Padding padding, Margin margin, String bulletOffset) {
        this.textStyle = style;
        lines = new ArrayList<>();
        this.baseComponents = new HashMap<>();

        this.baseComponents.put(Padding.class, padding);
        this.baseComponents.put(Margin.class, margin);
        this.baseComponents.put(TextStyle.class, style);


        var boundingBox = InnerBoxSize.from(this.entity).orElseThrow();
        BlockTextData blockTextData = breakLinesFromList(text, boundingBox, bulletOffset == null ? "" : bulletOffset);


        addComponent(blockTextData);
        addComponent(textStyle);
        return this;
    }


    public BlockTextData breakLines(@NonNull Entity entity, @NonNull InnerBoxSize innerBoxSize) {
        // Early exit if not a block of text
        if (!entity.hasAssignable(TextComponent.class)) {
            log.debug("Entity doesn't have BlockText component");
            return new BlockTextData(lines, (float) lineSpacing);
        }

        // Required components (fail fast but with a clear message)
        var text = entity.getComponent(Text.class)
                .orElseThrow(() -> new IllegalStateException("Missing Text component"));
        var style = entity.getComponent(TextStyle.class).orElse(TextStyle.defaultStyle());
        textStyle = style;

        var margin = entity.getComponent(Margin.class).orElse(Margin.zero());
        var padding = entity.getComponent(Padding.class).orElse(Padding.zero());

        final double maxWidth = innerBoxSize.innerW();
        final double horizontalMargins = margin.horizontal();

        // Make a shallow copy of components (excluding Text, which we will replace)
        var components = new HashMap<>(entity.view());
        components.remove(Text.class); // we'll set a new Text per line
        components.remove(TextComponent.class);
        components.remove(ContentSize.class);
        components.remove(Padding.class);
        baseComponents = new HashMap<>(components);

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
                    createLine(List.of(word)); // still create a line
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
                createLine(new ArrayList<>(line));
                // Start a new line with this word
                line.clear();
                if (horizontalMargins + wordWidth <= maxWidth) {
                    line.addLast(word);
                    lineWidth = horizontalMargins + wordWidth;
                } else {
                    log.warn("Word is too long; can't break line: '{}'", word);
                    createLine(List.of(word));
                    lineWidth = horizontalMargins;
                }
            }
        }

        // Flush the last line if any
        if (!line.isEmpty()) {
            createLine(new ArrayList<>(line));
        }


        BlockTextData blockTextData = new BlockTextData(lines, (float) lineSpacing);
        return blockTextData;
    }

    public BlockTextData breakLinesFromList(@NonNull List<String> text, @NonNull InnerBoxSize innerBoxSize, String bulletOffset) {


        TextStyle style = (TextStyle) baseComponents.getOrDefault(TextStyle.class, TextStyle.DEFAULT_STYLE);
        Margin margin = (Margin) baseComponents.getOrDefault(Margin.class, Margin.zero());
        style = style == null ? TextStyle.defaultStyle() : style;
        margin = margin == null ? Margin.zero() : margin;

        final double maxWidth = innerBoxSize.innerW();
        final double horizontalMargins = margin.horizontal();

        // Make a shallow copy of components (excluding Text, which we will replace)


        for (String textLine : text) {

            if (style.getTextWidth(textLine) <= maxWidth) {
                createLine(List.of(textLine));
                continue;
            }


            // Split text into tokens (simple space-based; enhance with hyphenation if needed)
            String[] words = textLine.split("\\s+");

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
                        createLine(List.of(word)); // still create a line
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
                    createLine(new ArrayList<>(line));
                    // Start a new line with this word
                    line.clear();
                    if (horizontalMargins + wordWidth <= maxWidth) {
                        line.addLast(" ".repeat(bulletOffset.length()) + word);
                        lineWidth = horizontalMargins + wordWidth;
                    } else {
                        log.warn("Word is too long; can't break line: '{}'", word);
                        createLine(List.of(word));
                        lineWidth = horizontalMargins;
                    }
                }
            }

            // Flush the last line if any
            if (!line.isEmpty()) {
                createLine(new ArrayList<>(line));
            }

        }


        return new BlockTextData(lines, (float) lineSpacing);
    }

    private void createLine(List<String> words) {
        // Join with spaces for correct rendering
        String lineText = String.join(" ", words);
        lines.add(createLineTextData(lineText));
    }

    private LineTextData createLineTextData(String chunkText) {
        log.debug("createLineTextData: '{}'", chunkText);
        LineTextData lineTextData = new LineTextData(chunkText, textStyle.getTextWidth(chunkText));
        log.debug("createLineTextData: {}", lineTextData);
        return lineTextData;
    }


    @Override
    public void initialize() {
        entity.addComponent(new BlockText());
    }

//
//    /**
//     * Initializes the container builder with the specified alignment.
//     * This method calls the common creation logic from the superclass and then
//     * adds a {@link BlockText} component to the entity.
//     *
//     * @param align The {@link Align} strategy for arranging children within the container.
//     * @return This builder instance for method chaining.
//     */
//    public BlockTextBuilder create(Align align) {
//        align(align);
//        entity.addComponentIfAbsent(new BlockText()); // Add the specific component
//        return self();
//    }

    public BlockTextBuilder align(Align align) {
        HAnchor h = align.h();
        if (HAnchor.LEFT != h && HAnchor.RIGHT != h && HAnchor.CENTER != h) {

            log.info("Align has to be HAnchor.LEFT or HAnchor.RIGHT  current {}", align);
            throw new IllegalStateException("Align has to be HAnchor.LEFT or HAnchor.RIGHT in BlockText");


        }
        entity.addComponent(align);
        return self();
    }

    /**
     * Compute container size for text
     *
     * @return
     */

    private ContentSize computeContentSize() {
        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        var blockTextData = entity.getComponent(BlockTextData.class).orElseThrow();
        var width = blockTextData.lines().stream().max(Comparator.comparingDouble(LineTextData::width)).map(LineTextData::width).orElseThrow();
        var spacingOpt = entity.getComponent(Align.class);
        double spacing = spacingOpt.orElse(Align.defaultAlign(0.0)).spacing();


        double textHeight = entity.getComponent(TextStyle.class).orElse(TextStyle.DEFAULT_STYLE).getLineHeight();
        double calculatedHigh = (blockTextData.lines().size()) * textHeight;
        double spacingFullHigh = (blockTextData.lines().size() - 1) * spacing;
        double high = calculatedHigh + spacingFullHigh + padding.vertical();

        return new ContentSize(width + padding.horizontal(), high);
    }

    @Override
    public Entity build() {

        //Definition a size for current block
        ContentSize contentSize = computeContentSize();

        entity.addComponent(contentSize);
        manager().putEntity(entity());

        return entity();
    }


}

