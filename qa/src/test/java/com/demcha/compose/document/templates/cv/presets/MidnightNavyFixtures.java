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
 * Shared fixture data for the {@link MidnightNavy} gates — the SAME document
 * feeds the pixel parity test and the layout snapshot test, so a geometry
 * shift the pixel budget absorbs still trips the exact snapshot, and vice
 * versa.
 */
final class MidnightNavyFixtures {

    /** The break a body stacks its lines on. */
    private static final String NEWLINE = String.valueOf((char) 10);

    private MidnightNavyFixtures() {
    }

    /**
     * The canonical one-page CV — five contact rows including two links, two
     * degrees, ten metered skills, three rated languages, the summary, three
     * roles, three achievement discs and two certifications.
     *
     * @return the document
     */
    public static CvDocument canonicalCv() {
        List<CvDocument.Placement> placements = new ArrayList<>();
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, education()));
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, skills()));
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, languages()));
        placements.add(new CvDocument.Placement(Slot.MAIN, summary()));
        placements.add(new CvDocument.Placement(Slot.MAIN, experience()));
        placements.add(new CvDocument.Placement(Slot.MAIN, achievements()));
        placements.add(new CvDocument.Placement(Slot.MAIN, certifications()));
        return new CvDocument(identity(), placements);
    }

    static CvIdentity identity() {
        return new CvIdentity(
                CvName.of("Alex", "Morgan"),
                "Marketing Manager",
                new Contact("+1 (555) 123-4567",
                        "alex.morgan@email.com",
                        "New York, NY, USA"),
                List.of(new Link("LinkedIn", "https://www.linkedin.com/in/alexmorgan"),
                        new Link("Portfolio", "https://alexmorgan.com")),
                Optional.empty());
    }

    /** The degrees, newest first. */
    static EntriesSection education() {
        return new EntriesSection("EDUCATION", List.of(
                CvEntry.builder("MASTER OF BUSINESS ADMINISTRATION")
                        .subtitle("University of Chicago Booth School of Business")
                        .date("2016 \u2013 2018")
                        .build(),
                CvEntry.builder("BACHELOR OF SCIENCE IN MARKETING")
                        .subtitle("Boston University")
                        .date("2012 \u2013 2016")
                        .build()));
    }

    /** The metered skills, as one flat group. */
    static SkillsSection skills() {
        List<CvSkill> entries = new ArrayList<>();
        entries.add(CvSkill.of("Strategic Planning", 0.82));
        entries.add(CvSkill.of("Digital Marketing", 0.84));
        entries.add(CvSkill.of("Market Research", 0.76));
        entries.add(CvSkill.of("Brand Management", 0.84));
        entries.add(CvSkill.of("Data Analysis", 0.81));
        entries.add(CvSkill.of("Project Management", 0.68));
        entries.add(CvSkill.of("Google Analytics", 0.83));
        entries.add(CvSkill.of("SEO / SEM", 0.76));
        entries.add(CvSkill.of("Microsoft Office Suite", 0.75));
        entries.add(CvSkill.of("Communication", 0.96));
        return new SkillsSection("SKILLS",
                List.of(new SkillGroup("SKILLS", entries)));
    }

    /** The languages. The design shows a rating in fifths. */
    static SkillsSection languages() {
        List<CvSkill> entries = new ArrayList<>();
        entries.add(CvSkill.of("English", 1.0));
        entries.add(CvSkill.of("Spanish", 0.8));
        entries.add(CvSkill.of("French", 0.6));
        return new SkillsSection("LANGUAGES",
                List.of(new SkillGroup("LANGUAGES", entries)));
    }

    /** The opening prose. */
    static ParagraphSection summary() {
        return new ParagraphSection("PROFESSIONAL SUMMARY",
                "Results-driven Marketing Manager with 6+ years of "
                        + "experience in developing and executing data-driven "
                        + "marketing strategies that drive brand growth and "
                        + "customer engagement. Proven track record in leading "
                        + "cross-functional teams, managing successful campaigns, "
                        + "and delivering measurable results. Passionate about "
                        + "building strong brands and creating impactful marketing "
                        + "initiatives.");
    }

    /** The roles held, each a headline over its bullets. */
    static EntriesSection experience() {
        return new EntriesSection("EXPERIENCE", List.of(
                CvEntry.builder("MARKETING MANAGER")
                        .subtitle("Starwave Solutions")
                        .place("New York, NY")
                        .date("2021 \u2013 Present")
                        .body(String.join(NEWLINE,
                                "Develop and implement comprehensive marketing strategies "
                                        + "that increased brand awareness by 40% and lead "
                                        + "generation by 35%.",
                                "Manage a team of 6 marketing professionals and "
                                        + "collaborate with sales, product, and design teams to "
                                        + "drive campaign success.",
                                "Oversee digital campaigns across SEO, SEM, social media, "
                                        + "and email marketing resulting in a 25% increase in ROI.",
                                "Analyze market trends and competitor activities to "
                                        + "identify new opportunities and optimize marketing "
                                        + "efforts."))
                        .build(),
                CvEntry.builder("SENIOR MARKETING SPECIALIST")
                        .subtitle("BrightLine Technologies")
                        .place("New York, NY")
                        .date("2018 \u2013 2021")
                        .body(String.join(NEWLINE,
                                "Executed multi-channel marketing campaigns that improved "
                                        + "customer engagement by 30%.",
                                "Conducted market research and data analysis to support "
                                        + "strategic decision-making.",
                                "Managed social media channels and content strategy, "
                                        + "growing followers by 50%.",
                                "Coordinated with creative teams to develop compelling "
                                        + "content and visuals."))
                        .build(),
                CvEntry.builder("MARKETING COORDINATOR")
                        .subtitle("Peak Performance Group")
                        .place("Boston, MA")
                        .date("2016 \u2013 2018")
                        .body(String.join(NEWLINE,
                                "Supported the development and execution of marketing "
                                        + "plans and campaigns.",
                                "Assisted in organizing events, webinars, and promotional "
                                        + "activities.",
                                "Monitored campaign performance and prepared reports for "
                                        + "management.",
                                "Maintained and updated marketing databases and CRM "
                                        + "systems."))
                        .build()));
    }

    /**
     * The discs. A card has one line and no heading over it, so the line is
     * the entry's title.
     */
    static EntriesSection achievements() {
        return new EntriesSection("ACHIEVEMENTS", List.of(
                CvEntry.builder("Increased brand awareness by 40% within 2 years.")
                        .icon("trophy")
                        .build(),
                CvEntry.builder("Boosted lead generation by 35% annually.")
                        .icon("growth")
                        .build(),
                CvEntry.builder("Recognized as Top Performer in 2022.")
                        .icon("award")
                        .build()));
    }

    /** The certifications, one to a column. */
    static EntriesSection certifications() {
        return new EntriesSection("CERTIFICATIONS", List.of(
                CvEntry.builder("Google Analytics Certified")
                        .subtitle("Google")
                        .date("2020")
                        .build(),
                CvEntry.builder("HubSpot Content Marketing Certified")
                        .subtitle("HubSpot Academy")
                        .date("2021")
                        .build()));
    }
}
