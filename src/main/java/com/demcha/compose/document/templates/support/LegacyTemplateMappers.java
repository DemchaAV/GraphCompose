package com.demcha.compose.document.templates.support;

import com.demcha.compose.document.templates.data.*;
import com.demcha.compose.document.templates.theme.CvTheme;
import com.demcha.compose.document.templates.theme.WeeklyScheduleTheme;

/**
 * Explicit bridge mappers from the deprecated {@code com.demcha.templates.*}
 * namespace into canonical {@code document.templates.*} models.
 */
public final class LegacyTemplateMappers {
    private LegacyTemplateMappers() {
    }

    public static CvTheme toCanonical(com.demcha.templates.CvTheme legacy) {
        return legacy == null ? null : new CvTheme(
                legacy.primaryColor(),
                legacy.secondaryColor(),
                legacy.bodyColor(),
                legacy.accentColor(),
                legacy.headerFont(),
                legacy.bodyFont(),
                legacy.nameFontSize(),
                legacy.headerFontSize(),
                legacy.bodyFontSize(),
                legacy.spacing(),
                legacy.modulMargin(),
                legacy.spacingModuleName());
    }

    public static com.demcha.templates.CvTheme toLegacy(CvTheme canonical) {
        return canonical == null ? null : new com.demcha.templates.CvTheme(
                canonical.primaryColor(),
                canonical.secondaryColor(),
                canonical.bodyColor(),
                canonical.accentColor(),
                canonical.headerFont(),
                canonical.bodyFont(),
                canonical.nameFontSize(),
                canonical.headerFontSize(),
                canonical.bodyFontSize(),
                canonical.spacing(),
                canonical.moduleMargin(),
                canonical.spacingModuleName());
    }

    public static WeeklyScheduleTheme toCanonical(com.demcha.templates.WeeklyScheduleTheme legacy) {
        return legacy == null ? null : new WeeklyScheduleTheme(
                legacy.titleColor(),
                legacy.accentColor(),
                legacy.bodyColor(),
                legacy.mutedTextColor(),
                legacy.gridBorderColor(),
                legacy.bandFillColor(),
                legacy.nameColumnFillColor(),
                legacy.emptyCellFillColor(),
                legacy.titleFont(),
                legacy.bodyFont(),
                legacy.titleFontSize(),
                legacy.weekLabelFontSize(),
                legacy.dayLabelFontSize(),
                legacy.noteFontSize(),
                legacy.metricFontSize(),
                legacy.personNameFontSize(),
                legacy.cellFontSize(),
                legacy.footerFontSize(),
                legacy.rootSpacing(),
                legacy.sectionSpacing(),
                legacy.nameColumnWidth(),
                legacy.bandPaddingVertical(),
                legacy.bandPaddingHorizontal(),
                legacy.bodyPaddingVertical(),
                legacy.bodyPaddingHorizontal());
    }

    public static com.demcha.templates.WeeklyScheduleTheme toLegacy(WeeklyScheduleTheme canonical) {
        return canonical == null ? null : new com.demcha.templates.WeeklyScheduleTheme(
                canonical.titleColor(),
                canonical.accentColor(),
                canonical.bodyColor(),
                canonical.mutedTextColor(),
                canonical.gridBorderColor(),
                canonical.bandFillColor(),
                canonical.nameColumnFillColor(),
                canonical.emptyCellFillColor(),
                canonical.titleFont(),
                canonical.bodyFont(),
                canonical.titleFontSize(),
                canonical.weekLabelFontSize(),
                canonical.dayLabelFontSize(),
                canonical.noteFontSize(),
                canonical.metricFontSize(),
                canonical.personNameFontSize(),
                canonical.cellFontSize(),
                canonical.footerFontSize(),
                canonical.rootSpacing(),
                canonical.sectionSpacing(),
                canonical.nameColumnWidth(),
                canonical.bandPaddingVertical(),
                canonical.bandPaddingHorizontal(),
                canonical.bodyPaddingVertical(),
                canonical.bodyPaddingHorizontal());
    }

