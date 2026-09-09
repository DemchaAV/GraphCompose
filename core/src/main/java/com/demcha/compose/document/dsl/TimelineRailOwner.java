package com.demcha.compose.document.dsl;

/**
 * One timeline's identity, and the configuration its rail will be drawn from.
 *
 * <p>Every marker in a timeline anchors on the same instance of this, so the pass that
 * later draws the rail asks for <em>its</em> owner and gets back that timeline's markers
 * and nobody else's. Two timelines on one page are two owners and never merge, and a pass
 * asking for an owner the document does not contain gets an empty list — which is how it
 * decides it has nothing to do, with no document inspection and no feature flag.</p>
 *
 * <p>Deliberately a plain final class and not a record. The seam compares these with
 * {@code ==}, and a record invites the reader to think in value equality: two timelines
 * configured identically are still two timelines, and a record would make them look like
 * one.</p>
 *
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
final class TimelineRailOwner {

    private final TimelineRailSpec rail;

    TimelineRailOwner(TimelineRailSpec rail) {
        this.rail = rail;
    }

    /**
     * The rail this timeline asked for.
     *
     * @return the rail spec
     */
    TimelineRailSpec rail() {
        return rail;
    }
}
