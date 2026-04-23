package com.demcha.compose.document.templates.data.coverletter;

import com.demcha.compose.document.templates.data.common.Header;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoverLetterDocumentSpecTest {

    @Test
    void builderShouldCreateDocumentLevelCoverLetterSpec() {
        CoverLetterDocumentSpec spec = CoverLetterDocumentSpec.builder()
                .header(header -> header
                        .name("Artem Demchyshyn")
                        .email("artem@demo.dev", "artem@demo.dev"))
                .letter("Dear hiring team,")
                .job(job -> job
                        .company("Northwind Systems")
                        .title("Platform Engineer"))
                .build();

        assertThat(spec.header().getName()).isEqualTo("Artem Demchyshyn");
        assertThat(spec.header().getEmail().getTo()).isEqualTo("artem@demo.dev");
        assertThat(spec.body()).isEqualTo("Dear hiring team,");
        assertThat(spec.jobDetails().company()).isEqualTo("Northwind Systems");
        assertThat(spec.jobDetails().title()).isEqualTo("Platform Engineer");
    }

    @Test
    void ofShouldKeepExplicitInputsTogether() {
        Header header = Header.builder().name("Artem").build();
        JobDetails job = JobDetails.builder().company("Compose Ltd").build();

        CoverLetterDocumentSpec spec = CoverLetterDocumentSpec.of(header, "Body", job);

        assertThat(spec.header()).isSameAs(header);
        assertThat(spec.body()).isEqualTo("Body");
        assertThat(spec.jobDetails()).isSameAs(job);
    }
}
