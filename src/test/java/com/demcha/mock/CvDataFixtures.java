package com.demcha.mock;

import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.List;

/**
 * Canonical v2 {@link CvDocument} fixture for the benchmark suite — a
 * realistic multi-section CV ("Jordan Rivera") used as a stable render
 * workload. Sibling of {@code InvoiceDataFixtures} / {@code ProposalDataFixtures}.
 */
public final class CvDataFixtures {

    private CvDataFixtures() {
    }

    public static CvDocument canonicalCv() {
        CvIdentity identity = CvIdentity.builder()
                .name("Jordan", "Rivera")
                .jobTitle("Backend Java Developer")
                .contact("+44 20 5555 1000", "jordan@demo.dev", "London, UK")
                .link("LinkedIn", "https://linkedin.com/in/jordan-rivera-demo")
                .link("GitHub", "https://github.com/jordan-rivera-demo")
                .build();

        ParagraphSection summary = new ParagraphSection(
                "Professional Summary",
                "Platform engineer building resilient PDF and document-generation "
                        + "workflows for reliable business output.");

        SkillsSection skills = SkillsSection.builder("Technical Skills")
                .leveledGroup("Languages", List.of(
                        CvSkill.of("Java 21", 0.95),
                        CvSkill.of("Kotlin", 0.8),
                        CvSkill.of("SQL", 0.8)))
                .leveledGroup("Document & Print", List.of(
                        CvSkill.of("PDFBox", 0.9),
                        CvSkill.of("Apache POI", 0.7)))
                .leveledGroup("Build & infrastructure", List.of(
                        CvSkill.of("Maven", 0.9),
                        CvSkill.of("GitHub Actions", 0.85),
                        CvSkill.of("Docker", 0.7)))
                .build();

        EntriesSection education = EntriesSection.builder("Education & Certifications")
                .entry("MSc Computer Science", "University of Manchester", "2021",
                        "Distinction. Thesis on composable layout primitives.")
                .entry("Oracle Java Certification", "Professional track", "2023",
                        "Java 17 platform deep-dive: records, sealed types, pattern matching.")
                .build();

        RowsSection projects = RowsSection.builder("Projects", RowStyle.BULLETED_STACKED)
                .row("GraphCompose",
                        "Declarative PDF layout engine for reusable document generation.")
                .row("Template Studio",
                        "Internal tool for evaluating CV, proposal, and invoice output.")
                .build();

        EntriesSection experience = EntriesSection.builder("Professional Experience")
                .entry("Senior Platform Engineer", "Northwind Systems", "2024-Present",
                        "Led reusable document flows for reporting, billing, and hiring.")
                .entry("Software Engineer", "BrightLeaf Labs", "2021-2024",
                        "Built backend services and production document rendering pipelines.")
                .build();

        RowsSection additional = RowsSection.builder("Additional Information", RowStyle.PLAIN)
                .row("Location", "Based in London; available for hybrid or remote collaboration.")
                .row("Interests", "Platform architecture, DX, and document-quality automation.")
                .build();

        return CvDocument.builder()
                .identity(identity)
                .sections(summary, skills, education, projects, experience, additional)
                .build();
    }
}
