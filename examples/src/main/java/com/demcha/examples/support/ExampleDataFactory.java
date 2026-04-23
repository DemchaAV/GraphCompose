package com.demcha.examples.support;

import com.demcha.compose.document.templates.data.common.EmailYaml;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.invoice.InvoiceData;
import com.demcha.compose.document.templates.data.coverletter.JobDetails;
import com.demcha.compose.document.templates.data.cv.MainPageCV;
import com.demcha.compose.document.templates.data.cv.MainPageCvDTO;
import com.demcha.compose.document.templates.data.cv.ModuleSummary;
import com.demcha.compose.document.templates.data.cv.ModuleYml;
import com.demcha.compose.document.templates.data.proposal.ProposalData;
import com.demcha.compose.document.templates.data.schedule.ScheduleAssignment;
import com.demcha.compose.document.templates.data.schedule.ScheduleCategory;
import com.demcha.compose.document.templates.data.schedule.ScheduleDay;
import com.demcha.compose.document.templates.data.schedule.ScheduleMetricRow;
import com.demcha.compose.document.templates.data.schedule.SchedulePerson;
import com.demcha.compose.document.templates.data.schedule.ScheduleSlot;
import com.demcha.compose.document.templates.data.schedule.WeeklyScheduleData;

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
        return Header.builder()
                .name("Artem Demchyshyn")
                .address("London, UK")
                .phoneNumber("+44 20 5555 1000")
                .email(EmailYaml.builder()
                        .to("artem@demo.dev")
                        .subject("Job Application")
                        .body("Hello")
                        .displayText("artem@demo.dev")
                        .build())
                .linkedIn("https://linkedin.com/in/graphcompose", "LinkedIn")
                .gitHub("https://github.com/DemchaAV", "GitHub")
                .build();
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
        return JobDetails.builder()
                .url("https://northwind.example/jobs/platform")
                .title("Senior Platform Engineer")
                .company("Northwind Systems")
                .location("London / Remote")
                .description("Lead reusable internal platform capabilities.")
                .seniorityLevel("Senior")
                .employmentType("Full-time")
                .build();
    }

    public static InvoiceData sampleInvoice() {
        return InvoiceData.builder()
                .title("Invoice")
                .invoiceNumber("GC-2026-041")
                .issueDate("02 Apr 2026")
                .dueDate("16 Apr 2026")
                .reference("Examples Module Delivery")
                .status("Pending")
                .fromParty(party -> party
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK", "EC1A 4GC")
                        .email("billing@graphcompose.dev")
                        .phone("+44 20 5555 1000")
                        .taxId("GB-99887766"))
                .billToParty(party -> party
                        .name("Northwind Systems")
                        .addressLines("Accounts Payable", "410 Market Avenue", "Manchester, UK")
                        .email("ap@northwind.example")
                        .phone("+44 161 555 2200")
                        .taxId("NW-2026-01"))
                .lineItem("Invoice template delivery", "Built-in template, styling, and metadata layout", "1", "GBP 1,950", "GBP 1,950")
                .lineItem("Proposal template delivery", "Long-form content flow and pricing table", "1", "GBP 2,150", "GBP 2,150")
                .lineItem("Examples module", "Runnable file-render examples and usage notes", "1", "GBP 1,200", "GBP 1,200")
                .summaryRow("Subtotal", "GBP 5,300")
                .summaryRow("VAT (20%)", "GBP 1,060")
                .totalRow("Total", "GBP 6,360")
                .note("This invoice covers the first delivery package for GraphCompose business templates.")
                .note("Please reference the invoice number in any remittance message.")
                .paymentTerm("Payment due within 14 calendar days.")
                .paymentTerm("Bank transfer preferred.")
                .paymentTerm("Contact billing@graphcompose.dev for remittance instructions.")
                .footerNote("Generated by the standalone GraphCompose examples module.")
                .build();
    }

    public static ProposalData sampleProposal() {
        return ProposalData.builder()
                .title("Proposal")
                .proposalNumber("PROP-2026-014")
                .preparedDate("02 Apr 2026")
                .validUntil("16 Apr 2026")
                .projectTitle("GraphCompose rollout for internal document operations")
                .executiveSummary("This proposal describes a practical adoption path for reusable GraphCompose templates, render tests, and runnable examples across billing, hiring, and client-facing delivery workflows.")
                .sender(party -> party
                        .name("GraphCompose Studio")
                        .addressLines("18 Layout Street", "London, UK", "EC1A 4GC")
                        .email("hello@graphcompose.dev")
                        .phone("+44 20 5555 1000")
                        .website("graphcompose.dev"))
                .recipient(party -> party
                        .name("Northwind Systems")
                        .addressLines("Product Engineering", "410 Market Avenue", "Manchester, UK")
                        .email("platform@northwind.example")
                        .phone("+44 161 555 2200")
                        .website("northwind.example"))
                .section("Scope",
                        "Introduce built-in invoice and proposal templates with a consistent business presentation layer.",
                        "Keep the production artifact clean by moving development-only preview code out of the published runtime scope.")
                .section("Deliverables",
                        "Public DTOs and template interfaces for invoice and proposal rendering.",
                        "Render tests and a standalone examples module that generates PDF files on demand.")
                .timelineItem("Week 1", "5 days", "Invoice API and first template delivery.")
                .timelineItem("Week 2", "5 days", "Proposal layout, review loop, and render tests.")
                .timelineItem("Week 3", "3 days", "Examples module and README handoff.")
                .pricingRow("Foundation", "Template APIs and DTO modeling", "GBP 3,200")
                .pricingRow("Document delivery", "Invoice and proposal templates with tests", "GBP 4,450")
                .emphasizedPricingRow("Total investment", "Fixed-price project delivery", "GBP 9,500")
                .acceptanceTerm("Proposal pricing is valid until the stated expiration date.")
                .acceptanceTerm("Additional template families can be scoped in a separate phase.")
                .footerNote("Prepared to demonstrate the business-document side of GraphCompose.")
                .build();
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

    private static ScheduleSlot slot(String start, String end) {
        return new ScheduleSlot(start, end);
    }
}
