package com.demcha.compose.document.templates.data;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Template input describing the target role and company for job-application
 * documents.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobDetails(
        String url,
        String title,
        String company,
        String location,
        String description,
        String seniorityLevel,
        String employmentType) {
}
