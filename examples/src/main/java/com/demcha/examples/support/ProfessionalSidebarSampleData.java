package com.demcha.examples.support;

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
 * Sample data for the Professional Sidebar CV example.
 *
 * <p>Sized to the design: the preset draws a fixed one-page sheet, and a CV
 * much longer than this one does not compose at all — the two columns are a
 * single atomic row, so it raises {@code AtomicNodeTooLargeException}. The
 * sidebar's one-line rows are sized to it too: a degree, skill or language
 * longer than the narrow column measures overflows the row it is anchored in
 * rather than wrapping inside it.</p>
 */
public final class ProfessionalSidebarSampleData {

    /** The en dash the design sets between dates. */
    private static final String DASH = "–";

    private ProfessionalSidebarSampleData() {
    }

    /**
     * A backend engineer's one-page CV.
     *
     * @return the sample document
     */
    public static CvDocument sample() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("MARTA", "LINDQVIST")
                        .jobTitle("SENIOR BACKEND ENGINEER")
                        .contact(new Contact("+46 8 123 456 78",
                                "marta.lindqvist@example.com",
                                "Stockholm, Sweden"))
                        .link(new Link("linkedin.com/in/mlindqvist",
                                "https://linkedin.com/in/mlindqvist"))
                        .link(new Link("martalindqvist.dev",
                                "https://martalindqvist.dev"))
                        .build())
                .section(Slot.MAIN, new ParagraphSection("PROFILE",
                        "Backend engineer with nine years on payment and identity systems,"
                                + " most of it owning services end to end: API design, the data"
                                + " model underneath, and the on-call rota that follows."
                                + " Comfortable in a team that ships small and often, and the"
                                + " one who writes the runbook nobody asked for."))
                .section(Slot.SIDEBAR, SkillsSection.of("SKILLS",
                        new SkillGroup("Core", List.of(
                                CvSkill.of("Java", 0.9),
                                CvSkill.of("Kotlin", 0.72),
                                CvSkill.of("Spring Boot", 0.86),
                                CvSkill.of("PostgreSQL", 0.8),
                                CvSkill.of("Kafka", 0.68),
                                CvSkill.of("Kubernetes", 0.62),
                                CvSkill.of("Terraform", 0.55),
                                CvSkill.of("Observability", 0.74),
                                CvSkill.of("Threat Modelling", 0.6),
                                CvSkill.of("Mentoring", 0.78)))))
                .section(Slot.SIDEBAR, EntriesSection.builder("EDUCATION")
                        .entry("MSc COMPUTER SCIENCE", "KTH Royal Institute",
                                "2014 " + DASH + " 2016", "")
                        .entry("BSc SOFTWARE DESIGN", "Uppsala University",
                                "2011 " + DASH + " 2014", "")
                        .build())
                .section(Slot.SIDEBAR, SkillsSection.of("LANGUAGES",
                        new SkillGroup("Spoken", List.of(
                                CvSkill.of("Swedish", 1.0),
                                CvSkill.of("English", 1.0),
                                CvSkill.of("German", 0.6)))))
                .section(Slot.MAIN, EntriesSection.builder("EXPERIENCE")
                        .entry("SENIOR BACKEND ENGINEER",
                                "Nordkassa   |   Stockholm, SE",
                                "2021 " + DASH + " Present",
                                String.join("\n",
                                        "Led the split of the settlement monolith into four"
                                                + " services with no customer-visible downtime.",
                                        "Cut p99 authorisation latency from 780ms to 210ms by"
                                                + " reworking the ledger write path.",
                                        "Introduced contract tests between payments and risk,"
                                                + " ending a class of release-day rollbacks.",
                                        "Mentors three engineers; runs the internal design"
                                                + " review."))
                        .entry("BACKEND ENGINEER",
                                "Vinge Identity   |   Gothenburg, SE",
                                "2018 " + DASH + " 2021",
                                String.join("\n",
                                        "Built the OAuth2 and OIDC provider now serving eleven"
                                                + " internal products.",
                                        "Moved session storage to Redis with a migration that"
                                                + " ran live for six weeks.",
                                        "Wrote the audit pipeline the compliance team still"
                                                + " reports from."))
                        .entry("SOFTWARE ENGINEER",
                                "Almgren Data   |   Uppsala, SE",
                                "2016 " + DASH + " 2018",
                                String.join("\n",
                                        "Delivered ingest and reporting services for retail"
                                                + " forecasting customers.",
                                        "Replaced nightly batch reconciliation with a streaming"
                                                + " job on Kafka."))
                        .build())
                .section(Slot.MAIN, EntriesSection.builder("PROJECTS")
                        .entry("LEDGERKIT", "", "2024",
                                "Open-source double-entry ledger for JVM services, with a"
                                        + " property-based test suite over the posting rules.")
                        .entry("ONCALL DIGEST", "", "2022",
                                "Weekly incident summariser that reads alert history and drafts"
                                        + " the retrospective agenda.")
                        .build())
                .section(Slot.MAIN, new ParagraphSection("REFERENCES",
                        "Available on request."))
                .build();
    }
}
