package com.demcha.compose.document.templates.support.cv;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.data.cv.CvModule;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.font.FontName;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Panel-led CV composer for more visually distinctive built-in templates.
 */
public final class PanelCvTemplateComposer {
    private final String rootName;
    private final CvTheme theme;
    private final Palette palette;
    private final Layout layout;

    public PanelCvTemplateComposer(String rootName, CvTheme theme, Palette palette, Layout layout) {
        this.rootName = rootName == null || rootName.isBlank() ? "PanelCvRoot" : rootName;
        this.theme = Objects.requireNonNull(theme, "theme");
        this.palette = Objects.requireNonNull(palette, "palette");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");
        PageFlowBuilder flow = document.dsl()
                .pageFlow()
                .name(rootName)
                .spacing(layout.rootSpacing());

        addHeader(flow, spec.header());
        if (layout.twoColumn()) {
            addTwoColumnBody(flow, spec);
        } else {
            addStackedBody(flow, spec);
        }
        flow.build();
    }

    private void addHeader(PageFlowBuilder flow, Header header) {
        if (header == null) {
            return;
        }
        flow.addSection("PanelCvHeader", section -> section
                .spacing(4)
                .padding(layout.headerPadding())
                .fillColor(color(palette.headerFill()))
                .stroke(stroke(palette.panelStroke(), 0.4))
                .cornerRadius(layout.cornerRadius())
                .addParagraph(paragraph -> paragraph
                        .text(safe(header.getName()).toUpperCase(Locale.ROOT))
                        .textStyle(style(theme.headerFont(), theme.nameFontSize(), DocumentTextDecoration.BOLD, palette.headerText()))
                        .align(layout.headerAlign())
                        .margin(DocumentInsets.zero()))
                .addParagraph(paragraph -> paragraph
                        .text(contactLine(header))
                        .textStyle(style(theme.bodyFont(), theme.bodyFontSize() - 0.5, DocumentTextDecoration.DEFAULT, palette.headerMeta()))
                        .align(layout.headerAlign())
                        .margin(DocumentInsets.zero()))
                .addParagraph(paragraph -> paragraph
                        .text(linkLine(header))
                        .textStyle(style(theme.bodyFont(), theme.bodyFontSize() - 0.5, DocumentTextDecoration.UNDERLINE, palette.accent()))
                        .align(layout.headerAlign())
                        .margin(DocumentInsets.zero())));
    }

    private void addTwoColumnBody(PageFlowBuilder flow, CvDocumentSpec spec) {
        flow.addRow("PanelCvColumns", row -> row
                .gap(layout.columnGap())
                .weights(layout.sidebarWeight(), layout.mainWeight())
                .addSection("PanelCvSidebar", sidebar -> {
                    sidebar.spacing(layout.panelGap())
                            .padding(layout.sidebarPadding())
                            .fillColor(color(palette.sidebarFill()))
                            .stroke(stroke(palette.panelStroke(), 0.35))
                            .cornerRadius(layout.cornerRadius());
                    addModulePanel(sidebar, "TechnicalSkills", "Skills", findModule(spec, "technical skills", "skills"), true);
                    addModulePanel(sidebar, "Education", "Education", findModule(spec, "education", "certifications"), false);
                    addModulePanel(sidebar, "Additional", "Additional", findModule(spec, "additional information", "additional"), false);
                })
                .addSection("PanelCvMain", main -> {
                    main.spacing(layout.panelGap());
                    addModulePanel(main, "Summary", "Profile", findModule(spec, "summary", "professional summary", "profile"), false);
                    addModulePanel(main, "Experience", "Experience", findModule(spec, "experience", "employment"), false);
                    addModulePanel(main, "Projects", "Selected Projects", findModule(spec, "projects"), false);
                }));
    }

    private void addStackedBody(PageFlowBuilder flow, CvDocumentSpec spec) {
        addModulePanel(flow, "Summary", "Profile", findModule(spec, "summary", "professional summary", "profile"), false);
        flow.addRow("PanelCvStackedCards", row -> row
                .gap(layout.columnGap())
                .weights(1, 1)
                .addSection("PanelCvStackedLeft", left -> {
                    left.spacing(layout.panelGap());
                    addModulePanel(left, "Skills", "Skills", findModule(spec, "technical skills", "skills"), true);
                    addModulePanel(left, "Education", "Education", findModule(spec, "education", "certifications"), false);
                })
                .addSection("PanelCvStackedRight", right -> {
                    right.spacing(layout.panelGap());
                    addModulePanel(right, "Experience", "Experience", findModule(spec, "experience", "employment"), false);
                    addModulePanel(right, "Projects", "Projects", findModule(spec, "projects"), false);
                }));
        addModulePanel(flow, "Additional", "Additional", findModule(spec, "additional information", "additional"), false);
    }

    private void addModulePanel(PageFlowBuilder flow, String name, String title, CvModule module, boolean compactList) {
        flow.addSection("PanelCv" + name + "Panel", section -> addModulePanelContent(section, title, module, compactList));
    }

    private void addModulePanel(SectionBuilder parent, String name, String title, CvModule module, boolean compactList) {
        parent.addSection("PanelCv" + name + "Panel", section -> addModulePanelContent(section, title, module, compactList));
    }

