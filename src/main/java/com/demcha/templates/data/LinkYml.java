package com.demcha.templates.data;

import com.demcha.compose.layout_core.components.content.link.LinkUrl;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LinkYml {
    private LinkUrl linkUrl;
    private String displayText;

}
