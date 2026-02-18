package com.demcha.mock;

import com.demcha.Templatese.data.EmailYaml;
import com.demcha.Templatese.data.Header;
import com.demcha.Templatese.data.LinkYml;
import com.demcha.Templatese.data.MainPageCV;
import com.demcha.compose.loyaut_core.components.content.link.LinkUrl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class MainPageCVMock {
    private static final Logger log = LoggerFactory.getLogger(MainPageCVMock.class);

    private final MainPageCV mainPageCV;

    public MainPageCVMock() {
        mainPageCV = init();
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
        ObjectMapper mapper = new ObjectMapper();

        var file = Path.of("src", "test", "resources", "data", "original_cv_data_raw.json");

        try {
            return mapper.readValue(file.toFile(), MainPageCV.class);
        } catch (IOException e) {
            log.error("Failed to load original_cv_data_raw.json", e);
            throw new RuntimeException(e);
        }
    }
}
