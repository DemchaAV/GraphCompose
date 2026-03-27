package com.demcha.Templatese;


import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Template input describing the target role and company.
 * Used to personalize cover letters and other job-application documents.
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
