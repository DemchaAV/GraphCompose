package com.demcha.compose.document.templates.support.cv;

import com.demcha.compose.document.api.DocumentSession;
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
import com.demcha.compose.font.FontName;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Minimal two-column CV inspired by classic timeline resume layouts.
 */
public final class TimelineMinimalCvTemplateComposer {
    private static final DocumentColor INK = DocumentColor.rgb(74, 74, 74);
    private static final DocumentColor SOFT = DocumentColor.rgb(122, 122, 122);
    private static final DocumentColor RULE = DocumentColor.rgb(195, 195, 195);
    private static final DocumentColor DOT = DocumentColor.rgb(170, 170, 170);
    private static final double TIMELINE_DOT = 7.0;
    private static final double TIMELINE_LINE_BOX = 1.0;
    private static final double TIMELINE_LINE_OFFSET = (TIMELINE_DOT - TIMELINE_LINE_BOX) / 2.0;

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");

        document.dsl()
                .pageFlow()
                .name("TimelineMinimalRoot")
                .spacing(12)
                .addRow("TimelineMinimalHeader", row -> row
                        .gap(18)
                        .weights(1.35, 0.65)
                        .addSection("TimelineMinimalName", section -> section
                                .spacing(4)
                                .addParagraph(paragraph -> paragraph
                                        .text(spacedUpper(name(spec.header())))
                                        .textStyle(style(FontName.BARLOW_CONDENSED, 28, DocumentTextDecoration.DEFAULT, INK))
                                        .margin(DocumentInsets.zero()))
                                .addParagraph(paragraph -> paragraph
                                        .text("PROFESSIONAL TITLE")
                                        .textStyle(style(FontName.BARLOW_CONDENSED, 9.5, DocumentTextDecoration.BOLD, INK))
                                        .margin(DocumentInsets.zero())))
                        .addSection("TimelineMinimalContact", section -> addContact(section, spec.header())))
                .addLine(line -> line
                        .name("TimelineMinimalHeaderRule")
                        .horizontal(document.canvas().innerWidth())
                        .color(RULE)
                        .thickness(0.8)
                        .margin(DocumentInsets.zero()))
                .addRow("TimelineMinimalBody", row -> addBodyRow(row,
                        List.of(new ModulePlacement("Education", find(spec, "education", "certifications"), 5),
                                new ModulePlacement("Skills", find(spec, "technical skills", "skills"), 6),
                                new ModulePlacement("Expertise", find(spec, "projects"), 3)),
                        List.of(new ModulePlacement("Professional Profile", find(spec, "summary", "professional summary", "profile"), 1),
                                new ModulePlacement("Work Experience", find(spec, "experience", "employment"), 4)),
                        300))
                .addPageBreak(pageBreak -> pageBreak.name("TimelineMinimalContinuedPage"))
                .addRow("TimelineMinimalBodyContinued", row -> addBodyRow(row,
                        List.of(new ModulePlacement("Languages", find(spec, "additional information", "additional"), 3),
                                new ModulePlacement("Interests", find(spec, "interests", "additional"), 4),
                                new ModulePlacement("References", find(spec, "references", "contact"), 5)),
                        List.of(new ModulePlacement("Work Experience Continued", find(spec, "experience", "employment"), 3),
                                new ModulePlacement("Professional Development", find(spec, "projects"), 4)),
                        320))
                .build();
    }

    private void addBodyRow(com.demcha.compose.document.dsl.RowBuilder row,
                            List<ModulePlacement> sidebarModules,
                            List<ModulePlacement> mainModules,
                            double axisHeight) {
        row.gap(16)
                .weights(0.74, 0.12, 1.74)
                .addSection("TimelineMinimalSidebar", sidebar -> {
                    sidebar.spacing(10);
                    for (ModulePlacement placement : sidebarModules) {
                        addSidebarModule(sidebar, placement.title(), placement.module(), placement.limit());
                    }
                })
                .addSection("TimelineMinimalAxis", axis -> addTimelineAxis(axis, axisHeight))
                .addSection("TimelineMinimalMain", main -> {
                    main.spacing(11);
                    for (ModulePlacement placement : mainModules) {
                        boolean bullets = placement.limit() > 1;
                        addMainModule(main, placement.title(), placement.module(), bullets, placement.limit());
                    }
                });
    }

    private void addContact(SectionBuilder section, Header header) {
        section.spacing(3);
        if (header == null) {
            return;
        }
        addContactLine(section, safe(header.getAddress()));
        addContactLine(section, safe(header.getPhoneNumber()));
        if (header.getEmail() != null) {
            addContactLine(section, header.getEmail().getDisplayText());
        }
        if (header.getLinkedIn() != null) {
            addContactLine(section, header.getLinkedIn().getDisplayText());
        }
    }

    private void addContactLine(SectionBuilder section, String text) {
        if (text.isBlank()) {
            return;
        }
        section.addParagraph(paragraph -> paragraph
                .text(text)
                .textStyle(style(FontName.LATO, 7.8, DocumentTextDecoration.BOLD, SOFT))
                .align(TextAlign.RIGHT)
                .margin(DocumentInsets.zero()));
    }

    private void addSidebarModule(SectionBuilder sidebar, String title, CvModule module, int limit) {
        List<String> lines = moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        sidebar.addSection("TimelineMinimalSidebar" + normalize(title), section -> {
            section.spacing(6)
                    .addParagraph(paragraph -> paragraph
                            .text(title.toUpperCase(Locale.ROOT))
                            .textStyle(style(FontName.BARLOW_CONDENSED, 12.5, DocumentTextDecoration.BOLD, INK))
                            .margin(DocumentInsets.zero()));
            for (String line : lines.stream().limit(limit).toList()) {
                section.addParagraph(paragraph -> paragraph
                        .text(excerpt(line, 76))
                        .textStyle(style(FontName.LATO, 7.5, DocumentTextDecoration.BOLD, INK))
                        .lineSpacing(1)
                        .margin(DocumentInsets.zero()));
            }
            section.addLine(line -> line
                    .horizontal(118)
                    .color(RULE)
                    .thickness(0.65)
                    .margin(DocumentInsets.top(5)));
        });
    }

    private void addTimelineAxis(SectionBuilder axis, double height) {
        double segment = height / 4;
        axis.spacing(0)
                .padding(new DocumentInsets(28, 0, 0, 0))
                .addLine(line -> timelineLine(line, segment))
                .addCircle(TIMELINE_DOT, circle -> circle.stroke(DocumentStroke.of(DOT, 0.8)).fillColor(DocumentColor.WHITE))
                .addLine(line -> timelineLine(line, segment))
                .addCircle(TIMELINE_DOT, circle -> circle.stroke(DocumentStroke.of(DOT, 0.8)).fillColor(DocumentColor.WHITE))
                .addLine(line -> timelineLine(line, segment))
                .addCircle(TIMELINE_DOT, circle -> circle.stroke(DocumentStroke.of(DOT, 0.8)).fillColor(DocumentColor.WHITE))
                .addLine(line -> timelineLine(line, segment));
    }

    private void timelineLine(com.demcha.compose.document.dsl.LineBuilder line, double height) {
        line.vertical(height)
                .color(RULE)
                .thickness(0.75)
                .margin(new DocumentInsets(0, 0, 0, TIMELINE_LINE_OFFSET));
    }

    private void addMainModule(SectionBuilder main, String title, CvModule module, boolean bullets, int limit) {
        List<String> lines = moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        main.addSection("TimelineMinimalMain" + normalize(title), section -> {
            section.spacing(5)
                    .addParagraph(paragraph -> paragraph
                            .text(title.toUpperCase(Locale.ROOT))
                            .textStyle(style(FontName.BARLOW_CONDENSED, 13.5, DocumentTextDecoration.BOLD, INK))
                            .margin(DocumentInsets.zero()));
            if (bullets) {
                for (String line : lines.stream().limit(limit).toList()) {
                    section.addParagraph(paragraph -> paragraph
                            .text(excerpt(line, 136))
                            .textStyle(style(FontName.LATO, 7.8, DocumentTextDecoration.DEFAULT, INK))
                            .lineSpacing(1.2)
                            .bulletOffset("-")
                            .margin(DocumentInsets.zero()));
                }
            } else {
                section.addParagraph(paragraph -> paragraph
                        .text(excerpt(lines.getFirst(), 245))
                        .textStyle(style(FontName.LATO, 7.9, DocumentTextDecoration.DEFAULT, INK))
                        .lineSpacing(1.4)
                        .margin(DocumentInsets.zero()));
            }
            section.addLine(line -> line
                    .horizontal(300)
                    .color(RULE)
                    .thickness(0.65)
                    .margin(DocumentInsets.top(6)));
        });
    }

    private record ModulePlacement(String title, CvModule module, int limit) {
    }

    private CvModule find(CvDocumentSpec spec, String... keys) {
        for (CvModule module : spec.modules()) {
            String haystack = normalize(module.name() + " " + module.title());
            for (String key : keys) {
                if (haystack.contains(normalize(key))) {
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
                case PARAGRAPH -> addLines(lines, block.text());
                case LIST -> block.items().forEach(item -> addLines(lines, item));
                default -> {
                    // This print-like template keeps custom blocks out of the compact flow.
                }
            }
        }
        return List.copyOf(lines);
    }

    private void addLines(List<String> lines, String value) {
        for (String line : safe(value).split("\\R")) {
            if (!line.isBlank()) {
                lines.add(line.trim());
            }
        }
    }

    private String name(Header header) {
        return header == null ? "" : safe(header.getName());
    }

    private String spacedUpper(String value) {
        String upper = safe(value).toUpperCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < upper.length(); i++) {
            char current = upper.charAt(i);
            builder.append(current);
            if (Character.isWhitespace(current)) {
                builder.append("  ");
            }
            if (Character.isLetter(current) && i + 1 < upper.length() && Character.isLetter(upper.charAt(i + 1))) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }

    private String normalize(String value) {
        String safeValue = safe(value).toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < safeValue.length(); i++) {
            char current = safeValue.charAt(i);
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private String stripMarkdown(String value) {
        return safe(value).replace("**", "").replace("*", "").replace("`", "").replace("_", "");
    }

    private String excerpt(String value, int maxChars) {
        String clean = stripMarkdown(value).replaceAll("\\s+", " ").trim();
        if (clean.length() <= maxChars) {
            return clean;
        }
        int boundary = clean.lastIndexOf(' ', maxChars - 1);
        int end = boundary > maxChars / 2 ? boundary : maxChars - 1;
        return clean.substring(0, end).trim() + "...";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private DocumentTextStyle style(FontName font, double size, DocumentTextDecoration decoration, DocumentColor color) {
        return DocumentTextStyle.builder()
                .fontName(font)
                .size(size)
                .decoration(decoration)
                .color(color)
                .build();
    }
}
