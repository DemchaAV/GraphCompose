package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.GraphCompose;
import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.data.CvName;
import com.demcha.compose.document.templates.cv.v2.data.CvSkill;
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
    void skillBar_renders_with_and_without_level() throws Exception {
        CvTheme theme = CvTheme.mintEditorial();
        // Levelled skill → label + proficiency bar.
        renderWithSection(section ->
                SkillBar.render(section, CvSkill.of("Java 21", 0.9), 120, theme));
        // Name-only skill → label, no bar (graceful degrade).
        renderWithSection(section ->
                SkillBar.render(section, CvSkill.of("Kotlin"), 120, theme));
    }

    @Test
    void iconTextRow_renders_with_and_without_link() throws Exception {
        CvTheme theme = CvTheme.mintEditorial();
        DocumentImageData icon = DocumentImageData.fromBytes(readMintIcon("phone.png"));
        // Linked row (whole row clickable) and plain row.
        renderWithSection(section -> IconTextRow.render(section, icon, 9.0,
                "hello@example.com", theme.bodyStyle(),
                new com.demcha.compose.document.node.DocumentLinkOptions(
                        "mailto:hello@example.com"),
                DocumentInsets.bottom(12)));
        renderWithSection(section -> IconTextRow.render(section, icon, 9.0,
                "London, UK", theme.bodyStyle(), null, DocumentInsets.bottom(12)));
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
        renderWithSection(section ->
                SectionHeader.upperRule(section, "Skills",
                        CvTheme.nordicClean(), bodyStyle(),
                        DocumentColor.rgb(28, 128, 135), 64));
        renderWithSection(section ->
                SectionHeader.spacedCapsRule(section, "Experience", theme,
                        bodyStyle(), DocumentColor.rgb(126, 93, 52),
                        72, 1.0, DocumentInsets.zero()));
    }

    @Test
    void flowSectionHeader_variants_render_without_throwing() throws Exception {
        CvTheme theme = CvTheme.blueBanner();
        renderWithFlow(flow -> FlowSectionHeader.banner(flow, "FlowBanner",
                "Experience", 240, theme, bodyStyle(),
                DocumentInsets.top(2), DocumentInsets.bottom(2)));
        renderWithFlow(flow -> FlowSectionHeader.label(flow, "FlowLabel",
                "PROJECTS", 240, CvTheme.editorialBlue(), bodyStyle(),
                DocumentInsets.top(2), DocumentInsets.of(2),
                DocumentInsets.zero(), true));
    }

    @Test
    void moduleAndBand_widgets_render_without_throwing() throws Exception {
        CvTheme theme = CvTheme.nordicClean();
        renderWithSection(section -> ProfileBand.render(section, "Profile",
                "**Markdown** body", ProfileBand.Style.builder()
                        .titleStyle(bodyStyle())
                        .bodyStyle(bodyStyle())
                        .accentLeft(DocumentColor.rgb(28, 128, 135), 2)
                        .build()));
        renderWithSection(section -> SectionModule.tick(section, "Tick",
                "Skills", CvTheme.compactMono(),
                DocumentColor.rgb(0, 126, 151), 24, bodyStyle(),
                body -> body.addParagraph(p -> p.text("Java").textStyle(bodyStyle()))));
        renderWithSection(section -> SectionModule.upperRule(section, "Rule",
                "Experience", theme, bodyStyle(),
                DocumentColor.rgb(28, 128, 135), 72,
                body -> body.addParagraph(p -> p.text("Senior Engineer")
                        .textStyle(bodyStyle()))));
    }

    @Test
    void masthead_renders_without_throwing() throws Exception {
        CvTheme theme = CvTheme.editorialBlue();
        renderWithSection(section -> Masthead.centered(section, identity(),
                theme, Masthead.Style.builder()
                        .nameStyle(bodyStyle())
                        .titleStyle(bodyStyle())
                        .metaStyle(bodyStyle())
                        .linkStyle(underlinedLinkStyle())
                        .separatorStyle(bodyStyle())
                        .lineMargin(DocumentInsets.top(1))
                        .build()));
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

    private static void renderWithFlow(FlowAction action) throws Exception {
        try (DocumentSession session = GraphCompose.document()
                .pageSize(420, 595)
                .margin(DocumentInsets.of(24))
                .create()) {
            var flow = session.dsl().pageFlow().name("FlowWidgetTestRoot");
            action.run(flow);
            flow.build();
            assertThat(session.roots()).isNotEmpty();
        }
    }

    private static byte[] readMintIcon(String fileName) throws Exception {
        try (var input = WidgetSmokeTest.class.getResourceAsStream(
                "/templates/cv/mint-editorial/icons/" + fileName)) {
            assertThat(input).as("mint editorial icon %s", fileName).isNotNull();
            return input.readAllBytes();
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

    private static DocumentTextStyle bodyStyle() {
        return DocumentTextStyle.builder()
                .fontName(FontName.HELVETICA)
                .size(9)
                .decoration(DocumentTextDecoration.DEFAULT)
                .color(DocumentColor.rgb(30, 40, 55))
                .build();
    }

    @FunctionalInterface
    private interface SectionAction {
        void run(com.demcha.compose.document.dsl.SectionBuilder section);
    }

    @FunctionalInterface
    private interface FlowAction {
        void run(com.demcha.compose.document.dsl.PageFlowBuilder flow);
    }
}