    public static JobDetails toCanonical(com.demcha.templates.JobDetails legacy) {
        return legacy == null ? null : new JobDetails(
                legacy.url(),
                legacy.title(),
                legacy.company(),
                legacy.location(),
                legacy.description(),
                legacy.seniorityLevel(),
                legacy.employmentType());
    }

    public static com.demcha.templates.JobDetails toLegacy(JobDetails canonical) {
        return canonical == null ? null : new com.demcha.templates.JobDetails(
                canonical.url(),
                canonical.title(),
                canonical.company(),
                canonical.location(),
                canonical.description(),
                canonical.seniorityLevel(),
                canonical.employmentType());
    }

    public static EmailYaml toCanonical(com.demcha.templates.data.EmailYaml legacy) {
        if (legacy == null) {
            return null;
        }
        EmailYaml email = new EmailYaml();
        email.setTo(legacy.getTo());
        email.setSubject(legacy.getSubject());
        email.setBody(legacy.getBody());
        email.setDisplayText(legacy.getDisplayText());
        return email;
    }

    public static com.demcha.templates.data.EmailYaml toLegacy(EmailYaml canonical) {
        if (canonical == null) {
            return null;
        }
        com.demcha.templates.data.EmailYaml email = new com.demcha.templates.data.EmailYaml();
        email.setTo(canonical.getTo());
        email.setSubject(canonical.getSubject());
        email.setBody(canonical.getBody());
        email.setDisplayText(canonical.getDisplayText());
        return email;
    }

    public static LinkYml toCanonical(com.demcha.templates.data.LinkYml legacy) {
        if (legacy == null) {
            return null;
        }
        LinkYml link = new LinkYml();
        link.setLinkUrl(legacy.getLinkUrl());
        link.setDisplayText(legacy.getDisplayText());
        return link;
    }

    public static com.demcha.templates.data.LinkYml toLegacy(LinkYml canonical) {
        if (canonical == null) {
            return null;
        }
        com.demcha.templates.data.LinkYml link = new com.demcha.templates.data.LinkYml();
        link.setLinkUrl(canonical.getLinkUrl());
        link.setDisplayText(canonical.getDisplayText());
        return link;
    }

    public static Header toCanonical(com.demcha.templates.data.Header legacy) {
        if (legacy == null) {
            return null;
        }
        Header header = new Header();
        header.setName(legacy.getName());
        header.setAddress(legacy.getAddress());
        header.setPhoneNumber(legacy.getPhoneNumber());
        header.setEmail(toCanonical(legacy.getEmail()));
        header.setGitHub(toCanonical(legacy.getGitHub()));
        header.setLinkedIn(toCanonical(legacy.getLinkedIn()));
        return header;
    }

    public static com.demcha.templates.data.Header toLegacy(Header canonical) {
        if (canonical == null) {
            return null;
        }
        com.demcha.templates.data.Header header = new com.demcha.templates.data.Header();
        header.setName(canonical.getName());
        header.setAddress(canonical.getAddress());
        header.setPhoneNumber(canonical.getPhoneNumber());
        header.setEmail(toLegacy(canonical.getEmail()));
        header.setGitHub(toLegacy(canonical.getGitHub()));
        header.setLinkedIn(toLegacy(canonical.getLinkedIn()));
        return header;
    }

    public static ModuleSummary toCanonical(com.demcha.templates.data.ModuleSummary legacy) {
        if (legacy == null) {
            return null;
        }
        ModuleSummary summary = new ModuleSummary();
        summary.setModuleName(legacy.getModuleName());
        summary.setBlockSummary(legacy.getBlockSummary());
        return summary;
    }

    public static com.demcha.templates.data.ModuleSummary toLegacy(ModuleSummary canonical) {
        if (canonical == null) {
            return null;
        }
        com.demcha.templates.data.ModuleSummary summary = new com.demcha.templates.data.ModuleSummary();
        summary.setModuleName(canonical.getModuleName());
        summary.setBlockSummary(canonical.getBlockSummary());
        return summary;
    }

