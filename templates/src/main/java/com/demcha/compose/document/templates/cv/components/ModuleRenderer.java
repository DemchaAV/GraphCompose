package com.demcha.compose.document.templates.cv.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.cv.data.BodyStyle;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvItem;
import com.demcha.compose.document.templates.cv.data.CvRow;
import com.demcha.compose.document.templates.cv.data.ModuleSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;

import java.util.List;

/**
 * Renders a {@link ModuleSection} by lowering it onto the renderers
 * this package already ships.
 *
 * <p>Nothing here draws. Each {@code CvKind} is a rule for turning
 * {@link CvItem}s into the inputs {@link ParagraphRenderer},
 * {@link RowRenderer} and {@link EntryRenderer} already take, which is
 * what makes a runtime-assembled module and a hand-written
 * {@code EntriesSection} carrying the same content lay out the same
 * way — a property the parity suite checks node for node rather than
 * by eye.</p>
 *
 * <p>The lowering is also where a kind's documented indifference
 * happens: {@code ENTRIES} builds its {@link CvEntry} with a blank
 * date, so an item's {@code period} reaches no renderer at all. Every
 * field a kind ignores is dropped here, in one place, rather than by
 * each renderer deciding what to skip.</p>
 */
public final class ModuleRenderer {

    private ModuleRenderer() {
    }

    /**
     * Renders every item of {@code module} into {@code host}, drawing the
     * canonical way.
     *
     * @param host   host section receiving the body
     * @param module the module supplying items, kind, and role
     * @param theme  the active theme supplying palette, typography, and spacing
     */
    public static void render(SectionBuilder host, ModuleSection module, BrandTheme theme) {
        render(host, module, theme, CvRenderKit.defaults());
    }

    /**
     * Renders every item of {@code module} into {@code host}, drawing
     * through {@code kit}.
     *
     * <p>Prefers the kind methods below. A template that implements
     * {@code CvConstructor} should call those (or {@code render} on
     * itself) rather than this overload: this one exists so a kit can
     * restyle the three drawing primitives without re-deciding which
     * fields a kind reads.</p>
     *
     * @param host   host section receiving the body
     * @param module the module supplying items, kind, and role
     * @param theme  the active theme supplying palette, typography, and spacing
     * @param kit    how this template draws paragraphs, rows, and entries
     */
    public static void render(SectionBuilder host, ModuleSection module, BrandTheme theme,
                              CvRenderKit kit) {
        switch (module.kind()) {
            case PARAGRAPH -> paragraph(host, module, theme, kit);
            case BULLETS -> bullets(host, module, theme, kit);
            case BULLETS_STACKED -> bulletsStacked(host, module, theme, kit);
            case INLINE_LIST -> inlineList(host, module, theme, kit);
            case ENTRIES -> entries(host, module, theme, kit);
            case ENTRIES_DATED -> entriesDated(host, module, theme, kit);
        }
    }

    /**
     * Canonical {@link com.demcha.compose.document.templates.cv.data.CvKind#PARAGRAPH}.
     *
     * @param host   host section receiving the body
     * @param module the module
     * @param theme  the active theme
     */
    public static void paragraph(SectionBuilder host, ModuleSection module, BrandTheme theme) {
        paragraph(host, module, theme, CvRenderKit.defaults());
    }

    /**
     * {@link com.demcha.compose.document.templates.cv.data.CvKind#PARAGRAPH} drawn through {@code kit}.
     *
     * @param host   host section receiving the body
     * @param module the module
     * @param theme  the active theme
     * @param kit    drawing primitives
     */
    public static void paragraph(SectionBuilder host, ModuleSection module, BrandTheme theme,
                                 CvRenderKit kit) {
        for (CvItem item : module.items()) {
            paragraph(host, item, theme, kit);
        }
    }

    /**
     * Canonical {@link com.demcha.compose.document.templates.cv.data.CvKind#BULLETS}.
     *
     * @param host   host section receiving the body
     * @param module the module
     * @param theme  the active theme
     */
    public static void bullets(SectionBuilder host, ModuleSection module, BrandTheme theme) {
        bullets(host, module, theme, CvRenderKit.defaults());
    }

    /**
     * {@link com.demcha.compose.document.templates.cv.data.CvKind#BULLETS} drawn through {@code kit}.
     *
     * @param host   host section receiving the body
     * @param module the module
     * @param theme  the active theme
     * @param kit    drawing primitives
     */
    public static void bullets(SectionBuilder host, ModuleSection module, BrandTheme theme,
                               CvRenderKit kit) {
        for (CvItem item : module.items()) {
            bullet(host, item, theme, kit);
        }
    }

