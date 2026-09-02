package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.LineBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.HorizontalAlign;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.RowVerticalAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.node.TextVerticalAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentRowColumn;
import com.demcha.compose.document.style.DocumentTextIndent;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.core.text.TextOrnaments;
import com.demcha.compose.document.templates.data.proposal.ProposalBrand;

import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.ACCENT;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.BADGE_DIAMETER;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.BADGE_GAP;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.BADGE_GLYPH;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.BODY;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.BODY_LEADING;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.DOC_LABEL;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.DOC_LABEL_WIDTH;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.GAP_HEADING;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.GAP_ROW;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.HAIRLINE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.INK;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.LOGO_SIZE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.MONOGRAM;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.RULE;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.SECTION_HEADING;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.WORDMARK;
import static com.demcha.compose.document.templates.proposal.presets.NorthlineStyles.WORDMARK_WIDTH;

/**
 * The building blocks the Northline bands share: the page header, the
 * icon-badged section heading, the teal bullet, and the small node helpers.
 *
 * <p>Constraint notes carried over from the ported template: the engine
 * refuses a row nested inside another row's cell, so the header flattens
 * its columns, and the section heading is a shape container hosting two
 * anchored children rather than a row — half its uses sit inside a row
 * cell. The heading container carries no fill and no margin (a margin
 * would displace its paint); the gap above it comes from the enclosing
 * flow's spacing.</p>
 */
final class NorthlineWidgets {

    private NorthlineWidgets() {
    }

    /**
     * Logo square, wordmark, document label and the hairline that closes
     * the header. One flat row: the logo is a shape container owning its
     * monogram, the wordmark and the label are sections, and the weight(1)
     * column between them holds the two ends apart.
     */
    static void renderHeader(SectionBuilder header, ProposalBrand brand) {
        header.spacing(0);
        header.addRow("HeaderRow", row -> {
            row.verticalAlign(RowVerticalAlign.CENTER)
                    .gap(LOGO_SIZE * 0.25)
                    // Not auto(): a section's natural width is the width it
                    // is offered, so two auto columns each claim the whole
                    // row. Both ends are measured strips instead.
                    .columns(DocumentRowColumn.fixed(LOGO_SIZE),
                            DocumentRowColumn.fixed(WORDMARK_WIDTH),
                            DocumentRowColumn.weight(1),
                            DocumentRowColumn.fixed(DOC_LABEL_WIDTH));
            row.add(logoMark(brand.monogram()));
            row.addSection("Wordmark", mark -> {
                mark.spacing(0);
                mark.addParagraph(p -> p.text(brand.nameLine1()).textStyle(WORDMARK));
                mark.addParagraph(p -> p.text(brand.nameLine2()).textStyle(WORDMARK));
            });
            // A zero-width spacer: the row distributes by columns, and the
            // weight(1) column above already holds the ends apart.
            row.addSpacer(0);
            row.addSection("DocumentLabel", label -> {
                label.spacing(0);
                label.addParagraph(p -> p
                        .text(TextOrnaments.spacedUpper(brand.documentLabel()))
                        .textStyle(DOC_LABEL)
                        .align(TextAlign.RIGHT));
                // addAligned, not addLine: a section stacks children at the
                // content width with no child-alignment option, and the rule
                // sits flush RIGHT under the label.
                label.addAligned(HorizontalAlign.RIGHT, new LineBuilder()
                        .name("DocumentLabelRule")
                        .horizontal(BADGE_DIAMETER * 1.3)
                        .thickness(2.2)
                        .color(ACCENT)
                        .margin(DocumentInsets.top(6))
                        .build());
            });
        });
        header.addLine(line -> line
                .fill()
                .thickness(HAIRLINE)
                .color(RULE)
                .margin(DocumentInsets.top(GAP_HEADING)));
    }

    /** The navy square and the monogram it owns. */
    private static DocumentNode logoMark(String monogram) {
        return new ShapeContainerBuilder()
                .name("LogoMark")
                .rectangle(LOGO_SIZE, LOGO_SIZE)
                .fillColor(INK)
                .center(paragraph("Monogram", monogram, MONOGRAM, TextAlign.CENTER))
                .build();
    }

    /**
     * The heading every section opens with: a teal badge owning a white
     * glyph, and the title vertically centred beside it.
     *
     * @param title     the heading text, already uppercase in the data
     * @param iconToken the icon token from the data
     * @param width     the width of the column the heading sits in
     */
    static DocumentNode sectionHeading(String title, String iconToken, double width) {
        return new ShapeContainerBuilder()
                .name("SectionHeading")
                .rectangle(width, BADGE_DIAMETER)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(badge(iconToken), 0, 0, LayerAlign.CENTER_LEFT)
                .position(new ParagraphBuilder()
                        .name("SectionHeadingTitle")
                        .text(title)
                        .textStyle(SECTION_HEADING)
                        .verticalAlign(TextVerticalAlign.CENTER)
                        .build(), BADGE_DIAMETER + BADGE_GAP, 0, LayerAlign.CENTER_LEFT)
                .build();
    }

    /**
     * The teal disc and the glyph it owns. A blank token leaves the disc
     * plain — the container still needs a layer, so an empty paragraph
     * stands in for the glyph.
     */
    private static DocumentNode badge(String iconToken) {
        ShapeContainerBuilder badge = new ShapeContainerBuilder()
                .name("SectionBadge")
                .circle(BADGE_DIAMETER)
                .fillColor(ACCENT);
        if (iconToken.isBlank()) {
            badge.center(new ParagraphBuilder()
                    .name("BadgeGlyph").text("").textStyle(SECTION_HEADING).build());
        } else {
            badge.center(icon(iconToken, BADGE_GLYPH));
        }
        return badge.build();
    }

    /**
     * A bullet item: an inline teal dot followed by the text, with a
     * hanging indent so wrapped lines clear the dot. The dot is an inline
     * shape run rather than a bullet glyph, so it keeps its own colour and
     * cannot be lost to font coverage.
     */
    static void renderBullet(SectionBuilder host, String text) {
        host.addParagraph(p -> p
                .dot(4.2, ACCENT)
                .inlineText("   " + text, BODY)
                .bulletOffset("   ")
                // Wrapped lines align under the first line text, not the dot.
                .indentStrategy(DocumentTextIndent.FROM_SECOND_LINE)
                .lineSpacing(BODY_LEADING)
                .margin(0f, 0f, (float) GAP_ROW, 0f));
    }

    /** A named, vertically centred paragraph node. */
    static DocumentNode paragraph(String name,
                                  String text,
                                  DocumentTextStyle textStyle,
                                  TextAlign align) {
        return new ParagraphBuilder()
                .name(name)
                .text(text)
                .textStyle(textStyle)
                .align(align)
                .verticalAlign(TextVerticalAlign.CENTER)
                .build();
    }

    /** One packaged icon at a given size, as a node. */
    static DocumentNode icon(String token, double size) {
        return new ImageBuilder()
                .name("Icon-" + token)
                .source(NorthlineIcons.image(token))
                .size(size, size)
                .build();
    }

    /** The same icon, configured in place on a row's own image builder. */
    static void configureIcon(ImageBuilder image, String token, double size) {
        image.name("Icon-" + token)
                .source(NorthlineIcons.image(token))
                .size(size, size);
    }
}
