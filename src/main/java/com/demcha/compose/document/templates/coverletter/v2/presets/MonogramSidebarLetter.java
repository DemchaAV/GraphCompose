package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.*;
import com.demcha.compose.document.node.LayerAlign;
import com.demcha.compose.document.node.LayerStackNode;
import com.demcha.compose.document.node.SpacerNode;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.*;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.TextOrnaments;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvName;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.font.FontName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code MonogramSidebar} CV preset.
 *
 * <p>Carries the CV's signature <strong>monogram-ring badge</strong>
 * into the letter: the dark-slate initials ring sits centred at the top,
 * over the centred spaced-caps name (stacked first / last) and a muted
 * gold spaced-caps role line, then a centred contact line and a
 * single-column letter body via the shared {@link LetterBody}. The CV's
 * pale-teal sidebar column (painted by {@code pageBackgrounds}) and its
 * icon contact stack are sidebar-only and are dropped for the
 * single-column letter; the badge + name treatment is what makes the two
 * read as a set. Palette / typography come from
 * {@link CvTheme#monogramSidebar()}.</p>
 *
 * <p>The gold accent, dark monogram ring, and the PT-Serif monogram font
 * are mirrored from the CV, where they are preset-local.</p>
 */
public final class MonogramSidebarLetter {

    /**
     * Stable template identifier.
     */
    public static final String ID = "monogram-sidebar-letter";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Monogram Sidebar Letter";

    /**
     * Recommended page margin (in points) — generous business-letter feel.
     */
    public static final double RECOMMENDED_MARGIN = 48.0;

    /**
     * Muted gold accent (name sub-line + links). Mirrors the CV token.
     */
    private static final DocumentColor ACCENT = DocumentColor.rgb(158, 146, 104);

    /**
     * Dark slate monogram ring + initials. Mirrors the CV token.
     */
    private static final DocumentColor MONOGRAM_RING = DocumentColor.rgb(54, 62, 74);

    /**
     * PT-Serif used only for the monogram initials. Mirrors the CV token.
     */
    private static final FontName MONOGRAM_FONT = FontName.PT_SERIF;

    /**
     * Monogram ring diameter (matches the CV badge).
     */
    private static final double MONOGRAM_DIAMETER = 122.0;

    /**
     * Letter body size. The Monogram Sidebar CV theme uses a 7.5pt body
     * tuned for its dense two-column layout — too small for a
     * single-column letter, so the prose is rendered a touch larger here.
     */
    private static final double LETTER_BODY_SIZE = 9.0;

    private MonogramSidebarLetter() {
    }

    /**
     * Builds the letter with its Monogram Sidebar theme.
     *
     * @return a {@code DocumentTemplate} for the "Monogram Sidebar Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.monogramSidebar());
    }

    /**
     * Builds the letter with a caller-supplied theme (share the paired
     * CV's theme instance for a guaranteed visual match).
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Monogram Sidebar Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private record Template(CvTheme theme) implements DocumentTemplate<CoverLetterDocument> {

        @Override
            public String id() {
                return ID;
            }

            @Override
            public String displayName() {
                return DISPLAY_NAME;
            }

            @Override
            public void compose(DocumentSession document, CoverLetterDocument doc) {
                Objects.requireNonNull(document, "document");
                Objects.requireNonNull(doc, "doc");

                double innerWidth = document.canvas().innerWidth();
                PageFlowBuilder flow = document.dsl()
                        .pageFlow()
                        .name("CoverLetterV2MonogramSidebarRoot")
                        .spacing(theme.spacing().pageFlowSpacing());

                flow.addSection("CoverLetterV2MonogramSidebarHeader", section -> {
                    section.spacing(2).padding(DocumentInsets.zero());
                    addMonogramBlock(section, initials(doc.identity().name()),
                            innerWidth);
                    addNameBlock(section, doc.identity());
                    ContactLine.centered(section, doc.identity(), theme,
                            contactMetaStyle(), contactLinkStyle(),
                            contactSeparatorStyle());
                });

                flow.addSection("CoverLetterV2MonogramSidebarBody", host ->
                        LetterBody.render(host, doc, theme, LETTER_BODY_SIZE));

                flow.build();
            }

            private void addMonogramBlock(SectionBuilder section, String initialsText,
                                          double innerWidth) {
                LayerStackNode badge = new LayerStackBuilder()
                        .name("CoverLetterV2MonogramSidebarBadge")
                        .back(new EllipseBuilder()
                                .name("CoverLetterV2MonogramSidebarRing")
                                .size(MONOGRAM_DIAMETER, MONOGRAM_DIAMETER)
                                .stroke(DocumentStroke.of(MONOGRAM_RING, 1.25))
                                .build())
                        .layer(new ParagraphBuilder()
                                .name("CoverLetterV2MonogramSidebarInitials")
                                .text(initialsText)
                                .textStyle(CvTextStyles.of(MONOGRAM_FONT, 44.0,
                                        DocumentTextDecoration.BOLD, MONOGRAM_RING))
                                .align(TextAlign.LEFT)
                                .build(), LayerAlign.CENTER)
                        .build();

                section.addLayerStack(outer -> outer
                        .name("CoverLetterV2MonogramSidebarFrame")
                        .margin(DocumentInsets.bottom(20))
                        .back(new SpacerNode(
                                "CoverLetterV2MonogramSidebarSpace",
                                Math.max(MONOGRAM_DIAMETER, innerWidth),
                                MONOGRAM_DIAMETER,
                                DocumentInsets.zero(),
                                DocumentInsets.zero()))
                        .layer(badge, LayerAlign.TOP_CENTER));
            }

            private void addNameBlock(SectionBuilder section, CvIdentity identity) {
                CvName name = identity.name();
                List<String> parts = new ArrayList<>();
                if (!name.first().isBlank()) {
                    parts.add(name.first());
                }
                if (!name.last().isBlank()) {
                    parts.add(name.last());
                }
                if (parts.isEmpty()) {
                    parts.add("");
                }
                DocumentTextStyle nameStyle = nameStyle();
                DocumentTextStyle titleStyle = subtitleStyle();

                for (int index = 0; index < parts.size(); index++) {
                    String part = parts.get(index);
                    DocumentInsets margin = index == parts.size() - 1
                            ? DocumentInsets.zero()
                            : DocumentInsets.bottom(6);
                    section.addParagraph(paragraph -> paragraph
                            .text(TextOrnaments.spacedUpper(part))
                            .textStyle(nameStyle)
                            .align(TextAlign.CENTER)
                            .lineSpacing(1.0)
                            .margin(margin));
                }
                String jobTitle = identity.jobTitle();
                if (jobTitle != null && !jobTitle.isBlank()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(TextOrnaments.spacedUpper(jobTitle))
                            .textStyle(titleStyle)
                            .align(TextAlign.CENTER)
                            .margin(new DocumentInsets(12, 0, 18, 0)));
                }
            }

            private DocumentTextStyle nameStyle() {
                return CvTextStyles.of(theme.typography().headlineFont(),
                        theme.typography().sizeHeadline(),
                        DocumentTextDecoration.DEFAULT, theme.palette().ink());
            }

            private DocumentTextStyle subtitleStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.BOLD, ACCENT);
            }

            private DocumentTextStyle contactMetaStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.DEFAULT, theme.palette().muted());
            }

            private DocumentTextStyle contactLinkStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.UNDERLINE, ACCENT);
            }

            private DocumentTextStyle contactSeparatorStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.DEFAULT, theme.palette().rule());
            }

            private static String initials(CvName name) {
                if (name == null) {
                    return "";
                }
                StringBuilder builder = new StringBuilder();
                appendInitial(builder, name.first());
                appendInitial(builder, name.last());
                return builder.toString();
            }

            private static void appendInitial(StringBuilder builder, String value) {
                if (builder.length() >= 2 || value == null) {
                    return;
                }
                String trimmed = value.trim();
                if (!trimmed.isEmpty() && Character.isLetter(trimmed.charAt(0))) {
                    builder.append(Character.toUpperCase(trimmed.charAt(0)));
                }
            }
        }
}
