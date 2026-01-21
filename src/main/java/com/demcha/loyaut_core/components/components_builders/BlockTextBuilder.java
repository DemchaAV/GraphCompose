package com.demcha.loyaut_core.components.components_builders;

import com.demcha.loyaut_core.components.LineTextData;
import com.demcha.loyaut_core.components.containers.abstract_builders.EmptyBox;
import com.demcha.loyaut_core.components.content.text.BlockTextData;
import com.demcha.loyaut_core.components.content.text.Text;
import com.demcha.loyaut_core.components.content.text.TextDataBody;
import com.demcha.loyaut_core.components.content.text.TextStyle;
import com.demcha.loyaut_core.components.core.Component;
import com.demcha.loyaut_core.components.core.Entity;
import com.demcha.loyaut_core.components.geometry.ContentSize;
import com.demcha.loyaut_core.components.geometry.InnerBoxSize;
import com.demcha.loyaut_core.components.layout.Align;
import com.demcha.loyaut_core.components.layout.HAnchor;
import com.demcha.loyaut_core.components.renderable.BlockText;
import com.demcha.loyaut_core.components.renderable.TextComponent;
import com.demcha.loyaut_core.components.style.Margin;
import com.demcha.loyaut_core.components.style.Padding;
import com.demcha.loyaut_core.core.EntityManager;
import com.demcha.loyaut_core.system.LayoutSystem;
import com.demcha.loyaut_core.system.interfaces.Font;
import com.demcha.loyaut_core.utils.TextSanitizer;
import com.demcha.markdown.MarkDownParser;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Slf4j
public class BlockTextBuilder extends EmptyBox<BlockTextBuilder> {
    private final MarkDownParser markDownParser = new MarkDownParser();
    Map<Class<? extends Component>, Component> baseComponents;
    @Setter
    private double lineSpacing = 0.0;
    private List<LineTextData> lines;
    private TextStyle textStyle;

    public BlockTextBuilder(EntityManager entityManager, Align align, TextStyle textStyle) {
        super(entityManager);
        align(align);
        this.textStyle = textStyle;
        this.addComponent(textStyle);
    }


    public BlockTextBuilder text(TextBuilder textBuilder) {
        var rowText = textBuilder.build();
        var style = rowText.getComponent(TextStyle.class).orElse(textStyle.DEFAULT_STYLE);
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
        var style = entity.getComponent(TextStyle.class).orElse(textStyle);
        textStyle = style;

        // Make a shallow copy of components (excluding Text, which we will replace)
        var components = new HashMap<>(entity.view());
        components.remove(Text.class); // we'll set a new Text per line
        components.remove(TextComponent.class);
        components.remove(ContentSize.class);
        components.remove(Padding.class);
        baseComponents = new HashMap<>(components);

        // Remove original entity (we're going to replace it with per-line children)
        entityManager.remove(entity);

        return breakLinesFromList(List.of(text.value()), innerBoxSize, "");
    }

