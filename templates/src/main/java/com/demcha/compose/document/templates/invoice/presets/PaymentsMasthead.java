package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.PathBuilder;
import com.demcha.compose.document.dsl.RowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.style.DocumentBorders;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceMasthead;

import java.util.ArrayList;
import java.util.List;

import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ACCENT_BAR_H;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ACCENT_BAR_OFFSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ACCENT_BAR_W;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BAND_BOTTOM_PX;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BAND_H;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BAND_LAVENDER;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BAND_LAVENDER_QUAD;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BAND_LEFT_PX;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BAND_NAVY_QUAD;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BAND_RIGHT_PX;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BAND_W;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BODY_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BRAND_LOCKUP_H;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.BRAND_LOCKUP_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.DIVIDER;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.HEADER_RULE_OVERHANG_L;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.HEADER_RULE_OVERHANG_R;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.HEADER_RULE_W;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.HEADER_STACK_BOTTOM_PX;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.INK;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.ISSUER_NAME_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.LINE_BOX;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.MARGIN_T;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.META_COL_X;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.META_LABEL_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.META_LABEL_W;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.META_PITCH;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.META_VALUE_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.META_VALUE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.RULE_MED;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.RULE_SOFT;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TEXT_INSET;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TEXT_TOP_BEARING;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TITLE_DROP;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.TITLE_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.WORDMARK_SIZE;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.gap;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.px;
import static com.demcha.compose.document.templates.invoice.presets.PaymentsStyles.style;

/**
 * The header: the diagonal band, the lockup beside the title, the issuer block
 * against the metadata grid, and the rule that closes them.
 *
 * <h2>One stack, because two of its three parts are page furniture</h2>
 *
 * <p>The band bleeds past the top margin to the paper's edge and the accent bar
 * stands left of the content box, so neither belongs to the flow. Both are
 * positioned by offset from the stack's top-left; only the header content is a
 * layer.</p>
 *
 * <h2>The lockup box is the design's, whatever fills it</h2>
 *
 * <p>The design's mark measures 136 × 55.3 design px. A document that brings a
 * logo has it drawn to that height, so a wider or narrower mark keeps the
 * design's optical weight; a document that brings only a name has the name set
 * as a wordmark instead. Either way the title beside it does not move, because
 * the title is a left-anchored member of the metadata column rather than a
 * right-anchored member of the page.</p>
 */
final class PaymentsMasthead {

    private PaymentsMasthead() {
    }

    /**
     * Draws the header and its closing rule.
     *
     * @param page     the page flow
     * @param brand    the lockup
     * @param supplier who is invoicing
     * @param masthead the title and the metadata rows
     */
    static void render(PageFlowBuilder page, InvoiceBrand brand, InvoiceContactBlock supplier,
                       InvoiceMasthead masthead) {
        page.addLayerStack(stack -> stack
                .name("MastheadStack")
                .padding(DocumentInsets.zero())
                .position(band(), px(BAND_LEFT_PX - 51), -MARGIN_T, LayerAlign.TOP_LEFT, 0)
                .position(accentBar(), ACCENT_BAR_OFFSET, 0, LayerAlign.TOP_LEFT, 0)
                .layer(headerContent(brand, supplier, masthead), LayerAlign.TOP_LEFT, 1));
        renderHeaderRule(page);
    }

    /** The lavender quadrilateral, then the navy one over it. */
    private static DocumentNode band() {
        SectionBuilder band = new SectionBuilder();
        band.name("HeaderBand").spacing(0).padding(DocumentInsets.zero());
        band.addLayerStack(stack -> stack
                .name("HeaderBandLayers")
                .layer(quad("BandLavender", BAND_LAVENDER, BAND_LAVENDER_QUAD),
                        LayerAlign.TOP_LEFT, 0)
                .layer(quad("BandNavy", INK, BAND_NAVY_QUAD), LayerAlign.TOP_LEFT, 1));
        return band.build();
    }

    /**
     * One quadrilateral of the band. The corners arrive in page design pixels
     * and are normalised here — {@link #nx} and {@link #ny} are the only place
     * the bottom-left origin of a path's coordinate space is dealt with.
     */
    private static DocumentNode quad(String name, DocumentColor fill, double[][] corners) {
        PathBuilder path = new PathBuilder();
        path.name(name).size(BAND_W, BAND_H).fillColor(fill);
        path.moveTo(nx(corners[0][0]), ny(corners[0][1]));
        for (int i = 1; i < corners.length; i++) {
            path.lineTo(nx(corners[i][0]), ny(corners[i][1]));
        }
        return path.closePath().build();
    }

