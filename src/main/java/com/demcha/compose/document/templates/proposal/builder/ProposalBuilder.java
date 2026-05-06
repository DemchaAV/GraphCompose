package com.demcha.compose.document.templates.proposal.builder;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.components.MarkdownText;
import com.demcha.compose.document.templates.proposal.spec.ProposalSpec;
import com.demcha.compose.document.templates.themes.Spacing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for assembling a Templates v2 proposal
 * {@link DocumentTemplate}.
 *
 * <p>Produces a single-column proposal with the following sections:
 * title, From / To parties, content sections (heading + body, in
 * source order), pricing summary, footer note. Markdown markers
 * ({@code **bold**}, {@code *italic*}) in body text are routed
 * through {@link MarkdownText}.</p>
 *
 * <p>Like {@link com.demcha.compose.document.templates.invoice.builder.InvoiceBuilder},
 * this is the minimal v2 proposal surface — the legacy
 * {@code ProposalTemplateV2} continues to ship the cinematic
 * presentation. Visual feature parity for the v2 proposal lands in a
 * follow-up release.</p>
 */
public final class ProposalBuilder {

    private String id;
    private String displayName;
    private DocumentTextStyle titleStyle;
    private DocumentTextStyle headingStyle;
    private DocumentTextStyle bodyStyle;
    private Spacing spacing;

    private ProposalBuilder() {
    }

    /**
     * Returns a fresh builder.
     *
     * @return new builder
     */
    public static ProposalBuilder builder() {
        return new ProposalBuilder();
    }

    /**
     * Sets the stable identifier exposed via
     * {@link DocumentTemplate#id()}.
     *
     * @param value non-null identifier
     * @return this builder
     */
    public ProposalBuilder id(String value) {
        this.id = Objects.requireNonNull(value, "id");
        return this;
    }

    /**
     * Sets the human-readable display name.
     *
     * @param value non-null display name
     * @return this builder
     */
    public ProposalBuilder displayName(String value) {
        this.displayName = Objects.requireNonNull(value, "displayName");
        return this;
    }

    /**
     * Sets the text style applied to the proposal title.
     *
     * @param value non-null title text style
     * @return this builder
     */
    public ProposalBuilder titleStyle(DocumentTextStyle value) {
        this.titleStyle = Objects.requireNonNull(value, "titleStyle");
        return this;
    }

    /**
     * Sets the text style applied to section headings.
     *
     * @param value non-null heading text style
     * @return this builder
     */
    public ProposalBuilder headingStyle(DocumentTextStyle value) {
        this.headingStyle = Objects.requireNonNull(value, "headingStyle");
        return this;
    }

    /**
     * Sets the text style applied to body paragraphs (parties,
     * section bodies, pricing rows, notes).
     *
     * @param value non-null body text style
     * @return this builder
     */
    public ProposalBuilder bodyStyle(DocumentTextStyle value) {
        this.bodyStyle = Objects.requireNonNull(value, "bodyStyle");
        return this;
    }

    /**
     * Sets the active spacing tokens.
     *
     * @param value non-null spacing tokens
     * @return this builder
     */
    public ProposalBuilder spacing(Spacing value) {
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
    public DocumentTemplate<ProposalSpec> build() {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(titleStyle, "titleStyle");
        Objects.requireNonNull(headingStyle, "headingStyle");
        Objects.requireNonNull(bodyStyle, "bodyStyle");
        Objects.requireNonNull(spacing, "spacing");

        final String capturedId = id;
        final String capturedDisplay = displayName;
        final DocumentTextStyle capturedTitle = titleStyle;
        final DocumentTextStyle capturedHeading = headingStyle;
        final DocumentTextStyle capturedBody = bodyStyle;
        final Spacing capturedSpacing = spacing;

        return new DocumentTemplate<ProposalSpec>() {
            @Override
            public String id() {
                return capturedId;
            }

            @Override
            public String displayName() {
                return capturedDisplay;
            }

            @Override
            public void compose(DocumentSession session, ProposalSpec spec) {
                Objects.requireNonNull(session, "session");
                Objects.requireNonNull(spec, "spec");
                List<DocumentNode> children = new ArrayList<>();
                children.add(titleParagraph(spec.title()));
                children.add(partyLine("From: " + spec.fromParty().name()));
                children.add(partyLine("To: " + spec.toParty().name()));
                for (ProposalSpec.Section section : spec.sections()) {
                    if (!section.heading().isBlank()) {
                        children.add(headingParagraph(section.heading()));
                    }
                    if (!section.body().isBlank()) {
                        children.add(bodyParagraph(section.body()));
                    }
                }
                if (!spec.pricingRows().isEmpty()) {
                    children.add(headingParagraph("Pricing"));
                    for (ProposalSpec.PricingRow row : spec.pricingRows()) {
                        String text = row.isHeadline()
                                ? "**" + row.label() + ": " + row.value() + "**"
                                : row.label() + ": " + row.value();
                        children.add(bodyParagraph(text));
                    }
                }
                if (!spec.footerNote().isBlank()) {
                    children.add(bodyParagraph(spec.footerNote()));
                }
                session.add(new ContainerNode(
                        "proposal." + capturedId,
                        children,
                        capturedSpacing.moduleGap(),
                        DocumentInsets.zero(),
                        DocumentInsets.zero(),
                        null, null, null, null));
            }

            private ParagraphNode titleParagraph(String text) {
                return new ParagraphNode(
                        "proposal.title", "",
                        MarkdownText.parse(text, capturedTitle),
                        capturedTitle, TextAlign.LEFT,
                        capturedSpacing.lineSpacing(), "", null, null, null,
                        DocumentInsets.zero(),
                        new DocumentInsets(0, 0, capturedSpacing.sectionTitleBelow(), 0),
                        null);
            }

            private ParagraphNode headingParagraph(String text) {
                return new ParagraphNode(
                        "proposal.heading", "",
                        MarkdownText.parse(text, capturedHeading),
                        capturedHeading, TextAlign.LEFT,
                        capturedSpacing.lineSpacing(), "", null, null, null,
                        DocumentInsets.zero(),
                        new DocumentInsets(
                                capturedSpacing.sectionTitleAbove(), 0,
                                capturedSpacing.sectionTitleBelow(), 0),
                        null);
            }

            private ParagraphNode bodyParagraph(String text) {
                return new ParagraphNode(
                        "proposal.line", "",
                        MarkdownText.parse(text, capturedBody),
                        capturedBody, TextAlign.LEFT,
                        capturedSpacing.lineSpacing(), "", null, null, null,
                        DocumentInsets.zero(), DocumentInsets.zero(), null);
            }

            private ParagraphNode partyLine(String text) {
                return bodyParagraph(text);
            }
        };
    }
}
