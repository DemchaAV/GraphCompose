package com.demcha.compose.document.templates.invoice.builder;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.components.MarkdownText;
import com.demcha.compose.document.templates.invoice.spec.InvoiceSpec;
import com.demcha.compose.document.templates.themes.Spacing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for assembling a Templates v2 invoice
 * {@link DocumentTemplate}.
 *
 * <p>Produces a single-column invoice with the following sections:
 * invoice number heading, issue/due date row, From / Bill-To parties
 * side by side, a line-item table (rendered as paragraphs in this
 * minimal v2 surface — a fully styled table layout will land in a
 * follow-up that adds Templates v2 table styles), summary rows, and
 * footer notes.</p>
 *
 * <p>This is a deliberately minimal invoice surface for v1.6: the
 * legacy {@code InvoiceTemplateV2} continues to provide the cinematic
 * presentation, and the v2 surface here covers the canonical data
 * shape and composition seam that a custom preset can build on. The
 * full visual feature parity (cinematic chrome, line-item table
 * styling, party panels) lands in a follow-up release.</p>
 */
public final class InvoiceBuilder {

    private String id;
    private String displayName;
    private DocumentTextStyle headingStyle;
    private DocumentTextStyle bodyStyle;
    private Spacing spacing;

    private InvoiceBuilder() {
    }

    /**
     * Returns a fresh builder.
     *
     * @return new builder
     */
    public static InvoiceBuilder builder() {
        return new InvoiceBuilder();
    }

    /**
     * Sets the stable identifier exposed via
     * {@link DocumentTemplate#id()}.
     *
     * @param value non-null identifier
     * @return this builder
     */
    public InvoiceBuilder id(String value) {
        this.id = Objects.requireNonNull(value, "id");
        return this;
    }

    /**
     * Sets the human-readable display name.
     *
     * @param value non-null display name
     * @return this builder
     */
    public InvoiceBuilder displayName(String value) {
        this.displayName = Objects.requireNonNull(value, "displayName");
        return this;
    }

    /**
     * Sets the text style applied to invoice number / section
     * headings.
     *
     * @param value non-null heading text style
     * @return this builder
     */
    public InvoiceBuilder headingStyle(DocumentTextStyle value) {
        this.headingStyle = Objects.requireNonNull(value, "headingStyle");
        return this;
    }

    /**
     * Sets the text style applied to body paragraphs (parties,
     * line items, summary rows, notes).
     *
     * @param value non-null body text style
     * @return this builder
     */
    public InvoiceBuilder bodyStyle(DocumentTextStyle value) {
        this.bodyStyle = Objects.requireNonNull(value, "bodyStyle");
        return this;
    }

    /**
     * Sets the active spacing tokens.
     *
     * @param value non-null spacing tokens
     * @return this builder
     */
    public InvoiceBuilder spacing(Spacing value) {
        this.spacing = Objects.requireNonNull(value, "spacing");
        return this;
    }

