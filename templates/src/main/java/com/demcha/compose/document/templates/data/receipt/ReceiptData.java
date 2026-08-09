package com.demcha.compose.document.templates.data.receipt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Display-oriented payment receipt input — a transfer confirmation, a direct
 * debit advice, a card payment receipt.
 *
 * <p>Everything here is display text the issuing system already decided.
 * Amounts arrive formatted with their currency, dates arrive in the locale
 * the issuer prints them in, and account identifiers arrive masked or not as
 * the issuer chose. A receipt is a restatement of a settled fact, so the
 * template's job is to lay that fact out, never to compute or reformat
 * it.</p>
 *
 * <p>The optional blocks — timeline, notes, support lines, verification URL
 * — are what separates a receipt somebody keeps from one that only proves a
 * number. Leave any of them empty and the preset drops the block rather than
 * rendering an empty heading.</p>
 *
 * @param documentTitle    heading of the document, for example
 *                         {@code Transfer confirmation}
 * @param issuerName       the institution that issued the receipt
 * @param generatedOn      when this copy was produced, already formatted
 * @param reference        the issuer's own reference for the payment, shown
 *                         beside the title
 * @param amountLabel      caption above the amount, for example
 *                         {@code Amount sent}
 * @param amount           the amount, formatted with its currency
 * @param amountCaption    one line under the amount naming the instrument or
 *                         the counterparty
 * @param status           the payment's status and tone
 * @param summaryFields    the two-to-four facts shown beside the amount
 *                         (value date, operation date, scheme)
 * @param payerLabel       caption over the paying side, for example
 *                         {@code From}
 * @param payer            the paying side
 * @param beneficiaryLabel caption over the receiving side, for example
 *                         {@code To}
 * @param beneficiary      the receiving side
 * @param detailGroups     titled blocks of label/value rows
 * @param timeline         the payment's steps, oldest first; empty to omit
 * @param notes            free-text notes shown under the details
 * @param verificationUrl  URL encoded into the footer QR code; empty to omit
 * @param verificationText one line explaining what the QR code leads to
 * @param supportLines     how to reach the issuer, one entry per line
 * @param legalNote        the small print block at the foot of the page
 */