    public static ModuleYml toCanonical(com.demcha.templates.data.ModuleYml legacy) {
        if (legacy == null) {
            return null;
        }
        ModuleYml module = new ModuleYml();
        module.setName(legacy.getName());
        module.setModulePoints(legacy.getModulePoints());
        return module;
    }

    public static com.demcha.templates.data.ModuleYml toLegacy(ModuleYml canonical) {
        if (canonical == null) {
            return null;
        }
        com.demcha.templates.data.ModuleYml module = new com.demcha.templates.data.ModuleYml();
        module.setName(canonical.getName());
        module.setModulePoints(canonical.getModulePoints());
        return module;
    }

    public static MainPageCV toCanonical(com.demcha.templates.data.MainPageCV legacy) {
        if (legacy == null) {
            return null;
        }
        MainPageCV cv = new MainPageCV();
        cv.setHeader(toCanonical(legacy.getHeader()));
        cv.setModuleSummary(toCanonical(legacy.getModuleSummary()));
        cv.setTechnicalSkills(toCanonical(legacy.getTechnicalSkills()));
        cv.setEducationCertifications(toCanonical(legacy.getEducationCertifications()));
        cv.setProjects(toCanonical(legacy.getProjects()));
        cv.setProfessionalExperience(toCanonical(legacy.getProfessionalExperience()));
        cv.setAdditional(toCanonical(legacy.getAdditional()));
        return cv;
    }

    public static com.demcha.templates.data.MainPageCV toLegacy(MainPageCV canonical) {
        if (canonical == null) {
            return null;
        }
        com.demcha.templates.data.MainPageCV cv = new com.demcha.templates.data.MainPageCV();
        cv.setHeader(toLegacy(canonical.getHeader()));
        cv.setModuleSummary(toLegacy(canonical.getModuleSummary()));
        cv.setTechnicalSkills(toLegacy(canonical.getTechnicalSkills()));
        cv.setEducationCertifications(toLegacy(canonical.getEducationCertifications()));
        cv.setProjects(toLegacy(canonical.getProjects()));
        cv.setProfessionalExperience(toLegacy(canonical.getProfessionalExperience()));
        cv.setAdditional(toLegacy(canonical.getAdditional()));
        return cv;
    }

    public static MainPageCvDTO toCanonical(com.demcha.templates.api.MainPageCvDTO legacy) {
        if (legacy == null) {
            return null;
        }
        MainPageCvDTO dto = new MainPageCvDTO();
        dto.setHeader(toCanonical(legacy.getHeader()));
        dto.setModuleSummary(toCanonical(legacy.getModuleSummary()));
        dto.setTechnicalSkills(toCanonical(legacy.getTechnicalSkills()));
        dto.setEducationCertifications(toCanonical(legacy.getEducationCertifications()));
        dto.setProjects(toCanonical(legacy.getProjects()));
        dto.setProfessionalExperience(toCanonical(legacy.getProfessionalExperience()));
        dto.setAdditional(toCanonical(legacy.getAdditional()));
        return dto;
    }

    public static com.demcha.templates.api.MainPageCvDTO toLegacy(MainPageCvDTO canonical) {
        if (canonical == null) {
            return null;
        }
        com.demcha.templates.api.MainPageCvDTO dto = new com.demcha.templates.api.MainPageCvDTO();
        dto.setHeader(toLegacy(canonical.getHeader()));
        dto.setModuleSummary(toLegacy(canonical.getModuleSummary()));
        dto.setTechnicalSkills(toLegacy(canonical.getTechnicalSkills()));
        dto.setEducationCertifications(toLegacy(canonical.getEducationCertifications()));
        dto.setProjects(toLegacy(canonical.getProjects()));
        dto.setProfessionalExperience(toLegacy(canonical.getProfessionalExperience()));
        dto.setAdditional(toLegacy(canonical.getAdditional()));
        return dto;
    }

