package com.demcha.compose.document.templates.cv.v2.components;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.templates.cv.v2.data.CvIdentity;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;
import com.demcha.compose.document.templates.cv.v2.widgets.ContactLine;

/**
 * @deprecated Use
 * {@link com.demcha.compose.document.templates.cv.v2.widgets.ContactLine#centered}
 * instead — the widget gives you named centred/right-aligned
 * variants plus a configurable field order. Kept as a thin
 * delegating shim so v2 code written before the widgets layer keeps
 * compiling unchanged.
 */
@Deprecated
public final class ContactRenderer {

    private ContactRenderer() {
    }

    /**
     * @param section  the section builder being populated
     * @param identity the contact identity supplying the field values
     * @param theme    the active theme supplying palette, typography, and spacing
     * @deprecated delegates to {@link ContactLine#centered}.
     */
    @Deprecated
    public static void render(SectionBuilder section, CvIdentity identity, CvTheme theme) {
        ContactLine.centered(section, identity, theme);
    }
}
