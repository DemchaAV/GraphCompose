package com.demcha.compose.document.templates.receipt.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.dsl.TimelineMarker;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.templates.core.text.TextOrnaments;
import com.demcha.compose.document.templates.core.theme.BrandTheme;
import com.demcha.compose.document.templates.data.receipt.ReceiptEvent;
import com.demcha.compose.document.templates.receipt.components.ReceiptStyles;

import java.util.List;
import java.util.Objects;

/**
 * The payment's steps as a connected timeline — instructed, cleared,
 * settled — with the last step marked as the one it reached.
 *
 * <p>This is the block a status word cannot replace. "Completed" says the
 * payment finished; the trail says when each stage happened, which is what a
 * reader chasing a late payment or a support agent reading the receipt back
 * actually needs.</p>
 *
 * <p>Steps before the last render as hollow circles in the rule colour and
 * the last as a filled accent dot, so the eye lands on where the payment
 * got to without reading a single timestamp.</p>
 */
public final class StatusTrail {

    /** Diameter of a timeline marker, in points. */
    private static final double MARKER_SIZE = 7.0;

    /** Space between the connector rail and the marker, in points. */
    private static final double GUTTER = 12.0;

    /**
     * Marker column weight. Small, because these markers are 7pt dots: the
     * builder's default is sized for numbered discs and would indent every
     * step by a tenth of the page.
     */
    private static final double MARKER_COLUMN_WEIGHT = 0.022;

    /** Gap between a marker and its step name, in points. */
    private static final double MARKER_GAP = 7.0;

    /** Separator between a step's timestamp and its explanation. */
    private static final String META_SEPARATOR = "  ·  ";

    private StatusTrail() {
    }

    private static String joinMeta(ReceiptEvent event) {
        if (event.timestamp().isBlank()) {
            return event.detail();
        }
        if (event.detail().isBlank()) {
            return event.timestamp();
        }
        return event.timestamp() + META_SEPARATOR + event.detail();
    }

    /**
     * Renders the trail under a spaced-caps heading; an empty step list
     * renders nothing.
     *
     * @param host   section the trail is appended to
     * @param title  heading over the trail
     * @param events the payment's steps, oldest first
     * @param accent the issuer's brand accent, used for the reached step
     * @param theme  active theme
     */
    public static void render(SectionBuilder host, String title, List<ReceiptEvent> events,
                              DocumentColor accent, BrandTheme theme) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(theme, "theme");
        if (events == null || events.isEmpty()) {
            return;
        }
        DocumentColor reached = accent == null ? theme.palette().ink() : accent;

        host.addSection("ReceiptStatusTrail", block -> {
            block.keepTogether();
            if (title != null && !title.isBlank()) {
                block.addParagraph(p -> p
                        .text(TextOrnaments.spacedUpper(title))
                        .textStyle(ReceiptStyles.groupTitle(theme))
                        .margin(new DocumentInsets(0, 0, 8, 0)));
            }
            block.addTimeline(timeline -> {
                timeline.connector(theme.palette().rule(), theme.spacing().accentRuleWidth())
                        .gutter(GUTTER)
                        .markerColumnWeight(MARKER_COLUMN_WEIGHT)
                        .markerGap(MARKER_GAP)
                        .spacing(theme.spacing().entrySeparation())
                        .titleStyle(ReceiptStyles.valueStrong(theme))
                        .metaStyle(ReceiptStyles.caption(theme))
                        .keepEntriesTogether();
                for (int i = 0; i < events.size(); i++) {
                    ReceiptEvent event = events.get(i);
                    boolean last = i == events.size() - 1;
                    TimelineMarker marker = last
                            ? TimelineMarker.dot(MARKER_SIZE, reached)
                            : TimelineMarker.dot(MARKER_SIZE, theme.palette().rule());
                    // Timestamp and detail go on one meta line rather than into the
                    // builder's body slot: the body renders as a sibling of the
                    // marker row, so it would start at the rail instead of under the
                    // step's own title.
                    String meta = joinMeta(event);
                    timeline.entry(marker, entry -> {
                        entry.title(event.label());
                        if (!meta.isBlank()) {
                            entry.meta(meta);
                        }
                    });
                }
            });
        });
    }
}
