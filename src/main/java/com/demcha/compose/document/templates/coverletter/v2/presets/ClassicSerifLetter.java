package com.demcha.compose.document.templates.coverletter.v2.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.api.DocumentTemplate;
import com.demcha.compose.document.templates.coverletter.v2.components.LetterBody;
import com.demcha.compose.document.templates.coverletter.v2.data.CoverLetterDocument;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;
import com.demcha.compose.document.templates.cv.v2.widgets.Headline;

import java.util.Objects;

/**
 * v2 cover-letter pair for the {@code ClassicSerif} CV preset.
 *
 * <p>Renders the <strong>identical masthead</strong> as
 * {@link com.demcha.compose.document.templates.cv.v2.presets.ClassicSerif}
 * — a centred letter-spaced PT-Serif name, a thin tan rule, and a
 * centred contact line with tan-accent underlined links — then a
 * single-column letter body via the shared {@link LetterBody}. Both
 * documents read their palette / typography from
 * {@link CvTheme#classicSerif()}.</p>
 *
 * <p>The header mirrors the CV's preset-local header DSL (spaced name +
 * rule line + centred contact). The bronze {@code ACCENT} is mirrored
 * from the CV, which keeps it preset-local there because no other brand
 * shares that fifth colour token.</p>
 */
public final class ClassicSerifLetter {

    /**
     * Stable template identifier.
     */
    public static final String ID = "classic-serif-letter";

    /**
     * Human-readable display name.
     */
    public static final String DISPLAY_NAME = "Classic Serif Letter";

    /**
     * Recommended page margin (in points) — generous business-letter feel.
     */
    public static final double RECOMMENDED_MARGIN = 48.0;

    /**
     * Bronze accent for contact links. Mirrors the ClassicSerif CV's preset-local token.
     */
    private static final DocumentColor ACCENT = DocumentColor.rgb(126, 93, 52);

    private ClassicSerifLetter() {
    }

    /**
     * Builds the letter with its Classic Serif theme.
     *
     * @return a {@code DocumentTemplate} for the "Classic Serif Letter"
     */
    public static DocumentTemplate<CoverLetterDocument> create() {
        return create(CvTheme.classicSerif());
    }

    /**
     * Builds the letter with a caller-supplied theme (share the paired
     * CV's theme instance for a guaranteed visual match).
     *
     * @param theme the active theme supplying palette, typography, and spacing
     * @return a {@code DocumentTemplate} for the "Classic Serif Letter"
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

                double width = document.canvas().innerWidth();
                PageFlowBuilder flow = document.dsl()
                        .pageFlow()
                        .name("CoverLetterV2ClassicSerifRoot")
                        .spacing(theme.spacing().pageFlowSpacing());

                addHeader(flow, doc.identity(), width);

                flow.addSection("CoverLetterV2ClassicSerifBody", host ->
                        LetterBody.render(host, doc, theme));

                flow.build();
            }

            private void addHeader(PageFlowBuilder flow, CvIdentity identity,
                                   double width) {
                flow.addSection("CoverLetterV2ClassicSerifHeader", section -> {
                    section.spacing(5);
                    Headline.spacedCentered(section, identity.name(), theme);
                    section.addLine(line -> line
                            .name("CoverLetterV2ClassicSerifHeaderRule")
                            .horizontal(width)
                            .color(theme.palette().rule())
                            .thickness(theme.spacing().accentRuleWidth())
                            .margin(new DocumentInsets(1, 0, 0, 0)));
                    ContactLine.centered(section, identity, theme,
                            contactMetaStyle(), contactLinkStyle(),
                            contactSeparatorStyle());
                });
            }

            private DocumentTextStyle contactMetaStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.DEFAULT,
                        theme.palette().muted());
            }

            private DocumentTextStyle contactLinkStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.UNDERLINE,
                        ACCENT);
            }

            private DocumentTextStyle contactSeparatorStyle() {
                return CvTextStyles.of(theme.typography().bodyFont(),
                        theme.typography().sizeContact(),
                        DocumentTextDecoration.DEFAULT,
                        theme.palette().rule());
            }
        }
}
