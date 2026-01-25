package com.demcha.compose.Templatese.data;

import com.demcha.compose.loyaut_core.components.content.link.LinkUrl;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LinkYml {
    private LinkUrl linkUrl;
    private String displayText;

}
