package com.demcha.compose.document.templates.data;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mutable profile/contact header data used by CV and cover-letter templates.
 */
@Data
@NoArgsConstructor
public class Header {
    private String name;
    private String address;
    private String phoneNumber;
    private EmailYaml email;
    private LinkYml gitHub;
    private LinkYml linkedIn;
}
