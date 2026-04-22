package com.demcha.mock;

import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.invoice.InvoiceLineItem;
import com.demcha.compose.document.templates.data.invoice.InvoiceParty;
import com.demcha.compose.document.templates.data.invoice.InvoiceSummaryRow;

import java.util.List;

public final class InvoiceDataFixtures {

    private InvoiceDataFixtures() {
    }

    public static InvoiceData standardInvoice() {
        return new InvoiceData(
                "Invoice",
                "GC-2026-041",
                "02 Apr 2026",
                "16 Apr 2026",
                "Platform Refresh Sprint",
                "Pending",
                new InvoiceParty(
                        "GraphCompose Studio",
                        List.of("18 Layout Street", "London, UK", "EC1A 4GC"),
                        "billing@graphcompose.dev",
                        "+44 20 5555 1000",
                        "GB-99887766"),
                new InvoiceParty(
                        "Northwind Systems",
                        List.of("Attn: Finance Team", "410 Market Avenue", "Manchester, UK"),
                        "ap@northwind.example",
                        "+44 161 555 2200",
                        "NW-2026-01"),
                List.of(
                        new InvoiceLineItem("Discovery workshop", "Stakeholder interviews and current-state review", "1", "GBP 1,450", "GBP 1,450"),
                        new InvoiceLineItem("Template architecture", "Reusable document flows for invoice and proposal output", "2", "GBP 980", "GBP 1,960"),
                        new InvoiceLineItem("Render QA", "Visual validation and guideline passes", "3", "GBP 320", "GBP 960"),
                        new InvoiceLineItem("Developer enablement", "Examples module and onboarding notes", "1", "GBP 780", "GBP 780")),
                List.of(
                        new InvoiceSummaryRow("Subtotal", "GBP 5,150", false),
                        new InvoiceSummaryRow("VAT (20%)", "GBP 1,030", false),
                        new InvoiceSummaryRow("Total", "GBP 6,180", true)),
                List.of(
                        "Please include the invoice number on your remittance advice.",
                        "All work was delivered as agreed during the April implementation window."),
                List.of(
                        "Payment due within 14 calendar days.",
                        "Bank transfer preferred; contact billing@graphcompose.dev for remittance details.",
                        "Late payments may delay additional template customization work."),
                "Thank you for choosing GraphCompose for production document rendering."
        );
    }
}
