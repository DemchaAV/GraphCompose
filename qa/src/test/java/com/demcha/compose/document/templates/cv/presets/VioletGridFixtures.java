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

/**
 * Shared fixture data for the {@link VioletGrid} gates — the SAME document
 * feeds the pixel parity test and the layout snapshot test, so a geometry
 * shift the pixel budget absorbs still trips the exact snapshot, and vice
 * versa.
 */
final class VioletGridFixtures {

    /** The break a body stacks its lines on. */
    private static final String NEWLINE = String.valueOf((char) 10);

    private VioletGridFixtures() {
    }

    /**
     * The canonical one-page CV — five contact lines including two links,
     * three opening lines, six marked skills, eleven tools, three roles, two
     * projects, a degree, three rated languages and the closing quotation.
     */
    static CvDocument canonicalCv() {
        List<CvDocument.Placement> placements = new ArrayList<>();
        placements.add(new CvDocument.Placement(Slot.MAIN, summary()));
        placements.add(new CvDocument.Placement(Slot.MAIN, skills()));
        placements.add(new CvDocument.Placement(Slot.MAIN, tools()));
        placements.add(new CvDocument.Placement(Slot.MAIN, experience()));
        placements.add(new CvDocument.Placement(Slot.MAIN, projects()));
        placements.add(new CvDocument.Placement(Slot.FOOTER, education()));
        placements.add(new CvDocument.Placement(Slot.FOOTER, languages()));
        placements.add(new CvDocument.Placement(Slot.FOOTER, quote()));
        return new CvDocument(identity(), placements);
    }

    static CvIdentity identity() {
        return new CvIdentity(
                CvName.of("SOFIA", "MARTINEZ"),
                "UX / UI DESIGNER",
                new Contact("+1 (415) 555-7842",
                        "sofia.martinez.design@gmail.com",
                        "San Francisco, CA, USA"),
                List.of(
                        new Link("sofiamartinez.design", "https://sofiamartinez.design"),
                        new Link("LinkedIn", "https://www.linkedin.com/in/sofia-martinez-ux")),
                Optional.empty());
    }

    /** The opening lines, set as written rather than wrapped. */
    static ParagraphSection summary() {
        return new ParagraphSection("SUMMARY", String.join(NEWLINE,
                "User-centered designer with 5+ years of experience "
                        + "crafting intuitive digital experiences for web and "
                        + "mobile products.",
                "I combine research, design thinking, and visual design "
                        + "to solve complex problems and deliver accessible, "
                        + "elegant solutions.",
                "Passionate about collaborating with cross-functional "
                        + "teams to create products that delight users and drive "
                        + "business impact."));
    }

    /** The six-up grid: a mark, a label and a description each. */
    static EntriesSection skills() {
        return new EntriesSection("DESIGN SKILLS", List.of(
                CvEntry.builder("UX RESEARCH")
                        .icon("ux-research")
                        .body("User interviews, surveys, personas, journey maps, "
                                + "competitive analysis")
                        .build(),
                CvEntry.builder("INFORMATION ARCHITECTURE")
                        .icon("information-architecture")
                        .body("Sitemaps, user flows, card sorting, content strategy")
                        .build(),
                CvEntry.builder("WIREFRAMING")
                        .icon("wireframing")
                        .body("Low to high-fidelity wireframes, layout design, "
                                + "interaction flows")
                        .build(),
                CvEntry.builder("PROTOTYPING")
                        .icon("prototyping")
                        .body("Interactive prototypes, microinteractions, transitions")
                        .build(),
                CvEntry.builder("USABILITY TESTING")
                        .icon("usability-testing")
                        .body("Test planning, usability tests, heuristic evaluation, "
                                + "insights & iteration")
                        .build(),
                CvEntry.builder("DESIGN SYSTEMS")
                        .icon("design-systems")
                        .body("Component libraries, design tokens, pattern "
                                + "documentation")
                        .build()));
    }

    /** The tools strip — names without levels. */
    static SkillsSection tools() {
        List<CvSkill> entries = new ArrayList<>();
        for (String name : new String[] {
                "Figma", "FigJam", "Adobe XD", "Sketch", "Miro", "Notion", "Jira", "Slack", "Maze", "Hotjar", "Airtable"}) {
            entries.add(CvSkill.of(name));
        }
        return new SkillsSection("TOOLS",
                List.of(new SkillGroup("TOOLS", entries)));
    }