    public BlockTextData breakLinesFromList(@NonNull List<String> text, @NonNull InnerBoxSize innerBoxSize, String bulletOffset) {
        FontContainer fontContainer = getFontContainer();
        TextStyle style = fontContainer.style() == null ? textStyle : fontContainer.style();
        Margin margin = (Margin) baseComponents.getOrDefault(Margin.class, Margin.zero());
        margin = margin == null ? Margin.zero() : margin;

        final double maxWidth = innerBoxSize.width();
        final double horizontalMargins = margin.horizontal();

        String offsetStr = bulletOffset == null ? "" : " ".repeat(bulletOffset.length());

        text = text.stream().map(TextSanitizer::sanitize).toList();

        for (String textLine : text) {
            List<TextDataBody> tokens;
            if (entityManager.isMarkdown()) {
                tokens = markDownParser.getBody(textLine, style);
            } else {
                String[] words = textLine.split("\\s+");
                tokens = new ArrayList<>();
                for (int i = 0; i < words.length; i++) {
                    tokens.add(new TextDataBody(words[i], style));
                    if (i < words.length - 1) {
                        tokens.add(new TextDataBody(" ", style));
                    }
                }
            }

            if (tokens.isEmpty()) continue;

            Deque<TextDataBody> line = new ArrayDeque<>();
            double lineWidth = horizontalMargins;

            for (TextDataBody token : tokens) {
                double tokenWidth = fontContainer.font().getTextWidth(token.textStyle(), token.text());

                if (lineWidth + tokenWidth <= maxWidth) {
                    line.addLast(token);
                    lineWidth += tokenWidth;
                } else {
                    if (isSticky(token.text()) && !line.isEmpty()) {
                        TextDataBody last = line.peekLast();
                        if (last != null && !last.text().isBlank()) {
                            double lastWidth = fontContainer.font().getTextWidth(last.textStyle(), last.text());

                            line.removeLast();
                            if (!line.isEmpty()) {
                                createLineFromBodies(new ArrayList<>(line));
                            }
                            line.clear();
                            lineWidth = horizontalMargins;

                            line.addLast(last);
                            lineWidth += lastWidth;

                            line.addLast(token);
                            lineWidth += tokenWidth;

                            if (lineWidth > maxWidth) {
                                createLineFromBodies(new ArrayList<>(line));
                                line.clear();
                                lineWidth = horizontalMargins;
                            }
                            continue;
                        }
                    }

                    if (token.text().isBlank()) {
                        continue;
                    }

                    if (!line.isEmpty()) {
                        createLineFromBodies(new ArrayList<>(line));
                        line.clear();
                        lineWidth = horizontalMargins;

                        if (!offsetStr.isEmpty()) {
                            TextDataBody indent = new TextDataBody(offsetStr, style);
                            double indentWidth = fontContainer.font().getTextWidth(style, offsetStr);
                            line.addLast(indent);
                            lineWidth += indentWidth;
                        }
                    }

                    if (lineWidth + tokenWidth <= maxWidth) {
                        line.addLast(token);
                        lineWidth += tokenWidth;
                    } else {
                        line.addLast(token);
                        createLineFromBodies(new ArrayList<>(line));
                        line.clear();
                        lineWidth = horizontalMargins;
                    }
                }
            }

            if (!line.isEmpty()) {
                createLineFromBodies(new ArrayList<>(line));
            }
        }

        return new BlockTextData(lines, (float) lineSpacing);
    }

    private boolean isSticky(String text) {
        if (text == null || text.isEmpty()) return false;
        char first = text.charAt(0);
        return ",.;:!?)]}".indexOf(first) >= 0;
    }

    private @NotNull BlockTextBuilder.FontContainer getFontContainer() {
        TextStyle style = (TextStyle) baseComponents.getOrDefault(TextStyle.class, textStyle);
        LayoutSystem layoutSystem = entityManager.getSystems().getSystem(LayoutSystem.class).orElseThrow();
        Class aClass = layoutSystem.getRenderingSystem().fontClazz();
        Font font = (Font<?>) entityManager.getFonts().getFont(style.fontName(), aClass).orElseThrow();
        FontContainer result = new FontContainer(style, font);
        return result;
    }

    private void createLine(List<String> words) {
        // Join with spaces for correct rendering
        String lineText = String.join(" ", words);
        FontContainer fontContainer = getFontContainer();
        lines.add(createLineTextData(lineText, fontContainer.style()));
    }

    private void createLineFromBodies(List<TextDataBody> bodies) {
        lines.add(new LineTextData(bodies, 0));
    }

    private LineTextData createLineTextData(String chunkText, TextStyle textStyle) {
        log.debug("createLineTextData: '{}'", chunkText);
        LineTextData lineTextData;
        if (entityManager.isMarkdown()) {
            List<TextDataBody> body = markDownParser.getBody(chunkText, textStyle);
            lineTextData = new LineTextData(body, 0);
        } else {
            lineTextData = LineTextData.createWithoutMarkdown(chunkText, textStyle, 0);
        }
        log.debug("createLineTextData: {}", lineTextData);
        return lineTextData;
    }

    @Override
    public void initialize() {
        entity.addComponent(new BlockText());
    }

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

        if (blockTextData.lines().isEmpty()) {
            return new ContentSize(padding.horizontal(), padding.vertical());
        }

        TextStyle style = entity.getComponent(TextStyle.class).orElse(TextStyle.DEFAULT_STYLE);
        var fontContainer = getFontContainer();

        var width = blockTextData.lines().stream()
                .mapToDouble(line -> line.width(fontContainer.font()))
                .max()
                .orElse(0.0);

        var spacingOpt = entity.getComponent(Align.class);
        double spacing = spacingOpt.orElse(Align.defaultAlign(0.0)).spacing();


        double textHeight = fontContainer.font().getTextHeight(style);
        double calculatedHigh = (blockTextData.lines().size()) * textHeight;
        double spacingFullHigh = Math.max(0, (blockTextData.lines().size() - 1) * spacing);
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

    private record FontContainer(TextStyle style, Font font) {
    }


}
