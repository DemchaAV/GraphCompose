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
 * Shared fixture data for the {@link TerracottaRail} gates — the SAME
 * document feeds the pixel parity test and the layout snapshot test, so a
 * geometry shift the pixel budget absorbs still trips the exact snapshot,
 * and vice versa.
 */
final class TerracottaRailFixtures {

    /** The break a fact stacks its values on. */
    private static final String NEWLINE = String.valueOf((char) 10);

    /** The dash this sheet writes a date range with. */
    private static final String EN_DASH = String.valueOf((char) 0x2013);

    private TerracottaRailFixtures() {
    }

    /**
     * The canonical one-page CV — the monogram, four contact rows including
     * a link, two bulleted lists, two credentials, three facts behind their
     * marks, a three-paragraph summary, three roles on the rail, three
     * marked projects and two degrees.
     */
    static CvDocument canonicalCv() {
        List<CvDocument.Placement> placements = new ArrayList<>();
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, competencies()));
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, software()));
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, certifications()));
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, facts()));
        placements.add(new CvDocument.Placement(Slot.MAIN, summary()));
        placements.add(new CvDocument.Placement(Slot.MAIN, experience()));
        placements.add(new CvDocument.Placement(Slot.MAIN, projects()));
        placements.add(new CvDocument.Placement(Slot.MAIN, education()));
        return new CvDocument(identity(), placements);
    }

    static CvIdentity identity() {
        return new CvIdentity(
                CvName.of("OLIVER", "BENNETT"),
                "SENIOR ARCHITECT",
                new Contact("+44 7700 900123", "oliver.bennett@email.com",
                        "Bristol, United Kingdom"),
                List.of(new Link("linkedin.com/in/oliverbennett-architect",
                        "https://linkedin.com/in/oliverbennett-architect")),
                Optional.empty());
    }

    static SkillsSection competencies() {
        return plainSkills("CORE COMPETENCIES",
                "Architectural Design", "Concept Development", "Planning Applications",
                "Technical Drawings", "Revit", "AutoCAD", "BIM Coordination",
                "Design Presentations", "Project Delivery", "Stakeholder Management",
                "Sustainable Design", "Building Regulations");
    }

    static SkillsSection software() {
        return plainSkills("SOFTWARE",
                "Revit", "AutoCAD", "Rhino", "SketchUp", "Adobe InDesign", "Photoshop",
                "Microsoft Office");
    }

    static EntriesSection certifications() {
        return new EntriesSection("CERTIFICATIONS", List.of(
                CvEntry.builder("ARB Registered Architect").build(),
                CvEntry.builder("RIBA Chartered Member").build()));
    }

    static EntriesSection facts() {
        return new EntriesSection("ADDITIONAL INFORMATION", List.of(
                CvEntry.builder("Languages:")
                        .icon("globe")
                        .body("English (Native)," + NEWLINE + "Spanish (Conversational)")
                        .build(),
                CvEntry.builder("Right to Work:")
                        .icon("badge")
                        .body("United Kingdom")
                        .build(),
                CvEntry.builder("Availability:")
                        .icon("clock")
                        .body("1 month notice")
                        .build()));
    }

    static ParagraphSection summary() {
        return new ParagraphSection("PROFESSIONAL SUMMARY", String.join(NEWLINE,
                "Senior Architect with over 8 years of experience delivering residential, "
                        + "mixed-use, and commercial projects across all RIBA work stages.",
                "Proven expertise in design development, planning applications, and technical "
                        + "delivery with a strong focus on sustainability, quality, and user "
                        + "experience.",
                "Collaborative communicator skilled in coordinating consultants, engaging "
                        + "stakeholders, and leading multidisciplinary teams to achieve "
                        + "successful outcomes."));
    }

    static EntriesSection experience() {
        return new EntriesSection("PROFESSIONAL EXPERIENCE", List.of(
                CvEntry.builder("Senior Architect")
                        .subtitle("Northline Studio, Bristol, UK")
                        .date("2021" + EN_DASH + "Present")
                        .body(String.join(NEWLINE,
                                "Lead design packages from concept through to detailed design "
                                        + "across mid-scale residential and mixed-use projects.",
                                "Coordinate structural, M&E, landscape and planning consultants "
                                        + "to ensure integrated and buildable solutions.",
                                "Prepare and manage planning applications, design and access "
                                        + "statements, and supporting documentation.",
                                "Present and communicate design proposals to clients and "
                                        + "stakeholders, securing approvals and driving projects "
                                        + "forward."))
                        .build(),
                CvEntry.builder("Architect")
                        .subtitle("Urban Form Partners, Bath, UK")
                        .date("2018" + EN_DASH + "2021")
                        .body(String.join(NEWLINE,
                                "Developed concept and technical designs for a range of "
                                        + "residential, commercial, and education projects.",
                                "Prepared coordinated drawing sets, schedules, and "
                                        + "specifications to support planning and construction.",
                                "Supported project delivery on site, resolving design queries "
                                        + "and ensuring quality outcomes.",
                                "Coordinated with contractors and consultants to maintain "
                                        + "programme, budget and design intent."))
                        .build(),
                CvEntry.builder("Part II Architectural Assistant")
                        .subtitle("Axis Design Workshop, Cardiff, UK")
                        .date("2016" + EN_DASH + "2018")
                        .body(String.join(NEWLINE,
                                "Supported senior architects across all RIBA stages on a "
                                        + "variety of project types.",
                                "Produced CAD and Revit drawings, models, and visualisations to "
                                        + "communicate design intent.",
                                "Undertook research, prepared presentation boards, and assisted "
                                        + "with planning documentation."))
                        .build()));
    }

    static EntriesSection projects() {
        return new EntriesSection("SELECTED PROJECTS", List.of(
                CvEntry.builder("Harbour Point")
                        .subtitle("Mixed-Use Regeneration")
                        .place("Bristol")
                        .icon("building")
                        .body("Mixed-use development delivering 142 homes, retail space and "
                                + "public realm improvements; secured planning consent and is "
                                + "now on site.")
                        .build(),
                CvEntry.builder("The Assembly Hotel")
                        .subtitle("Boutique Hotel Refurbishment")
                        .place("Bath")
                        .icon("hotel")
                        .body("Sensitive refurbishment of listed building to create a "
                                + "36-bedroom boutique hotel; enhanced heritage features and "
                                + "guest experience.")
                        .build(),
                CvEntry.builder("Elmwood Mews")
                        .subtitle("Residential Development")
                        .place("Clifton, Bristol")
                        .icon("house")
                        .body("Design of 28 high-quality homes within a sustainable masterplan; "
                                + "achieved high environmental standards and strong sales "
                                + "performance.")
                        .build()));
    }

    static EntriesSection education() {
        return new EntriesSection("EDUCATION", List.of(
                CvEntry.builder("MArch Architecture")
                        .subtitle("University of Sheffield")
                        .date("2014" + EN_DASH + "2016")
                        .build(),
                CvEntry.builder("BA (Hons) Architecture")
                        .subtitle("University of the West of England")
                        .date("2011" + EN_DASH + "2014")
                        .build()));
    }

    private static SkillsSection plainSkills(String title, String... names) {
        List<CvSkill> entries = new ArrayList<>();
        for (String name : names) {
            entries.add(new CvSkill(name, OptionalDouble.empty()));
        }
        return new SkillsSection(title, List.of(new SkillGroup(title, entries)));
    }
}
