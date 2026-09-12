package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import com.demcha.compose.document.templates.cv.data.Slot;

import java.util.List;

/**
 * The canonical fixture for {@link ProfessionalSidebar} — the document the
 * design was drawn around, and the one both gates measure.
 *
 * <p>One fixture feeds the layout snapshot and the pixel baseline, so a
 * change that moves the sheet moves both together instead of leaving one
 * gate measuring a document the other no longer renders.</p>
 */
public final class ProfessionalSidebarFixtures {

    /** The en dash the design sets between dates. */
    private static final String DASH = "–";

    private ProfessionalSidebarFixtures() {
    }

    /**
     * The canonical CV: five contact channels, ten levelled skills, two
     * degrees, three languages, three roles, two projects and a references
     * note — one page.
     *
     * @return the fixture document
     */
    public static CvDocument canonicalCv() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("YOUR", "NAME")
                        .jobTitle("YOUR PROFESSIONAL TITLE")
                        .contact(new Contact("+1 234 567 8900",
                                "your.email@example.com",
                                "City, State, Country"))
                        .link(new Link("linkedin.com/in/username",
                                "https://linkedin.com/in/username"))
                        .link(new Link("www.yourwebsite.com",
                                "https://www.yourwebsite.com"))
                        .build())
                .section(Slot.MAIN, new ParagraphSection("PROFILE",
                        "Java backend engineer experienced in building secure Spring Boot"
                                + " services, REST APIs and data-driven products. Delivers clean"
                                + " architecture, reliable integrations and well-tested"
                                + " persistence layers with PostgreSQL, JPA and Flyway."
                                + " Comfortable owning features from API design through"
                                + " Docker-based delivery and production support."))
                .section(Slot.SIDEBAR, SkillsSection.of("SKILLS", new SkillGroup("Core", List.of(
                        CvSkill.of("Java", 0.72),
                        CvSkill.of("Spring Boot", 0.7),
                        CvSkill.of("SQL / Databases", 0.7),
                        CvSkill.of("RESTful APIs", 0.6),
                        CvSkill.of("Microservices", 0.72),
                        CvSkill.of("Git / GitHub", 0.72),
                        CvSkill.of("Docker", 0.56),
                        CvSkill.of("Testing (JUnit)", 0.76),
                        CvSkill.of("Problem Solving", 0.72),
                        CvSkill.of("Communication", 0.7)))))
                .section(Slot.SIDEBAR, EntriesSection.builder("EDUCATION")
                        .entry("DEGREE NAME", "University Name", "2016 " + DASH + " 2020", "")
                        .entry("DEGREE NAME", "University Name", "2012 " + DASH + " 2016", "")
                        .build())
                .section(Slot.SIDEBAR, SkillsSection.of("LANGUAGES",
                        new SkillGroup("Spoken", List.of(
                                CvSkill.of("English", 1.0),
                                CvSkill.of("Ukrainian", 0.8),
                                CvSkill.of("Russian", 0.6)))))
                .section(Slot.MAIN, EntriesSection.builder("EXPERIENCE")
                        .entry("JAVA BACKEND DEVELOPER",
                                "Product Studio   |   London, UK",
                                "2023 " + DASH + " Present",
                                String.join("\n",
                                        "Built secure Spring Boot REST APIs for customer"
                                                + " workflows.",
                                        "Added JWT authentication, validation and role-based"
                                                + " access.",
                                        "Tuned JPA queries and PostgreSQL indexes for faster"
                                                + " responses.",
                                        "Added JUnit tests and Docker environments for reliable"
                                                + " releases."))
                        .entry("SOFTWARE DEVELOPER",
                                "Digital Solutions Ltd   |   Manchester, UK",
                                "2021 " + DASH + " 2023",
                                String.join("\n",
                                        "Integrated internal services with third-party REST"
                                                + " APIs.",
                                        "Delivered Flyway migrations and improved data"
                                                + " consistency.",
                                        "Introduced structured logging and production"
                                                + " diagnostics.",
                                        "Worked with frontend engineers on clear API contracts."))
                        .entry("JUNIOR JAVA DEVELOPER",
                                "TechWorks   |   Remote",
                                "2019 " + DASH + " 2021",
                                String.join("\n",
                                        "Developed Java services and reusable backend"
                                                + " components.",
                                        "Fixed defects across REST, persistence and security"
                                                + " layers.",
                                        "Wrote JUnit tests and supported continuous integration.",
                                        "Documented endpoints and operational runbooks."))
                        .build())
                .section(Slot.MAIN, EntriesSection.builder("PROJECTS")
                        .entry("CVREWRITER", "", "2026",
                                "AI-powered resume tailoring platform with Spring Boot, JWT"
                                        + " authentication, PDF generation and application"
                                        + " tracking.")
                        .entry("GRAPHCOMPOSE", "", "2025",
                                "Declarative Java and Kotlin document layout engine with"
                                        + " semantic components, pagination and multi-format"
                                        + " rendering.")
                        .build())
                .section(Slot.MAIN, new ParagraphSection("REFERENCES",
                        "Available upon request."))
                .build();
    }
}
