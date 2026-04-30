package com.demcha.compose.document.templates.support.cv;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.DocumentLinkOptions;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.data.common.EmailYaml;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.common.LinkYml;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.data.cv.CvModule;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.font.FontName;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Editorial serif CV with a measured cover page, generous rules, and a quieter
 * second-page detail flow.
 */
public final class ClassicSerifCvTemplateComposer {
    private static final DocumentColor INK = DocumentColor.rgb(45, 43, 40);
    private static final DocumentColor MUTED = DocumentColor.rgb(105, 101, 94);
    private static final DocumentColor RULE = DocumentColor.rgb(187, 177, 160);
    private static final DocumentColor SOFT_FILL = DocumentColor.rgb(250, 247, 241);
    private static final DocumentColor ACCENT = DocumentColor.rgb(126, 93, 52);

    private final CvTheme theme;

    public ClassicSerifCvTemplateComposer() {
        this(defaultTheme());
    }

    public ClassicSerifCvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");
        PageFlowBuilder flow = document.dsl()
                .pageFlow()
                .name("ClassicSerifRoot")
                .spacing(8);

        addHeader(flow, spec.header(), document.canvas().innerWidth());
        addSummary(flow, findModule(spec, "summary", "professional summary", "profile"));
        addFeatureRow(flow, spec);
        flow.addPageBreak(pageBreak -> pageBreak.name("ClassicSerifDetailsPage"));
        addLinearModule(flow, "Experience", findModule(spec, "experience", "employment"));
        addLinearModule(flow, "Projects", findModule(spec, "projects"));
        addLinearModule(flow, "Education", findModule(spec, "education", "certifications"));
        addLinearModule(flow, "Additional", findModule(spec, "additional information", "additional"));
        flow.build();
    }

    private void addHeader(PageFlowBuilder flow, Header header, double width) {
        flow.addSection("ClassicSerifHeader", section -> {
            section.spacing(5)
                    .padding(new DocumentInsets(8, 0, 7, 0))
                    .addParagraph(paragraph -> paragraph
                            .text(spacedUpper(safe(header == null ? "" : header.getName())))
                            .textStyle(style(theme.headerFont(), 27.0, DocumentTextDecoration.DEFAULT, INK))
                            .align(TextAlign.CENTER)
                            .margin(DocumentInsets.zero()))
                    .addLine(line -> line
                            .name("ClassicSerifHeaderRule")
                            .horizontal(width)
                            .color(RULE)
                            .thickness(0.65)
                            .margin(new DocumentInsets(1, 0, 0, 0)));
            addContact(section, header);
        });
    }

    private void addContact(SectionBuilder section, Header header) {
        List<ContactPart> parts = contactParts(header);
        if (parts.isEmpty()) {
            return;
        }
        DocumentTextStyle meta = style(theme.bodyFont(), 8.7, DocumentTextDecoration.DEFAULT, MUTED);
        DocumentTextStyle link = style(theme.bodyFont(), 8.7, DocumentTextDecoration.UNDERLINE, ACCENT);
        DocumentTextStyle separator = style(theme.bodyFont(), 8.7, DocumentTextDecoration.DEFAULT, RULE);
        section.addParagraph(paragraph -> paragraph
                .textStyle(meta)
                .align(TextAlign.CENTER)
                .lineSpacing(1.15)
                .margin(DocumentInsets.zero())
                .rich(rich -> {
                    for (int index = 0; index < parts.size(); index++) {
                        ContactPart part = parts.get(index);
                        if (part.linkOptions() == null) {
                            rich.style(part.text(), meta);
                        } else {
                            rich.with(part.text(), link, part.linkOptions());
                        }
                        if (index < parts.size() - 1) {
                            rich.style("   |   ", separator);
                        }
                    }
                }));
    }

    private void addSummary(PageFlowBuilder flow, CvModule summary) {
        String text = String.join(" ", moduleLines(summary));
        if (text.isBlank()) {
            return;
        }
        flow.addSection("ClassicSerifSummary", section -> {
            section.spacing(5)
                    .padding(new DocumentInsets(12, 18, 13, 18))
                    .fillColor(SOFT_FILL)
                    .accentTop(ACCENT, 1.15)
                    .accentBottom(RULE, 0.45);
            addTitle(section, "Professional Profile", TextAlign.CENTER);
            section.addParagraph(paragraph -> paragraph
                    .text(stripMarkdown(text))
                    .textStyle(style(theme.bodyFont(), 9.8, DocumentTextDecoration.DEFAULT, INK))
                    .lineSpacing(1.55)
                    .align(TextAlign.CENTER)
                    .margin(DocumentInsets.zero()));
        });
    }

    private void addFeatureRow(PageFlowBuilder flow, CvDocumentSpec spec) {
        flow.addRow("ClassicSerifFirstPageGrid", row -> row
                .spacing(15)
                .weights(1.0, 1.0)
                .addSection("ClassicSerifSkillsColumn", left -> {
                    left.spacing(7);
                    addFeatureModule(left, "Core Skills", findModule(spec, "technical skills", "skills"), 7);
                    addFeatureModule(left, "Education", findModule(spec, "education", "certifications"), 3);
                })
                .addSection("ClassicSerifEvidenceColumn", right -> {
                    right.spacing(7);
                    addFeatureModule(right, "Experience", findModule(spec, "experience", "employment"), 2);
                    addFeatureModule(right, "Selected Projects", findModule(spec, "projects"), 3);
                }));
    }

    private void addFeatureModule(SectionBuilder parent, String title, CvModule module, int limit) {
        List<String> lines = moduleLines(module).stream().limit(limit).toList();
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("ClassicSerifFeature" + normalize(title), section -> {
            section.spacing(4)
                    .padding(new DocumentInsets(9, 10, 10, 10))
                    .stroke(DocumentStroke.of(RULE, 0.35));
            addTitle(section, title, TextAlign.LEFT);
            section.addList(list -> list
                    .items(lines.stream().map(ClassicSerifCvTemplateComposer::stripMarkdown).toList())
                    .bullet()
                    .textStyle(style(theme.bodyFont(), 8.65, DocumentTextDecoration.DEFAULT, INK))
                    .lineSpacing(1.25)
                    .itemSpacing(2.0)
                    .margin(DocumentInsets.zero()));
        });
    }

    private void addLinearModule(PageFlowBuilder flow, String title, CvModule module) {
        List<String> lines = moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        flow.addSection("ClassicSerif" + normalize(title), section -> {
            section.spacing(4)
                    .padding(new DocumentInsets(0, 0, 2, 0));
            addTitle(section, title, TextAlign.LEFT);
            section.addLine(line -> line
                    .name("ClassicSerif" + normalize(title) + "Rule")
                    .horizontal(72)
                    .color(ACCENT)
                    .thickness(1.0)
                    .margin(new DocumentInsets(0, 0, 2, 0)));
            for (String line : lines) {
                renderLine(section, line);
            }
        });
    }

    private void renderLine(SectionBuilder section, String rawLine) {
        WorkEntry entry = parseWorkEntry(rawLine);
        if (entry == null) {
            section.addParagraph(paragraph -> paragraph
                    .text(stripMarkdown(rawLine))
                    .textStyle(style(theme.bodyFont(), 9.0, DocumentTextDecoration.DEFAULT, INK))
                    .lineSpacing(1.35)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1)));
            return;
        }

        section.addRow("ClassicSerifEntryHeader", row -> row
                .spacing(12)
                .weights(1.0, 0.36)
                .addSection("Title", title -> title
                        .padding(DocumentInsets.zero())
                        .addParagraph(paragraph -> paragraph
                                .text(stripMarkdown(entry.heading()))
                                .textStyle(style(theme.bodyFont(), 9.2, DocumentTextDecoration.BOLD, INK))
                                .align(TextAlign.LEFT)
                                .margin(DocumentInsets.zero())))
                .addSection("Date", date -> date
                        .padding(DocumentInsets.zero())
                        .addParagraph(paragraph -> paragraph
                                .text(stripMarkdown(entry.date()))
                                .textStyle(style(theme.bodyFont(), 8.7, DocumentTextDecoration.DEFAULT, MUTED))
                                .align(TextAlign.RIGHT)
                                .margin(DocumentInsets.zero()))));
        if (!entry.description().isBlank()) {
            section.addParagraph(paragraph -> paragraph
                    .text(stripMarkdown(entry.description()))
                    .textStyle(style(theme.bodyFont(), 8.8, DocumentTextDecoration.DEFAULT, INK))
                    .lineSpacing(1.35)
                    .align(TextAlign.LEFT)
                    .margin(DocumentInsets.top(1)));
        }
    }

    private void addTitle(SectionBuilder section, String title, TextAlign align) {
        section.addParagraph(paragraph -> paragraph
                .text(spacedUpper(title))
                .textStyle(style(theme.headerFont(), 9.2, DocumentTextDecoration.BOLD, ACCENT))
                .align(align)
                .margin(DocumentInsets.zero()));
    }

    private WorkEntry parseWorkEntry(String item) {
        int pipeIndex = safe(item).indexOf('|');
        if (pipeIndex < 0) {
            return null;
        }
        String heading = item.substring(0, pipeIndex).trim();
        String afterPipe = item.substring(pipeIndex + 1).trim();
        int dash = afterPipe.indexOf(" - ");
        if (heading.isBlank() || dash <= 0) {
            return null;
        }
        String date = afterPipe.substring(0, dash).trim();
        String description = afterPipe.substring(dash + 3).trim();
        if (!date.matches(".*(\\d{4}|Present|present|Ongoing|ongoing).*")) {
            return null;
        }
        return new WorkEntry(heading, date, description);
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
                case PARAGRAPH -> splitInto(lines, block.text());
                case LIST -> {
                    for (String item : block.items()) {
                        splitInto(lines, item);
                    }
                }
                default -> {
                    // Serif layout intentionally consumes plain narrative modules.
                }
            }
        }
        return List.copyOf(lines);
    }

    private void splitInto(List<String> lines, String value) {
        for (String line : safe(value).split("\\R")) {
            if (!line.isBlank()) {
                lines.add(line.trim());
            }
        }
    }

    private List<ContactPart> contactParts(Header header) {
        if (header == null) {
            return List.of();
        }
        List<ContactPart> parts = new ArrayList<>();
        addPart(parts, safe(header.getPhoneNumber()), null);
        if (header.getEmail() != null) {
            addPart(parts, emailDisplay(header.getEmail()), emailLink(header.getEmail()));
        }
        addPart(parts, safe(header.getAddress()), null);
        if (header.getLinkedIn() != null) {
            addPart(parts, linkDisplay(header.getLinkedIn()), linkOptions(header.getLinkedIn()));
        }
        if (header.getGitHub() != null) {
            addPart(parts, linkDisplay(header.getGitHub()), linkOptions(header.getGitHub()));
        }
        return List.copyOf(parts);
    }

    private void addPart(List<ContactPart> parts, String text, DocumentLinkOptions linkOptions) {
        if (text != null && !text.isBlank()) {
            parts.add(new ContactPart(text.trim(), linkOptions));
        }
    }

    private DocumentLinkOptions emailLink(EmailYaml email) {
        String to = safe(email.getTo());
        return to.isBlank() ? null : new DocumentLinkOptions("mailto:" + to);
    }

    private DocumentLinkOptions linkOptions(LinkYml link) {
        return link.getLinkUrl() == null || !link.getLinkUrl().isValid()
                ? null
                : new DocumentLinkOptions(link.getLinkUrl().getUrl());
    }

    private String emailDisplay(EmailYaml email) {
        String display = safe(email.getDisplayText());
        return display.isBlank() ? safe(email.getTo()) : display;
    }

    private String linkDisplay(LinkYml link) {
        String display = safe(link.getDisplayText());
        return display.isBlank() && link.getLinkUrl() != null ? safe(link.getLinkUrl().getUrl()) : display;
    }

    private static String stripMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
    }

    private static String spacedUpper(String value) {
        String upper = safe(value).toUpperCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < upper.length(); index++) {
            char current = upper.charAt(index);
            builder.append(current);
            if (Character.isLetterOrDigit(current)
                    && index + 1 < upper.length()
                    && Character.isLetterOrDigit(upper.charAt(index + 1))) {
                builder.append(' ');
            } else if (Character.isWhitespace(current)) {
                builder.append("  ");
            }
        }
        return builder.toString();
    }

    private static String normalize(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < safe(value).length(); index++) {
            char current = Character.toLowerCase(safe(value).charAt(index));
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
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

    private static CvTheme defaultTheme() {
        return new CvTheme(
                new Color(45, 43, 40),
                new Color(126, 93, 52),
                new Color(45, 43, 40),
                new Color(126, 93, 52),
                FontName.PT_SERIF,
                FontName.PT_SERIF,
                27.0,
                9.2,
                9.0,
                2.2,
                Margin.top(3),
                0);
    }

    private record ContactPart(String text, DocumentLinkOptions linkOptions) {
    }

    private record WorkEntry(String heading, String date, String description) {
    }
}