    private void addModulePanelContent(SectionBuilder section, String title, CvModule module, boolean compactList) {
        List<String> lines = moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }

        section.spacing(layout.innerSpacing())
                .padding(layout.panelPadding())
                .fillColor(color(palette.panelFill()))
                .stroke(stroke(palette.panelStroke(), 0.45))
                .cornerRadius(layout.cornerRadius())
                .addParagraph(paragraph -> paragraph
                        .text(title.toUpperCase(Locale.ROOT))
                        .textStyle(style(theme.headerFont(), theme.headerFontSize(), DocumentTextDecoration.BOLD, palette.accent()))
                        .margin(DocumentInsets.zero()))
                .addShape(shape -> shape
                        .name("PanelCv" + title.replaceAll("\\W+", "") + "Accent")
                        .size(layout.accentWidth(), layout.accentHeight())
                        .fillColor(color(palette.accent()))
                        .cornerRadius(layout.accentHeight()));

        if (compactList || lines.size() > 1) {
            section.addList(list -> list
                    .items(lines)
                    .bullet()
                    .textStyle(bodyStyle())
                    .itemSpacing(layout.listItemSpacing())
                    .lineSpacing(layout.lineSpacing())
                    .margin(DocumentInsets.zero()));
        } else {
            section.addParagraph(paragraph -> paragraph
                    .text(lines.getFirst())
                    .textStyle(bodyStyle())
                    .lineSpacing(layout.lineSpacing())
                    .margin(DocumentInsets.zero()));
        }
    }

    private CvModule findModule(CvDocumentSpec spec, String... keys) {
        for (CvModule module : spec.modules()) {
            String normalized = normalize(module.name() + " " + module.title());
            for (String key : keys) {
                if (normalized.contains(normalize(key))) {
                    return module;
                }
            }
        }
        return null;
    }

    private List<String> moduleLines(CvModule module) {
        if (module == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (CvModule.BodyBlock block : module.bodyBlocks()) {
            switch (block.kind()) {
                case PARAGRAPH -> addSplitLines(lines, block.text());
                case LIST -> block.items().forEach(item -> addSplitLines(lines, item));
                default -> {
                    // Panel templates intentionally keep rich/table custom blocks out of compact cards.
                }
            }
        }
        return List.copyOf(lines);
    }

    private void addSplitLines(List<String> lines, String value) {
        for (String line : stripMarkdown(value).split("\\R")) {
            if (!line.isBlank()) {
                lines.add(line.trim());
            }
        }
    }

    private String contactLine(Header header) {
        return joinNonBlank(" | ", header.getAddress(), header.getPhoneNumber());
    }

    private String linkLine(Header header) {
        return joinNonBlank(" | ",
                header.getEmail() == null ? "" : header.getEmail().getDisplayText(),
                header.getLinkedIn() == null ? "" : header.getLinkedIn().getDisplayText(),
                header.getGitHub() == null ? "" : header.getGitHub().getDisplayText());
    }

    private String joinNonBlank(String separator, String... values) {
        List<String> parts = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    parts.add(value.trim());
                }
            }
        }
        return String.join(separator, parts);
    }

    private String normalize(String value) {
        String safe = safe(value).toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < safe.length(); i++) {
            char current = safe.charAt(i);
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private static String stripMarkdown(String value) {
        return safe(value).replace("**", "").replace("*", "").replace("`", "").replace("_", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private DocumentTextStyle bodyStyle() {
        return style(theme.bodyFont(), theme.bodyFontSize(), DocumentTextDecoration.DEFAULT, palette.bodyText());
    }

    private DocumentTextStyle style(FontName font, double size, DocumentTextDecoration decoration, Color color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color(color))
                .build();
    }

    private DocumentColor color(Color color) {
        return DocumentColor.of(color);
    }

    private DocumentStroke stroke(Color color, double width) {
        return DocumentStroke.of(color(color), width);
    }

    public record Palette(
            Color headerFill,
            Color headerText,
            Color headerMeta,
            Color sidebarFill,
            Color panelFill,
            Color panelStroke,
            Color accent,
            Color bodyText
    ) {
    }

    public record Layout(
            boolean twoColumn,
            TextAlign headerAlign,
            DocumentInsets headerPadding,
            DocumentInsets sidebarPadding,
            DocumentInsets panelPadding,
            double rootSpacing,
            double columnGap,
            double sidebarWeight,
            double mainWeight,
            double panelGap,
            double innerSpacing,
            double cornerRadius,
            double accentWidth,
            double accentHeight,
            double listItemSpacing,
            double lineSpacing
    ) {
        public static Layout sidebar(TextAlign align) {
            return new Layout(
                    true,
                    align,
                    new DocumentInsets(14, 16, 14, 16),
                    DocumentInsets.of(10),
                    DocumentInsets.of(10),
                    11,
                    12,
                    0.78,
                    1.52,
                    9,
                    5,
                    7,
                    46,
                    2.2,
                    3,
                    1.2);
        }

        public static Layout stacked(TextAlign align) {
            return new Layout(
                    false,
                    align,
                    new DocumentInsets(14, 16, 14, 16),
                    DocumentInsets.of(10),
                    DocumentInsets.of(10),
                    10,
                    12,
                    1,
                    1,
                    9,
                    5,
                    7,
                    54,
                    2.2,
                    3,
                    1.2);
        }
    }
}
