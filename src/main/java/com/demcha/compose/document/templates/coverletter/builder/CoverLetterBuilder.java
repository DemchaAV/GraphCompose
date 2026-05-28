package com.demcha.compose.document.templates.coverletter.builder;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.ContainerNode;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.node.ParagraphNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.components.Header;
import com.demcha.compose.document.templates.components.MarkdownText;
import com.demcha.compose.document.templates.coverletter.layouts.LetterFormat;
import com.demcha.compose.document.templates.coverletter.spec.CoverLetterHeader;
import com.demcha.compose.document.templates.coverletter.spec.CoverLetterSpec;
import com.demcha.compose.document.templates.themes.Spacing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for assembling a Templates v2 cover-letter
 * {@link DocumentTemplate}.
 *
 * <p>A cover-letter preset wraps one builder call inside its
 * {@code create(BusinessTheme)} factory — the same flat-recipe
 * pattern used by CV presets. Each preset shares the typography
 * palette of its paired CV preset (same heading style, same body
 * style, same spacing rhythm) so a writer's CV and cover letter
 * read as one matched set.</p>
 *
 * <p>All seven knobs ({@code id}, {@code displayName},
 * {@code header}, {@code layout}, {@code bodyStyle}, {@code spacing},
 * and at least implicit alignment via the layout / body style) must
 * be configured before calling {@link #build()}.</p>
 *
 * @deprecated Superseded by the layered <code>…v2…</code> surface (the current
 *             standard) — the layered model
 *             {@link com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument}
 *             plus the {@code coverletter.v2} presets. Kept for backward
 *             compatibility; scheduled for removal in a future major. See
 *             {@code docs/templates/v2-layered/}.
 */
@Deprecated(since = "1.7.0", forRemoval = true)
public final class CoverLetterBuilder {

    private String id;
    private String displayName;
    private Header header;
    private LetterFormat layout;
    private DocumentTextStyle bodyStyle;
    private Spacing spacing;

    private CoverLetterBuilder() {
    }

    /**
     * Returns a fresh builder.
     *
     * @return new builder instance
     */
    public static CoverLetterBuilder builder() {
        return new CoverLetterBuilder();
    }

    /**
     * Sets the stable identifier exposed via
     * {@link DocumentTemplate#id()}.
     *
     * @param value non-null identifier
     * @return this builder
     */
    public CoverLetterBuilder id(String value) {
        this.id = Objects.requireNonNull(value, "id");
        return this;
    }

    /**
     * Sets the human-readable display name.
     *
     * @param value non-null display name
     * @return this builder
     */
    public CoverLetterBuilder displayName(String value) {
        this.displayName = Objects.requireNonNull(value, "displayName");
        return this;
    }

    /**
     * Sets the header component used to render the document header.
     *
     * @param value non-null header component
     * @return this builder
     */
    public CoverLetterBuilder header(Header value) {
        this.header = Objects.requireNonNull(value, "header");
        return this;
    }

    /**
     * Sets the layout responsible for arranging header + letter
     * blocks.
     *
     * @param value non-null layout
     * @return this builder
     */
    public CoverLetterBuilder layout(LetterFormat value) {
        this.layout = Objects.requireNonNull(value, "layout");
        return this;
    }

    /**
     * Sets the text style applied to greeting, body paragraphs, and
     * closing.
     *
     * @param value non-null body text style
     * @return this builder
     */
    public CoverLetterBuilder bodyStyle(DocumentTextStyle value) {
        this.bodyStyle = Objects.requireNonNull(value, "bodyStyle");
        return this;
    }

    /**
     * Sets the active spacing tokens.
     *
     * @param value non-null spacing tokens
     * @return this builder
     */
    public CoverLetterBuilder spacing(Spacing value) {
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
    public DocumentTemplate<CoverLetterSpec> build() {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(bodyStyle, "bodyStyle");
        Objects.requireNonNull(spacing, "spacing");

        final String capturedId = id;
        final String capturedDisplay = displayName;
        final Header capturedHeader = header;
        final LetterFormat capturedLayout = layout;
        final DocumentTextStyle capturedBody = bodyStyle;
        final Spacing capturedSpacing = spacing;

        return new DocumentTemplate<CoverLetterSpec>() {
            @Override
            public String id() {
                return capturedId;
            }

            @Override
            public String displayName() {
                return capturedDisplay;
            }

            @Override
            public void compose(DocumentSession session, CoverLetterSpec spec) {
                Objects.requireNonNull(session, "session");
                Objects.requireNonNull(spec, "spec");

                DocumentNode headerNode = composeHeader(spec);
                List<DocumentNode> blocks = new ArrayList<>(3);
                if (!spec.greeting().isBlank()) {
                    blocks.add(paragraph(
                            "letter.greeting",
                            spec.greeting(),
                            /* topMargin    */ capturedSpacing.moduleGap(),
                            /* bottomMargin */ capturedSpacing.paragraphSpacing()));
                }
                blocks.add(bodyContainer(spec.bodyParagraphs()));
                if (!spec.closing().isBlank()) {
                    blocks.add(paragraph(
                            "letter.closing",
                            spec.closing(),
                            /* topMargin    */ capturedSpacing.paragraphSpacing(),
                            /* bottomMargin */ 0.0));
                }

                DocumentNode root = capturedLayout.compose(headerNode, blocks);
                session.add(root);
            }

            private DocumentNode composeHeader(CoverLetterSpec spec) {
                Header.Input input = new Header.Input(
                        spec.header().name(),
                        spec.header().contactItems(),
                        headerLinks(spec.header()));
                return capturedHeader.compose(input);
            }

            private List<Header.Link> headerLinks(CoverLetterHeader header) {
                List<Header.Link> result = new ArrayList<>();
                if (!header.email().isBlank()) {
                    result.add(Header.Link.active(
                            header.email(),
                            "mailto:" + header.email()));
                }
                for (CoverLetterHeader.Link link : header.links()) {
                    if (link.url().isBlank()) {
                        result.add(Header.Link.plain(link.label()));
                    } else {
                        result.add(Header.Link.active(link.label(), link.url()));
                    }
                }
                return result;
            }

            private DocumentNode bodyContainer(List<String> paragraphs) {
                List<DocumentNode> children = new ArrayList<>(paragraphs.size());
                for (int i = 0; i < paragraphs.size(); i++) {
                    children.add(paragraph(
                            "letter.body[" + i + "]",
                            paragraphs.get(i),
                            /* topMargin    */ 0.0,
                            /* bottomMargin */ 0.0));
                }
                return new ContainerNode(
                        "letter.body",
                        children,
                        /* spacing */ capturedSpacing.paragraphSpacing(),
                        DocumentInsets.zero(),
                        DocumentInsets.zero(),
                        null, null, null, null);
            }

            private ParagraphNode paragraph(String name, String text,
                                             double topMargin, double bottomMargin) {
                return new ParagraphNode(
                        name,
                        /* text       */ "",
                        /* inlineRuns */ MarkdownText.parse(text, capturedBody),
                        capturedBody,
                        TextAlign.LEFT,
                        capturedSpacing.lineSpacing(),
                        /* bulletOffset   */ "",
                        /* indentStrategy */ null,
                        /* link           */ null,
                        /* bookmark       */ null,
                        /* padding        */ DocumentInsets.zero(),
                        /* margin         */ new DocumentInsets(
                                topMargin, 0.0, bottomMargin, 0.0),
                        /* autoSize       */ null);
            }
        };
    }
}
