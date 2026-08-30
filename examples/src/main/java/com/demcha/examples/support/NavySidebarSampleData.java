package com.demcha.examples.support;

import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.RowStyle;
import com.demcha.compose.document.templates.cv.data.RowsSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import com.demcha.compose.document.templates.cv.data.Slot;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Sample data for the Navy Sidebar CV example.
 *
 * <p>Sized to the design: the preset draws a fixed one-page sheet, and a CV
 * much longer than this one does not compose at all — the two columns are a
 * single atomic row, so it raises {@code AtomicNodeTooLargeException}.</p>
 *
 * <p>The portrait is a neutral silhouette rather than a photograph, because
 * the example ships in the repository; a real CV passes the candidate's own
 * image to {@code CvIdentity.Builder.portrait(...)} the same way.</p>
 */
public final class NavySidebarSampleData {

    private static final String PORTRAIT = "/cv-portrait-placeholder.png";

    private NavySidebarSampleData() {
    }

    /**
     * A marketing manager's one-page CV.
     *
     * @return the sample document
     */
    public static CvDocument sample() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Priya", "Raghavan")
                        .jobTitle("Marketing Manager")
                        .contact(new Contact("+44 20 7946 0812",
                                "priya.raghavan@example.com",
                                "Bristol, United Kingdom"))
                        .link(new Link("linkedin.com/in/praghavan",
                                "https://linkedin.com/in/praghavan"))
                        .portrait(portrait())
                        .build())
                .section(Slot.MAIN, new ParagraphSection("Summary",
                        "Marketing manager with eight years in B2B software, most of it"
                                + " running the demand side end to end: positioning, the"
                                + " campaigns that carry it, and the reporting that says"
                                + " whether it worked. Happiest with a small team and a"
                                + " short feedback loop."))
                .section(Slot.SIDEBAR, EntriesSection.builder("Education")
                        .entry("MSc Marketing Analytics", "University of Bristol",
                                "2015 - 2016", "Bristol, UK")
                        .entry("BA Business Management", "University of Leeds",
                                "2012 - 2015", "Leeds, UK")
                        .build())
                .section(Slot.SIDEBAR, SkillsSection.of("Skills", SkillGroup.of("Core",
                        "Positioning",
                        "Demand Generation",
                        "Marketing Analytics",
                        "SEO / SEM",
                        "Lifecycle Email",
                        "Content Strategy",
                        "HubSpot",
                        "Looker")))
                .section(Slot.SIDEBAR, RowsSection.builder("Languages", RowStyle.PLAIN)
                        .row("English", "Native")
                        .row("Tamil", "Native")
                        .row("German", "Intermediate")
                        .build())
                .section(Slot.MAIN, EntriesSection.builder("Experience")
                        .entry("Marketing Manager",
                                "Ardent Systems, Bristol, UK",
                                "Mar 2021 - Present",
                                String.join("\n",
                                        "Rebuilt the demand programme around three named"
                                                + " segments, lifting qualified pipeline 44% in"
                                                + " the first year.",
                                        "Runs a team of four across content, lifecycle and"
                                                + " events, and the agency relationship behind"
                                                + " paid search.",
                                        "Replaced a weekly spreadsheet with a Looker model the"
                                                + " sales team reads without asking for it."))
                        .entry("Senior Marketing Executive",
                                "Halworth Digital, Bristol, UK",
                                "Sep 2018 - Feb 2021",
                                String.join("\n",
                                        "Owned lifecycle email end to end, taking trial-to-paid"
                                                + " conversion from 9% to 14%.",
                                        "Launched the customer-story programme that still"
                                                + " supplies the sales deck.",
                                        "Ran competitor and win-loss research each quarter for"
                                                + " the product team."))
                        .entry("Marketing Executive",
                                "Kite & Compass, Leeds, UK",
                                "Oct 2016 - Aug 2018",
                                String.join("\n",
                                        "Planned and ran campaigns across search, social and"
                                                + " trade press for six retail clients.",
                                        "Built the reporting pack the agency used for every"
                                                + " monthly review."))
                        .build())
                .section(Slot.MAIN, new ParagraphSection("Achievements", String.join("\n",
                        "Grew organic sessions 60% in a year by rebuilding the site around"
                                + " search intent rather than the org chart.",
                        "Cut cost per qualified lead by a third by retiring two channels and"
                                + " funding the one that worked.",
                        "Named marketer of the year at Ardent in 2024.")))
                .section(Slot.MAIN, new ParagraphSection("Certifications", String.join("\n",
                        "Google Analytics Individual Qualification",
                        "HubSpot Content Marketing Certification",
                        "Professional Certificate in Marketing, CIM")))
                .build();
    }

    /**
     * The packaged silhouette that stands in for a photograph.
     *
     * @return the portrait image data
     */
    private static DocumentImageData portrait() {
        try (InputStream in = Objects.requireNonNull(
                NavySidebarSampleData.class.getResourceAsStream(PORTRAIT),
                "cv-portrait-placeholder.png missing from examples/src/main/resources/")) {
            return DocumentImageData.fromBytes(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the sample portrait", e);
        }
    }
}
