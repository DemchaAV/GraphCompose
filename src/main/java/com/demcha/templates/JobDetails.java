package com.demcha.templates;


import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Template input describing the target role and company.
 * Used to personalize cover letters and other job-application documents.
 *
 * @deprecated Use {@link com.demcha.compose.document.templates.data.JobDetails}
 *             instead.
 */
@Deprecated(forRemoval = false)
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
