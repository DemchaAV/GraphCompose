package com.demcha.compose.document.templates.proposal.presets;

import com.demcha.compose.document.svg.SvgIcon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * The marks the Indigo proposal packages.
 *
 * <p>Two sets of four, and they are not interchangeable: the header's marks are
 * drawn in the accent for the pale disc they sit on, and the band's in white for
 * the near-black tile. Naming a band mark on a header disc draws white on nearly
 * white, so each set belongs to the block it was cut for. A tile that names no
 * mark at all is still drawn — the disc and the tile are the design's shapes
 * rather than containers for a particular drawing.</p>
 */
final class IndigoIcons {

    /** The marks a document may name on a header disc. */
    static final List<String> HEADER_TOKENS = List.of(
            "date", "identifier", "valid", "prepared-by");

    /** The marks a document may name on a band tile. */
    static final List<String> BAND_TOKENS = List.of(
            "global", "infrastructure", "secure", "apis");

    /** Every mark this preset packages. */
    static final List<String> TOKENS =
            Stream.concat(HEADER_TOKENS.stream(), BAND_TOKENS.stream()).toList();

    private static final String ICON_ROOT = "/templates/proposal/indigo/icons/";

    private static final Map<String, SvgIcon> CACHE = new ConcurrentHashMap<>();

    private IndigoIcons() {
    }

    /**
     * The packaged mark a token names, if the block it was named on can carry
     * it.
     *
     * <p>The wrong set is refused rather than drawn: a band mark on a header
     * disc is white on nearly white, which renders as nothing at all and reads
     * as a bug in the preset rather than a token in the document.</p>
     *
     * @param token   one of this preset's tokens
     * @param allowed the set the block draws from
     * @return the parsed icon
     * @throws IllegalArgumentException when the token names no mark this block
     *         can draw, listing the ones it can — an icon token is data, so a
     *         wrong one is a data error and not a missing resource
     */
    static SvgIcon icon(String token, List<String> allowed) {
        if (!allowed.contains(token)) {
            throw new IllegalArgumentException(
                    "Unknown indigo proposal icon token '" + token
                            + "'. This block draws " + allowed + ".");
        }
        return CACHE.computeIfAbsent(token, IndigoIcons::read);
    }

    /**
     * Whether the document named a mark at all, for a tile that may carry none.
     *
     * <p>It asks whether one was named, not whether this preset packages it: a
     * token naming nothing here is a data error, and
     * {@link #icon(String, List)} reports it by name — which a silently empty
     * tile would hide.</p>
     *
     * @param token the token a document states
     * @return {@code true} when the document named one
     */
    static boolean has(String token) {
        return token != null && !token.isBlank();
    }

    private static SvgIcon read(String token) {
        String resourcePath = ICON_ROOT + token + ".svg";
        try (InputStream input = IndigoIcons.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing indigo proposal icon: " + resourcePath);
            }
            return SvgIcon.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read indigo proposal icon: " + resourcePath, e);
        }
    }
}
