package com.demcha.examples.support;

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
import com.demcha.compose.document.templates.cv.data.SkillsSection;

import java.util.List;

/**
 * Sample data for the Serif Headline CV example.
 *
 * <p>Sized to the design: the preset draws a fixed one-page sheet, and a CV
 * much longer than this one does not compose at all — the body is a single
 * atomic row, so it raises {@code AtomicNodeTooLargeException}.</p>
 *
 * <p>The project and achievement marks are this preset's own vocabulary —
 * {@code cart}, {@code api}, {@code trophy}, {@code chart}, {@code rocket} —
 * and each entry names the one it wants. The marks come in two colours:
 * {@code cart} and {@code api} are drawn in the navy of the design's ink,
 * the other three in its gold. This sample keeps a band to one colour, since
 * mixing them inside one row reads as a mistake rather than a choice.</p>
 */
public final class SerifHeadlineSampleData {

    private static final String NEWLINE = String.valueOf((char) 10);

    private SerifHeadlineSampleData() {
    }

    /**
     * A backend engineer's one-page CV.
     *
     * @return the sample document
     */
    public static CvDocument sample() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Tomas", "Halvorsen")
                        .jobTitle("Platform Engineer")
                        .contact(new Contact("+47 22 12 34 56",
                                "tomas.h@example.com",
                                "Oslo, Norway"))
                        .link(new Link("linkedin.com/in/thalvorsen",
                                "https://www.linkedin.com/in/thalvorsen"))
                        .link(new Link("github.com/thalvorsen",
                                "https://github.com/thalvorsen"))
                        .build())
                .section(new ParagraphSection("Summary",
                        "Platform engineer with seven years spent making other teams'"
                                + " deployments boring: build pipelines, the service"
                                + " templates behind them, and the observability that says"
                                + " whether a release went well. Writes the runbook before"
                                + " the incident."))
                .section(EntriesSection.builder("Experience")
                        .entry(CvEntry.builder("Senior Platform Engineer")
                                .subtitle("Nordvik Systems")
                                .place("Oslo, NO")
                                .date("Feb 2022 - Present")
                                .body(List.of(
                                        "Rebuilt the deployment pipeline around a golden path,"
                                                + " cutting median lead time from two days to"
                                                + " forty minutes.",
                                        "Owns the Kubernetes platform four product teams ship"
                                                + " onto, and the on-call rota that follows it.",
                                        "Introduced service-level objectives the teams actually"
                                                + " read, and retired the dashboards nobody did."))
                                .build())
                        .entry(CvEntry.builder("Platform Engineer")
                                .subtitle("Bergen Data")
                                .place("Bergen, NO")
                                .date("Aug 2019 - Jan 2022")
                                .body(List.of(
                                        "Moved forty services off hand-rolled scripts onto a"
                                                + " shared Terraform module set.",
                                        "Built the log and metric pipeline the incident process"
                                                + " still runs on.",
                                        "Ran the migration to a single identity provider across"
                                                + " every internal tool."))
                                .build())
                        .entry(CvEntry.builder("Backend Engineer")
                                .subtitle("Fjordline Software")
                                .place("Oslo, NO")
                                .date("Sep 2017 - Jul 2019")
                                .body(List.of(
                                        "Delivered booking and payment services for a ferry"
                                                + " operator's public API.",
                                        "Replaced a nightly reconciliation batch with a"
                                                + " streaming job."))
                                .build())
                        .build())
                .section(EntriesSection.builder("Projects")
                        .entry(CvEntry.builder("Ledgerkit")
                                .subtitle("Java, PostgreSQL, Testcontainers")
                                .body("Open-source double-entry ledger for JVM services, with a"
                                        + " property-based suite over the posting rules.")
                                .icon("chart")
                                .link("https://github.com/thalvorsen/ledgerkit")
                                .build())
                        .entry(CvEntry.builder("Runbook Digest")
                                .subtitle("Kotlin, Kafka, OpenSearch")
                                .body("Reads alert history and drafts the retrospective agenda"
                                        + " before the meeting starts.")
                                .icon("rocket")
                                .link("https://github.com/thalvorsen/runbook-digest")
                                .build())
                        .build())
                .section(EntriesSection.builder("Education")
                        .entry(CvEntry.builder("M.Sc. in Computer Science")
                                .subtitle("University of Oslo")
                                .date("2015 - 2017")
                                .place("Oslo, NO")
                                .build())
                        .entry(CvEntry.builder("B.Sc. in Informatics")
                                .subtitle("NTNU")
                                .date("2012 - 2015")
                                .place("Trondheim, NO")
                                .build())
                        .build())
                .section(SkillsSection.builder("Skills")
                        .leveledGroup("Languages", List.of(
                                CvSkill.of("Java", 0.86),
                                CvSkill.of("Kotlin", 0.72),
                                CvSkill.of("Go", 0.48),
                                CvSkill.of("SQL", 0.8)))
                        .leveledGroup("Platform", List.of(
                                CvSkill.of("Kubernetes", 0.84),
                                CvSkill.of("Terraform", 0.78),
                                CvSkill.of("GitHub Actions", 0.75),
                                CvSkill.of("Argo CD", 0.6)))
                        .leveledGroup("Data", List.of(
                                CvSkill.of("PostgreSQL", 0.82),
                                CvSkill.of("Kafka", 0.7),
                                CvSkill.of("OpenSearch", 0.55)))
                        .leveledGroup("Practices", List.of(
                                CvSkill.of("Observability", 0.8),
                                CvSkill.of("Incident Response", 0.76),
                                CvSkill.of("Threat Modelling", 0.55)))
                        .build())
                .section(new ParagraphSection("Soft Skills", String.join(NEWLINE,
                        "Mentoring   |   Facilitation",
                        "Writing   |   Pragmatism",
                        "Patience   |   Curiosity")))
                .section(RowsSection.builder("Certifications", RowStyle.PLAIN)
                        .row("Certified Kubernetes", "Administrator (CNCF)")
                        .row("Terraform Associate", "(HashiCorp)")
                        .row("AWS Solutions Architect", "Associate (AWS)")
                        .row("Professional Cloud", "Architect (Google)")
                        .row("Site Reliability", "Foundations (Linux Foundation)")
                        .build())
                .section(EntriesSection.builder("Achievements")
                        .entry(CvEntry.builder("Lead Time Cut by 96%")
                                .body("Took median deployment lead time from two days to forty"
                                        + " minutes across four teams.")
                                .icon("chart")
                                .build())
                        .entry(CvEntry.builder("Zero-Downtime Migration")
                                .body("Moved the booking platform onto Kubernetes over six"
                                        + " weeks without a customer-visible outage.")
                                .icon("rocket")
                                .build())
                        .entry(CvEntry.builder("Engineer of the Year 2024")
                                .body("Recognised for the platform work and for the mentoring"
                                        + " that came with it.")
                                .icon("trophy")
                                .build())
                        .build())
                .build();
    }
}
