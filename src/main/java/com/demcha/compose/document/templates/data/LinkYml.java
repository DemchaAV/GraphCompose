package com.demcha.compose.document.templates.data;

import com.demcha.compose.layout_core.components.content.link.LinkUrl;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mutable link DTO used by CV and cover-letter inputs.
 */
@Data
@NoArgsConstructor
public class LinkYml {
    private LinkUrl linkUrl;
    private String displayText;
}
