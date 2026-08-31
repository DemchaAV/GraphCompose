package com.demcha.examples.support;

import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvName;
import com.demcha.compose.document.templates.cv.data.CvSkill;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import com.demcha.compose.document.templates.cv.data.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared sample data for the Slate Orange CV example.
 *
 * <p>The sample is the preset's reference content: a one-page CV with four
 * contact lines including a link, ten marked competencies, three
 * achievements, three rated languages, three closing facts, a profile, four
 * roles, a degree and four certifications.</p>
 *
 * <p>Kept in lockstep with the qa module's {@code SlateOrangeFixtures} —
 * the two modules cannot share a source file, so a content change here
 * belongs there too.</p>
 */
public final class SlateOrangeSampleData {

    /** The break a body stacks its lines on. */
    private static final String NEWLINE = String.valueOf((char) 10);

    private SlateOrangeSampleData() {
    }

    /** The single-page reference CV. */
    public static CvDocument sample() {
        List<CvDocument.Placement> placements = new ArrayList<>();
        placements.add(new CvDocument.Placement(Slot.MAIN, specialisms()));
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, competencies()));
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, achievements()));
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, languages()));
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, facts()));
        placements.add(new CvDocument.Placement(Slot.MAIN, profile()));
        placements.add(new CvDocument.Placement(Slot.MAIN, experience()));
        placements.add(new CvDocument.Placement(Slot.FOOTER, education()));
        placements.add(new CvDocument.Placement(Slot.FOOTER, certifications()));
        return new CvDocument(identity(), placements);
    }

    private static CvIdentity identity() {
        return new CvIdentity(
                CvName.of("DANIEL", "HARPER"),
                "VERSATILE PROFESSIONAL",
                new Contact("(555) 123-4567",
                        "daniel.harper@email.com",
                        "Austin, Texas  \u2022  Open to Relocate"),
                List.of(new Link("LinkedIn", "https://www.linkedin.com/in/danielharper")),
                Optional.empty());
    }

    /** The strip under the role. Only the body is drawn; the title names the berth. */
    private static ParagraphSection specialisms() {
        return new ParagraphSection("SPECIALISMS", String.join(NEWLINE,
                "OPERATIONS", "CUSTOMER SUCCESS", "COORDINATION"));
    }

    /** The competencies, each behind the mark its entry names. */
    private static EntriesSection competencies() {
        return new EntriesSection("CORE COMPETENCIES", List.of(
                CvEntry.builder("Customer Service Excellence").icon("customer-service").build(),
                CvEntry.builder("Operations & Process Support").icon("operations").build(),
                CvEntry.builder("Project & Calendar Management").icon("calendar").build(),
                CvEntry.builder("Data Management & Reporting").icon("reporting").build(),
                CvEntry.builder("Communication & Stakeholders").icon("communication").build(),
                CvEntry.builder("Problem Solving & Adaptability").icon("problem-solving").build(),
                CvEntry.builder("Sales & Relationship Building").icon("sales").build(),
                CvEntry.builder("Microsoft Office & Google Suite").icon("office-suite").build(),
                CvEntry.builder("CRM Systems (Salesforce, HubSpot)").icon("crm").build(),
                CvEntry.builder("Time Management & Prioritization").icon("time-management").build()));
    }

    /** The achievements: a bold title over its body, beside one shared trophy. */
    private static EntriesSection achievements() {
        return new EntriesSection("SELECTED ACHIEVEMENTS", List.of(
                CvEntry.builder("Process Improvement")
                        .icon("achievement")
                        .body("Designed a new tracking system that reduced reporting time "
                                + "by 30% and improved accuracy.")
                        .build(),
                CvEntry.builder("Customer Satisfaction")
                        .icon("achievement")
                        .body("Maintained a 96% customer satisfaction rating through "
                                + "responsive support and issue resolution.")
                        .build(),
                CvEntry.builder("Revenue Contribution")
                        .icon("achievement")
                        .body("Consistently exceeded monthly sales goals by 15\u201320% "
                                + "through relationship building and consultative service.")
                        .build()));
    }

    /** The languages, each carrying both the rating and the word for it. */
    private static SkillsSection languages() {
        return new SkillsSection("LANGUAGES", List.of(new SkillGroup("LANGUAGES", List.of(
                CvSkill.of("English", 1.0, "Native"),
                CvSkill.of("Spanish", 0.8, "Professional Working"),
                CvSkill.of("French", 0.4, "Basic")))));
    }

    /** The closing facts, on the same mark column at a wider pitch. */
    private static EntriesSection facts() {
        return new EntriesSection("ADDITIONAL INFORMATION", List.of(
                CvEntry.builder("Availability: Full-time").icon("availability").build(),
                CvEntry.builder("Willing to Relocate: Yes").icon("relocation").build(),
                CvEntry.builder("Remote Work: Open to Hybrid/Remote").icon("remote-work").build()));
    }

    private static ParagraphSection profile() {
        return new ParagraphSection("PROFESSIONAL PROFILE",
                "Adaptable and resourceful professional with 6+ years of "
                        + "experience across operations, customer service, sales, and "
                        + "project support. Known for strong communication, "
                        + "organizational skills, and the ability to thrive in "
                        + "fast-paced environments. Adept at managing priorities, "
                        + "building relationships, and delivering results that drive "
                        + "efficiency, customer satisfaction, and business growth.");
    }

    /** The roles held, each a headline over its bullets. */
    private static EntriesSection experience() {
        return new EntriesSection("PROFESSIONAL EXPERIENCE", List.of(
                CvEntry.builder("Operations Coordinator")
                        .subtitle("BrightStart Logistics, Austin, TX")
                        .date("Jan 2022 \u2013 Present")
                        .body(String.join(NEWLINE,
                                "Coordinate daily operations and scheduling for a team of "
                                        + "12 across multiple sites.",
                                "Develop and maintain dashboards and reports to track KPIs "
                                        + "and operational metrics.",
                                "Streamline communication between departments, reducing "
                                        + "response time by 25%.",
                                "Manage vendor relationships and assist with contract and "
                                        + "invoice reconciliation."))
                        .build(),
                CvEntry.builder("Customer Support Specialist")
                        .subtitle("TechWave Solutions, Remote")
                        .date("Jun 2020 \u2013 Dec 2021")
                        .body(String.join(NEWLINE,
                                "Provided technical support to customers via phone, email, "
                                        + "and live chat.",
                                "Resolved an average of 40+ inquiries daily with a "
                                        + "first-contact resolution rate of 92%.",
                                "Created knowledge base articles and improved internal "
                                        + "documentation.",
                                "Collaborated with product and engineering teams to "
                                        + "escalate and resolve issues."))
                        .build(),
                CvEntry.builder("Sales Associate")
                        .subtitle("Urban Outfitters, Austin, TX")
                        .date("Mar 2019 \u2013 May 2020")
                        .body(String.join(NEWLINE,
                                "Consistently exceeded sales targets by 15\u201320% through "
                                        + "product knowledge and personalized customer service.",
                                "Built and maintained strong customer relationships, "
                                        + "driving repeat business.",
                                "Merchandised floor displays and contributed to visual "
                                        + "standards and promotions.",
                                "Recognized as \u201cTop Performer\u201d for Q4 2019."))
                        .build(),
                CvEntry.builder("Administrative Assistant / Project Assistant")
                        .subtitle("Greenfield Nonprofit Initiative, Austin, TX")
                        .date("May 2018 \u2013 Feb 2019")
                        .body(String.join(NEWLINE,
                                "Supported project managers with scheduling, documentation, "
                                        + "and meeting logistics.",
                                "Managed calendars, travel arrangements, and expense "
                                        + "reports.",
                                "Maintained donor records and prepared reports for "
                                        + "leadership and stakeholders.",
                                "Assisted with event planning and community outreach "
                                        + "initiatives."))
                        .build()));
    }

    /** The degree; the design's third line is where and when, as one line. */
    private static EntriesSection education() {
        return new EntriesSection("EDUCATION", List.of(
                CvEntry.builder("Bachelor of Arts in Communications")
                        .subtitle("University of Texas at Austin")
                        .place("Austin, TX  \u2022  Graduated May 2018")
                        .build()));
    }

    /** The certifications, one bulleted line each. */
    private static EntriesSection certifications() {
        return new EntriesSection("CERTIFICATIONS & TRAINING", List.of(
                CvEntry.builder("Google Project Management Certificate (Coursera) \u2013 2023").build(),
                CvEntry.builder("HubSpot Customer Service Certification \u2013 2022").build(),
                CvEntry.builder("Microsoft Excel: Advanced Formulas & Functions \u2013 2021").build(),
                CvEntry.builder("ServSafe Food Handler Certification \u2013 2019").build()));
    }
}
