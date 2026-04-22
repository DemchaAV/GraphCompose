package com.demcha.compose.document.templates.data.coverletter;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * Immutable job-application context passed into canonical cover-letter composition.
 *
 * <p><b>Pipeline role:</b> captures the target role, company, and optional job
 * posting metadata so template composers can resolve letter copy and chrome.</p>
 *
 * <p><b>Mutability:</b> immutable record. <b>Thread-safety:</b> thread-safe
 * when referenced through immutable component values.</p>
 */
public record JobDetails(
        String url,
        String title,
        String company,
        String location,
        String description,
        String seniorityLevel,
        String employmentType) {
}
