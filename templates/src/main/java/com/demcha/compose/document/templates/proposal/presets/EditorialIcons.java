package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.dsl.ShapeContainerBuilder;
import com.demcha.compose.document.node.DocumentNode;
import com.demcha.compose.document.style.ClipPolicy;
import com.demcha.compose.document.svg.SvgIcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classpath loader for the Editorial Proposal icon set — the marks beside
 * the at-a-glance facts and the check that opens every goal cell.
 *
 * <p>They ship inside the templates artifact under
 * {@code templates/proposal/editorial/icons/} as SVG, so they scale with
 * the box they are drawn into rather than being resampled. Parsed icons are
 * cached per token, so a two-page proposal reads each file once per JVM.</p>
 */
final class EditorialIcons {

    private static final String ICON_ROOT = "/templates/proposal/editorial/icons/";
    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private EditorialIcons() {
    }

    /**
     * Builds an icon node at a given width.
     *
     * @param token the icon token carried by the data
     * @param width the drawn width; the height follows the icon's ratio
     * @return the icon node
     * @throws IllegalArgumentException when the token names no packaged icon
     *         — an icon token is data, so a wrong one is a data error
     */
    static DocumentNode icon(String token, double width) {
        // Named after the token: an icon swapped in the data then reaches the
        // layout snapshot as a changed node path, not just as moved pixels.
        return new ShapeContainerBuilder()
                .name("Icon-" + token)
                .rectangle(width, width)
                .clipPolicy(ClipPolicy.OVERFLOW_VISIBLE)
                .centerLeft(CACHE.computeIfAbsent(token, EditorialIcons::read).node(width))
                .build();
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = EditorialIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException(
                        "Unknown editorial proposal icon token '" + token
                                + "' — no packaged icon at " + resourcePath + ".");
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read editorial proposal icon: " + resourcePath, e);
        }
    }
}
