package com.demcha.compose.document.templates.invoice.presets;

import com.demcha.compose.document.dsl.ParagraphBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.templates.core.identity.ContactUri;
import com.demcha.compose.document.templates.data.invoice.InvoiceNotesBlock;
import com.demcha.compose.document.templates.data.invoice.InvoicePaymentBlock;
import com.demcha.compose.document.templates.data.invoice.StructuredInvoiceData;

import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.ACCENT_PRIMARY;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.BODY;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.BODY_LINK;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.EMPHASIS_FILL;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.HAIRLINE;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.NOTES_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.NOTICE_ACCENT;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.NOTICE_BOLD;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.PAYMENT_WIDTH;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.SMALL;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.SMALL_BOLD;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingStyles.SMALL_ITALIC;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingText.link;
import static com.demcha.compose.document.templates.invoice.presets.ConsultingText.nonBreakingPhone;

/**
 * The closing band of the Consulting Invoice: where the money goes on the
 * left, and the notes with the due-by chip on the right.
 */
final class ConsultingClosing {

    private ConsultingClosing() {
    }

    /** Payment information: the bank badge, the labelled fields, the instruction. */
    static void renderPayment(SectionBuilder section, StructuredInvoiceData data) {
        InvoicePaymentBlock payment = data.payment();
        ConsultingMasthead.sectionHeading(section, payment.heading());
        // The band is as tall as the fields it holds (15pt each), never
        // shorter than the badge standing beside them.
        double bankBandHeight = Math.max(38.0, payment.fields().size() * 15.0);
        section.spacing(3)
                .addContainer(container -> container
                        .name("BankInformationBand")
                        .rectangle(PAYMENT_WIDTH, bankBandHeight)
                        .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                        .centerLeft(bankBadge())
                        .position(bankDetails(payment), 58, 0, LayerAlign.CENTER_LEFT))
                .addParagraph(paragraph -> paragraph
                        .text(payment.instruction())
                        .textStyle(SMALL_ITALIC)
                        .lineSpacing(1.30)
                        .margin(DocumentInsets.top(4)));
    }

    private static DocumentNode bankBadge() {
        return new ShapeContainerBuilder()
                .name("BankBadge")
                .circle(38)
                .fillColor(EMPHASIS_FILL)
                .center(ConsultingIcons.icon(ConsultingIcons.BANK, 19.5))
                .build();
    }

    private static DocumentNode bankDetails(InvoicePaymentBlock payment) {
        SectionBuilder details = new SectionBuilder().name("BankDetails").spacing(0);
        double width = PAYMENT_WIDTH - 61;
        for (InvoicePaymentBlock.Field field : payment.fields()) {
            details.addContainer(container -> container
                    .name("BankField")
                    .rectangle(width, 15)
                    .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                    .centerLeft(ConsultingMasthead.paragraph(field.label(), SMALL_BOLD))
                    .position(ConsultingMasthead.paragraph(field.value(), SMALL),
                            76, 0, LayerAlign.CENTER_LEFT));
        }
        return details.build();
    }

    /**
     * The notes column: the paragraphs, with the query channels linked
     * where they appear in the prose, and the due-by chip beneath them.
     */
    static void renderNotes(SectionBuilder section, StructuredInvoiceData data) {
        InvoiceNotesBlock notes = data.notes();
        section.accentLeft(HAIRLINE, 1.0).padding(0, 0, 0, 24).spacing(3);
        ConsultingMasthead.sectionHeading(section, notes.heading());
        for (String paragraphText : notes.paragraphs()) {
            section.addParagraph(paragraph -> {
                paragraph.lineSpacing(1.30).margin(DocumentInsets.zero());
                renderNoteParagraph(paragraph, paragraphText, notes);
            });
        }
        renderDueNotice(section, data.payment());
    }

    /**
     * Links the contact channels inside a note paragraph. The channels are
     * matched by value rather than by markup, so the prose stays plain
     * content: a paragraph that mentions neither is drawn as plain text.
     */
    private static void renderNoteParagraph(ParagraphBuilder paragraph,
                                            String text,
                                            InvoiceNotesBlock notes) {
        int emailIndex = notes.contactEmail().isEmpty() ? -1 : text.indexOf(notes.contactEmail());
        if (emailIndex < 0) {
            paragraph.text(text).textStyle(BODY);
            return;
        }
        String before = text.substring(0, emailIndex);
        int afterEmail = emailIndex + notes.contactEmail().length();
        int phoneIndex = notes.contactPhone().isEmpty()
                ? -1 : text.indexOf(notes.contactPhone(), afterEmail);
        if (phoneIndex < 0) {
            String after = text.substring(afterEmail);
            paragraph.rich(rich -> rich
                    .style(before, BODY)
                    .with(notes.contactEmail(), BODY_LINK,
                            link("mailto:" + notes.contactEmail()))
                    .style(after, BODY));
            return;
        }
        String between = text.substring(afterEmail, phoneIndex);
        String after = text.substring(phoneIndex + notes.contactPhone().length());
        paragraph.rich(rich -> rich
                .style(before, BODY)
                .with(notes.contactEmail(), BODY_LINK, link("mailto:" + notes.contactEmail()))
                .style(between, BODY)
                .with(nonBreakingPhone(notes.contactPhone()), BODY,
                        ContactUri.telLink(notes.contactPhone()))
                .style(after, BODY));
    }

    /** The due-by chip: a calendar badge and the notice, on the quiet fill. */
    private static void renderDueNotice(SectionBuilder section, InvoicePaymentBlock payment) {
        double width = NOTES_WIDTH - 24;
        section.addContainer(container -> container
                .name("PaymentTerms")
                .roundedRect(width, 34, 4)
                .fillColor(EMPHASIS_FILL)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .position(calendarBadge(), 8, 0, LayerAlign.CENTER_LEFT)
                .position(dueNotice(payment), 42, 0, LayerAlign.CENTER_LEFT));
    }

    private static DocumentNode calendarBadge() {
        return new ShapeContainerBuilder()
                .name("CalendarBadge")
                .circle(23)
                .stroke(DocumentStroke.of(ACCENT_PRIMARY, 0.9))
                .center(ConsultingIcons.icon(ConsultingIcons.CALENDAR, 12.5))
                .build();
    }

    private static DocumentNode dueNotice(InvoicePaymentBlock payment) {
        String notice = payment.dueNotice();
        String emphasis = payment.dueNoticeEmphasis();
        ParagraphBuilder paragraph = new ParagraphBuilder()
                .name("DueNotice")
                .lineSpacing(1.1)
                .margin(DocumentInsets.zero());
        int index = emphasis.isEmpty() ? -1 : notice.indexOf(emphasis);
        if (index < 0) {
            return paragraph.text(notice).textStyle(NOTICE_BOLD).build();
        }
        String before = notice.substring(0, index);
        String after = notice.substring(index + emphasis.length());
        return paragraph.rich(rich -> rich
                        .style(before, NOTICE_BOLD)
                        .style(emphasis, NOTICE_ACCENT)
                        .style(after, NOTICE_BOLD))
                .build();
    }
}
