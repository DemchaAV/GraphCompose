package com.demcha.compose.document.templates.support.cv;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.document.dsl.PageFlowBuilder;
import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentCornerRadius;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentStroke;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.data.cv.CvModule;
import com.demcha.compose.document.templates.support.cv.ProfessionalCvTemplateSupport.ContactPart;
import com.demcha.compose.document.templates.support.cv.ProfessionalCvTemplateSupport.ProjectEntry;
import com.demcha.compose.document.templates.support.cv.ProfessionalCvTemplateSupport.WorkEntry;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.engine.components.style.Margin;
import com.demcha.compose.font.FontName;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Senior engineering CV composer with a full-width command header, dark skill
 * rail, and compact achievement cards for technical leadership evidence.
 *
 * @author Artem Demchyshyn
 */
public final class TechLeadCvTemplateComposer {
    private static final DocumentColor NAVY = DocumentColor.rgb(13, 32, 47);
    private static final DocumentColor NAVY_SOFT = DocumentColor.rgb(35, 56, 72);
    private static final DocumentColor INK = DocumentColor.rgb(32, 42, 55);
    private static final DocumentColor MUTED = DocumentColor.rgb(91, 105, 119);
    private static final DocumentColor GREEN = DocumentColor.rgb(27, 145, 104);
    private static final DocumentColor GREEN_SOFT = DocumentColor.rgb(232, 246, 239);
    private static final DocumentColor RULE = DocumentColor.rgb(190, 212, 204);
    private static final DocumentColor WHITE = DocumentColor.WHITE;

    private final CvTheme theme;

    /**
     * Creates the composer with its default technical leadership theme.
     */
    public TechLeadCvTemplateComposer() {
        this(defaultTheme());
    }

    /**
     * Creates the composer with caller-supplied CV typography tokens.
     *
     * @param theme CV theme driving the main fonts
     */
    public TechLeadCvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");

        PageFlowBuilder flow = document.dsl()
                .pageFlow()
                .name("TechLeadRoot")
                .spacing(8);

        addHeader(flow, spec.header());
        addBody(flow, spec);