public record ReceiptData(
        String documentTitle,
        String issuerName,
        String generatedOn,
        String reference,
        String amountLabel,
        String amount,
        String amountCaption,
        ReceiptStatus status,
        List<ReceiptField> summaryFields,
        String payerLabel,
        ReceiptParty payer,
        String beneficiaryLabel,
        ReceiptParty beneficiary,
        List<ReceiptFieldGroup> detailGroups,
        List<ReceiptEvent> timeline,
        List<String> notes,
        String verificationUrl,
        String verificationText,
        List<String> supportLines,
        String legalNote) {

    /**
     * Normalizes optional text, defaults the two party captions, and freezes
     * every collection input.
     */
    public ReceiptData {
        documentTitle = Objects.requireNonNullElse(documentTitle, "Payment receipt");
        issuerName = Objects.requireNonNullElse(issuerName, "");
        generatedOn = Objects.requireNonNullElse(generatedOn, "");
        reference = Objects.requireNonNullElse(reference, "");
        amountLabel = Objects.requireNonNullElse(amountLabel, "Amount");
        amount = Objects.requireNonNullElse(amount, "");
        amountCaption = Objects.requireNonNullElse(amountCaption, "");
        status = Objects.requireNonNullElse(status, new ReceiptStatus("", null));
        summaryFields = List.copyOf(Objects.requireNonNullElse(summaryFields, List.of()));
        payerLabel = blankTo(payerLabel, "From");
        beneficiaryLabel = blankTo(beneficiaryLabel, "To");
        detailGroups = List.copyOf(Objects.requireNonNullElse(detailGroups, List.of()));
        timeline = List.copyOf(Objects.requireNonNullElse(timeline, List.of()));
        notes = List.copyOf(Objects.requireNonNullElse(notes, List.of()));
        verificationUrl = Objects.requireNonNullElse(verificationUrl, "");
        verificationText = Objects.requireNonNullElse(verificationText, "");
        supportLines = List.copyOf(Objects.requireNonNullElse(supportLines, List.of()));
        legalNote = Objects.requireNonNullElse(legalNote, "");
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * Reports whether a paying side was supplied.
     *
     * @return {@code true} when the payer carries a name or any row
     */
    public boolean hasPayer() {
        return isPresent(payer);
    }

    /**
     * Reports whether a receiving side was supplied.
     *
     * @return {@code true} when the beneficiary carries a name or any row
     */
    public boolean hasBeneficiary() {
        return isPresent(beneficiary);
    }

    private static boolean isPresent(ReceiptParty party) {
        return party != null
                && (!party.name().isBlank() || !party.fields().isEmpty() || !party.addressLines().isEmpty());
    }

    /**
     * Starts a fluent receipt data builder.
     *
     * @return receipt data builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for complete receipt content.
     */
    public static final class Builder {
        private String documentTitle;
        private String issuerName;
        private String generatedOn;
        private String reference;
        private String amountLabel;
        private String amount;
        private String amountCaption;
        private ReceiptStatus status;
        private final List<ReceiptField> summaryFields = new ArrayList<>();
        private String payerLabel;
        private ReceiptParty payer;
        private String beneficiaryLabel;
        private ReceiptParty beneficiary;
        private final List<ReceiptFieldGroup> detailGroups = new ArrayList<>();
        private final List<ReceiptEvent> timeline = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private String verificationUrl;
        private String verificationText;
        private final List<String> supportLines = new ArrayList<>();
        private String legalNote;

        private Builder() {
        }

        /**
         * Sets the document heading.
         *
         * @param documentTitle heading text
         * @return this builder
         */
        public Builder documentTitle(String documentTitle) {
            this.documentTitle = documentTitle;
            return this;
        }

        /**
         * Sets the issuing institution's name.
         *
         * @param issuerName issuer name
         * @return this builder
         */
        public Builder issuerName(String issuerName) {
            this.issuerName = issuerName;
            return this;
        }

        /**
         * Sets when this copy was produced.
         *
         * @param generatedOn formatted generation date
         * @return this builder
         */
        public Builder generatedOn(String generatedOn) {
            this.generatedOn = generatedOn;
            return this;
        }

        /**
         * Sets the issuer's reference for the payment.
         *
         * @param reference reference text
         * @return this builder
         */
        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        /**
         * Sets the hero amount and the caption above it.
         *
         * @param label  caption above the amount
         * @param amount amount formatted with its currency
         * @return this builder
         */
        public Builder amount(String label, String amount) {
            this.amountLabel = label;
            this.amount = amount;
            return this;
        }

        /**
         * Sets the line under the amount.
         *
         * @param amountCaption instrument or counterparty line
         * @return this builder
         */
        public Builder amountCaption(String amountCaption) {
            this.amountCaption = amountCaption;
            return this;
        }

        /**
         * Sets the payment status.
         *
         * @param status status label and tone
         * @return this builder
         */
        public Builder status(ReceiptStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the payment status from a label and a tone.
         *
         * @param label the issuer's status word
         * @param tone  how that word reads
         * @return this builder
         */
        public Builder status(String label, ReceiptStatusTone tone) {
            return status(new ReceiptStatus(label, tone));
        }

        /**
         * Appends a fact shown beside the amount.
         *
         * @param label field caption
         * @param value field value
         * @return this builder
         */
        public Builder summaryField(String label, String value) {
            summaryFields.add(ReceiptField.of(label, value));
            return this;
        }

        /**
         * Sets the paying side.
         *
         * @param label caption over the block
         * @param spec  party builder callback
         * @return this builder
         */
        public Builder payer(String label, Consumer<ReceiptParty.Builder> spec) {
            this.payerLabel = label;
            this.payer = build(spec);
            return this;
        }

        /**
         * Sets the receiving side.
         *
         * @param label caption over the block
         * @param spec  party builder callback
         * @return this builder
         */
        public Builder beneficiary(String label, Consumer<ReceiptParty.Builder> spec) {
            this.beneficiaryLabel = label;
            this.beneficiary = build(spec);
            return this;
        }

        private static ReceiptParty build(Consumer<ReceiptParty.Builder> spec) {
            ReceiptParty.Builder builder = ReceiptParty.builder();
            if (spec != null) {
                spec.accept(builder);
            }
            return builder.build();
        }

        /**
         * Appends a titled block of label/value rows.
         *
         * @param title group heading
         * @param spec  group builder callback
         * @return this builder
         */
        public Builder detailGroup(String title, Consumer<ReceiptFieldGroup.Builder> spec) {
            ReceiptFieldGroup.Builder builder = ReceiptFieldGroup.builder(title);
            if (spec != null) {
                spec.accept(builder);
            }
            detailGroups.add(builder.build());
            return this;
        }

        /**
         * Appends a prepared field group.
         *
         * @param group field group
         * @return this builder
         */
        public Builder addDetailGroup(ReceiptFieldGroup group) {
            detailGroups.add(Objects.requireNonNull(group, "group"));
            return this;
        }

        /**
         * Appends a step to the payment timeline.
         *
         * @param label     step name
         * @param timestamp formatted timestamp
         * @param detail    optional explanation
         * @return this builder
         */
        public Builder event(String label, String timestamp, String detail) {
            timeline.add(new ReceiptEvent(label, timestamp, detail));
            return this;
        }

        /**
         * Appends a note shown under the detail groups.
         *
         * @param note note text
         * @return this builder
         */
        public Builder note(String note) {
            if (note != null && !note.isBlank()) {
                notes.add(note);
            }
            return this;
        }

        /**
         * Sets the footer QR code target and the line that explains it.
         *
         * @param url  URL encoded into the QR code
         * @param text one-line explanation printed beside it
         * @return this builder
         */
        public Builder verification(String url, String text) {
            this.verificationUrl = url;
            this.verificationText = text;
            return this;
        }

        /**
         * Appends a support contact line.
         *
         * @param line contact line
         * @return this builder
         */
        public Builder supportLine(String line) {
            if (line != null && !line.isBlank()) {
                supportLines.add(line);
            }
            return this;
        }

        /**
         * Sets the small-print block at the foot of the page.
         *
         * @param legalNote legal text
         * @return this builder
         */
        public Builder legalNote(String legalNote) {
            this.legalNote = legalNote;
            return this;
        }

        /**
         * Builds immutable receipt data.
         *
         * @return receipt data
         */
        public ReceiptData build() {
            return new ReceiptData(
                    documentTitle,
                    issuerName,
                    generatedOn,
                    reference,
                    amountLabel,
                    amount,
                    amountCaption,
                    status,
                    summaryFields,
                    payerLabel,
                    payer,
                    beneficiaryLabel,
                    beneficiary,
                    detailGroups,
                    timeline,
                    notes,
                    verificationUrl,
                    verificationText,
                    supportLines,
                    legalNote);
        }
    }
}
