package com.demcha.testing.fixtures;

import com.demcha.compose.document.templates.data.common.EmailYaml;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.common.LinkYml;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.data.cv.CvModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared CV test fixtures used across snapshot and render tests.
 */
public final class CvTestFixtures {

    private CvTestFixtures() {
    }

    public static CvDocumentSpec createExpandedCvForOneAndHalfPages(CvDocumentSpec base) {
        CvDocumentSpec.Builder expanded = CvDocumentSpec.builder().header(copyHeader(base.header()));
        for (CvModule module : base.modules()) {
            expanded.addModule(expandModule(module));
        }
        return expanded.build();
    }

    public static List<String> lines(CvDocumentSpec cv, String moduleTitle) {
        return cv.modules().stream()
                .filter(module -> matches(module, moduleTitle))
                .findFirst()
                .map(CvTestFixtures::lines)
                .orElse(List.of());
    }

    private static CvModule expandModule(CvModule module) {
        String key = key(module.title());
        if (key.contains("professionalsummary")) {
            return CvModule.builder(module.title())
                    .name(module.name())
                    .paragraph(String.join(" ",
                            String.join(" ", lines(module)),
                            "Focused on platform engineering, document generation, backend integration, and resilient delivery for teams that need both speed and maintainability.",
                            "Comfortable translating ambiguous business needs into structured implementation plans, reusable libraries, and production-ready developer workflows.",
                            "Strong at decomposing large initiatives into reviewable milestones, mentoring contributors, and improving code health while continuing to ship value."))
                    .build();
        }
        if (key.contains("technicalskills")) {
            return copyListModuleWithExtraPoints(module, List.of(
                    "Java 21, Spring Boot, PDFBox, layout engines, rendering pipelines, and template abstractions for document generation at scale.",
                    "Testing strategy across unit, integration, render, and visual regression checks with strong focus on layout stability and pagination behaviour.",
                    "Architecture work covering API design, modularization, refactoring plans, performance tuning, and reusable design tokens for templates."));
        }
        if (key.contains("education")) {
            return copyListModuleWithExtraPoints(module, List.of(
                    "Advanced coursework in distributed systems, document automation, developer tooling, and software architecture communication.",
                    "Continuous self-study in typography for programmatic documents, PDF internals, content measurement, and layout debugging techniques."));
        }
        if (key.contains("projects")) {
            return copyListModuleWithExtraPoints(module, List.of(
                    "**GraphCompose Template Extensions** | Built reusable CV, proposal, invoice, and cover-letter template layers with markdown-aware block text, theme support, and render validation.",
                    "**Document Preview Tooling** | Added local preview workflows, scale resolution helpers, and visual fixtures that shorten iteration time for layout-heavy documents.",
                    "**Developer Experience Improvements** | Reduced friction around sample data, test outputs, and template exploration so contributors can validate changes quickly."));
        }
        if (key.contains("professionalexperience")) {
            return copyListModuleWithExtraPoints(module, List.of(
                    "**Senior Backend Engineer** | Led a multi-quarter modernization effort for internal services, introduced clearer service boundaries, and documented rollout plans for critical migrations.",
                    "**Platform Engineer** | Worked closely with product and operations to stabilize releases, improve observability, and simplify deployment pipelines for cross-team tools.",
                    "**Technical Lead**| Mentored engineers through design reviews, introduced stronger testing expectations, and maintained delivery momentum during periods of changing requirements.",
                    "**Consulting Engineer** | Delivered bespoke automation and reporting features, often combining backend APIs, file generation, and presentation-friendly exports for client teams."));
        }
        if (key.contains("additionalinformation")) {
            return copyListModuleWithExtraPoints(module, List.of(
                    "**Languages:** Ukrainian (native), English (professional working proficiency).",
                    "**Open-source interests:** document tooling, rendering systems, developer productivity, and maintainable test suites.",
                    "**Working style:** calm under ambiguity, highly collaborative, and strongly biased toward clear interfaces and observable behaviour."));
        }
        return module;
    }

    private static Header copyHeader(Header source) {
        Header header = new Header();
        header.setName(source.getName());
        header.setAddress(source.getAddress());
        header.setPhoneNumber(source.getPhoneNumber());
        header.setEmail(copyEmail(source.getEmail()));
        header.setGitHub(copyLink(source.getGitHub()));
        header.setLinkedIn(copyLink(source.getLinkedIn()));
        return header;
    }

    private static EmailYaml copyEmail(EmailYaml source) {
        EmailYaml email = new EmailYaml();
        email.setTo(source.getTo());
        email.setSubject(source.getSubject());
        email.setBody(source.getBody());
        email.setDisplayText(source.getDisplayText());
        return email;
    }

    private static LinkYml copyLink(LinkYml source) {
        LinkYml link = new LinkYml();
        link.setDisplayText(source.getDisplayText());
        link.setLinkUrl(source.getLinkUrl());
        return link;
    }

    private static CvModule copyListModuleWithExtraPoints(CvModule source, List<String> extraPoints) {
        List<String> points = new ArrayList<>(lines(source));
        points.addAll(extraPoints);
        CvModule.BodyBlock firstList = source.bodyBlocks().stream()
                .filter(block -> block.kind() == CvModule.BodyKind.LIST)
                .findFirst()
                .orElse(null);

        CvModule.Builder builder = CvModule.builder(source.title()).name(source.name());
        if (firstList != null && firstList.marker().isVisible()) {
            builder.list(points, list -> list
                    .marker(firstList.marker())
                    .continuationIndent(firstList.continuationIndent())
                    .normalizeMarkers(firstList.normalizeMarkers()));
        } else {
            builder.rows(points);
        }
        return builder.build();
    }

    private static List<String> lines(CvModule module) {
        List<String> lines = new ArrayList<>();
        for (CvModule.BodyBlock block : module.bodyBlocks()) {
            switch (block.kind()) {
                case PARAGRAPH -> lines.add(block.text());
                case LIST -> lines.addAll(block.items());
                default -> {
                }
            }
        }
        return lines.stream()
                .filter(line -> line != null && !line.isBlank())
                .toList();
    }

    private static boolean matches(CvModule module, String moduleTitle) {
        String moduleKey = key(module.name() + " " + module.title());
        return moduleKey.contains(key(moduleTitle));
    }

    private static String key(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}
