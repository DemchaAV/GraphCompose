package com.demcha.mock;

import com.demcha.Templatese.data.EmailYaml;
import com.demcha.Templatese.data.Header;
import com.demcha.Templatese.data.LinkYml;
import com.demcha.Templatese.data.MainPageCV;
import com.demcha.Templatese.data.ModuleSummary;
import com.demcha.Templatese.data.ModuleYml;
import com.demcha.compose.loyaut_core.components.content.link.LinkUrl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MainPageCVMock {
    private static final Logger log = LoggerFactory.getLogger(MainPageCVMock.class);
    private static final Path RAW_CV_PATH = Path.of("src", "test", "resources", "data", "Artem Demchyshyn-raw.json");

    private final MainPageCV mainPageCV;

    public MainPageCVMock() {
        this.mainPageCV = init();
    }

    public MainPageCV getMainPageCV() {
        return mainPageCV;
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

    private MainPageCV init() {
        try {
            JsonNode root = new ObjectMapper().readTree(RAW_CV_PATH.toFile());
            return mapRawJson(root);
        } catch (IOException e) {
            log.error("Failed to load {}", RAW_CV_PATH, e);
            throw new RuntimeException(e);
        }
    }

    private MainPageCV mapRawJson(JsonNode root) {
        MainPageCV cv = new MainPageCV();
        cv.setHeader(mapHeader(root.path("header")));

        JsonNode modules = root.path("modules");

        ModuleSummary summary = new ModuleSummary();
        JsonNode summaryNode = findModule(modules, "summary");
        summary.setModuleName(text(summaryNode, "name"));
        summary.setBlockSummary(text(summaryNode, "content"));
        cv.setModuleSummary(summary);

        cv.setTechnicalSkills(mapListModule(findModule(modules, "Technical Skills")));
        cv.setEducationCertifications(mapListModule(findModule(modules, "Education & Certifications")));
        cv.setProjects(mapListModule(findModule(modules, "Projects")));
        cv.setProfessionalExperience(mapListModule(findModule(modules, "Professional Experience")));
        cv.setAdditional(mapListModule(findModule(modules, "Additional Information")));
        return cv;
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

    private ModuleYml mapListModule(JsonNode moduleNode) {
        ModuleYml module = new ModuleYml();
        module.setName(text(moduleNode, "name"));
        module.setModulePoints(readItems(moduleNode.path("items")));
        return module;
    }

    private List<String> readItems(JsonNode itemsNode) {
        List<String> items = new ArrayList<>();
        if (itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                items.add(item.asText(""));
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
