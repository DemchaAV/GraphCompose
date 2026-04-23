package com.demcha.mock;

import com.demcha.compose.document.templates.data.common.EmailYaml;
import com.demcha.compose.document.templates.data.common.Header;
import com.demcha.compose.document.templates.data.common.LinkYml;
import com.demcha.compose.document.templates.data.cv.CvDocumentSpec;
import com.demcha.compose.document.templates.data.cv.CvModule;
import com.demcha.compose.layout_core.components.content.link.LinkUrl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CvDocumentSpecMock {
    private static final Logger log = LoggerFactory.getLogger(CvDocumentSpecMock.class);
    private static final Path RAW_CV_PATH = Path.of("src", "test", "resources", "data", "raw_cv_test.json");

    private final CvDocumentSpec cv;

    public CvDocumentSpecMock() {
        this.cv = init();
    }

    public CvDocumentSpec getCv() {
        return cv;
    }

    public static Header createHeader() {
        Header header = new Header();
        header.setName("Artem Demchyshyn");
        header.setAddress("Kyiv, Ukraine");
        header.setPhoneNumber("+380991234567");

        EmailYaml email = new EmailYaml();
        email.setTo("artem@example.com");
        email.setSubject("Job Application");
        email.setBody("Hello");
        email.setDisplayText("artem@example.com");
        header.setEmail(email);

        LinkYml linkedIn = new LinkYml();
        linkedIn.setLinkUrl(new LinkUrl("https://linkedin.com/in/artem"));
        linkedIn.setDisplayText("LinkedIn");
        header.setLinkedIn(linkedIn);

        LinkYml gitHub = new LinkYml();
        gitHub.setLinkUrl(new LinkUrl("https://github.com/artem"));
        gitHub.setDisplayText("GitHub");
        header.setGitHub(gitHub);

        return header;
    }

    private CvDocumentSpec init() {
        try {
            JsonNode root = new ObjectMapper().readTree(RAW_CV_PATH.toFile());
            return mapRawJson(root);
        } catch (IOException e) {
            log.error("Failed to load {}", RAW_CV_PATH, e);
            throw new RuntimeException(e);
        }
    }

    private CvDocumentSpec mapRawJson(JsonNode root) {
        JsonNode modules = root.path("modules");
        CvDocumentSpec.Builder builder = CvDocumentSpec.builder()
                .header(mapHeader(root.path("header")));

        JsonNode summary = findModule(modules, "summary");
        builder.addModule(CvModule.builder(text(summary, "name"))
                .name("Summary")
                .paragraph(text(summary, "content"))
                .build());
        addRows(builder, findModule(modules, "Technical Skills"), true);
        addRows(builder, findModule(modules, "Education & Certifications"), false);
        addRows(builder, findModule(modules, "Projects"), false);
        addRows(builder, findModule(modules, "Professional Experience"), false);
        addRows(builder, findModule(modules, "Additional Information"), false);
        return builder.build();
    }

    private void addRows(CvDocumentSpec.Builder builder, JsonNode moduleNode, boolean bullets) {
        String title = text(moduleNode, "name");
        List<String> items = readItems(moduleNode.path("items"));
        CvModule.Builder module = CvModule.builder(title).name(stableName(title));
        if (bullets) {
            module.list(items, list -> list.bullet());
        } else {
            module.rows(items);
        }
        builder.addModule(module.build());
    }

    private String stableName(String title) {
        return switch (title) {
            case "Technical Skills" -> "TechnicalSkills";
            case "Education & Certifications" -> "Education";
            case "Professional Experience" -> "Experience";
            case "Additional Information" -> "Additional";
            default -> title;
        };
    }

    private Header mapHeader(JsonNode headerNode) {
        Header header = new Header();
        header.setName(text(headerNode, "name"));
        header.setAddress(text(headerNode, "address"));
        header.setPhoneNumber(text(headerNode, "phoneNumber"));

        EmailYaml email = new EmailYaml();
        JsonNode emailNode = headerNode.path("email");
        email.setTo(text(emailNode, "to"));
        email.setSubject(text(emailNode, "subject"));
        email.setBody(text(emailNode, "body"));
        email.setDisplayText(text(emailNode, "displayText"));
        header.setEmail(email);

        header.setGitHub(mapLink(headerNode.path("gitHub")));
        header.setLinkedIn(mapLink(headerNode.path("linkedIn")));

        return header;
    }

    private LinkYml mapLink(JsonNode linkNode) {
        LinkYml link = new LinkYml();
        link.setDisplayText(text(linkNode, "displayText"));
        link.setLinkUrl(new LinkUrl(text(linkNode, "linkUrl")));
        return link;
    }

    private List<String> readItems(JsonNode itemsNode) {
        List<String> items = new ArrayList<>();
        if (itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                String value = item.asText("").trim();
                if (!value.isBlank()) {
                    items.add(value);
                }
            }
        }
        return items;
    }

    private JsonNode findModule(JsonNode modulesNode, String nameOrType) {
        if (modulesNode.isArray()) {
            for (JsonNode module : modulesNode) {
                if (nameOrType.equalsIgnoreCase(text(module, "name"))
                        || nameOrType.equalsIgnoreCase(text(module, "type"))) {
                    return module;
                }
            }
        }
        throw new IllegalArgumentException("Module not found in raw CV JSON: " + nameOrType);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("");
    }
}
