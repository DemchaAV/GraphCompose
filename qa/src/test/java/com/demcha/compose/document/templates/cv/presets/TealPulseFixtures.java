package com.demcha.compose.document.templates.cv.presets;

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
import java.util.OptionalDouble;

/**
 * Shared fixture data for the {@link TealPulse} gates — the SAME document
 * feeds the pixel parity test and the layout snapshot test, so a geometry
 * shift the pixel budget absorbs still trips the exact snapshot, and vice
 * versa.
 */
final class TealPulseFixtures {

    /** The break an entry stacks its highlights on. */
    private static final String NEWLINE = String.valueOf((char) 10);

    /** The dash this sheet writes a date range with. */
    private static final String EN_DASH = String.valueOf((char) 0x2013);

    private TealPulseFixtures() {
    }

    /**
     * The canonical one-page CV — the brand mark, four contact channels
     * including a link, twelve competencies, a summary, three roles with their
     * highlights, a degree, four certifications, three facts and the closing
     * tagline.
     */
    static CvDocument canonicalCv() {
        List<CvDocument.Placement> placements = new ArrayList<>();
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, competencies()));
        placements.add(new CvDocument.Placement(Slot.MAIN, summary()));
        placements.add(new CvDocument.Placement(Slot.MAIN, experience()));
        placements.add(new CvDocument.Placement(Slot.FOOTER, education()));
        placements.add(new CvDocument.Placement(Slot.FOOTER, certifications()));
        placements.add(new CvDocument.Placement(Slot.FOOTER, facts()));
        placements.add(new CvDocument.Placement(Slot.FOOTER, tagline()));
        return new CvDocument(identity(), placements);
    }

    static CvIdentity identity() {
        return new CvIdentity(
                CvName.of("ISABELLA", "MOORE"),
                "REGISTERED NURSE",
                new Contact("+44 7700 900123", "isabella.moore@email.com",
                        "Manchester, United Kingdom"),
                List.of(new Link("LinkedIn", "https://www.linkedin.com/in/isabellamoore-rn")),
                Optional.empty());
    }

    static SkillsSection competencies() {
        List<CvSkill> entries = new ArrayList<>();
        for (String name : new String[] {
                "Patient Assessment", "Care Planning", "Medication Administration",
                "Clinical Documentation", "Infection Prevention", "IV Therapy",
                "Wound Care", "Patient Education", "Safeguarding", "Team Collaboration",
                "Discharge Coordination", "Electronic Health Records"}) {
            entries.add(new CvSkill(name, OptionalDouble.empty()));
        }
        return new SkillsSection("CORE COMPETENCIES",
                List.of(new SkillGroup("CORE COMPETENCIES", entries)));
    }

    static ParagraphSection summary() {
        return new ParagraphSection("PROFESSIONAL SUMMARY",
                "Compassionate Registered Nurse with 7+ years of experience delivering "
                        + "high-quality patient care across acute medical, surgical, and "
                        + "community settings. Skilled in patient assessment, medication "
                        + "administration, multidisciplinary collaboration, discharge "
                        + "planning, and maintaining accurate clinical documentation. Known "
                        + "for calm decision-making, strong communication, and patient-centred "
                        + "care.");
    }

    static EntriesSection experience() {
        return new EntriesSection("PROFESSIONAL EXPERIENCE", List.of(
                CvEntry.builder("Senior Staff Nurse")
                        .subtitle("Manchester Royal Infirmary, Manchester, UK")
                        .date("2021" + EN_DASH + "Present")
                        .body(String.join(NEWLINE,
                                "Deliver direct nursing care for adult patients on a busy "
                                        + "acute medical ward.",
                                "Coordinate care plans with doctors, therapists, and support "
                                        + "staff to improve outcomes.",
                                "Supervise junior nurses and support student placements during "
                                        + "clinical rotations.",
                                "Improved discharge coordination and patient communication "
                                        + "across the ward.",
                                "Maintain precise documentation and ensure compliance with "
                                        + "clinical standards."))
                        .build(),
                CvEntry.builder("Staff Nurse")
                        .subtitle("Salford General Hospital, Salford, UK")
                        .date("2018" + EN_DASH + "2021")
                        .body(String.join(NEWLINE,
                                "Provided nursing care across medical and surgical units.",
                                "Administered medications, monitored vital signs, and escalated "
                                        + "deteriorating patients promptly.",
                                "Supported infection control procedures and safe discharge "
                                        + "planning.",
                                "Built strong rapport with patients and families during "
                                        + "treatment and recovery."))
                        .build(),
                CvEntry.builder("Community Nurse")
                        .subtitle("NorthCare Community Health, Greater Manchester, UK")
                        .date("2016" + EN_DASH + "2018")
                        .body(String.join(NEWLINE,
                                "Delivered home-based nursing care and patient education.",
                                "Managed wound care, medication support, and follow-up visits "
                                        + "for vulnerable patients.",
                                "Worked closely with GPs, social services, and families to "
                                        + "coordinate holistic care."))
                        .build()));
    }

    static EntriesSection education() {
        return new EntriesSection("EDUCATION", List.of(
                CvEntry.builder("BSc (Hons) Adult Nursing")
                        .subtitle("University of Manchester")
                        .date("2013" + EN_DASH + "2016")
                        .build()));
    }

    static EntriesSection certifications() {
        List<CvEntry> entries = new ArrayList<>();
        for (String name : new String[] {
                "NMC Registered Nurse", "Immediate Life Support (ILS)",
                "Venepuncture and Cannulation Certification", "Safeguarding Adults Level 3"}) {
            entries.add(CvEntry.builder(name).build());
        }
        return new EntriesSection("CERTIFICATIONS", entries);
    }

    static EntriesSection facts() {
        return new EntriesSection("ADDITIONAL INFORMATION", List.of(
                CvEntry.builder("Languages:")
                        .body("English (Native), Polish (Conversational)")
                        .build(),
                CvEntry.builder("Right to Work:").body("United Kingdom").build(),
                CvEntry.builder("Availability:").body("1 month notice").build()));
    }

    /**
     * The closing line. Its title names the berth and is not drawn — only the
     * body reaches the sheet.
     */
    static ParagraphSection tagline() {
        return new ParagraphSection("TAGLINE",
                "COMPASSIONATE CARE. CLINICAL EXCELLENCE. BETTER OUTCOMES.");
    }
}
