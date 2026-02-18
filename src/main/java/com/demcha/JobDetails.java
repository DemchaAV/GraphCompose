package com.demcha;


import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for scraped LinkedIn job details.
 * Used for JSON output in CLI commands.
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
    /**
     * Builder-style factory from existing VacancyDTO.
     */

}
