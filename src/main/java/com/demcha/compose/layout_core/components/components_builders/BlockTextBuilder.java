package com.demcha.compose.layout_core.components.components_builders;

import com.demcha.compose.layout_core.components.containers.abstract_builders.EmptyBox;
import com.demcha.compose.layout_core.components.content.text.BlockTextData;
import com.demcha.compose.layout_core.components.content.text.BlockTextLineMetrics;
import com.demcha.compose.layout_core.components.content.text.LineTextData;
import com.demcha.compose.layout_core.components.content.text.Text;
import com.demcha.compose.layout_core.components.content.text.TextDataBody;
import com.demcha.compose.layout_core.components.content.text.TextStyle;
import com.demcha.compose.layout_core.components.core.Component;
import com.demcha.compose.layout_core.components.core.Entity;
import com.demcha.compose.layout_core.components.geometry.ContentSize;
import com.demcha.compose.layout_core.components.geometry.InnerBoxSize;
import com.demcha.compose.layout_core.components.layout.Align;
import com.demcha.compose.layout_core.components.layout.HAnchor;
import com.demcha.compose.layout_core.components.renderable.BlockText;
import com.demcha.compose.layout_core.components.renderable.TextComponent;
import com.demcha.compose.layout_core.components.style.Margin;
import com.demcha.compose.layout_core.components.style.Padding;
import com.demcha.compose.layout_core.core.EntityManager;
import com.demcha.compose.layout_core.system.interfaces.TextMeasurementSystem;
import com.demcha.compose.layout_core.utils.TextSanitizer;
import com.demcha.compose.markdown.MarkDownParser;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Builder for breakable multi-line text blocks.
 * <p>
 * This builder is used when text should behave like paragraph content rather
 * than a single measured line. It can parse markdown, normalize bullet prefixes,
 * wrap lines to the available width, and emit the line metadata later consumed
 * by layout, pagination, and rendering.
 * </p>
 *
 * <p>Compared with {@link TextBuilder}, this builder is the better choice for
 * profile sections, descriptions, lists, and any content that may need to span
 * multiple lines or pages.</p>
 */
@Slf4j
public class BlockTextBuilder extends EmptyBox<BlockTextBuilder> {

    private final MarkDownParser markDownParser = new MarkDownParser();
    private BlockIndentStrategy blockStrategy;

    private Map<Class<? extends Component>, Component> baseComponents;

    @Setter
    private double lineSpacing = 0.0;

    private List<LineTextData> lines;

    private TextStyle textStyle;
    private String bulletOffset;

    BlockTextBuilder(EntityManager entityManager, Align align, TextStyle textStyle) {
        super(entityManager);
        align(align);
        this.textStyle = textStyle;
        this.addComponent(textStyle);
        this.blockStrategy = BlockIndentStrategy.NONE;
    }

    /**
     * Sets the indentation strategy used when wrapped lines are generated.
     *
     * @param strategy the indent strategy to apply
     * @return this builder
     */
    public BlockTextBuilder strategy(BlockIndentStrategy strategy) {
        this.blockStrategy = strategy;
        return this;
    }

    private static String normalizeBulletPrefix(String bulletOffset) {
        if (bulletOffset == null)
            return "";
        if (bulletOffset.isEmpty())
            return "";

        // If user passes a bullet like "-" or "===", add a space so it doesn't stick to
        // the first word.
        char last = bulletOffset.charAt(bulletOffset.length() - 1);
        if (!Character.isWhitespace(last)) {
            return bulletOffset + " ";
        }
        return bulletOffset;
    }

