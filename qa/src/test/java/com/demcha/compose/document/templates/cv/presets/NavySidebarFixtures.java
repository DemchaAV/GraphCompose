package com.demcha.compose.document.templates.cv.presets;

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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

/**
 * The canonical fixture for {@link NavySidebar} — the document the design was
 * drawn around, and the one both gates measure.
 *
 * <p>One fixture feeds the layout snapshot and the pixel baseline, so a
 * change that moves the sheet moves both together instead of leaving one
 * gate measuring a document the other no longer renders.</p>
 *
 * <p>The portrait is drawn here rather than committed: a photograph in the
 * repository would be a licensed asset guarding a layout, and what the gates
 * need is only that the disc has something in it. It is deterministic, so
 * the pixel baseline is stable.</p>
 */
public final class NavySidebarFixtures {

    private NavySidebarFixtures() {
    }

    /**
     * The canonical CV: a portrait, four contact channels, two degrees, eight
     * skills, three languages, three roles, three achievements and three
     * certifications — one page.
     *
     * @return the fixture document
     */
    public static CvDocument canonicalCv() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Olivia", "Martinez")
                        .jobTitle("Marketing Manager")
                        .contact(new Contact("+1 (555) 123-4567",
                                "your.email@gmail.com",
                                "New York, NY, USA"))
                        .link(new Link("linkedin.com/in/yourname",
                                "https://linkedin.com/in/yourname"))
                        .portrait(portrait())
                        .build())
                .section(Slot.MAIN, new ParagraphSection("SUMMARY",
                        "Results-driven marketing professional with 5+ years of experience"
                                + " developing and executing data-driven marketing strategies"
                                + " that drive brand growth, increase engagement, and deliver"
                                + " measurable results. Skilled in digital marketing, market"
                                + " research, and cross-functional collaboration."))
                .section(Slot.SIDEBAR, EntriesSection.builder("Education")
                        .entry("Master of Science in Marketing", "New York University",
                                "2020 - 2022", "New York, NY")
                        .entry("Bachelor of Business Administration", "University of California",
                                "2016 - 2020", "Los Angeles, CA")
                        .build())
                .section(Slot.SIDEBAR, SkillsSection.of("Skills", SkillGroup.of("Core",
                        "Digital Marketing",
                        "Data Analysis",
                        "Project Management",
                        "SEO / SEM",
                        "Microsoft Office Suite",
                        "Google Analytics",
                        "Social Media Strategy",
                        "Content Marketing")))
                .section(Slot.SIDEBAR, RowsSection.builder("Languages", RowStyle.PLAIN)
                        .row("English", "Native")
                        .row("Spanish", "Advanced")
                        .row("French", "Intermediate")
                        .build())
                .section(Slot.MAIN, EntriesSection.builder("Experience")
                        .entry("Marketing Manager",
                                "BrightWave Solutions, New York, NY",
                                "Jan 2022 - Present",
                                String.join("\n",
                                        "Develop and implement integrated marketing campaigns"
                                                + " that increased brand awareness by 35% and"
                                                + " lead generation by 50%.",
                                        "Manage a team of 4 marketing specialists and"
                                                + " collaborate with sales, product, and design"
                                                + " teams to align strategies.",
                                        "Analyze campaign performance using Google Analytics"
                                                + " and other tools to optimize ROI and"
                                                + " reporting."))
                        .entry("Marketing Specialist",
                                "BrightWave Solutions, New York, NY",
                                "Jun 2020 - Dec 2021",
                                String.join("\n",
                                        "Executed digital marketing campaigns across SEO, PPC,"
                                                + " email, and social media channels.",
                                        "Conducted market research and competitor analysis to"
                                                + " identify opportunities and inform strategy.",
                                        "Created engaging content for blogs, newsletters, and"
                                                + " social media that increased engagement by"
                                                + " 40%."))
                        .entry("Marketing Intern",
                                "BrightWave Solutions, New York, NY",
                                "Jan 2020 - May 2020",
                                String.join("\n",
                                        "Assisted in planning and executing marketing"
                                                + " initiatives and events.",
                                        "Supported content creation and social media"
                                                + " management.",
                                        "Analyzed data and prepared reports to support the"
                                                + " marketing team."))
                        .build())
                .section(Slot.MAIN, new ParagraphSection("Achievements", String.join("\n",
                        "Increased website traffic by 60% within one year through SEO and"
                                + " content strategy.",
                        "Launched a successful email marketing campaign with a 25% conversion"
                                + " rate.",
                        "Received \u201cEmployee of the Year\u201d award in 2023 for outstanding"
                                + " performance.")))
                .section(Slot.MAIN, new ParagraphSection("Certifications", String.join("\n",
                        "Google Analytics Individual Qualification (GAIQ)",
                        "HubSpot Content Marketing Certification",
                        "Facebook Blueprint Certification")))
                .build();
    }

    /**
     * A flat two-tone silhouette, drawn at fixture time.
     *
     * @return the portrait image data
     */
    static DocumentImageData portrait() {
        BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(222, 226, 231));
            g.fillRect(0, 0, 512, 512);
            g.setColor(new Color(150, 160, 173));
            g.fillOval(171, 102, 169, 169);
            g.fillOval(67, 307, 379, 379);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to draw the fixture portrait", e);
        }
        return DocumentImageData.fromBytes(png.toByteArray());
    }
}
