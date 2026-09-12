package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.List;

/**
 * The canonical fixture for {@link CharcoalGold} — the document the design
 * was drawn around, and the one both gates measure.
 *
 * <p>One fixture feeds the layout snapshot and the pixel baseline, so a
 * change that moves the sheet moves both together instead of leaving one
 * gate measuring a document the other no longer renders.</p>
 */
public final class CharcoalGoldFixtures {

    private static final String NEWLINE = String.valueOf((char) 10);

    private CharcoalGoldFixtures() {
    }

    /**
     * The canonical CV: a photograph, five contact channels, ten rated
     * skills, four languages, two degrees, three roles, three certifications,
     * three achievements and a six-tool strip — one page.
     *
     * @return the fixture document
     */
    public static CvDocument canonicalCv() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("ANASTASIA", "SMITH")
                        .jobTitle("PROJECT MANAGER")
                        .contact(new Contact("+44 7700 900123",
                                "hello@anastasiasmith.com",
                                "London, United Kingdom"))
                        .link(new Link("anastasiasmith.com",
                                "https://anastasiasmith.com"))
                        .link(new Link("linkedin.com/in/anastasiasmith",
                                "https://www.linkedin.com/in/anastasiasmith"))
                        .portrait(CvFixturePortrait.silhouette())
                        .build())
                .section(new ParagraphSection("Summary", String.join(NEWLINE,
                        "Results-driven Project Manager with 6+ years of experience leading cross-functional teams and delivering complex projects on time and within budget.",
                        "Skilled in agile and waterfall methodologies, process optimisation, and stakeholder management. Passionate about driving efficiency and creating value."
                )))
                .section(SkillsSection.of("SKILLS", new SkillGroup("Core", List.of(
                        CvSkill.of("Project Management", 1.0),
                        CvSkill.of("Budgeting & Forecasting", 1.0),
                        CvSkill.of("Stakeholder Communication", 1.0),
                        CvSkill.of("Risk Management", 1.0),
                        CvSkill.of("Data Analysis", 0.8),
                        CvSkill.of("Process Improvement", 0.8),
                        CvSkill.of("Microsoft Excel", 1.0),
                        CvSkill.of("Power BI", 0.8),
                        CvSkill.of("Jira / Confluence", 1.0),
                        CvSkill.of("Agile Methodologies", 0.8)
                ))))
                .section(RowsSection.builder("LANGUAGES", RowStyle.PLAIN)
                        .row("English", "Native")
                        .row("Russian", "Native")
                        .row("French", "B2 – Upper Intermediate")
                        .row("German", "A2 – Basic")
                        .build())
                .section(EntriesSection.builder("EDUCATION")
                        .entry(CvEntry.builder("MSc Project Management")
                                .subtitle("University College London").date("2018 – 2019").build())
                        .entry(CvEntry.builder("BSc Business Administration")
                                .subtitle("King's College London").date("2014 – 2017").build())
                        .build())
                .section(EntriesSection.builder("EXPERIENCE")
                        .entry(CvEntry.builder("Senior Project Manager")
                                .subtitle("DIGITAL SOLUTIONS LTD")
                                .place("LONDON, UK")
                                .date("2021 – Present")
                                .body(List.of(
                                        "Lead end-to-end delivery of enterprise software projects worth £1M–£3M.",
                                        "Manage cross-functional teams of up to 15 members.",
                                        "Implement agile practices that improved delivery speed by 25%.",
                                        "Build strong client relationships and ensure stakeholder alignment."
                                ))
                                .build())
                        .entry(CvEntry.builder("Project Manager")
                                .subtitle("INNOVATECH SYSTEMS")
                                .place("LONDON, UK")
                                .date("2019 – 2021")
                                .body(List.of(
                                        "Delivered 12+ projects across IT and business transformation.",
                                        "Monitored budgets, timelines, and resource allocation.",
                                        "Identified risks and implemented mitigation strategies.",
                                        "Achieved 98% on-time project delivery."
                                ))
                                .build())
                        .entry(CvEntry.builder("Project Coordinator")
                                .subtitle("GLOBAL BUSINESS SERVICES")
                                .place("LONDON, UK")
                                .date("2017 – 2019")
                                .body(List.of(
                                        "Supported project managers in planning and execution.",
                                        "Prepared reports, dashboards, and documentation.",
                                        "Coordinated meetings and tracked action items.",
                                        "Improved internal processes and project templates."
                                ))
                                .build())
                        .build())
                .section(EntriesSection.builder("CERTIFICATIONS")
                        .entry(CvEntry.builder("PMP® Certification")
                                .subtitle("Project Management Institute").date("2020").icon("certificate").build())
                        .entry(CvEntry.builder("PRINCE2® Foundation")
                                .subtitle("AXELOS").date("2019").icon("certificate").build())
                        .entry(CvEntry.builder("AgilePM® Foundation")
                                .subtitle("APMG International").date("2019").icon("certificate").build())
                        .build())
                .section(EntriesSection.builder("ACHIEVEMENTS")
                        .entry(CvEntry.builder("Excellence in Delivery Award")
                                .subtitle("Digital Solutions Ltd").date("2023").icon("trophy").build())
                        .entry(CvEntry.builder("Process Improvement Initiative")
                                .subtitle("Increased efficiency by 30%").date("2022").icon("growth").build())
                        .entry(CvEntry.builder("Top Performer")
                                .subtitle("Innovatech Systems").date("2020").icon("star").build())
                        .build())
                .section(new ParagraphSection("TECHNICAL TOOLS", String.join(NEWLINE,
                        "Microsoft 365",
                        "Jira",
                        "Confluence",
                        "Asana",
                        "Slack",
                        "Power BI"
                )))
                .build();
    }
}
