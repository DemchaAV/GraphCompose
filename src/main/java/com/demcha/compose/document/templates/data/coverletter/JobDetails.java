package com.demcha.compose.document.templates.data.coverletter;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Immutable job-application context passed into canonical cover-letter composition.
 *
 * <p><b>Pipeline role:</b> captures the target role, company, and optional job
 * posting metadata so template composers can resolve letter copy and chrome.</p>
 *
 * <p><b>Mutability:</b> immutable record. <b>Thread-safety:</b> thread-safe
 * when referenced through immutable component values.</p>
 *
 * @param url optional job posting URL
 * @param title target role title
 * @param company target company name
 * @param location target job location
 * @param description optional job description text
 * @param seniorityLevel optional seniority label
 * @param employmentType optional employment type label
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
     * Starts a fluent job details builder.
     *
     * @return job details builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for job details.
     */
    public static final class Builder {
        private String url;
        private String title;
        private String company;
        private String location;
        private String description;
        private String seniorityLevel;
        private String employmentType;

        private Builder() {
        }

        /**
         * Sets the job posting URL.
         *
         * @param url job posting URL
         * @return this builder
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Sets the target role title.
         *
         * @param title role title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the company name.
         *
         * @param company company name
         * @return this builder
         */
        public Builder company(String company) {
            this.company = company;
            return this;
        }

        /**
         * Sets the job location.
         *
         * @param location job location
         * @return this builder
         */
        public Builder location(String location) {
            this.location = location;
            return this;
        }

        /**
         * Sets the job description text.
         *
         * @param description job description text
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the seniority label.
         *
         * @param seniorityLevel seniority label
         * @return this builder
         */
        public Builder seniorityLevel(String seniorityLevel) {
            this.seniorityLevel = seniorityLevel;
            return this;
        }

        /**
         * Sets the employment type label.
         *
         * @param employmentType employment type label
         * @return this builder
         */
        public Builder employmentType(String employmentType) {
            this.employmentType = employmentType;
            return this;
        }

        /**
         * Builds immutable job details.
         *
         * @return job details
         */
        public JobDetails build() {
            return new JobDetails(
                    url,
                    title,
                    company,
                    location,
                    description,
                    seniorityLevel,
                    employmentType);
        }
    }
}
