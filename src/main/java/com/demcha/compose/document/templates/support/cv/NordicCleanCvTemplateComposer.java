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
 * Editorial one-page CV composer with a quiet Nordic palette, a soft summary
 * strip, and a compact evidence column for work and project outcomes.
 *
 * @author Artem Demchyshyn
 */
public final class NordicCleanCvTemplateComposer {
    private static final DocumentColor INK = DocumentColor.rgb(18, 39, 52);
    private static final DocumentColor MUTED = DocumentColor.rgb(82, 104, 116);
    private static final DocumentColor ACCENT = DocumentColor.rgb(28, 128, 135);
    private static final DocumentColor ACCENT_SOFT = DocumentColor.rgb(226, 244, 245);
    private static final DocumentColor RAIL_FILL = DocumentColor.rgb(244, 249, 249);
    private static final DocumentColor RULE = DocumentColor.rgb(188, 219, 222);
    private static final DocumentColor WHITE = DocumentColor.WHITE;

    private final CvTheme theme;

    /**
     * Creates the composer with its default Nordic body theme.
     */
    public NordicCleanCvTemplateComposer() {
        this(defaultTheme());
    }

    /**
     * Creates the composer with caller-supplied CV typography tokens.
     *
     * @param theme CV theme driving the main fonts
     */
    public NordicCleanCvTemplateComposer(CvTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public void compose(DocumentSession document, CvDocumentSpec documentSpec) {
        CvDocumentSpec spec = Objects.requireNonNull(documentSpec, "documentSpec");

        PageFlowBuilder flow = document.dsl()
                .pageFlow()
                .name("NordicCleanRoot")
                .spacing(7);

        addHeader(flow, spec.header());
        addProfile(flow, ProfessionalCvTemplateSupport.findModule(
                spec, "summary", "professional summary", "profile"));
        addBody(flow, spec);

        flow.build();
    }

    private void addHeader(PageFlowBuilder flow, Header header) {
        flow.addRow("NordicCleanHeader", row -> row
                .spacing(14)
                .weights(1.2, 0.8)
                .addSection("NordicCleanIdentity", identity -> identity
                        .spacing(3)
                        .padding(new DocumentInsets(1, 0, 2, 0))
                        .addParagraph(paragraph -> paragraph
                                .text(ProfessionalCvTemplateSupport.safe(header == null ? "" : header.getName())
                                        .toUpperCase(Locale.ROOT))
                                .textStyle(style(theme.headerFont(), 27.0, DocumentTextDecoration.BOLD, INK))
                                .autoSize(27.0, 22.0)
                                .margin(DocumentInsets.zero()))
                        .addShape(shape -> shape
                                .name("NordicCleanNameAccent")
                                .size(64, 2.6)
                                .fillColor(ACCENT)
                                .cornerRadius(1.3)
                                .margin(DocumentInsets.zero()))
                        .addParagraph(paragraph -> paragraph
                                .text("BACKEND JAVA DEVELOPER")
                                .textStyle(style(theme.bodyFont(), 7.7, DocumentTextDecoration.BOLD, MUTED))
                                .margin(DocumentInsets.zero())))
                .addSection("NordicCleanContact", contact -> addContactStack(contact, header, TextAlign.RIGHT)));
    }

    private void addProfile(PageFlowBuilder flow, CvModule module) {
        List<String> lines = ProfessionalCvTemplateSupport.moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        flow.addSection("NordicCleanProfile", section -> section
                .spacing(4)
                .padding(new DocumentInsets(8, 10, 8, 10))
                .fillColor(ACCENT_SOFT)
                .accentLeft(ACCENT, 3.0)
                .cornerRadius(4)
                .addParagraph(paragraph -> paragraph
                        .text("PROFILE")
                        .textStyle(labelStyle(8.0, ACCENT))
                        .margin(DocumentInsets.zero()))
                .addParagraph(paragraph -> paragraph
                        .text(lines.getFirst())
                        .textStyle(bodyStyle(7.85, INK))
                        .lineSpacing(1.25)
                        .margin(DocumentInsets.zero())));
    }

    private void addBody(PageFlowBuilder flow, CvDocumentSpec spec) {
        flow.addRow("NordicCleanBody", row -> row
                .spacing(15)
                .weights(0.72, 1.28)
                .addSection("NordicCleanRail", rail -> {
                    rail.spacing(8)
                            .padding(new DocumentInsets(9, 10, 9, 10))
                            .fillColor(RAIL_FILL)
                            .stroke(DocumentStroke.of(RULE, 0.35))
                            .cornerRadius(4);
                    addSkills(rail, ProfessionalCvTemplateSupport.findModule(spec, "technical skills", "skills"));
                    addEducation(rail, ProfessionalCvTemplateSupport.findModule(spec, "education", "certifications"));
                    addSimpleLines(rail, "Additional", ProfessionalCvTemplateSupport.findModule(
                            spec, "additional information", "additional"), 2);
                })
                .addSection("NordicCleanMain", main -> {
                    main.spacing(9);
                    addExperience(main, ProfessionalCvTemplateSupport.findModule(spec, "experience", "employment"));
                    addProjects(main, ProfessionalCvTemplateSupport.findModule(spec, "projects"));
                }));
    }

    private void addSkills(SectionBuilder parent, CvModule module) {
        List<String> lines = ProfessionalCvTemplateSupport.moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("NordicCleanSkills", section -> {
            addHeading(section, "Skills", 82);
            for (String line : lines.stream().limit(7).toList()) {
                addLabelValueLine(section, line, 7.15, 1.05);
            }
        });
    }

    private void addEducation(SectionBuilder parent, CvModule module) {
        List<String> lines = ProfessionalCvTemplateSupport.moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("NordicCleanEducation", section -> {
            addHeading(section, "Education", 82);
            for (String line : lines.stream().limit(4).toList()) {
                WorkEntry entry = ProfessionalCvTemplateSupport.parseWorkEntry(line);
                section.addParagraph(paragraph -> paragraph
                        .textStyle(bodyStyle(7.05, INK))
                        .lineSpacing(1.05)
                        .margin(DocumentInsets.bottom(2))
                        .rich(rich -> {
                            rich.style(entry.title(), style(theme.bodyFont(), 7.05, DocumentTextDecoration.BOLD, INK));
                            if (!entry.subtitle().isBlank()) {
                                rich.style(" / " + entry.subtitle(), bodyStyle(7.05, MUTED));
                            }
                            if (!entry.date().isBlank()) {
                                rich.style(" / " + entry.date(), bodyStyle(6.85, MUTED));
                            }
                        }));
            }
        });
    }

    private void addSimpleLines(SectionBuilder parent, String title, CvModule module, int limit) {
        List<String> lines = ProfessionalCvTemplateSupport.moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("NordicClean" + ProfessionalCvTemplateSupport.normalize(title), section -> {
            addHeading(section, title, 82);
            for (String line : lines.stream().limit(limit).toList()) {
                addLabelValueLine(section, line, 7.1, 1.05);
            }
        });
    }

    private void addExperience(SectionBuilder parent, CvModule module) {
        List<String> lines = ProfessionalCvTemplateSupport.moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("NordicCleanExperience", section -> {
            addHeading(section, "Experience", 130);
            for (String line : lines.stream().limit(2).toList()) {
                WorkEntry entry = ProfessionalCvTemplateSupport.parseWorkEntry(line);
                addWorkEntry(section, entry);
            }
        });
    }

    private void addProjects(SectionBuilder parent, CvModule module) {
        List<String> lines = ProfessionalCvTemplateSupport.moduleLines(module);
        if (lines.isEmpty()) {
            return;
        }
        parent.addSection("NordicCleanProjects", section -> {
            addHeading(section, "Selected Projects", 130);
            for (String line : lines.stream().limit(4).toList()) {
                ProjectEntry project = ProfessionalCvTemplateSupport.parseProjectEntry(line);
                section.addParagraph(paragraph -> paragraph
                        .textStyle(bodyStyle(7.25, INK))
                        .lineSpacing(1.08)
                        .margin(DocumentInsets.bottom(3))
                        .rich(rich -> {
            rich.style(project.title(), style(theme.bodyFont(), 7.35, DocumentTextDecoration.BOLD, INK));
            if (!project.context().isBlank()) {
                rich.style(" " + project.context(), bodyStyle(6.95, MUTED));
            }
            rich.style(" - " + project.description(), bodyStyle(7.2, INK));
                        }));
            }
        });
    }

    private void addWorkEntry(SectionBuilder section, WorkEntry entry) {
        section.addParagraph(paragraph -> paragraph
                .textStyle(bodyStyle(8.0, INK))
                .margin(DocumentInsets.zero())
                .rich(rich -> {
                    rich.style(entry.title(), style(theme.bodyFont(), 8.0, DocumentTextDecoration.BOLD, INK));
                    if (!entry.date().isBlank()) {
                        rich.style(" / " + entry.date(),
                                style(theme.bodyFont(), 7.35, DocumentTextDecoration.BOLD, ACCENT));
                    }
                }));
        if (!entry.subtitle().isBlank()) {
            section.addParagraph(paragraph -> paragraph
                    .text(entry.subtitle())
                    .textStyle(bodyStyle(7.2, MUTED))
                    .margin(DocumentInsets.zero()));
        }
        if (!entry.description().isBlank()) {
            section.addParagraph(paragraph -> paragraph
                    .text(entry.description())
                    .textStyle(bodyStyle(7.45, INK))
                    .lineSpacing(1.12)
                    .margin(DocumentInsets.bottom(5)));
        }
    }

    private void addHeading(SectionBuilder section, String title, double ruleWidth) {
        section.spacing(3)
                .addParagraph(paragraph -> paragraph
                        .text(title.toUpperCase(Locale.ROOT))
                        .textStyle(labelStyle(7.6, ACCENT))
                        .margin(DocumentInsets.zero()))
                .addLine(line -> line
                        .horizontal(ruleWidth)
                        .color(ACCENT)
                        .thickness(1.1)
                        .margin(DocumentInsets.bottom(2)));
    }

    private void addLabelValueLine(SectionBuilder section, String line, double size, double lineSpacing) {
        String clean = ProfessionalCvTemplateSupport.stripMarkdown(line);
        int colon = clean.indexOf(':');
        section.addParagraph(paragraph -> paragraph
                .textStyle(bodyStyle(size, INK))
                .lineSpacing(lineSpacing)
                .margin(DocumentInsets.bottom(1.5))
                .rich(rich -> {
                    if (colon > 0) {
                        rich.style(clean.substring(0, colon + 1), style(theme.bodyFont(), size, DocumentTextDecoration.BOLD, INK));
                        rich.style(" " + clean.substring(colon + 1).trim(), bodyStyle(size, MUTED));
                    } else {
                        rich.style(clean, bodyStyle(size, INK));
                    }
                }));
    }

    private void addContactStack(SectionBuilder section, Header header, TextAlign align) {
        section.spacing(2)
                .padding(new DocumentInsets(3, 0, 0, 0));
        DocumentTextStyle meta = bodyStyle(7.4, MUTED);
        DocumentTextStyle link = style(theme.bodyFont(), 7.4, DocumentTextDecoration.UNDERLINE, ACCENT);
        for (ContactPart part : ProfessionalCvTemplateSupport.contactParts(header)) {
            section.addParagraph(paragraph -> paragraph
                    .textStyle(part.linkOptions() == null ? meta : link)
                    .align(align)
                    .link(part.linkOptions())
                    .margin(DocumentInsets.zero())
                    .text(part.text()));
        }
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
                new Color(18, 39, 52),
                new Color(28, 128, 135),
                new Color(82, 104, 116),
                new Color(28, 128, 135),
                FontName.BARLOW,
                FontName.LATO,
                27,
                8.0,
                7.6,
                3,
                Margin.top(2),
                0);
    }
}