    /**
     * Build an indentation string (spaces) with visual width ~= prefix width.
     * This is crucial for custom bullets like "===" where char-count based indents
     * are wrong.
     */
    private static String computeIndentFromPrefix(TextMeasurementSystem measurementSystem, TextStyle style, String prefix) {
        if (prefix == null || prefix.isEmpty())
            return "";

        double target = measurementSystem.textWidth(style, prefix);
        double spaceW = measurementSystem.textWidth(style, " ");

        if (spaceW <= 0)
            return "";

        int spaces = (int) Math.ceil(target / spaceW);
        String indent = " ".repeat(Math.max(0, spaces));

        // Debug: confirm we actually match the prefix width.
        double indentW = measurementSystem.textWidth(style, indent);
        log.debug("Bullet indent compute | prefix='{}' prefixW={} spaceW={} spaces={} indentW={}",
                vis(prefix), target, spaceW, spaces, indentW);

        return indent;
    }

    private static void appendPrefixBodies(List<TextDataBody> out, String prefix, TextStyle style) {
        if (prefix == null || prefix.isEmpty())
            return;

        boolean hasVisible = prefix.chars().anyMatch(ch -> !Character.isWhitespace(ch));

        // If it's only spaces/tabs, keep as a single token.
        if (!hasVisible) {
            out.add(new TextDataBody(prefix, style));
            return;
        }

        // Split trailing whitespace into separate tokens to avoid "===CVRewriter" /
        // "-Honed" issues
        int end = prefix.length();
        while (end > 0 && Character.isWhitespace(prefix.charAt(end - 1))) {
            end--;
        }

        String core = prefix.substring(0, end);
        String ws = prefix.substring(end);

        if (!core.isEmpty()) {
            out.add(new TextDataBody(core, style));
        }

        for (int i = 0; i < ws.length(); i++) {
            char c = ws.charAt(i);
            out.add(new TextDataBody(String.valueOf(c), style));
        }
    }

    private static String vis(String s) {
        if (s == null)
            return "null";
        return s
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    // =========================
    // Bullet + Indent helpers
    // =========================

    private static String bodiesToDbg(List<TextDataBody> bodies) {
        if (bodies == null)
            return "null";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bodies.size(); i++) {
            TextDataBody b = bodies.get(i);
            if (i > 0)
                sb.append(", ");
            if (b == null) {
                sb.append("null");
            } else {
                sb.append("'").append(vis(b.text())).append("'");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static int countLeadingWhitespace(String s) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '	') {
                i++;
            } else {
                break;
            }
        }
        return i;
    }

    public BlockTextBuilder text(TextBuilder textBuilder) {
        var rowText = textBuilder.build();

        var style = rowText.getComponent(TextStyle.class).orElse(textStyle.DEFAULT_STYLE);
        this.textStyle = style;
        this.lines = new ArrayList<>();

        var boundingBox = InnerBoxSize.from(this.entity).orElseThrow();
        BlockTextData blockTextData = breakLines(rowText, boundingBox);

        addComponent(blockTextData);
        addComponent(textStyle);
        return this;
    }

