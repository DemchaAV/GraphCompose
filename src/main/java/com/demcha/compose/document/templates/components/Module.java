package com.demcha.compose.document.templates.components;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.InlineRun;
import com.demcha.compose.document.node.ListMarker;
import com.demcha.compose.document.node.ListNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.blocks.Block;
import com.demcha.compose.document.templates.blocks.BulletListBlock;
import com.demcha.compose.document.templates.blocks.IndentedBlock;
import com.demcha.compose.document.templates.blocks.KeyValueBlock;
import com.demcha.compose.document.templates.blocks.MultiParagraphBlock;
import com.demcha.compose.document.templates.blocks.NumberedListBlock;
import com.demcha.compose.document.templates.blocks.ParagraphBlock;
import com.demcha.compose.document.templates.themes.Spacing;
import com.demcha.compose.document.theme.BusinessTheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Templates v2 module — a section heading paired with a body
 * {@link Block}.
 *
 * <p>Module is the smallest unit a preset places into a layout slot.
 * It owns the rendering decision for translating a {@link Block} into
 * the engine's {@link DocumentNode} primitives, using the active
 * {@link BusinessTheme} and {@link Spacing} tokens.</p>
 *
 * <p>For now Module supports a single {@link Style} variant —
 * {@link #headingFlat(BusinessTheme)} — which produces a flat coloured
 * heading flush with the left edge above the body. Additional style
 * variants ({@code headingBoxed}, {@code headingUnderline}) will be
 * added in subsequent preset migrations as concrete needs emerge.</p>
 */
public final class Module {

    private final String name;
    private final String title;
    private final Block body;
    private final Style style;

    private Module(String name, String title, Block body, Style style) {
        this.name = Objects.requireNonNull(name, "name");
        this.title = Objects.requireNonNull(title, "title");
        this.body = Objects.requireNonNull(body, "body");
        this.style = Objects.requireNonNull(style, "style");
    }

    /**
     * Creates a module with the given identifier, heading title, body
     * block, and rendering style.
     *
     * @param name  semantic name of the module — used for snapshot
     *              stability and layout graph paths (must not be null)
     * @param title heading text rendered above the body (must not be
     *              null; may be empty to suppress the heading)
     * @param body  body block (must not be null)
     * @param style heading-rendering style (use {@link #headingFlat})
     * @return new module
     * @throws NullPointerException if any argument is null
     */
    public static Module of(String name, String title, Block body, Style style) {
        return new Module(name, title, body, style);
    }

    /**
     * Returns the module's semantic name.
     *
     * @return module name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the module's heading text.
     *
     * @return module title
     */
    public String title() {
        return title;
    }

    /**
     * Returns the module's body block.
     *
     * @return body block
     */
    public Block body() {
        return body;
    }

    /**
     * Composes the module into a single {@link DocumentNode} suitable
     * for adding to a layout slot.
     *
     * @param theme   active business theme — provides typography and
     *                palette tokens
     * @param spacing active spacing tokens
     * @return container node holding the heading and body
     * @throws NullPointerException if either argument is null
     */
    public DocumentNode compose(BusinessTheme theme, Spacing spacing) {
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(spacing, "spacing");

        List<DocumentNode> children = new ArrayList<>(2);

        if (!title.isEmpty()) {
            children.add(headingParagraph(theme, spacing));
        }
        children.add(renderBody(theme, spacing));

        return new ContainerNode(
                name,
                children,
                /* spacing      */ 0.0,
                /* padding      */ DocumentInsets.zero(),
                /* margin       */ DocumentInsets.zero(),
                /* fillColor    */ null,
                /* stroke       */ null,
                /* cornerRadius */ null,
                /* borders      */ null);
    }

    private ParagraphNode headingParagraph(BusinessTheme theme, Spacing spacing) {
        DocumentTextStyle headingStyle = style.headingStyle != null
                ? style.headingStyle
                : theme.text().h2();
        return new ParagraphNode(
                name + ".heading",
                title,
                /* inlineRuns      */ null,
                headingStyle,
                TextAlign.LEFT,
                /* lineSpacing     */ 0.0,
                /* bulletOffset    */ "",
                /* indentStrategy  */ null,
                /* link            */ null,
                /* bookmark        */ null,
                /* padding         */ DocumentInsets.zero(),
                /* margin          */ new DocumentInsets(
                        style.marginAbove,
                        0.0,
                        style.marginBelow,
                        0.0),
                /* autoSize        */ null);
    }

    private DocumentTextStyle bodyStyle(BusinessTheme theme) {
        return style.bodyStyle != null ? style.bodyStyle : theme.text().body();
    }

    private DocumentNode renderBody(BusinessTheme theme, Spacing spacing) {
        if (body instanceof ParagraphBlock p) {
            return renderParagraph(p, theme, spacing);
        }
        if (body instanceof BulletListBlock b) {
            return renderList(b.items(), ListMarker.bullet(), "bullet", theme, spacing);
        }
        if (body instanceof NumberedListBlock n) {
            return renderList(n.items(), ListMarker.custom("1."), "numbered", theme, spacing);
        }
        if (body instanceof MultiParagraphBlock m) {
            return renderMultiParagraph(m, theme, spacing);
        }
        if (body instanceof IndentedBlock i) {
            return renderIndented(i, theme, spacing);
        }
        if (body instanceof KeyValueBlock k) {
            return renderKeyValue(k, theme, spacing);
        }
        throw new IllegalStateException("Unsupported block: " + body);
    }

    private ParagraphNode renderParagraph(ParagraphBlock block, BusinessTheme theme, Spacing spacing) {
        DocumentTextStyle bodyStyle = bodyStyle(theme);
        List<InlineRun> runs = MarkdownText.parse(block.text(), bodyStyle);
        return new ParagraphNode(
                name + ".body",
                /* text       */ "",
                /* inlineRuns */ runs,
                bodyStyle,
                block.align(),
                spacing.lineSpacing(),
                "",
                null,
                null,
                null,
                /* padding */ leftIndent(spacing),
                DocumentInsets.zero(),
                null);
    }

    private ListNode renderList(List<String> items, ListMarker marker, String suffix,
                                BusinessTheme theme, Spacing spacing) {
        return new ListNode(
                name + "." + suffix,
                items,
                marker,
                bodyStyle(theme),
                TextAlign.LEFT,
                spacing.lineSpacing(),
                spacing.listItemSpacing(),
                /* continuationIndent */ "",
                /* normalizeMarkers   */ true,
                /* padding */ leftIndent(spacing),
                DocumentInsets.zero());
    }

    private DocumentNode renderMultiParagraph(MultiParagraphBlock block, BusinessTheme theme, Spacing spacing) {
        DocumentTextStyle bodyStyle = bodyStyle(theme);
        List<DocumentNode> paragraphs = new ArrayList<>(block.paragraphs().size());
        for (int i = 0; i < block.paragraphs().size(); i++) {
            String source = block.paragraphs().get(i);
            paragraphs.add(new ParagraphNode(
                    name + ".paragraph[" + i + "]",
                    "",
                    MarkdownText.parse(source, bodyStyle),
                    bodyStyle,
                    TextAlign.LEFT,
                    spacing.lineSpacing(),
                    "",
                    null,
                    null,
                    null,
                    DocumentInsets.zero(),
                    DocumentInsets.zero(),
                    null));
        }
        return new ContainerNode(
                name + ".body",
                paragraphs,
                /* spacing */ spacing.paragraphSpacing(),
                /* padding */ leftIndent(spacing),
                DocumentInsets.zero(),
                null,
                null,
                null,
                null);
    }

    private DocumentNode renderIndented(IndentedBlock block, BusinessTheme theme, Spacing spacing) {
        DocumentTextStyle bodyStyle = bodyStyle(theme);
        // Title is the same family as body but rendered bold so it
        // visually separates from the indented body underneath.
        DocumentTextStyle titleStyle = DocumentTextStyle.builder()
                .fontName(bodyStyle.fontName())
                .size(bodyStyle.size())
                .decoration(com.demcha.compose.document.style.DocumentTextDecoration.BOLD)
                .color(bodyStyle.color())
                .build();
        List<DocumentNode> entries = new ArrayList<>(block.items().size() * 2);
        for (int i = 0; i < block.items().size(); i++) {
            IndentedBlock.Item item = block.items().get(i);
            // Title — bold weight via h3 style; markdown parsed for inline emphasis.
            entries.add(new ParagraphNode(
                    name + ".item[" + i + "].title",
                    "",
                    MarkdownText.parse(item.title(), titleStyle),
                    titleStyle,
                    TextAlign.LEFT,
                    spacing.lineSpacing(),
                    "",
                    null,
                    null,
                    null,
                    DocumentInsets.zero(),
                    DocumentInsets.zero(),
                    null));
            // Body — regular paragraph nested one half-step beyond the
            // module body indent so the title visually sits one level above.
            entries.add(new ParagraphNode(
                    name + ".item[" + i + "].body",
                    "",
                    MarkdownText.parse(item.body(), bodyStyle),
                    bodyStyle,
                    TextAlign.LEFT,
                    spacing.lineSpacing(),
                    "",
                    null,
                    null,
                    null,
                    /* padding */ new DocumentInsets(0.0, 0.0, 0.0, spacing.bodyIndent()),
                    DocumentInsets.zero(),
                    null));
        }
        return new ContainerNode(
                name + ".body",
                entries,
                spacing.listItemSpacing(),
                /* padding */ leftIndent(spacing),
                DocumentInsets.zero(),
                null,
                null,
                null,
                null);
    }

    private DocumentNode renderKeyValue(KeyValueBlock block, BusinessTheme theme, Spacing spacing) {
        DocumentTextStyle bodyStyle = bodyStyle(theme);
        // Key uses the bold variant of the body font so a "Key: value"
        // line shows the key emphasised on the same baseline as the
        // value, without depending on the legacy markdown pipeline.
        List<DocumentNode> rows = new ArrayList<>(block.entries().size());
        for (int i = 0; i < block.entries().size(); i++) {
            KeyValueBlock.Entry entry = block.entries().get(i);
            // Synthesize the key as a markdown bold prefix so all the
            // emphasis logic flows through the shared MarkdownText
            // parser; this also means an entry whose key already
            // contains markdown is rendered correctly.
            String composed = "**" + entry.key() + ":** " + entry.value();
            rows.add(new ParagraphNode(
                    name + ".entry[" + i + "]",
                    "",
                    MarkdownText.parse(composed, bodyStyle),
                    bodyStyle,
                    TextAlign.LEFT,
                    spacing.lineSpacing(),
                    "",
                    null,
                    null,
                    null,
                    DocumentInsets.zero(),
                    DocumentInsets.zero(),
                    null));
        }
        return new ContainerNode(
                name + ".body",
                rows,
                spacing.lineSpacing(),
                /* padding */ leftIndent(spacing),
                DocumentInsets.zero(),
                null,
                null,
                null,
                null);
    }

    private static DocumentInsets leftIndent(Spacing spacing) {
        return new DocumentInsets(0.0, 0.0, 0.0, spacing.bodyIndent());
    }

    /**
     * Returns the canonical "flat heading" rendering style — heading
     * text in the active {@code h2} style, body text in the active
     * {@code body} style, flush with the left edge, with default
     * vertical margins driven by the active spacing tokens.
     *
     * <p>Callers can adjust the margins or override either style after
     * the fact:</p>
     *
     * <pre>{@code
     * Module.headingFlat(theme)
     *     .marginAbove(spacing.sectionTitleAbove())
     *     .marginBelow(spacing.sectionTitleBelow())
     *     .bodyStyle(customSmallerBody)
     * }</pre>
     *
     * @param theme active business theme
     * @return new style instance
     * @throws NullPointerException if {@code theme} is null
     */
    public static Style headingFlat(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Style(
                theme.text().h2(),
                theme.text().body(),
                /* above */ 8.0,
                /* below */ 4.0);
    }

    /**
     * Module heading-rendering style.
     *
     * @param headingStyle text style applied to the heading line; if
     *                     null the active theme's {@code h2} is used
     * @param bodyStyle    text style applied to the body content
     *                     (paragraph, list, indented body, key-value);
     *                     if null the active theme's {@code body} is
     *                     used
     * @param marginAbove  vertical margin above the heading
     * @param marginBelow  vertical margin below the heading (separates
     *                     it from the body)
     */
    public static record Style(
            DocumentTextStyle headingStyle,
            DocumentTextStyle bodyStyle,
            double marginAbove,
            double marginBelow) {

        /**
         * Returns a copy with the given margin above the heading.
         *
         * @param value margin above in points; non-negative finite
         * @return new style instance
         */
        public Style marginAbove(double value) {
            return new Style(headingStyle, bodyStyle, value, marginBelow);
        }

        /**
         * Returns a copy with the given margin below the heading.
         *
         * @param value margin below in points; non-negative finite
         * @return new style instance
         */
        public Style marginBelow(double value) {
            return new Style(headingStyle, bodyStyle, marginAbove, value);
        }

        /**
         * Returns a copy with the given body style override.
         *
         * @param value text style for body content; null falls back to
         *              the active theme's {@code body} style
         * @return new style instance
         */
        public Style bodyStyle(DocumentTextStyle value) {
            return new Style(headingStyle, value, marginAbove, marginBelow);
        }
    }
}