    private static double nx(double pagePx) {
        return (pagePx - BAND_LEFT_PX) / (BAND_RIGHT_PX - BAND_LEFT_PX);
    }

    /** Page y runs down; a path's y runs up from its own bottom edge. */
    private static double ny(double pagePx) {
        return 1.0 - pagePx / BAND_BOTTOM_PX;
    }

    private static DocumentNode accentBar() {
        SectionBuilder holder = new SectionBuilder();
        holder.name("AccentBarHolder").spacing(0).padding(DocumentInsets.zero());
        holder.addLine(line -> line
                .name("HeaderAccentBar")
                .vertical(ACCENT_BAR_H)
                .thickness(ACCENT_BAR_W)
                .color(ACCENT));
        return holder.build();
    }

    private static DocumentNode headerContent(InvoiceBrand brand, InvoiceContactBlock supplier,
                                              InvoiceMasthead masthead) {
        SectionBuilder content = new SectionBuilder();
        content.name("HeaderContent").spacing(0).padding(DocumentInsets.zero());
        content.addRow("Masthead", row -> renderLockupRow(row, brand, masthead));
        content.addRow("HeaderSplit", row -> {
            // The two cells of the split do not share a top edge: the metadata
            // block starts 11 px below the issuer name's box.
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.TOP)
                    // 12 px by the design's own ink gap, less the 2.5 the title's
                    // line box gives back.
                    .margin(new DocumentInsets(px(12 - 2.5), 0, 0, 0))
                    .columns(DocumentRowColumn.fixed(META_COL_X), DocumentRowColumn.weight(1));
            row.addSection("IssuerBlock", cell -> renderIssuer(cell, supplier));
            row.addSection("InvoiceMeta", cell -> {
                cell.spacing(0).padding(new DocumentInsets(px(11), 0, 0, 0));
                renderMeta(cell, masthead.entries());
            });
        });
        return content.build();
    }

    /**
     * TOP, not CENTER: the title's line box is much taller than the lockup, so
     * centring would push the lockup down the row it is supposed to set the top
     * edge of.
     */
    private static void renderLockupRow(RowBuilder row, InvoiceBrand brand,
                                        InvoiceMasthead masthead) {
        row.spacing(0)
                .verticalAlign(RowVerticalAlign.TOP)
                .margin(new DocumentInsets(px(46 - 28), 0, 0, 0))
                .columns(DocumentRowColumn.fixed(META_COL_X), DocumentRowColumn.weight(1));
        row.addSection("BrandLockup", cell -> {
            cell.spacing(0).padding(new DocumentInsets(0, 0, 0, BRAND_LOCKUP_INSET));
            if (brand.logo() != null) {
                cell.add(new ImageBuilder()
                        .name("BrandLogo")
                        .source(brand.logo())
                        .height(BRAND_LOCKUP_H)
                        .build());
            } else if (!brand.name().isBlank()) {
                cell.addParagraph(p -> p
                        .name("BrandWordmark")
                        .text(brand.name())
                        .textStyle(style(WORDMARK_SIZE, ACCENT, DocumentTextDecoration.BOLD)));
            }
        });
        row.addParagraph(p -> p
                .name("InvoiceTitle")
                .text(masthead.title())
                .textStyle(style(TITLE_SIZE, INK, DocumentTextDecoration.BOLD))
                .margin(new DocumentInsets(TITLE_DROP, 0, 0, 0)));
    }

    /**
     * The issuer's name, its address, and its registrations — the last set off
     * by exactly two line pitches. That gap is authored, not drift.
     */
    private static void renderIssuer(SectionBuilder cell, InvoiceContactBlock supplier) {
        cell.spacing(0).padding(new DocumentInsets(0, 0, 0, TEXT_INSET));
        cell.addParagraph(p -> p
                .name("IssuerName")
                .text(supplier.legalName())
                .textStyle(style(ISSUER_NAME_SIZE, INK, DocumentTextDecoration.BOLD)));
        cell.addSection("IssuerAddress", lines -> {
            lines.spacing(gap(21.33, BODY_SIZE))
                    .margin(new DocumentInsets(px(172 - 140) - LINE_BOX * ISSUER_NAME_SIZE
                            + TEXT_TOP_BEARING * (ISSUER_NAME_SIZE - BODY_SIZE), 0, 0, 0));
            PaymentsWidgets.textLines(lines, "IssuerAddressLine", supplier.addressLines(),
                    BODY_SIZE, BODY);
        });
        List<String> registrations = registrationLines(supplier);
        if (!registrations.isEmpty()) {
            cell.addSection("IssuerRegistration", lines -> {
                lines.spacing(gap(21.0, BODY_SIZE))
                        .margin(new DocumentInsets(gap(2 * 21.33, BODY_SIZE), 0, 0, 0));
                PaymentsWidgets.textLines(lines, "IssuerRegistrationLine", registrations,
                        BODY_SIZE, BODY);
            });
        }
    }

    /** The two labelled registrations a supplier may carry, as printed lines. */
    private static List<String> registrationLines(InvoiceContactBlock supplier) {
        List<String> lines = new ArrayList<>();
        addRegistration(lines, supplier.registrationLabel(), supplier.registrationNumber());
        addRegistration(lines, supplier.taxRegistrationLabel(), supplier.taxRegistrationNumber());
        return lines;
    }

    private static void addRegistration(List<String> lines, String label, String number) {
        if (number == null || number.isBlank()) {
            return;
        }
        lines.add(label == null || label.isBlank() ? number : label + " " + number);
    }

    /**
     * Two stacks side by side rather than a row per entry, so the divider
     * between them is one continuous border. It is the value column's left
     * border, not a node: that is why it begins with the first row and ends with
     * the last rather than running the block's full box.
     *
     * <p>The two stacks carry different spacings because their type sizes differ
     * — a bold label and a regular value have different line boxes — and the
     * shared pitch is what makes their rows line up.</p>
     */
    private static void renderMeta(SectionBuilder cell, List<InvoiceMasthead.Entry> entries) {
        PaymentsWidgets.layeredRow(cell, "MetaGrid", row -> {
            row.spacing(0)
                    .verticalAlign(RowVerticalAlign.TOP)
                    .columns(DocumentRowColumn.fixed(META_LABEL_W), DocumentRowColumn.weight(1));
            row.addSection("MetaLabels", labels -> {
                labels.spacing(gap(META_PITCH, META_LABEL_SIZE));
                int index = 0;
                for (InvoiceMasthead.Entry entry : entries) {
                    String name = "MetaLabel" + (++index);
                    labels.addParagraph(p -> p
                            .name(name)
                            .text(entry.label())
                            .textStyle(style(META_LABEL_SIZE, INK, DocumentTextDecoration.BOLD)));
                }
            });
            row.addSection("MetaValues", values -> {
                values.spacing(gap(META_PITCH, META_VALUE_SIZE))
                        .borders(DocumentBorders.left(DocumentStroke.of(DIVIDER, RULE_MED)))
                        .padding(new DocumentInsets(0, 0, 0, META_VALUE_INSET));
                int index = 0;
                for (InvoiceMasthead.Entry entry : entries) {
                    String name = "MetaValue" + (++index);
                    values.addParagraph(p -> p
                            .name(name)
                            .text(entry.value())
                            .textStyle(entry.emphasized()
                                    ? style(META_LABEL_SIZE, ACCENT, DocumentTextDecoration.BOLD)
                                    : style(META_VALUE_SIZE, BODY,
                                            DocumentTextDecoration.DEFAULT)));
                }
            });
        });
    }

    /**
     * Wider than the content box on both sides — it is furniture for the header
     * block rather than a divider between two pieces of content, which is why it
     * overhangs. Its top margin is negative because the band, not the content,
     * is the tallest layer of the stack above it.
     */
    private static void renderHeaderRule(PageFlowBuilder page) {
        page.addLine(line -> line
                .name("HeaderRule")
                .horizontal(HEADER_RULE_W)
                .thickness(RULE_MED)
                .color(RULE_SOFT)
                .margin(new DocumentInsets(
                        px(359 - HEADER_STACK_BOTTOM_PX),
                        -HEADER_RULE_OVERHANG_R,
                        px(378 - 361),
                        -HEADER_RULE_OVERHANG_L)));
    }
}
