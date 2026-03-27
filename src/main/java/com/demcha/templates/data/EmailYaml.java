package com.demcha.templates.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EmailYaml {
    private String to;
    private String subject;
    private String body;
    private String displayText;
}
