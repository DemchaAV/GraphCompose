package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.EllipseBuilder;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.ShapeBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;

import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.LOCKUP_LEFT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.LOCKUP_RULE_THICKNESS;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.LOCKUP_RULE_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.LOCKUP_RULE_Y;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.MONOGRAM_BOTTOM;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.MONOGRAM_BOTTOM_Y;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.MONOGRAM_TOP;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.MONOGRAM_TOP_Y;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ON_ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ORNAMENT_ARCH_HEIGHT_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ORNAMENT_ARCH_LEFT_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ORNAMENT_ARCH_WIDTH_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ORNAMENT_DISC_LEFT_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ORNAMENT_DISC_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ORNAMENT_DISC_TOP_FACTOR;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ORNAMENT_SPRIG_LEFT_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ORNAMENT_SPRIG_TOP_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ORNAMENT_SPRIG_WIDTH_RATIO;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.ORNAMENT_TINT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAGE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAGE_MARGIN_LEFT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.PAGE_MARGIN_TOP;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SIDEBAR_BRAND_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SIDEBAR_STUB_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.SIDEBAR_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TAGLINE;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TAGLINE_Y;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.TRACK_WORDMARK;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.WORDMARK;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioStyles.WORDMARK_Y;
import static com.demcha.compose.document.templates.invoice.presets.LumaStudioWidgets.tracked;

/**
 * The sidebar: one node with one negative margin.
 *
 * <p>The body flow's left margin excludes the column, so the lockup and the
 * ornament are composed into a single overflowing stub pulled back over the
 * margin — both of its offsets are the page margins themselves. The
 * terracotta brand block is content rather than a page background, painted
 * above the ornament in the stub's z-order, because the ornament's disc is a
 * circle far larger than the column and the block has to cover its top.</p>
 */
final class LumaStudioSidebar {

    private LumaStudioSidebar() {
    }

    static DocumentNode render(InvoiceBrand brand) {
        return new ShapeContainerBuilder()
                .name("Sidebar")
                .rectangle(SIDEBAR_WIDTH, SIDEBAR_STUB_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .margin(new DocumentInsets(
                        -PAGE_MARGIN_TOP, 0, PAGE_MARGIN_TOP - SIDEBAR_STUB_HEIGHT,
                        -PAGE_MARGIN_LEFT))
                .position(ornament(), 0, SIDEBAR_BRAND_HEIGHT, LayerAlign.TOP_LEFT, 0)
                .position(brandBlock(), 0, 0, LayerAlign.TOP_LEFT, 1)
                .position(lockup(brand), 0, 0, LayerAlign.TOP_LEFT, 2)
                .build();
    }

    private static DocumentNode brandBlock() {
        return new ShapeBuilder()
                .name("BrandBlock")
                .size(SIDEBAR_WIDTH, SIDEBAR_BRAND_HEIGHT)
                .fillColor(ACCENT)
                .margin(DocumentInsets.zero())
                .build();
    }

    /**
     * The lockup: the two monogram lines, a short rule, the tracked wordmark
     * and the tagline, each seated at its own measured height.
     */
    private static DocumentNode lockup(InvoiceBrand brand) {
        return new ShapeContainerBuilder()
                .name("BrandLockup")
                .rectangle(SIDEBAR_WIDTH, SIDEBAR_BRAND_HEIGHT)
                .clipPolicy(ClipPolicy.CLIP_PATH)
                .position(seated(brand.monogramTop(), MONOGRAM_TOP),
                        LOCKUP_LEFT, MONOGRAM_TOP_Y, LayerAlign.TOP_LEFT)
                .position(seated(brand.monogramBottom(), MONOGRAM_BOTTOM),
                        LOCKUP_LEFT, MONOGRAM_BOTTOM_Y, LayerAlign.TOP_LEFT)
                .position(new LineBuilder()
                                .name("LockupRule")
                                .horizontal(LOCKUP_RULE_WIDTH)
                                .thickness(LOCKUP_RULE_THICKNESS)
                                .color(ON_ACCENT)
                                .margin(DocumentInsets.zero())
                                .build(),
                        LOCKUP_LEFT, LOCKUP_RULE_Y, LayerAlign.TOP_LEFT)
                .position(tracked("Wordmark", brand.name(), WORDMARK,
                                TRACK_WORDMARK, ACCENT, TextVerticalAlign.TOP),
                        LOCKUP_LEFT, WORDMARK_Y, LayerAlign.TOP_LEFT)
                .position(seated(brand.tagline(), TAGLINE),
                        LOCKUP_LEFT, TAGLINE_Y, LayerAlign.TOP_LEFT)
                .build();
    }

    private static DocumentNode seated(String text, DocumentTextStyle style) {
        return new ParagraphBuilder()
                .text(text)
                .textStyle(style)
                .align(TextAlign.LEFT)
                .verticalAlign(TextVerticalAlign.TOP)
                .lineSpacing(0)
                .margin(DocumentInsets.zero())
                .build();
    }

    /**
     * The art below the brand block: a tinted quarter-disc, an arch at the
     * foot, and the sprig between them.
     *
     * <p>The container clips to its bounds rather than to its path: the disc
     * is a circle far wider than the column, and a rectangular container's
     * path clip does not cut its positioned layers.</p>
     */
    private static DocumentNode ornament() {
        double columnHeight = PAGE.height() - SIDEBAR_BRAND_HEIGHT;
        double discDiameter = PAGE.width() * ORNAMENT_DISC_RATIO;
        double archWidth = PAGE.width() * ORNAMENT_ARCH_WIDTH_RATIO;

        DocumentNode disc = new EllipseBuilder()
                .name("OrnamentDisc")
                .circle(discDiameter)
                .fillColor(ORNAMENT_TINT)
                .margin(DocumentInsets.zero())
                .build();
        DocumentNode arch = new ShapeBuilder()
                .name("OrnamentArch")
                .size(archWidth, PAGE.height() * ORNAMENT_ARCH_HEIGHT_RATIO)
                .cornerRadius(DocumentCornerRadius.top(archWidth / 2.0))
                .fillColor(ACCENT)
                .margin(DocumentInsets.zero())
                .build();

        return new ShapeContainerBuilder()
                .name("SidebarOrnament")
                .rectangle(SIDEBAR_WIDTH, columnHeight)
                .clipPolicy(ClipPolicy.CLIP_BOUNDS)
                .position(disc, PAGE.width() * ORNAMENT_DISC_LEFT_RATIO,
                        discDiameter * ORNAMENT_DISC_TOP_FACTOR, LayerAlign.TOP_LEFT)
                .position(arch, PAGE.width() * ORNAMENT_ARCH_LEFT_RATIO, 0,
                        LayerAlign.BOTTOM_LEFT)
                .position(LumaStudioIcons.icon(LumaStudioIcons.SPRIG,
                                PAGE.width() * ORNAMENT_SPRIG_WIDTH_RATIO),
                        PAGE.width() * ORNAMENT_SPRIG_LEFT_RATIO,
                        PAGE.height() * ORNAMENT_SPRIG_TOP_RATIO, LayerAlign.TOP_LEFT)
                .build();
    }
}
