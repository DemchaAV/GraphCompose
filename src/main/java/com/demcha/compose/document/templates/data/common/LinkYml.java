package com.demcha.compose.document.templates.data.common;

import com.demcha.compose.layout_core.components.content.link.LinkUrl;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
/**
 * Mutable hyperlink payload used by canonical template header and profile data.
 *
 * <p><b>Pipeline role:</b> stores a destination plus display text that shared
 * template composers translate into visible contact links.</p>
 *
 * <p><b>Mutability:</b> mutable Lombok-backed bean. <b>Thread-safety:</b>
 * not thread-safe.</p>
 */
public class LinkYml {
    private LinkUrl linkUrl;
    private String displayText;
}
