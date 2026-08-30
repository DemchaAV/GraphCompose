package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ImageBuilder;
import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.data.invoice.InvoiceBrand;
import com.demcha.compose.document.templates.data.invoice.InvoiceContactBlock;
import com.demcha.compose.document.templates.data.invoice.InvoiceMasthead;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;

import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.ACCENT_PRIMARY;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.BODY_ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.BRAND_LOGO_BOX_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.BRAND_MARK;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.BRAND_NAME;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.BRAND_QUALIFIER;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.CONTACT_ROW_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.INVOICE_TITLE;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.INVOICE_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.METADATA_ROW_HEIGHT;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.SMALL;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.SECTION_HEADING;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.SMALL_BOLD;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.SUPPLIER_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingText.link;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingText.telUri;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingText.tracked;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingText.websiteUri;

/**
 * The masthead of the Consulting Invoice: the sender's lockup and contact
 * channels on the left, the document title and its metadata on the right.
 *
 * <p>Both halves are shape containers with anchored children rather than
 * rows, because a row is refused inside a row cell and the masthead itself
 * is one row of the page flow.</p>
 */
final class ConsultingMasthead {

    private ConsultingMasthead() {
    }

    /** The sender's half: brand lockup, then one band per contact channel. */
    static void renderSupplier(SectionBuilder section, StructuredInvoiceData data) {
        InvoiceBrand brand = data.brand();
        InvoiceContactBlock supplier = data.supplier();

        section.spacing(2);
        if (brand.hasLogo()) {
            section.add(logoLockup(brand));
        } else {
            renderTextLockup(section, brand);
        }
        // A channel the sender did not supply gets no band: an empty band
        // would print a mark with nothing beside it, and an empty value has
        // no address to link to.
        String address = String.join("\n", supplier.addressLines());
        if (!address.isBlank()) {
            contactBand(section, ConsultingIcons.LOCATION, 10.0, address,
                    CONTACT_ROW_HEIGHT + 5.0, null);
        }
        if (!supplier.phone().isBlank()) {
            contactBand(section, ConsultingIcons.PHONE, 9.2, supplier.phone(),
                    CONTACT_ROW_HEIGHT, telUri(supplier.phone()));
        }
        if (!supplier.email().isBlank()) {
            contactBand(section, ConsultingIcons.EMAIL, 9.2, supplier.email(),
                    CONTACT_ROW_HEIGHT, "mailto:" + supplier.email());
        }
        if (!supplier.website().isBlank()) {
            contactBand(section, ConsultingIcons.WEBSITE, 9.4, supplier.website(),
                    CONTACT_ROW_HEIGHT, websiteUri(supplier.website()));
        }
    }

    /**
     * The logo in its box. Height-only sizing lets the engine derive the
     * width from the image's own metadata, so a wider or narrower logo
     * stays on the same baseline as the contact bands below it.
     */
    private static DocumentNode logoLockup(InvoiceBrand brand) {
        DocumentNode logo = new ImageBuilder()
                .name("BrandLogo")
                .source(brand.logo())
                .height(BRAND_LOGO_BOX_HEIGHT)
                .build();
        return new ShapeContainerBuilder()
                .name("BrandLogoBox")
                .rectangle(SUPPLIER_WIDTH, BRAND_LOGO_BOX_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .centerLeft(logo)
                .build();
    }

    /**
     * The wordmark-only lockup a document without a logo falls back to: the
     * brand's initial as a mark, the name beside it, the qualifier tracked
     * between rules, and the tagline.
     */
    private static void renderTextLockup(SectionBuilder section, InvoiceBrand brand) {
        String mark = brand.name().isBlank() ? "" : brand.name().substring(0, 1);
        section.addParagraph(paragraph -> paragraph
                        .rich(rich -> rich
                                .style(mark, BRAND_MARK)
                                .space()
                                .style(brand.name(), BRAND_NAME))
                        .lineSpacing(1.0)
                        .margin(DocumentInsets.zero()))
                .addParagraph(paragraph -> paragraph
                        .text(brand.qualifier().isBlank()
                                ? "" : "—  " + tracked(brand.qualifier()) + "  —")
                        .textStyle(BRAND_QUALIFIER)
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.zero()))
                .addParagraph(paragraph -> paragraph
                        .text(brand.tagline())
                        .textStyle(BODY_ACCENT)
                        .align(TextAlign.CENTER)
                        .margin(DocumentInsets.symmetric(5, 0)));
    }

