package com.demcha.examples.support;

import com.demcha.compose.document.templates.core.identity.Contact;
import com.demcha.compose.document.templates.core.identity.Link;
import com.demcha.compose.document.templates.cv.data.CvDocument;
import com.demcha.compose.document.templates.cv.data.CvEntry;
import com.demcha.compose.document.templates.cv.data.CvIdentity;
import com.demcha.compose.document.templates.cv.data.CvName;
import com.demcha.compose.document.templates.cv.data.EntriesSection;
import com.demcha.compose.document.templates.cv.data.ParagraphSection;
import com.demcha.compose.document.templates.cv.data.SkillGroup;
import com.demcha.compose.document.templates.cv.data.SkillsSection;
import com.demcha.compose.document.templates.cv.data.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared sample data for the Orange Ops CV example.
 *
 * <p>The sample is the preset's reference content: a one-page CV with four
 * contact items including one link, eleven skills, four achievement cards, a
 * degree, four certifications, the opening prose, three roles, a four-metric
 * strip and four closing lines.</p>
 *
 * <p>Kept in lockstep with the qa module's {@code OrangeOpsFixtures} — the two
 * modules cannot share a source file, so a content change here belongs there
 * too.</p>
 */
public final class OrangeOpsSampleData {

    /** The break a body stacks its lines on. */
    private static final String NEWLINE = String.valueOf((char) 10);

    private OrangeOpsSampleData() {
    }

