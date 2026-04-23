package com.demcha.compose.document.templates.data.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonTemplateDataBuilderTest {

    @Test
    void emailAndLinkFactoriesShouldCreateBuilderFriendlyContactValues() {
        EmailYaml email = EmailYaml.builder()
                .to("artem@demo.dev")
                .subject("Hello")
                .body("Compose-first")
                .displayText("artem@demo.dev")
                .build();
        EmailYaml shorthand = EmailYaml.mailto("billing@graphcompose.dev", "billing@graphcompose.dev");
        LinkYml linkedIn = LinkYml.of("https://linkedin.com/in/graphcompose", "LinkedIn");

        assertThat(email.getTo()).isEqualTo("artem@demo.dev");
        assertThat(email.getSubject()).isEqualTo("Hello");
        assertThat(email.getBody()).isEqualTo("Compose-first");
        assertThat(email.getDisplayText()).isEqualTo("artem@demo.dev");

        assertThat(shorthand.getTo()).isEqualTo("billing@graphcompose.dev");
        assertThat(shorthand.getDisplayText()).isEqualTo("billing@graphcompose.dev");

        assertThat(linkedIn.getLinkUrl()).isNotNull();
        assertThat(linkedIn.getLinkUrl().getUrl()).isEqualTo("https://linkedin.com/in/graphcompose");
        assertThat(linkedIn.getDisplayText()).isEqualTo("LinkedIn");
    }

    @Test
    void headerBuilderShouldAssembleContactRowWithoutManualMutableSetup() {
        Header header = Header.builder()
                .name("Artem Demchyshyn")
                .address("London, UK")
                .phoneNumber("+44 20 5555 1000")
                .email("artem@demo.dev", "artem@demo.dev")
                .linkedIn("https://linkedin.com/in/graphcompose", "LinkedIn")
                .gitHub("https://github.com/DemchaAV", "GitHub")
                .build();

        assertThat(header.getName()).isEqualTo("Artem Demchyshyn");
        assertThat(header.getAddress()).isEqualTo("London, UK");
        assertThat(header.getPhoneNumber()).isEqualTo("+44 20 5555 1000");
        assertThat(header.getEmail()).isNotNull();
        assertThat(header.getLinkedIn()).isNotNull();
        assertThat(header.getGitHub()).isNotNull();
    }
}