    /** The roles held, each a headline over its bullets. */
    static EntriesSection experience() {
        return new EntriesSection("EXPERIENCE", List.of(
                CvEntry.builder("Senior UX/UI Designer")
                        .subtitle("NovaFin (FinTech Startup)")
                        .place("San Francisco, CA")
                        .date("2022 - Present")
                        .body(String.join(NEWLINE,
                                "Led end-to-end design for a personal finance platform, "
                                        + "from discovery to launch, improving task success rate by "
                                        + "32%.",
                                "Conducted user research, built personas and journey "
                                        + "maps, and translated insights into clear product "
                                        + "opportunities.",
                                "Designed wireframes, prototypes, and high-fidelity "
                                        + "interfaces for web and mobile using Figma.",
                                "Established and maintained a scalable design system, "
                                        + "increasing design consistency and development speed.",
                                "Collaborated with product managers, engineers, and data "
                                        + "analysts in an agile environment."))
                        .build(),
                CvEntry.builder("UX Designer")
                        .subtitle("BrightHealth (HealthTech)")
                        .place("San Francisco, CA")
                        .date("2020 - 2022")
                        .body(String.join(NEWLINE,
                                "Redesigned patient portal experience, reducing support "
                                        + "tickets by 28% and improving user satisfaction.",
                                "Planned and facilitated usability tests and synthesized "
                                        + "findings to drive iterative improvements.",
                                "Partnered with engineers to ensure feasible solutions "
                                        + "and pixel-perfect implementation.",
                                "Created reusable components and style guides to support "
                                        + "a cohesive cross-platform experience."))
                        .build(),
                CvEntry.builder("Junior UX/UI Designer")
                        .subtitle("Lumen Digital Agency")
                        .place("Austin, TX")
                        .date("2018 - 2020")
                        .body(String.join(NEWLINE,
                                "Supported discovery workshops, user flows, and "
                                        + "wireframes for clients across e-commerce and SaaS.",
                                "Designed responsive websites and landing pages following "
                                        + "best practices in accessibility and usability.",
                                "Collaborated with copywriters and developers to deliver "
                                        + "high-quality, user-centered solutions."))
                        .build()));
    }

    /** The projects, each behind its tinted tile. */
    static EntriesSection projects() {
        return new EntriesSection("SELECTED PROJECTS", List.of(
                CvEntry.builder("NovaFin Mobile App")
                        .subtitle("Personal Finance Management")
                        .date("2023")
                        .icon("project-wallet")
                        .body("Led UX/UI design for budgeting, goal tracking, and "
                                + "insights features. Conducted user interviews and "
                                + "usability tests that informed a simplified navigation "
                                + "and data visualization. Resulted in 32% improvement in "
                                + "task success rate and 22% increase in weekly active "
                                + "users.")
                        .build(),
                CvEntry.builder("BrightHealth Patient Portal")
                        .subtitle("Healthcare Platform Redesign")
                        .date("2021")
                        .icon("project-health")
                        .body("Redesigned key patient flows including appointment "
                                + "scheduling, test results, and messaging. Created a new "
                                + "design system and component library used across web and "
                                + "mobile. Reduced support tickets by 28%.")
                        .build()));
    }

    /** The degree, with the place and the years on its third line. */
    static EntriesSection education() {
        return new EntriesSection("EDUCATION", List.of(
                CvEntry.builder("Bachelor of Fine Arts in Graphic Design")
                        .subtitle("California College of the Arts")
                        .place("San Francisco, CA")
                        .date("2014 - 2018")
                        .icon("graduation")
                        .build()));
    }

    /** The languages, each carrying both the rating and the word for it. */
    static SkillsSection languages() {
        return new SkillsSection("LANGUAGES", List.of(new SkillGroup("LANGUAGES", List.of(
                CvSkill.of("English", 1.0, "Native"),
                CvSkill.of("Spanish", 1.0, "Native"),
                CvSkill.of("Portuguese", 0.6, "Conversational")))));
    }

    /** The closing line. Its title names the berth and is not drawn. */
    static ParagraphSection quote() {
        return new ParagraphSection("QUOTE",
                "I design with empathy, iterate with purpose, and "
                        + "collaborate to build products people love.");
    }
}
