package com.demcha.compose.layout_core.components.content.bookmark;

import com.demcha.compose.layout_core.components.core.Component;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Content component that marks an entity as a PDF bookmark (outline) entry.
 *
 * <p>Attach this to a {@code Module} or any entity that has a resolved
 * {@code Placement}. The renderer will create a {@code PDOutlineItem} pointing
 * to the page and Y-coordinate of this entity.</p>
 *
 * @author Artem Demchyshyn
 */
@Getter
@ToString
@EqualsAndHashCode
public final class BookmarkEntry implements Component {

    /** Display title in the PDF bookmark panel. */
    private final String title;

    /** Nesting level (0 = root, 1 = child, 2 = grandchild, etc.). */
    private final int level;

    public BookmarkEntry(String title) {
        this(title, 0);
    }

    public BookmarkEntry(String title, int level) {
        this.title = title;
        this.level = Math.max(0, level);
    }
}