    public BlockTextBuilder text(List<String> text,
            TextStyle style,
            Padding padding,
            Margin margin) {
        this.textStyle = style;
        this.lines = new ArrayList<>();
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
    public BlockTextBuilder bulletOffset(String bulletOffset){
        this.bulletOffset = bulletOffset;
        return this;
    }

    // =========================
    // Tokenize + Wrap Helpers
    // =========================

    public BlockTextData breakLines(@NonNull Entity entity, @NonNull InnerBoxSize innerBoxSize) {
        // Early exit if not a block of text
        if (!entity.hasAssignable(TextComponent.class)) {
            log.debug("Entity doesn't have BlockText component");
            return new BlockTextData(lines, (float) lineSpacing);
        }

        var text = entity.getComponent(Text.class)
                .orElseThrow(() -> new IllegalStateException("Missing Text component"));

        var style = entity.getComponent(TextStyle.class).orElse(textStyle);
        this.textStyle = style;

        // Copy components (excluding those that are irrelevant for line entities)
        var components = new HashMap<>(entity.view());
        components.remove(Text.class);
        components.remove(TextComponent.class);
        components.remove(ContentSize.class);
        components.remove(Padding.class);

        this.baseComponents = new HashMap<>(components);

        // Replace original entity with per-line children
        entityManager.remove(entity);

        return breakLinesFromList(List.of(text.value()), innerBoxSize, "");
    }

    public BlockTextData breakLinesFromList(@NonNull List<String> text,
            @NonNull InnerBoxSize innerBoxSize,
            String bulletOffset) {

        final TextMeasurementSystem measurementSystem = textMeasurementSystem();
        final TextStyle style = measurementStyle();

        Margin margin = (Margin) baseComponents.getOrDefault(Margin.class, Margin.zero());
        margin = margin == null ? Margin.zero() : margin;

        final double maxWidth = innerBoxSize.width();
        final double horizontalMargins = margin.horizontal();

        final BulletSpec bullet = BulletSpec.from(bulletOffset, measurementSystem, style);

        log.debug(
                "breakLinesFromList | markdown={} | listSize={} | maxWidth={} | hMargins={} | bulletPrefix='{}' | softIndent='{}' | hardIndent='{}'",
                entityManager.isMarkdown(), text.size(), maxWidth, horizontalMargins,
                vis(bullet.prefix()), vis(bullet.softWrapIndent()), vis(bullet.hardWrapIndent()));

        for (String inputLine : text) {
            // Bullet/indent are handled as separate tokens (NOT concatenated into the
            // markdown input).
            String normalizedText = normalizeNewlines(inputLine);
            String[] explicitLines = splitExplicitLines(normalizedText);

            for (int lineIndex = 0; lineIndex < explicitLines.length; lineIndex++) {
                String explicitLine = TextSanitizer.sanitize(explicitLines[lineIndex]);

                List<TextDataBody> tokens = tokenizeExplicitLine(explicitLine, lineIndex, bullet, style);
                if (tokens.isEmpty()) {
                    continue;
                }

                // Soft wrap indent differs from explicit "\n" continuation indent.
                // Apply strategy for wrapped lines
                String wrapIndent = "";
                if (blockStrategy.indentWrappedLines()) {
                    wrapIndent = (lineIndex == 0) ? bullet.softWrapIndent() : bullet.hardWrapIndent();
                }

                wrapTokensIntoLines(tokens, measurementSystem, maxWidth, wrapIndent, style);
            }
        }

        return new BlockTextData(lines, (float) lineSpacing);
    }

    private String normalizeNewlines(String text) {
        // Handle both actual newlines and escaped sequences (literal backslash + n)
        if (text == null)
            return "";
        String normalized = text.replace("\\n", "\n").replace("\\r", "\r");
        log.debug("AFTER NORMALIZE normalizedText: '{}'", normalized);
        return normalized;
    }

    private String[] splitExplicitLines(String normalizedText) {
        String[] lines = normalizedText.split("\\r?\\n");
        log.debug("SPLIT into {} lines: {}", lines.length, Arrays.toString(lines));
        return lines;
    }

    private List<TextDataBody> tokenizeExplicitLine(String explicitLine,
            int explicitLineIndex,
            BulletSpec bullet,
            TextStyle style) {
        log.debug("Processing line {}: '{}'", explicitLineIndex, explicitLine);

        if (explicitLine == null || explicitLine.isBlank()) {
            return List.of();
        }

        boolean isMarkdown = entityManager.isMarkdown();

        if (isMarkdown) {
            // Render bullet/indent as plain TextDataBody tokens, parse the rest as inline
            // markdown.
            List<TextDataBody> bodies = tokenizeMarkdownLine(explicitLine, explicitLineIndex, bullet, style);

            log.debug("Markdown tokensCount={} | explicitIndex={} | rawLine='{}' | tokens={}",
                    bodies.size(), explicitLineIndex, vis(explicitLine), bodiesToDbg(bodies));

            if (bodies.isEmpty()) {
                log.warn(
                        "Markdown produced 0 tokens for non-empty line | explicitIndex={} | line='{}' | hardIndent='{}'",
                        explicitLineIndex, vis(explicitLine), vis(bullet == null ? "" : bullet.hardWrapIndent()));
            }

            return bodies;
        }

        // Non-markdown: split into words + spaces
        String[] words = explicitLine.split("\\s+");
        List<TextDataBody> tokens = new ArrayList<>();

        // Apply strategy for first line indent
        if (explicitLineIndex == 0) {
            if (blockStrategy.indentFirstLine()) {
                appendPrefixBodies(tokens, bullet == null ? "" : bullet.prefix(), style);
            }
        } else {
            // For subsequent explicit lines, apply strategy for wrapped lines
            if (blockStrategy.indentWrappedLines()) {
                appendPrefixBodies(tokens, bullet == null ? "" : bullet.hardWrapIndent(), style);
            }
        }

        for (int i = 0; i < words.length; i++) {
            if (words[i].isEmpty())
                continue;

            tokens.add(new TextDataBody(words[i], style));

            if (i < words.length - 1) {
                tokens.add(new TextDataBody(" ", style));
            }
        }

        return tokens;
    }

    /**
     * Wraps tokens into lines based on the maximum width.
     * Handles "sticky" punctuation to prevent orphaned brackets.
     */
    private void wrapTokensIntoLines(List<TextDataBody> tokens,
            TextMeasurementSystem measurementSystem,
            double maxWidth,
            String offsetStr,
            TextStyle baseStyle) {

        if (tokens == null || tokens.isEmpty())
            return;

        // Find the style of the first non-blank token (for accurate space width
        // calculation)
        TextStyle styleForIndent = baseStyle;
        for (TextDataBody t : tokens) {
            if (t.text() != null && !t.text().isBlank()) {
                styleForIndent = t.textStyle();
                break;
            }
        }

        // Measure indent reliably (10 spaces => 10 * spaceWidth)
        double indentWidth = 0;
        if (!offsetStr.isEmpty() && offsetStr.isBlank()) {
            double spaceW = measurementSystem.textWidth(styleForIndent, " ");
            indentWidth = spaceW * offsetStr.length();
        } else if (!offsetStr.isEmpty()) {
            indentWidth = measurementSystem.textWidth(styleForIndent, offsetStr);
        }

        Deque<TextDataBody> line = new ArrayDeque<>();
        double lineWidth = 0;

        for (int i = 0; i < tokens.size(); i++) {
            TextDataBody token = tokens.get(i);
            String text = token.text();

            boolean isIndentToken = !offsetStr.isEmpty() && text.equals(offsetStr);

            // Skip leading blanks, BUT keep the indent token AND the first token (bullet
            // prefix)
            boolean isFirstToken = (i == 0);
            if (line.isEmpty() && text.isBlank() && !isIndentToken && !isFirstToken) {
                continue;
            }

            double tokenWidth = isIndentToken
                    ? indentWidth
                    : measurementSystem.textWidth(token.textStyle(), text);

            boolean stickyToNext = isStickyToNext(text);
            boolean stickyToPrev = isSticky(text);

            // Check if fits
            if (lineWidth + tokenWidth <= maxWidth) {
                // It fits. But if it's sticky to next, check if next fits too.
                boolean forceWrap = false;
                if (stickyToNext && i + 1 < tokens.size()) {
                    TextDataBody nextToken = tokens.get(i + 1);
                    double nextWidth = measurementSystem.textWidth(nextToken.textStyle(), nextToken.text());

                    // If adding the next token would exceed the width, we force a wrap NOW
                    // so that the current token (e.g. "(") moves to the next line together with the
                    // next token.
                    if (lineWidth + tokenWidth + nextWidth > maxWidth) {
                        forceWrap = true;
                    }
                }

                if (!forceWrap) {
                    line.addLast(token);
                    lineWidth += tokenWidth;
                    continue;
                }
            }

            // Doesn't fit OR forceWrap
            if (text.isBlank() && !isIndentToken) {
                continue;
            }

            // If sticky to previous, try to bring previous token to the new line
            if (stickyToPrev && !line.isEmpty()) {
                TextDataBody prev = line.removeLast();
                double previousWidth = measuredTokenWidth(prev, offsetStr, indentWidth, measurementSystem);
                lineWidth -= previousWidth;
                // Flush the line without 'prev'
                if (!line.isEmpty()) {
                    boolean hasContent = false;
                    for (TextDataBody b : line) {
                        boolean isIndent = !offsetStr.isEmpty() && b.text().equals(offsetStr);
                        if (!isIndent && !b.text().isBlank()) {
                            hasContent = true;
                            break;
                        }
                    }

                    if (hasContent) {
                        createLineFromBodies(new ArrayList<>(line), lineWidth, baseStyle);
                    }
                }
                line.clear();
                lineWidth = 0;

                // Add indent for wrapped lines
                if (!offsetStr.isEmpty()) {
                    line.addLast(new TextDataBody(offsetStr, baseStyle));
                    lineWidth += indentWidth;
                }

                // Add 'prev' to new line
                line.addLast(prev);
                lineWidth += previousWidth;

                // Add current token
                line.addLast(token);
                lineWidth += tokenWidth;
            } else {
                // Normal wrap
                if (!line.isEmpty()) {
                    createLineFromBodies(new ArrayList<>(line), lineWidth, baseStyle);
                    line.clear();
                    lineWidth = 0;

                    // add indent for wrapped lines
                    if (!offsetStr.isEmpty()) {
                        line.addLast(new TextDataBody(offsetStr, baseStyle));
                        lineWidth += indentWidth;
                    }
                }

                line.addLast(token);
                lineWidth += tokenWidth;
            }
        }

        if (!line.isEmpty()) {
            createLineFromBodies(new ArrayList<>(line), lineWidth, baseStyle);
        }
    }

    /**
     * Returns the measured width of a token, using a precomputed indent width for
     * indent-only tokens and the measurement system for all other tokens.
     *
     * @param token             token to measure
     * @param offsetStr         current wrap-indent string (empty when no indent is active)
     * @param indentWidth       precomputed pixel width of the indent string
     * @param measurementSystem active text measurement system
     * @return measured width in layout units, or {@code 0.0} if the token is {@code null}
     */
    private double measuredTokenWidth(TextDataBody token,
                                      String offsetStr,
                                      double indentWidth,
                                      TextMeasurementSystem measurementSystem) {
        if (token == null) {
            return 0.0;
        }

        boolean isIndentToken = !offsetStr.isEmpty() && offsetStr.equals(token.text());
        return isIndentToken
                ? indentWidth
                : measurementSystem.textWidth(token.textStyle(), token.text());
    }

    // =========================
    // Line Factories
    // =========================

    /**
     * Determines if the token should "stick" to the previous token.
     * For example, closing brackets ')' or punctuation ',' should not be separated
     * from the word preceding them.
     */
    private boolean isSticky(String text) {
        if (text == null || text.isEmpty())
            return false;
        char first = text.charAt(0);
        return ",.;:!?)]}".indexOf(first) >= 0;
    }

    /**
     * Determines if the token should "stick" to the next token.
     * For example, opening brackets '(' should not be separated from the word
     * following them.
     */
    private boolean isStickyToNext(String text) {
        if (text == null || text.isEmpty())
            return false;
        char last = text.charAt(text.length() - 1);
        return "([{".indexOf(last) >= 0;
    }

    private @NotNull TextMeasurementSystem textMeasurementSystem() {
        return entityManager.getSystems()
                .getSystem(TextMeasurementSystem.class)
                .orElseThrow(() -> new IllegalStateException("TextMeasurementSystem is required to build block text."));
    }

    private @NotNull TextStyle measurementStyle() {
        if (baseComponents != null) {
            TextStyle style = (TextStyle) baseComponents.get(TextStyle.class);
            if (style != null) {
                return style;
            }
        }
        return textStyle == null ? TextStyle.DEFAULT_STYLE : textStyle;
    }

    /**
     * Creates a new {@link LineTextData} from the accumulated token bodies, caching
     * the measured line width and resolved line metrics so that layout, alignment,
     * and pagination can reuse them without re-measuring.
     *
     * @param bodies        ordered text bodies forming one visual line
     * @param lineWidth     precomputed total width of the line in layout units
     * @param fallbackStyle style used to resolve metrics for bodies without explicit styles
     */
    private void createLineFromBodies(List<TextDataBody> bodies,
                                      double lineWidth,
                                      TextStyle fallbackStyle) {
        log.debug("createLineFromBodies: {}", bodies);
        TextMeasurementSystem.LineMetrics lineMetrics =
                BlockTextLineMetrics.resolveBodiesMetrics(entityManager, bodies, fallbackStyle);
        lines.add(new LineTextData(
                bodies,
                0,
                lineWidth,
                lineMetrics,
                lineMetrics.baselineOffsetFromBottom()));
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

    private ContentSize computeContentSize() {
        Padding padding = entity.getComponent(Padding.class).orElse(Padding.zero());
        var blockTextData = entity.getComponent(BlockTextData.class).orElseThrow();

        if (blockTextData.lines().isEmpty()) {
            return new ContentSize(padding.horizontal(), padding.vertical());
        }

        TextStyle style = entity.getComponent(TextStyle.class).orElse(TextStyle.DEFAULT_STYLE);
        TextMeasurementSystem measurementSystem = textMeasurementSystem();

        var width = blockTextData.lines().stream()
                .mapToDouble(line -> line.hasCachedLineWidth()
                        ? line.lineWidth()
                        : line.width(measurementSystem, style))
                .max()
                .orElse(0.0);

        var spacingOpt = entity.getComponent(Align.class);
        double spacing = spacingOpt.orElse(Align.defaultAlign(0.0)).spacing();

        TextMeasurementSystem.LineMetrics baseMetrics =
                BlockTextLineMetrics.resolveStyleMetrics(entityManager, style);
        List<TextMeasurementSystem.LineMetrics> lineMetrics = blockTextData.lines().stream()
                .map(line -> BlockTextLineMetrics.resolveLineMetrics(entityManager, line, style))
                .toList();

        double high = padding.vertical();
        for (int i = 0; i < lineMetrics.size(); i++) {
            high += lineMetrics.get(i).lineHeight();
            if (i < lineMetrics.size() - 1) {
                high += BlockTextLineMetrics.interLineGap(
                        lineMetrics.get(i),
                        lineMetrics.get(i + 1),
                        baseMetrics,
                        spacing);
            }
        }

        return new ContentSize(width + padding.horizontal(), high);
    }

    // =========================
    // Debug helpers (no logic)
    // =========================

    @Override
    public Entity build() {
        ContentSize contentSize = computeContentSize();
        entity.addComponent(contentSize);
        return registerBuiltEntity();
    }

    private List<TextDataBody> tokenizeMarkdownLine(String explicitLine,
            int explicitLineIndex,
            BulletSpec bullet,
            TextStyle style) {
        if (explicitLine == null || explicitLine.isBlank()) {
            return List.of();
        }

        List<TextDataBody> bodies = new ArrayList<>();

        // Bullet/first-line indent ONLY on the first explicit line
        if (explicitLineIndex == 0) {
            if (blockStrategy.indentFirstLine()) {
                appendPrefixBodies(bodies, bullet == null ? "" : bullet.prefix(), style);
            }
        }

        // Continuation indent for lines after explicit "\n"
        String continuationIndent = "";
        if (explicitLineIndex > 0 && bullet != null && blockStrategy.indentWrappedLines()) {
            continuationIndent = bullet.hardWrapIndent();
        }

        // Remove leading whitespace before parsing markdown (4 spaces / tab becomes a
        // code block)
        int leadWs = countLeadingWhitespace(explicitLine);
        String leadingWs = explicitLine.substring(0, leadWs);
        String rest = explicitLine.substring(leadWs);

        String indent = continuationIndent + leadingWs;
        if (!indent.isEmpty()) {
            bodies.add(new TextDataBody(indent, style));
        }

        // Parse the rest as inline markdown (with list-marker fallback)
        bodies.addAll(tokenizeMarkdownInline(rest, style));
        return bodies;
    }

    /**
     * Inline markdown tokenization with a safety fallback for list markers.
     * Many markdown parsers consider lines starting with "- ", "* ", "+ " to be
     * LIST blocks.
     * When parsing line-by-line (inline), they may return 0 tokens.
     * We preserve the marker as plain text and parse the rest.
     */
    private List<TextDataBody> tokenizeMarkdownInline(String lineToParse, TextStyle style) {
        if (lineToParse == null || lineToParse.isBlank()) {
            return List.of();
        }

        // Detect a markdown list marker at the beginning (after leading spaces)
        int i = 0;
        while (i < lineToParse.length() && lineToParse.charAt(i) == ' ') {
            i++;
        }

        if (i + 1 < lineToParse.length()) {
            char c = lineToParse.charAt(i);
            boolean marker = (c == '-' || c == '*' || c == '+');
            boolean hasSpaceAfter = (i + 1 < lineToParse.length() && lineToParse.charAt(i + 1) == ' ');

            if (marker && hasSpaceAfter) {
                // IMPORTANT: don't keep the trailing space inside the same token as the marker.
                // Some width-calculators trim/ignore trailing spaces, which visually produces
                // "-Honed".
                // Split marker and space into separate tokens.
                String leading = lineToParse.substring(0, i); // spaces before marker
                String rest = lineToParse.substring(i + 2);

                List<TextDataBody> bodies = new ArrayList<>();
                if (!leading.isEmpty()) {
                    bodies.add(new TextDataBody(leading, style));
                }

                bodies.add(new TextDataBody(String.valueOf(lineToParse.charAt(i)), style)); // '-', '*', '+'
                bodies.add(new TextDataBody(" ", style));
                bodies.addAll(markDownParser.getBody(rest, style));
                return bodies;
            }
        }

        // Default: parse entire line as inline markdown
        return markDownParser.getBody(lineToParse, style);
    }

    /**
     * bulletPrefix:
     * - if contains visible chars (e.g. "-", "•", "==="), we render it ONLY on the
     * first explicit line
     * and compute indent width based on font metrics.
     * - if whitespace-only (e.g. " "), we treat it as a plain first-line indent and
     * keep the classic
     * hanging indent for explicit "\n" continuation lines.
     * <p>
     * softWrapIndent: used when a single line wraps by width.
     * hardWrapIndent: used for explicit "\n" continuation lines (lineIndex > 0).
     */
    private record BulletSpec(String prefix, String softWrapIndent, String hardWrapIndent) {

        static BulletSpec from(String bulletOffset, TextMeasurementSystem measurementSystem, TextStyle style) {
            String raw = bulletOffset == null ? "" : bulletOffset;

            boolean hasVisibleChars = raw.chars().anyMatch(ch -> !Character.isWhitespace(ch));

            if (hasVisibleChars) {
                String prefix = normalizeBulletPrefix(raw);
                String indent = computeIndentFromPrefix(measurementSystem, style, prefix);

                // For visible bullets, both soft-wrap and explicit "\n" continuation should
                // align under the content.
                return new BulletSpec(prefix, indent, indent);
            }

            // Whitespace-only: treat as a FIXED left indent for all lines.
            // This is what you want for paragraphs like "Projects" where there is no
            // visible bullet.
            // (If you want a true hanging indent, pass a visible bullet prefix like "- ",
            // "• ", "=== ", etc.)
            String prefix = raw;
            return new BulletSpec(prefix, prefix, prefix);
        }
    }
}

