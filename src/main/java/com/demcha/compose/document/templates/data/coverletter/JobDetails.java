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

    public static Builder builder() {
        return new Builder();
    }

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

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder company(String company) {
            this.company = company;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder seniorityLevel(String seniorityLevel) {
            this.seniorityLevel = seniorityLevel;
            return this;
        }

        public Builder employmentType(String employmentType) {
            this.employmentType = employmentType;
            return this;
        }

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
