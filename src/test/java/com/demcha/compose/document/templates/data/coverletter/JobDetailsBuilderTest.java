package com.demcha.compose.document.templates.data.coverletter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobDetailsBuilderTest {

    @Test
    void builderShouldCreateImmutableJobContext() {
        JobDetails details = JobDetails.builder()
                .url("https://northwind.example/jobs/platform")
                .title("Senior Platform Engineer")
                .company("Northwind Systems")
                .location("London / Remote")
                .description("Lead reusable internal platform capabilities.")
                .seniorityLevel("Senior")
                .employmentType("Full-time")
                .build();

        assertThat(details.url()).isEqualTo("https://northwind.example/jobs/platform");
        assertThat(details.title()).isEqualTo("Senior Platform Engineer");
        assertThat(details.company()).isEqualTo("Northwind Systems");
        assertThat(details.location()).isEqualTo("London / Remote");
        assertThat(details.description()).isEqualTo("Lead reusable internal platform capabilities.");
        assertThat(details.seniorityLevel()).isEqualTo("Senior");
        assertThat(details.employmentType()).isEqualTo("Full-time");
    }
}
