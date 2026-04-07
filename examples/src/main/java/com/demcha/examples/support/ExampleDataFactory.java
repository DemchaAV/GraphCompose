package com.demcha.examples.support;

import com.demcha.compose.layout_core.components.content.link.LinkUrl;
import com.demcha.templates.JobDetails;
import com.demcha.templates.api.MainPageCvDTO;
import com.demcha.templates.data.EmailYaml;
import com.demcha.templates.data.Header;
import com.demcha.templates.data.InvoiceData;
import com.demcha.templates.data.InvoiceLineItem;
import com.demcha.templates.data.InvoiceParty;
import com.demcha.templates.data.InvoiceSummaryRow;
import com.demcha.templates.data.LinkYml;
import com.demcha.templates.data.MainPageCV;
import com.demcha.templates.data.ModuleSummary;
import com.demcha.templates.data.ModuleYml;
import com.demcha.templates.data.ProposalData;
import com.demcha.templates.data.ProposalParty;
import com.demcha.templates.data.ProposalPricingRow;
import com.demcha.templates.data.ProposalSection;
import com.demcha.templates.data.ProposalTimelineItem;
import com.demcha.templates.data.ScheduleAssignment;
import com.demcha.templates.data.ScheduleCategory;
import com.demcha.templates.data.ScheduleDay;
import com.demcha.templates.data.ScheduleMetricRow;
import com.demcha.templates.data.SchedulePerson;
import com.demcha.templates.data.ScheduleSlot;
import com.demcha.templates.data.WeeklyScheduleData;

import java.awt.Color;
import java.util.List;

public final class ExampleDataFactory {

    private ExampleDataFactory() {
    }

    public static MainPageCV sampleCv() {
        MainPageCV cv = new MainPageCV();
        cv.setHeader(sampleHeader());
        cv.setModuleSummary(summary(
                "Professional Summary",
                "Platform engineer building resilient PDF and document-generation workflows for reliable business output."));
        cv.setTechnicalSkills(module("Technical Skills", List.of(
                "Java 21, PDFBox, Maven, REST APIs",
                "Template design systems, pagination, semantic layout composition",
                "Testing strategy, CI pipelines, developer enablement")));
        cv.setEducationCertifications(module("Education & Certifications", List.of(
                "**MSc Computer Science** - University of Manchester | 2021",
                "**Oracle Java Certification** - Professional track | 2023")));
        cv.setProjects(module("Projects", List.of(
                "**GraphCompose** - Declarative PDF layout engine for reusable document generation",
                "**Template Studio** - Internal tool for evaluating CV, proposal, and invoice output")));
        cv.setProfessionalExperience(module("Professional Experience", List.of(
                "**Senior Platform Engineer**, Northwind Systems | *2024-Present* - Led reusable document flows for reporting, billing, and hiring operations.",
                "**Software Engineer**, BrightLeaf Labs | *2021-2024* - Built backend services and production document rendering pipelines.")));
        cv.setAdditional(module("Additional Information", List.of(
                "Based in London and available for hybrid or remote collaboration.",
                "Interested in platform architecture, DX, and document-quality automation.")));
        return cv;
    }

    public static MainPageCvDTO sampleCvRewrite() {
        return MainPageCvDTO.from(sampleCv());
    }

    public static Header sampleHeader() {
        Header header = new Header();
        header.setName("Artem Demchyshyn");
        header.setAddress("London, UK");
        header.setPhoneNumber("+44 20 5555 1000");
        header.setEmail(email("artem@demo.dev", "Job Application", "Hello", "artem@demo.dev"));
        header.setLinkedIn(link("https://linkedin.com/in/graphcompose", "LinkedIn"));
        header.setGitHub(link("https://github.com/DemchaAV", "GitHub"));
        return header;
    }

    public static String sampleCoverLetter() {
        return """
                Hiring team at ${companyName},

                I am excited to share my interest in the Senior Platform Engineer role. My recent work has focused on building reusable document-generation systems that balance public API design, render quality, and maintainability.

                I enjoy translating fuzzy workflow requirements into clear template abstractions, reliable test coverage, and examples that make adoption easier for the rest of the team.

                I would welcome the opportunity to bring that same mix of engineering rigor and product thinking to your platform group.
                """;
    }

    public static JobDetails sampleJobDetails() {
        return new JobDetails(
                "https://northwind.example/jobs/platform",
                "Senior Platform Engineer",
                "Northwind Systems",
                "London / Remote",
                "Lead reusable internal platform capabilities.",
                "Senior",
                "Full-time");
    }

