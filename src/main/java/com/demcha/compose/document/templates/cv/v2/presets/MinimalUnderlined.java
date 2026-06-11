package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.SectionDispatcher;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.Slot;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;
import com.demcha.compose.document.templates.cv.v2.widgets.SectionHeader;

import java.util.List;
import java.util.Objects;

/**
 * v2 demonstration preset — Minimal Underlined.
 *
 * <p>Same data model ({@link CvDocument}), same theme tokens
 * ({@link CvTheme}), same body renderers — but the section header
 * is drawn as a small left-aligned uppercase title with an
 * underline rule instead of a centred banner panel.</p>
 *
 * <p><strong>Purpose:</strong> show that writing a new preset is a
 * compositional exercise. The pieces that change:</p>
 *
 * <ul>
 *   <li>{@link com.demcha.compose.document.templates.cv.v2.components.BannerRenderer}
 *       is <em>not</em> used.</li>
 *   <li>A small inline paragraph + an {@code accentBottom} draws the
 *       section title.</li>
 * </ul>
 *
 * <p>Everything else — identity rendering, contact line, section
 * body dispatch — is the same call into the same components as
 * {@link BoxedSections}. No code is duplicated, only composed
 * differently.</p>
 *
 * <p>Compare {@code BoxedSections.compose()} and
 * {@code MinimalUnderlined.compose()} side by side: they are
 * structurally identical except for the section-title rendering.</p>
 */
public final class MinimalUnderlined {

    /**
     * Stable template identifier.
     */
    public static final String ID = "minimal-underlined";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Minimal Underlined";

    /**
     * Recommended page margin (in points).
     */
    public static final double RECOMMENDED_MARGIN = 32.0;

    private MinimalUnderlined() {
    }

    /**
     * Builds the preset with the classic theme.
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.boxedClassic());
    }

    /**
     * Builds the preset with a caller-supplied theme.
     *
     * @param theme active theme
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create(CvTheme theme) {
        Objects.requireNonNull(theme, "theme");
        return new Template(theme);
    }

    private record Template(CvTheme theme) implements DocumentTemplate<CvDocument> {

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

                PageFlowBuilder pageFlow = document.dsl()
                        .pageFlow()
                        .name("CvV2MinimalRoot")
                        .spacing(theme.spacing().pageFlowSpacing())
                        .addSection("Headline", section ->
                                Headline.spacedCentered(section, doc.identity().name(), theme))
                        .addSection("Contact", section -> {
                            section.accentBottom(theme.palette().rule(),
                                    theme.spacing().accentRuleWidth());
                            ContactLine.centered(section, doc.identity(), theme);
                        });

                // Single-column preset — only renders MAIN-slot sections.
                // Sidebar / footer placements are intentionally dropped here.
                List<CvSection> sections = doc.sectionsIn(Slot.MAIN);
                for (int i = 0; i < sections.size(); i++) {
                    final CvSection sec = sections.get(i);
                    final int idx = i;
                    pageFlow.addSection("Title_" + idx, host ->
                            SectionHeader.underlined(host, sec.title(), theme));
                    pageFlow.addSection("Body_" + idx, host ->
                            SectionDispatcher.renderBody(host, sec, theme));
                }

                pageFlow.build();
            }
        }
}
