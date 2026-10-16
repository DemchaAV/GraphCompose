package com.demcha.compose.document.backend.semantic.docx;

import com.demcha.compose.document.image.DocumentImageData;
import com.demcha.compose.document.style.DocumentColor;
import com.demcha.compose.document.svg.SvgIcon;
import com.demcha.compose.document.svg.SvgPath;

import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Optional;

/**
 * Whether two builds of a node tree hold the same values: what a page zone's content built for
 * the first page and for the pages after it are compared by before the export writes it once
 * for every page ({@link DocxSemanticBackend}).
 *
 * <p>The nodes are records, read component by component, down to every run, style, inset and
 * link. A colour, a picture's data and an SVG drawing are read by what they hold, since each one
 * built afresh is not equal to itself. Any other value is read by its own {@code equals}, so one
 * of a kind that is not a value reads as otherwise once it is built afresh: the zone is then
 * written twice, alike, rather than once with one build's content where the other's belonged.</p>
 */
final class DocxNodeValues {

    private DocxNodeValues() {
    }

    /**
     * Whether two values are the same.
     *
     * @param one   a value, a node tree among them
     * @param other another, or {@code null}
     */
    static boolean alike(Object one, Object other) {
        if (one == other) {
            return true;
        }
        if (one == null || other == null || one.getClass() != other.getClass()) {
            return false;
        }
        if (one instanceof DocumentColor colour) {
            return colour.color().equals(((DocumentColor) other).color());
        }
        if (one instanceof DocumentImageData picture) {
            DocumentImageData drawn = (DocumentImageData) other;
            return picture.path().equals(drawn.path()) && alike(picture.bytes(), drawn.bytes());
        }
        if (one instanceof SvgIcon icon) {
            SvgIcon drawn = (SvgIcon) other;
            return alike(icon.layers(), drawn.layers()) && icon.sourceWidth() == drawn.sourceWidth()
                   && icon.sourceHeight() == drawn.sourceHeight() && alike(icon.text(), drawn.text());
        }
        if (one instanceof SvgPath path) {
            SvgPath drawn = (SvgPath) other;
            return alike(path.segments(), drawn.segments()) && path.sourceWidth() == drawn.sourceWidth()
                   && path.sourceHeight() == drawn.sourceHeight();
        }
        if (one instanceof Optional<?> value) {
            Optional<?> drawn = (Optional<?>) other;
            return value.isPresent() == drawn.isPresent() && (value.isEmpty() || alike(value.get(), drawn.get()));
        }
        if (one instanceof List<?> values) {
            List<?> drawn = (List<?>) other;
            if (values.size() != drawn.size()) {
                return false;
            }
            for (int index = 0; index < values.size(); index++) {
                if (!alike(values.get(index), drawn.get(index))) {
                    return false;
                }
            }
            return true;
        }
        if (one.getClass().isArray()) {
            int length = Array.getLength(one);
            if (length != Array.getLength(other)) {
                return false;
            }
            for (int index = 0; index < length; index++) {
                if (!alike(Array.get(one, index), Array.get(other, index))) {
                    return false;
                }
            }
            return true;
        }
        if (one.getClass().isRecord()) {
            for (RecordComponent component : one.getClass().getRecordComponents()) {
                try {
                    if (!alike(component.getAccessor().invoke(one), component.getAccessor().invoke(other))) {
                        return false;
                    }
                } catch (ReflectiveOperationException | RuntimeException unread) {
                    // A component it cannot read tells nothing of the two: they read as otherwise.
                    return false;
                }
            }
            return true;
        }
        return one.equals(other);
    }
}