    /**
     * Canonical {@link com.demcha.compose.document.templates.cv.data.CvKind#BULLETS_STACKED}.
     *
     * @param host   host section receiving the body
     * @param module the module
     * @param theme  the active theme
     */
    public static void bulletsStacked(SectionBuilder host, ModuleSection module, BrandTheme theme) {
        bulletsStacked(host, module, theme, CvRenderKit.defaults());
    }

    /**
     * {@link com.demcha.compose.document.templates.cv.data.CvKind#BULLETS_STACKED} drawn through {@code kit}.
     *
     * @param host   host section receiving the body
     * @param module the module
     * @param theme  the active theme
     * @param kit    drawing primitives
     */
    public static void bulletsStacked(SectionBuilder host, ModuleSection module, BrandTheme theme,
                                      CvRenderKit kit) {
        List<CvItem> items = module.items();
        for (int i = 0; i < items.size(); i++) {
            stackedBullet(host, items.get(i), theme, kit, i > 0);
        }
    }

    /**
     * Canonical {@link com.demcha.compose.document.templates.cv.data.CvKind#INLINE_LIST}.
     *
     * @param host   host section receiving the body
     * @param module the module
     * @param theme  the active theme
     */
    public static void inlineList(SectionBuilder host, ModuleSection module, BrandTheme theme) {
        inlineList(host, module, theme, CvRenderKit.defaults());
    }

    /**
     * {@link com.demcha.compose.document.templates.cv.data.CvKind#INLINE_LIST} drawn through {@code kit}.
     *
     * @param host   host section receiving the body
     * @param module the module
     * @param theme  the active theme
     * @param kit    drawing primitives
     */
    public static void inlineList(SectionBuilder host, ModuleSection module, BrandTheme theme,
                                  CvRenderKit kit) {
        for (CvItem item : module.items()) {
            inlineList(host, item, theme, kit);
        }
    }

    /**
     * Canonical {@link com.demcha.compose.document.templates.cv.data.CvKind#ENTRIES}.
     *
     * @param host   host section receiving the body
     * @param module the module
     * @param theme  the active theme
     */
    public static void entries(SectionBuilder host, ModuleSection module, BrandTheme theme) {
        entries(host, module, theme, CvRenderKit.defaults());
    }

    /**
     * {@link com.demcha.compose.document.templates.cv.data.CvKind#ENTRIES} drawn through {@code kit}.
     *
     * @param host   host section receiving the body
     * @param module the module
     * @param theme  the active theme
     * @param kit    drawing primitives
     */
    public static void entries(SectionBuilder host, ModuleSection module, BrandTheme theme,
                               CvRenderKit kit) {
        List<CvItem> items = module.items();
        for (int i = 0; i < items.size(); i++) {
            entry(host, items.get(i), "", theme, kit, i > 0);
        }
    }

    /**
     * Canonical {@link com.demcha.compose.document.templates.cv.data.CvKind#ENTRIES_DATED}.
     *
     * @param host   host section receiving the body
     * @param module the module
     * @param theme  the active theme
     */
    public static void entriesDated(SectionBuilder host, ModuleSection module, BrandTheme theme) {
        entriesDated(host, module, theme, CvRenderKit.defaults());
    }

    /**
     * {@link com.demcha.compose.document.templates.cv.data.CvKind#ENTRIES_DATED} drawn through {@code kit}.
     *
     * @param host   host section receiving the body
     * @param module the module
     * @param theme  the active theme
     * @param kit    drawing primitives
     */
    public static void entriesDated(SectionBuilder host, ModuleSection module, BrandTheme theme,
                                    CvRenderKit kit) {
        List<CvItem> items = module.items();
        for (int i = 0; i < items.size(); i++) {
            CvItem item = items.get(i);
            entry(host, item, item.period(), theme, kit, i > 0);
        }
    }

    /**
     * Prose: one paragraph per body line, the title left out (see
     * {@code CvKind.PARAGRAPH}). A bulleted body still bullets — the
     * body style is the author's second choice, independent of kind.
     */
    private static void paragraph(SectionBuilder host, CvItem item, BrandTheme theme,
                                  CvRenderKit kit) {
        for (String line : item.body()) {
            if (item.bodyStyle() == BodyStyle.BULLETS) {
                bulletedLine(host, line, theme.bodyStyle(), theme);
            } else {
                kit.paragraph(host, line, theme);
            }
        }
    }

    /**
     * One line per item — bold label, the description collapsed into a
     * comma-separated run after it. An item with nothing to list renders
     * as its label alone: {@link RowStyle#PLAIN} would leave a colon
     * pointing at nothing.
     */
    private static void inlineList(SectionBuilder host, CvItem item, BrandTheme theme,
                                   CvRenderKit kit) {
        // The title, not linkedTitle: this kind documents that it ignores the
        // link, and RowRenderer bolds a label by wrapping it in markdown
        // markers — which would nest around a link and print as literal
        // asterisks.
        if (item.body().isEmpty()) {
            ParagraphPrimitive.writeBody(host, item.title(), theme.bodyBoldStyle(), theme);
            return;
        }
        kit.row(host, new CvRow(item.title(), String.join(", ", item.body())),
                RowStyle.PLAIN, theme);
    }