    public static InvoiceData toCanonical(com.demcha.templates.data.InvoiceData legacy) {
        return legacy == null ? null : new InvoiceData(
                legacy.title(),
                legacy.invoiceNumber(),
                legacy.issueDate(),
                legacy.dueDate(),
                legacy.reference(),
                legacy.status(),
                toCanonical(legacy.fromParty()),
                toCanonical(legacy.billToParty()),
                legacy.lineItems().stream().map(LegacyTemplateMappers::toCanonical).toList(),
                legacy.summaryRows().stream().map(LegacyTemplateMappers::toCanonical).toList(),
                legacy.notes(),
                legacy.paymentTerms(),
                legacy.footerNote());
    }

    public static com.demcha.templates.data.InvoiceData toLegacy(InvoiceData canonical) {
        return canonical == null ? null : new com.demcha.templates.data.InvoiceData(
                canonical.title(),
                canonical.invoiceNumber(),
                canonical.issueDate(),
                canonical.dueDate(),
                canonical.reference(),
                canonical.status(),
                toLegacy(canonical.fromParty()),
                toLegacy(canonical.billToParty()),
                canonical.lineItems().stream().map(LegacyTemplateMappers::toLegacy).toList(),
                canonical.summaryRows().stream().map(LegacyTemplateMappers::toLegacy).toList(),
                canonical.notes(),
                canonical.paymentTerms(),
                canonical.footerNote());
    }

    public static InvoiceLineItem toCanonical(com.demcha.templates.data.InvoiceLineItem legacy) {
        return legacy == null ? null : new InvoiceLineItem(
                legacy.description(),
                legacy.details(),
                legacy.quantity(),
                legacy.unitPrice(),
                legacy.amount());
    }

    public static com.demcha.templates.data.InvoiceLineItem toLegacy(InvoiceLineItem canonical) {
        return canonical == null ? null : new com.demcha.templates.data.InvoiceLineItem(
                canonical.description(),
                canonical.details(),
                canonical.quantity(),
                canonical.unitPrice(),
                canonical.amount());
    }

    public static InvoiceParty toCanonical(com.demcha.templates.data.InvoiceParty legacy) {
        return legacy == null ? null : new InvoiceParty(
                legacy.name(),
                legacy.addressLines(),
                legacy.email(),
                legacy.phone(),
                legacy.taxId());
    }

    public static com.demcha.templates.data.InvoiceParty toLegacy(InvoiceParty canonical) {
        return canonical == null ? null : new com.demcha.templates.data.InvoiceParty(
                canonical.name(),
                canonical.addressLines(),
                canonical.email(),
                canonical.phone(),
                canonical.taxId());
    }

    public static InvoiceSummaryRow toCanonical(com.demcha.templates.data.InvoiceSummaryRow legacy) {
        return legacy == null ? null : new InvoiceSummaryRow(
                legacy.label(),
                legacy.value(),
                legacy.emphasized());
    }

    public static com.demcha.templates.data.InvoiceSummaryRow toLegacy(InvoiceSummaryRow canonical) {
        return canonical == null ? null : new com.demcha.templates.data.InvoiceSummaryRow(
                canonical.label(),
                canonical.value(),
                canonical.emphasized());
    }

    public static ProposalData toCanonical(com.demcha.templates.data.ProposalData legacy) {
        return legacy == null ? null : new ProposalData(
                legacy.title(),
                legacy.proposalNumber(),
                legacy.preparedDate(),
                legacy.validUntil(),
                legacy.projectTitle(),
                legacy.executiveSummary(),
                toCanonical(legacy.sender()),
                toCanonical(legacy.recipient()),
                legacy.sections().stream().map(LegacyTemplateMappers::toCanonical).toList(),
                legacy.timeline().stream().map(LegacyTemplateMappers::toCanonical).toList(),
                legacy.pricingRows().stream().map(LegacyTemplateMappers::toCanonical).toList(),
                legacy.acceptanceTerms(),
                legacy.footerNote());
    }

