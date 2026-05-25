package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.MarkdownInline;
import com.demcha.compose.document.templates.cv.v2.components.SectionDispatcher;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvRow;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.RowsSection;
import com.demcha.compose.document.templates.cv.v2.data.RowStyle;
import com.demcha.compose.document.templates.cv.v2.data.Slot;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.document.templates.cv.v2.widgets.SectionHeader;
import com.demcha.compose.document.templates.cv.v2.widgets.Subheadline;

import java.util.List;
import java.util.Objects;

/**
 * v2 port of the classic "Centered Headline" CV preset.
 *
 * <p>Visual signature ported from the legacy v1 preset:</p>
 * <ul>
 *   <li>Centred letter-spaced uppercase name in Poppins 24pt as the
 *       page's loudest element.</li>
 *   <li>A small centred {@code P R O F E S S I O N A L   T I T L E}
 *       subheadline beneath the name.</li>
 *   <li>Thin full-width rules above and below the centred contact
 *       line.</li>
 *   <li>Section titles rendered as <strong>quiet</strong> small
 *       spaced-caps Lato bold in a soft grey — left-aligned, no
 *       banner panel.</li>
 *   <li>Body content in Lato 8.7pt with markdown inline emphasis
 *       (the canonical {@link SectionDispatcher} body pipeline does
 *       the heavy lifting; project-style stacked rows are rendered
 *       without bullets for this classic preset).</li>
 *   <li>Thin inter-module rules separating each section block.</li>
 * </ul>
 *
 * <p>The preset reuses three existing widgets ({@link Headline},
 * {@link ContactLine}, {@link SectionHeader#flatSpacedCaps}) and one
 * newly introduced widget ({@link Subheadline}). The thin rules
 * between sections are emitted as inline {@code pageFlow.addLine(...)}
 * calls because they are part of the page-flow composition, not a
 * section-scoped widget — turning them into a widget would force every
 * caller to round-trip through a {@link com.demcha.compose.document.dsl.SectionBuilder},
 * which is the wrong granularity for a single-line ornament.</p>
 *
 * <p><strong>Why the subline says "PROFESSIONAL TITLE" verbatim:</strong>
 * the v2 {@link com.demcha.compose.document.templates.cv.v2.data.CvIdentity}
 * model does not carry a {@code jobTitle} field today — the v1 preset
 * hard-coded the same string for visual signature purposes and we
 * preserve that exactly to keep the baseline matching. When a
 * {@code jobTitle} field is introduced, swap the hard-coded string
 * for {@code doc.identity().jobTitle()} (with a sensible fallback).</p>
 */
public final class CenteredHeadline {

    /** Stable template identifier. */
    public static final String ID = "centered-headline";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Centered Headline";

    /** Recommended page margin (in points) — matches the legacy v1 preset. */
    public static final double RECOMMENDED_MARGIN = 28.0;

    /**
     * Fixed subheadline text. Matches the v1 preset's hard-coded
     * caption verbatim — see class Javadoc for rationale.
     */
    private static final String SUBHEADLINE_CAPTION = "Professional Title";

    private CenteredHeadline() {
        // utility class — not instantiable
    }