    /**
     * A bullet whose description shares its line
     * ({@link RowStyle#BULLETED}).
     *
     * <p>The title goes in unlinked. This row bolds its label by wrapping
     * it in markdown markers, which would nest around link markup and
     * reach the page as literal asterisks; a module whose titles are
     * links wants {@link CvKind#BULLETS_STACKED}, which bolds through the
     * text style and leaves the link intact.</p>
     */
    private static void bullet(SectionBuilder host, CvItem item, BrandTheme theme,
                               CvRenderKit kit) {
        if (item.body().isEmpty()) {
            // PLAIN/BULLETED end the label with a colon, which would point at
            // nothing. A title-only entry is a plain bullet.
            ParagraphPrimitive.writeBulleted(host, item.title(), theme.bodyBoldStyle(),
                    theme.decoration().bulletGlyph(),
                    DocumentInsets.top((float) theme.spacing().paragraphMarginTop()), theme);
            return;
        }
        kit.row(host, new CvRow(item.title(), String.join(" ", item.body())),
                RowStyle.BULLETED, theme);
    }

    /**
     * A bullet whose description is stacked underneath and indented to
     * the title ({@link RowStyle#BULLETED_STACKED}).
     */
    private static void stackedBullet(SectionBuilder host, CvItem item, BrandTheme theme,
                                      CvRenderKit kit, boolean separate) {
        // Stacked items are multi-line blocks, so they get the same gap the
        // dispatcher puts between stacked rows — without it consecutive items
        // read as one.
        if (separate) {
            host.spacer(0, theme.spacing().entrySeparation());
        }
        kit.row(host, new CvRow(linkedTitle(item), ""), RowStyle.BULLETED_STACKED, theme);
        // A bulleted body nests a bullet under the item's own; prose is indented
        // to the title instead of carrying a second glyph.
        String glyph = item.bodyStyle() == BodyStyle.BULLETS
                ? theme.decoration().stackedIndent() + theme.decoration().bulletGlyph()
                : theme.decoration().stackedIndent();
        for (String line : item.body()) {
            ParagraphPrimitive.writeBulleted(host, line, theme.bodyStyle(),
                    glyph, DocumentInsets.zero(), theme);
        }
    }

    /**
     * A timeline entry. The header goes through {@link EntryRenderer}
     * with an empty body so the title / date / subtitle zones are the
     * ones every other entry uses; the description follows underneath
     * in the style the item asked for.
     */
    private static void entry(SectionBuilder host, CvItem item, String date,
                              BrandTheme theme, CvRenderKit kit, boolean separate) {
        if (separate) {
            host.spacer(0, theme.spacing().entrySeparation());
        }
        kit.entry(host,
                new CvEntry(linkedTitle(item), subtitleWithLocation(item), date, ""), theme);
        for (String line : item.body()) {
            if (item.bodyStyle() == BodyStyle.BULLETS) {
                bulletedLine(host, line, theme.bodyStyle(), theme);
            } else {
                ParagraphPrimitive.writeBody(host, line, theme.bodyStyle(), theme);
            }
        }
    }

    private static void bulletedLine(SectionBuilder host, String line,
                                     DocumentTextStyle style, BrandTheme theme) {
        ParagraphPrimitive.writeBulleted(host, line, style,
                theme.decoration().bulletGlyph(),
                DocumentInsets.top((float) theme.spacing().paragraphMarginTop()), theme);
    }

    /**
     * The title, wrapped in markdown link syntax when the item carries a
     * link. Every renderer here already routes titles through the shared
     * markdown helper, so this needs no separate link path.
     *
     * <p>A title containing a bracket is left alone. The markdown link
     * pattern's label admits no brackets, so wrapping
     * {@code "Ledger [v2]"} would match nothing and print the whole
     * construction — URL included — as visible text. Either the title
     * already carries its own {@code [text](url)}, which renders as the
     * link it is, or it is prose with a bracket in it and reaches the
     * page as written.</p>
     */
    private static String linkedTitle(CvItem item) {
        if (item.link() == null
                || item.title().indexOf('[') >= 0
                || item.title().indexOf(']') >= 0) {
            return item.title();
        }
        return "[" + item.title() + "](" + item.link().url() + ")";
    }

    /**
     * The italic line under an entry title: subtitle and location joined
     * when both are present, whichever exists when only one is, blank
     * when neither — no separator left dangling.
     */
    private static String subtitleWithLocation(CvItem item) {
        if (item.subtitle().isBlank()) {
            return item.location();
        }
        if (item.location().isBlank()) {
            return item.subtitle();
        }
        return item.subtitle() + " · " + item.location();
    }
}
