package com.demcha.compose.document.templates.cv.presets;

import com.demcha.compose.document.api.DocumentSession;
import com.demcha.compose.font.FontFamilyDefinition;

/**
 * Registers the display family the {@link OrangeOps} gates compose against.
 *
 * <p>The preset names Oswald and carries none of it — neither the templates
 * artifact nor {@code graph-compose-fonts} ships the family — so every caller
 * registers it, and in this module the caller is a test. The two faces are qa's
 * own test resources under {@code /fonts/oswald/}, beside the licence they are
 * distributed under. Without this call the engine substitutes and both gates
 * would be measuring a face the design was never set in.</p>
 */
final class OrangeOpsTestFont {

    private OrangeOpsTestFont() {
    }

    static void register(DocumentSession document) {
        document.registerFontFamily(
                FontFamilyDefinition.classpath(OrangeOps.DISPLAY_FONT,
                                "/fonts/oswald/Oswald-Regular.ttf")
                        .wordFamily("Oswald")
                        .boldResource("/fonts/oswald/Oswald-SemiBold.ttf")
                        .build());
    }
}