    public static com.demcha.templates.data.ProposalData toLegacy(ProposalData canonical) {
        return canonical == null ? null : new com.demcha.templates.data.ProposalData(
                canonical.title(),
                canonical.proposalNumber(),
                canonical.preparedDate(),
                canonical.validUntil(),
                canonical.projectTitle(),
                canonical.executiveSummary(),
                toLegacy(canonical.sender()),
                toLegacy(canonical.recipient()),
                canonical.sections().stream().map(LegacyTemplateMappers::toLegacy).toList(),
                canonical.timeline().stream().map(LegacyTemplateMappers::toLegacy).toList(),
                canonical.pricingRows().stream().map(LegacyTemplateMappers::toLegacy).toList(),
                canonical.acceptanceTerms(),
                canonical.footerNote());
    }

    public static ProposalParty toCanonical(com.demcha.templates.data.ProposalParty legacy) {
        return legacy == null ? null : new ProposalParty(
                legacy.name(),
                legacy.addressLines(),
                legacy.email(),
                legacy.phone(),
                legacy.website());
    }

    public static com.demcha.templates.data.ProposalParty toLegacy(ProposalParty canonical) {
        return canonical == null ? null : new com.demcha.templates.data.ProposalParty(
                canonical.name(),
                canonical.addressLines(),
                canonical.email(),
                canonical.phone(),
                canonical.website());
    }

    public static ProposalPricingRow toCanonical(com.demcha.templates.data.ProposalPricingRow legacy) {
        return legacy == null ? null : new ProposalPricingRow(
                legacy.label(),
                legacy.description(),
                legacy.amount(),
                legacy.emphasized());
    }

    public static com.demcha.templates.data.ProposalPricingRow toLegacy(ProposalPricingRow canonical) {
        return canonical == null ? null : new com.demcha.templates.data.ProposalPricingRow(
                canonical.label(),
                canonical.description(),
                canonical.amount(),
                canonical.emphasized());
    }

    public static ProposalSection toCanonical(com.demcha.templates.data.ProposalSection legacy) {
        return legacy == null ? null : new ProposalSection(
                legacy.title(),
                legacy.paragraphs());
    }

    public static com.demcha.templates.data.ProposalSection toLegacy(ProposalSection canonical) {
        return canonical == null ? null : new com.demcha.templates.data.ProposalSection(
                canonical.title(),
                canonical.paragraphs());
    }

    public static ProposalTimelineItem toCanonical(com.demcha.templates.data.ProposalTimelineItem legacy) {
        return legacy == null ? null : new ProposalTimelineItem(
                legacy.phase(),
                legacy.duration(),
                legacy.details());
    }

    public static com.demcha.templates.data.ProposalTimelineItem toLegacy(ProposalTimelineItem canonical) {
        return canonical == null ? null : new com.demcha.templates.data.ProposalTimelineItem(
                canonical.phase(),
                canonical.duration(),
                canonical.details());
    }

    public static WeeklyScheduleData toCanonical(com.demcha.templates.data.WeeklyScheduleData legacy) {
        return legacy == null ? null : new WeeklyScheduleData(
                legacy.title(),
                legacy.weekLabel(),
                legacy.days().stream().map(LegacyTemplateMappers::toCanonical).toList(),
                legacy.categories().stream().map(LegacyTemplateMappers::toCanonical).toList(),
                legacy.headerMetrics().stream().map(LegacyTemplateMappers::toCanonical).toList(),
                legacy.people().stream().map(LegacyTemplateMappers::toCanonical).toList(),
                legacy.assignments().stream().map(LegacyTemplateMappers::toCanonical).toList(),
                legacy.footerNotes());
    }

