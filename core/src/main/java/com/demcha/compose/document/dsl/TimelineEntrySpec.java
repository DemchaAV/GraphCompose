package com.demcha.compose.document.dsl;

import java.util.function.Consumer;

/**
 * One entry, with no memory of which API described it.
 *
 * <p>Content arrives already resolved: whichever builder produced this entry has already
 * chosen the text styles, so nothing downstream reads a title, a meta line or a body. What
 * remains is the geometric distinction the layout actually depends on — whether a block
 * sits <em>beside</em> the marker, indented past the marker column, or <em>below</em> it at
 * the entry's own left edge.</p>
 *
 * @param marker the marker drawn in the rail for this entry
 * @param beside content rendered into the header row's column next to the marker; applied
 *               even when it draws nothing, because the column itself is part of the
 *               entry's geometry
 * @param below  content rendered under the header row, starting at the entry's left edge
 * @author Artem Demchyshyn
 * @since 2.4.0
 */
record TimelineEntrySpec(TimelineMarker marker,
                         Consumer<SectionBuilder> beside,
                         Consumer<SectionBuilder> below) {
}
