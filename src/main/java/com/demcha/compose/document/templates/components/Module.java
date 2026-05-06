package com.demcha.compose.document.templates.components;

import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
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

    private DocumentNode renderBody(BusinessTheme theme, Spacing spacing) {
        return switch (body) {
            case ParagraphBlock p -> renderParagraph(p, theme, spacing);
            case BulletListBlock b -> renderList(b.items(), ListMarker.bullet(),
                    "bullet", theme, spacing);
            case NumberedListBlock n -> renderList(n.items(), ListMarker.custom("1."),
                    "numbered", theme, spacing);
            case MultiParagraphBlock m -> renderMultiParagraph(m, theme, spacing);
            case IndentedBlock i -> renderIndented(i, theme, spacing);
            case KeyValueBlock k -> renderKeyValue(k, theme, spacing);
        };
    }

    private ParagraphNode renderParagraph(ParagraphBlock block, BusinessTheme theme, Spacing spacing) {
        return new ParagraphNode(
                name + ".body",
                block.text(),
                null,
                theme.text().body(),
                block.align(),
                spacing.lineSpacing(),
                "",
                null,
                null,
                null,
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                null);
    }

    private ListNode renderList(List<String> items, ListMarker marker, String suffix,
                                BusinessTheme theme, Spacing spacing) {
        return new ListNode(
                name + "." + suffix,
                items,
                marker,
                theme.text().body(),
                TextAlign.LEFT,
                spacing.lineSpacing(),
                spacing.listItemSpacing(),
                /* continuationIndent */ "",
                /* normalizeMarkers   */ true,
                DocumentInsets.zero(),
                DocumentInsets.zero());
    }

    private DocumentNode renderMultiParagraph(MultiParagraphBlock block, BusinessTheme theme, Spacing spacing) {
        List<DocumentNode> paragraphs = new ArrayList<>(block.paragraphs().size());
        for (int i = 0; i < block.paragraphs().size(); i++) {
            paragraphs.add(new ParagraphNode(
                    name + ".paragraph[" + i + "]",
                    block.paragraphs().get(i),
                    null,
                    theme.text().body(),
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
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                null,
                null,
                null,
                null);
    }

    private DocumentNode renderIndented(IndentedBlock block, BusinessTheme theme, Spacing spacing) {
        List<DocumentNode> entries = new ArrayList<>(block.items().size() * 2);
        for (int i = 0; i < block.items().size(); i++) {
            IndentedBlock.Item item = block.items().get(i);
            // Title — bold weight via h3 style (close to body but emphasised)
            entries.add(new ParagraphNode(
                    name + ".item[" + i + "].title",
                    item.title(),
                    null,
                    theme.text().h3(),
                    TextAlign.LEFT,
                    spacing.lineSpacing(),
                    "",
                    null,
                    null,
                    null,
                    DocumentInsets.zero(),
                    DocumentInsets.zero(),
                    null));
            // Body — regular paragraph indented from the left
            entries.add(new ParagraphNode(
                    name + ".item[" + i + "].body",
                    item.body(),
                    null,
                    theme.text().body(),
                    TextAlign.LEFT,
                    spacing.lineSpacing(),
                    "",
                    null,
                    null,
                    null,
                    /* padding */ new DocumentInsets(0.0, 0.0, 0.0, 12.0),
                    DocumentInsets.zero(),
                    null));
        }
        return new ContainerNode(
                name + ".body",
                entries,
                spacing.listItemSpacing(),
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                null,
                null,
                null,
                null);
    }

    private DocumentNode renderKeyValue(KeyValueBlock block, BusinessTheme theme, Spacing spacing) {
        List<DocumentNode> rows = new ArrayList<>(block.entries().size());
        for (int i = 0; i < block.entries().size(); i++) {
            KeyValueBlock.Entry entry = block.entries().get(i);
            // Render as "Key: value" — bold key handled by the engine via
            // h3 style on the leading run; for v1 simplicity we emit a
            // single paragraph carrying the concatenation. Inline run
            // styling will land in a follow-up B.3.b refinement.
            rows.add(new ParagraphNode(
                    name + ".entry[" + i + "]",
                    entry.key() + ": " + entry.value(),
                    null,
                    theme.text().body(),
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
                DocumentInsets.zero(),
                DocumentInsets.zero(),
                null,
                null,
                null,
                null);
    }

    /**
     * Returns the canonical "flat heading" rendering style — heading
     * text in the active {@code h2} style, flush with the left edge,
     * with default vertical margins driven by the active spacing
     * tokens.
     *
     * <p>Callers can adjust the margins after the fact:</p>
     *
     * <pre>{@code
     * Module.headingFlat(theme)
     *     .marginAbove(spacing.sectionTitleAbove())
     *     .marginBelow(spacing.sectionTitleBelow())
     * }</pre>
     *
     * @param theme active business theme
     * @return new style instance
     * @throws NullPointerException if {@code theme} is null
     */
    public static Style headingFlat(BusinessTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Style(theme.text().h2(), /* above */ 8.0, /* below */ 4.0);
    }

    /**
     * Module heading-rendering style.
     *
     * @param headingStyle text style applied to the heading line; if
     *                     null the active theme's {@code h2} is used
     * @param marginAbove  vertical margin above the heading
     * @param marginBelow  vertical margin below the heading (separates
     *                     it from the body)
     */
    public static record Style(
            DocumentTextStyle headingStyle,
            double marginAbove,
            double marginBelow) {

        /**
         * Returns a copy with the given margin above the heading.
         *
         * @param value margin above in points; non-negative finite
         * @return new style instance
         */
        public Style marginAbove(double value) {
            return new Style(headingStyle, value, marginBelow);
        }

        /**
         * Returns a copy with the given margin below the heading.
         *
         * @param value margin below in points; non-negative finite
         * @return new style instance
         */
        public Style marginBelow(double value) {
            return new Style(headingStyle, marginAbove, value);
        }
    }
}
