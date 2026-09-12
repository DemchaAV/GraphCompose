package com.demcha.examples.support;

import com.demcha.compose.document.image.DocumentImageData;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

/**
 * Sample data for the Charcoal Gold CV example.
 *
 * <p>Sized to the design: the preset draws a fixed one-page sheet, and a CV
 * much longer than this one does not compose at all — the two columns are a
 * single atomic row, so it raises {@code AtomicNodeTooLargeException}.</p>
 *
 * <p>The credential marks are this preset's own vocabulary —
 * {@code certificate}, {@code trophy}, {@code growth}, {@code star} — and
 * each entry names the one it wants. The portrait is a neutral silhouette
 * rather than a photograph, because the example ships in the repository; a
 * real CV passes the candidate's own image the same way.</p>
 */
public final class CharcoalGoldSampleData {

    private static final String NEWLINE = String.valueOf((char) 10);
    private static final String PORTRAIT = "/cv-portrait-placeholder.png";

    private CharcoalGoldSampleData() {
    }

    /**
     * A delivery lead's one-page CV.
     *
     * @return the sample document
     */
    public static CvDocument sample() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("HELENA", "MARSH")
                        .jobTitle("DELIVERY LEAD")
                        .contact(new Contact("+44 161 496 0114",
                                "helena@example.com",
                                "Manchester, United Kingdom"))
                        .link(new Link("helenamarsh.co.uk", "https://helenamarsh.co.uk"))
                        .link(new Link("linkedin.com/in/hmarsh",
                                "https://www.linkedin.com/in/hmarsh"))
                        .portrait(portrait())
                        .build())
                .section(new ParagraphSection("Summary", String.join(NEWLINE,
                        "Delivery lead with nine years running software programmes for"
                                + " regulated clients, from discovery through the audit that"
                                + " follows go-live.",
                        "Happiest with a small team, a short feedback loop, and a plan that"
                                + " survives contact with the first week.")))
                .section(SkillsSection.of("SKILLS", new SkillGroup("Core", List.of(
                        CvSkill.of("Delivery Management", 1.0),
                        CvSkill.of("Stakeholder Management", 1.0),
                        CvSkill.of("Risk & Assurance", 1.0),
                        CvSkill.of("Budget Ownership", 0.8),
                        CvSkill.of("Agile Coaching", 0.8),
                        CvSkill.of("Vendor Management", 0.8),
                        CvSkill.of("Roadmapping", 1.0),
                        CvSkill.of("Jira / Confluence", 1.0),
                        CvSkill.of("Power BI", 0.6),
                        CvSkill.of("Public Speaking", 0.6)))))
                .section(RowsSection.builder("LANGUAGES", RowStyle.PLAIN)
                        .row("English", "Native")
                        .row("Welsh", "Native")
                        .row("Spanish", "B2 – Upper Intermediate")
                        .row("Italian", "A2 – Basic")
                        .build())
                .section(EntriesSection.builder("EDUCATION")
                        .entry(CvEntry.builder("MSc Programme Management")
                                .subtitle("University of Manchester")
                                .date("2015 – 2016")
                                .build())
                        .entry(CvEntry.builder("BA Economics")
                                .subtitle("University of Leeds")
                                .date("2012 – 2015")
                                .build())
                        .build())
                .section(EntriesSection.builder("EXPERIENCE")
                        .entry(CvEntry.builder("Delivery Lead")
                                .subtitle("NORTHWALL GROUP")
                                .place("MANCHESTER, UK")
                                .date("2021 – Present")
                                .link("https://northwall.example.com")
                                .body(List.of(
                                        "Runs a portfolio of four programmes worth £8M against"
                                                + " a fixed regulatory deadline.",
                                        "Rebuilt the intake process so a request is either"
                                                + " scheduled or refused within a week.",
                                        "Coaches three delivery managers and chairs the monthly"
                                                + " assurance review.",
                                        "Cut third-party spend by 18% by consolidating four"
                                                + " vendor contracts into one."))
                                .build())
                        .entry(CvEntry.builder("Senior Project Manager")
                                .subtitle("BRIDGEMOOR CONSULTING")
                                .place("LEEDS, UK")
                                .date("2018 – 2021")
                                .body(List.of(
                                        "Delivered fourteen client programmes across finance"
                                                + " and public sector.",
                                        "Owned budgets, timelines and the escalation path to"
                                                + " the client's board.",
                                        "Introduced the risk register the practice still uses."))
                                .build())
                        .entry(CvEntry.builder("Project Manager")
                                .subtitle("ALDGATE DIGITAL")
                                .place("LONDON, UK")
                                .date("2016 – 2018")
                                .body(List.of(
                                        "Ran delivery for two product teams and the platform"
                                                + " work beneath them.",
                                        "Built the reporting pack the leadership team read"
                                                + " weekly."))
                                .build())
                        .build())
                .section(EntriesSection.builder("CERTIFICATIONS")
                        .entry(CvEntry.builder("PMP® Certification")
                                .subtitle("Project Management Institute")
                                .date("2019")
                                .icon("certificate")
                                .build())
                        .entry(CvEntry.builder("PRINCE2® Practitioner")
                                .subtitle("AXELOS")
                                .date("2018")
                                .icon("certificate")
                                .build())
                        .entry(CvEntry.builder("Certified Scrum Professional")
                                .subtitle("Scrum Alliance")
                                .date("2017")
                                .icon("certificate")
                                .build())
                        .build())
                .section(EntriesSection.builder("ACHIEVEMENTS")
                        .entry(CvEntry.builder("Programme of the Year")
                                .subtitle("Northwall Group")
                                .date("2024")
                                .icon("trophy")
                                .build())
                        .entry(CvEntry.builder("Intake Redesign")
                                .subtitle("Lead time down 62%")
                                .date("2023")
                                .icon("growth")
                                .build())
                        .entry(CvEntry.builder("Client Choice Award")
                                .subtitle("Bridgemoor Consulting")
                                .date("2020")
                                .icon("star")
                                .build())
                        .build())
                .section(new ParagraphSection("TECHNICAL TOOLS", String.join(NEWLINE,
                        "Jira", "Confluence", "Miro", "Power BI", "Smartsheet", "Slack")))
                .build();
    }

    /**
     * The packaged silhouette that stands in for a photograph.
     *
     * @return the portrait image data
     */
    private static DocumentImageData portrait() {
        try (InputStream in = Objects.requireNonNull(
                CharcoalGoldSampleData.class.getResourceAsStream(PORTRAIT),
                "cv-portrait-placeholder.png missing from examples/src/main/resources/")) {
            return DocumentImageData.fromBytes(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the sample portrait", e);
        }
    }
}