        flow.build();
    }

    private void addHeader(PageFlowBuilder flow, Header header) {
        flow.addSection("TechLeadHeader", section -> section
                .spacing(5)
                .padding(new DocumentInsets(13, 15, 13, 15))
                .fillColor(NAVY)
                .cornerRadius(DocumentCornerRadius.top(4))
                .accentBottom(GREEN, 2.5)
                .addRow("TechLeadHeaderRow", row -> row
                        .spacing(12)
                        .weights(1.15, 0.85)
                        .addSection("TechLeadIdentity", identity -> identity
                                .padding(DocumentInsets.zero())
                                .spacing(3)
                                .addParagraph(paragraph -> paragraph
                                        .text(ProfessionalCvTemplateSupport.safe(header == null ? "" : header.getName())
                                                .toUpperCase(Locale.ROOT))
                                        .textStyle(style(theme.headerFont(), 24.5, DocumentTextDecoration.BOLD, WHITE))
                                        .autoSize(24.5, 19.0)
                                        .margin(DocumentInsets.zero()))
                                .addParagraph(paragraph -> paragraph
                                        .text("SECURE BACKEND SYSTEMS / DELIVERY LEADERSHIP")
                                        .textStyle(style(theme.bodyFont(), 7.6, DocumentTextDecoration.BOLD,
                                                DocumentColor.rgb(190, 209, 219)))
                                        .margin(DocumentInsets.zero())))
                        .addSection("TechLeadContact", contact -> addContactStack(contact, header))));
    }

    private void addBody(PageFlowBuilder flow, CvDocumentSpec spec) {
        flow.addRow("TechLeadBody", row -> row
                .spacing(14)
                .weights(0.76, 1.64)
                .addSection("TechLeadRail", rail -> {
                    rail.spacing(8)
                            .padding(new DocumentInsets(10, 10, 11, 10))
                            .fillColor(NAVY)
                            .cornerRadius(DocumentCornerRadius.bottom(4))
                            .accentTop(GREEN, 2.0);
                    addSkills(rail, ProfessionalCvTemplateSupport.findModule(spec, "technical skills", "skills"));
                    addEducation(rail, ProfessionalCvTemplateSupport.findModule(spec, "education", "certifications"));
                    addAdditional(rail, ProfessionalCvTemplateSupport.findModule(spec, "additional information", "additional"));
                })
                .addSection("TechLeadMain", main -> {
                    main.spacing(8);
                    addProfile(main, ProfessionalCvTemplateSupport.findModule(
                            spec, "summary", "professional summary", "profile"));
                    addExperience(main, ProfessionalCvTemplateSupport.findModule(spec, "experience", "employment"));
                    addProjects(main, ProfessionalCvTemplateSupport.findModule(spec, "projects"));
                }));
    }

    private void addProfile(SectionBuilder parent, CvModule module) {
        List<String> lines = ProfessionalCvTemplateSupport.moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("TechLeadProfile", section -> section
                .spacing(4)
                .padding(new DocumentInsets(8, 10, 8, 10))
                .fillColor(GREEN_SOFT)
                .accentLeft(GREEN, 3.0)
                .cornerRadius(DocumentCornerRadius.right(4))
                .addParagraph(paragraph -> paragraph
                        .text("ENGINEERING PROFILE")
                        .textStyle(labelStyle(8.0, GREEN))
                        .margin(DocumentInsets.zero()))
                .addParagraph(paragraph -> paragraph
                        .text(lines.getFirst())
                        .textStyle(bodyStyle(7.75, INK))
                        .lineSpacing(1.2)
                        .margin(DocumentInsets.zero())));
    }

    private void addSkills(SectionBuilder parent, CvModule module) {
        List<String> lines = ProfessionalCvTemplateSupport.moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("TechLeadSkills", section -> {
            addRailHeading(section, "Core Stack");
            for (String line : lines.stream().limit(7).toList()) {
                String clean = ProfessionalCvTemplateSupport.stripMarkdown(line);
                int colon = clean.indexOf(':');
                section.addParagraph(paragraph -> paragraph
                        .textStyle(style(theme.bodyFont(), 6.95, DocumentTextDecoration.DEFAULT, WHITE))
                        .lineSpacing(1.0)
                        .margin(DocumentInsets.bottom(1.8))
                        .rich(rich -> {
                            if (colon > 0) {
                                rich.style(clean.substring(0, colon + 1),
                                        style(theme.bodyFont(), 6.95, DocumentTextDecoration.BOLD, GREEN));
                                rich.style(" " + compactValues(clean.substring(colon + 1), 5),
                                        style(theme.bodyFont(), 6.9, DocumentTextDecoration.DEFAULT,
                                                DocumentColor.rgb(220, 231, 236)));
                            } else {
                                rich.style(clean, style(theme.bodyFont(), 6.9, DocumentTextDecoration.DEFAULT, WHITE));
                            }
                        }));
            }
        });
    }

    private void addEducation(SectionBuilder parent, CvModule module) {
        List<String> lines = ProfessionalCvTemplateSupport.moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("TechLeadEducation", section -> {
            addRailHeading(section, "Learning");
            for (String line : lines.stream().limit(4).toList()) {
                WorkEntry entry = ProfessionalCvTemplateSupport.parseWorkEntry(line);
                section.addParagraph(paragraph -> paragraph
                        .textStyle(style(theme.bodyFont(), 6.95, DocumentTextDecoration.DEFAULT, WHITE))
                        .lineSpacing(1.0)
                        .margin(DocumentInsets.bottom(2.3))
                        .rich(rich -> {
                            rich.style(entry.title(), style(theme.bodyFont(), 6.95, DocumentTextDecoration.BOLD, WHITE));
                            if (!entry.date().isBlank()) {
                                rich.style(" / " + entry.date(), style(theme.bodyFont(), 6.7,
                                        DocumentTextDecoration.DEFAULT, DocumentColor.rgb(182, 201, 210)));
                            }
                        }));
            }
        });
    }

    private void addAdditional(SectionBuilder parent, CvModule module) {
        List<String> lines = ProfessionalCvTemplateSupport.moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("TechLeadAdditional", section -> {
            addRailHeading(section, "Details");
            for (String line : lines.stream().limit(2).toList()) {
                section.addParagraph(paragraph -> paragraph
                        .text(ProfessionalCvTemplateSupport.stripMarkdown(line))
                        .textStyle(style(theme.bodyFont(), 6.95, DocumentTextDecoration.DEFAULT,
                                DocumentColor.rgb(220, 231, 236)))
                        .lineSpacing(1.0)
                        .margin(DocumentInsets.bottom(1.8)));
            }
        });
    }

    private void addExperience(SectionBuilder parent, CvModule module) {
        List<String> lines = ProfessionalCvTemplateSupport.moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("TechLeadExperience", section -> {
            addMainHeading(section, "Leadership Experience");
            for (String line : lines.stream().limit(2).toList()) {
                WorkEntry entry = ProfessionalCvTemplateSupport.parseWorkEntry(line);
                section.addSection("TechLeadRoleCard", card -> {
                    card.spacing(3)
                            .padding(new DocumentInsets(6, 8, 6, 8))
                            .fillColor(WHITE)
                            .stroke(DocumentStroke.of(RULE, 0.35))
                            .cornerRadius(DocumentCornerRadius.right(3))
                            .accentLeft(GREEN, 2.0);
                    addRoleHeader(card, entry);
                    if (!entry.subtitle().isBlank()) {
                        card.addParagraph(paragraph -> paragraph
                                .text(entry.subtitle())
                                .textStyle(bodyStyle(7.0, MUTED))
                                .margin(DocumentInsets.zero()));
                    }
                    if (!entry.description().isBlank()) {
                        card.addParagraph(paragraph -> paragraph
                                .text(entry.description())
                                .textStyle(bodyStyle(7.25, INK))
                                .lineSpacing(1.08)
                                .margin(DocumentInsets.zero()));
                    }
                });
            }
        });
    }

    private void addProjects(SectionBuilder parent, CvModule module) {
        List<String> lines = ProfessionalCvTemplateSupport.moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("TechLeadProjects", section -> {
            addMainHeading(section, "Technical Evidence");
            for (String line : lines.stream().limit(4).toList()) {
                ProjectEntry project = ProfessionalCvTemplateSupport.parseProjectEntry(line);
                section.addSection("TechLeadProjectCard", card -> card
                        .spacing(3)
                        .padding(new DocumentInsets(5, 8, 5, 8))
                        .fillColor(WHITE)
                        .stroke(DocumentStroke.of(RULE, 0.3))
                        .cornerRadius(3)
                        .addParagraph(paragraph -> paragraph
                                .textStyle(bodyStyle(7.2, INK))
                                .lineSpacing(1.06)
                                .margin(DocumentInsets.zero())
                                .rich(rich -> {
                                    rich.style(project.title(),
                                            style(theme.bodyFont(), 7.35, DocumentTextDecoration.BOLD, INK));
                                    if (!project.context().isBlank()) {
                                        rich.style(" " + project.context(), bodyStyle(6.85, GREEN));
                                    }
                                    rich.style(" - " + project.description(), bodyStyle(7.1, INK));
                                })));
            }
        });
    }

    private void addRoleHeader(SectionBuilder card, WorkEntry entry) {
        card.addParagraph(paragraph -> paragraph
                .textStyle(bodyStyle(8.0, INK))
                .margin(DocumentInsets.zero())
                .rich(rich -> {
                    rich.style(entry.title(), style(theme.bodyFont(), 8.0, DocumentTextDecoration.BOLD, INK));
                    if (!entry.date().isBlank()) {
                        rich.style(" / " + entry.date(),
                                style(theme.bodyFont(), 7.1, DocumentTextDecoration.BOLD, GREEN));
                    }
                }));
    }

    private void addContactStack(SectionBuilder section, Header header) {
        section.spacing(2)
                .padding(DocumentInsets.zero());
        DocumentTextStyle meta = style(theme.bodyFont(), 7.2, DocumentTextDecoration.DEFAULT,
                DocumentColor.rgb(196, 211, 220));
        DocumentTextStyle link = style(theme.bodyFont(), 7.2, DocumentTextDecoration.UNDERLINE,
                DocumentColor.rgb(78, 207, 161));
        for (ContactPart part : ProfessionalCvTemplateSupport.contactParts(header)) {
            section.addParagraph(paragraph -> paragraph
                    .text(part.text())
                    .textStyle(part.linkOptions() == null ? meta : link)
                    .link(part.linkOptions())
                    .align(TextAlign.RIGHT)
                    .margin(DocumentInsets.zero()));
        }
    }

    private void addRailHeading(SectionBuilder section, String title) {
        section.spacing(3)
                .addParagraph(paragraph -> paragraph
                        .text(title.toUpperCase(Locale.ROOT))
                        .textStyle(style(theme.headerFont(), 7.4, DocumentTextDecoration.BOLD, GREEN))
                        .margin(DocumentInsets.zero()))
                .addLine(line -> line
                        .horizontal(82)
                        .color(NAVY_SOFT)
                        .thickness(0.8)
                        .margin(DocumentInsets.bottom(2)));
    }

    private void addMainHeading(SectionBuilder section, String title) {
        section.spacing(5)
                .addParagraph(paragraph -> paragraph
                        .text(title.toUpperCase(Locale.ROOT))
                        .textStyle(labelStyle(7.8, GREEN))
                        .margin(DocumentInsets.zero()))
                .addLine(line -> line
                        .horizontal(176)
                        .color(GREEN)
                        .thickness(1.0)
                        .margin(DocumentInsets.bottom(1)));
    }

    private String compactValues(String value, int maxItems) {
        String[] tokens = ProfessionalCvTemplateSupport.stripMarkdown(value).split(",");
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String token : tokens) {
            String clean = token.trim();
            if (clean.isBlank()) {
                continue;
            }
            if (count > 0) {
                builder.append(", ");
            }
            builder.append(clean);
            count++;
            if (count == maxItems) {
                break;
            }
        }
        return builder.toString();
    }

    private DocumentTextStyle labelStyle(double size, DocumentColor color) {
        return style(theme.headerFont(), size, DocumentTextDecoration.BOLD, color);
    }

    private DocumentTextStyle bodyStyle(double size, DocumentColor color) {
        return style(theme.bodyFont(), size, DocumentTextDecoration.DEFAULT, color);
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
                new Color(13, 32, 47),
                new Color(27, 145, 104),
                new Color(32, 42, 55),
                new Color(27, 145, 104),
                FontName.BARLOW,
                FontName.LATO,
                24.5,
                8.0,
                7.6,
                2.5,
                Margin.top(2),
                0);
    }
}
