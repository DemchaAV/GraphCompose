package com.demcha.Templatese.data;

import com.demcha.loyaut_core.components.content.link.LinkUrl;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LinkYml {
    private LinkUrl linkUrl;
    private String displayText;

}