    public static InvoiceData sampleInvoice() {
        return new InvoiceData(
                "Invoice",
                "GC-2026-041",
                "02 Apr 2026",
                "16 Apr 2026",
                "Examples Module Delivery",
                "Pending",
                new InvoiceParty(
                        "GraphCompose Studio",
                        List.of("18 Layout Street", "London, UK", "EC1A 4GC"),
                        "billing@graphcompose.dev",
                        "+44 20 5555 1000",
                        "GB-99887766"),
                new InvoiceParty(
                        "Northwind Systems",
                        List.of("Accounts Payable", "410 Market Avenue", "Manchester, UK"),
                        "ap@northwind.example",
                        "+44 161 555 2200",
                        "NW-2026-01"),
                List.of(
                        new InvoiceLineItem("Invoice template delivery", "Built-in template, styling, and metadata layout", "1", "GBP 1,950", "GBP 1,950"),
                        new InvoiceLineItem("Proposal template delivery", "Long-form content flow and pricing table", "1", "GBP 2,150", "GBP 2,150"),
                        new InvoiceLineItem("Examples module", "Runnable file-render examples and usage notes", "1", "GBP 1,200", "GBP 1,200")),
                List.of(
                        new InvoiceSummaryRow("Subtotal", "GBP 5,300", false),
                        new InvoiceSummaryRow("VAT (20%)", "GBP 1,060", false),
                        new InvoiceSummaryRow("Total", "GBP 6,360", true)),
                List.of(
                        "This invoice covers the first delivery package for GraphCompose business templates.",
                        "Please reference the invoice number in any remittance message."),
                List.of(
                        "Payment due within 14 calendar days.",
                        "Bank transfer preferred.",
                        "Contact billing@graphcompose.dev for remittance instructions."),
                "Generated by the standalone GraphCompose examples module."
        );
    }

    public static ProposalData sampleProposal() {
        return new ProposalData(
                "Proposal",
                "PROP-2026-014",
                "02 Apr 2026",
                "16 Apr 2026",
                "GraphCompose rollout for internal document operations",
                "This proposal describes a practical adoption path for reusable GraphCompose templates, render tests, and runnable examples across billing, hiring, and client-facing delivery workflows.",
                new ProposalParty(
                        "GraphCompose Studio",
                        List.of("18 Layout Street", "London, UK", "EC1A 4GC"),
                        "hello@graphcompose.dev",
                        "+44 20 5555 1000",
                        "graphcompose.dev"),
                new ProposalParty(
                        "Northwind Systems",
                        List.of("Product Engineering", "410 Market Avenue", "Manchester, UK"),
                        "platform@northwind.example",
                        "+44 161 555 2200",
                        "northwind.example"),
                List.of(
                        new ProposalSection("Scope", List.of(
                                "Introduce built-in invoice and proposal templates with a consistent business presentation layer.",
                                "Keep the production artifact clean by moving development-only preview code out of the published runtime scope.")),
                        new ProposalSection("Deliverables", List.of(
                                "Public DTOs and template interfaces for invoice and proposal rendering.",
                                "Render tests and a standalone examples module that generates PDF files on demand."))),
                List.of(
                        new ProposalTimelineItem("Week 1", "5 days", "Invoice API and first template delivery."),
                        new ProposalTimelineItem("Week 2", "5 days", "Proposal layout, review loop, and render tests."),
                        new ProposalTimelineItem("Week 3", "3 days", "Examples module and README handoff.")),
                List.of(
                        new ProposalPricingRow("Foundation", "Template APIs and DTO modeling", "GBP 3,200", false),
                        new ProposalPricingRow("Document delivery", "Invoice and proposal templates with tests", "GBP 4,450", false),
                        new ProposalPricingRow("Total investment", "Fixed-price project delivery", "GBP 9,500", true)),
                List.of(
                        "Proposal pricing is valid until the stated expiration date.",
                        "Additional template families can be scoped in a separate phase."),
                "Prepared to demonstrate the business-document side of GraphCompose."
        );
    }

