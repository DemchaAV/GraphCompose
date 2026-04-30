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
 * Compact technical CV variant with a dark command-bar header, a dense skill
 * rail, and a larger evidence column for experience and projects.
 */
public final class CompactMonoCvTemplateComposer {
    private static final DocumentColor INK = DocumentColor.rgb(28, 34, 42);
    private static final DocumentColor MUTED = DocumentColor.rgb(84, 96, 112);
    private static final DocumentColor PAPER = DocumentColor.rgb(248, 250, 252);
    private static final DocumentColor RAIL = DocumentColor.rgb(236, 244, 242);
    private static final DocumentColor RULE = DocumentColor.rgb(188, 204, 215);
    private static final DocumentColor ACCENT = DocumentColor.rgb(0, 126, 151);
    private static final DocumentColor HEADER = DocumentColor.rgb(18, 24, 32);
    private static final DocumentColor HEADER_SOFT = DocumentColor.rgb(192, 207, 219);

    private final CvTheme theme;

    public CompactMonoCvTemplateComposer() {
        this(defaultTheme());
    }

    public CompactMonoCvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");
        PageFlowBuilder flow = document.dsl()
                .pageFlow()
                .name("CompactMonoRoot")
                .spacing(9)
                .fillColor(PAPER);

        addHeader(flow, spec.header(), document.canvas().innerWidth());
        flow.addRow("CompactMonoBody", row -> row
                .spacing(14)
                .weights(0.78, 1.62)
                .addSection("CompactMonoRail", rail -> {
                    rail.spacing(8)
                            .padding(new DocumentInsets(11, 11, 13, 11))
                            .fillColor(RAIL)
                            .accentLeft(ACCENT, 3.0);
                    addRailModule(rail, "Skills", findModule(spec, "technical skills", "skills"), true, 7);
                    addRailModule(rail, "Education", findModule(spec, "education", "certifications"), false, 3);
                    addRailModule(rail, "Additional", findModule(spec, "additional information", "additional"), false, 4);
                })
                .addSection("CompactMonoMain", main -> {
                    main.spacing(8);
                    addMainModule(main, "Profile", findModule(spec, "summary", "professional summary", "profile"), 1);
                    addMainModule(main, "Experience", findModule(spec, "experience", "employment"), 2);
                    addMainModule(main, "Selected Projects", findModule(spec, "projects"), 4);
                }));