    /**
     * Builds the preset with the classic Centered Headline theme
     * ({@link CvTheme#centeredHeadline()}).
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.centeredHeadline());
    }

    /**
     * Builds the preset with a caller-supplied theme. Allows
     * variations on the Centered Headline theme (alternate
     * typography, slightly different palette) without forking this
     * class.
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private static final class Template implements DocumentTemplate<CvDocument> {

        private final CvTheme theme;

        Template(CvTheme theme) {
            this.theme = theme;
        }

        @Override
        public String id() {
            return ID;
        }

        @Override
        public String displayName() {
            return DISPLAY_NAME;
        }

        @Override
        public void compose(DocumentSession document, CvDocument doc) {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(doc, "doc");

            double ruleWidth = document.canvas().innerWidth();

            // -- name + subheadline + contact, framed by thin rules ----
            PageFlowBuilder pageFlow = document.dsl()
                    .pageFlow()
                    .name("CenteredHeadlineRoot")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .addSection("Headline", section -> {
                        Headline.spacedCentered(section, doc.identity().name(), theme);
                        Subheadline.centeredSpacedCaps(section, SUBHEADLINE_CAPTION,
                                subheadlineStyle());
                    })
                    .addLine(line -> rule(line, "HeadlineRule", ruleWidth, 7, 0))
                    .addSection("Contact",
                            section -> ContactLine.centered(section, doc.identity(), theme))
                    .addLine(line -> rule(line, "ContactRule", ruleWidth, 0, 8));

            // -- sections — flatSpacedCaps title, dispatched body,
            //    thin inter-module rule between consecutive sections.
            List<CvSection> sections = doc.sectionsIn(Slot.MAIN);
            for (int i = 0; i < sections.size(); i++) {
                final CvSection sec = sections.get(i);
                final int idx = i;
                pageFlow.addSection("Title_" + idx, host ->
                        SectionHeader.flatSpacedCaps(host, sec.title(),
                                theme.palette().muted(), theme, null));
                pageFlow.addLine(line ->
                        rule(line, "TitleBottomRule_" + idx, ruleWidth, 8, 8));
                pageFlow.addSection("Body_" + idx, host ->
                        renderBody(host, sec));
                if (idx < sections.size() - 1) {
                    pageFlow.addLine(line ->
                            rule(line, "InterModuleRule_" + idx, ruleWidth, 5, 8));
                }
            }

            pageFlow.build();
        }

        private void renderBody(SectionBuilder host, CvSection sec) {
            if (sec instanceof RowsSection rows
                    && rows.style() == RowStyle.BULLETED_STACKED) {
                host.spacing(theme.spacing().sectionBodySpacing())
                        .padding(theme.spacing().sectionBodyPadding());
                for (int i = 0; i < rows.rows().size(); i++) {
                    if (i > 0) {
                        host.spacer(0, theme.spacing().entrySeparation());
                    }
                    renderStackedProject(host, rows.rows().get(i));
                }
                return;
            }
            SectionDispatcher.renderBody(host, sec, theme);
        }

        private void renderStackedProject(SectionBuilder host, CvRow row) {
            DocumentTextStyle titleStyle = theme.bodyBoldStyle();
            DocumentTextStyle bodyStyle = theme.bodyStyle();
            host.addParagraph(p -> p
                    .text(row.label())
                    .textStyle(titleStyle)
                    .align(TextAlign.LEFT)
                    .lineSpacing(theme.typography().bodyLineSpacing())
                    .margin(DocumentInsets.top((float) theme.spacing().paragraphMarginTop())));
            if (!row.body().isBlank()) {
                host.addParagraph(p -> p
                        .textStyle(bodyStyle)
                        .align(TextAlign.LEFT)
                        .lineSpacing(theme.typography().bodyLineSpacing())
                        .margin(DocumentInsets.zero())
                        .rich(rich -> MarkdownInline.append(rich, row.body(), bodyStyle)));
            }
        }

        /**
         * Builds the subheadline text style. Inlined here (not a theme
         * slot) because no other widget reaches for this exact
         * combination — Subheadline takes the style as a parameter so
         * presets stay in control of the visual weight of their
         * captions.
         */
        private DocumentTextStyle subheadlineStyle() {
            return DocumentTextStyle.builder()
                    .fontName(theme.typography().headlineFont())
                    .size(8.6)
                    .decoration(DocumentTextDecoration.DEFAULT)
                    .color(theme.palette().muted())
                    .build();
        }

        /**
         * Configure a thin full-width horizontal rule. The widths and
         * margin pattern match the v1 preset exactly: top inset
         * creates breathing room above the rule, bottom inset before
         * the next paragraph.
         */
        private void rule(com.demcha.compose.document.dsl.LineBuilder line,
                          String name, double width, double top, double bottom) {
            line.name(name)
                    .horizontal(width)
                    .color(theme.palette().rule())
                    .thickness(theme.spacing().accentRuleWidth())
                    .margin(new DocumentInsets(top, 0, bottom, 0));
        }
    }
}
