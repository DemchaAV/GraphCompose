package com.demcha.compose.document.templates.receipt.presets;

import com.demcha.compose.document.templates.data.receipt.ReceiptDocumentSpec;
import com.demcha.compose.document.templates.data.receipt.ReceiptStatus;

/**
 * Shared fixture data for the receipt gates — the SAME spec feeds the pixel gate and the
 * exact layout snapshot, so a geometry shift the 50k pixel budget absorbs still trips the
 * snapshot, and a colour change the snapshot cannot see still trips the pixels.
 *
 * <p>Kept in the test tree and built only from main code, so the gates depend on the
 * templates module and not on the examples module.</p>
 */
final class ReceiptFixtures {

    private ReceiptFixtures() {
    }

    /**
     * Canonical sample receipt — exercises the masthead, the hero with its status chip and
     * summary fields, both party sides, two detail groups, the status trail, the notes
     * block, and the footer with its QR code.
     *
     * @return the canonical receipt spec shared by every receipt gate
     */
    static ReceiptDocumentSpec canonicalReceipt() {
        return ReceiptDocumentSpec.of(receipt -> receipt
                .documentTitle("Transfer confirmation")
                .issuerName("Northwind Pay")
                .generatedOn("09 August 2026")
                .reference("NWP-4821-0067")
                .amount("Amount collected", "£66.62")
                .amountCaption("Direct Debit collected by Harbour Finance Ltd")
                .status(ReceiptStatus.settled("Completed"))
                .summaryField("Value date", "07 Jul 2026")
                .summaryField("Operation date", "07 Jul 2026")
                .summaryField("Scheme", "Bacs Direct Debit")
                .payer("Paid from", party -> party
                        .name("Alex Sample")
                        .addressLines("12 Example Way", "Brentford TW0 0AA", "United Kingdom")
                        .field("Account", "•••• 4396")
                        .field("Sort code", "00-00-00"))
                .beneficiary("Paid to", party -> party
                        .name("Harbour Finance Ltd")
                        .addressLines("1 Sample Quay", "Manchester M0 0AA")
                        .field("Account", "•••• 5604")
                        .field("Sort code", "00-00-11"))
                .detailGroup("Transfer details", group -> group
                        .field("Mandate reference", "MND-0110-8054-5652")
                        .field("Transaction ID", "b71f0c2e-9a44-4f18-bd30-51c7a2e9d840")
                        .field("Payment scheme", "Bacs Direct Debit")
                        .field("Statement description", "HARBOUR FIN 4821"))
                .detailGroup("Amount breakdown", group -> group
                        .field("Amount", "£66.62")
                        .field("Transfer fee", "£0.00")
                        .field("Exchange rate", "1.00 GBP / GBP")
                        .emphasized("Total debited", "£66.62"))
                .event("Instructed", "07 Jul 2026, 08:12 BST",
                        "Collection request submitted to the scheme.")
                .event("In clearing", "08 Jul 2026, 09:00 BST",
                        "Three-working-day Bacs cycle.")
                .event("Settled", "09 Jul 2026, 06:30 BST",
                        "Funds debited and confirmed by the receiving bank.")
                .note("Keep this confirmation for your records. It is not a tax invoice.")
                .verification("https://example.com/verify/NWP-4821-0067",
                        "Scan to check this confirmation against the issuing records.")
                .supportLine("Support  +44 20 0000 0000")
                .supportLine("help@northwind-pay.example")
                .legalNote("Northwind Pay is a fictional institution used in GraphCompose "
                        + "examples; every name, account, and reference on this page is "
                        + "invented."));
    }
}