    public static com.demcha.templates.data.WeeklyScheduleData toLegacy(WeeklyScheduleData canonical) {
        return canonical == null ? null : new com.demcha.templates.data.WeeklyScheduleData(
                canonical.title(),
                canonical.weekLabel(),
                canonical.days().stream().map(LegacyTemplateMappers::toLegacy).toList(),
                canonical.categories().stream().map(LegacyTemplateMappers::toLegacy).toList(),
                canonical.headerMetrics().stream().map(LegacyTemplateMappers::toLegacy).toList(),
                canonical.people().stream().map(LegacyTemplateMappers::toLegacy).toList(),
                canonical.assignments().stream().map(LegacyTemplateMappers::toLegacy).toList(),
                canonical.footerNotes());
    }

    public static ScheduleAssignment toCanonical(com.demcha.templates.data.ScheduleAssignment legacy) {
        return legacy == null ? null : new ScheduleAssignment(
                legacy.personId(),
                legacy.dayId(),
                legacy.categoryId(),
                legacy.slots().stream().map(LegacyTemplateMappers::toCanonical).toList(),
                legacy.note());
    }

    public static com.demcha.templates.data.ScheduleAssignment toLegacy(ScheduleAssignment canonical) {
        return canonical == null ? null : new com.demcha.templates.data.ScheduleAssignment(
                canonical.personId(),
                canonical.dayId(),
                canonical.categoryId(),
                canonical.slots().stream().map(LegacyTemplateMappers::toLegacy).toList(),
                canonical.note());
    }

    public static ScheduleCategory toCanonical(com.demcha.templates.data.ScheduleCategory legacy) {
        return legacy == null ? null : new ScheduleCategory(
                legacy.id(),
                legacy.label(),
                legacy.fillColor(),
                legacy.textColor(),
                legacy.borderColor());
    }

    public static com.demcha.templates.data.ScheduleCategory toLegacy(ScheduleCategory canonical) {
        return canonical == null ? null : new com.demcha.templates.data.ScheduleCategory(
                canonical.id(),
                canonical.label(),
                canonical.fillColor(),
                canonical.textColor(),
                canonical.borderColor());
    }

    public static ScheduleDay toCanonical(com.demcha.templates.data.ScheduleDay legacy) {
        return legacy == null ? null : new ScheduleDay(
                legacy.id(),
                legacy.label(),
                legacy.headerNote(),
                legacy.headerCategoryId());
    }

    public static com.demcha.templates.data.ScheduleDay toLegacy(ScheduleDay canonical) {
        return canonical == null ? null : new com.demcha.templates.data.ScheduleDay(
                canonical.id(),
                canonical.label(),
                canonical.headerNote(),
                canonical.headerCategoryId());
    }

    public static ScheduleMetricRow toCanonical(com.demcha.templates.data.ScheduleMetricRow legacy) {
        return legacy == null ? null : new ScheduleMetricRow(
                legacy.label(),
                legacy.dayValues());
    }

    public static com.demcha.templates.data.ScheduleMetricRow toLegacy(ScheduleMetricRow canonical) {
        return canonical == null ? null : new com.demcha.templates.data.ScheduleMetricRow(
                canonical.label(),
                canonical.dayValues());
    }

    public static SchedulePerson toCanonical(com.demcha.templates.data.SchedulePerson legacy) {
        return legacy == null ? null : new SchedulePerson(
                legacy.id(),
                legacy.displayName(),
                legacy.sortOrder());
    }

    public static com.demcha.templates.data.SchedulePerson toLegacy(SchedulePerson canonical) {
        return canonical == null ? null : new com.demcha.templates.data.SchedulePerson(
                canonical.id(),
                canonical.displayName(),
                canonical.sortOrder());
    }

    public static ScheduleSlot toCanonical(com.demcha.templates.data.ScheduleSlot legacy) {
        return legacy == null ? null : new ScheduleSlot(
                legacy.start(),
                legacy.end());
    }

    public static com.demcha.templates.data.ScheduleSlot toLegacy(ScheduleSlot canonical) {
        return canonical == null ? null : new com.demcha.templates.data.ScheduleSlot(
                canonical.start(),
                canonical.end());
    }
}