    /**
     * The sample CV.
     *
     * @return the document the example renders
     */
    public static CvDocument sample() {
        List<CvDocument.Placement> placements = new ArrayList<>();
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, skills()));
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, achievements()));
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, education()));
        placements.add(new CvDocument.Placement(Slot.SIDEBAR, certifications()));
        placements.add(new CvDocument.Placement(Slot.MAIN, profile()));
        placements.add(new CvDocument.Placement(Slot.MAIN, experience()));
        placements.add(new CvDocument.Placement(Slot.MAIN, metrics()));
        placements.add(new CvDocument.Placement(Slot.MAIN, additional()));
        return new CvDocument(identity(), placements);
    }

    private static CvIdentity identity() {
        return new CvIdentity(
                CvName.of("Marcus", "Bennett"),
                "Warehouse Operations Supervisor",
                new Contact("+44 7700 900123",
                        "marcus.bennett@email.com",
                        "Birmingham, West Midlands, UK"),
                List.of(new Link("LinkedIn", "https://www.linkedin.com/in/marcusbennett")),
                Optional.empty());
    }

    /** The skills, as one flat group — the design draws no group names. */
    private static SkillsSection skills() {
        return new SkillsSection("KEY SKILLS", List.of(SkillGroup.ofNames("KEY SKILLS", List.of(
                "Warehouse Operations Management",
                "Team Leadership & Development",
                "Inventory Control & Accuracy",
                "Logistics & Freight Coordination",
                "Order Fulfilment & Dispatch",
                "KPI Monitoring & Reporting",
                "Lean Process Improvement",
                "Health & Safety Compliance",
                "WMS & ERP Systems",
                "Problem Solving & Decision Making",
                "Stakeholder Communication"))));
    }

    /** The achievement cards: a mark, a title and a body each. */
    private static EntriesSection achievements() {
        return new EntriesSection("ACHIEVEMENTS", List.of(
                CvEntry.builder("15% PRODUCTIVITY INCREASE")
                        .icon("achievement-productivity")
                        .body("Improved warehouse productivity by 15% through workflow "
                                + "redesign and performance management.")
                        .build(),
                CvEntry.builder("99.2% INVENTORY ACCURACY")
                        .icon("achievement-accuracy")
                        .body("Maintained inventory accuracy above 99% for 12 "
                                + "consecutive months through cycle count discipline and "
                                + "process control.")
                        .build(),
                CvEntry.builder("ZERO LOST TIME INCIDENTS")
                        .icon("achievement-safety")
                        .body("Led a safety-first culture resulting in zero lost time "
                                + "incidents over 24 months across operations.")
                        .build(),
                CvEntry.builder("\u00a3280K COST SAVINGS")
                        .icon("achievement-savings")
                        .body("Delivered \u00a3280K in annual savings by reducing waste, "
                                + "lowering overtime and renegotiating carrier contracts.")
                        .build()));
    }

    /** The degree, its institution, place and years one line each. */
    private static EntriesSection education() {
        return new EntriesSection("EDUCATION", List.of(
                CvEntry.builder("BSc (Hons) Logistics & Supply Chain Management")
                        .icon("graduation")
                        .body(String.join(NEWLINE,
                                "De Montfort University",
                                "Leicester, UK",
                                "2011 \u2013 2014"))
                        .build()));
    }

    /** The certifications: a title over its issuer. */
    private static EntriesSection certifications() {
        return new EntriesSection("CERTIFICATIONS", List.of(
                CvEntry.builder("IOSH Managing Safely")
                        .subtitle("Institution of Occupational Safety and Health \u2013 2021")
                        .build(),
                CvEntry.builder("Lean Six Sigma Yellow Belt")
                        .subtitle("The Knowledge Academy \u2013 2020")
                        .build(),
                CvEntry.builder("Counterbalance Forklift Truck Instructor")
                        .subtitle("RTITB \u2013 2019")
                        .build(),
                CvEntry.builder("First Aid at Work")
                        .subtitle("St John Ambulance \u2013 2019")
                        .build()));
    }

    /** The opening prose. */
    private static ParagraphSection profile() {
        return new ParagraphSection("PROFESSIONAL PROFILE",
                "Results-driven Warehouse Operations Supervisor with 8+ "
                        + "years of experience leading high-performing teams and "
                        + "optimising warehouse performance in fast-paced "
                        + "distribution environments. Proven track record of "
                        + "improving operational efficiency, ensuring inventory "
                        + "accuracy, and delivering excellent service levels while "
                        + "maintaining a strong focus on health and safety. Adept "
                        + "at using data to drive decisions, streamline processes, "
                        + "and achieve measurable results.");
    }

    /** The roles held, each a headline over its bullets. */
    private static EntriesSection experience() {
        return new EntriesSection("WORK EXPERIENCE", List.of(
                CvEntry.builder("Warehouse Operations Supervisor")
                        .subtitle("ExpressLink Distribution Ltd | Birmingham, UK")
                        .date("Mar 2021 \u2013 Present")
                        .body(String.join(NEWLINE,
                                "Oversee daily warehouse operations across a 150,000 sq "
                                        + "ft facility, managing a team of 45 staff across inbound, "
                                        + "storage, picking, packing and dispatch.",
                                "Ensure timely and accurate order fulfilment, achieving "
                                        + "98%+ on-time delivery.",
                                "Monitor and maintain inventory accuracy through cycle "
                                        + "counts, audits and investigation of variances.",
                                "Analyse KPIs and operational data to identify trends and "
                                        + "implement improvements.",
                                "Coordinate with transport and customer service teams to "
                                        + "resolve delivery issues and improve service levels.",
                                "Drive a culture of safety and accountability; deliver "
                                        + "regular training and ensure full compliance with H&S "
                                        + "regulations.",
                                "Lead continuous improvement initiatives using Lean "
                                        + "principles to reduce waste and enhance productivity."))
                        .build(),
                CvEntry.builder("Warehouse Team Leader")
                        .subtitle("Midland Gate Logistics | Coventry, UK")
                        .date("May 2017 \u2013 Feb 2021")
                        .body(String.join(NEWLINE,
                                "Led a team of 25 warehouse operatives across day-to-day "
                                        + "operations.",
                                "Managed inbound receipts, putaway, picking and shipping "
                                        + "activities.",
                                "Maintained accurate stock records and completed regular "
                                        + "cycle counts.",
                                "Supported the implementation of a new WMS, improving "
                                        + "stock visibility and reporting.",
                                "Reduced picking errors by 23% through training and "
                                        + "process optimisation."))
                        .build(),
                CvEntry.builder("Warehouse Coordinator")
                        .subtitle("TotalSupply Solutions | Northampton, UK")
                        .date("Jun 2015 \u2013 Apr 2017")
                        .body(String.join(NEWLINE,
                                "Coordinated daily operations and allocated tasks to "
                                        + "warehouse staff.",
                                "Monitored stock levels and initiated replenishment to "
                                        + "avoid stockouts.",
                                "Assisted with KPI reporting and performance reviews.",
                                "Ensured compliance with company policies and health & "
                                        + "safety standards."))
                        .build()));
    }

    /**
     * The metric strip. The parenthetical is part of the berth title: the
     * preset splits it off and sets it smaller.
     */
    private static EntriesSection metrics() {
        return new EntriesSection("KEY KPI SNAPSHOT (Recent 12 Months)", List.of(
                CvEntry.builder("15%")
                        .icon("kpi-productivity")
                        .body("Productivity" + NEWLINE + "Increase")
                        .build(),
                CvEntry.builder("99.2%")
                        .icon("kpi-accuracy")
                        .body("Inventory" + NEWLINE + "Accuracy")
                        .build(),
                CvEntry.builder("98.4%")
                        .icon("kpi-delivery")
                        .body("On-Time" + NEWLINE + "Delivery")
                        .build(),
                CvEntry.builder("0")
                        .icon("kpi-safety")
                        .body("Lost Time" + NEWLINE + "Incidents")
                        .build()));
    }

    /** The closing lines: a mark, a label and its value. */
    private static EntriesSection additional() {
        return new EntriesSection("ADDITIONAL INFORMATION", List.of(
                CvEntry.builder("Systems:")
                        .icon("systems")
                        .body("SAP EWM, Manhattan WMS, Microsoft Dynamics, Excel "
                                + "(Advanced)")
                        .build(),
                CvEntry.builder("Languages:")
                        .icon("languages")
                        .body("English (Native)")
                        .build(),
                CvEntry.builder("Driving Licence:")
                        .icon("driving")
                        .body("Full UK Driving Licence")
                        .build(),
                CvEntry.builder("Interests:")
                        .icon("interests")
                        .body("Strength training, Motorsport, Volunteering with local "
                                + "food banks")
                        .build()));
    }
}