    /**
     * Validates configuration and returns the assembled
     * {@link DocumentTemplate}.
     *
     * @return ready-to-use template instance
     * @throws NullPointerException if any required setter has not been
     *                              called
     */
    public DocumentTemplate<InvoiceSpec> build() {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(headingStyle, "headingStyle");
        Objects.requireNonNull(bodyStyle, "bodyStyle");
        Objects.requireNonNull(spacing, "spacing");

        final String capturedId = id;
        final String capturedDisplay = displayName;
        final DocumentTextStyle capturedHeading = headingStyle;
        final DocumentTextStyle capturedBody = bodyStyle;
        final Spacing capturedSpacing = spacing;

        return new DocumentTemplate<InvoiceSpec>() {
            @Override
            public String id() {
                return capturedId;
            }

            @Override
            public String displayName() {
                return capturedDisplay;
            }

            @Override
            public void compose(DocumentSession session, InvoiceSpec spec) {
                Objects.requireNonNull(session, "session");
                Objects.requireNonNull(spec, "spec");
                List<DocumentNode> children = new ArrayList<>();
                children.add(headingRow("Invoice " + spec.invoiceNumber()));
                children.add(bodyParagraph("Issued: " + spec.issueDate()
                        + (spec.dueDate().isBlank() ? "" : "   Due: " + spec.dueDate())));
                children.add(partyBlock("From", spec.fromParty()));
                children.add(partyBlock("Bill To", spec.billToParty()));
                children.add(lineItemsBlock(spec.lineItems()));
                if (!spec.summaryRows().isEmpty()) {
                    children.add(summaryBlock(spec.summaryRows()));
                }
                for (String note : spec.notes()) {
                    children.add(bodyParagraph(note));
                }
                session.add(new ContainerNode(
                        "invoice." + capturedId,
                        children,
                        capturedSpacing.moduleGap(),
                        DocumentInsets.zero(),
                        DocumentInsets.zero(),
                        null, null, null, null));
            }

            private ParagraphNode headingRow(String text) {
                return new ParagraphNode(
                        "invoice.heading", "",
                        MarkdownText.parse(text, capturedHeading),
                        capturedHeading, TextAlign.LEFT,
                        capturedSpacing.lineSpacing(), "", null, null, null,
                        DocumentInsets.zero(),
                        new DocumentInsets(0, 0, capturedSpacing.sectionTitleBelow(), 0),
                        null);
            }

            private ParagraphNode bodyParagraph(String text) {
                return new ParagraphNode(
                        "invoice.line", "",
                        MarkdownText.parse(text, capturedBody),
                        capturedBody, TextAlign.LEFT,
                        capturedSpacing.lineSpacing(), "", null, null, null,
                        DocumentInsets.zero(), DocumentInsets.zero(), null);
            }

            private DocumentNode partyBlock(String label, InvoiceSpec.Party party) {
                List<DocumentNode> rows = new ArrayList<>();
                rows.add(bodyParagraph("**" + label + "**"));
                rows.add(bodyParagraph(party.name()));
                for (String line : party.addressLines()) {
                    rows.add(bodyParagraph(line));
                }
                if (!party.email().isBlank()) {
                    rows.add(bodyParagraph(party.email()));
                }
                if (!party.phone().isBlank()) {
                    rows.add(bodyParagraph(party.phone()));
                }
                if (!party.taxId().isBlank()) {
                    rows.add(bodyParagraph("Tax ID: " + party.taxId()));
                }
                return new ContainerNode(
                        "invoice." + label.toLowerCase().replace(' ', '_'),
                        rows,
                        capturedSpacing.lineSpacing(),
                        DocumentInsets.zero(), DocumentInsets.zero(),
                        null, null, null, null);
            }

            private DocumentNode lineItemsBlock(List<InvoiceSpec.LineItem> items) {
                List<DocumentNode> rows = new ArrayList<>();
                rows.add(bodyParagraph("**Description | Qty | Unit | Amount**"));
                for (InvoiceSpec.LineItem item : items) {
                    String row = String.format("%s | %s | %s | %s",
                            item.description(), item.quantity(),
                            item.unitPrice(), item.amount());
                    rows.add(bodyParagraph(row));
                    if (!item.details().isBlank()) {
                        rows.add(bodyParagraph("   " + item.details()));
                    }
                }
                return new ContainerNode(
                        "invoice.lineItems",
                        rows,
                        capturedSpacing.listItemSpacing(),
                        DocumentInsets.zero(), DocumentInsets.zero(),
                        null, null, null, null);
            }

            private DocumentNode summaryBlock(List<InvoiceSpec.SummaryRow> rows) {
                List<DocumentNode> nodes = new ArrayList<>();
                for (InvoiceSpec.SummaryRow row : rows) {
                    String text = row.isTotal()
                            ? "**" + row.label() + ": " + row.value() + "**"
                            : row.label() + ": " + row.value();
                    nodes.add(bodyParagraph(text));
                }
                return new ContainerNode(
                        "invoice.summary",
                        nodes,
                        capturedSpacing.lineSpacing(),
                        DocumentInsets.zero(), DocumentInsets.zero(),
                        null, null, null, null);
            }
        };
    }
}