    public static WeeklyScheduleData sampleWeeklySchedule() {
        return new WeeklyScheduleData(
                "Scott's Weekly Floor Schedule",
                "Week Of 30 Mar - 05 Apr 2026",
                List.of(
                        new ScheduleDay("mon", "Monday\n30th", "Clean crushed ice\nMachine & area", "request"),
                        new ScheduleDay("tue", "Tuesday\n31st", "Pianist 18:30\nClean ice machine", "off"),
                        new ScheduleDay("wed", "Wednesday\n1st", "Motown GF\nPianist FF", "hol"),
                        new ScheduleDay("thu", "Thursday\n2nd", "Ex hire terrace\ndinner\nMGM meeting\n3:30pm", "stock"),
                        new ScheduleDay("fri", "Friday\n3rd", "Good Friday\nPianist 19:00", "standby"),
                        new ScheduleDay("sat", "Saturday\n4th", "Masterclass 2x2PAX\nPianist 19:00", "training"),
                        new ScheduleDay("sun", "Sunday\n5th", "Easter Sunday\nFull stock take", "bar-back")),
                List.of(
                        new ScheduleCategory("request", "REQUEST", new Color(166, 166, 166), Color.BLACK, new Color(80, 80, 80)),
                        new ScheduleCategory("off", "OFF", new Color(205, 0, 0), Color.BLACK, new Color(110, 0, 0)),
                        new ScheduleCategory("hol", "HOL", new Color(243, 196, 54), Color.BLACK, new Color(176, 126, 6)),
                        new ScheduleCategory("stock", "STOCK", new Color(0, 173, 76), Color.BLACK, new Color(0, 110, 49)),
                        new ScheduleCategory("standby", "STANDBY", new Color(177, 132, 226), Color.BLACK, new Color(102, 71, 150)),
                        new ScheduleCategory("training", "TRAINING", new Color(245, 131, 24), Color.BLACK, new Color(183, 82, 0)),
                        new ScheduleCategory("bar-back", "BAR BACK", new Color(176, 132, 76), Color.BLACK, new Color(120, 88, 44))),
                List.of(
                        new ScheduleMetricRow("COVERS", List.of("27 / 37", "41 / 36", "30 / 29", "57 / 63", "46 / 71", "73 / 97", "155 / 26")),
                        new ScheduleMetricRow("TEAM FOCUS", List.of("Alex floor", "Glass count", "Vee lab", "Sergii lab", "Pianist", "Alex floor", "Sergii floor"))),
                List.of(
                        new SchedulePerson("sergii", "SERGII", 10),
                        new SchedulePerson("mark", "MARK", 20),
                        new SchedulePerson("alex", "ALEX", 30),
                        new SchedulePerson("bianca", "BIANCA", 40),
                        new SchedulePerson("sam", "SAM", 50),
                        new SchedulePerson("kira", "KIRA", 60),
                        new SchedulePerson("daria", "DARIA", 70),
                        new SchedulePerson("violetta", "VIOLETTA", 80),
                        new SchedulePerson("peter", "PETER", 90),
                        new SchedulePerson("den", "DEN", 100),
                        new SchedulePerson("daniel", "DANIEL", 110)),
                List.of(
                        new ScheduleAssignment("sergii", "mon", "stock", List.of(slot("09:00", "18:00")), ""),
                        new ScheduleAssignment("sergii", "thu", "request", List.of(slot("09:00", "18:00")), ""),
                        new ScheduleAssignment("sergii", "fri", "training", List.of(slot("08:00", "16:00"), slot("16:00", "22:00")), ""),
                        new ScheduleAssignment("sergii", "sun", "standby", List.of(slot("12:00", "16:00"), slot("16:00", "22:00")), ""),
                        new ScheduleAssignment("mark", "mon", "request", List.of(slot("16:00", "00:00")), ""),
                        new ScheduleAssignment("mark", "tue", "off", List.of(slot("12:00", "18:00")), ""),
                        new ScheduleAssignment("mark", "wed", "request", List.of(slot("16:00", "00:00")), ""),
                        new ScheduleAssignment("mark", "fri", "standby", List.of(slot("16:00", "01:00")), ""),
                        new ScheduleAssignment("mark", "sun", "bar-back", List.of(slot("17:00", "03:00")), ""),
                        new ScheduleAssignment("alex", "tue", "off", List.of(slot("16:00", "00:00")), ""),
                        new ScheduleAssignment("alex", "wed", "request", List.of(slot("16:00", "00:00")), ""),
                        new ScheduleAssignment("alex", "thu", "request", List.of(slot("16:00", "00:00")), ""),
                        new ScheduleAssignment("alex", "fri", "standby", List.of(slot("16:00", "22:00")), ""),
                        new ScheduleAssignment("alex", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                        new ScheduleAssignment("bianca", "thu", "standby", List.of(slot("16:00", "00:00")), ""),
                        new ScheduleAssignment("bianca", "fri", "standby", List.of(slot("17:00", "01:00")), ""),
                        new ScheduleAssignment("bianca", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                        new ScheduleAssignment("bianca", "sun", "request", List.of(slot("12:00", "16:00"), slot("17:00", "00:00")), ""),
                        new ScheduleAssignment("artem", "tue", "off", List.of(slot("17:00", "00:00")), ""),
                        new ScheduleAssignment("artem", "wed", "request", List.of(slot("17:00", "00:00")), ""),
                        new ScheduleAssignment("artem", "thu", "request", List.of(slot("17:00", "00:00")), ""),
                        new ScheduleAssignment("artem", "fri", "standby", List.of(slot("16:00", "01:00")), ""),
                        new ScheduleAssignment("artem", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                        new ScheduleAssignment("kharren", "wed", "request", List.of(slot("09:00", "17:00")), ""),
                        new ScheduleAssignment("kharren", "thu", "request", List.of(slot("09:00", "17:00")), ""),
                        new ScheduleAssignment("kharren", "fri", "standby", List.of(slot("09:00", "17:00")), ""),
                        new ScheduleAssignment("kharren", "sat", "training", List.of(slot("16:00", "22:00")), ""),
                        new ScheduleAssignment("kharren", "sun", "request", List.of(slot("09:00", "16:00"), slot("17:00", "22:00")), ""),
                        new ScheduleAssignment("daria", "sat", "training", List.of(slot("16:00", "01:00")), ""),
                        new ScheduleAssignment("daria", "sun", "request", List.of(slot("12:00", "16:00"), slot("17:00", "00:00")), ""),
                        new ScheduleAssignment("violetta", "tue", "request", List.of(slot("09:00", "17:00")), ""),
                        new ScheduleAssignment("violetta", "wed", "request", List.of(slot("09:00", "18:00")), ""),
                        new ScheduleAssignment("violetta", "fri", "standby", List.of(slot("16:00", "22:00")), ""),
                        new ScheduleAssignment("violetta", "sat", "request", List.of(slot("09:00", "17:00")), ""),
                        new ScheduleAssignment("peter", "fri", "standby", List.of(slot("16:00", "01:00")), ""),
                        new ScheduleAssignment("peter", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                        new ScheduleAssignment("dmytro", "mon", "request", List.of(slot("17:00", "00:00")), ""),
                        new ScheduleAssignment("dmytro", "sat", "training", List.of(slot("12:00", "16:00"), slot("17:00", "01:00")), ""),
                        new ScheduleAssignment("dmytro", "sun", "request", List.of(slot("12:00", "16:00"), slot("17:00", "00:00")), ""),
                        new ScheduleAssignment("daniel", "tue", "request", List.of(slot("17:00", "00:00")), ""),
                        new ScheduleAssignment("daniel", "wed", "request", List.of(slot("17:00", "00:00")), ""),
                        new ScheduleAssignment("daniel", "thu", "standby", List.of(slot("17:00", "00:00")), ""),
                        new ScheduleAssignment("daniel", "fri", "standby", List.of(slot("17:00", "01:00")), ""),
                        new ScheduleAssignment("daniel", "sun", "request", List.of(slot("12:00", "16:00"), slot("17:00", "00:00")), "")),
                List.of(
                        "Add or remove people by editing only the people and assignments lists.",
                        "Category colours and labels are driven entirely from the shared category catalog.")
        );
    }

    private static ModuleSummary summary(String moduleName, String blockSummary) {
        ModuleSummary summary = new ModuleSummary();
        summary.setModuleName(moduleName);
        summary.setBlockSummary(blockSummary);
        return summary;
    }

    private static ModuleYml module(String name, List<String> points) {
        ModuleYml module = new ModuleYml();
        module.setName(name);
        module.setModulePoints(points);
        return module;
    }

    private static EmailYaml email(String to, String subject, String body, String displayText) {
        EmailYaml email = new EmailYaml();
        email.setTo(to);
        email.setSubject(subject);
        email.setBody(body);
        email.setDisplayText(displayText);
        return email;
    }

    private static LinkYml link(String url, String displayText) {
        LinkYml link = new LinkYml();
        link.setLinkUrl(new LinkUrl(url));
        link.setDisplayText(displayText);
        return link;
    }

    private static ScheduleSlot slot(String start, String end) {
        return new ScheduleSlot(start, end);
    }
}