        flow.build();
    }

    private void addHeader(PageFlowBuilder flow, Header header, double width) {
        flow.addSection("CompactMonoHeader", section -> {
            section.spacing(4)
                    .padding(new DocumentInsets(13, 16, 14, 16))
                    .fillColor(HEADER)
                    .cornerRadius(3)
                    .addParagraph(paragraph -> paragraph
                            .text(safe(header == null ? "" : header.getName()).toUpperCase(Locale.ROOT))
                            .textStyle(style(theme.headerFont(), 23.5, DocumentTextDecoration.BOLD, DocumentColor.WHITE))
                            .align(TextAlign.LEFT)
                            .margin(DocumentInsets.zero()));
            addContact(section, header);
            section.addLine(line -> line
                    .name("CompactMonoHeaderWidthRule")
                    .horizontal(Math.max(0, width - 32))
                    .color(HEADER)
                    .thickness(0.1)
                    .margin(DocumentInsets.zero()));
        });
    }

    private void addContact(SectionBuilder section, Header header) {
        List<ContactPart> parts = contactParts(header);
        if (parts.isEmpty()) {
            return;
        }
        DocumentTextStyle meta = style(theme.bodyFont(), 8.3, DocumentTextDecoration.DEFAULT, HEADER_SOFT);
        DocumentTextStyle link = style(theme.bodyFont(), 8.3, DocumentTextDecoration.UNDERLINE, DocumentColor.rgb(108, 213, 222));
        DocumentTextStyle separator = style(theme.bodyFont(), 8.3, DocumentTextDecoration.DEFAULT, DocumentColor.rgb(102, 117, 132));

        section.addParagraph(paragraph -> paragraph
                .textStyle(meta)
                .align(TextAlign.LEFT)
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
                            rich.style("  /  ", separator);
                        }
                    }
                }));
    }

    private void addRailModule(SectionBuilder parent, String title, CvModule module, boolean skillMode, int limit) {
        List<String> lines = moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("CompactMono" + normalize(title), section -> {
            section.spacing(4)
                    .padding(new DocumentInsets(0, 0, 2, 0));
            addModuleLabel(section, title, 8.0);
            List<String> display = lines.stream()
                    .limit(limit)
                    .map(skillMode ? CompactMonoCvTemplateComposer::compactSkillLine : CompactMonoCvTemplateComposer::stripMarkdown)
                    .filter(line -> !line.isBlank())
                    .toList();
            section.addList(list -> list
                    .items(display)
                    .noMarker()
                    .continuationIndent("  ")
                    .textStyle(style(theme.bodyFont(), 7.65, DocumentTextDecoration.DEFAULT, INK))
                    .lineSpacing(0.95)
                    .itemSpacing(2.2)
                    .margin(DocumentInsets.zero()));
        });
    }

    private void addMainModule(SectionBuilder parent, String title, CvModule module, int limit) {
        List<String> lines = moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("CompactMono" + normalize(title), section -> {
            section.spacing(4)
                    .padding(new DocumentInsets(9, 10, 10, 11))
                    .fillColor(DocumentColor.WHITE)
                    .stroke(DocumentStroke.of(RULE, 0.35))
                    .cornerRadius(3);
            addModuleLabel(section, title, 8.4);
            for (String line : lines.stream().limit(limit).toList()) {
                String text = stripMarkdown(line);
                section.addParagraph(paragraph -> paragraph
                        .text(text)
                        .textStyle(style(theme.bodyFont(), 8.45, DocumentTextDecoration.DEFAULT, INK))
                        .lineSpacing(1.25)
                        .align(TextAlign.LEFT)
                        .margin(DocumentInsets.zero()));
            }
        });
    }

    private void addModuleLabel(SectionBuilder section, String title, double size) {
        section.addShape(shape -> shape
                .name("CompactMonoLabelTick")
                .size(22, 2.2)
                .fillColor(ACCENT)
                .cornerRadius(1.1)
                .margin(DocumentInsets.zero()));
        section.addParagraph(paragraph -> paragraph
                .text(title.toUpperCase(Locale.ROOT))
                .textStyle(style(theme.headerFont(), size, DocumentTextDecoration.BOLD, ACCENT))
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.zero()));
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
                    // Compact variant renders the plain CV narrative only.
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
        addPart(parts, safe(header.getAddress()), null);
        addPart(parts, safe(header.getPhoneNumber()), null);
        if (header.getEmail() != null) {
            addPart(parts, emailDisplay(header.getEmail()), emailLink(header.getEmail()));
        }
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

    private static String compactSkillLine(String line) {
        String clean = stripMarkdown(line);
        int colon = clean.indexOf(':');
        if (colon < 0) {
            return clean;
        }
        String label = clean.substring(0, colon).trim();
        String[] tokens = clean.substring(colon + 1).split(",");
        List<String> picked = new ArrayList<>();
        for (String token : tokens) {
            String value = token.trim();
            if (!value.isBlank()) {
                picked.add(value);
            }
            if (picked.size() == 4) {
                break;
            }
        }
        return label + " :: " + String.join(", ", picked);
    }

    private static String stripMarkdown(String value) {
        return safe(value)
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replace("*", "")
                .replace("_", "");
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
                new Color(28, 34, 42),
                new Color(0, 126, 151),
                new Color(28, 34, 42),
                new Color(0, 126, 151),
                FontName.IBM_PLEX_MONO,
                FontName.LATO,
                23.5,
                8.6,
                8.4,
                2.0,
                Margin.top(2),
                0);
    }

    private record ContactPart(String text, DocumentLinkOptions linkOptions) {
    }
}
