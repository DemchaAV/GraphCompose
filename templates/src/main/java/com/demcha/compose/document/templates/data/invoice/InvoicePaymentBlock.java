package com.demcha.compose.document.templates.data.invoice;

import java.util.List;
import java.util.Objects;

/**
 * The payment-information block of a structured invoice: where to send the
 * money, and by when.
 *
 * <p>Distinct from the narrative model's
 * {@link InvoiceData#paymentTerms()}, a list of prose bullets: the fields
 * here are labelled values printed as a table (bank, account name, sort
 * code or BSB, account number, payment reference), because a payer reads
 * them one field at a time. The labels are content — they differ by
 * banking system.</p>
 *
 * @param heading           the block heading
 *                          (e.g. {@code "PAYMENT INFORMATION"})
 * @param fields            the labelled payment fields, in print order
 * @param instruction       the sentence under the fields
 * @param dueNotice         the short due-by notice set apart from the rest
 * @param dueNoticeEmphasis the run inside {@code dueNotice} that carries
 *                          the emphasis — usually the term itself, as in
 *                          {@code "30 days"}; empty leaves the notice
 *                          evenly set
 * @param accountHolder     who the money is paid to, set above the fields
 *                          where a design names the account before listing
 *                          it; blank when absent
 * @param signOff           the closing line a design sets beside the due
 *                          notice, usually a thank-you; blank when absent
 */
public record InvoicePaymentBlock(
        String heading,
        List<Field> fields,
        String instruction,
        String dueNotice,
        String dueNoticeEmphasis,
        String accountHolder,
        String signOff) {

    /**
     * Normalizes optional fields and freezes the field list.
     */
    public InvoicePaymentBlock {
        heading = Objects.requireNonNullElse(heading, "");
        fields = List.copyOf(Objects.requireNonNullElse(fields, List.of()));
        instruction = Objects.requireNonNullElse(instruction, "");
        dueNotice = Objects.requireNonNullElse(dueNotice, "");
        dueNoticeEmphasis = Objects.requireNonNullElse(dueNoticeEmphasis, "");
        accountHolder = Objects.requireNonNullElse(accountHolder, "");
        signOff = Objects.requireNonNullElse(signOff, "");
    }

    /**
     * Backward-compatible constructor for callers that predate the account
     * holder and the sign-off.
     *
     * @param heading           the block heading
     * @param fields            the labelled payment fields, in print order
     * @param instruction       the sentence under the fields
     * @param dueNotice         the short due-by notice
     * @param dueNoticeEmphasis the emphasised run inside the notice
     */
    public InvoicePaymentBlock(String heading, List<Field> fields, String instruction,
                               String dueNotice, String dueNoticeEmphasis) {
        this(heading, fields, instruction, dueNotice, dueNoticeEmphasis, "", "");
    }

    /**
     * One labelled payment field.
     *
     * @param label the field label
     * @param value the field value
     */
    public record Field(String label, String value) {

        /**
         * Normalizes optional fields to empty strings.
         */
        public Field {
            label = Objects.requireNonNullElse(label, "");
            value = Objects.requireNonNullElse(value, "");
        }
    }
}
