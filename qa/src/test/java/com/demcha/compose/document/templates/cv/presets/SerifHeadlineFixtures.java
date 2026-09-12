package com.demcha.compose.document.templates.cv.presets;

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
 * The canonical fixture for {@link SerifHeadline} — the document the design
 * was drawn around, and the one both gates measure.
 *
 * <p>One fixture feeds the layout snapshot and the pixel baseline, so a
 * change that moves the sheet moves both together instead of leaving one
 * gate measuring a document the other no longer renders.</p>
 */
public final class SerifHeadlineFixtures {

    /** The line break the presets split a body on. */
    private static final String NEWLINE = String.valueOf((char) 10);

    private SerifHeadlineFixtures() {
    }

    /**
     * The canonical CV: five contact channels, three roles, two projects,
     * two degrees, four skill groups, three soft-skill lines, five
     * certifications and three achievements — one page.
     *
     * @return the fixture document
     */
    public static CvDocument canonicalCv() {
        return CvDocument.builder()
                .identity(CvIdentity.builder()
                        .name("Alexander", "Morgan")
                        .jobTitle("Software Engineer")
                        .contact(new Contact("+1 (555) 123-4567",
                                "alex.morgan@email.com",
                                "New York, NY, USA"))
                        .link(new Link("linkedin.com/in/alexmorgan",
                                "https://www.linkedin.com/in/alexmorgan"))
                        .link(new Link("github.com/alexmorgan",
                                "https://github.com/alexmorgan"))
                        .build())
                .section(new ParagraphSection("Summary",
                        "Software Engineer with 5+ years of experience designing and building scalable web applications and backend services. Proficient in Java, Spring Boot, and cloud technologies. Passionate about clean code, system design, and delivering high-quality software that creates real value for users."))
                .section(EntriesSection.builder("Experience")
                        .entry(CvEntry.builder("Senior Software Engineer")
                                .subtitle("TechSolutions Inc.")
                                .date("Jan 2022 – Present")
                                .place("New York, NY")
                                .body(List.of(
                                        "Designed and developed microservices using Java, Spring Boot, and Docker serving over 1M active users.",
                                        "Implemented RESTful APIs and integrated with third-party services.",
                                        "Optimized database queries and application performance, improving response time by 35%.",
                                        "Led a team of 4 engineers and mentored junior developers."
                                ))
                                .build())
                        .entry(CvEntry.builder("Software Engineer")
                                .subtitle("InnovaTech Ltd.")
                                .date("Jun 2019 – Dec 2021")
                                .place("New York, NY")
                                .body(List.of(
                                        "Built scalable web applications using Java, Hibernate, and JavaScript.",
                                        "Collaborated with cross-functional teams to deliver new features.",
                                        "Implemented CI/CD pipelines using Jenkins and Docker.",
                                        "Improved code quality and reduced production bugs by 25%."
                                ))
                                .build())
                        .entry(CvEntry.builder("Junior Software Engineer")
                                .subtitle("DevSphere")
                                .date("Jun 2018 – May 2019")
                                .place("New York, NY")
                                .body(List.of(
                                        "Assisted in developing and maintaining internal tools and applications.",
                                        "Wrote unit and integration tests to ensure code reliability.",
                                        "Participated in agile ceremonies and contributed to sprint goals."
                                ))
                                .build())
                        .build())
                .section(EntriesSection.builder("Projects")
                        .entry(CvEntry.builder("E-Commerce Platform")
                                .subtitle("Java, Spring Boot, React, PostgreSQL, AWS")
                                .body("Developed a full-stack e-commerce platform with user authentication, product catalog, cart, and payment integration.")
                                .icon("cart")
                                .build())
                        .entry(CvEntry.builder("Task Management API")
                                .subtitle("Java, Spring Boot, MongoDB, Docker")
                                .body("Built a RESTful API for task management with role-based access, real-time updates, and Dockerized deployment.")
                                .icon("api")
                                .build())
                        .build())
                .section(EntriesSection.builder("Education")
                        .entry(CvEntry.builder("M.S. in Computer Science")
                                .subtitle("New York University")
                                .date("2018 – 2020")
                                .place("New York, NY")
                                .build())
                        .entry(CvEntry.builder("B.S. in Computer Science")
                                .subtitle("University of California, Los Angeles")
                                .date("2014 – 2018")
                                .place("Los Angeles, CA")
                                .build())
                        .build())
                .section(SkillsSection.builder("Skills")
                        .leveledGroup("Languages", List.of(
                                CvSkill.of("Java", 0.75),
                                CvSkill.of("Python", 0.58),
                                CvSkill.of("JavaScript / TypeScript", 0.48),
                                CvSkill.of("SQL", 0.75)
                        ))
                        .leveledGroup("Frameworks & Libraries", List.of(
                                CvSkill.of("Spring Boot", 0.75),
                                CvSkill.of("Hibernate", 0.54),
                                CvSkill.of("React", 0.75),
                                CvSkill.of("Node.js", 0.39),
                                CvSkill.of("Express", 0.75)
                        ))
                        .leveledGroup("Databases", List.of(
                                CvSkill.of("MySQL", 0.75),
                                CvSkill.of("PostgreSQL", 0.75),
                                CvSkill.of("MongoDB", 0.75)
                        ))
                        .leveledGroup("Tools & Technologies", List.of(
                                CvSkill.of("Docker", 0.75),
                                CvSkill.of("Kubernetes", 0.75),
                                CvSkill.of("Jenkins", 0.75),
                                CvSkill.of("Git", 0.39),
                                CvSkill.of("AWS", 0.75)
                        ))
                        .build())
                .section(new ParagraphSection("Soft Skills", String.join(NEWLINE,
                        "Problem Solving   |   Communication",
                        "Teamwork   |   Adaptability",
                        "Leadership   |   Time Management"
                )))
                .section(RowsSection.builder("Certifications", RowStyle.PLAIN)
                        .row("AWS Certified Developer – Associate",
                                "(AWS)")
                        .row("Oracle Certified Professional,",
                                "Java SE 17 (Oracle)")
                        .row("Docker Certified Associate",
                                "(DCA)")
                        .row("Spring Professional Certification",
                                "(VMware)")
                        .row("Kubernetes Fundamentals",
                                "(Linux Foundation)")
                        .build())
                .section(EntriesSection.builder("Achievements")
                        .entry(CvEntry.builder("Engineer of the Year – 2023")
                                .body("Recognized for outstanding performance and leadership.")
                                .icon("trophy")
                                .build())
                        .entry(CvEntry.builder("Performance Optimization")
                                .body("Improved system efficiency by 40% through database and code optimizations.")
                                .icon("chart")
                                .build())
                        .entry(CvEntry.builder("Successful Migration")
                                .body("Led the migration of a monolith application to microservices architecture.")
                                .icon("rocket")
                                .build())
                        .build())
                .build();
    }
}
