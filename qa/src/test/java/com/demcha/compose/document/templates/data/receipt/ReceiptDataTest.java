package com.demcha.compose.document.templates.data.receipt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Normalization contract of the receipt records.
 *
 * <p>These records are what a caller's payment system hands the template, so
 * the interesting cases are the ones a caller gets wrong: a missing status, a
 * blank party caption, a null collection. Each has to produce something the
 * preset can render rather than a null the widget discovers at layout
 * time.</p>
 */
class ReceiptDataTest {

    @Test
    void defaultsEveryOptionalField() {
        ReceiptData data = ReceiptData.builder().build();

        assertThat(data.documentTitle()).isEqualTo("Payment receipt");
        assertThat(data.amountLabel()).isEqualTo("Amount");
        assertThat(data.payerLabel()).isEqualTo("From");
        assertThat(data.beneficiaryLabel()).isEqualTo("To");
        assertThat(data.issuerName()).isEmpty();
        assertThat(data.amount()).isEmpty();
        assertThat(data.detailGroups()).isEmpty();
        assertThat(data.timeline()).isEmpty();
        assertThat(data.notes()).isEmpty();
        assertThat(data.supportLines()).isEmpty();
    }

    @Test
    void missingStatusReadsAsInProgress() {
        // The least-claiming reading: a receipt that does not say it settled
        // must not be coloured as if it had.
        ReceiptData data = ReceiptData.builder().build();
        assertThat(data.status().tone()).isEqualTo(ReceiptStatusTone.IN_PROGRESS);
        assertThat(data.status().hasLabel()).isFalse();
    }

    @Test
    void blankPartyCaptionsFallBackRatherThanRenderEmpty() {
        ReceiptData data = ReceiptData.builder()
                .payer("  ", party -> party.name("Alex Sample"))
                .beneficiary("", party -> party.name("Harbour Finance Ltd"))
                .build();

        assertThat(data.payerLabel()).isEqualTo("From");
        assertThat(data.beneficiaryLabel()).isEqualTo("To");
    }

    @Test
    void presenceFollowsContentNotTheObject() {
        // A party builder always yields an object, so "has a payer" has to mean
        // "carries something worth drawing" — otherwise every receipt renders an
        // empty card.
        ReceiptData empty = ReceiptData.builder()
                .payer("Paid from", party -> { })
                .build();
        assertThat(empty.hasPayer()).isFalse();

        ReceiptData named = ReceiptData.builder()
                .payer("Paid from", party -> party.name("Alex Sample"))
                .build();
        assertThat(named.hasPayer()).isTrue();

        ReceiptData fieldsOnly = ReceiptData.builder()
                .beneficiary("Paid to", party -> party.field("Account", "•••• 5604"))
                .build();
        assertThat(fieldsOnly.hasBeneficiary()).isTrue();
    }

    @Test
    void collectionsAreFrozen() {
        ReceiptData data = ReceiptData.builder()
                .summaryField("Value date", "07 Jul 2026")
                .detailGroup("Transfer details", group -> group.field("Fee", "£0.00"))
                .build();

        assertThatThrownBy(() -> data.summaryFields().add(ReceiptField.of("x", "y")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> data.detailGroups().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void blankNotesAndSupportLinesAreDropped() {
        ReceiptData data = ReceiptData.builder()
                .note("  ")
                .note(null)
                .note("Keep this for your records.")
                .supportLine("")
                .supportLine("help@example.com")
                .build();

        assertThat(data.notes()).containsExactly("Keep this for your records.");
        assertThat(data.supportLines()).containsExactly("help@example.com");
    }

    @Test
    void partyDropsBlankAddressLines() {
        ReceiptParty party = ReceiptParty.builder()
                .name("Alex Sample")
                .addressLines("12 Example Way", "  ", null, "Brentford TW0 0AA")
                .build();

        assertThat(party.addressLines()).containsExactly("12 Example Way", "Brentford TW0 0AA");
    }

    @Test
    void nullCollectionsNormalizeToEmpty() {
        ReceiptFieldGroup group = new ReceiptFieldGroup(null, null);
        assertThat(group.title()).isEmpty();
        assertThat(group.fields()).isEmpty();

        ReceiptParty party = new ReceiptParty(null, null, null);
        assertThat(party.name()).isEmpty();
        assertThat(party.fields()).isEmpty();

        ReceiptEvent event = new ReceiptEvent(null, null, null);
        assertThat(event.label()).isEmpty();
        assertThat(event.timestamp()).isEmpty();
        assertThat(event.detail()).isEmpty();
    }

    @Test
    void specWrapsMissingPayloadRatherThanCarryingNull() {
        assertThat(new ReceiptDocumentSpec(null).receipt()).isNotNull();
        assertThat(ReceiptDocumentSpec.of(null).receipt().documentTitle())
                .isEqualTo("Payment receipt");
        assertThat(ReceiptDocumentSpec.from(ReceiptData.builder()
                        .documentTitle("Transfer confirmation")
                        .build()).receipt().documentTitle())
                .isEqualTo("Transfer confirmation");
    }

    @Test
    void emphasisIsCarriedPerField() {
        ReceiptFieldGroup group = ReceiptFieldGroup.builder("Amount breakdown")
                .field("Fee", "£0.00")
                .emphasized("Total debited", "£66.62")
                .add(ReceiptField.of("Rate", "1.00"))
                .build();

        assertThat(group.fields()).extracting(ReceiptField::emphasized)
                .containsExactly(false, true, false);
        assertThat(group.fields()).extracting(ReceiptField::label)
                .isEqualTo(List.of("Fee", "Total debited", "Rate"));
    }
}
