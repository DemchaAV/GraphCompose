package com.demcha.compose.document.templates.cv.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.cv.v2.components.BannerRenderer;
import com.demcha.compose.document.templates.cv.v2.components.ContactRenderer;
import com.demcha.compose.document.templates.cv.v2.components.HeadlineRenderer;
import com.demcha.compose.document.templates.cv.v2.components.SectionDispatcher;
import com.demcha.compose.document.templates.cv.v2.data.CvDocument;
import com.demcha.compose.document.templates.cv.v2.data.CvSection;
import com.demcha.compose.document.templates.cv.v2.data.Slot;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

import java.util.List;
import java.util.Objects;

/**
 * v2 reference preset — Boxed Sections.
 *
 * <p>Centred letter-spaced headline, thin rules above and below the
 * contact line, one pale-grey banner per section. Visual signature
 * of the canonical {@code cv-boxed-sections.pdf} reference output.</p>
 *
 * <p>The preset owns three responsibilities and no more:</p>
 *
 * <ol>
 *   <li>Define its identity ({@link #ID}, {@link #DISPLAY_NAME},
 *       {@link #RECOMMENDED_MARGIN}).</li>
 *   <li>Pick a default theme — {@link CvTheme#boxedClassic()} — that
 *       callers can override via the
 *       {@link #create(CvTheme)} overload.</li>
 *   <li>Walk {@link CvDocument#sections()} top-to-bottom, emitting a
 *       banner + body pair per section. All actual drawing is
 *       delegated to {@code cv/v2/components}.</li>
 * </ol>
 *
 * <p>Compare to the 700-line legacy preset that parsed dates,
 * sniffed module titles, and embedded RGB literals — this class is
 * the canonical example of what a v2 preset should look like.</p>
 */
public final class BoxedSections {

    /** Stable template identifier. */
    public static final String ID = "boxed-sections";

    /** Human-readable display name. */
    public static final String DISPLAY_NAME = "Boxed Sections";

    /** Recommended page margin (in points). */
    public static final double RECOMMENDED_MARGIN = 28.0;

    private BoxedSections() {
        // utility class — not instantiable
    }

    /**
     * Builds the preset with the classic Boxed Sections theme.
     *
     * @return ready-to-use template
     */
    public static DocumentTemplate<CvDocument> create() {
        return create(CvTheme.boxedClassic());
    }

    /**
     * Builds the preset with a caller-supplied theme. Allows callers
     * to ship an alternate palette / typography / spacing without
     * touching this class.
     *
     * @param theme active theme
     * @return ready-to-use template
     * @throws NullPointerException if {@code theme} is null
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

            PageFlowBuilder pageFlow = document.dsl()
                    .pageFlow()
                    .name("CvV2Root")
                    .spacing(theme.spacing().pageFlowSpacing())
                    .addSection("CvV2Headline", section -> {
                        section.accentBottom(theme.palette().rule(),
                                theme.spacing().accentRuleWidth());
                        HeadlineRenderer.render(section, doc.identity().name(), theme);
                    })
                    .addSection("CvV2Contact", section -> {
                        section.accentBottom(theme.palette().rule(),
                                theme.spacing().accentRuleWidth());
                        ContactRenderer.render(section, doc.identity(), theme);
                    });

            // Single-column preset — only renders MAIN-slot sections.
            // Sidebar / footer placements are intentionally dropped here;
            // switch to a multi-column preset to render them.
            List<CvSection> sections = doc.sectionsIn(Slot.MAIN);
            for (int i = 0; i < sections.size(); i++) {
                final CvSection sec = sections.get(i);
                final int idx = i;
                pageFlow.addSection("CvV2Banner_" + idx,
                        host -> BannerRenderer.render(host, sec.title(), theme));
                pageFlow.addSection("CvV2Body_" + idx,
                        host -> SectionDispatcher.renderBody(host, sec, theme));
            }

            pageFlow.build();
        }
    }
}