    /** One contact line: the channel mark, and the value anchored beside it. */
    private static void contactBand(SectionBuilder section,
                                    String iconName,
                                    double iconSize,
                                    String text,
                                    double height,
                                    String linkUri) {
        DocumentNode contactText = linkUri == null
                ? paragraph(text, SMALL)
                : linkedParagraph(text, SMALL, linkUri);
        section.addContainer(container -> container
                .name("ContactBand")
                .rectangle(SUPPLIER_WIDTH, height)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .centerLeft(ConsultingIcons.icon(iconName, iconSize))
                .position(contactText, 23, 0, LayerAlign.CENTER_LEFT));
    }

    /** The document half: the tracked title, then one band per metadata row. */
    static void renderInvoiceHeader(SectionBuilder section, StructuredInvoiceData data) {
        InvoiceMasthead masthead = data.masthead();
        section.accentLeft(HAIRLINE, 1.1)
                .padding(0, 0, 0, 24)
                .spacing(1)
                .addParagraph(paragraph -> paragraph
                        .text(tracked(masthead.title()))
                        .textStyle(INVOICE_TITLE)
                        .margin(DocumentInsets.bottom(8)));
        for (InvoiceMasthead.Entry entry : masthead.entries()) {
            metadataBand(section, entry);
        }
    }

    /** One metadata row: the label at the left, the value at the 48% mark. */
    private static void metadataBand(SectionBuilder section, InvoiceMasthead.Entry entry) {
        double width = INVOICE_WIDTH - 24;
        section.addContainer(container -> container
                .name("MetadataBand")
                .rectangle(width, METADATA_ROW_HEIGHT)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .centerLeft(paragraph(entry.label(), SMALL_BOLD))
                .position(paragraph(entry.value(), entry.emphasized() ? BODY_ACCENT : BODY),
                        width * 0.48,
                        0,
                        LayerAlign.CENTER_LEFT));
    }

    /** A plain left-aligned paragraph node on the shared line spacing. */
    static DocumentNode paragraph(String text, DocumentTextStyle style) {
        return new ParagraphBuilder()
                .text(text)
                .textStyle(style)
                .align(TextAlign.LEFT)
                .lineSpacing(1.15)
                .margin(DocumentInsets.zero())
                .build();
    }

    private static DocumentNode linkedParagraph(String text, DocumentTextStyle style, String uri) {
        return new ParagraphBuilder()
                .text(text)
                .textStyle(style)
                .align(TextAlign.LEFT)
                .link(link(uri))
                .lineSpacing(1.15)
                .margin(DocumentInsets.zero())
                .build();
    }

    /**
     * The heading every lower region opens with: tracked capitals over a
     * short accent rule.
     */
    static void sectionHeading(SectionBuilder section, String heading) {
        if (heading.isBlank()) {
            // No heading, no rule: an orphan rule reads as a divider.
            return;
        }
        section.addParagraph(paragraph -> paragraph
                        .text(tracked(heading))
                        .textStyle(SECTION_HEADING)
                        .lineSpacing(1.0)
                        .margin(DocumentInsets.zero()))
                .addLine(line -> line
                        .horizontal(30)
                        .thickness(1.2)
                        .color(ACCENT_PRIMARY)
                        .margin(DocumentInsets.symmetric(4, 0)));
    }

}
