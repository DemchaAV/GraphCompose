package com.demcha.compose.document.snapshot;

import java.util.List;

/**
 * A {@link LayoutSnapshot} plus whichever optional diagnostic sections were
 * asked for.
 *
 * <p>A separate type rather than extra components on {@code LayoutSnapshot},
 * and that is the whole point of it. {@code LayoutSnapshot} is what consumers
 * hold committed baselines of; adding a component to that record would change
 * its JSON under any mapper, its {@code toString()} and its {@code equals} —
 * so a suite that serialized it by hand would go red on an upgrade that moved
 * nothing in the document. Wrapping leaves that shape untouched and puts the
 * new data somewhere a consumer only reaches by asking for it.</p>
 *
 * <p>{@link #layout()} is exactly the snapshot
 * {@link com.demcha.compose.document.api.DocumentSession#layoutSnapshot()}
 * returns, down to its {@code formatVersion}. The {@code formatVersion} on this
 * record versions the diagnostic envelope, which has its own schema and its own
 * lifecycle: a new section here does not move the layout snapshot's version, and
 * a change to the layout snapshot does not move this one.</p>
 *
 * @param formatVersion diagnostic envelope schema version
 * @param layout        the standard layout snapshot, unchanged
 * @param typography    resolved paragraph text runs in deterministic order; empty
 *                      when typography was not requested, and empty for a document
 *                      whose only text is drawn outside the paragraph pipeline
 * @author Artem Demchyshyn
 * @since 2.2.2
 */
public record LayoutDiagnosticSnapshot(
        String formatVersion,
        LayoutSnapshot layout,
        List<LayoutTypographySnapshot> typography) {

    /**
     * Freezes the typography list so a snapshot cannot be mutated after extraction.
     */
    public LayoutDiagnosticSnapshot {
        typography = typography == null ? List.of() : List.copyOf(typography);
    }
}
