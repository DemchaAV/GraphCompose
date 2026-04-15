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

    public static LinkYml toCanonical(com.demcha.templates.data.LinkYml legacy) {
        if (legacy == null) {
            return null;
        }
        LinkYml link = new LinkYml();
        link.setLinkUrl(legacy.getLinkUrl());
        link.setDisplayText(legacy.getDisplayText());
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

    public static ModuleSummary toCanonical(com.demcha.templates.data.ModuleSummary legacy) {
        if (legacy == null) {
            return null;
        }
        ModuleSummary summary = new ModuleSummary();
        summary.setModuleName(legacy.getModuleName());
        summary.setBlockSummary(legacy.getBlockSummary());
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

    public static InvoiceLineItem toCanonical(com.demcha.templates.data.InvoiceLineItem legacy) {
        return legacy == null ? null : new InvoiceLineItem(
                legacy.description(),
                legacy.details(),
                legacy.quantity(),
                legacy.unitPrice(),
                legacy.amount());
    }

    public static InvoiceParty toCanonical(com.demcha.templates.data.InvoiceParty legacy) {
        return legacy == null ? null : new InvoiceParty(
                legacy.name(),
                legacy.addressLines(),
                legacy.email(),
                legacy.phone(),
                legacy.taxId());
    }

    public static InvoiceSummaryRow toCanonical(com.demcha.templates.data.InvoiceSummaryRow legacy) {
        return legacy == null ? null : new InvoiceSummaryRow(
                legacy.label(),
                legacy.value(),
                legacy.emphasized());
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

    public static ProposalParty toCanonical(com.demcha.templates.data.ProposalParty legacy) {
        return legacy == null ? null : new ProposalParty(
                legacy.name(),
                legacy.addressLines(),
                legacy.email(),
                legacy.phone(),
                legacy.website());
    }

    public static ProposalPricingRow toCanonical(com.demcha.templates.data.ProposalPricingRow legacy) {
        return legacy == null ? null : new ProposalPricingRow(
                legacy.label(),
                legacy.description(),
                legacy.amount(),
                legacy.emphasized());
    }

    public static ProposalSection toCanonical(com.demcha.templates.data.ProposalSection legacy) {
        return legacy == null ? null : new ProposalSection(
                legacy.title(),
                legacy.paragraphs());
    }

    public static ProposalTimelineItem toCanonical(com.demcha.templates.data.ProposalTimelineItem legacy) {
        return legacy == null ? null : new ProposalTimelineItem(
                legacy.phase(),
                legacy.duration(),
                legacy.details());
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

    public static ScheduleAssignment toCanonical(com.demcha.templates.data.ScheduleAssignment legacy) {
        return legacy == null ? null : new ScheduleAssignment(
                legacy.personId(),
                legacy.dayId(),
                legacy.categoryId(),
                legacy.slots().stream().map(LegacyTemplateMappers::toCanonical).toList(),
                legacy.note());
    }

    public static ScheduleCategory toCanonical(com.demcha.templates.data.ScheduleCategory legacy) {
        return legacy == null ? null : new ScheduleCategory(
                legacy.id(),
                legacy.label(),
                legacy.fillColor(),
                legacy.textColor(),
                legacy.borderColor());
    }

    public static ScheduleDay toCanonical(com.demcha.templates.data.ScheduleDay legacy) {
        return legacy == null ? null : new ScheduleDay(
                legacy.id(),
                legacy.label(),
                legacy.headerNote(),
                legacy.headerCategoryId());
    }

    public static ScheduleMetricRow toCanonical(com.demcha.templates.data.ScheduleMetricRow legacy) {
        return legacy == null ? null : new ScheduleMetricRow(
                legacy.label(),
                legacy.dayValues());
    }

    public static SchedulePerson toCanonical(com.demcha.templates.data.SchedulePerson legacy) {
        return legacy == null ? null : new SchedulePerson(
                legacy.id(),
                legacy.displayName(),
                legacy.sortOrder());
    }

    public static ScheduleSlot toCanonical(com.demcha.templates.data.ScheduleSlot legacy) {
        return legacy == null ? null : new ScheduleSlot(
                legacy.start(),
                legacy.end());
    }
}
