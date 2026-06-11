package com.demcha.compose.document.templates.cv.v2.widgets;

import com.demcha.compose.document.dsl.SectionBuilder;
import com.demcha.compose.document.node.TextAlign;
import com.demcha.compose.document.style.DocumentInsets;
import com.demcha.compose.document.style.DocumentTextDecoration;
import com.demcha.compose.document.style.DocumentTextStyle;
import com.demcha.compose.document.templates.cv.v2.components.CvTextStyles;
import com.demcha.compose.document.templates.cv.v2.components.TextOrnaments;
import com.demcha.compose.document.templates.cv.v2.data.CvSkill;
import com.demcha.compose.document.templates.cv.v2.theme.CvTheme;

import java.util.Objects;

/**
 * Data-driven proficiency-bar widget — a spaced-caps skill label above a
 * thin full-width track with a short vertical marker positioned by the
 * skill's proficiency level.
 *
 * <h2>What it renders</h2>
 *
 * <p>Two stacked elements:</p>
 *
 * <ol>
 *   <li>the skill name in letter-spaced uppercase (bold, ink), via
 *       {@link TextOrnaments#spacedUpper(String)};</li>
 *   <li>a horizontal track line spanning {@code trackWidth} in the theme
 *       rule colour, overlaid with a short vertical marker (ink) whose
 *       left offset is {@code level * trackWidth} — so a higher
 *       proficiency pushes the tick further right.</li>
 * </ol>
 *
 * <p>The marker is drawn as a second {@code addLine} immediately after the
 * track, pulled back up over the track with a negative top margin and
 * shifted right with a left margin of {@code level * trackWidth}. This is
 * the same overlay trick the flat Mint Editorial blueprint used for its
 * {@code skillBar()} (track 0.65pt rule, marker 1.2pt ink, ~8pt tall).</p>
 *
 * <h2>When to use</h2>
 *
 * <p>Reach for {@code SkillBar} when a preset renders skills as visual
 * meters rather than plain labels or comma-separated chips — the editorial
 * sidebar look. It reads the proficiency from {@link CvSkill#level()}: when
 * the level is empty the label renders <strong>with no bar</strong> (so a
 * mixed list of levelled and name-only skills degrades gracefully instead of
 * drawing a meaningless empty track).</p>
 *
 * <h2>Reuse</h2>
 *
 * <p>Lives in {@code cv/v2/widgets} because a proficiency meter keyed off
 * {@link CvSkill} is CV-specific (no other document family models skill
 * levels). Mint Editorial is the first consumer; any future CV preset that
 * wants level-driven skill bars can reuse it without re-deriving the marker
 * geometry.</p>
 */
public final class SkillBar {

    /**
     * Track stroke width in points — a hairline rule.
     */
    private static final double TRACK_THICKNESS = 0.65;

    /**
     * Proficiency marker stroke width in points — heavier than the track.
     */
    private static final double MARKER_THICKNESS = 1.2;

    /**
     * Proficiency marker height in points.
     */
    private static final double MARKER_HEIGHT = 8.0;

    /**
     * Gap (points) between the skill label and the track below it.
     */
    private static final double LABEL_TO_TRACK_GAP = 8.0;

    /**
     * Vertical pull-up (points) applied to the marker so it overlays the
     * track instead of stacking below it. Half the marker height plus a
     * touch so the tick straddles the track line.
     */
    private static final double MARKER_OVERLAP = MARKER_HEIGHT / 2.0 + 0.35;

    /**
     * Gap (points) below the marker before the next skill bar.
     */
    private static final double BAR_BOTTOM_GAP = 12.0;

    private SkillBar() {
    }

    /**
     * Renders a skill label and, when the skill carries a proficiency
     * level, its proficiency bar.
     *
     * @param host       host section the label + bar are appended to
     * @param skill      skill to render; its {@link CvSkill#level()} drives
     *                   the marker position (empty level → label only)
     * @param trackWidth full track width in points (typically the sidebar
     *                   inner width)
     * @param theme      active theme — supplies the body font, label/ink
     *                   colour, and the rule colour for the track
     */
    public static void render(SectionBuilder host, CvSkill skill,
                              double trackWidth, CvTheme theme) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(theme, "theme");

        boolean levelled = skill.level().isPresent();
        DocumentTextStyle labelStyle = CvTextStyles.of(
                theme.typography().bodyFont(),
                theme.typography().sizeEntryTitle(),
                DocumentTextDecoration.BOLD,
                theme.palette().ink());
        // When there is no bar, the label is the whole entry, so give it a
        // little breathing room below to match the rhythm of a barred entry.
        double labelBottom = levelled ? LABEL_TO_TRACK_GAP : BAR_BOTTOM_GAP;
        host.addParagraph(paragraph -> paragraph
                .text(TextOrnaments.spacedUpper(skill.name()))
                .textStyle(labelStyle)
                .align(TextAlign.LEFT)
                .margin(DocumentInsets.bottom(labelBottom)));

        if (!levelled) {
            return;
        }

        double level = skill.level().getAsDouble();
        double markerLeft = Math.max(0.0, Math.min(1.0, level)) * trackWidth;
        host.addLine(line -> line
                .horizontal(trackWidth)
                .color(theme.palette().rule())
                .thickness(TRACK_THICKNESS)
                .margin(DocumentInsets.zero()));
        host.addLine(line -> line
                .vertical(MARKER_HEIGHT)
                .color(theme.palette().ink())
                .thickness(MARKER_THICKNESS)
                .margin(new DocumentInsets(-MARKER_OVERLAP, 0,
                        BAR_BOTTOM_GAP, markerLeft)));
    }
}
