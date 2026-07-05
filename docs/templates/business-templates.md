# Business Templates — Invoice & Proposal

GraphCompose ships maintained templates for two common business
documents: **invoices** and **proposals**. You supply a typed data
spec, pick a `BrandTheme`, and the preset renders a consistent,
branded document. You never position anything by hand.

> For **CVs and cover letters**, use the same layered model's CV and
> cover-letter families — see the
> [Templates v2 (layered) quickstart](v2-layered/quickstart.md).
> Arriving from a pre-2.0 surface? See
> [Which template system should I use?](which-template-system.md).

## The compose-first contract

Every template follows the same five steps. The preset owns the
document structure; your application owns the data and the output
destination.

1. Build a data spec — `InvoiceDocumentSpec` or `ProposalDocumentSpec`.
2. Choose a `BrandTheme` (each family ships a default —
   `BrandTheme.invoiceModern()`, `BrandTheme.proposalModern()`).
3. Ask the preset for a template — `ModernInvoice.create(theme)`
   returns a `DocumentTemplate<InvoiceDocumentSpec>`.
4. Open a `DocumentSession`.
5. `template.compose(document, spec)`, then render.

The template composes into an **open** `DocumentSession` — it never
decides file vs stream vs bytes. The caller does.

## Invoice

<!-- doc-example: id=business-invoice mode=method -->
```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.presets.ModernInvoice;

import java.nio.file.Path;

InvoiceDocumentSpec invoice = InvoiceDocumentSpec.builder()
        .title("Invoice")
        .invoiceNumber("GC-2026-041")
        .issueDate("25 Jun 2026")
        .dueDate("25 Jul 2026")
        .reference("GraphCompose implementation")
        .status("Due")
        .fromParty(party -> party
                .name("GraphCompose Studio")
                .addressLines("10 Example Street", "London")
                .email("billing@example.com")
                .phone("+44 20 0000 0000")
                .taxId("VAT GB000000000"))
        .billToParty(party -> party
                .name("Client Ltd")
                .addressLines("22 Client Road", "Manchester")
                .email("accounts@client.example"))
        .lineItem("Document engine integration", "Implementation and support",
                "1", "4,800.00", "4,800.00")
        .summaryRow("Subtotal", "4,800.00")
        .summaryRow("VAT", "960.00")
        .totalRow("Total", "5,760.00")
        .paymentTerm("Payment due within 30 days.")
        .footerNote("Thank you for your business.")
        .build();

BrandTheme theme = BrandTheme.invoiceModern();
DocumentTemplate<InvoiceDocumentSpec> template = ModernInvoice.create(theme);

float margin = (float) ModernInvoice.RECOMMENDED_MARGIN;
try (DocumentSession document = GraphCompose.document(Path.of("invoice.pdf"))
        .pageSize(DocumentPageSize.A4)
        .pageBackground(theme.palette().mainFill())
        .margin(margin, margin, margin, margin)
        .create()) {
    template.compose(document, invoice);
    document.buildPdf();
}
```

The preset renders a masthead with the invoice metadata, two-column
seller / buyer blocks, a themed line-item table with summary and
total rows, and a notes / payment-terms footer.

## Proposal

Same shape, different spec. Use a proposal when the artifact is sales or
project scope rather than billing. The timeline takes a three-argument
`timelineItem(phase, duration, details)`.

<!-- doc-example: id=business-proposal mode=method -->
```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentPageSize;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.proposal.ProposalDocumentSpec;
import com.demcha.compose.document.templates.proposal.presets.ModernProposal;

import java.nio.file.Path;

ProposalDocumentSpec proposal = ProposalDocumentSpec.builder()
        .title("Proposal")
        .proposalNumber("PR-2026-014")
        .preparedDate("25 Jun 2026")
        .validUntil("25 Jul 2026")
        .projectTitle("Document Automation Platform")
        .executiveSummary("A proposal for building reliable PDF generation into the product workflow.")
        .sender(party -> party
                .name("GraphCompose Studio")
                .addressLines("10 Example Street", "London")
                .email("hello@example.com")
                .website("graphcompose.example"))
        .recipient(party -> party
                .name("Client Ltd")
                .addressLines("22 Client Road", "Manchester")
                .email("product@client.example"))
        .section("Scope", "Backend integration, template setup, and regression checks.")
        .timelineItem("Week 1", "1 week", "Data model and rendering endpoint.")
        .pricingRow("Implementation", "Fixed scope", "4,800.00")
        .emphasizedPricingRow("Total", "Excluding tax", "4,800.00")
        .acceptanceTerm("Proposal valid for 30 days.")
        .footerNote("Prepared with GraphCompose.")
        .build();

BrandTheme theme = BrandTheme.proposalModern();
DocumentTemplate<ProposalDocumentSpec> template = ModernProposal.create(theme);

float margin = (float) ModernProposal.RECOMMENDED_MARGIN;
try (DocumentSession document = GraphCompose.document(Path.of("proposal.pdf"))
        .pageSize(DocumentPageSize.A4)
        .pageBackground(theme.palette().mainFill())
        .margin(margin, margin, margin, margin)
        .create()) {
    template.compose(document, proposal);
    document.buildPdf();
}
```

## Rendering on a server

In production the spec usually comes from application data and the
document is streamed to the caller's stream. The template composes the
same way before any output method; create one session per request.
`create()` without arguments uses the family's default theme.

<!-- doc-example: id=business-stream mode=members -->
```java
import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.data.invoice.InvoiceDocumentSpec;
import com.demcha.compose.document.templates.invoice.presets.ModernInvoice;

import java.io.OutputStream;

void streamInvoice(InvoiceDocumentSpec invoice, OutputStream out) throws Exception {
    DocumentTemplate<InvoiceDocumentSpec> template = ModernInvoice.create();

    try (DocumentSession document = GraphCompose.document().create()) {
        template.compose(document, invoice);
        document.writePdf(out);
    }
}
```

## Customizing

If the built-in structure is close but not exact, prefer these moves in
order:

1. Check whether the spec already has the field you need.
2. Change the `BrandTheme` — swap a whole factory, or replace one token
   bundle (`Palette` / `Typography` / `Spacing` / `Decoration`) and keep
   the rest. See [Recipes — themes](../recipes/themes.md).
3. Wrap the template call with session-level PDF chrome — a footer,
   metadata, or protection — see [Getting started](../getting-started.md).
4. Only fork or write a new preset when the document *structure* itself
   differs. A custom template implements the same `DocumentTemplate<S>`
   contract and composes into the same session — see
   [authoring presets](v2-layered/authoring-presets.md).

## See also

- [Which template system should I use?](which-template-system.md) — naming history + the pre-2.0 migration map.
- [Templates v2 (layered) quickstart](v2-layered/quickstart.md) — CVs and cover letters.
- [Recipes — themes](../recipes/themes.md) — customizing `BrandTheme`.
- [Streaming](../recipes/streaming.md) — server output patterns.
