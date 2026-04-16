package com.demcha.compose.document.templates.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
/**
 * Mutable top-of-document identity block used by canonical job-application templates.
 *
 * <p><b>Pipeline role:</b> supplies the contact and profile fields consumed by
 * shared template composers when they build the visible header region.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
public class Header {
    private String name;
    private String address;
    private String phoneNumber;
    private EmailYaml email;
    private LinkYml gitHub;
    private LinkYml linkedIn;
}
