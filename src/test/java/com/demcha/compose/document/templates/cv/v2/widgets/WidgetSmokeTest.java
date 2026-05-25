package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvName;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.font.FontName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the cv/v2/widgets layer. Each widget gets called in
 * every variant exposed at the public API, against the default
 * theme, to catch DSL-level regressions before they surface in the
 * preset render outputs.
 */
class WidgetSmokeTest {

    @Test
    void headline_variants_render_without_throwing() throws Exception {
        renderWithSection(section -> {
            Headline.spacedCentered(section, name(), CvTheme.boxedClassic());
        });
        renderWithSection(section -> {
            Headline.uppercaseCentered(section, name(), CvTheme.editorialBlue());
        });
        renderWithSection(section -> {
            Headline.uppercaseLeftAligned(section, name(), CvTheme.nordicClean());
        });
        renderWithSection(section -> {
            Headline.rightAligned(section, name(), CvTheme.boxedClassic());
        });
        renderWithSection(section -> {
            Headline.render(section, name(), CvTheme.boxedClassic(),
                    TextAlign.LEFT, false);
        });
    }

    @Test
    void contactLine_variants_render_without_throwing() throws Exception {
        renderWithSection(section -> {
            ContactLine.centered(section, identity(), CvTheme.boxedClassic());
        });
        renderWithSection(section -> {
            ContactLine.centered(section, identity(), CvTheme.boxedClassic(),
                    null, underlinedLinkStyle(), null);
        });
        renderWithSection(section -> {
            ContactLine.rightAligned(section, identity(), CvTheme.boxedClassic());
        });
        renderWithSection(section -> {
            ContactLine.leftAligned(section, identity(), CvTheme.compactMono(),
                    null, underlinedLinkStyle(), null);
        });
        renderWithSection(section -> {
            ContactLine.rightAlignedStacked(section, identity(),
                    CvTheme.nordicClean(), null, underlinedLinkStyle());
        });
        renderWithSection(section -> {
            ContactLine.render(section, identity(), CvTheme.boxedClassic(),
                    TextAlign.LEFT, ContactLine.Order.PHONE_FIRST);
        });
    }

    @Test
    void subheadline_variants_render_without_throwing() throws Exception {
        CvTheme theme = CvTheme.centeredHeadline();
        renderWithSection(section ->
                Subheadline.centeredSpacedCaps(section, "Professional Title",
                        theme.bodyStyle()));
    }

    @Test
    void sectionHeader_variants_render_without_throwing() throws Exception {
        CvTheme theme = CvTheme.boxedClassic();
        renderWithSection(section ->
                SectionHeader.banner(section, "Professional Summary", theme));
        renderWithSection(section ->
                SectionHeader.fullWidthBanner(section, "Professional Summary",
                        CvTheme.blueBanner()));
        renderWithSection(section ->
                SectionHeader.underlined(section, "Skills", theme));
        renderWithSection(section ->
                SectionHeader.flat(section, "Experience",
                        DocumentColor.rgb(41, 128, 185), theme));
        renderWithSection(section ->
                SectionHeader.flatSpacedCaps(section, "Projects",
                        theme.palette().muted(), theme, null));
        renderWithSection(section ->
                SectionHeader.tickLabel(section, "Projects",
                        CvTheme.compactMono(),
                        DocumentColor.rgb(0, 126, 151), 22));
    }

    @Test
    void widgets_work_against_modernProfessional_theme() throws Exception {
        CvTheme theme = CvTheme.modernProfessional();
        renderWithSection(section -> Headline.rightAligned(section, name(), theme));
        renderWithSection(section -> ContactLine.rightAligned(section, identity(), theme));
        renderWithSection(section -> SectionHeader.flat(section, "Summary",
                DocumentColor.rgb(0, 0, 0), theme));
    }

    private static void renderWithSection(SectionAction action) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.of(24))
                .create()) {
            session.dsl().pageFlow()
                    .name("WidgetTestRoot")
                    .addSection("WidgetSlot", action::run)
                    .build();
            assertThat(session.roots()).isNotEmpty();
        }
    }

    private static CvName name() {
        return CvName.of("Jane", "Doe");
    }

    private static CvIdentity identity() {
        return CvIdentity.builder()
                .name("Jane", "Doe")
                .contact("+44 0", "j@d.com", "London")
                .link("LinkedIn", "https://linkedin.com/in/jane-doe")
                .build();
    }

    private static DocumentTextStyle underlinedLinkStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.PT_SERIF)
                .size(8.5)
                .decoration(DocumentTextDecoration.UNDERLINE)
                .color(DocumentColor.rgb(126, 93, 52))
                .build();
    }

    @FunctionalInterface
    private interface SectionAction {
        void run(com.demcha.compose.document.dsl.SectionBuilder section);
    }
}
